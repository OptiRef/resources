package fr.inria.cedar.compact.localsaturator;

import fr.inria.cedar.compact.query.SemiConjunctiveQuery;

public interface LocalSaturator {

	
	/**
	 * 
	 * This function modifies the stored semi conjunctive query such that each disjunction is saturated with respect to local unifications
	 * @throws Exception
	 */
	

	public void saturateInPlace();

	/**
	 * 
	 * @return the SCQ that is acted upon by the local saturator. The SCQ is locally saturated if saturateInPlace has been applied before
	 */
	
	public SemiConjunctiveQuery getSCQ();
	
}
