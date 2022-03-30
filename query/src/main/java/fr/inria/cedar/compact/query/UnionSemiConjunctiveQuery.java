package fr.inria.cedar.compact.query;

import java.util.Collection;

/**
 * 
 * Interface for unions of semi conjunctive queries.
 * @author Michael Thomazo (INRIA)
 *
 */
public interface UnionSemiConjunctiveQuery {

	/**
	 * 
	 * @return the collection of semi conjunctive queries contained in the USCQ
	 */
	public Collection<SemiConjunctiveQuery> getSCQs();
	
	/**
	 * 
	 * @param scq the semi conjunctive query to be added to the USCQ
	 */
	public void add(SemiConjunctiveQuery scq);
	

	/**
	 * @param scq
	 * Add a scq to the union, and removes any scq that is less general than scq.
	 */
	public void addAndRemoveRedundant(SemiConjunctiveQuery scq);

	/**
	 * 
	 * @param s
	 * @return yes if s is less general than one of the scqs in the union
	 * Beware: this is not equivalent to this entailing s in the general case.
	 */
	public boolean entails(SemiConjunctiveQuery s);
	
	/**
	 * deletes the disjunction containing only the answer predicate
	 */
	public void clean(String s);

	@Override
	public String toString();
}
