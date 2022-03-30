package fr.inria.cedar.compact.localsaturator;

import java.util.Iterator;

import fr.inria.cedar.compact.query.SemiConjunctiveQuery;
import fr.lirmm.graphik.graal.api.core.Predicate;
import fr.lirmm.graphik.graal.api.core.Rule;
import fr.lirmm.graphik.graal.api.core.VariableGenerator;
import fr.lirmm.graphik.graal.core.ruleset.IndexedByHeadPredicatesRuleSet;

public class HashLocalSaturator extends AbstractLocalSaturator{
	IndexedByHeadPredicatesRuleSet rules;

	public HashLocalSaturator(SemiConjunctiveQuery toSaturate, IndexedByHeadPredicatesRuleSet r, VariableGenerator vg){
		super(toSaturate,vg);
		this.rules = r;
	}
	

	/*
	 * (non-Javadoc)
	 * @see fr.inria.cedar.compact.localsaturator.AbstractLocalSaturator#getRelevantRules(fr.lirmm.graphik.graal.api.core.Predicate)
	 * Fetch all the rules whose (unique) head atom is of predicate p
	 */
	@Override
	protected Iterator<Rule> getRelevantRules(Predicate p){
		return this.rules.getRulesByHeadPredicate(p).iterator();
	}
	
	
}
