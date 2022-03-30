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

package edu.ntua.isci.common.lp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.ntua.isci.common.utils.Counter;

import edu.ntua.isci.common.utils.Utils;

public class Clause implements ClauseWrapper, Cloneable, Comparable<Clause> {

	protected Atom head;
	protected ArrayList<Atom> body;
	
	private int bodyDepth = -1;

	private Map<Predicate, ArrayList<Integer>> predicateIndex;
    private Set<Variable> vars = null;
    private Set<Term> terms = null;
	private Set<Term> bTerms = null;
	private Set<Variable> bVars = null;
	private Set<Variable> uVars = null;

	public Clause(Atom headAtom, Atom bodyAtom) {
		this.head = headAtom;
		
		this.body = new ArrayList<Atom>();
		body.add(bodyAtom);
	}
	
	public Clause(Atom headAtom, Collection<Atom> bodyAtoms) {
		this.head = headAtom;

		this.body = new ArrayList<Atom>();
		body.addAll(bodyAtoms);
	}
	
	public void reset() {
		predicateIndex = null;
	    vars = null;
	    terms = null;
		bTerms = null;
		bVars = null;
		uVars = null;
	}
	
  
	public Object clone() {
		try {
			Clause c = (Clause)super.clone();
			c.head = (Atom)head.clone();
			c.body = new ArrayList<Atom>();
			for (int i = 0; i < body.size(); i++) {
				c.body.add((Atom)body.get(i).clone());
			}
			
			return c;
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}				
	}		
	
	private Map<Predicate, ArrayList<Integer>> getPredicateIndex() {
		if (predicateIndex == null) {
			predicateIndex = computePredicateIndex();
		}
		
		return predicateIndex;
	}
	
	private HashMap<Predicate, ArrayList<Integer>> computePredicateIndex() {
		
		HashMap<Predicate, ArrayList<Integer>> index = new HashMap<Predicate, ArrayList<Integer>>();
		
		for (int i = 0; i < body.size(); i++){
			Predicate p = body.get(i).getPredicate();
		
			ArrayList<Integer> list = index.get(p);
			if (list == null) {
				list = new ArrayList<Integer>();
				index.put(p, list);
			}
			list.add(i);
		}

		return index;
	}
	
	public Clause getClause() {
		return this;
	}
	
	public Atom getHead() {
		return head;
	}
	
	public int bodySize() {
		return body.size();
	}

	public ArrayList<Atom> getBody() {
		return body;
	}
	
	public Set<Atom> getBodyAtoms() {
		Set<Atom> res = new HashSet<Atom>();
		res.addAll(body);
		return res;
	}
	
	public Atom getBodyAtomAt(int i) {
		return body.get(i);
	}

	public boolean isBound(Variable v) {
		return getBoundVariables().contains(v);
	}
	
	public int bodyDepth() {
		if (bodyDepth == -1) {
			int maxDepth = 0;
		
			for (int i = 0; i < body.size(); i++){
				maxDepth = Math.max(maxDepth, body.get(i).depth());
			}
			
			bodyDepth = maxDepth;
		}
		
		return bodyDepth;
	}
	
	public boolean isTautology() {
        for (int i = 0; i < body.size(); i++) {
        	if (body.get(i).equals(head)) {
        		return true;
        	}
        }
        return false;
    }
	
    public boolean isFunctionFree() {
    	if (head.depth() == 0 && bodyDepth() == 0){
    		return true;
    	}
    	
    	return false;
    }
    
    public int[] termIndexPattern(Term t) {
    	return AtomArray.termIndexPattern(body, t);
    }
    
    private Set<Variable> getVariableSet() {
		if (vars == null) {
			Set<Variable> res = new HashSet<Variable>();
			res.addAll(head.getVariables());
			
			for (Atom b : body) {
				res.addAll(b.getVariables());
			}
			
			vars = res;
		}
		
		return vars;
    }
    
	public Set<Variable> getVariables() {
		getVariableSet();
		
		return (Set<Variable>)((HashSet<Variable>)vars).clone();
	}
	
