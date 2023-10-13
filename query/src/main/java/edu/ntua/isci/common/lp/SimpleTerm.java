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

public abstract class SimpleTerm extends Term implements Cloneable {

	protected String name;
	
	protected SimpleTerm(String name) {
		this.name = name;
	}

	public String root() {
		return name;
	}
	
	public String getName() {
		return name;
	}
	
	public int hashCode () {
		if (name == null) {
			return 0;
		}
		
		return name.hashCode();
	}
	
	public String toString() {
		return name;
	}
	
	public Object clone() {
		SimpleTerm t = (SimpleTerm)super.clone();
		t.name = name;
			
		return t;
	}		
	
}
