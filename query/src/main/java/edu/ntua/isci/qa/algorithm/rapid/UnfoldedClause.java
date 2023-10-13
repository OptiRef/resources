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

import edu.ntua.isci.common.lp.Clause;

public class UnfoldedClause {
	private Clause clause;
	private boolean pure;
	private boolean isTop;
	
	public UnfoldedClause(Clause clause) {
		this(clause, true, false);
	}
	
	public UnfoldedClause(Clause clause, boolean pure, boolean isTop) {
		this.clause = clause;
		this.pure = pure;
		this.isTop = isTop;
	}
	
	public Clause getClause() {
		return clause;
	}
	
	public boolean isPure() {
		return pure;
	}

	public boolean isTop() {
		return isTop;
	}

	public int hashCode() {
		return clause.hashCode();
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof UnfoldedClause) {
			return clause.equals(((UnfoldedClause)obj).clause);
		} else {
			return false;
		}
	}
}
