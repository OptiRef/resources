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

package edu.ntua.isci.common.lp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Substitution extends HashMap<Variable, Term> implements Cloneable {
	
	private static final long serialVersionUID = 1L;

	public Substitution() {
		super();
	}
	
	public boolean containsAll(Substitution subst) {
		for (Map.Entry<Variable, Term> pair : subst.entrySet()) {
			Variable v = pair.getKey();
			Term t = pair.getValue();
			
			Term ct = get(v);
			
			if (ct != null && ct.equals(t)) {
				continue;
			} else {
				return false;
			}
		}
		
		return true;
	}

	public Substitution removeAll(Substitution subst) {
		Substitution res = (Substitution)this.clone();
		
		for (Map.Entry<Variable, Term> pair : subst.entrySet()) {
			Variable v = pair.getKey();
			Term t = pair.getValue();
			
			Term ct = get(v);
			
			if (ct != null && ct.equals(t)) {
				res.remove(v);
			}
		}
		
		return res;
	}
	
	public boolean hasCycle() {
		Set<Variable> left = getVariables();
		
		for (Variable v : left) {
			Set<Variable> used = new HashSet<Variable>();
			used.add(v);
	
			Variable v1 = v;
			while (left.contains(v1)) {
				Term v2 = v1.apply(this);
				
				if (!v2.isVariable()) {
					break;
				}
				
				if (used.contains(v2)) {
					return true;
				}
				
				used.add((Variable)v2);
				v1 = (Variable)v2;
			}
		}
		
		return false;
	}
	
	public boolean add(Substitution newSubst) {
		
		for (Variable v : newSubst.getVariables()) {

			Term tOrig = this.get(v);			
			Term tNew = newSubst.get(v);
			
			if (tOrig != null && tNew != null && !tOrig.equals(tNew)) {
				return false;
			}
			
			if (tOrig == null && tNew != null) {
				this.put(v, tNew);		
			}
		}
		
		return true;
		
	}

	public void compact() {
		for (Iterator<Map.Entry<Variable, Term>> iter = entrySet().iterator(); iter.hasNext();) {
			Map.Entry<Variable, Term> entry = iter.next();
			if (entry.getKey().equals(entry.getValue())) {
				iter.remove();
			}
		}
	}
	
	public Term put(Variable var, Term t) {
		Term et = get(var);
			
		if (et != null) {
			if (!et.equals(t)) {
				return null;
			} else {
				return t;
			}
		} else {
			super.put(var, t);	
			return t;
		}
	}
		

	public Set<Variable> getVariables() {
		return keySet();
	}
	
	private static ArrayList<Term> disagreementSet(Tuple... tuples) {
		ArrayList<Term> res = new ArrayList<Term>();
		
		int s = tuples[0].size();

		for (int j = 0; j < s; j++) {
			Term t = tuples[0].getTerm(j);
			
			for (int i = 1; i < tuples.length; i++) {
				if (!tuples[i].getTerm(j).equals(t)) {
					res.add(tuples[i].getTerm(j).firstDifferenceTerm(t));
					for (int k = 1; k < tuples.length; k++) {
						Term d = t.firstDifferenceTerm(tuples[k].getTerm(j));
						if (d != null && !res.contains(d)) {
							res.add(d);
						}
					}
					
					return res;
				}
			}
		}
		
		return res;
			
	}
	
	public Substitution compose(Substitution s2) {
		Substitution res = new Substitution();
		
		for (Map.Entry<Variable, Term> s : entrySet()) {
			Variable v = s.getKey();
			Term t = s.getValue().apply(s2);
			if (!t.equals(v)) {
				res.put(v, t);
			}
		}

		Set<Variable> vars = getVariables();
		
		for (Map.Entry<Variable, Term> s : s2.entrySet()) {
			Variable v = s.getKey();
			Term t = s.getValue();
			if (!vars.contains(v)) {
				res.put(v, t);
			}
		}
		
		return res;
		
	}

	public static Substitution mgu(Atom... atoms) {
		
		Predicate p = atoms[0].getPredicate();
		
		Tuple[] tuples = new Tuple[atoms.length];
		
		tuples[0] = atoms[0].getArguments();
		
		for (int i = 1 ; i < atoms.length; i++) {
			if (!atoms[i].getPredicate().equals(p)) {
				return null;
			} else {
				tuples[i] = atoms[i].getArguments();
			}
		}
		
		return mgu(tuples);
	
	}
	
	public Substitution restrict(Set<Variable> vars) {
		Substitution res = new Substitution();
		
		for (Map.Entry<Variable, Term> entry : entrySet()) {
			if (vars.contains(entry.getKey())) {
				res.put(entry.getKey(), entry.getValue());
			}
		}
		
		return res;
		
	}

	public boolean isRenaming(Set<Variable> tvars) {

		Substitution rs = restrict(tvars);
		
		Set<Variable> ys = new HashSet<Variable>();
		
		for (Term term : rs.values()) {
			if (!term.isVariable() || ys.contains(term)) {
				return false;
			}
			 
			ys.add((Variable)term);
		}
		
		Set<Variable> xs = rs.keySet();
		Set<Variable> es = new HashSet<Variable>();
		es.addAll(tvars);

		if (!es.containsAll(xs)) {
			return false;
		}
		
		es.removeAll(xs);
		
		int s = ys.size();
		ys.removeAll(es);
		
		if (ys.size() != s) {
			return false;
		}
		
		return true;
		
	}
	
	public static Substitution mgu(Tuple... tuples) {

		for (int i = 0; i < tuples.length; i++) {
			if (tuples[i].size() != tuples[0].size()) {
				return null;
			}
		}
		
		Substitution subst = new Substitution();

		Tuple[] sTuples = new Tuple[tuples.length];
		
		for (;;) {
			
			for (int i = 0; i < tuples.length; i++) {
				sTuples[i] = tuples[i].apply(subst);
			}
			
			ArrayList<Term> ds = disagreementSet(sTuples);
		
			if (ds.isEmpty()) {
				return subst;
			}
			
			boolean unify = false;

			loop:
			for (int i = ds.size() - 1; i >= 0; i--) {
				Term t1 = ds.get(i);
				
				for (int j = ds.size() - 1; j >=0 ; j--) {
					if (i == j) {
						continue;
					}
					
					Term t2 = ds.get(j);
					
					if (t1.isVariable() && !t2.getVariables().contains(t1)) {
						Substitution s2 = new Substitution();
						s2.put((Variable)t1, t2);
						subst = subst.compose(s2);
						unify = true;
						break loop;
					}
				}
			}
			
			if (!unify) {
				return null;
			}
			
		}
	}
	
	public void remove(Variable v, Term t) {
		Term nt = get(v); 
		if (nt != null && nt.equals(t)) {
			remove(v);
		}
	}

	public String toString() {
		String s = "";
		
		for (Map.Entry<Variable, Term> entry : entrySet()) {
			if (s.length() != 0) {
				s += ", ";
			}
			s += entry.getKey().toString() + "/" + entry.getValue().toString();
		}
		
		return "{" + s + "}";
	}

}