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
import java.util.HashSet;
import java.util.Set;

import edu.ntua.isci.common.lp.Predicate;

import edu.ntua.isci.qa.algorithm.rapid.IndexEntry;
import edu.ntua.isci.qa.algorithm.rapid.IndexEntryList;


public class DLIndexEntryList extends IndexEntryList {
	private Set<Predicate> preds;
	
	public DLIndexEntryList(ArrayList<IndexEntry> atomList) {
		super(atomList);
		
		preds = new HashSet<Predicate>();
		
		for (IndexEntry ie : atomList) {
			preds.add(ie.getAtom().getPredicate());
		}
	}
	
	public Set<Predicate> getPredicates() {
		return preds;
	}

}
