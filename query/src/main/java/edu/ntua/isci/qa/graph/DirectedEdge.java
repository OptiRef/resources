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

package edu.ntua.isci.qa.graph;

import edu.ntua.isci.common.lp.Substitution;
import edu.ntua.isci.common.lp.Term;

public class DirectedEdge<T extends Term> {
	public T n1, n2;
	
	public DirectedEdge(T n1, T n2) {
		this.n1 = n1;
		this.n2 = n2;
	}
	
	public DirectedEdge<T> apply(Substitution s) {
		return new DirectedEdge(n1.apply(s), n2.apply(s));
	}
	
	public T getP1() {
		return n1;
	}

	public T getP2() {
		return n2;
	}

	public int hashCode() {
		return n1.hashCode() + n2.hashCode();
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof DirectedEdge)) {
			return false;
		}
		
		DirectedEdge<T> e2 = (DirectedEdge<T>)obj;
		
		return e2.n1.equals(n1) && e2.n2.equals(n2);
	}
	
	public String toString() {
		return n1 + "-" + n2;
	}
}