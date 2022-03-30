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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.ntua.isci.qa.algorithm.PartialEngine;
import edu.ntua.isci.qa.algorithm.ResolveEngine;
import edu.ntua.isci.qa.algorithm.rapid.dllite.DLAtomUnfolder;
import edu.ntua.isci.qa.algorithm.rapid.elhi.ETDatalogFlattener;
import edu.ntua.isci.qa.rewriting.QueryCollector;
import edu.ntua.isci.common.lp.Atom;
import edu.ntua.isci.common.lp.Clause;
import edu.ntua.isci.common.lp.Constant;
import edu.ntua.isci.common.lp.Function;
import edu.ntua.isci.common.lp.FunctionalTerm;
import edu.ntua.isci.common.lp.Predicate;
import edu.ntua.isci.common.lp.Substitution;
import edu.ntua.isci.common.lp.Term;
import edu.ntua.isci.common.lp.Tuple;
import edu.ntua.isci.common.lp.Variable;
import edu.ntua.isci.common.utils.Counter;
import edu.ntua.isci.common.utils.SetUtils;

public abstract class Rapid extends PartialEngine implements ResolveEngine {

	public static String DUMMY_VAR_PREFIX = "v";
	public static String JOIN_VAR_PREFIX = "r";
	public static String EQ_VAR_PREFIX = "e";
	
	public static Variable UNBOUND_VARIABLE = new Variable(Variable.UNBOUND_PREFIX + "1");

	public static Variable[] fixedUnboundVars; 
	public static Variable[] extraUnboundVars;

	public static String FIXED_UNBOUND_PREFIX = "_f";
	public static String EXTRA_UNBOUND_PREFIX = "_w";

	public static ArrayList<Variable> dummyVars = new ArrayList<>();
	public static ArrayList<Variable> joinDummyVars = new ArrayList<>();

	public static Set<Term> dummyTerms;

	public static int expandMode = 2;
	
	public static int fcounter;
	
	static {
		fixedUnboundVars = new Variable[2];
		extraUnboundVars = new Variable[2];
		                
		for (int j = 0; j < 2; j++) {
			dummyVars.add(new Variable(DUMMY_VAR_PREFIX + (j + 1)));
			joinDummyVars.add(new Variable(JOIN_VAR_PREFIX + (j + 1)));
			fixedUnboundVars[j] = new Variable(FIXED_UNBOUND_PREFIX + (j + 1));
			extraUnboundVars[j] = new Variable(EXTRA_UNBOUND_PREFIX + (j + 1));
		}			
		
		dummyTerms = new HashSet<Term>(Rapid.dummyVars);
	}

	public static boolean isJoinVariable(Variable v) {
		return v.getName().startsWith(JOIN_VAR_PREFIX);
	}

	public static boolean isCoreVariable(Variable v) {
		return !(isJoinVariable(v) || v.hasUnboundPrefix());
	}

	public static Substitution replaceUnboundVariablesByUnderscoreSubstitution(Clause c) {
		Substitution subst = new Substitution();
	
		Set<Variable> uVars = c.getUnboundVariables(); 
		
		for (Atom atom : c.getBody()) {
			Term[] terms = atom.getArguments().getTerms();
			for (int i = 0; i < terms.length; i++) {
				if (uVars.contains(terms[i])) {
					subst.put((Variable)terms[i], Rapid.fixedUnboundVars[i]);
				}
			}
		}
		
		return subst;
	}
	
	protected ClauseUnfolder clauseUnfolder;
	public AtomUnfolder atomUnfolder;
	protected Flattener flattener;
	
	protected boolean unfold;
	
	private Set<Clause> shrinkedClauses;
	
	protected Rapid(String name, boolean unfold) {
		super(name);
		
		this.unfold = unfold;
	}
	
	public AtomUnfolder getAtomUnfolder() {
		return atomUnfolder;
	}

	protected boolean preinit = false;
	
	public void preinit(Collection<Clause> atomClauses) {
		preinit = true;
		initialize();
		
		getAtomUnfolder().precompute(atomClauses);
	}
	
