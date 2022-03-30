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

import java.util.HashSet;
import java.util.Set;

import edu.ntua.isci.common.lp.Atom;
import edu.ntua.isci.common.lp.Variable;

import edu.ntua.isci.qa.algorithm.rapid.UnfoldedClause;
import edu.ntua.isci.qa.algorithm.rapid.ClauseUnfolder;

public class DLFastSubsumeChecker extends DLSubsumeChecker {
	
	public UnfoldedClause check(Atom head, DLIndexEntryList[] atomList, int[] index, boolean isSymmetric, ClauseUnfolder unf) {
		boolean pure = !isSymmetric;
		
		UnfoldedClause newClause = null;
		
		Set<DLIndexEntry> aies = new HashSet<DLIndexEntry>();
		Set<Set<DLIndexEntry>> roots = new HashSet<Set<DLIndexEntry>>();
		Set<DLIndexEntry> haveChildren = new HashSet<DLIndexEntry>();

		for (int j = 0; j < atomList.length; j++) {
			
			if (index[j] != -1) {
				DLIndexEntry aie = (DLIndexEntry)atomList[j].getList().get(index[j]);
				aies.add(aie);
				
				if (aie.roots != null) {
					DLIndexEntry unboundRoot = null; 

					for (DLIndexEntry ie : aie.roots) {
						boolean allUnbound = true;
						for (Variable v : ie.getAtom().getVariables()) {
							if (!v.hasUnboundPrefix()) {
								allUnbound = false;
								break;
							}
						}
						
						if (allUnbound) {
							unboundRoot = ie;
						}
					}
					
					if (unboundRoot == null) {
						roots.add(aie.roots);
					} else {
						Set<DLIndexEntry> ur = new HashSet<DLIndexEntry>();
						ur.add(unboundRoot);
						
						roots.add(ur);
						
						Set<DLIndexEntry> rootsCopy = (Set<DLIndexEntry>)((HashSet)aie.roots).clone();
						rootsCopy.remove(unboundRoot);
						
						if (!rootsCopy.isEmpty()) {
							roots.add(rootsCopy);
						}
					}
				}
				
				if (aie.children != null) {
					haveChildren.add(aie);
				}
			}
			
		}

		if (!roots.isEmpty()) {
			Set<DLIndexEntry>[] rootsArray = roots.toArray(new Set[] {});
			
			for (int i = 0; i < rootsArray.length; i++) {
				Set<DLIndexEntry> tmp = new HashSet<DLIndexEntry>();
				tmp.addAll(rootsArray[i]);

				Set<DLIndexEntry> ch = new HashSet<DLIndexEntry>(); 
				for (DLIndexEntry r : rootsArray[i]) {
					ch.addAll(r.children);
				}

				for (DLIndexEntry a : aies) {
					if (!ch.contains(a)) {
						tmp.add(a);
					}
				}

				Set<Integer> covered = new HashSet<Integer>();
				for (DLIndexEntry a : tmp) {
					covered.addAll(a.covered);
				}

				if (covered.size() == atomList.length) {
					return null;
				}
			}
		}
		
		if (newClause == null) {
			newClause = unf.createClause(adjustIndex(index), pure);
		}
		
		return newClause;
	}
}