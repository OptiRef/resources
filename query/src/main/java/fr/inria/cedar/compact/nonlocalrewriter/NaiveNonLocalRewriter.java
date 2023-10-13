package fr.inria.cedar.compact.nonlocalrewriter;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import fr.inria.cedar.compact.mess.Util;
import fr.inria.cedar.compact.query.CompactDisjunction;
import fr.inria.cedar.compact.query.CompactDisjunctionLinkedList;
import fr.inria.cedar.compact.query.SemiConjunctiveQuery;
import fr.inria.cedar.compact.query.SemiConjunctiveQueryLinkedList;
import fr.lirmm.graphik.graal.api.core.Atom;
import fr.lirmm.graphik.graal.api.core.Rule;
import fr.lirmm.graphik.graal.api.core.RuleSet;
import fr.lirmm.graphik.graal.api.core.Substitution;
import fr.lirmm.graphik.graal.api.core.Term;
import fr.lirmm.graphik.graal.api.core.VariableGenerator;
import fr.lirmm.graphik.util.stream.CloseableIteratorWithoutException;
import fr.lirmm.graphik.graal.api.core.Variable;

/**
 * 
 * @author Michael Thomazo (INRIA)
 *
 */
public class NaiveNonLocalRewriter implements NonLocalRewriter{

	//the scq to be rewritten
	SemiConjunctiveQuery scq;
	//the set of rules with which this.scq is rewritten
	RuleSet rules;
	//a variable generator to create fresh variables
	VariableGenerator varGen;

	/**
	 * 
	 * @param toRewrite: the semi conjunctive query to be rewritten
	 * @param r: the set of rules with respect to which toRewrite is rewritten
	 * @param vg: a VariableGenerator, used to create fresh variables during piece based rewriting
	 */

	public NaiveNonLocalRewriter(SemiConjunctiveQuery toRewrite, RuleSet r,VariableGenerator vg){
		this.scq = toRewrite;
		this.rules = r;
		this.varGen = vg;
	}

	/**
	 * return the set of (non-local) rewritings of this.scq with respect to all the rules in this.rules
	 */
	@Override
	public Collection<SemiConjunctiveQuery> rewrites() {
		Iterator<Rule> itRule = this.rules.iterator();
		Collection<SemiConjunctiveQuery> result = new LinkedList<>();
		while (itRule.hasNext()){
			Rule rule = itRule.next();
			if (Util.mayBeNonLocal(rule)){
				result.addAll(rewrites(rule));
			}
		}
		return result;
	}

	/**
	 * 
	 * @param u: a piece-based unifier of this.scq with r
	 * @param r: a rule
	 * @return the SCQ obtained by rewriting this.scq with respect to u and r
	 */
	public SemiConjunctiveQuery rewrites(Unifier u,Rule r){
		List<CompactDisjunction> disjs = new LinkedList<>();
		Substitution subst = u.getSubstitution();
		//creates images for body variables that are not in the frontier
		for (Term t:r.getBody().getVariables()){
			if (!u.getSubstitution().getTerms().contains(t)){
				u.getSubstitution().put((Variable)t, this.varGen.getFreshSymbol());
			}
		}
		Iterator<CompactDisjunction> cdIt = this.scq.getDisjunctions().iterator();
		int i=-1;
		while (cdIt.hasNext()){
			i++;
			CompactDisjunction disj = cdIt.next();
			if (!u.getDisjunctionIndex().contains(Integer.valueOf(i))){
				try{
					Atom root = disj.getRoot();
					CompactDisjunction disjToAdd = new CompactDisjunctionLinkedList(
							subst.createImageOf(disj.getAtoms()),subst.createImageOf(root));
					disjs.add(disjToAdd);
				}
				catch (Exception e){
					e.printStackTrace();
				}
			}
		}
		CloseableIteratorWithoutException<Atom> atomIt = r.getBody().iterator();
		//computing the shared variables of the rewritten scq
		final Set<Term> nextShared = this.scq.getSharedTerms();
		for (Term t:r.getBody().getTerms()){
			if (!r.getFrontier().contains(t)){
				nextShared.add(t);
			}
		}

		
		while (atomIt.hasNext()){
			Atom a = atomIt.next();
			Iterator<CompactDisjunction> disjIt = disjs.iterator();
			//checking the atom to be added is not entailed by the rest of the query
			while (disjIt.hasNext()){
				CompactDisjunction disj = disjIt.next();
				if (disj.contains(subst.createImageOf(a), 
						nextShared
						.stream()
						.map(s -> subst.createImageOf(s))
						.collect(Collectors.toSet()))){
					disjIt.remove();
				}
			}
			CompactDisjunction bodyDisj = new CompactDisjunctionLinkedList(subst.createImageOf(a));
			disjs.add(bodyDisj);

		}
		//compute the updated list of answer variables
		List<Term> ansVar = new LinkedList<>();
		for (Term t:this.scq.getAnswerVariables()){
			ansVar.add(u.getSubstitution().createImageOf(t));
		}
		return new SemiConjunctiveQueryLinkedList(disjs,ansVar);
	}

	/**
	 * 
	 * @param rule 
	 * @return the set of SCQs that can be obtained by rewriting this.scq w.r.t. rule 
	 */
	private Collection<SemiConjunctiveQuery> rewrites(Rule rule){
		Collection<SemiConjunctiveQuery> result = new LinkedList<>();
		PartialUnifierStorer pus = new PartialUnifierStorer(this.scq,rule);
		//initialize the unifier builder
		pus.populateUnifierBuilder();
		List<Unifier> unifiers = pus.generateUnifiers();
		for (Unifier u:unifiers){
			result.add(rewrites(u, rule));
		}
		return result;
	}
}
