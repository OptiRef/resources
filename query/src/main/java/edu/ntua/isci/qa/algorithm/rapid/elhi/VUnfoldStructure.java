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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.ntua.isci.qa.algorithm.rapid.AtomUnfolder;
import edu.ntua.isci.qa.algorithm.rapid.FunctionMap;
import edu.ntua.isci.qa.algorithm.rapid.Rapid;
import edu.ntua.isci.qa.algorithm.rapid.elhi.HUnfoldStructure.CommonFunctionResult;
import edu.ntua.isci.qa.lp.theory.FCTheory;
import edu.ntua.isci.common.lp.Atom;
import edu.ntua.isci.common.lp.AtomArray;
import edu.ntua.isci.common.lp.Clause;
import edu.ntua.isci.common.lp.Function;
import edu.ntua.isci.common.lp.FunctionalTerm;
import edu.ntua.isci.common.lp.Predicate;
import edu.ntua.isci.common.lp.Substitution;
import edu.ntua.isci.common.lp.Term;
import edu.ntua.isci.common.lp.Tuple;
import edu.ntua.isci.common.lp.Variable;
import edu.ntua.isci.common.utils.Counter;
import edu.ntua.isci.common.utils.SetUtils;

public class VUnfoldStructure {
	private Atom top;
	
	private ArrayList<HUnfoldStructure> content;
	private ArrayList<Counter> order;
	
	private FunctionMap[] argFunctions;
	private FunctionMap[] tmpFunctions;
	
	public boolean isInBlock;
	private Set<Integer> cycles;
	
	private boolean findCommon;
	
	public VUnfoldStructure(boolean findCommon) {
		this.findCommon = findCommon;
		content = new ArrayList<HUnfoldStructure>();
		order = new ArrayList<Counter>();
		
		argFunctions = new FunctionMap[] { new FunctionMap(), new FunctionMap() };

		cycles = new HashSet<Integer>();
	}

	public ArrayList<HUnfoldStructure> getElements() {
		return content;
	}
	
	public Atom getTopAtom() {
		return top;
	}
	
	public Set<Integer> getCycleIds() {
		return cycles;
	}

	public void addCycleId(int i) {
		cycles.add(i);
	}

	public boolean isInBlockingPath() {
		return isInBlock;
	}
	
	private boolean alreadyContainsHUS(int pos, HUnfoldStructure ehus) {
		if (pos >= 0) {
			ArrayList<Atom> eatoms = ehus.toConjunction();
			while (pos < content.size() && content.get(pos).compareTo(ehus) == 0) {
				if (content.get(pos).toConjunction().equals(eatoms)) {
					return true;
				}
				pos++;
			}
		}
		
		return false;
	}
	
	
	private void processArrayList(FunctionMap[] aF, FCTheory fullProgram, ArrayList<ArrayList<Atom>> atomList, Counter newUsedBoundVarsCounter, Counter newUsedUnboundVarsCounter, ArrayList<VUnfoldStructure> backAtoms, ArrayList<Substitution> backSubsts, Map<Atom, VUnfoldStructure> tmpMap, ETAtomUnfolder eau) {

		for (int t = 0; t < atomList.size(); t++) {
			ArrayList<Atom> atoms = atomList.get(t); 

			HUnfoldStructure hus = new HUnfoldStructure(findCommon);
			
			FunctionMap[] cf = hus.populate(fullProgram, atoms, newUsedBoundVarsCounter, newUsedUnboundVarsCounter, backAtoms, backSubsts, tmpMap, eau);
			
			int pos = binarySearch(hus);

			if (pos < 0 || !alreadyContainsHUS(pos, hus)) {
				if (pos < 0) {
					pos = -pos -1;
				}
				content.add(pos, hus);
				for (int k = 0; k < order.size(); k++) {
					if (order.get(k).getValue() >= pos) {
						order.get(k).increase();
					}
				}
				order.add(new Counter(pos));
				
				isInBlock |= hus.isInBlockingPath();
				
				for (int i = 0; i < cf.length; i++) {
					if (cf[i] != null) {
						aF[i].union(cf[i]);
					}
				}
				
				for (Variable v : AtomArray.getVariables(atoms)) {
					if (v.hasUnboundPrefix() || !Rapid.isJoinVariable(v)) {
						continue;
					}
		
					FunctionMap fs = hus.usedFunctions.get(v);
					if (fs == null) {
						fs = new FunctionMap();
						hus.usedFunctions.put(v, fs);
					}

					CommonFunctionResult cfr = hus.getCommonFunctions(v);
					fs.union(cfr.fm);

					atomList.addAll(cfr.fm.createBodies(cfr.body, cfr.subst));
				}
			}
		}
	}

