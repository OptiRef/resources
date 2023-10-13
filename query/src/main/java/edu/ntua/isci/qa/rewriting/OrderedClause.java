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

package edu.ntua.isci.qa.rewriting;

import edu.ntua.isci.common.lp.Clause;
import edu.ntua.isci.common.lp.ClauseWrapper;

public class OrderedClause implements ClauseWrapper {
	private Clause clause;
	private int order;
	
	public OrderedClause(Clause clause, int order) {
		this.clause = clause;
		this.order = order;
	}
	
	public boolean isTop() {
		return order == 0;
	}
	
	public int getOrder() {
		return order;
	}
	
	public Clause getClause() {
		return clause;
	}

	public <U extends ClauseWrapper> boolean isCCSubsumedBy(U c) {
		return clause.isCCSubsumedBy(c.getClause());
	}
	
	public String toString() {
		return clause.toString();
	}
	
}
