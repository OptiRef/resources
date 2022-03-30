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
import java.util.Map;
import java.util.Set;

import edu.ntua.isci.common.lp.Function;

public abstract class AtomUnfolding {
	
	protected FunctionMap[] argFunctions;
	
	protected AtomUnfolding(FunctionMap[] argFunctions) {
		this.argFunctions = argFunctions;
	}

	public FunctionMap[] getFunctionMap() {
		return argFunctions;
	}

	public FunctionMap getFunctionMap(int arg) {
		return argFunctions[arg];
	}

	public void setFunctionMap(int arg, FunctionMap fm) {
		argFunctions[arg] = fm;
	}
	
	public Set<Function> getAddFunctions() {
		
		Set<Function> newFunctions = new HashSet<>();
		
		for (int i = 0; i < 2; i++) {
			for (Map.Entry<Function, ArrayList<SortAtom>> entry : argFunctions[i].entrySet()) {
				for (SortAtom sa : entry.getValue()) {
					if (sa.imode) {
						newFunctions.add(entry.getKey());
						break;
					}
				}
			}
		}
		
		return newFunctions;
	}
	
}
