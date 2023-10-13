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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.ntua.isci.common.lp.Atom;
import edu.ntua.isci.common.lp.Function;
import edu.ntua.isci.common.lp.Predicate;
import edu.ntua.isci.common.lp.Substitution;
import edu.ntua.isci.common.lp.Variable;
import edu.ntua.isci.common.utils.SetUtils;

public class FunctionMap extends HashMap<Function, ArrayList<SortAtom>> {
	
	private static final long serialVersionUID = 1L;
	
	public FunctionMap() {
		super();
	}
	
	public boolean equalsFM(FunctionMap fm) {
		Set<Function> f1 = this.keySet();
		Set<Function> f2 = fm.keySet();
		
		if (!SetUtils.areEqual(f1, f2)) {
			return false;
		}
		
		for (Function f : f1) {
			ArrayList<SortAtom> sa1 = this.get(f);
			ArrayList<SortAtom> sa2 = fm.get(f);

			if (sa1.size() != sa2.size()) {
				return false;
			}
			
			for (int i = 0; i < sa1.size(); i++) {
				if (!sa1.get(i).equals(sa2.get(i))) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	public FunctionMap copy() {
		FunctionMap fm = new FunctionMap();
		for (Map.Entry<Function, ArrayList<SortAtom>> entry : entrySet()) {
			fm.put(entry.getKey(), new ArrayList<>(entry.getValue()));
		}
		
		return fm;
	}
	
	
	public void union(FunctionMap... afm) {
		Set<Function> used = new HashSet<>();
		
		for (FunctionMap fm : afm) {
			for (Map.Entry<Function, ArrayList<SortAtom>> entry : fm.entrySet()) {
				Function f = entry.getKey();
	
				ArrayList<SortAtom> aSet = get(f);
				
				if (used.add(f)) {
					if (aSet == null) {
						aSet = new ArrayList<>();
					} else {
						aSet = new ArrayList<>(aSet);
					}
					put(f, aSet);
				}
				
				SortAtom.addAllCompact(aSet, entry.getValue());
			}
		}
	}
	
	public static FunctionMap sunion(FunctionMap m1, FunctionMap m2) {
		FunctionMap res = new FunctionMap();

		Set<Function> used = new HashSet<Function>();
		for (Map.Entry<Function, ArrayList<SortAtom>> entry : m1.entrySet()) {
			Function f = entry.getKey();
			ArrayList<SortAtom> aSet = new ArrayList<SortAtom>(entry.getValue());
			
			ArrayList<SortAtom> aSet2 = m2.get(f);
			if (aSet2 != null) {
				SortAtom.addAllCompact(aSet, aSet2);
			}
			
			res.put(f, aSet);
			
			used.add(f);
		}
		
		for (Map.Entry<Function, ArrayList<SortAtom>> entry : m2.entrySet()) {
			Function f = entry.getKey();
			if (used.contains(f)) {
				continue;
			}
			
			res.put(f, new ArrayList<>(entry.getValue()));
		}
		
		return res;
	}
	
	public static FunctionMap difference(FunctionMap m1, FunctionMap m2) {
		FunctionMap res = new FunctionMap();
		
		for (Map.Entry<Function, ArrayList<SortAtom>> entry : m1.entrySet()) {
			Function f = entry.getKey();
			ArrayList<SortAtom> aSet1 = entry.getValue();
			
			ArrayList<SortAtom> aSet2 = m2.get(f);
			if (aSet2 != null) {
				ArrayList<SortAtom> aSet = new ArrayList<>();
				
				loop:
				for (SortAtom s1 : aSet1) {
					for (SortAtom s2 : aSet2) {
						if (s1.containsAll(s2)) {
							continue loop;
						}
					}
					
					aSet.add(s1);
				}
				
				if (!aSet.isEmpty()) {
					SortAtom.compact(aSet);
					res.put(f, aSet);
				}
			} else {
				res.put(f, aSet1);
			}
		}
		
		return res;
	}
	
	public void difference(FunctionMap fm) {
		for (Iterator<Map.Entry<Function, ArrayList<SortAtom>>> fiter = entrySet().iterator(); fiter.hasNext();) {
			Map.Entry<Function, ArrayList<SortAtom>> entry = fiter.next();
			
			ArrayList<SortAtom> aSet2 = fm.get(entry.getKey());
			if (aSet2 != null) {
				ArrayList<SortAtom> aSet1 = entry.getValue();

				for (Iterator<SortAtom> iter = aSet1.iterator(); iter.hasNext();) {
					SortAtom s1 = iter.next();

					for (SortAtom s2 : aSet2) {
						if (s1.containsAll(s2)) {
							iter.remove();
							break;
						}
					}
				}
				
				if (!aSet1.isEmpty()) {
					SortAtom.compact(aSet1);
				} else {
					fiter.remove();
				}
			}
		}
	}

	public void intersection(FunctionMap m2) {
		for (Iterator<Map.Entry<Function, ArrayList<SortAtom>>> fiter = entrySet().iterator(); fiter.hasNext();) {
			Map.Entry<Function, ArrayList<SortAtom>> entry = fiter.next();
			
			Function f = entry.getKey();
			
			ArrayList<SortAtom> aSet2 = m2.get(f);
			if (aSet2 != null) {
				ArrayList<SortAtom> aSet1 = entry.getValue();

				SortAtom[] aSet1array = aSet1.toArray(new SortAtom[] {});
				aSet1.clear();
				
				for (SortAtom a1 : aSet1array) {
					for (SortAtom a2 : aSet2) {
						SortAtom tSet = (SortAtom)a1.clone();
						tSet.add(a2);

						SortAtom.add(aSet1, tSet);
					}
				}
				
				SortAtom.compact(aSet1);
			} else {
				fiter.remove();
			}
		}
	}
	
	public void addPredicates(Set<Predicate> newPreds) {
		if (newPreds.size() == 0) {
			return;
		}
		
	
		for (Map.Entry<Function, ArrayList<SortAtom>> entry : entrySet()) {
			Rapid.fcounter++;

			ArrayList<SortAtom> aSet = entry.getValue();
			
			for (int i = 0; i < aSet.size(); i++) {
				SortAtom sa = (SortAtom)aSet.get(i).clone();
				aSet.set(i, sa);
				for (Predicate p : newPreds) {
					sa.add(p);
				}
				
			}
			
			SortAtom.compact(aSet);
		}
	}
	
	public ArrayList<ArrayList<Atom>> createBodies(ArrayList<Atom> body, Substitution subst) {
		ArrayList<ArrayList<Atom>> res = new ArrayList<ArrayList<Atom>>();
		
		for (ArrayList<SortAtom> iaSets : values()) {
			for (SortAtom aSets : iaSets) {
				ArrayList<Atom> newBody = (ArrayList<Atom>)body.clone();
				for (Predicate p : aSets.getPredicates()) {
					Atom a = new Atom(p, new Variable(Rapid.DUMMY_VAR_PREFIX + "1"));
					if (!newBody.contains(a)) {
						newBody.add(a);
					}
				}
				res.add(newBody);
			}
		}
		
		return res;
	}

}
