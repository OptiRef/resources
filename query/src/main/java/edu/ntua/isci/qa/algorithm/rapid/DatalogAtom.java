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

import edu.ntua.isci.common.lp.Atom;
import edu.ntua.isci.common.lp.Clause;

public class DatalogAtom {
	public Atom substAtom;
	public Atom leftAtom;
	public Atom rightAtom;
		
	public DatalogAtom(Atom a1, Atom a2, Atom a3) {
		this.substAtom = a1;
		this.leftAtom = a2;
		this.rightAtom = a3;
	}
	
	public Clause toDatalogClause() {
		if (!leftAtom.equals(rightAtom)) {
			return new Clause(leftAtom, rightAtom);
		}
		
		return null;
	}
	
	public int hashCode() {
		return leftAtom.hashCode();
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof DatalogAtom) {
			return leftAtom.equals(((DatalogAtom)obj).leftAtom);
		}
		
		return false;
	}
}