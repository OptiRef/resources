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

public class Function extends Functional  {

	public Function(String name, int arity) {
		super(name, arity);
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof Function)) {
			return false;
		} else {
			return name.equals(((Function)obj).getName()) && arity == ((Function)obj).getArity();
		}
	}
	
	public int hashCode() {
		if (name != null) {
			return name.hashCode() + arity;
		} else {
			return 0;
		}
	}
	
	public int compareTo(Function p) {
		
		int nc = name.compareTo(p.getName());
		if (nc != 0) {
			return nc;
		} else {
			if (arity < p.getArity()) {
				return -1;
			} else if (arity > p.getArity()) {
				return 1;
			} else {
				return 0;
			}
		}
	}	
}
