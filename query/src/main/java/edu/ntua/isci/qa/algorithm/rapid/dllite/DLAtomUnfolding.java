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

import edu.ntua.isci.common.lp.Atom;

import edu.ntua.isci.qa.algorithm.rapid.AtomUnfolding;
import edu.ntua.isci.qa.algorithm.rapid.FunctionMap;

public class DLAtomUnfolding extends AtomUnfolding {
	private Atom[] unfoldings;
	
	public boolean[] mark;

	// unfoldings array must be sorted by Atom.PredicateComparator
	public DLAtomUnfolding(Atom[] unfoldings, FunctionMap[] argFunctions) {
		super(argFunctions);

		this.unfoldings = unfoldings;
	}

	public Atom[] getUnfoldings() {
		return unfoldings;
	}

	public void remove(Atom atom) {
		for (int i = 0; i < unfoldings.length; i++) {
			if (atom.equals(unfoldings[i])) {
				Atom[] newUnfoldings = new Atom[unfoldings.length - 1];
				System.arraycopy(unfoldings, 0, newUnfoldings, 0, i);
				System.arraycopy(unfoldings, i + 1, newUnfoldings, i, unfoldings.length - i - 1);
				unfoldings = newUnfoldings; 
				break;
			}
		}
	}
		
	public String toString() {
		String s = "";
		for (Atom a : unfoldings) {
			if (s.length() > 0) {
				s += ", ";
			}
			s += a;
		}
		s = "(" + argFunctions[0] + ", " + argFunctions[1] + ") : [" + s + "]"; 

		return s;
	}

	
}