	public Set<Term> getTerms() {
		if (terms == null) {
			Set<Term> res = new HashSet<Term>();
			res.addAll(head.getTerms());
			
			for (Atom b : body) {
				res.addAll(b.getTerms());
			}
			
			terms = res;
		} 
		
		return (Set<Term>)((HashSet<Term>)terms).clone();
	}	
	
	public Set<Predicate> getPredicatesInBody() {
		Set<Predicate> res = new HashSet<Predicate>();
		
		for (Atom a : body) {
			res.add(a.getPredicate());
		}
		
		return res;
	}
	
	
	public Clause apply(Substitution subst) {
		
		ArrayList<Atom> newBody = new ArrayList<Atom>();

		for (int i = 0; i < body.size(); i++) {
			Atom newAtom = body.get(i).apply(subst);
			if (!newBody.contains(newAtom)) {
				newBody.add(newAtom);
			}
		}
		
		
		return new Clause(head.apply(subst), newBody);
	}


	public int compareTo(Clause r) {
		int c = head.compareTo(r.getHead());
		if (c != 0) {
			return c;
		} else {
			ArrayList<Atom> cBody = r.getBody();
			for (int i = 0; i < Math.min(cBody.size(), body.size()); i++) {
				int nc = body.get(i).compareTo(cBody.get(i));
				if (nc != 0) {
					return nc;
				}
			}
			
			if (body.size() < cBody.size()) {
				return -1;
			} else if (body.size() > cBody.size()) {
				return 1;
			} else {
				return 0;
			}
		}
	}	
	
	public int hashCode() {
		int h = head.getPredicate().hashCode();

		 
		for (Predicate p: getPredicatesInBody()) {
			h += p.hashCode();
		}

		return h;
	}
	
	private class ClauseSubstitution {
		Clause clause;
		Substitution subst;
		
		private ClauseSubstitution(Clause clause, Substitution subst) {
			this.clause = clause;
			this.subst = subst;
		}
		
		public int hashCode() {
			return clause.hashCode();
		}
		
		public boolean equals(Object obj) {
			if (obj instanceof ClauseSubstitution) {
				return clause.equals(((ClauseSubstitution)obj).clause);
			} else {
				return false;
			}
		}
	}
	
	public Clause condense() {

		ArrayList<Clause> unprocessed = new ArrayList<Clause>();
		ArrayList<Clause> condensations = new ArrayList<Clause>();
	
		unprocessed.add(this);
		
		while(!unprocessed.isEmpty()){
			Clause givenClause = unprocessed.remove(0);
			condensations.add(givenClause);

			int givenClauseSize = givenClause.bodySize();
			
			for(int i = 0; i < givenClauseSize - 1; i++){
				Atom givenClauseAtomI = givenClause.getBodyAtomAt(i);
				loop:
				for(int j = i+1; j < givenClauseSize; j++){
					Substitution unifier = Substitution.mgu(givenClauseAtomI, givenClause.getBodyAtomAt(j));
					if (unifier != null){
						Clause newQuery = givenClause.apply(unifier);
						
						for (Clause c: unprocessed){
							if (c.isRenamingOf(newQuery)){
								continue loop;
							}					
						}
						
						for (Clause c: condensations){
							if (c.isRenamingOf(newQuery)){
								continue loop;
							}
						}
						
						unprocessed.add(newQuery);
	                }
				}
			}
		}
		
		Collections.sort(condensations, new Comparator<Clause>(){
			public int compare(Clause c1, Clause c2){
			    return (new Integer(c1.bodySize())).compareTo(new Integer(c2.bodySize()));
			}
		});

		for(Clause c: condensations){
			if (c.subsumes(this)){
				return c;
			}
		}

		return this;
	}

