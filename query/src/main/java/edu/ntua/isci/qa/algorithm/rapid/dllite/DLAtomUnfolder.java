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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.ntua.isci.common.lp.Atom;
import edu.ntua.isci.common.lp.Clause;
import edu.ntua.isci.common.lp.Predicate;
import edu.ntua.isci.common.lp.Substitution;
import edu.ntua.isci.common.lp.Term;
import edu.ntua.isci.common.lp.Variable;

import edu.ntua.isci.qa.algorithm.rapid.AtomUnfolder;
import edu.ntua.isci.qa.algorithm.rapid.AtomUnfoldingResult;
import edu.ntua.isci.qa.algorithm.rapid.FlattenerExtra;
import edu.ntua.isci.qa.algorithm.rapid.FunctionMap;
import edu.ntua.isci.qa.algorithm.rapid.ProperRenamedAtom;
import edu.ntua.isci.qa.algorithm.rapid.Rapid;
import edu.ntua.isci.qa.lp.theory.FCTheory;

public class DLAtomUnfolder extends AtomUnfolder {

	protected Map<Atom, DLAtomUnfolding> map;
	
	public DLAtomUnfolder(FCTheory lp) {
		super(lp);
		reset();
	}
	
	public void reset() {
		map = new HashMap<Atom, DLAtomUnfolding>();
	}
	
	public AtomUnfoldingResult getUnfolding(Atom atom, Collection<? extends Term> boundTerms, Atom head, FlattenerExtra extra) {
		ProperRenamedAtom pra = new ProperRenamedAtom(atom, boundTerms); 

		Atom searchAtom = pra.getRenamedAtom();
		DLAtomUnfolding unf = map.get(searchAtom);
		
		if (unf == null) {
			unf = unfold(searchAtom);
			map.put(searchAtom, unf);
		}		
		
		return new AtomUnfoldingResult(unf, pra.getSubstitution());
	}
	
	private DLAtomUnfolding unfold(Atom atom) {
		FunctionMap[] argFunctions = new FunctionMap[] { new FunctionMap(), new FunctionMap() };
		
		ArrayList<Atom> toCheck = new ArrayList<Atom>();
		toCheck.add(atom);

		ArrayList<Atom> res = new ArrayList<Atom>();
		res.add(atom);

    	int i = 0;
    	while (i < toCheck.size()) {
    		for (Atom newAtom : generateResolvents(toCheck.get(i++), argFunctions)) {
    			if (unboundCheckAdd(res, newAtom)) {
    				toCheck.add(newAtom);
    			}
    		}
    	}

    	return new DLAtomUnfolding(res.toArray(new Atom[] {}), argFunctions);
    }

	public AtomUnfoldingResult getUnfolding(Set<Atom> atoms, Collection<? extends Term> boundTerms, Atom head, FlattenerExtra extra) {
		if (atoms.size() == 1) {
			return getUnfolding(atoms.iterator().next(), boundTerms, head, extra);
		}

		FunctionMap[] targFunctions = new FunctionMap[] { new FunctionMap(), new FunctionMap() };		
		ArrayList<Atom> res = new ArrayList<Atom>(); 

		Substitution subst = new Substitution();
		
		// All atoms in atom should have the same arguments to work correctly!!!
		for (Atom atom : atoms) {
			ProperRenamedAtom pra = new ProperRenamedAtom(atom, boundTerms); 
	
			Atom searchAtom = pra.getRenamedAtom();
			DLAtomUnfolding unf = map.get(searchAtom);

			if (unf == null) {
				unf = unfold(searchAtom);
				map.put(searchAtom, unf);
			}

			for (Atom newAtom : unf.getUnfoldings()) {
				unboundCheckAdd(res, newAtom);
			}
			
			subst.add(pra.getSubstitution());
			
			targFunctions[0].union(unf.getFunctionMap(0));
			targFunctions[1].union(unf.getFunctionMap(1));
		}
		
		return new AtomUnfoldingResult(new DLAtomUnfolding(res.toArray(new Atom[] {}), targFunctions), subst);
	}
	
	public Atom[] datalogComputeUnfolding(Atom atom, Collection<? extends Term> boundTerms, Atom head, FlattenerExtra extra) {
		return ((DLAtomUnfolding)getUnfolding(atom, boundTerms, head, extra).getUnfolding()).getUnfoldings();
	}
	
	private boolean unboundCheckAdd(ArrayList<Atom> res, Atom newAtom) {
		
		Predicate pred = newAtom.getPredicate();
		int p = Atom.binarySearch(res, pred);
		if (p < 0) {
			res.add(-p - 1, newAtom);
			return true;
		} else {
			while (p < res.size() && res.get(p).getPredicate().equals(pred)) {
				Atom ex = res.get(p); 
				if (ex.unboundEquals(newAtom)) {
					int exU = 0;
					for (Variable v : ex.getVariables()) {
						if (v.getName().startsWith(Rapid.EXTRA_UNBOUND_PREFIX)) {
							exU++;
						}
					}

					int aU = 0;
					for (Variable v : newAtom.getVariables()) {
						if (v.getName().startsWith(Rapid.EXTRA_UNBOUND_PREFIX)) {
							aU++;
						}
					}

    				if (exU > aU) {
    					res.remove(p);
    					res.add(p, newAtom);
    				}
    				
    				return false;
				}
				
				p++;
			}
			
			res.add(p, newAtom);
			return true;
		}
	}
	
    private ArrayList<Atom> generateResolvents(Atom atom, FunctionMap[] argFunctions) {
    	ArrayList<Atom> result = new ArrayList<Atom>();
    	
   		for (Clause sideClause : program.getClauses(atom.getPredicate())) {
   			FunctionMap[] fb = AtomUnfolder.functionCollect(atom, sideClause);

   			if (fb != null) {
   				argFunctions[0].union(fb[0]);
   				argFunctions[1].union(fb[1]);
   			} else {
				Substitution s = Substitution.mgu(atom, sideClause.getHead());
				
				if (s != null) {
					Atom bodyAtom = sideClause.getBodyAtomAt(0);
	
					if (bodyAtom.getVariables().contains(Rapid.UNBOUND_VARIABLE)) {
						Set<Variable> sVars = bodyAtom.apply(s).getVariables();
	
						if (sVars.contains(Rapid.extraUnboundVars[0])) {
							s.put(Rapid.UNBOUND_VARIABLE, Rapid.extraUnboundVars[1]);
						} else {
							s.put(Rapid.UNBOUND_VARIABLE, Rapid.extraUnboundVars[0]);
						}
					}
					
			    	Atom newAtom = bodyAtom.apply(s);
			    	
			    	Set<Variable> vars = newAtom.getVariables();
	    	
			    	Substitution s2 = new Substitution();
			    	
			    	for (Variable v : vars) {
			    		if (!v.hasUnboundPrefix() && !Rapid.dummyVars.contains(v)) {
			    			if (vars.contains(Rapid.extraUnboundVars[0])) {
								s2.put(v, Rapid.extraUnboundVars[1]);
							} else {
								s2.put(v, Rapid.extraUnboundVars[0]);
							}
			    		}
			    	}
			    	
					result.add(newAtom.apply(s2));
				}
	   		}
   		}
        
        return result;
    }
}
