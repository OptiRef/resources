package fr.inria.cedar.compact.query;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import fr.lirmm.graphik.graal.api.core.Term;

/**
 * An implementation of semi conjunctive queries
 * @author Michael Thomazo (INRIA)
 *
 */
public interface SemiConjunctiveQuery {

	
	/**
	 * 
	 * @param disj a disjunction
	 * adds to the SCQ a new disjunction
	 */
	public void addDisjunction(CompactDisjunction disj);
	
	/**
	 * 
	 * @return the collection of disjunction in the SCQ
	 */
	public Collection<CompactDisjunction> getDisjunctions();
	
	/**
	 * @return the list of answer variables
	 */
	
	public List<Term> getAnswerVariables();
	/**
	 * 
	 * @return the collection of disjunction in the SCQ that contains each terms of t
	 */
	public List<CompactDisjunction> getDisjunction(Set<Term> t);
	
	/**
	 * 
	 * @return terms that appear in at least two disjunctions
	 */
	
	public Set<Term> getSharedTerms();

	/**
	 * 
	 * @return true if and only if this entails q, false otherwise
	 */
	
	public boolean entails(SemiConjunctiveQuery q);
	
	/**
	 * 
	 * @return removes the atoms containing s, and deletes disjunctions that are empty after this operation
	 */

	public void clean(String s);
	@Override
	public String toString();
}
