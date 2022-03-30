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

package edu.ntua.isci.qa.algorithm.rapid;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import edu.ntua.isci.qa.graph.DirectedEdge;
import edu.ntua.isci.qa.lp.theory.FCTheory;
import edu.ntua.isci.common.lp.Atom;
import edu.ntua.isci.common.lp.Clause;
import edu.ntua.isci.common.lp.Predicate;
import edu.ntua.isci.common.lp.Substitution;
import edu.ntua.isci.common.lp.Term;
import edu.ntua.isci.common.lp.Variable;
import edu.ntua.isci.common.utils.Counter;
import edu.ntua.isci.common.utils.SetUtils;

public abstract class ClauseUnfolder {

	protected FCTheory program;
	protected AtomUnfolder au;

	protected Atom head;
	protected IndexEntryList[] atomList; 
	protected ArrayList<UnfoldedClause> result;

	protected Set<Term> boundTerms;
	protected Set<Variable> headVars;
	protected Set<Variable> boundVars;
	
	protected Set<DirectedEdge<Term>> edges; 
	protected ArrayList<Substitution>[] sList;
	protected Set<Atom>[] topAtoms;

	protected boolean isSymmetric;
	
	protected Variable[][] unboundVarMap;
	protected Set<Variable> unboundVars;

	protected ClauseUnfolder(FCTheory program, AtomUnfolder au) {
		this.program = program;
		this.au = au;
	}
	
	public abstract ArrayList<UnfoldedClause> unfold(Clause c, FlattenerExtra extra);
	
	public abstract ArrayList<UnfoldedClause> unfold(Clause c, int eIndex, Set<Atom> extraAtoms, FlattenerExtra extra);
	
	protected abstract IndexEntryList[] createAtomList(Set<Atom>[] topAtoms, Set<Term> boundTerms, FlattenerExtra extra);
	
	public abstract int effectiveTopIndex(int k);
	
	public AtomUnfolder getAtomUnfolder() {
		return au;
	}
	
	protected void prepare(Clause clause, int eIndex, Set<Atom> extraAtoms, FlattenerExtra extra) {
		int size = clause.bodySize();
		
		head = clause.getHead();
		headVars = head.getVariables();

		unboundVars = clause.getUnboundVariables();
		
		boundVars = clause.getBoundVariables();
		boundTerms = clause.getBoundTerms();

		unboundVarMap = new Variable[clause.bodySize()][2];

		for (int i = 0; i < size; i++) {
			Term[] terms = clause.getBodyAtomAt(i).getArguments().getTerms();
			for (int j = 0; j < terms.length; j++) {
				if (unboundVars.contains(terms[j])) {
					unboundVarMap[i][j] = (Variable)terms[j];
				}
			}
		}

		Substitution subst = Rapid.replaceUnboundVariablesByUnderscoreSubstitution(clause);
		
		topAtoms = new Set[size];
		for (int i = 0; i < topAtoms.length; i++) {
			topAtoms[i] = new HashSet<Atom>();
			topAtoms[i].add(clause.getBodyAtomAt(i).apply(subst));
			
			if (i == eIndex && extraAtoms != null) {
				for (Atom a : extraAtoms) {
					topAtoms[i].add(a.apply(subst));
				}
			}
		}
		
		atomList = createAtomList(topAtoms, boundTerms, extra);

		edges = new HashSet<DirectedEdge<Term>>();

		for (int ti = 0; ti < topAtoms.length; ti++) {
			int i = effectiveTopIndex(ti);
			
			for (IndexEntry ie : atomList[i].getList()) {
				Atom a = ie.getAtom();
				if (a.isRole() && boundTerms.containsAll(a.getTerms())) {
					edges.add(new DirectedEdge<Term>(a.getArgument(0), a.getArgument(1)));
				}
			}
			
			atomList[i].markTopAtoms(topAtoms[ti]);
		}
		
		crossCheck();


		isSymmetric = join(new Substitution(), 0, new HashSet<Variable>());

		result = new ArrayList<UnfoldedClause>();
	}

	private Set<DirectedEdge<Term>> transformEdges(Substitution subst) {
		Set<DirectedEdge<Term>> newEdges = new HashSet<DirectedEdge<Term>>();
		for (DirectedEdge<Term> e : edges) {
			newEdges.add(e.apply(subst));
		}
		return newEdges;
	}
	
