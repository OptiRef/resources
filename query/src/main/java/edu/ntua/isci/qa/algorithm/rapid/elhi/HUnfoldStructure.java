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

package edu.ntua.isci.qa.algorithm.rapid.elhi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.ntua.isci.qa.algorithm.rapid.FunctionMap;
import edu.ntua.isci.qa.algorithm.rapid.ProperRenamedAtom;
import edu.ntua.isci.qa.algorithm.rapid.Rapid;
import edu.ntua.isci.qa.algorithm.rapid.SortAtom;
import edu.ntua.isci.qa.lp.theory.FCTheory;
import edu.ntua.isci.common.lp.Atom;
import edu.ntua.isci.common.lp.AtomArray;
import edu.ntua.isci.common.lp.Predicate;
import edu.ntua.isci.common.lp.Substitution;
import edu.ntua.isci.common.lp.Term;
import edu.ntua.isci.common.lp.Variable;
import edu.ntua.isci.common.utils.Counter;

public class HUnfoldStructure implements Comparable<HUnfoldStructure> {
	
	private ArrayList<VUnfoldStructure> content;
	private ArrayList<Substitution> substs;
	
	public boolean isInBlock;
	public Set<Integer> blockedIndices;
	
	private ArrayList<Atom> firstAtom;
	
	public Map<Variable, FunctionMap> usedFunctions;
	
	private static int cycle = 0;
	private boolean findCommon;
	
	public SortAtom sa;
	
	public HUnfoldStructure(boolean findCommon) {
		this.findCommon = findCommon;
		
		this.content = new ArrayList<VUnfoldStructure>();
		this.substs = new ArrayList<Substitution>();
		
		usedFunctions = new HashMap<Variable, FunctionMap>();
	}

	public VUnfoldStructure getElementAt(int k) {
		return content.get(k);
	}

	public Substitution getSubstitutionAt(int k) {
		return substs.get(k);
	}

	public Set<Integer> getBlockedIndices() {
		return blockedIndices;
	}

	public int size() {
		return content.size();
	}
	
	public boolean isInBlockingPath() {
		return isInBlock;
	}
	
	public void add(VUnfoldStructure v, Substitution s) {
		content.add(v);
		substs.add(s);
		
		firstAtom = null;
	}
	
	public ArrayList<Atom> toConjunction() {
		if (firstAtom == null) {
			firstAtom = new ArrayList<Atom>();
			for (int i = 0; i < content.size(); i++) {
				firstAtom.add(content.get(i).getTopAtom().apply(substs.get(i)));
			}
		}
		
		return firstAtom;
	}

	public FunctionMap[] populate(FCTheory program, ArrayList<Atom> atoms, Counter newUsedBoundVarsCounter, Counter newUsedUnboundVarsCounter, ArrayList<VUnfoldStructure> backAtoms, ArrayList<Substitution> backSubsts, Map<Atom, VUnfoldStructure> tmpMap, ETAtomUnfolder eau) {
		
		FunctionMap[] cf = new FunctionMap[2];

		Set<Predicate> remainingPreds = new HashSet<>();
		
		for (int i = 0; i < atoms.size(); i++) {
			Atom cAtom = atoms.get(i);
			
			Set<Term> bTerms = new HashSet<Term>();
			for (Term v : cAtom.getTerms()) {
				if (!v.hasUnboundPrefix()) {
					bTerms.add(v);
				}
			}
			
			ProperRenamedAtom pra = new ProperRenamedAtom(cAtom, bTerms);
			Atom pAtom = pra.getRenamedAtom();

			Substitution sa = pra.getSubstitution().compose(backSubsts.get(backSubsts.size() - 1)); 

			if (contains(backAtoms, backSubsts, pAtom, pAtom.apply(sa))) {
				isInBlock = true;
				if (blockedIndices == null) {
					blockedIndices = new HashSet<Integer>();
				}
				blockedIndices.add(i);
			}

			VUnfoldStructure nvus = tmpMap.get(pAtom);

			if (nvus != null) {
				add(nvus, pra.getSubstitution());
			} else {
				nvus = eau.getFullyComputedVUS(pAtom);
					
				if (nvus != null && (eau.isDatalog() || eau.isUnfold())) {
					add(nvus, pra.getSubstitution());
					tmpMap.put(pAtom, nvus);
				} else {
					nvus = new VUnfoldStructure(findCommon);
					add(nvus, pra.getSubstitution());
				
					tmpMap.put(pAtom, nvus);
						
					ArrayList<Substitution> newBackSubsts = (ArrayList<Substitution>)backSubsts.clone();
					newBackSubsts.add(sa);

					nvus.unfold(program, pAtom, newUsedBoundVarsCounter, newUsedUnboundVarsCounter, backAtoms, newBackSubsts, tmpMap, eau);
				}
			}
	
			isInBlock |= nvus.isInBlockingPath();
			
			Term[] terms = cAtom.getArguments().getTerms();
			
			boolean used = false;
			for (int j = 0; j < terms.length; j++) {
				int j2 = (j == 0) ? 1 : 0;
				
				if (terms[j].equals(Rapid.dummyVars.get(j))) {
					if (cf[j] == null) {
						cf[j] = nvus.getFunctionMap(j).copy();
					} else {
						cf[j].intersection(nvus.getFunctionMap(j));
					}
					used = true;
				} else if (terms[j].equals(Rapid.dummyVars.get(j2))) {
					if (cf[j2] == null) {
						cf[j2] = nvus.getFunctionMap(j).copy();
					} else {
						cf[j2].intersection(nvus.getFunctionMap(j));
					}
					used = true;
				} 
			}

			if (!used) {
				remainingPreds.add(pAtom.getPredicate());
			}
		}

		if (remainingPreds.size() > 0) {
			for (int j = 0; j < 2; j++) {
				if (cf[j] != null) {
					cf[j].addPredicates(remainingPreds);
				}
			}
		}
		
		return cf;
	}
	