	public void execute() {
		atomUnfolder.setQueryPredicate(queryPredicate);

		Counter rmark = new Counter(0);
		
		FlattenerExtra extra = new FlattenerExtra();
		
		while(true) {
			if (!saturateQ(extra)) {
				break;
			}
			atomUnfolder.reset();
		}
		
		ArrayList<MarkedClause> queue = new ArrayList<>();
		queue.add(new MarkedClause(query, rmark.getValue()));
		
		
		shrinkedClauses = new HashSet<>();
		shrinkedClauses.add(query);
		
		int k = 0;
		while (k < queue.size()) {
			Clause currentQuery = queue.get(k++).getClause(); 
			
			for (Variable v : SetUtils.difference(currentQuery.getBoundVariables(), currentQuery.getHead().getVariables())) {
				rmark.increase();
				
				for (MarkedClause c : resolve(currentQuery, v, rmark, extra)) {
					if (shrinkedClauses.add(c.getClause())) {
						queue.add(c);
					}
				}
			}
		}
		
		if (unfold) {
			int id = unfold(clauseUnfolder, qc, queue, extra);
			
			for (MarkedClause c : flattener.getExtraQueries(atomUnfolder, clauseUnfolder, extra)) {
				qc.add(id + c.getMark(), c.getClause());
			}
			
		} else {
			int id = expand(strip(queue), extra);
			
			for (Clause cc : lp.getEqualityHeadClauses()) {
				qc.add(id, cc);
			}
		}
	}
	
	public static int unfold(ClauseUnfolder au, QueryCollector qc, Clause query, FlattenerExtra extra) {
		ArrayList<MarkedClause> cList = new ArrayList<MarkedClause>();
		cList.add(new MarkedClause(query, 0));
		
		return unfold(au, qc, cList, extra);
	}
	
	protected static int unfold(ClauseUnfolder cu, QueryCollector qc, ArrayList<MarkedClause> queryList, FlattenerExtra extra) {
		queryList = Clause.getNonRedundantClauses(queryList);
		
		int id = 0;
		
		int k = 0;
		while (k < queryList.size()) {
			MarkedClause mc = queryList.get(k++);
			
			Set<Atom> appendix = new HashSet<Atom>();
			int eIndex = -1;

			if (cu.getAtomUnfolder() instanceof DLAtomUnfolder) {
				while (k < queryList.size() && queryList.get(k).getMark() == mc.getMark()) {
					Clause nc = queryList.get(k++).getClause();
					if (eIndex == -1) {
						eIndex = nc.bodySize() - 1;
					} else if (eIndex > -1 && nc.bodySize() - 1 != eIndex) {
						throw new RuntimeException("ERROR!!!");
					} 
					
					appendix.add(nc.getBodyAtomAt(eIndex));
				}
			}
			
			ArrayList<Clause> last = new ArrayList<Clause>();

			Set<Variable> topBoundVars = mc.getClause().getBoundVariables();

			for (UnfoldedClause cp : cu.unfold(mc.getClause(), eIndex, appendix, extra)) {
				Clause up = cp.getClause().condenseWithOriginalVariables();
				if (cp.isPure() && up.getBoundVariables().containsAll(topBoundVars)) {
					qc.add(id, up, cp.isTop());
				} else {
					last.add(up);
				}
			}
			
			id++;
			for (Clause extraClause : last) {
				qc.add(id++, extraClause);
			}
		}
		
		return id;
	}
	
	public Set<Clause> getShrinkedClauses() {
		return shrinkedClauses;
	}

	public boolean isCGLLR() {
		return false;
	}
	
	protected abstract ArrayList<Clause> strip(ArrayList<MarkedClause> queryList);
	
	protected int expand(ArrayList<Clause> queries, FlattenerExtra extra) {
		if (expandMode == 1) {
			return expand1(queries, extra);
		} else {
			return expand2(queries, extra);
		}
	}
	
	protected int expand1(ArrayList<Clause> queries, FlattenerExtra extra) {
		Set<DatalogAtom> lsAtom = new HashSet<>();
		
		for (int k = 0; k < queries.size(); k++) {
			Clause c = queries.get(k);

			Set<Term> boundVars = c.getBoundTerms();
			ArrayList<Atom> newBody = new ArrayList<Atom>();
			
			for (Atom atom : c.getBody()) {
				DatalogAtom res = normalizeAtom(atom, boundVars);
				newBody.add(res.substAtom);
				lsAtom.add(res);
			}

			qc.add(0, new Clause(query.getHead(), newBody));
		}

		for (DatalogAtom da : lsAtom) {
			Atom[] aunf = atomUnfolder.datalogComputeUnfolding(da.rightAtom, Rapid.dummyVars, null, extra);

			for (Atom body : aunf) {
				if (!body.equals(da.rightAtom)) {
					qc.add(1, new Clause(da.leftAtom, body));
				}
			}
		}
		
		for (MarkedClause mc : flattener.getExtraQueries(atomUnfolder, clauseUnfolder, extra)) {
			Clause c  = mc.getClause();
			Set<Term> bTerms = c.getBoundTerms();
			ArrayList<Atom> newBody = new ArrayList<Atom>();
			for (Atom atom : c.getBody()) {
				DatalogAtom res = normalizeAtom(atom, bTerms);
				newBody.add(res.substAtom);
				lsAtom.add(res);
			}

			qc.add(1, new Clause(c.getHead(), newBody));
		}

		for (DatalogAtom da : lsAtom) {
			Clause ec = da.toDatalogClause();
			if (ec != null) {
				qc.add(1, ec);
			}
		}
		
		return 2;
	}