	private ArrayList<Atom> unboundIntersection(ArrayList<Atom> res, ArrayList<Atom> newAtoms) {

		ArrayList<Atom> kept = new ArrayList<>();
		
		for (Atom newAtom : newAtoms) {
			Predicate pred = newAtom.getPredicate();
			int p = Atom.binarySearch(res, pred);
			if (p >= 0) {
				while (p < res.size() && res.get(p).getPredicate().equals(pred)) {
					Atom ex = res.get(p); 
					if (ex.unboundEquals(newAtom)) {
						kept.add(ex);
						break;
					}
					
					p++;
				}
			}
		}
		
		return kept;
	}
	
	private ArrayList<Atom> unboundDifference(ArrayList<Atom> res, ArrayList<Atom> newAtoms) {
		ArrayList<Atom> kept = new ArrayList<>();
		kept.addAll(res);
		
		for (Atom newAtom : newAtoms) {
			Predicate pred = newAtom.getPredicate();
			int p = Atom.binarySearch(kept, pred);
			if (p >= 0) {
				while (p < res.size() && res.get(p).getPredicate().equals(pred)) {
					if (kept.get(p).unboundEquals(newAtom)) {
						kept.remove(p);
						break;
					}
					
					p++;
				}
			}
		}
		
		return kept;
	}

	public boolean unfold(FCTheory program, Atom searchAtom, Counter newUsedBoundVarsCounter, Counter newUsedUnboundVarsCounter, Map<Atom, VUnfoldStructure> tmpMap, ETAtomUnfolder map) {
		ArrayList<Substitution> backSubsts = new ArrayList<>();
		backSubsts.add(new Substitution());
		
		return unfold(program, searchAtom, newUsedBoundVarsCounter, newUsedUnboundVarsCounter, new ArrayList<VUnfoldStructure>(), backSubsts, tmpMap, map);
	}
	
	public boolean unfold(FCTheory program, Atom searchAtom, Counter newUsedBoundVarsCounter, Counter newUsedUnboundVarsCounter, ArrayList<VUnfoldStructure> backAtoms, ArrayList<Substitution> backSubsts, Map<Atom, VUnfoldStructure> tmpMap, ETAtomUnfolder map) {
    	top = searchAtom;

    	ArrayList<VUnfoldStructure> newBackAtoms = (ArrayList<VUnfoldStructure>)backAtoms.clone();
		newBackAtoms.add(this);

		processArrayList(argFunctions, program, 
				         generateUnfoldings(program, searchAtom, argFunctions, newUsedBoundVarsCounter, newUsedUnboundVarsCounter), 
				         newUsedBoundVarsCounter, newUsedUnboundVarsCounter, newBackAtoms, backSubsts, tmpMap, map);
		
		return isInBlock;
    }

	public void traverse(Map<Atom, VUnfoldStructure> tmpMap) {
		for (int ti = 0; ti < content.size(); ti++) {
			HUnfoldStructure hus = content.get(order.get(ti).getValue());

			for (int j = 0; j < hus.size(); j++) {
				VUnfoldStructure nvus = hus.getElementAt(j);
				Atom cAtom = nvus.getTopAtom();
				
				if (tmpMap.get(cAtom) == null) {
					tmpMap.put(cAtom, nvus);
					nvus.traverse(tmpMap);
				}
			}
		}
    }

	
	public boolean iunfold(FCTheory program, Counter newUsedBoundVarsCounter, Counter newUsedUnboundVarsCounter, Map<Atom, VUnfoldStructure> tmpMap, ETAtomUnfolder map) {
		Counter c = new Counter(0);

		for (VUnfoldStructure v : tmpMap.values()) {
			v.tmpFunctions = null;
		}

		ArrayList<Substitution> backSubsts = new ArrayList<Substitution>();
		backSubsts.add(new Substitution());

		iunfold(c, program, newUsedBoundVarsCounter, newUsedUnboundVarsCounter, new ArrayList<VUnfoldStructure>(), backSubsts, tmpMap, map); 
		
		return c.getValue() > 0;
	}