	public Clause condenseWithOriginalVariables() {
		
		ArrayList<ClauseSubstitution> unprocessed = new ArrayList<ClauseSubstitution>();
		ArrayList<ClauseSubstitution> condensations = new ArrayList<ClauseSubstitution>();
	
		unprocessed.add(new ClauseSubstitution(this, new Substitution()));
		
		while(!unprocessed.isEmpty()){
			ClauseSubstitution givenClause = unprocessed.remove(0);
			condensations.add(givenClause);
			
			int givenClauseSize = givenClause.clause.bodySize();

			for(int i = 0; i < givenClauseSize - 1; i++){
				Atom givenClauseAtomI = givenClause.clause.getBodyAtomAt(i);
				loop:
				for(int j = i+1; j < givenClauseSize; j++){
					Substitution unifier = Substitution.mgu(givenClauseAtomI, givenClause.clause.getBodyAtomAt(j));
					if (unifier != null){
						Clause newQuery = givenClause.clause.apply(unifier);

						for (ClauseSubstitution c: unprocessed){
							if (c.clause.isRenamingOf(newQuery)){
								continue loop;
							}					
						}
						
						for (ClauseSubstitution c: condensations){
							if (c.clause.isRenamingOf(newQuery)){
								continue loop;
							}
						}
						
						unprocessed.add(new ClauseSubstitution(newQuery, givenClause.subst.compose(unifier)));
	                }
				}
			}
		}
		
		Collections.sort(condensations, new Comparator<ClauseSubstitution>(){
			public int compare(ClauseSubstitution c1, ClauseSubstitution c2){
			    return (new Integer(c1.clause.bodySize())).compareTo(new Integer(c2.clause.bodySize()));
			}
		});

		Set<Variable> unboundVars = this.getUnboundVariables();
		
		for (ClauseSubstitution c: condensations) {
			if (c.clause.subsumes(this)) {
				Substitution iSubst = new Substitution();
				
			    for (Variable v : c.subst.getVariables()) {
			    	if (unboundVars.contains(c.subst.get(v)) && !unboundVars.contains(v)) {
			    		iSubst.put((Variable)c.subst.get(v), v);
			    	}
			    }
			    
			    removeRedundantAtomsSubstitution = c.subst.compose(iSubst);

				return c.clause.apply(iSubst);
			}
		}
		
		return this;
	
	}
	
	public Substitution removeRedundantAtomsSubstitution;

	public Set<Variable> getBoundVariables() {
		if (bVars == null) {
			Set<Variable> res = getVariables();
			res.removeAll(getUnboundVariables());
			bVars = res;
		}
		
		return (Set<Variable>)((HashSet<Variable>)bVars).clone();
	}
	
	public Set<Variable> getUnboundVariables() {
		if (uVars == null) {
			Map<Variable, Counter> cc = new HashMap<Variable, Counter>();
			
			Set<Atom> bodyAtoms = new HashSet<Atom>();
			
			for (Atom atom : body) {
				if (!bodyAtoms.add(atom)) {
					continue;
				}
				
				for (Term t : atom.getArguments().getTerms()) {
					if (t.isVariable()) {
						Counter c = cc.get(t);
						if (c == null) {
							c = new Counter(1);
							cc.put((Variable)t, c);
						} else {
							c.increase();
						}
					}
				}			
			}

			for (Variable v : head.getVariables()) {
				cc.remove(v);
			}

			Set<Variable> res = new HashSet<Variable>();
			
			for (Map.Entry<Variable, Counter> entry : cc.entrySet()) {
				if (entry.getValue().getValue() == 1) {
					res.add(entry.getKey());
				}
			}
			
			uVars = res;
		}
		
		return (Set<Variable>)((HashSet<Variable>)uVars).clone();

	}	

	public Set<Term> getBoundTerms() {
		if (bTerms == null) {
			Set<Term> res = getTerms();
			res.removeAll(getUnboundVariables());
		
			bTerms = res;
		}
		
		return (Set<Term>)((HashSet<Term>)bTerms).clone();
	}
	

	public <U extends ClauseWrapper> boolean isCCSubsumedBy(U u) {
		return u.getClause().subsumes(this);
	}
	