	protected int expand2(ArrayList<Clause> queries, FlattenerExtra extra) {
		Set<DatalogAtom> lsAtom = new HashSet<>();
		
		
		for (int k = 0; k < queries.size(); k++) {
			Clause c = queries.get(k);
			Set<Term> boundVars = c.getBoundTerms();
			ArrayList<Atom> newBody = new ArrayList<Atom>();
			
			for (Atom atom : c.getBody()) {
				DatalogAtom res = normalizeAtom(atom, boundVars);
				newBody.add(res.substAtom);
				lsAtom.add(res);
			}

			qc.add(0, new Clause(c.getHead(), newBody));
		}

		for (DatalogAtom da : lsAtom) {
			Atom[] aunf = atomUnfolder.datalogComputeUnfolding(da.rightAtom, Rapid.dummyVars, null, extra);
			
			if (!da.leftAtom.getPredicate().equals(da.rightAtom.getPredicate())) {
				qc.add(1, new Clause(da.leftAtom, da.rightAtom));
			}
			
			for (Atom body : aunf) {
				if (!body.equals(da.rightAtom)) {
					qc.add(1, new Clause(da.leftAtom, body));
				}
			}
		}
		
		for (MarkedClause mc : flattener.getExtraQueries(atomUnfolder, clauseUnfolder, extra)) {
			Clause c  = mc.getClause();
			qc.add(1, c);
		}

		for (DatalogAtom da : lsAtom) {
			ArrayList<SortAtom> newAtoms = ((ETDatalogFlattener)flattener).unboundRoleMap.get(da.rightAtom);
			if (newAtoms != null) {
				for (SortAtom a: newAtoms){
					for (Variable v : da.leftAtom.getVariables()) {
						if (!v.hasUnboundPrefix()) {
							qc.add(1, new Clause(da.leftAtom, a.toAtomArray(v)));
							break;
						}
					}
				}
			}
		}
		
		for (Clause c: Clause.getNonRedundantClauses(extra.getFClauses())) {
			qc.add(2, c);
		}
		
		for (Function f : extra.getFSet()) {
			for (Clause c : lp.getClausesForFunction(f)) {
				qc.add(2, c);
			}
		}
		
		
		return 3;
	}

	
	public ArrayList<MarkedClause> resolve(Clause currentQuery, Variable v, Counter mark, FlattenerExtra extra) {
		return resolve(currentQuery, v, mark, extra, false);
	}
	
