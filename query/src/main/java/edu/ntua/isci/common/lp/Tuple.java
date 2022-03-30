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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Tuple implements Cloneable, Comparable<Tuple> {

	private Term[] tuple;

	public Tuple() {
		this.tuple = new Term[] {};
	}

	public Tuple(Term... tuple) {
		this.tuple = tuple;
	}
	
	public int size() {
		return tuple.length;
	}
	
	public Term[] getTerms() {
		return tuple;
	}
	
	public Term getTerm(int i) {
		return tuple[i];
	}

	public boolean hasSomeTermIn(Set<? extends Term> set) {
		for (int i = 0; i < tuple.length; i++) {
			if (set.contains(tuple[i])) {
				return true;
			}
		}
		
		return false;
	}
	
	public int depth() {
    	if (tuple.length == 0) {
	    	return 0;
    	}
	    
    	int depth = 0;
	    for (int i = 0; i < tuple.length; i++) {
	    	depth = Math.max(depth, tuple[i].depth());
	    }
	    		
	    return depth;
	}
	
	public Object clone() {
		try {
			Tuple t = (Tuple)super.clone();
			t.tuple = new Term[tuple.length];
			
			for (int i = 0; i < tuple.length; i++) {
				t.tuple[i] = (Term)tuple[i].clone(); 
			}
			
			return t;
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}				
	}			
	
	public Set<Variable> getVariables() {
		Set<Variable> res = new HashSet<>();
		
		for (int i = 0; i < tuple.length; i++) {
			res.addAll(tuple[i].getVariables());
		}
		
		return res;
	}
	
	public Tuple apply(Substitution subst) {
		Term[] t = new Term[tuple.length];
		
		for (int i = 0; i < tuple.length; i++) {
			t[i] = tuple[i].apply(subst);
		}
		
		return new Tuple(t);
	}
	
	public int compareTo(Tuple t) {
		for (int i = 0; i < Math.min(tuple.length, t.size()); i++) {
			int nc = tuple[i].compareTo(t.getTerm(i));
			if (nc != 0) {
				return nc;
			}
		}
		
		if (tuple.length < t.size()) {
			return -1;
		} else if (tuple.length > t.size()) {
			return 1;
		} else {
			return 0;
		}
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof Tuple)) {
			return false;
		} else {
			Term[] arr = ((Tuple)obj).tuple;
			
			if (tuple.length != arr.length) {
				return false;
			} else {
				
				for (int i = 0; i < tuple.length; i++) {
					if (!tuple[i].equals(arr[i])) {
						return false;
					}
				}
				return true;
			}
		}
	}
	
	public boolean unboundEquals(Tuple obj) {
		Term[] arr = ((Tuple)obj).tuple;
			
		if (tuple.length != arr.length) {
			return false;
		} else {
			
			for (int i = 0; i < tuple.length; i++) {
				if (!tuple[i].unboundEquals(arr[i])) {
					return false;
				}
			}
			return true;
		}
	}
	
	public int hashCode() {
		if (tuple == null) {
			return 0;
		}
		
		int hashCode = 0;
		
		for (int i = 0; i < tuple.length; i++) {
			hashCode += tuple[i] != null ? tuple[i].hashCode() : 0;
		}

		return hashCode;
	}
	
	public String toString() {
		String s = "(";

		for (int i = 0; i < tuple.length; i++) {
			s += tuple[i];
			if (i < tuple.length - 1) {
				s += ", ";
			}
		}

		s += ")";
		
		return s;
		
	}
	
	public String[] toStringArray() {
		String s[] = new String[tuple.length];


		for (int i = 0; i < tuple.length; i++) {
			s[i]= tuple[i].toString();
		
		}
		return s;
		
	}
	public List<String> toStringList() {
		String s[] = new String[tuple.length];
		for (int i = 0; i < tuple.length; i++) {
			s[i]= tuple[i].toString();
		}
		return Arrays.asList(s);
	}

}
			
	
	 	
	