	public class CommonFunctionResult {
		public FunctionMap fm;
		public ArrayList<Atom> body;
		public Substitution subst;
		
		public CommonFunctionResult(FunctionMap fm, ArrayList<Atom> body, Substitution subst) {
			this.fm = fm;
			this.body = body;
			this.subst = subst;
		}
	}
	
	public CommonFunctionResult getCommonFunctions(Variable newVar) {
		ArrayList<Atom> firstAtoms = toConjunction();
		
		int[] pattern = AtomArray.termIndexPattern(firstAtoms, newVar);

		ArrayList<Atom> body = new ArrayList<Atom>();
		Substitution s = new Substitution();
		
		FunctionMap rf = null;

		for (int k = 0; k < pattern.length; k++) {
			Atom firstAtom = firstAtoms.get(k);
			if (pattern[k] == 0) {
				body.add(firstAtom);
				continue;
			}

			for (Variable v : firstAtom.getVariables()) {
				if (!v.hasUnboundPrefix() && !v.equals(newVar)) {
					s = substs.get(k);
				}
			}

			FunctionMap newFunctions = content.get(k).getFunctionMap(pattern[k] - 1);

			if (rf == null) {
				rf = newFunctions.copy();
			} else {
				rf.intersection(newFunctions);
			}
		}
		
		return new CommonFunctionResult(rf, body, s);
	}

	private boolean contains(ArrayList<VUnfoldStructure> list, ArrayList<Substitution> subst, Atom at1, Atom at2) {
		for (int i = 0; i < list.size(); i++) {
			Atom listTop = list.get(i).getTopAtom();
			
			if (at1.unboundEquals(listTop)) {
				if (!at2.unboundEquals(listTop.apply(subst.get(i)))) {
					list.get(i).addCycleId(cycle++);
				}
				
				return true;
			}
		}
		
		return false;
	}

	public String toString() {
		String s = "[" + (isInBlock ? "+":"") + (blockedIndices == null?"":blockedIndices) + " ";

		for (int i = 0 ; i < content.size(); i++) {
			VUnfoldStructure v = content.get(i);

			s += "<" + (substs.get(i) == null  || substs.get(i).isEmpty() ? "":substs.get(i) + "::") + v.getTopAtom() + "> ";
		}
		
		s += "] ";
		
		return s;
	}
	

	private void computespreds() {
		sa = new SortAtom();
		for (int i = 0; i < size(); i++) {
			sa.add(content.get(i).getTopAtom().getPredicate());
		}
	}
	
	public int compareTo(HUnfoldStructure hus) {
		if (sa == null) {
			computespreds();
		}
		if (hus.sa == null) {
			hus.computespreds();
		}
			
		return sa.compareTo(hus.sa);
	}

}
