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

import java.util.Set;

public class FunctionalTerm extends Term {

	public Function function;
	public Tuple args;

	public FunctionalTerm(Function function, Term... args) {
		this(function, new Tuple(args));
	}

	public FunctionalTerm(Function function, Tuple args) {
		this.function = function;
		this.args = args;
	}
	
	public String root() {
		return function.toString();
	}
	
	public Function getFunction() {
		return function;
	}
	
	public Tuple getArguments() {
		return args;
	}
	
	public Set<Variable> getVariables() {
		return args.getVariables();
	}

	public boolean isConstant() {
		return false;
	}	
	
	public boolean isVariable() {
		return false;
	}
	
	public boolean isFunctionalTerm() {
		return true;
	}
	
    public int depth() {
    	if (args.size() == 0) {
    		return 0;
    	} else {
    		return 1 + args.depth();
    	}
    }
    
	public Term apply(Substitution subst) {
		return new FunctionalTerm(function, args.apply(subst));
	}
	
	public int compareTo(Term t) {
		if (t instanceof Variable || t instanceof Constant) {
			return 1;
		}
		
		int nc = function.compareTo(((FunctionalTerm)t).getFunction());
		if (nc != 0) {
			return nc;
		}

		return args.compareTo(((FunctionalTerm)t).getArguments());
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof FunctionalTerm)) {
			return false;
		} else {
			if (!function.equals(((FunctionalTerm)obj).getFunction())) {
				return false;
			}
			
			return args.equals(((FunctionalTerm)obj).getArguments());
		}
	}
	
	public int hashCode() {
		return function.hashCode() + args.hashCode();
		
	}	
	
	public Object clone() {
		FunctionalTerm t = (FunctionalTerm)super.clone();
		t.function = function;
		t.args = (Tuple)args.clone(); 
		
		return t;
	}		
	
	public String toString() {
		return function + args.toString();
	}
	
	public Term firstDifferenceTerm(Term t) {
		
		if (this.equals(t)) {
			return null;
		}
		
		if (!t.isFunctionalTerm()) {
			return t;
		}
		
		FunctionalTerm ft = (FunctionalTerm)t;
		
		if (!function.equals(ft.getFunction())) {
			return ft;
		} else {
			for (int i = 0; i < args.size(); i++) {
				Term t1 = args.getTerm(i);
				Term t2 = ft.getArguments().getTerm(i);
		
				Term r = t1.firstDifferenceTerm(t2);
				
				if (r != null) {
					return r;
				}
			}
		}
		
		return null;
		
	}
	
    public String getFunctionalPrefix() {
    	
    	if (args.size() == 0) {
    		return function.getName();
    	} else {
    		String arguments = "";
    		for(int i = 0; i < args.size(); i++){
    			arguments += args.getTerm(i).getFunctionalPrefix();
    		}
    		
    		return function.getName() + arguments;
    	}
    }	
	
}