	public ArrayList<MarkedClause> resolve(Clause currentQuery, Variable v, Counter mark, FlattenerExtra extra, boolean allowHeadVars) {
		ArrayList<MarkedClause> res = new ArrayList<MarkedClause>();
		
		Set<Variable> headVars = currentQuery.getHead().getVariables();
		if (!allowHeadVars && headVars.contains(v)) {
			return res;
		}
		
		Set<Term> boundTerms = currentQuery.getBoundTerms();

		Set<Function> addFunctionSet = null;
		FunctionMap functionSet = null;
		Set<Variable> otherVariables = new HashSet<Variable>();
		Set<Constant> otherConstants = new HashSet<Constant>();

		int[] pattern = currentQuery.termIndexPattern(v);

		for (int k = 0; k < pattern.length; k++) {
			if (pattern[k] == 0) {
				continue;
			} else if (pattern[k] == 3) {
				return res;
			}
			
			Atom atom = currentQuery.getBodyAtomAt(k);

			AtomUnfolding aur = atomUnfolder.getUnfolding(atom, boundTerms, null, extra).getUnfolding();
			
			FunctionMap newFunctions = null;

			if (atom.isConcept()) {
				newFunctions = aur.getFunctionMap(0);
			} else {
				Term otherTerm = null;
			
				if (pattern[k] == 1) {
					newFunctions = aur.getFunctionMap(0);
					if (boundTerms.contains(atom.getArgument(1))) {
						otherTerm = atom.getArgument(1);
					}
				} else if (pattern[k] == 2) {
					newFunctions = aur.getFunctionMap(1);
					if (boundTerms.contains(atom.getArgument(0))) {
						otherTerm = atom.getArgument(0);
					}
				}
				
				if (otherTerm != null) {
					if (otherTerm.isVariable()) {
						otherVariables.add((Variable)otherTerm);
					} else  if (otherTerm.isConstant()) {
						otherConstants.add((Constant)otherTerm);
					}
				}
			}

			if (otherConstants.size() > 1) {
				return res;
			}
			
			if (functionSet == null) {
				functionSet = newFunctions.copy();
			} else {
				functionSet.intersection(newFunctions);
			}
			
			if (functionSet.isEmpty()) {
				return res;
			}
		}
		
		if (functionSet == null) {
			return res;
		}
		
		if (addFunctionSet != null) {
			for (Iterator<Map.Entry<Function, ArrayList<SortAtom>>> iter = functionSet.entrySet().iterator();  iter.hasNext();) {
				Map.Entry<Function, ArrayList<SortAtom>> entry = iter.next();
				if (!addFunctionSet.contains(entry.getKey())) {
					iter.remove();
				}
			}
		}

		Term substTerm = null;
		Substitution substi = new Substitution();
		
		Set<Variable> allVariables = currentQuery.getVariables();

		if (otherConstants.size() == 0) {
			if (otherVariables.size() > 0) {
				Iterator<Variable> iter = otherVariables.iterator();
				substTerm = iter.next();
				while (!headVars.contains(substTerm) && iter.hasNext()) {
					substTerm = iter.next();
				}
			} else {
				substTerm = Variable.getNewVariable(Variable.UNBOUND_PREFIX, allVariables);
				allVariables.add((Variable)substTerm);
			}
		} else {
			substTerm = otherConstants.iterator().next();
		}

		for (Variable vv : otherVariables) {
			if (!vv.equals(substTerm)) {
				substi.put(vv, substTerm);
			}
		}

		ArrayList<Atom> baseBody = new ArrayList<Atom>();
		for (int k = 0; k < currentQuery.bodySize(); k++) {
			if (pattern[k] == 0) {
				baseBody.add(currentQuery.getBodyAtomAt(k).apply(substi));
			}
		}
		
		ArrayList<Clause> last = new ArrayList<>();
		
		Atom head = currentQuery.getHead();
		
		for (ResolveStruct rs : lp.addAtomsForFunctions(functionSet, baseBody, substTerm)) {
			ArrayList<Atom> newBody = rs.getAtoms();
		
			Atom newHead;
			if (allowHeadVars && head.getVariables().contains(v)) {
				Substitution subst = (Substitution)substi.clone();
				subst.put(v, new FunctionalTerm(rs.getFunction(), substTerm));
				newHead = head.apply(subst);
				
				if (rejectEq(head, newHead)) {
					continue;
				}
			} else {
				newHead = head.apply(substi);
			}
			
			int s1 = newBody.size();
			Clause newClause = new Clause(newHead, newBody).condense();
			
			if (s1 == newClause.bodySize() && s1 >= baseBody.size() + 1) {
				res.add(new MarkedClause(newClause, mark.getValue()));
			} else if (s1 <= baseBody.size()) {
				res.clear();
				res.add(new MarkedClause(newClause, mark.getValue()));
				last.clear();
				break;
			} else {
				last.add(newClause);
			}
		}
		
		for (Clause c : last) {
			res.add(new MarkedClause(c, mark.increaseUse()));
		}
		
		res = Clause.getNonRedundantClauses(res);

		return res;
	}

	public boolean rejectEq(Atom head, Atom newHead) {
		if (head.isEqualityAtom()) {
			if (head.getArgument(0).isVariable() && head.getArgument(1).isVariable() && newHead.getArgument(1).isVariable()) {
			} else if (newHead.getArgument(0).isFunctionalTerm() && newHead.getArgument(1).isFunctionalTerm() && newHead.getArgument(0).equals(newHead.getArgument(1))) {
				return true;
			}
		}
		
		return false;
	}

