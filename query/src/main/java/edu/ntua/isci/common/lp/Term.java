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

public abstract class Term implements Cloneable, Comparable<Term> {

	public abstract Set<Variable> getVariables();
	
	public abstract Term apply(Substitution subst);
	
	public abstract boolean isConstant();
	
	public abstract boolean isVariable();
	
	public abstract boolean isFunctionalTerm();
	
	public abstract int depth();
	
	public abstract String toString();

	public boolean hasUnboundPrefix() {
		return false;
	}

	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}
	
	public Term firstDifferenceTerm(Term t) {
		if (this.equals(t)) {
			return null;
		} else {
			return t;
		}
	}	
	
	public abstract String getFunctionalPrefix();
	
	public boolean unboundEquals(Term t) {
		return this.equals(t);
	}	

}
