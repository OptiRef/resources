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
import java.util.Set;

import edu.ntua.isci.common.lp.Atom;

import edu.ntua.isci.qa.algorithm.rapid.IndexEntry;

public class DLIndexEntry extends IndexEntry {
	public ArrayList<Integer> fIndex;
	public DLIndexEntry firstEntry;
	public DLIndexEntry nextEntry;
	
	public ArrayList<Integer> covered;
	
	public DLIndexEntry mainRoot;
	
	public Set<DLIndexEntry> roots;
	public Set<DLIndexEntry> children;
	
	public DLIndexEntry(Atom atom) {
		super(atom);
	}

	public String toString() {
		return atom.toString();
	}
}
