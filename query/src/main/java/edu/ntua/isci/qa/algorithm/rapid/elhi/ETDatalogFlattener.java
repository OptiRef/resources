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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.ntua.isci.common.lp.Atom;
import edu.ntua.isci.common.lp.Clause;
import edu.ntua.isci.common.lp.Variable;
import edu.ntua.isci.qa.algorithm.rapid.AtomUnfolder;
import edu.ntua.isci.qa.algorithm.rapid.AtomUnfolding;
import edu.ntua.isci.qa.algorithm.rapid.FlattenerExtra;
import edu.ntua.isci.qa.algorithm.rapid.MarkedClause;
import edu.ntua.isci.qa.algorithm.rapid.ClauseUnfolder;
import edu.ntua.isci.qa.algorithm.rapid.Rapid;
import edu.ntua.isci.qa.algorithm.rapid.SortAtom;
import edu.ntua.isci.qa.algorithm.rapid.dllite.DLAtomUnfolding;
import edu.ntua.isci.qa.lp.theory.FCTheory;

public class ETDatalogFlattener extends ETFlattener {
	
	public Map<Atom, ArrayList<SortAtom>> unboundRoleMap = new HashMap<>();
	
	public ETDatalogFlattener(FCTheory fc) {
		super(fc);
	}
	
	public AtomUnfolding createAtomUnfolding(VUnfoldStructure vus, Map<Atom, VUnfoldStructure> map, Atom head, FlattenerExtra extra) {
		if (Rapid.expandMode == 1) {
			for (Map.Entry<Atom, VUnfoldStructure> entry : map.entrySet()) {
				Atom b = Rapid.normalizeAtom(entry.getKey(), Rapid.dummyTerms).substAtom;
				for (HUnfoldStructure hus : entry.getValue().getElements()) {
					extra.add(new Clause(b, hus.toConjunction()));
				}
			}
		} else {
			for (Map.Entry<Atom, VUnfoldStructure> entry : map.entrySet()) {
				Atom b = entry.getKey();
				
				for (HUnfoldStructure hus : entry.getValue().getElements()) {
					if (hus.size() == 1 && hus.getElementAt(0).getTopAtom().isRole()) {
						extra.add(new Clause(b, hus.toConjunction()));
					} 
				}
				
				if (b.isRole()) {
					for (Variable v : b.getVariables()) {
						if (v.hasUnboundPrefix()) {
							ArrayList<SortAtom> cNewAtoms = new ArrayList<>();
							for (Atom a : entry.getValue().collectLeftMostUnfoldings(true)) {
								if (!a.isRole()) {
									SortAtom.add(cNewAtoms, SortAtom.createSortAtom(a));
								}
							}
							for (HUnfoldStructure hus : entry.getValue().getElements()) {
								if (hus.size() > 1 ) {
									SortAtom.add(cNewAtoms, SortAtom.createSortAtom(hus.toConjunction()));
								} 
							}
							
							SortAtom.compact(cNewAtoms);
							unboundRoleMap.put(b, cNewAtoms);
							
							break;
						}
					}
				}
			}

			for (Map.Entry<Atom, VUnfoldStructure> entry : map.entrySet()) {
				Atom b = entry.getKey();
				Set<Atom> singleBodies = new HashSet<>();
				
				if (unboundRoleMap.get(b) != null) {
					continue;
				}
				
				for (HUnfoldStructure hus : entry.getValue().getElements()) {
					if (hus.size() == 1 && hus.getElementAt(0).getTopAtom().isRole()) {
						continue;
					} 

					ArrayList<Atom> body = hus.toConjunction();

					if (body.size() == 1) {
						singleBodies.add(body.get(0));
					} else {
						extra.add(new Clause(b, hus.toConjunction()));
					}
				}
				
				for (Atom a : singleBodies) {
					extra.add(new Clause(b, a));
				}
			}
		}
		
		return new DLAtomUnfolding(new Atom[0], vus.getFunctionMaps());
	}
	
	public ArrayList<MarkedClause> getExtraQueries(AtomUnfolder au, ClauseUnfolder cu, FlattenerExtra extra) {
		ArrayList<MarkedClause> res = new ArrayList<>();
		
		int id = 0;
		for (int i = 0; i < extra.size(); i++) {
			res.add(new MarkedClause(extra.getClauseAt(i), id++));
		}
			
		return res;
	}

}
