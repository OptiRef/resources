package fr.inria.cedar.compact.nonlocalrewriter;


import java.util.Set;
import java.util.TreeSet;

import fr.inria.cedar.compact.query.SemiConjunctiveQuery;
import fr.inria.cedar.compact.query.SemiConjunctiveQueryLinkedList;
import fr.lirmm.graphik.graal.api.core.Substitution;
import fr.lirmm.graphik.graal.api.core.Term;
import fr.lirmm.graphik.graal.api.core.Rule;

/**
 * 
 * @author Michael Thomazo (INRIA)
 *
 */
public class Unifier {

	//the semi conjunctive query whose disjunctions are unified by this
	final SemiConjunctiveQueryLinkedList scq;
	//the substitution potentially leading to a piece based unification
	final Substitution substitution;
	//contains the shared terms of scq that are unified by substitution with an existential variable
	final Set<Term> sharedExist;
	//contains the set of disjunctions that are unified by this
	final Set<Integer> disjunctionIndex;
	final Rule rule;


	public Unifier(SemiConjunctiveQuery q,Substitution s, Set<Term> terms, Set<Integer> disjIndex, Rule r){
		if (q instanceof SemiConjunctiveQueryLinkedList){
			this.scq = (SemiConjunctiveQueryLinkedList) q;	
		}
		else{
			this.scq = new SemiConjunctiveQueryLinkedList(q);
		}
		this.substitution = s;
		this.sharedExist = terms;
		this.rule = r;
		this.disjunctionIndex = disjIndex;
	}

	/**
	 * 
	 * @return the scq to be unified
	 */
	public SemiConjunctiveQuery getQuery(){
		return this.scq;
	}
	/**
	 * 
	 * @return the substitution being a unification
	 */
	public Substitution getSubstitution(){
		return this.substitution;
	}

	/**
	 * 
	 * @return the indices of the disjunctions being unified
	 */
	public Set<Integer> getDisjunctionIndex(){
		return this.disjunctionIndex;
	}
	
	/**
	 * 
	 * @return the rule with which this.scq is unified
	 */
	public Rule getRule(){
		return this.rule;
	}

	/**
	 * 
	 * @return true if and only if the unification unifies only one disjunctions
	 */
	public boolean isAlmostLocal(){
		return this.sharedExist.isEmpty();
	}

	/**
	 * 
	 * @return a set of indices corresponding to disjunctions that must be additionally unified to potentially get a piece based unifications
	 */
	public Set<Integer> needExpansion(){
		final Set<Integer> result = new TreeSet<>();
		for (Integer i:this.scq.getDisjunctionIndex(this.sharedExist)){
			if (!this.disjunctionIndex.contains(i)){
				result.add(i);
			}
		}
		return result;
	}

	/**
	 * 
	 * @return true if and only if this represent a piece-based unifier between this.scq and this.rule
	 */
	public boolean isPieceBasedUnifier(){
		if (!this.needExpansion().isEmpty()){
			return false;
		}
		for (Term t:this.rule.getExistentials()){
			if (this.substitution.createImageOf(t).isConstant()){
				return false;
			}
			for (Term e:this.rule.getExistentials()){
				if (!t.equals(e)&&this.substitution.createImageOf(t).equals(e)){
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public String toString(){
		return this.substitution.toString() + " \n" + this.sharedExist.toString() + "\n";
	}

}