	public static DatalogAtom normalizeAtom(Atom atom, Collection<Term> boundVars) {
		
		Predicate atomPred = atom.getPredicate();
		Predicate newPred = null;
		Atom clauseAtom = null;
		Atom unfoldAtom = null;
		Atom rootAtom = null;

		boolean u1 = false;
		boolean u2 = false;

		if (!boundVars.contains(atom.getArgument(0))) {
			u1 = true;
		}

		if (atom.isConcept()) {
			if (u1) {
				newPred = new Predicate(atomPred.getName() + "_u", 0);
				
				clauseAtom = new Atom(newPred, new Tuple());
				rootAtom = new Atom(newPred, new Tuple());
				unfoldAtom = new Atom(atomPred, new Tuple(Rapid.fixedUnboundVars[0]));
				
			} else {
				newPred = atomPred;
				
				clauseAtom = new Atom(newPred, atom.getArguments());
				rootAtom = new Atom(newPred, new Tuple(Rapid.dummyVars.get(0)));
				unfoldAtom = new Atom(atomPred, new Tuple(Rapid.dummyVars.get(0)));
			}
		} else { 
			
			if (atom.getArguments().size() > 1 && !boundVars.contains(atom.getArgument(1))) {
				u2 = true;
			}
			
			if (u1 && u2) {
				newPred = new Predicate(atomPred.getName() + "_uu", 0);

				clauseAtom = new Atom(newPred, new Tuple());
				rootAtom = new Atom(newPred, new Tuple());
				unfoldAtom = new Atom(atomPred, new Tuple(Rapid.fixedUnboundVars[0], Rapid.fixedUnboundVars[1]));
				
			} else if (u1) {
				newPred = new Predicate(atomPred.getName() + "_u.", 1);

				clauseAtom = new Atom(newPred, new Tuple(atom.getArgument(1)));
				rootAtom = new Atom(newPred, new Tuple(Rapid.dummyVars.get(1)));
				unfoldAtom = new Atom(atomPred, new Tuple(Rapid.fixedUnboundVars[0], Rapid.dummyVars.get(1)));
				
			} else if (u2) {
				newPred = new Predicate(atomPred.getName() + "_.u", 1);

				clauseAtom = new Atom(newPred, new Tuple(atom.getArgument(0)));
				rootAtom = new Atom(newPred, new Tuple(Rapid.dummyVars.get(0)));
				unfoldAtom = new Atom(atomPred, new Tuple(Rapid.dummyVars.get(0), Rapid.fixedUnboundVars[1]));
			} else {
				newPred = atomPred;
				
				clauseAtom = new Atom(newPred, atom.getArguments());
				rootAtom = new Atom(newPred, new Tuple(Rapid.dummyVars.get(0), Rapid.dummyVars.get(1)));
				unfoldAtom = new Atom(atomPred, new Tuple(Rapid.dummyVars.get(0), Rapid.dummyVars.get(1)));
			}
		}

		return new DatalogAtom(clauseAtom, rootAtom, unfoldAtom);
	}
	
	public static Clause adjustVariables(Clause c) {
		
		Set<Variable> vars = c.getVariables();
		
		Counter cc = new Counter(0);
		
		Substitution s = new Substitution();
		for (Variable v : c.getBoundVariables()) {
			if (v.hasUnboundPrefix()) {
				s.put(v, Variable.getNewVariable("v", vars, cc));
			}
		}
		
		return c.apply(s);
	}

