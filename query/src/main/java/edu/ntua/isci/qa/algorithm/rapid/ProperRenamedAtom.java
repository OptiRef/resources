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

import java.util.Collection;

import edu.ntua.isci.common.lp.Atom;
import edu.ntua.isci.common.lp.Predicate;
import edu.ntua.isci.common.lp.Substitution;
import edu.ntua.isci.common.lp.Term;
import edu.ntua.isci.common.lp.Variable;

public class ProperRenamedAtom {
	private Substitution subst;
	private Atom renamedAtom;
	
	public ProperRenamedAtom(Atom atom, Collection<? extends Term> boundTerms) {
		Term[] terms = atom.getArguments().getTerms();
		
		boolean eq = false;
		if (atom.getPredicate().getName().startsWith(Predicate.EQUAL_PREDICATE_NAME)) {
			eq = true;
		}
		
		Variable[] tuple = new Variable[atom.getPredicate().getArity()];
		subst = new Substitution();
	
		for (int i = 0; i < tuple.length; i++) {
			if (boundTerms.contains(terms[i])) {
				if (!eq) { 
					tuple[i] = (Variable)Rapid.dummyVars.get(i);
				} else {
					tuple[i] = (Variable)Rapid.joinDummyVars.get(i);
				}
			} else {
				tuple[i] = Rapid.fixedUnboundVars[i];
			}
			
			if (!tuple[i].equals(terms[i])) {
				subst.put(tuple[i], terms[i]);
			}
		}
		
		renamedAtom = new Atom(atom.getPredicate(), tuple);
	}
	
	public Atom getRenamedAtom() {
		return renamedAtom;
	}
	
	public Substitution getSubstitution() {
		return subst;
	}
	
	public String toString() {
		return " < " + subst + " > " + renamedAtom;   
	}
}