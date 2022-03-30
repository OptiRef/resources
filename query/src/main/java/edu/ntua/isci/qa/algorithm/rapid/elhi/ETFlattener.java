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

import java.util.Map;

import edu.ntua.isci.common.lp.Atom;
import edu.ntua.isci.common.lp.Substitution;
import edu.ntua.isci.common.lp.Term;
import edu.ntua.isci.common.lp.Variable;

import edu.ntua.isci.qa.algorithm.rapid.AtomUnfolding;
import edu.ntua.isci.qa.algorithm.rapid.Flattener;
import edu.ntua.isci.qa.algorithm.rapid.FlattenerExtra;
import edu.ntua.isci.qa.algorithm.rapid.Rapid;
import edu.ntua.isci.qa.lp.theory.FCTheory;

public abstract class ETFlattener extends Flattener {

	protected FCTheory lp;
	
	public abstract AtomUnfolding createAtomUnfolding(VUnfoldStructure vus, Map<Atom, VUnfoldStructure> map, Atom head, FlattenerExtra extra);
	
	protected ETFlattener(FCTheory lp) {
		this.lp = lp;
	}

	
	protected Substitution appendSuffix(Substitution subst, int s) {
		Substitution res = new Substitution(); 
		
		for (Variable v : subst.getVariables()) {
			Variable t = (Variable)subst.get(v);
		
			if (!Rapid.isCoreVariable(t)) {
				res.put(v, new Variable(t.getName() + s));
			} else {
				res.put(v, t);
			}
		}
		
		return res;
	}
	
	public static boolean formEquals(Atom a1, Atom a2) {
		if (!a1.getPredicate().equals(a2.getPredicate())) {
			return false;
		}
		
		Term[] t1 = a1.getArguments().getTerms();
		Term[] t2 = a2.getArguments().getTerms();
		
		for (int i = 0; i < t1.length; i++) {
			if (t1[i].isConstant() && t1[i].equals(t2[i])) {
				continue;
			} else if (t1[i].isVariable() && t2[i].isVariable()) {
				Variable v1 = (Variable)t1[i];
				Variable v2 = (Variable)t2[i];
				
				if (v1.hasUnboundPrefix() && v2.hasUnboundPrefix()) {
					continue;
				} else if (!v1.hasUnboundPrefix() && !v2.hasUnboundPrefix()) {
					continue;
				}
			}
			
			return false;
		}
		
		return true;
	}
	
}
