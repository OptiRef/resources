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

import java.util.ArrayList;

import edu.ntua.isci.common.lp.Clause;

import edu.ntua.isci.qa.lp.theory.FCTheory;
import edu.ntua.isci.common.utils.Utils;

public abstract class QueryCollector {

	protected Clause initQuery;
	
	private ArrayList<Integer> ids;

	protected ArrayList<OrderedClause> orqueries;
	protected ArrayList<Clause> nrQueries;
	
	protected ArrayList<Integer> nrBlockStart;
	protected ArrayList<Integer> nrBlockStartIds;
	
	protected boolean nrc;
	
	public boolean removeAUX;
	protected FCTheory lp;
	
	protected int top;
	
	public long startTime;
	
	protected boolean removeRedundant;

	protected QueryCollector(FCTheory lp, Clause initQuery, boolean removeRedundant) {
		this.lp = lp;
		
		this.initQuery = initQuery;
		
		this.removeRedundant = removeRedundant;
		
		nrBlockStart = new ArrayList<Integer>();
		nrBlockStartIds = new ArrayList<Integer>();
		
		ids = new ArrayList<Integer>();
		
		orqueries = new ArrayList<OrderedClause>();
		
		startTime = System.currentTimeMillis();
		
		top = -1;
		
		removeAUX = true;
	}
	
	
	public void setRemoveAUX(boolean s) {
		this.removeAUX = s;
	}
	
	public void clear() {
		orqueries.clear();
		nrBlockStart.clear();
		nrBlockStartIds.clear();
		
		if (ids != null) {
			ids.clear();
		}
		
		if (nrQueries != null) {
			nrQueries.clear();
		}

	}

	public synchronized boolean add(Clause c) {
		return add(-1, c, false);
	}

	public synchronized boolean add(int id, Clause c) {
		return add(id, c, false);
	}
	
	public synchronized boolean add(int id, Clause cc, boolean isTop) {
		if (cc.isTautology()) {
			return false;
		}
		
		if (removeRedundant && isRedundant(id, cc)) {
			return false;
		}	
		
		processQuery(cc);
		
		orqueries.add(new OrderedClause(cc, isTop ? 0 : orqueries.size() + 1));
		
		if (id == -1) {
			ids = null;
		} else {
			if (nrBlockStart.size() == 0) {
				nrBlockStart.add(0);
				nrBlockStartIds.add(id);
			} else {
				if (nrBlockStartIds.get(nrBlockStart.size() - 1) != id) {
					nrBlockStart.add(ids.size());
					nrBlockStartIds.add(id);
				}
			}
			
			ids.add(id);
		}
		
		nrc = false;
		
		return true;
	}

	protected abstract void processQuery(Clause c);
	
	public ArrayList<Clause> getQueries() {
		ArrayList<Clause> queries = new ArrayList<Clause>();
		for (OrderedClause c : orqueries) {
			if (c.isTop()) {
				queries.add(0, c.getClause());
			} else {
				queries.add(c.getClause());
			}
		}
		
		return queries; 
	}
	
	public ArrayList<Clause> computeNonRedundantQueries() {
		return computeNonRedundantQueries(true);
	}
	
	public ArrayList<Clause> computeNonRedundantQueries(boolean allCondensed) {
		if (!nrc) {
			nrc = true;
			
			nrQueries = new ArrayList<Clause>();
			
			ArrayList<OrderedClause> tmpQueries = orqueries;
			if (!allCondensed) {
				tmpQueries = new ArrayList<OrderedClause>();
				for (OrderedClause oc : orqueries) {
					tmpQueries.add(new OrderedClause(oc.getClause().condenseWithOriginalVariables(), oc.getOrder()));
				}
			}
			
			for (OrderedClause c : Clause.getNonRedundantClauses(tmpQueries, removeRedundant, ids, nrBlockStart, nrBlockStartIds)) {
				if (c.isTop()) {
					nrQueries.add(0, c.getClause());
				} else {
					nrQueries.add(c.getClause());
				}
			}
		} 
		
		return nrQueries;
	}

	public boolean isRedundant(int id, Clause query) {

		for (int i = 0; i < orqueries.size();) {
			if (ids != null && id == ids.get(i)) {
				i = nrBlockStart.get(Utils.binarySearch(nrBlockStartIds, id));
				if (i + 1 < nrBlockStart.size()) {
					i = nrBlockStart.get(i + 1);
				} else {
					break;
				}
				continue;
			}
			
			if (orqueries.get(i).getClause().subsumes(query)) {
				return true;
			}
			
			i++;
		}
		
		return false;
	}		

	
}
