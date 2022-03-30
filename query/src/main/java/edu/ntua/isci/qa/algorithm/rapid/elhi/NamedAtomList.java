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

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import edu.ntua.isci.common.lp.Atom;
import edu.ntua.isci.common.lp.Term;
import edu.ntua.isci.common.lp.Variable;


public class NamedAtomList {
	private String prefix;
	private String index;
	
	private Set<Atom> atoms;
	private Set<Term> terms;
	private Set<Variable> vars;
	
	public NamedAtomList(String prefix) {
		this.prefix = prefix;
		
		this.atoms = new TreeSet<Atom>();
		this.terms = new HashSet<Term>();
		this.vars = new HashSet<Variable>();
	}
	
	public void add(Atom a) {
		atoms.add(a);
		terms.addAll(a.getTerms());
		vars.addAll(a.getVariables());
	}
	
	public String getPrefix() {
		return prefix;
	}

	public String getIndexedPrefix() {
		return index + prefix;
	}

	public void setIndex(int i) {
		this.index = i + "";
	}
	
	public Set<Atom> getAtoms() {
		return atoms;
	}
	
	public Set<Term> getTerms() {
		return terms;
	}
	
	public Set<Variable> getVariables() {
		return vars;
	}

}
