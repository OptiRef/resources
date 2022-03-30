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

package edu.ntua.isci.qa.algorithm.rapid.dllite;

import java.util.ArrayList;

import edu.ntua.isci.common.lp.Clause;

import edu.ntua.isci.qa.algorithm.rapid.MarkedClause;
import edu.ntua.isci.qa.algorithm.rapid.Rapid;

public class DLRapid extends Rapid {

	private int mode;
	
	public static DLRapid createDatalogRapid() {
		return new DLRapid(1, false);
	}

	public static DLRapid createSimpleUnfoldRapid() {
		return new DLRapid(1, true);
	}

	public static DLRapid createFastUnfoldRapid() {
		return new DLRapid(2, true);
	}

	private DLRapid(int mode, boolean unfold) {
		super("DLRapid-" + mode + (unfold == true ? " unfold":""), unfold);
		
		this.mode = mode;
	}
	
	public boolean isELHI() {
		return false;
	}

	public boolean isSHIQ() {
		return false;
	}
	
	public void initialize() {
		flattener = new DLFlattener();
		atomUnfolder = new DLAtomUnfolder(lp);
		
		if (mode == 1) {
			clauseUnfolder = new DLUnfolder(lp, atomUnfolder, new DLSimpleSubsumeChecker());
		} else if (mode == 2) {
			clauseUnfolder = new DLUnfolder(lp, atomUnfolder, new DLFastSubsumeChecker());
		}
	}
	
	public ArrayList<Clause> postsc(ArrayList<Clause> c) {
		return c;
	}
	
	protected ArrayList<Clause> strip(ArrayList<MarkedClause> queryList) {
		ArrayList<Clause> queries = new ArrayList<Clause>();
	
		for (int k = 0; k < queryList.size(); k++) {
			queries.add(((DLUnfolder)clauseUnfolder).testUnfold(queryList.get(k).getClause(), null));
		}

		queries = Clause.getNonRedundantClauses(queries);

		return queries;
	}

}