	private boolean saturateQ(FlattenerExtra extra) {
		Counter rmark = new Counter(0);
		
		Set<Clause> computedClauses = new HashSet<>();

		ArrayList<MarkedClause> queue = new ArrayList<>();
		for (Clause c : lp.getEqualityHeadClauses()) {
			queue.add(new MarkedClause(c, rmark.getValue()));
			computedClauses.add(c);
		}
		
		int top = queue.size();
				
		int k = 0;
		while (k < queue.size()) {
			Clause q = queue.get(k++).getClause();
			
			Set<Variable> vars = q.getHead().getVariables();

			int cv = 0;
			for (Variable v : vars) {
				rmark.increase();
				for (MarkedClause c : resolve(q, v, rmark, extra, true)) {
					if (computedClauses.add(c.getClause())) {
						if (k <= top && cv == 0 || k > top) {
							queue.add(c);
						} 
					}
					if (k <= top) {
						extra.addFClause(c.getClause());
					}
				}
				cv++;
			}
		}
		
		Map<Predicate, FunctionMap[]> extraFuncs = new HashMap<>();
		boolean newClauses = false;

		Set<Atom> heads = new HashSet<>();
		
		queue = Clause.getNonRedundantClauses(queue);
		
		for (int jj = 0; jj < queue.size(); jj++) {
			Clause c = queue.get(jj).getClause();

			Term t0 = c.getHead().getArgument(0);
			Term t1 = c.getHead().getArgument(1);
			
			if (t0.depth() == 0 && t1.depth() == 0) {
				continue;
			} else if (t0.depth() == 1 && t1.depth() == 1 && !t0.equals(t1)) {
				Function[] fs = new Function[] {((FunctionalTerm)t0).getFunction(), ((FunctionalTerm)t1).getFunction() };

				SortAtom sa = SortAtom.createSortAtom(c.getBody());
				ArrayList<SortAtom> asa = new ArrayList<>();
				asa.add(sa);
				
				for (int j = 0; j < 1; j++) {
					int j2 = j == 0? 1 : 0;
				
					for (Clause cc : lp.getClausesForFunction(fs[j])) {
						Atom head = cc.getHead();
	
						heads.add(head);
						
						FunctionMap nf = new FunctionMap();
						nf.put(fs[j2], asa);
						
						FunctionMap fm[] = extraFuncs.get(head.getPredicate());
						if (fm == null) {
							fm = new FunctionMap[] { new FunctionMap(), new FunctionMap() };
							extraFuncs.put(head.getPredicate(), fm);
						}
						
						for (int i = 0; i < head.getPredicate().getArity(); i++) {
							if (head.getArgument(i).isFunctionalTerm()) {
								fm[i].union(nf);
							}
						}
					}
				}
			} else {
				FunctionalTerm ft = null;
				
				if (t1.depth() == 2 && !t0.isFunctionalTerm()) {
					ft = (FunctionalTerm)t1;
				} else if (t0.depth() == 2 && !t1.isFunctionalTerm()) {
					ft = (FunctionalTerm)t0;
				} 
				
				if (ft != null) {
					SortAtom sa = SortAtom.createSortAtom(c.getBody());
					
					for (Clause cc : lp.getClausesForFunction(ft.getFunction())) {
						Atom head = cc.getHead();
						
						heads.add(head);
						
						int i = 0;
						int i2 = 0;
						
						if (head.isRole()) {
							if (head.getArgument(1).isFunctionalTerm()) {
								i = 1;
							}
							i2 = i == 0 ? 1 : 0;						
						}
	
						FunctionMap nf = new FunctionMap();
							
						Function fs2 = ((FunctionalTerm)ft.getArguments().getTerm(0)).getFunction();
						
						ArrayList<SortAtom> asa = new ArrayList<>();
						asa.add(sa);
						nf.put(fs2, asa);
							
						Atom at = cc.getBody().get(0);
						
						///??????????????? by unfolding
						for (Clause fc : lp.getClausesForFunction(fs2)) {
							if (fc.getHead().isConcept() && fc.getHead().getPredicate().equals(at.getPredicate())) {
								for (Atom p : fc.getBody()) {
									sa.add(p.getPredicate());
								}
								break;
							}
						}
						
						if (head.isRole()) {
							FunctionMap fm[] = extraFuncs.get(head.getPredicate());
							if (fm == null) {
								fm = new FunctionMap[] { new FunctionMap(), new FunctionMap() };
								extraFuncs.put(head.getPredicate(), fm);
							}
							
							fm[i2].union(nf);
						} else {
							Variable v = new Variable("x");
							Clause nc = new Clause(new Atom(head.getPredicate(), v), sa.toAtomArray(v));
							boolean addClause = lp.addClause(nc);
							newClauses |= addClause;
						}
					}
				}
			}
		}
		
		Map<Predicate, FunctionMap[]> oldFuncs = lp.extraFuncs; 
		lp.extraFuncs = extraFuncs;
		
		if (newClauses) {
			return true;
		}
		
		if (extraFuncs.isEmpty()) {
			return false;
		} else if (oldFuncs == null || oldFuncs.isEmpty()) {
			return true;
		} else {
			Set<Predicate> p1 = oldFuncs.keySet();
			Set<Predicate> p2 = extraFuncs.keySet();
			
			if (!SetUtils.areEqual(p1, p2)) {
				return true;
			} else {
				for (Predicate p : p1) {
					FunctionMap[] fm1 = oldFuncs.get(p); 
					FunctionMap[] fm2 = extraFuncs.get(p);
					
					for (int i = 0; i < 2; i++) {
						if (!fm1[i].equalsFM(fm2[i])) {
							return true;
						}
					}
				}
			}
		}
		
		return false;

	}


}

