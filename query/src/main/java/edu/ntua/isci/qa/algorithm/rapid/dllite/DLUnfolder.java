/*Copyright 2011, 2013, 2015 Alexandros Chortaras

 This file is part of Rapid.

 Rapid is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Rapid is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Rapid.  If not, see <http://www.gnu.org/licenses/>.*/

package edu.ntua.isci.qa.algorithm.rapid.dllite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import edu.ntua.isci.common.lp.Atom;
import edu.ntua.isci.common.lp.Clause;
import edu.ntua.isci.common.lp.Predicate;
import edu.ntua.isci.common.lp.Substitution;
import edu.ntua.isci.common.lp.Term;

import edu.ntua.isci.qa.algorithm.rapid.AtomUnfolder;
import edu.ntua.isci.qa.algorithm.rapid.AtomUnfoldingResult;
import edu.ntua.isci.qa.algorithm.rapid.FlattenerExtra;
import edu.ntua.isci.qa.algorithm.rapid.IndexEntry;
import edu.ntua.isci.qa.algorithm.rapid.IndexEntryList;
import edu.ntua.isci.qa.algorithm.rapid.Rapid;
import edu.ntua.isci.qa.algorithm.rapid.UnfoldedClause;
import edu.ntua.isci.qa.algorithm.rapid.ClauseUnfolder;
import edu.ntua.isci.qa.lp.theory.FCTheory;
import edu.ntua.isci.qa.utils.Utils;

public class DLUnfolder extends ClauseUnfolder {

	protected DLSubsumeChecker sbCheck;
	
	public DLUnfolder(FCTheory program, AtomUnfolder au, DLSubsumeChecker sbCheck) {
		super(program, au);

		this.sbCheck = sbCheck;
	}

	public ArrayList<Clause> getExtraQueries() {
		return new ArrayList<Clause>(); 
	}
	
	public Clause testUnfold(Clause clause, FlattenerExtra extra) {
		int size = clause.bodySize();
		
		Substitution subst = Rapid.replaceUnboundVariablesByUnderscoreSubstitution(clause);
		
		topAtoms = new Set[size];
		for (int i = 0; i < topAtoms.length; i++) {
			topAtoms[i] = new HashSet<Atom>();
			topAtoms[i].add(clause.getBodyAtomAt(i).apply(subst));
		}

		atomList = createAtomList(topAtoms, clause.getBoundTerms(), extra);

		for (int ti = 0; ti < topAtoms.length; ti++) {
			atomList[effectiveTopIndex(ti)].markTopAtoms(topAtoms[ti]);
		}
		
		Set<Integer> remove = new HashSet<Integer>();
		for (int i = 0; i < atomList.length; i++) {
			ArrayList<Integer> cover = ((DLIndexEntry)atomList[i].getFirst().iterator().next()).covered; 
			for (int k : cover) {
				if (k != i) {
					remove.add(k);
				}
			}
		}

		ArrayList<Atom> newClause = new ArrayList<Atom>(); 
		for (int i = 0; i < size; i++) {
			if (!remove.contains(i)) {
				newClause.add(clause.getBodyAtomAt(i));
			}
		}
		
		return new Clause(clause.getHead(), newClause);
	}

	public ArrayList<UnfoldedClause> unfold(Clause clause, FlattenerExtra extra) {
		return unfold(clause, -1, null, extra);
	}
	
	public ArrayList<UnfoldedClause> unfold(Clause clause, int eIndex, Set<Atom> appendix, FlattenerExtra extra) {

		int[] index = new int[clause.bodySize()];
		if (index.length > 0) {
			index[0] = -1;
		}

		prepare(clause, eIndex, appendix, extra);
		
		unfold(index, 0,  new HashSet<Integer>(), new ArrayList<ArrayList<Integer>>());
		
		return result;
	}

