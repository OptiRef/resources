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

import edu.ntua.isci.common.lp.Atom;
import edu.ntua.isci.common.lp.Clause;
import edu.ntua.isci.common.lp.Substitution;
import edu.ntua.isci.common.lp.Term;
import edu.ntua.isci.common.lp.Variable;

import edu.ntua.isci.qa.algorithm.rapid.AtomUnfolding;
import edu.ntua.isci.qa.algorithm.rapid.FunctionMap;
import edu.ntua.isci.qa.algorithm.rapid.Rapid;

public class ETAtomUnfolding extends AtomUnfolding {
	
	private ArrayList<NamedAtomList> namedList;
	public ArrayList<Clause> cycleQueries;
	
	public ETAtomUnfolding(ArrayList<NamedAtomList> namedList, FunctionMap[] argFunctions) {
		super(argFunctions);

		this.namedList = namedList;
	}
	
	public ArrayList<NamedAtomList> getNamedListAs(int i) {
		ArrayList<NamedAtomList> res = new ArrayList<NamedAtomList>();
		
		for (NamedAtomList nal : namedList) {
			NamedAtomList n = new NamedAtomList(nal.getPrefix());
			n.setIndex(i);
			
			res.add(n);
			
			// keep first two unbound variables;
			Variable[] ut = new Variable[2];
			int ii = 0;
			for (Term t : nal.getTerms()) {
				if (t.hasUnboundPrefix()) {
					ut[ii++] = (Variable)t;
				}
				if (ii == 2) {
					break;
				}
			}
			
			for (Atom a : nal.getAtoms()) {
				Substitution s = new Substitution();
				int jj = 0;
				for (Variable v : a.getVariables()) {
					if (Rapid.isJoinVariable(v)) {
						s.put(v, new Variable(v.getName() + "." + i));
					} else if (v.hasUnboundPrefix()) {
						s.put(v, new Variable(ut[jj++].getName() + "." + i));
					}
				}

				n.add(a.apply(s));
			}
		}
		
		return res;
	}
	
}
