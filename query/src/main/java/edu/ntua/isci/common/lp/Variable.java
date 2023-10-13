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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import edu.ntua.isci.common.utils.Counter;


public class Variable extends SimpleTerm {
	
	public static String VARIABLE_PREFIX = "?";
	
	protected static String UNBOUND = "_";

	public static String HEADVAR_PREFIX = "x";
	public static String BODYVAR_PREFIX = "y";
	
	public static String BOUND_PREFIX = "w";
	
	public static String UNBOUND_PREFIX = UNBOUND + "u";
	public static String UNBOUND_FIXED = UNBOUND + "i";
	
	public Variable(String name) {
		super(name);
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public Set<Variable> getVariables() {
		Set<Variable> res = new HashSet<>();
		res.add(this);
		
		return res;
	}

	public ArrayList<Variable> getOrderedVariables() {
		ArrayList<Variable> res = new ArrayList<>();
		res.add(this);
		
		return res;
	}
	
	public boolean isConstant() {
		return false;
	}	
	
	public boolean isVariable() {
		return true;
	}
	
	public boolean isFunctionalTerm() {
		return false;
	}
	
	public boolean hasUnboundPrefix() {
		return name.startsWith(UNBOUND);
	}

	public static Variable getNewVariable(String prefix, Set<Variable> vars) {
		int i = 1;
		
		Variable v2 = new Variable(prefix + i);
		while (vars.contains(v2)) {
			v2 = new Variable(prefix + (++i));
		}
		
		return v2;
	}
	
	public static Variable getNewVariable(String prefix, Set<Variable> vars, Counter c) {

		Variable v2 = new Variable(prefix + c.getValue());
		c.increase();
		while (vars.contains(v2)) {
			v2 = new Variable(prefix + c.getValue());
			c.increase();
		}
		
		return v2;
	}
	
    public int depth() {
    	return 0;
    }
    
	public Term apply(Substitution subst) {
		Term t = subst.get(this);
		if (t != null) {
			return t;
		} else {
			return this;
		}
	}	

	public static Set<Variable> apply(Set<Variable> vars, Substitution s) { 
		Set<Variable> res = new HashSet<Variable>();
		
		for (Variable v : vars) {
			res.add((Variable)v.apply(s));
		}
		
		return res;
	}

	public Object clone() {
		Variable v = (Variable)super.clone();
		v.name = name;
		return v;
	}		
	
	public int compareTo(Term t) {
		if (t instanceof Constant || t instanceof FunctionalTerm) {
			return -1;
		}

		return name.compareTo(((Variable)t).getName());
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof Variable)) {
			return false;
		}
		
		return name.equals(((Variable)obj).getName());
	}
	
	public boolean unboundEquals(Term t) {
		if (t.isVariable() && this.hasUnboundPrefix() && ((Variable)t).hasUnboundPrefix()) {
			return true;
		}
		
		return equals(t);
	}
	
	public int hashCode() {
		return name.hashCode();
	}
	
	public String toString() {
		return VARIABLE_PREFIX + name;
	}	
	
	public String getFunctionalPrefix() {
		return "";
	}
	
}
