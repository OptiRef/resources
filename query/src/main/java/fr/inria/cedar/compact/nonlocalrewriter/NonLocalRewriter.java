package fr.inria.cedar.compact.nonlocalrewriter;

import java.util.Collection;

import fr.inria.cedar.compact.query.SemiConjunctiveQuery;

/**
 * 
 * @author Michael Thomazo (INRIA)
 *
 */
public interface NonLocalRewriter {

	
	/**
	 * @return The set of scqs that can be obtained by one step of non-local rewriting by using the rules stored in the NonLocalRewriter
	 */
	public Collection<SemiConjunctiveQuery> rewrites();

}
