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

import edu.ntua.isci.common.lp.Atom;
import edu.ntua.isci.common.lp.Function;

public class ResolveStruct {
	private Function func;
	private ArrayList<Atom> atoms;
	
	public ResolveStruct(ArrayList<Atom> atoms, Function f) {
		this.atoms = atoms;
		this.func = f; 
	}
	
	public ArrayList<Atom> getAtoms() {
		return atoms;
	}
	
	public Function getFunction() {
		return func;
	}
	
	public String toString() {
		return func + " : " + atoms;
	}
}
