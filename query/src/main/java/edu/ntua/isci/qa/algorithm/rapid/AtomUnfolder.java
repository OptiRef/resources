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
import java.util.Collection;
import java.util.Set;

import edu.ntua.isci.common.lp.Atom;
import edu.ntua.isci.common.lp.Clause;
import edu.ntua.isci.common.lp.FunctionalTerm;
import edu.ntua.isci.common.lp.Predicate;
import edu.ntua.isci.common.lp.Term;

import edu.ntua.isci.qa.lp.theory.FCTheory;

public abstract class AtomUnfolder {

	protected FCTheory program;
	protected Predicate queryPredicate;
	
	protected AtomUnfolder(FCTheory program) {
		this.program = program;
	}
	
	public abstract void reset();
	
	public abstract AtomUnfoldingResult getUnfolding(Atom atom, Collection<? extends Term> boundTerms, Atom head, FlattenerExtra extra);

	public abstract AtomUnfoldingResult getUnfolding(Set<Atom> atom, Collection<? extends Term> boundTerms, Atom head, FlattenerExtra extra);

	public void setQueryPredicate(Predicate qp) {
		this.queryPredicate = qp;
	}
	
	public static FunctionMap[] functionCollect(Atom atom, Clause sideClause) {
		if (!sideClause.isFunctionFree()) {
			FunctionMap[] fm = null;
			
			Term[] sideTuple = sideClause.getHead().getArguments().getTerms();
			Term[] atomTuple = atom.getArguments().getTerms();

			ArrayList<SortAtom> sa = new ArrayList<>();
			sa.add(SortAtom.createSortAtom(sideClause.getBody()));

			if (atom.isRole()) {
				for (int i = 0; i < 2 ; i++) {
					int i2 = i == 0 ? 1 : 0;
				
					if (sideTuple[i].isFunctionalTerm() && !atomTuple[i].hasUnboundPrefix()) {
						fm = new FunctionMap[] { new FunctionMap(), new FunctionMap() };
						
						if (atomTuple[i].equals(Rapid.dummyVars.get(i))) {
							fm[i].put(((FunctionalTerm)sideTuple[i]).getFunction(), sa);
						} else {
							fm[i2].put(((FunctionalTerm)sideTuple[i]).getFunction(), sa);
						}
						break;
					}
				}
			} else {
				if (!atomTuple[0].hasUnboundPrefix()) {
					fm = new FunctionMap[] { new FunctionMap(), new FunctionMap() };
					
					if (atomTuple[0].equals(Rapid.dummyVars.get(0))) {
						fm[0].put(((FunctionalTerm)sideTuple[0]).getFunction(), sa);
					}
				}
			}
			
			return fm;
		}
		
		return null;
	}

	
	public abstract Atom[] datalogComputeUnfolding(Atom atom, Collection<? extends Term> boundTerms, Atom head, FlattenerExtra extra);
	
	
	protected boolean preinit = false;
	
    public void precompute(Collection<Clause> atomClauses) {
    	preinit = true;
    	for (Clause c : atomClauses) {
    		getUnfolding(c.getBodyAtomAt(0), c.getHead().getVariables(), c.getHead(), new FlattenerExtra());
    	}
    }
    
    public boolean isPrecomputed() {
    	return preinit;
    }

}