	protected boolean join(Substitution subst, int k, Set<Variable> fixed) {
		
		if (k == sList.length) {
			if (subst.size() > 0 && edges.containsAll(transformEdges(subst))) {
				return true;
			} else {
				return false;
			}
		}

		for (int i = 0; i < sList[k].size(); i++) {
			Substitution newSubst = (Substitution)subst.clone();
			if (SetUtils.intersection(fixed, sList[k].get(i).getVariables()).isEmpty() && newSubst.add(sList[k].get(i))) {
				if (join(newSubst, k + 1, fixed)) {
					return true;
				}
			} 
		}

		Set<Variable> newFixed = (Set)((HashSet<Variable>)fixed).clone();
		newFixed.addAll(SetUtils.intersection(atomList[effectiveTopIndex(k)].getFirst().iterator().next().getAtom().getVariables(), boundVars));
		
		if (SetUtils.intersection(newFixed, subst.getVariables()).isEmpty()) {
			if (join(subst, k + 1, newFixed)) {
				return true;
			}
		}
		
		return false;
	}

	protected void crossCheck() {
		int size = topAtoms.length;
		
		sList = new ArrayList[size];
		
		for (int ti = 0; ti < size; ti++) {
			int i = effectiveTopIndex(ti);
			
			sList[ti] = new ArrayList<Substitution>();

			for (int j = 0; j < atomList.length; j++) {
				ArrayList<IndexEntry> jList = atomList[j].getList();
				
				for (int k = 0; k < atomList[i].getList().size(); k++) {
					
					Atom a1 = atomList[i].getIndexEntryAt(k).getAtom();
					Predicate pred = a1.getPredicate();

					int pos = atomList[j].binarySearch(a1.getPredicate());
					if (pos < 0) {
						continue;
					} else {
						while (pos < jList.size() && jList.get(pos).getAtom().getPredicate().equals(pred)) {
							Atom a2 = jList.get(pos).getAtom();  
							
							if (a1.getPredicate().equals(a2.getPredicate())) {
								
								boolean block = false;
								for (int ii = 0; ii < a1.getArguments().size(); ii++) {
									Term t1 = a1.getArgument(ii);
									Term t2 = a2.getArgument(ii);
									
									if ((headVars.contains(t1) && !t1.equals(t2))) {
										block = true;
										break;
									}
								}
								
								if (!block) {
									Substitution s1 = new Substitution();

									for (int ii = 0; ii < a1.getArguments().size(); ii++) {
										Term t1 = a1.getArgument(ii);
										
										if (t1.isVariable()) {
											Term t2 = a2.getArgument(ii);
	
											if (boundTerms.contains(t1) && boundTerms.contains(t2)) {
												Term t = s1.put((Variable)t1, t2);
//												System.out.println(ti + " " + a1 + " " + a2 + " " + s1 + " " + t);
												if (t == null) {
													s1.clear();
													break;
												}
											}
										}
									}
									
									s1.compact();

									if (!s1.isEmpty() && !sList[ti].contains(s1)) {
										sList[ti].add(s1);
									}
								}
							}

							pos++;
						}
					}
				}
			}
		}
	}

	
	public UnfoldedClause createClause(ArrayList<Point> index, boolean pure) {
		ArrayList<Atom> body = new ArrayList<Atom>();
		
		Counter cc = new Counter(0);
		
		boolean isLastClauseTop = true;
		for (Point p : index) {
			if (p == null) {
				continue;
			}
			
			IndexEntry aie = p.getIndexEntry(atomList);
			
			int j = p.getColumn();
			
			if (atomList[j].getFirst() != aie) {
				isLastClauseTop = false;
			}

			Substitution s = new Substitution();

			for (Variable v : aie.getAtom().getVariables()) {
				if (v.hasUnboundPrefix()) {
					if (v.equals(Rapid.fixedUnboundVars[0])) {
						s.put(v, unboundVarMap[j][0]);
					} else if (v.equals(Rapid.fixedUnboundVars[1])) {
						s.put(v, unboundVarMap[j][1]);
					} else {
						s.put(v, Variable.getNewVariable(Variable.UNBOUND_PREFIX, unboundVars, cc));
					}
				}
			}

			body.add(aie.getAtom().apply(s));
		}
		
		return new UnfoldedClause(new Clause(head, body), pure, isLastClauseTop);
	}
	

	protected class Found implements Comparable<Found> {
		public int pos;
		public int type;
		public Atom atom;
		
		private boolean pure;
		
		public Found(int pos, int type, Atom atom) {
			this(pos, type, atom, true);
		}

		public Found(int pos, int type, Atom atom, boolean pure) {
			this.pos = pos;
			this.type = type;
			this.atom = atom;
			this.pure = pure;
		}
		
		public boolean isPure() {
			return pure;
		}

		public int compareTo(Found o) {
			if (type < o.type) {
				return -1;
			} else if (type > o.type) {
				return 1;
			} else {
				return 0;
			}
		}
	}
}
