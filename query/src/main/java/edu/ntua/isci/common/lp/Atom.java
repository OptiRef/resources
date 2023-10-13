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
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class Atom implements Cloneable, Comparable<Atom> {
	
	protected Predicate pred;
	protected Tuple args;
	
	public static Atom TRUTH_ATOM = new Atom(Predicate.TRUTH_PREDICATE);
	public static Atom FALSE_ATOM = new Atom(Predicate.FALSE_PREDICATE);

	public Atom(Predicate pred, Term... args) {
		this(pred, new Tuple(args));
	}
	
	public Atom(Predicate pred, Tuple args) {
		this.pred = pred;
		this.args = args;
		
	
		if (pred.equals(Predicate.EQUALITY_PREDICATE)) {
			Term[] t = args.getTerms();
			if (t[0].isFunctionalTerm() && !t[1].isFunctionalTerm()) {
				this.args = new Tuple(t[1], t[0]);
			} else if (t[0].isFunctionalTerm() && t[1].isFunctionalTerm()) {
				FunctionalTerm f0 = (FunctionalTerm)t[0]; 
				FunctionalTerm f1 = (FunctionalTerm)t[1];
				
				int c = f0.getFunction().compareTo(f1.getFunction());
				if (c <= 0) {
					this.args = new Tuple(t[0], t[1]);
				} else {
					this.args = new Tuple(t[1], t[0]);
				}
			}
		}
	}
	
	public boolean containsTerm(Term t){
		for (Term x : args.getTerms()){
			if (t.equals(x)) {
				return true;
			}
		}
		
		return false;
	}
	
	public boolean isRole() {
		return getPredicate().isRole();
	}

	public boolean isConcept() {
		return getPredicate().isConcept();
	}
	
	public boolean isEqualityAtom() {
		return getPredicate().getName().startsWith(Predicate.EQUAL_PREDICATE_NAME);
	}
	
	public Predicate getPredicate() {
		return pred;
	}
	
	public Tuple getArguments() {
		return args;
	}	

	public Term getArgument(int i) {
		return args.getTerm(i);
	}	
	
	public Atom apply(Substitution subst) {
		return new Atom(pred, args.apply(subst));
	}

	public Set<Variable> getVariables() {
		return 	args.getVariables();
	}	
	
	public Set<Term> getTerms() {
		Set<Term> res = new HashSet<Term>();
		
		for (int i = 0; i < args.size(); i++) {
			res.add(args.getTerm(i));
		}
		
		return res;
	}
	
	public int depth() {
    	return args.depth();
    }
	
	public Object clone() {
		return new Atom(pred, (Tuple)args.clone());
	}

	public int compareTo(Atom a) {
		int nc = pred.compareTo(a.getPredicate());
		if (nc != 0) {
			return nc;
		} else {
			return getArguments().compareTo(a.getArguments());
		}
    }
	
	public boolean isFunctionFree() {
		for (int i = 0; i < args.size(); i++) {
			if (args.getTerm(i).isFunctionalTerm()) {
				return false;
			}
		}
		
		return true;
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof Atom)) {
			return false;
		} else {
			return pred.equals(((Atom)obj).getPredicate()) && args.equals(((Atom)obj).getArguments());
		}
	}
	
	public boolean unboundEquals(Object obj) {
		if (!(obj instanceof Atom)) {
			return false;
		} else {
			return pred.equals(((Atom)obj).getPredicate()) && args.unboundEquals(((Atom)obj).getArguments());
		}
	}		
	
	public int hashCode() {
		if (pred != null && args != null) {
			return pred.hashCode() + args.hashCode();
		} else {
			return 0;
		}
	}
	
	public String toString() {
		return pred.toString() + args.toString();
	}
    
    public static class PredicateComparator implements Comparator<Atom> {
    	private boolean cs;
    	
    	public PredicateComparator() {
    		this.cs = true;
    	}
    	
    	public PredicateComparator(boolean cs) {
    		this.cs = cs;
    	}
    	
    	public int compare(Atom a1, Atom a2) {
    		if (cs) {
    			return a1.getPredicate().compareTo(a2.getPredicate());
    		} else {
    			return a1.getPredicate().getName().compareToIgnoreCase(a2.getPredicate().getName());
    		}
    	}
    }

    public static Substitution matchAtoms(Atom a1, Atom a2, Substitution subst) {
        
    	boolean substitutionCopied = false;
        
        for (int i = 0; i < a1.getPredicate().getArity(); i++) {
            Term t1 = a1.getArgument(i);
            Term t2 = a2.getArgument(i);
            
            if (t1.isVariable() && subst.get((Variable)t1) == null) {
                if (!substitutionCopied) {
                    subst = (Substitution)subst.clone();
                    substitutionCopied = true;
                }
                subst.put((Variable)t1, t2);
            }
            
            if (!t1.apply(subst).equals(t2)) {
                return null;
            }
        }
        
        return subst;
    }
    
	public static class Subsumee {
		public Atom subsumee;
		public int mode;
		
		public Subsumee(Atom subsumee, int mode) {
			this.subsumee = subsumee;
			this.mode = mode;
		}
		
		public String toString() {
			return mode + " : " + subsumee;
		}
	}
    
	public static Subsumee exactSubsumes(Atom r1, Atom r2, Set<Term> boundTerms) {

		if (!r1.getPredicate().equals(r2.getPredicate())) {
			return null;
		}			

		boolean s1 = exactMatchAtoms(r1, r2, new Substitution(), boundTerms) != null;
		boolean s2 = exactMatchAtoms(r2, r1, new Substitution(), boundTerms) != null;
		
		if (s1 && s2) {
			return new Subsumee(r1, 0);
		} else if (s1) {
			return new Subsumee(r2, 1);
		} else if (s2) {
			return new Subsumee(r1, 2);
		} else {
			return null;
		}
	}
	
    public static Substitution exactMatchAtoms(Atom a1, Atom a2, Substitution subst, Set<Term> boundTerms) {
        
    	Substitution res = (Substitution)subst.clone();
        
        for (int i = 0; i < a1.getPredicate().getArity(); i++) {
            Term t1 = a1.getArgument(i);
            Term t2 = a2.getArgument(i);
            
            if (t1.isVariable() && !boundTerms.contains(t1) && res.get((Variable)t1) == null) {
            	res.put((Variable)t1, t2);
            }
            
            if (!t1.apply(res).equals(t2)) {
                return null;
            }
        }
        
        return res;
    }
    
    public static int binarySearch(ArrayList<Atom> atomList, Predicate key) {
		int low = 0;
		int high = atomList.size() - 1;
		
		while (low <= high) {
			int mid = (low + high) >>> 1;
			
			int cmp = atomList.get(mid).getPredicate().compareTo(key); 
			
			if (cmp < 0) {
				low = mid + 1;
			} else if (cmp > 0) {
				high = mid - 1;
			} else {
				while (mid > 0 && atomList.get(mid - 1).getPredicate().equals(key)) {
					mid--;
				}
				
				return mid;
			}
		}
		
		return -(low + 1);
	}
    

	public static boolean unboundAdd(ArrayList<Atom> sortedList, Atom atom) {
		
		Predicate pred = atom.getPredicate();
		
		int p = Atom.binarySearch(sortedList, pred);
		if (p < 0) {
			sortedList.add(-p - 1, atom);
			return true;
		} else {
			while (p < sortedList.size() && sortedList.get(p).getPredicate().equals(pred)) {
				Atom ex = sortedList.get(p); 
				if (ex.unboundEquals(atom)) {
    				return false;
				}
				
				p++;
			}
			
			sortedList.add(p, atom);
			return true;
		}
	}
}