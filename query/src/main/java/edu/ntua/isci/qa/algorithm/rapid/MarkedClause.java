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
import edu.ntua.isci.common.lp.ClauseWrapper;

public class MarkedClause implements ClauseWrapper {
	private Clause clause;
	private int mark;
	
	public MarkedClause(Clause clause, int mark) {
		this.clause = clause;
		this.mark = mark;
	}
	
	public Clause getClause() {
		return clause;
	}
	
	public int getMark() {
		return mark;
	}
	
	public String toString() {
		return mark + " " + clause;
	}

	public <U extends ClauseWrapper> boolean isCCSubsumedBy(U u) {
		return clause.isCCSubsumedBy(u.getClause());
	}
	
	public int hashCode() {
		return clause.hashCode();
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof MarkedClause) {
			return clause.equals(((MarkedClause)obj).clause);
		} else {
			return false;
		}
	}

}
