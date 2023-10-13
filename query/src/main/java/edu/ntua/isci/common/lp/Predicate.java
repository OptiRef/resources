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

public class Predicate extends Functional implements Comparable<Predicate> {
	
	public static Predicate TRUTH_PREDICATE = new Predicate("t", 0);
	public static Predicate FALSE_PREDICATE = new Predicate("f", 0);
	
	public static String QUERY_PREDICATE_NAME = "Q";
	public static String EQUAL_PREDICATE_NAME = "=";

	public static Predicate EQUALITY_PREDICATE = new Predicate(EQUAL_PREDICATE_NAME, 2);
	
	public Predicate(String name, int arity) {
		super(name, arity);
	}

	public boolean isConcept() {
		return arity == 1;
	}
	
	public boolean isRole() {
		return arity == 2;
	}
	
	public Atom toGeneralAtom() {
		Variable[] vars = new Variable[arity];
		
		for (int i = 0; i < vars.length; i++) {
			vars[i] = new Variable("x" + i);
		}
		
		return new Atom(this, vars);
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof Predicate)) {
			return false;
		} else {
			return name.equals(((Predicate)obj).getName()) && arity == ((Predicate)obj).getArity();
		}
	}
	
	public int hashCode() {
		if (name != null) {
			return name.hashCode() + arity;
		} else {
			return 0;
		}
	}
	
	public int compareTo(Predicate p) {
		
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

//	public boolean isQueryPredicate() {
//		return name.equals(QUERY_PREDICATE_NAME);
//	}
	
	public boolean isArithmeticPredicate() {
		return false;
	}

}