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
import edu.ntua.isci.common.lp.Predicate;


public class IndexEntryList {
	protected ArrayList<IndexEntry> atomList;
	protected Set<Integer> first;

	public IndexEntryList(ArrayList<IndexEntry> atomList) {
		this.atomList = atomList;
		this.first = new HashSet<Integer>();
	}
	
	public ArrayList<IndexEntry> getList() {
		return atomList;
	}
	
	public IndexEntry getIndexEntryAt(int k) {
		return atomList.get(k);
	}
	
	public Set<IndexEntry> getFirst() {
		Set<IndexEntry> res = new HashSet<IndexEntry>();
	
		for (int i : first) {
			res.add(atomList.get(i));
		}
		
		return res;
	}

	public int lastInsertIfNotInList(int i, Atom atom, IndexEntry newEntry) {
		
		Predicate pred = atom.getPredicate();
	
		int fI = i;
		while (fI >= 0 && atomList.get(fI).getAtom().getPredicate().equals(pred)) {
			if (atomList.get(fI).getAtom().unboundEquals(atom)) {
				return -1;
			}
			fI--;
		}
		
		
		int sI = i + 1; 
		while (sI < atomList.size() && atomList.get(sI).getAtom().getPredicate().equals(pred)) {
			if (atomList.get(sI).getAtom().unboundEquals(atom)) {
				return -1;
			}
			sI++;
		}
		
		atomList.add(sI, newEntry);
		
		return sI;
	}
	
	public boolean isInList(Atom atom, int end) {
		Predicate aPred = atom.getPredicate();
		int pos = binarySearch(aPred);

		if (pos < 0) {
			return false;
		} else {
			while (pos < end && atomList.get(pos).getAtom().getPredicate().equals(aPred)) {
				if (atomList.get(pos).getAtom().unboundEquals(atom)) {
					return true;
				} else {
					pos++;
				}
			}
		}
		
		return false;
	}


	public void markTopAtoms(Set<Atom> atoms) {
		for (Atom atom : atoms) {
			Predicate aPred = atom.getPredicate();
			int pos = binarySearch(aPred);
	
			if (pos < 0) {
				return;
			}
			
			while (pos < atomList.size() && atomList.get(pos).getAtom().getPredicate().equals(aPred)) {
				if (atomList.get(pos).getAtom().unboundEquals(atom)) {
					first.add(pos);
					break;
				} else {
					pos++;
				}
			}
		}
	}

    public int binarySearch(Predicate key) {
		int low = 0;
		int high = atomList.size() - 1;
		
		while (low <= high) {
			int mid = (low + high) >>> 1;
			
			int cmp = atomList.get(mid).getAtom().getPredicate().compareTo(key); 
			
			if (cmp < 0) {
				low = mid + 1;
			} else if (cmp > 0) {
				high = mid - 1;
			} else {
				while (mid > 0 && atomList.get(mid - 1).getAtom().getPredicate().equals(key)) {
					mid--;
				}
				
				return mid;
			}
		}
		
		return -(low + 1);
	}

}
