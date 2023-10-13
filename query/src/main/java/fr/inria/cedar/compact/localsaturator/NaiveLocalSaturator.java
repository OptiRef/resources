package fr.inria.cedar.compact.localsaturator;

import java.util.Iterator;

import fr.inria.cedar.compact.query.SemiConjunctiveQuery;
import fr.lirmm.graphik.graal.api.core.Predicate;
import fr.lirmm.graphik.graal.api.core.Rule;
import fr.lirmm.graphik.graal.api.core.RuleSet;
import fr.lirmm.graphik.graal.api.core.VariableGenerator;

public class NaiveLocalSaturator extends AbstractLocalSaturator{

	RuleSet rules;

	public NaiveLocalSaturator(SemiConjunctiveQuery toSaturate, RuleSet r, VariableGenerator vg){
		super(toSaturate,vg);
		this.rules = r;
	}

	/*
	 * (non-Javadoc)
	 * @see fr.inria.cedar.compact.localsaturator.AbstractLocalSaturator#getRelevantRules(fr.lirmm.graphik.graal.api.core.Predicate)
	 * fetch all rules from rules
	 */

	@Override protected Iterator<Rule> getRelevantRules(Predicate p){
		return this.rules.iterator();
	}

}