	// assumes clauses have no redundant atoms
	public static <U extends ClauseWrapper> ArrayList<U> getNonRedundantClauses(ArrayList<U> clauses) {
		return getNonRedundantClauses(clauses, false, null, null, null, null);
	}
	
	public static <U extends ClauseWrapper> ArrayList<U> getNonRedundantClauses(ArrayList<U> clauses, ArrayList<Integer> cids) {
		ArrayList<Integer> ids = new ArrayList<>();

		ArrayList<Integer> nrBlockStart = new ArrayList<>();
		ArrayList<Integer> nrBlockStartIds = new ArrayList<>();

		for (int i = 0; i < clauses.size(); i++) {
			int id = cids.get(i);
		
			if (id == -1) {
				ids = null;
			} else {
				if (nrBlockStart.size() == 0) {
					nrBlockStart.add(0);
					nrBlockStartIds.add(id);
				} else {
					if (nrBlockStartIds.get(nrBlockStart.size() - 1) != id) {
						nrBlockStart.add(ids.size());
						nrBlockStartIds.add(id);
					}
				}
				
				ids.add(id);
			}
		}
		
		return getNonRedundantClauses(clauses, false, null, ids, nrBlockStart, nrBlockStartIds);
	}
	
	public static <U extends ClauseWrapper> ArrayList<U> getNonRedundantClauses(ArrayList<U> clauses, boolean oneway, ArrayList<Integer> ids, ArrayList<Integer> blocks, ArrayList<Integer> blockIds) {
		return getNonRedundantClauses(clauses, oneway, null, ids, blocks, blockIds);
	}

	private static class LengthComparator<U extends ClauseWrapper> implements Comparator<U> {

		public int compare(U o1, U o2) {
			int i1 = o1.getClause().bodySize();
			int i2 = o2.getClause().bodySize();
			
			if (i1 < i2) {
				return -1;
			} else if (i1 > i2) {
				return 1;
			} else {
				return 0;
			}
		}
	}
	
	private static LengthComparator<ClauseWrapper> lc = new LengthComparator<ClauseWrapper>();
	
	private static class CT implements Comparable<CT> {
		public double len;
		public int start;
		public int end;
		
		public CT(double len, int start, int end) {
			this.len = len;
			this.start = start;
			this.end = end;
		}

		public int compareTo(CT obj) {
			if (len < obj.len) {
				return -1;
			} else if (len > obj.len) {
				return 1;
			} else {
				return 0;
			}
		}
	}
	
	public static <U extends ClauseWrapper> void sort(ArrayList<U> clauses, ArrayList<Integer> ids, ArrayList<Integer> blockStart, ArrayList<Integer> blockStartIds) {
		
		if (ids == null) {
			Collections.sort(clauses, lc);
		} else {
			ArrayList<U> ires = new ArrayList<U>();
			CT[] lens = new CT[blockStart.size()];
			
			int size = blockStart.size();
			for (int i = 0; i < size; i++) {
				int start = blockStart.get(i);
				int end;
				if (i == size - 1) {
					end = clauses.size();
				} else {
					end = blockStart.get(i+1);
				}
				ArrayList<U> tmp = new ArrayList<U>();

				int c = 0;
				for (int j = start; j < end; j++) {
					U cc = clauses.get(j);
					tmp.add(cc);
					c += cc.getClause().bodySize();
				}

				Collections.sort(tmp, lc);
				ires.addAll(tmp);
				
				lens[i] = new CT(((double)c)/(end-start), start, end);			
			}

			Arrays.sort(lens);
			
			ArrayList<U> newClauses = new ArrayList<U>();
			ArrayList<Integer> newIds = new ArrayList<Integer>();
			ArrayList<Integer> newBlockStart = new ArrayList<Integer>();
			ArrayList<Integer> newBlockStartIds = new ArrayList<Integer>();
			
			int id = 0;
			for (CT ct : lens) {
				newBlockStart.add(newClauses.size());
				
				for (int j = ct.start; j < ct.end; j++) {
					newClauses.add(ires.get(j));
					newIds.add(id);
				}
				
				newBlockStartIds.add(id++);
			}
			
			clauses.clear();
			clauses.addAll(newClauses);
			ids.clear();
			ids.addAll(newIds);
			blockStart.clear();
			blockStart.addAll(newBlockStart);
			blockStartIds.clear();
			blockStartIds.addAll(newBlockStartIds);
		}
	}
	
