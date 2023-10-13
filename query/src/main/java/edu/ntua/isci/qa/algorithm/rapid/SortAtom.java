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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import edu.ntua.isci.common.lp.Atom;
import edu.ntua.isci.common.lp.Predicate;
import edu.ntua.isci.common.lp.Variable;

public class SortAtom implements Comparable<SortAtom> {
	private Predicate[] preds;
	public boolean imode = true;
	public int annotation = 0;

	public static boolean simode = false;
	
	public static SortAtom createSortAtom(ArrayList<Atom> atoms) { 
		Set<Predicate> nbSet = new HashSet<>();
		 
		for (Atom a : atoms) {
			assert a.isConcept();

			nbSet.add(a.getPredicate());
		}

		return new SortAtom(nbSet);
	}
	
	public static SortAtom createSortAtom(Atom atom) {
		return new SortAtom(new Predicate[] {atom.getPredicate()} );
	}
	
	private SortAtom(Predicate[] ats) {
		this.preds = ats;
	}
	
	public SortAtom() {
		preds = new Predicate[0];
	}
	
	public SortAtom(Set<Predicate> ats) {
		preds = new Predicate[ats.size()];
		int i = 0;
		for (Predicate p : ats) {
			preds[i++] = p;
		}
		 
		
		Arrays.sort(preds);
	}
	
	public Predicate[] getPredicates() {
		return preds;
	}
	
	public Object clone() {
		Predicate[] newPreds = new Predicate[preds.length];
		System.arraycopy(preds, 0, newPreds, 0, preds.length);
			
		SortAtom sa = new SortAtom(newPreds);
		sa.imode = imode;
		
		return sa;
	}	
	
	public void add(Predicate s) {
		int p = Arrays.binarySearch(preds, s);
		if (p < 0) {
			p = -p - 1;
			Predicate[] newPreds = new Predicate[preds.length + 1];
			System.arraycopy(preds, 0, newPreds, 0, p);
			System.arraycopy(preds, p, newPreds, p + 1, preds.length - p);
			newPreds[p] = s;
			preds = newPreds;
			imode = true;
		}		
	}
	
	public void add(SortAtom sa) {
		Predicate[] newPreds;
		
		for (int i = 0; i < sa.preds.length; i++) {
			int p = Arrays.binarySearch(preds, sa.preds[i]);
			if (p < 0) {
				p = -p - 1;
				newPreds = new Predicate[preds.length + 1];
				System.arraycopy(preds, 0, newPreds, 0, p);
				System.arraycopy(preds, p, newPreds, p + 1, preds.length - p);
				newPreds[p] = sa.preds[i];
				preds = newPreds;
				imode = true;
			}
		}		
	}
	
	public boolean containsAll(SortAtom sa) {
		if (preds.length < sa.preds.length) {
			return false;
		}
		
		for (int i = 0; i < sa.preds.length; i++) {
			int p = Arrays.binarySearch(preds, sa.preds[i]);
			if (p < 0) {
				return false;
			}
		}
		
		return true;
	}
	
	
	public boolean equals(Object obj) {
		if (!(obj instanceof SortAtom)) {
			return false;
		}
		
		SortAtom sa = (SortAtom)obj;
		
		if (sa.preds.length != preds.length) {
			return false;
		}
		
		for (int i = 0; i < preds.length; i++) {
			if (!sa.preds[i].equals(preds[i])) {
				return false;
			}
		}
		
		return true;
	}
	
	public int hashCode() {
		int res = preds.length;
		
		for (int i = 0; i < preds.length; i++) {
			res += preds[i].hashCode();
		}
		
		return res;
	}
	
//	@Override
	public int compareTo(SortAtom arg0) {
		int s1 = preds.length;
		int s2 = arg0.preds.length;
		
		if (s1 < s2) {
			return -1;
		} else if (s1 > s2) {
			return 1;
		} else {
			for (int i = 0; i < s1; i++) {
				int c = preds[i].compareTo(arg0.preds[i]);
				if (c != 0) {
					return  c;
				}
			}
		
			return 0;
		}
	}
	
	public String toString() {
		String s = "";
		
		if (imode) {
			s += "*";
		}
		
		for (int i = 0; i < preds.length; i++) {
			if (i > 0) {
				s += "+";
			}
			s += preds[i].toString(false);
		}
		
		return s;
	}
	
	public ArrayList<Atom> toAtomArray(Variable v) {
		ArrayList<Atom> body = new ArrayList<>();
		for (Predicate p : preds) {
			body.add(new Atom(p, v));
		}
		
		return body;
	}
	
	public static void addAllCompact(ArrayList<SortAtom> list, ArrayList<SortAtom> lsa) {
		for (SortAtom sa : lsa) {
			add(list, sa);
		}
		compact(list);
	}

	
	public static void add(ArrayList<SortAtom> list, SortAtom sa) {
		int pos = Collections.binarySearch(list, sa);
		if (pos < 0) {
			if (simode) {
				sa = (SortAtom)sa.clone();
				sa.imode = true;
			}
			list.add(-pos - 1, sa);
		}
	}
	
	public static void compact(ArrayList<SortAtom> list) {
		for (int i = 0; i < list.size(); i++) {
			SortAtom at = list.get(i);
			for (int j = i + 1; j < list.size();) {
				if (list.get(j).containsAll(at)) {
					list.remove(j);
				} else {
					j++;
				}
			}
		}
	}
}