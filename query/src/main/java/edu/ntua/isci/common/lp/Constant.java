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

import java.util.HashSet;
import java.util.Set;

public class Constant extends SimpleTerm {

	public Constant(String name) {
		super(name);
	}

	public Set<Variable> getVariables() {
		return new HashSet<Variable>();
	}

	public boolean isConstant() {
		return true;
	}	
	
	public boolean isVariable() {
		return false;
	}
	
	public boolean isFunctionalTerm() {
		return false;
	}	
	
    public int depth() {
    	return 0;
    }
    
	public Term apply(Substitution subst) {
		return this;
	}

	public Object clone() {
		Constant c = (Constant)super.clone();
		c.name = name;
		return c;
	}		
	
	public int compareTo(Term t) {
		if (t instanceof Variable) {
			return 1;
		} else if (t instanceof FunctionalTerm) {
			return -1;
		}
		
		return name.compareTo(((Constant)t).getName());
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof Constant)) {
			return false;
		}
		
		return name.equals(((Constant)obj).getName());
	}
	
	public int hashCode() {
		return name.hashCode();
	}
	
	public String toString() {
		return name;
	}
	
	public String getFunctionalPrefix() {
		return name;
	}

	
}