	protected void unfold(int[] index, int k, Set<Integer> next, ArrayList<ArrayList<Integer>> history) {
		if (k == atomList.length) {
			if (isCoveringOK(history)) {
				UnfoldedClause newClause = sbCheck.check(head, (DLIndexEntryList[])atomList, index, isSymmetric, this);
				if (newClause != null) {
					result.add(newClause);
				}
			}
			
		} else {
			if (next.contains(k)) {
				Arrays.fill(index, k, index.length, -1);
				
				unfold(index, k + 1, next, history);
			} else {
				ArrayList<IndexEntry> kList = atomList[k].getList();

				loop:
				for (int i = 0; i < kList.size(); i++) {
					index[k]++;
					Arrays.fill(index, k + 1, index.length, -1);
					
					DLIndexEntry currentEntry = (DLIndexEntry)kList.get(i);
					
					ArrayList<ArrayList<Integer>> newHistory = new ArrayList<ArrayList<Integer>>();
					
					if (Utils.subsumesSome(history, currentEntry.covered)) {
						continue loop;
					} else {
						for (int an : currentEntry.covered) {
							if (index[an] != -1 && atomList[an].isInList(currentEntry.getAtom(), index[an])) {
								continue loop;
							}
						}

						newHistory.addAll(history);
						newHistory.add(currentEntry.covered);
					}
					
					Set<Integer> newNext = new HashSet<Integer>();
					newNext.addAll(next);
					if (currentEntry.fIndex != null) {
						newNext.addAll(currentEntry.fIndex);
					}
					
					unfold(index, k + 1, newNext, newHistory);
				}
			}
		}
	}

	public static boolean isCoveringOK(ArrayList<ArrayList<Integer>> history) {
		Set<Integer> common = new HashSet<Integer>();
		Set<Integer> nonCommon = new HashSet<Integer>();

		for (ArrayList<Integer> list : history) {
			for (int i : list) {
				if (common.contains(i)) {
					
				} else if (nonCommon.contains(i)) {
					common.add(i);
					nonCommon.remove(i);
				} else {
					nonCommon.add(i);
				}
			}
		}
		
		for (ArrayList<Integer> list : history) {
			if (common.containsAll(list)) {
				return false;
			}
		}
		
		return true;
	}
	
	protected IndexEntryList[] createAtomList(Set<Atom>[] topAtoms, Set<Term> boundTerms, FlattenerExtra extra) {	
		int size = topAtoms.length;
		
		ArrayList<Atom[]> aList = new ArrayList<Atom[]>();
		Substitution[] aSubst = new Substitution[size];
		
		for (int k = 0; k < size; k++) {
			AtomUnfoldingResult aur = au.getUnfolding(topAtoms[k], boundTerms, null, extra);
			if (!aur.getSubstitution().isEmpty()) {
				aSubst[k] = aur.getSubstitution();
			}
	
			aList.add(((DLAtomUnfolding)aur.getUnfolding()).getUnfoldings());
		}

		DLIndexEntryList[] atomList = new DLIndexEntryList[size];
		
		for (int k = 0; k < size; k++) {
			ArrayList<IndexEntry> currentList = new ArrayList<IndexEntry>();
			
			for (Atom a : aList.get(k)) {
				DLIndexEntry aie;
				if (aSubst[k] == null) {
					aie = new DLIndexEntry(a);
				} else {
					aie = new DLIndexEntry(a.apply(aSubst[k]));
				}

				currentList.add(aie);
			}

			atomList[k] = new DLIndexEntryList(currentList);
			
			for (int i = 0; i < currentList.size(); i++) {
				exactLinkBack(atomList, k, i, boundTerms);
				subsumeLinkBack(atomList, k, i, boundTerms);
			}
		}
		
		return atomList;
	}

	public int effectiveTopIndex(int k) {
		return k;
	}
	
	protected void exactLinkBack(DLIndexEntryList[] atomList, int k, int i, Set<Term> boundTerms) {
		DLIndexEntry currentEntry = (DLIndexEntry)atomList[k].getIndexEntryAt(i);
		
		for (int j = k - 1; j >= 0; j--) {
			DLIndexEntryList prevList = atomList[j];
			
			for (Found f : atomFind(prevList, currentEntry.getAtom(), boundTerms)) {
				if (f.type == 0) {
					propagateBack((DLIndexEntry)prevList.getIndexEntryAt(f.pos), currentEntry, k, boundTerms); 
					return;
				}
			} 
		}
		
		currentEntry.covered = new ArrayList<Integer>();
		currentEntry.covered.add(k);
	}
	