	private FunctionMap[] iunfold(Counter count, FCTheory program, Counter newUsedBoundVarsCounter, Counter newUsedUnboundVarsCounter, ArrayList<VUnfoldStructure> backAtoms, ArrayList<Substitution> backSubsts, Map<Atom, VUnfoldStructure> tmpMap, ETAtomUnfolder map) {

		if (tmpFunctions != null) {
			return tmpFunctions;
		}
		
		ArrayList<VUnfoldStructure> newBackAtoms = (ArrayList<VUnfoldStructure>)backAtoms.clone();
		newBackAtoms.add(this);
		
		tmpFunctions = new FunctionMap[] { new FunctionMap(), new FunctionMap() };
		
		for (int i = 0; i < content.size(); i++) {
			HUnfoldStructure hus = content.get(i);
			
			ArrayList<Atom> firstAtoms = hus.toConjunction();
			
			if (hus.getBlockedIndices() != null) {

				for (Variable v : AtomArray.getVariables(firstAtoms)) {
					if (v.hasUnboundPrefix()) {
						continue;
					}
					
					CommonFunctionResult cfr = hus.getCommonFunctions(v);
						
					if (!cfr.fm.isEmpty()) {
						boolean headVar = false;
						for (int k = 0; k < 2; k++) {
							if (v.equals(Rapid.dummyVars.get(k))) {
								Set<Predicate> remainingPreds = new HashSet<>();
								for (Atom fa :  firstAtoms) {
									if (!fa.containsTerm(v)) {
										remainingPreds.add(fa.getPredicate());
									}
								}

								cfr.fm.addPredicates(remainingPreds);
								
								tmpFunctions[k].union(cfr.fm);
								headVar = true;
								break;
							}
						}
						
						if (!headVar) {
							FunctionMap fs = hus.usedFunctions.get(v);
							FunctionMap nf = FunctionMap.difference(cfr.fm, fs);
							fs.union(nf);

							for (ArrayList<Atom> newBody : nf.createBodies(cfr.body, cfr.subst)) {
								ArrayList<ArrayList<Atom>> unf = new ArrayList<ArrayList<Atom>>();
								unf.add(newBody);
	
								processArrayList(tmpFunctions, program, unf, newUsedBoundVarsCounter, newUsedUnboundVarsCounter, newBackAtoms, backSubsts, tmpMap, map);
							}
						}
					}
				}
			} 

			if (hus.isInBlockingPath() && (hus.getBlockedIndices() == null || (hus.size() > hus.getBlockedIndices().size()))) {

				FunctionMap[][] vFunctions = new FunctionMap[hus.size()][2];
				FunctionMap newF = new FunctionMap();

				for (int j = 0; j < hus.size(); j++) {
					VUnfoldStructure vus = hus.getElementAt(j);
					
					
					
					if (hus.getBlockedIndices() != null && hus.getBlockedIndices().contains(j)) {
						vFunctions[j] = vus.argFunctions;
					} else {
						if (vus.tmpFunctions != null) {
							vFunctions[j] = vus.tmpFunctions;
						} else {
							
							ArrayList<Substitution> newBackSubsts = (ArrayList<Substitution>)backSubsts.clone();
							newBackSubsts.add(hus.getSubstitutionAt(j).compose(backSubsts.get(backSubsts.size() - 1)));

							vFunctions[j] = vus.iunfold(count, program, newUsedBoundVarsCounter, newUsedUnboundVarsCounter, newBackAtoms, newBackSubsts, tmpMap, map);
						}
					}
					
					newF.union(vFunctions[j][0], vFunctions[j][1]);
					
				}
				
				Map<Variable, FunctionMap> newFunctions = new HashMap<Variable, FunctionMap>();

				if (newF.size() > 0) {
					Set<Variable> used = new HashSet<Variable>();
					
					for (int j = 0; j < hus.size(); j++) {
						Atom jAtom = firstAtoms.get(j);
						
						for (int k = 0; k < jAtom.getPredicate().getArity(); k++) {
							Variable v = (Variable)jAtom.getArgument(k);

							FunctionMap jF = FunctionMap.sunion(vFunctions[j][k], hus.getElementAt(j).argFunctions[k]);
							
							if (used.add(v)) {
								jF.intersection(newF);
							} else {
								jF.intersection(newFunctions.get(v));
							}
							newFunctions.put(v, jF);
						}
					}
	
					for (Map.Entry<Variable, FunctionMap> entry : newFunctions.entrySet()) {
						FunctionMap cfm  = entry.getValue();
						if (cfm.isEmpty()) {
							continue;
						}
						
						Variable v = entry.getKey();
						
						boolean headVar = false;
						for (int k = 0; k < 2; k++) {
							if (v.equals(Rapid.dummyVars.get(k))) {
								Set<Predicate> remainingPreds = new HashSet<>();
								for (int j = 0; j < hus.size(); j++) {
									if (!firstAtoms.get(j).containsTerm(v)) {
										remainingPreds.add(firstAtoms.get(j).getPredicate());
									}
								}

								cfm.addPredicates(remainingPreds);
								
								tmpFunctions[k].union(cfm);
								headVar = true;
								break;
							}
						}
						
						if (!headVar) {
							FunctionMap fs = hus.usedFunctions.get(v);
							FunctionMap nf = FunctionMap.difference(cfm, fs);
							fs.union(nf);
							
							ArrayList<Atom> body = new ArrayList<Atom>();
							
							Substitution s = null;
							int[] pattern = AtomArray.termIndexPattern(firstAtoms, v);
							for (int k = 0; k < pattern.length; k++) {
								Atom firstAtom = firstAtoms.get(k);
								if (pattern[k] == 0) {
									body.add(firstAtom);
									continue;
								}
							
								for (Variable vv : firstAtom.getVariables()) {
									if (!vv.hasUnboundPrefix() && !vv.equals(v)) {
										s = hus.getSubstitutionAt(k);
									}
								}
							}
							
							for (ArrayList<Atom> newBody : nf.createBodies(body, s)) {
								ArrayList<ArrayList<Atom>> unf = new ArrayList<ArrayList<Atom>>();
								unf.add(newBody);
								
								processArrayList(tmpFunctions, program, unf, newUsedBoundVarsCounter, newUsedUnboundVarsCounter, newBackAtoms, backSubsts, tmpMap, map);
							}
						} 
					}
				}
			}
			
			//----ADDS SINGLE COMMON UNFOLDINGS TO PARENT
			if (findCommon && hus.size() > 1) {
				ArrayList<Atom> commonAtoms = new ArrayList<Atom>();
				Atom.PredicateComparator pc = new Atom.PredicateComparator();
				
				for (int k = 0; k < hus.size(); k++) {
					ArrayList<Atom> leftUnfoldings = hus.getElementAt(k).collectLeftMostUnfoldings(false);
					if (k == 0) {
						commonAtoms.addAll(leftUnfoldings);
					} else {
						commonAtoms = unboundIntersection(commonAtoms, leftUnfoldings);
						Collections.sort(commonAtoms, pc);
					}
					
					if (commonAtoms.size() == 0) {
						break;
					}
				}
				
				for (Atom a : unboundDifference(commonAtoms, collectLeftMostUnfoldings(false))) {
					ArrayList<ArrayList<Atom>> unf = new ArrayList<ArrayList<Atom>>();
					unf.add(AtomArray.create(a));
					
					processArrayList(new FunctionMap[] {new FunctionMap(), new FunctionMap()}, program, unf, newUsedBoundVarsCounter, newUsedUnboundVarsCounter, newBackAtoms, backSubsts, tmpMap, map);
				}
			}
		}
	
		tmpFunctions[0].difference(argFunctions[0]);
		tmpFunctions[1].difference(argFunctions[1]);

		argFunctions[0].union(tmpFunctions[0]);
		argFunctions[1].union(tmpFunctions[1]);
		
		if (tmpFunctions[0].size() > 0 || tmpFunctions[1].size() > 0) {
			count.increase();
		}
		
		return tmpFunctions;
    }    	

	
	private ArrayList<ArrayList<Atom>> generateUnfoldings(FCTheory program, Atom atom, FunctionMap[] argFunctions, Counter newBoundVarsCounter, Counter newUnboundVarsCounter) {
    	ArrayList<ArrayList<Atom>> result = new ArrayList<ArrayList<Atom>>();

    	if (program.extraFuncs != null) {
	    	FunctionMap[] fm = program.extraFuncs.get(atom.getPredicate());
	    	
	    	if (fm != null) {
				Substitution ss = new Substitution();
				ss.put(new Variable(Rapid.DUMMY_VAR_PREFIX + "1"), new Variable(Variable.HEADVAR_PREFIX + "1"));

	    		Term[] atomTuple = atom.getArguments().getTerms();
	
	    		int ar = atom.isRole() ? 2 : 1; 
				for (int i = 0; i < ar; i++) {
					if (!atomTuple[i].hasUnboundPrefix()) {
						argFunctions[i].union(fm[i]);
					} else {
						ArrayList<Atom> body = new ArrayList<>();
						
						for (Function f : fm[i].keySet()) {
							Atom at = null;
							Variable vv = new Variable(Variable.HEADVAR_PREFIX + "1");
							if (i == 0) {
								at = new Atom(atom.getPredicate(), new FunctionalTerm(f, vv), vv); 
							} else {
								at = new Atom(atom.getPredicate(), vv, new FunctionalTerm(f, vv));
							}

							for (ArrayList<Atom> a : fm[i].createBodies(body, new Substitution())) {
								Clause sideClause = new Clause(at, AtomArray.apply(a, ss));
								
								Substitution s = Substitution.mgu(atom, sideClause.getHead());
								
								if (s != null) {
									result.add(AtomArray.apply(sideClause.getBody(), s));
								}
							}
						}
					}
				}
			}
    	}    	
    	
   		for (Clause sideClause : program.getClauses(atom.getPredicate())) {
			FunctionMap[] fb = AtomUnfolder.functionCollect(atom, sideClause);

			if (fb != null) {
				argFunctions[0].union(fb[0]);
				argFunctions[1].union(fb[1]);
   				continue;
   			}
			

			Substitution subst = new Substitution();
			Atom sideHead = sideClause.getHead();

			if (sideHead.getPredicate().getName().startsWith(Predicate.EQUAL_PREDICATE_NAME)) {
				for (Variable v : sideClause.getHead().getVariables()) {
					subst.put(v, new Variable(Rapid.EQ_VAR_PREFIX + newBoundVarsCounter.increaseUse()));
				}
				int i = 0;
				for (Variable v : SetUtils.difference(sideClause.getBoundVariables(), sideHead.getVariables())) {
					subst.put(v, Rapid.dummyVars.get(i++));
				}
			} else {
				for (Variable v : SetUtils.difference(sideClause.getBoundVariables(), sideHead.getVariables())) {
					subst.put(v, new Variable(Rapid.JOIN_VAR_PREFIX + newBoundVarsCounter.increaseUse()));
				}
			}

			for (Variable v : sideClause.getUnboundVariables()) {
				subst.put(v, new Variable(Variable.UNBOUND_PREFIX + newUnboundVarsCounter.increaseUse()));
   			}

			sideClause = sideClause.apply(subst);

			Substitution s = Substitution.mgu(atom, sideClause.getHead());
			
			if (s != null) {
				result.add(AtomArray.apply(sideClause.getBody(), s));
			}
   		}

        return result;
    } 

