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

package edu.ntua.isci.qa.algorithm.rapid.elhi;

import java.util.ArrayList;

import edu.ntua.isci.common.lp.Clause;

import edu.ntua.isci.qa.algorithm.rapid.MarkedClause;
import edu.ntua.isci.qa.algorithm.rapid.Rapid;

public class ETRapid extends Rapid {

	public boolean datalog;
	public boolean fdatalog;
	
	public static ETRapid createDatalogRapid() {
		return new ETRapid(true, true);
	}

	protected ETRapid(boolean datalog, boolean fdatalog) {
		super("ETRapid" + (fdatalog ? "-datalog" : (datalog ? "-unfold" : "-expand")), fdatalog ? false : true);
		this.datalog = datalog;
		this.fdatalog = fdatalog;
	}
	
	public boolean isELHI() {
		return true;
	}
	
	public boolean isSHIQ() {
		return true;
	}
	
	public void execute() {
		if (datalog) {
			qc.setRemoveAUX(false);
		}
		super.execute();
	}
	
	public ArrayList<Clause> postsc(ArrayList<Clause> clauses) {
		ArrayList<Clause> rClauses = clauses; 
		if (!datalog) {
			rClauses = Clause.getNonRedundantClauses(clauses);
		}
		
		return super.postsc(rClauses);
	}

	public void initialize() {
		if (fdatalog) {
			flattener = new ETDatalogFlattener(lp);
			atomUnfolder = new ETAtomUnfolder(lp, (ETFlattener)flattener);
			clauseUnfolder = null;
		}
	}
	
	protected ArrayList<Clause> strip(ArrayList<MarkedClause> queryList) {
		ArrayList<Clause> res = new ArrayList<Clause>();
		ArrayList<Integer> ids = new ArrayList<Integer>();
		
		int id = 0;
		int pid = 0;
		for (int k = 0; k < queryList.size(); k++) {
			res.add(queryList.get(k).getClause());
			
			if (k == 0) {
				id = 0;
				pid = queryList.get(k).getMark();
			} else {
				int kid = queryList.get(k).getMark();
				if (kid != pid) {
					id++;
					pid = kid;
				}
			}
			
			ids.add(id);
		}

		ArrayList<Clause> ret = Clause.getNonRedundantClauses(res, ids);
	
		return ret;
	}

}