	public static <U extends ClauseWrapper> ArrayList<U> getNonRedundantClauses(ArrayList<U> clauses, boolean oneway, Counter ct, ArrayList<Integer> ids, ArrayList<Integer> blockStart, ArrayList<Integer> blockStartIds) {
		sort(clauses, ids, blockStart, blockStartIds);
		
		ArrayList<U> result = new ArrayList<U>();
		result.addAll(clauses);

		ArrayList<Integer> newIds = null;
		ArrayList<Integer> blocks = null;
		ArrayList<Integer> blocksIds = null;
		
		if (ids != null) {
			newIds = new ArrayList<Integer>();
			newIds.addAll(ids);
			
			blocks = new ArrayList<Integer>();
			blocksIds = new ArrayList<Integer>();
			
			blocks.addAll(blockStart);
			blocksIds.addAll(blockStartIds);
		}
		
//		blocks = null;
		
		ClauseWrapper cc;
		int id = -1;
		int bi = -1;

		int prevId = -1;
		
		if (!oneway) {
			loop1:
			for (int i = 0; i < result.size(); ) {
				cc = result.get(i);
				if (newIds != null) {
					id = newIds.get(i);

					if (blocks != null) {
						if (id != prevId) {
							bi = Utils.binarySearch(blocksIds, id);
							prevId = id;
						}
					}
				}

				for (int j = 0; j < i;) {
					if (ids != null && id == newIds.get(j)) {
						if (blocks != null) {
							if (bi + 1 < blocks.size()) {
								j = blocks.get(bi + 1);
							} else {
								break;
							}
						} else {
							j++;
						}

						continue;
					}
	
					if (cc.isCCSubsumedBy(result.get(j))) {
						if (ct != null && i < ct.getValue()) {
							ct.decrease();
						}
						
						result.remove(i);
						
						if (newIds != null) {
							newIds.remove(i);
							
							if (blocks != null) {
								for (int t = bi + 1; t < blocks.size(); t++) {
									blocks.set(t, blocks.get(t) - 1);
								}

								if (bi + 1 < blocks.size() && blocks.get(bi).equals(blocks.get(bi + 1))) {
									blocks.remove(bi);
									blocksIds.remove(bi);
								}
							}
						}
						continue loop1;
					}
					j++;
				}
				i++;
			}
		}
		
		prevId = -1;
		
		loop2:
		for (int i = 0; i < result.size();) {
			cc = result.get(i);
			if (newIds != null) {
				id = newIds.get(i);
				if (blocks != null) {
					if (id != prevId) {
						bi = Utils.binarySearch(blocksIds, id);
						prevId = id;
					}
				}
			}
			
			for (int j = i + 1; j < result.size();) {
				if (ids != null && id == newIds.get(j)) {
					if (blocks != null) {
						if (bi + 1 < blocks.size()) {
							j = blocks.get(bi + 1);
						} else {
							break;
						}
					} else {
						j++;
					}
					continue;
				}

				if (cc.isCCSubsumedBy(result.get(j))) {
					if (ct != null && i < ct.getValue()) {
						ct.decrease();
					}
					result.remove(i);

					if (newIds != null) {
						newIds.remove(i);
						
						if (blocks != null) {
							for (int t = bi + 1; t < blocks.size(); t++) {
								blocks.set(t, blocks.get(t) - 1);
							}

							if (bi + 1 < blocks.size() && blocks.get(bi).equals(blocks.get(bi + 1))) {
								blocks.remove(bi);
								blocksIds.remove(bi);
							}
						}					
					}
					continue loop2;
				}
				j++;
			}
			i++;
		}
		
		return result;
	}	
	