	public FunctionMap[] getFunctionMaps() {
		return new FunctionMap[] { argFunctions[0], argFunctions[1] };
	}
		
	public FunctionMap getFunctionMap(int arg) {
		return argFunctions[arg];
	}

	public String toString() {
		String s = " <" + (isInBlock ? "+":"") + (cycles.isEmpty()?"":cycles + " ");
		
		s += top + " ";// + order;
		
		for (HUnfoldStructure hus : content) {
			s += hus.toString();
		}
		
		s += " > ";
		
		return s;
	}
	
	public ArrayList<Atom> collectLeftMostUnfoldings(boolean onlyRoles) {
		ArrayList<Atom> unfoldings = new ArrayList<>();
		collectLeftMostUnfoldings(unfoldings, new Substitution(), onlyRoles);
		return unfoldings;
	}
	
	private void collectLeftMostUnfoldings(ArrayList<Atom> unfoldings, Substitution subst, boolean onlyRoles) {
		if (!Atom.unboundAdd(unfoldings, top.apply(subst))) {
			return;
		}
		
		for (HUnfoldStructure hus : content) {
			if (hus.size() == 1) {
				Substitution newSubst = (Substitution)subst.clone();
				newSubst = hus.getSubstitutionAt(0).compose(subst);
				Atom cTop = hus.getElementAt(0).getTopAtom();
				if (!onlyRoles || cTop.isRole()) {
					hus.getElementAt(0).collectLeftMostUnfoldings(unfoldings, newSubst, onlyRoles);
				} else {
					Atom.unboundAdd(unfoldings, cTop.apply(newSubst));
				}
			} 
		}
	}
	
	
    public int binarySearch(HUnfoldStructure key) {
		int low = 0;
		int high = content.size() - 1;
		
		while (low <= high) {
			int mid = (low + high) >>> 1;
			
			int cmp = content.get(mid).compareTo(key); 
			
			if (cmp < 0) {
				low = mid + 1;
			} else if (cmp > 0) {
				high = mid - 1;
			} else {
				while (mid > 0 && content.get(mid - 1).compareTo(key) == 0) {
					mid--;
				}
				
				return mid;
			}
		}
		
		return -(low + 1);
	}
}