	protected void subsumeLinkBack(DLIndexEntryList[] atomList, int k, int i, Set<Term> boundTerms) {
		DLIndexEntryList currentList = atomList[k];
		DLIndexEntry currentEntry = (DLIndexEntry)currentList.getIndexEntryAt(i);

		for (int j = k - 1; j >= 0; j--) {
			DLIndexEntryList prevList = atomList[j];

			for (Found f : atomFind(prevList, currentEntry.getAtom(), boundTerms)) {
				if (f.type == 2) {
					DLIndexEntry newEntry = new DLIndexEntry(f.atom);
					newEntry.mainRoot = currentEntry;
					
					currentList.lastInsertIfNotInList(i, f.atom, newEntry);
				} else if (f.type == 1) {
					DLIndexEntry newEntry = new DLIndexEntry(f.atom);
					newEntry.mainRoot = (DLIndexEntry)prevList.getIndexEntryAt(f.pos);
					
					int si = prevList.lastInsertIfNotInList(f.pos, f.atom, newEntry);
					if (si != -1) {
						exactLinkBack(atomList, j, si, boundTerms);
						subsumeLinkBack(atomList, j, si, boundTerms);
						
						propagateBack(newEntry, currentEntry, k, boundTerms);
					}
				}
			}
		}
	}
	
	protected void propagateBack(DLIndexEntry matchedEntry, DLIndexEntry newEntry, int k, Set<Term> boundTerms) {
		matchedEntry.nextEntry = newEntry; 
		
		DLIndexEntry firstEntry = matchedEntry;
		if (matchedEntry.firstEntry != null) {
			firstEntry = matchedEntry.firstEntry;
		}
		newEntry.firstEntry = firstEntry;

		Set<DLIndexEntry> roots = new HashSet<DLIndexEntry>();
		if (newEntry.mainRoot != null) {
			roots.add(newEntry.mainRoot);
		}

		DLIndexEntry entry = firstEntry;
		do {
			if (entry.fIndex == null) {
				entry.fIndex = new ArrayList<Integer>();
			}
			entry.fIndex.add(k);
			
			if (entry.roots != null) {
				roots.addAll(entry.roots);
			}
			if (entry.mainRoot != null) {
				roots.add(entry.mainRoot);
			}
			
			entry = entry.nextEntry;
		} while (entry != newEntry);

		if (roots.size() > 0) {
			Set<DLIndexEntry> allChildren = new HashSet<DLIndexEntry>();

			for (DLIndexEntry root : roots) {
				if (root.children != null) {
					for (DLIndexEntry ch : root.children) {
						allChildren.add(ch);
					}
				}
			}
			
			entry = firstEntry;
			
			do {
				entry.roots = roots;
				allChildren.add(entry);
				
				entry = entry.nextEntry;
			} while (entry != null);
			
			for (DLIndexEntry cRoot : roots) {
				Set<DLIndexEntry> children = new HashSet<DLIndexEntry>();
				for (DLIndexEntry c : allChildren) {
					Atom.Subsumee ss = Atom.exactSubsumes(cRoot.getAtom(), c.getAtom(), boundTerms);
					if (ss != null && ss.mode == 1) {
						children.add(c);
					}
				}
				cRoot.children = children;
			}
			
			entry = firstEntry;
		}
		
		newEntry.covered = matchedEntry.covered;
		newEntry.covered.add(k);
	}
	
	
	private ArrayList<Found> atomFind(DLIndexEntryList list, Atom atom, Set<Term> boundTerms) {
		Predicate pred = atom.getPredicate();
		ArrayList<IndexEntry> atomList = list.getList();
		
		ArrayList<Found> res = new ArrayList<Found>();

		int pos = list.binarySearch(pred);
		if (pos < 0) {
			return res;
		} else {
			while (pos < atomList.size() && atomList.get(pos).getAtom().getPredicate().equals(pred)) {
				Atom.Subsumee t = Atom.exactSubsumes(atomList.get(pos).getAtom(), atom, boundTerms);
				
				if (t != null) {
					res.add(new Found(pos, t.mode, t.subsumee));
				} 
				
				pos++;
			}
			
			return res;
		}
	}
}