	public String toString() {
		String s = "";
		
		for (Atom b : body) {
			if (s.length() != 0) {
				s += ", ";
			}
			s += b.toString();
		}
		
		return head.toString() + " <- " + s;
	}	
	
	public boolean equals(Object obj) {
		return isRenamingOf((Clause)obj); 
	}

	public boolean isRenamingOf(Clause rule) {
		return isRenamingOfS(rule) != null;
	}
	
    public Substitution isRenamingOfS(Clause rule) {
    	
		if (!head.getPredicate().equals(rule.getHead().getPredicate()) || body.size() != rule.bodySize()) {
			return null;
		}

		Set<Predicate> allPreds = new HashSet<Predicate>();
		for (int i = 0; i < this.body.size(); i++){
			allPreds.add(this.body.get(i).getPredicate());
			allPreds.add(rule.body.get(i).getPredicate());
		}
		
		for (Predicate p : allPreds) {
			ArrayList<Integer> l1 = this.getPredicateIndex().get(p);			
			ArrayList<Integer> l2 = rule.getPredicateIndex().get(p);
			
			if (l1 == null || l2 == null || l1.size() != l2.size()) {
				return null;
			}
		}
		
		Substitution subst = Atom.matchAtoms(head, rule.getHead(), new Substitution());
		if (subst == null) {
			return null;
		}

        Substitution res = match(true, body, AtomArray.getAtomIndex(rule.body), subst, 0);
        
        if (res != null && res.isRenaming(getVariables())) {
        	return res;
        } else {
        	return null;
        }
    }	
	
    public boolean subsumes(Clause rule) {
		if (!head.getPredicate().equals(rule.getHead().getPredicate())) {
			return false;
		}
				
		Set<Predicate> thatPreds = new HashSet<Predicate>();
		int thatBodySize = rule.bodySize();

		for (int i = 0; i < thatBodySize; i++){			
			thatPreds.add(rule.getBodyAtomAt(i).getPredicate());
		}
		
		for (Predicate p : this.getPredicateIndex().keySet()) {
			if (!thatPreds.contains(p)) {
				return false;
			}
		}			
    	
		Substitution subst = Atom.matchAtoms(head, rule.getHead(), new Substitution());
		if (subst == null) {
			return false;
		} 
		
		ArrayList<Atom> ruleHead = new ArrayList<Atom>();
		ruleHead.add(rule.head);

		Substitution s = match(false, body, AtomArray.getAtomIndex(rule.getBody()), subst, 0);
		
        return s != null;
    }

    private Substitution match(boolean oneToOne, ArrayList<Atom> subsumingAtoms, Map<Predicate,AtomArray.AtomIndexNode> subsumedAtomsIndex, Substitution substitution, int index) {
    	
        if (index == subsumingAtoms.size()) {
        	return substitution;
        } else {
            Atom subsumingAtom = subsumingAtoms.get(index);

            AtomArray.AtomIndexNode candidates = subsumedAtomsIndex.get(subsumingAtom.getPredicate());
            
            while (candidates != null) {
            	if (oneToOne){
                    if (!candidates.matched) {
                        Substitution matchedSubstitution = Atom.matchAtoms(subsumingAtom, candidates.atom, substitution);
                        if (matchedSubstitution != null) {
                            candidates.matched = true;
                            Substitution newSubst = match(oneToOne, subsumingAtoms, subsumedAtomsIndex, matchedSubstitution, index + 1); 
                            if (newSubst != null){
                                return newSubst;
                            } else {
                            	candidates.matched = false;
                            }
                        }
                    }
                    candidates = candidates.next;
            	} else {
            		Substitution matchedSubstitution = Atom.matchAtoms(subsumingAtom, candidates.atom, substitution);
                    if (matchedSubstitution != null) {
                    	Substitution newSubst = match(oneToOne, subsumingAtoms, subsumedAtomsIndex, matchedSubstitution, index + 1); 
                        if (newSubst != null) {
                            return newSubst;
                        }
                    }
                    candidates = candidates.next;
            	}
            }
            
            return null;
        }
    }
    
}


