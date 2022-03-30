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
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.ntua.isci.common.utils.Counter;

public class AtomArray {

	public static ArrayList<Atom> create(Atom atom) { 
		ArrayList<Atom> res = new ArrayList<Atom>();
		res.add(atom);
		
		return res;
	}

	public static ArrayList<Atom> apply(ArrayList<Atom> atoms, Substitution subst) {
		ArrayList<Atom> res = new ArrayList<Atom>();
		
		for (Atom a : atoms) {
			Atom newAtom = a.apply(subst);
			if (!res.contains(newAtom)) {
				res.add(newAtom);
			}
		}
		
		return res;
	}

	public static Set<Variable> getVariables(Collection<Atom> atoms) {
		Set<Variable> res = new HashSet<Variable>();
		
		for (Atom a : atoms) {
			res.addAll(a.getVariables());
		}
		
		return res;
	}

	public static Set<Term> getTerms(Collection<Atom> atoms) {
		Set<Term> res = new HashSet<Term>();
		
		for (Atom a : atoms) {
			res.addAll(a.getTerms());
		}
		
		return res;
	}
	
	public static Map<Variable, Counter> getCountedVariables(ArrayList<Atom> atoms) {
		Map<Variable, Counter> cc = new HashMap<Variable, Counter>();

		Set<Atom> bodyAtoms = new HashSet<Atom>();
			
		for (Atom atom : atoms) {
			if (!bodyAtoms.add(atom)) {
				continue;
			}
				
			for (Term t : atom.getArguments().getTerms()) {
				if (t.isVariable()) {
					Counter c = cc.get((Variable)t);
					if (c == null) {
						c = new Counter(1);
						cc.put((Variable)t, c);
					} else {
						c.increase();
					}
				}
			}
		}
			
		return cc;

	}	

    public static Set<Atom> getAtomsForTerm(Collection<Atom> atoms, Term t) {
    	Set<Atom> res = new HashSet<Atom>(); 
    	for (Atom a : atoms) {
    		if (a.getTerms().contains(t)) {
    			res.add(a);
    		}
    	}
    	
    	return res;
    }
    
    public static int[] termIndexPattern(ArrayList<Atom> atoms, Term t) {
    	int[] res = new int[atoms.size()];
    	Arrays.fill(res, 0);
    	
    	for (int i = 0; i < res.length; i++) {
    		Term[] terms = atoms.get(i).getArguments().getTerms();
    		
    		for (int j = 0; j < terms.length; j++) {
    			if (terms[j].equals(t)) {
    				res[i] += (j + 1);
    			}
    	 	}
    	}
    	
    	return res;
    }
    

	public static class Subsumee {
		public ArrayList<Atom> subsumee;
		public int mode;
		
		public Subsumee(ArrayList<Atom> subsumee, int mode) {
			this.subsumee = subsumee;
			this.mode = mode;
		}
	}

    public static Map<Predicate, AtomIndexNode> getAtomIndex(ArrayList<Atom> r) {
        Map<Predicate, AtomIndexNode> atomIndex = new HashMap<Predicate, AtomIndexNode>();
        
        for (Atom atom : r) {
        	atomIndex.put(atom.getPredicate(), new AtomIndexNode(atom, atomIndex.get(atom.getPredicate())));
        }
        
        return atomIndex;
    }	
    
    public static class AtomIndexNode {
        public final Atom atom;
        public final AtomIndexNode next;
        public boolean matched;

        public AtomIndexNode(Atom atom, AtomIndexNode next) {
            this.atom = atom;
            this.next = next;
            this.matched = false;
        }
        
        public String toString() {
        	return atom + (next == null ? "": ", " + next.toString());
        }
    }

    public static class PredicateComparator implements Comparator<ArrayList<Atom>> {

		public int compare(ArrayList<Atom> a1, ArrayList<Atom> a2) {
			int s1 = a1.size();
			int s2 = a2.size();
			
			if (s1 == 1 && s2 == 1) {
				return a1.get(0).getPredicate().compareTo(a2.get(0).getPredicate());
			} else {
				if (s1 < s2) {
					return -1;
				} else if (s1 > s2) {
					return 1;
				} else {
					return 0;
				}
			}
		}
    }
	
}
