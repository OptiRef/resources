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

package edu.ntua.isci.qa.lp.theory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.ntua.isci.common.lp.Atom;
import edu.ntua.isci.common.lp.Clause;
import edu.ntua.isci.common.lp.ClauseParser;
import edu.ntua.isci.common.lp.Function;
import edu.ntua.isci.common.lp.Predicate;
import edu.ntua.isci.common.lp.Substitution;
import edu.ntua.isci.common.lp.Term;
import edu.ntua.isci.common.lp.Variable;
import edu.ntua.isci.common.utils.SetUtils;

import edu.ntua.isci.qa.algorithm.rapid.FunctionMap;
import edu.ntua.isci.qa.algorithm.rapid.ResolveStruct;
import edu.ntua.isci.qa.algorithm.rapid.SortAtom;

public class FCTheory extends LogicProgram {

	private boolean isConjunctive;
	
	private Set<String> auxNames;
	private Set<String> fauxNames;
	
	
	public Map<Predicate, FunctionMap[]> extraFuncs;
	
	public FCTheory() {
		super();

		auxNames = new HashSet<>();
		fauxNames = new HashSet<>();
	}
	
	public FCTheory(Collection<Clause> set) {		
		this();
		
		for (Clause c : set) {
			addClause(c);
		}
	}	
	
	public FCTheory(String file) throws Exception {		
		this();
		
		BufferedReader pbf = new BufferedReader(new FileReader(file));
		ClauseParser cp = new ClauseParser();

		String pline;
		while ((pline = pbf.readLine()) != null) {
			if (pline.startsWith("%") || pline.trim().length() == 0) {
				continue;
			}
			
			addClause(cp.parseClause(pline));
		}	
		
		pbf.close();
	}	
	public boolean isConjuntive() {
		return isConjunctive;
	}
	
	public void addAUXNames(Set<String> names) {
		auxNames.addAll(names);
		
		fauxNames = new HashSet<>();
		for (Clause c: rules) {
			Atom a = c.getHead();
			if (auxNames.contains(a.getPredicate().getName()) && !a.isFunctionFree()) {
				fauxNames.add(a.getPredicate().getName());
			}
		}
	}
	
	public boolean isAUXAtom(Atom a) {
		return auxNames.contains(a.getPredicate().getName());
	}

	public boolean isFAUXAtom(Atom a) {
		return fauxNames.contains(a.getPredicate().getName());
	}
	
	public boolean addClause(Clause r) {
		//TODO: Check if r is valid
		Substitution subst = new Substitution();

		Set<Variable> uVars = r.getUnboundVariables();
		Atom head = r.getHead();
		Set<Variable> aVars = head.getVariables();
		
		if (r.getHead().isEqualityAtom()) {
			int k = 1;
			for (Variable v : aVars) {
				subst.put(v, new Variable(Variable.BODYVAR_PREFIX + (k++)));
			}
			
			k = 1;
			for (Variable v : SetUtils.difference(r.getBoundVariables(), aVars)) {
				subst.put(v, new Variable(Variable.HEADVAR_PREFIX + (k++)));
			}

		} else {
			int k = 1;
			for (Variable v : uVars) {
				subst.put(v, new Variable(Variable.UNBOUND_PREFIX + (k++)));
			}
	
			Variable fv = null;
			for (int i = 0; i < head.getPredicate().getArity(); i++) {
				if (head.getArgument(i).isFunctionalTerm()) {
					Variable v = head.getArgument(i).getVariables().iterator().next();
					subst.put(v, new Variable(Variable.HEADVAR_PREFIX + "1"));
					fv = v;
				}
			}
			
			k = 1;
			if (fv == null) {
				for (Variable v : aVars) {
					subst.put(v, new Variable(Variable.HEADVAR_PREFIX + (k++)));
				}
			}
			
			k = 1;
			for (Variable v : SetUtils.difference(r.getBoundVariables(), aVars)) {
				subst.put(v, new Variable(Variable.BODYVAR_PREFIX + (k++)));
			}
		}
		
		r = r.apply(subst);
		
		if (r.bodySize() > 1) {
			isConjunctive = true;
		}

		return super.addClause(r);
	}


	public ArrayList<ResolveStruct> addAtomsForFunctions(FunctionMap nf, ArrayList<Atom> body, Term substTerm) {
		ArrayList<ResolveStruct> res = new ArrayList<>();
		
		for (Map.Entry<Function, ArrayList<SortAtom>> fentry : nf.entrySet()) {
			for (SortAtom satoms : fentry.getValue()) {
				ArrayList<Atom> atoms = new ArrayList<>();
				for (Predicate p : satoms.getPredicates()) {
					atoms.add(new Atom(p, substTerm));
				}

				ArrayList<Atom> newBody = (ArrayList<Atom>)body.clone();
				for (Atom a : atoms) {
					if (!newBody.contains(a)) {
						newBody.add(a);
					}
				}
				
				res.add(new ResolveStruct(newBody, fentry.getKey()));
			}
		}

		return res;
	}

	public static FCTheory parseFile(String file) throws Exception {
		FCTheory res = new FCTheory();
		ClauseParser cp  = new ClauseParser();
		
		BufferedReader qbf = new BufferedReader(new FileReader(file));
		
		String qline;
		while ((qline = qbf.readLine()) != null) {
			if (qline.startsWith("%")) {
				continue;
			}
			res.addClause(cp.parseClause(qline));
		}
		
		qbf.close();

		return res;
	}

}
