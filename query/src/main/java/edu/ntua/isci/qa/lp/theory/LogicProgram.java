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

package edu.ntua.isci.qa.lp.theory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.ntua.isci.qa.algorithm.rapid.FunctionMap;
import edu.ntua.isci.qa.algorithm.rapid.SortAtom;

import edu.ntua.isci.common.lp.Atom;
import edu.ntua.isci.common.lp.Clause;
import edu.ntua.isci.common.lp.Function;
import edu.ntua.isci.common.lp.FunctionalTerm;
import edu.ntua.isci.common.lp.Predicate;
import edu.ntua.isci.common.lp.Term;


public class LogicProgram {

	protected ArrayList<Clause> rules;
	protected Map<Predicate, ArrayList<Clause>> ruleIndex;
	protected Map<Predicate, ArrayList<Clause>> iruleIndex;
	
	protected Map<Function, Set<Clause>> fIndex;
	public Map<Predicate, FunctionMap[]> pIndex;
	
	private ArrayList<Clause> eqHeadRules;

	public LogicProgram() {
		rules = new ArrayList<Clause>();
		ruleIndex = new HashMap<Predicate, ArrayList<Clause>>();
		iruleIndex = new HashMap<Predicate, ArrayList<Clause>>();
		
		fIndex = new HashMap<>();
		pIndex = new HashMap<>();
		
		eqHeadRules = new ArrayList<>();
	}
	
	public LogicProgram(Collection<Clause> set) {		
		this();
		
		for (Clause c : set) {
			addClause(c);
		}
	}
	
	public int size() {
		return rules.size();
	}
	
	public ArrayList<Clause> getEqualityHeadClauses() {
		return eqHeadRules;
	}
	
	public boolean addClause(Clause r) {
		
		if (!rules.contains(r)) {
			rules.add(r);
			
			if (r.getHead().isEqualityAtom()) {
				eqHeadRules.add(r);	
			}
			
			ArrayList<Clause> clauses = ruleIndex.get(r.getHead().getPredicate());
			if (clauses == null) {
				clauses = new ArrayList<Clause>();
				ruleIndex.put(r.getHead().getPredicate(), clauses);
			}
			clauses.add(r);

			for (Atom a : r.getBody()) {
				ArrayList<Clause> iclauses = iruleIndex.get(a.getPredicate());
				if (iclauses == null) {
					iclauses = new ArrayList<Clause>();
					iruleIndex.put(a.getPredicate(), iclauses);
				}
				iclauses.add(r);
			}
			
			Atom head = r.getHead(); 
			if (!r.isFunctionFree()) {
				for (int i = 0; i < head.getPredicate().getArity(); i++) {
					Term t = r.getHead().getArgument(i);
					
					if (t.isFunctionalTerm()) {
						Function f = ((FunctionalTerm)t).getFunction();
						Set<Clause> s = fIndex.get(f);
						if (s == null) {
							s = new HashSet<>();
							fIndex.put(f, s);
						}
						s.add(r);
						
						Predicate p = head.getPredicate();
						FunctionMap[] fm = pIndex.get(p);
						if (fm == null) {
							fm = new FunctionMap[] { new FunctionMap(), new FunctionMap() };
							pIndex.put(p, fm);
						}
						
						FunctionMap nf = new FunctionMap();
						ArrayList<SortAtom> asa = new ArrayList<>();
						asa.add(SortAtom.createSortAtom(r.getBody()));
						nf.put(f, asa);
						
						fm[i].union(fm);
					}
				}
			}
			
			return true;
		} else {
			return false;
		}
		
	}

	public ArrayList<Clause> getClauses() {
		ArrayList<Clause> clauses = new ArrayList<Clause>();
		clauses.addAll(rules);
		
		return clauses;
	}

	public ArrayList<Clause> getSortedClauses() {
		
		Predicate[] preds = new Predicate[ruleIndex.size()];
		int i = 0;
		for (Predicate p : ruleIndex.keySet()) {
			preds[i++] = p;
		}
		
		Arrays.sort(preds);
		
		ArrayList<Clause> res = new ArrayList<>();
		for (Predicate p : preds) {
			res.addAll(ruleIndex.get(p));
		}
		
		return res;
	}
	
	public ArrayList<Clause> getClauses(Predicate pred) {
		ArrayList<Clause> res = ruleIndex.get(pred);
		if (res == null) {
			return new ArrayList<Clause>();
		} else {
			return res;
		}
	}	
	
	public FunctionMap[] getFunctionMap(Predicate p) {
		return pIndex.get(p);
	}
	
	public Set<Clause> getClausesForFunction(Function f) {
		return fIndex.get(f);
	}
	
	public ArrayList<Clause> getClausesByBodyPredicate(Predicate pred) {
		ArrayList<Clause> res = iruleIndex.get(pred);
		if (res == null) {
			return new ArrayList<Clause>();
		} else {
			return res;
		}
	}

	
	public ArrayList<Clause> getGraphDependencyProgram(Set<Predicate> pred) {
		Set<Predicate> used = new HashSet<Predicate>();
		ArrayList<Predicate> queue = new ArrayList<Predicate>();
		Set<Predicate> reachablePredicates = new HashSet<Predicate>();
		
		int i = 0;
		queue.addAll(pred);
		used.addAll(pred);
		
		while (i < queue.size()){
			Predicate p = queue.get(i++);

			if (reachablePredicates.add(p)){
				for (Predicate newPred : getDirectReachablePredicates(p)) {
					if (used.add(newPred)) {
						queue.add(newPred);
					}
				}
			}
		}
		
		ArrayList<Clause> result = new ArrayList<Clause>();
		
		Set<Predicate> tmp = new HashSet<>(ruleIndex.keySet());
		tmp.removeAll(reachablePredicates);
		
		for (Predicate p : reachablePredicates) {
			ArrayList<Clause> cl = ruleIndex.get(p);
			if (cl != null) {
				result.addAll(cl);
			}
		}
		
		return result;
	}	
	
	private Set<Predicate> getDirectReachablePredicates(Predicate p) {
		Set<Predicate> res = new HashSet<Predicate>();
		
		ArrayList<Clause> cl = ruleIndex.get(p);
		if (cl != null) {
			for (Clause r : cl) {
				for (Atom t : r.getBody()) {
					res.add(t.getPredicate());
				}
			}
		}
		
		return res;
	}
	

	public String toString() {
		String  s = "";

		int i = 0;
		for (Clause r : rules) {
			s += i++ + ": " + r.toString() + "\n";
		}
		
		return s;
	}
}
