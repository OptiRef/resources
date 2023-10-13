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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import edu.ntua.isci.common.lp.Atom;
import edu.ntua.isci.common.lp.Clause;
import edu.ntua.isci.common.lp.Function;
import edu.ntua.isci.common.lp.FunctionalTerm;
import edu.ntua.isci.common.lp.Predicate;
import edu.ntua.isci.common.lp.Term;
import edu.ntua.isci.qa.lp.theory.LogicProgram;

public class FlattenerExtra {
	protected Set<Clause> elClausesSet;
	protected ArrayList<Clause> elClausesArray;
	protected ArrayList<ArrayList<Atom>> elClausesAtoms;
	
	protected ArrayList<Clause> fClausesArray;
	protected Set<Function> fSet;

	public FlattenerExtra() {
		elClausesSet = new HashSet<>();
		elClausesArray = new ArrayList<>();
		elClausesAtoms = new ArrayList<>();
		
		fClausesArray = new ArrayList<>();
		fSet = new HashSet<>();
	}
	
	
	public ArrayList<Clause> getExtraClauses() {
		return elClausesArray;
	}

	public void add(Clause nc) {
		if (elClausesSet.add(nc)) {
			elClausesArray.add(nc);
			elClausesAtoms.add(null);
		}
	}
	
	
	public void add(Clause nc, ArrayList<Atom> atoms) {
		if (elClausesSet.add(nc)) {
			elClausesArray.add(nc);
			elClausesAtoms.add(atoms);
		}
	}
	
	public void add(FlattenerExtra extra) {
		for (int i  = 0; i < extra.size(); i++) {
			add(extra.getClauseAt(i), extra.getAtomsAt(i));
		}
	}
	
	public boolean contains(Clause nc) {
		return elClausesSet.contains(nc);
	}
	
	public int size() {
		return elClausesArray.size();
	}
	
	public Clause getClauseAt(int i) {
		return elClausesArray.get(i);
	}


	public ArrayList<Atom> getAtomsAt(int i) {
		return elClausesAtoms.get(i);
	}
	
	private Function getEqFunction(Atom a) {
		if (a.getPredicate().getName().equals(Predicate.EQUAL_PREDICATE_NAME)) {
			Term t1 = a.getArgument(0);
			Term t2 = a.getArgument(1);
			
			if (t1.depth() == 0 && t2.depth() == 1) {
				return ((FunctionalTerm)t2).getFunction(); 
			} else if (t1.depth() == 1 && t2.depth() == 0) {
				return ((FunctionalTerm)t1).getFunction();
			}
		}
		
		return null;
	}
	
	public void addFClause(Clause c) {
		Function f = getEqFunction(c.getHead());
		if (f == null) {
			return;
		}
		
		fClausesArray.add(c);
		
		fSet.add(f);
	}
	
	public ArrayList<Clause> getFClauses() {
		return fClausesArray;
	}
	
	public Set<Function> getFSet() {
		return fSet;
	}

}
