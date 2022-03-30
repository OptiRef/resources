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

package edu.ntua.isci.qa.algorithm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;

import edu.ntua.isci.common.dl.LoadedOntology;
import edu.ntua.isci.common.dl.LoadedOntologyAccess;
import edu.ntua.isci.common.lp.Atom;
import edu.ntua.isci.common.lp.Clause;
import edu.ntua.isci.common.lp.Predicate;

import edu.ntua.isci.qa.lp.theory.FCTheory;
import edu.ntua.isci.qa.lp.theory.LogicProgram;
import edu.ntua.isci.qa.owl.OWL2LogicTheory;
import edu.ntua.isci.qa.rewriting.QueryCollector;
import edu.ntua.isci.qa.rewriting.SimpleQueryCollector;

public abstract class Engine extends Thread {

	public static boolean db = false;
	public static boolean realTimeEvaluation = true;
	
	public static boolean removeRedundant = false;
	
	protected String name;
	
	protected FCTheory lp;
	protected Clause query;
	protected Predicate queryPredicate;
	
	protected QueryCollector qc;

	ArrayList<OWLAxiom> ignoredAxioms;
	
	protected boolean cleanDeadAUXClauses;
	
	protected Engine(String name) {
		this.name = name;
		
		lp = new FCTheory();
		
		ignoredAxioms = new ArrayList<>();
		
		cleanDeadAUXClauses = true;
	}
	
	public abstract boolean isCGLLR();
	
	public abstract boolean isELHI();
	
	public abstract boolean isSHIQ();
	
	public String getEngineName() {
		return name;
	}

	public void setTheory(FCTheory theory) {
		for (Clause c : theory.getClauses()) {
			lp.addClause(c);
		}
		
//		initialize();
	}
	
	public FCTheory extraProgram;
	
	public FCTheory getCurrentTheory() {
		return lp;
	}

	public ArrayList<OWLAxiom> getIgnoredAxioms() {
		return ignoredAxioms;
	}

	public boolean isQueryPredicate(Predicate p) {
		return p.equals(queryPredicate);
	}
	
	protected boolean isUCQ(ArrayList<Clause> clauses) {
		for (Clause c : clauses) {
			if (!isQueryPredicate(c.getHead().getPredicate())) {
				return false;
			}
		}
		
		return true;
	}
	
	public OWL2LogicTheory importOntology(LoadedOntology ontRef, LoadedOntologyAccess loa, int level) throws LowExpressivityException {
		OWL2LogicTheory lt = new OWL2LogicTheory(ontRef);
		
		for (Clause c : lt.clausify(loa, isCGLLR(), level)) {
			lp.addClause(c);
		}
		
		if ((level == OWL2LogicTheory.ELHI || level == OWL2LogicTheory.HORN_SHIQ) && lp.isConjuntive() && (isCGLLR() || !isELHI())) {
			throw new LowExpressivityException();
		}
		
		lp.addAUXNames(lt.getAUXNames());

		ignoredAxioms.addAll(lt.getIgnoredAxioms());
		
//		initialize();
		
		return lt;

	}

	public boolean producesCondensed() {
		return true;
	}

	public QueryCollector createQueryCollector(Clause query) {
		this.query = query.condenseWithOriginalVariables();
		
		qc = new SimpleQueryCollector(lp, query, removeRedundant);

		return qc;
	}

	public QueryCollector getQueryCollector() {
		return qc;
	}

	
	public abstract void initialize();

	public abstract void execute();
	
	public void prepare(Map<String, Object> extraParameters) { }
		
	public ArrayList<Clause> postsc(ArrayList<Clause> clauses) {
		ArrayList<Clause> res = null;
		
		if (!isUCQ(clauses)) {
			LogicProgram llp = new LogicProgram(clauses);
			
			Set<Predicate> preds = new HashSet<>();
			preds.add(query.getHead().getPredicate());
			if (llp.getEqualityHeadClauses().size() > 0) {
				preds.add(Predicate.EQUALITY_PREDICATE);
			}
			
			res = llp.getGraphDependencyProgram(preds);
			
			if (cleanDeadAUXClauses) {
				removeAUX(res);
			}
		} else {
			if (cleanDeadAUXClauses) {
				res = new ArrayList<Clause>();
				
				loop:
				for (Clause c : clauses) {
					for (Atom atom : c.getBody()) {
						if (lp.isAUXAtom(atom)) {
							continue loop;
						}
					}
					res.add(c);
				}
			} else {
				res = clauses;
			}
		}
			
		return res;
	}
	
	public void setCleanDeadAuxClauses(boolean s) {
		cleanDeadAUXClauses = s;
	}
	
	protected void removeAUX(ArrayList<Clause> clauses) {
		while (true) {
			Set<String> headAUXPredicates = new HashSet<>();
			Set<String> bodyAUXPredicates = new HashSet<>();
			for (Clause r : clauses) {
				for (Atom a : r.getBody()) {
					int name = -1;
					if (lp.isAUXAtom(a) || name != -1) {
						bodyAUXPredicates.add(a.getPredicate().getName());
					}
				}
				if (lp.isAUXAtom(r.getHead())) {
					headAUXPredicates.add(r.getHead().getPredicate().getName());
				}
			}
			
			bodyAUXPredicates.removeAll(headAUXPredicates);

			boolean remove = false;
			for (Iterator<Clause> it = clauses.iterator(); it.hasNext();) {
				Clause nc = it.next();
				for (Atom a : nc.getBody()) {
					if (bodyAUXPredicates.contains(a.getPredicate().getName())) {
						it.remove();
						remove = true;
						break;
					}
				}
			}
			
			if (!remove) {
				break;
			}
		}		

	}

	
	public void run() {
		try {
			execute();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public ComputedRewriting computeRewritings(Clause query) {
		return computeRewritings(query, true);
	}

	public ComputedRewriting computeRewritings(Clause query, boolean sc) {
		this.queryPredicate = query.getHead().getPredicate();
		
		Stats st = new Stats(); 
		
		QueryCollector qc = createQueryCollector(query);

		initialize();
		
		long xttime1 = System.currentTimeMillis();
			
		execute();
		
		long rTime = (System.currentTimeMillis() - xttime1);
		
		ArrayList<Clause> allRewritings = qc.getQueries();

		long xttime2 = System.currentTimeMillis();  
		
		ArrayList<Clause> nonRedundantRewritings = null;
		
		if (sc) {
			nonRedundantRewritings = postsc(qc.computeNonRedundantQueries(producesCondensed()));
		} else {
			nonRedundantRewritings = postsc(allRewritings);
		}
		
		long cTime = (System.currentTimeMillis() - xttime2);
		
		long mTime = 0;
			
		st.addIterationTimes(rTime, cTime, mTime);
		
		return new ComputedRewriting(allRewritings, nonRedundantRewritings, st);
	}
	
}
