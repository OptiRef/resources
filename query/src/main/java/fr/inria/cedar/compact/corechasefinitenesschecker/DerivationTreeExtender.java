package fr.inria.cedar.compact.corechasefinitenesschecker;


import fr.inria.cedar.compact.mess.Util;
import fr.lirmm.graphik.graal.api.core.Atom;

import fr.lirmm.graphik.graal.api.core.Rule;
import fr.lirmm.graphik.graal.api.core.Term;
import fr.lirmm.graphik.graal.api.core.Variable;
import fr.lirmm.graphik.graal.core.ruleset.IndexedByBodyPredicatesRuleSet;
import fr.lirmm.graphik.graal.core.ruleset.IndexedByHeadPredicatesRuleSet;
import fr.lirmm.graphik.util.stream.CloseableIteratorWithoutException;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * 
 * @author Michael Thomazo  (Inria)
 * Saturates the derivation tree with which this object has been initialized with all possible rule applications from
 * rules. After calling saturate(), dt may not be a "proper" representation of a derivation tree (i.e., some children may
 * be describe even though they cannot be children of the given bag. This is fixed by cusing the DerivationTreeSimplifier.
 *
 */
public class DerivationTreeExtender {

	/* children maps a sharing type to its potential children in a derivation tree
	 * it contains as well pairs (st,s) where the image of s is in the terms shared
	 * of the current sharing type, and as such cannot be a child of the current sharing type.
	 * This is to ease the computation, allowing to perform only local updates between parent and children
	 */
	final DerivationTree dt;
	//set of sharing types on which direct rules have already been applied
	final Set<SharingType> saturated;
	//the original set of rules
	final IndexedByBodyPredicatesRuleSet rules;
	final IndexedByHeadPredicatesRuleSet indexByHeadRules;

	public DerivationTreeExtender(final DerivationTree dt, final IndexedByBodyPredicatesRuleSet rules){
		this.dt = dt;
		this.saturated = new HashSet<SharingType>();
		this.rules = rules;
		this.indexByHeadRules = new IndexedByHeadPredicatesRuleSet(rules);
		initializeMaps();
	}


	/**
	 * 
	 * @param st a sharing type
	 * @param body a linear rule body
	 * @param frontier the frontier of the rule
	 * @return null if the rule is not applicable to an atom of sharing type st, a mapping from the terms of the rule to the positions of st otherwise
	 */
	private Map<Term,Integer> isApplicable(final SharingType st, final Atom body,final Set<Variable> frontier){
		if (!st.p.equals(body.getPredicate()))
			return null;
		final Map<Term,Integer> helper = new HashMap<Term,Integer>();
		for (int i=0;i<body.getPredicate().getArity();i++){
			final Term termI = body.getTerm(i);
			if (helper.containsKey(termI)){
				if (!st.samePartition(i, helper.get(termI))){
					return null;
				}
			}
			else{
				helper.put(termI, i);
			}
		}
		return helper;
	}

	/**
	 * 
	 * @param termMapping: a mapping from the variables of frontier to integer
	 * @param head: an atom
	 * @param frontier: a set of variables
	 * @return a mapping that assigns to each position of head where a frontier variable appear the image of that variable through termMapping
	 */
	public Map<Integer,Integer> getPositionMapping(Map<Term,Integer> termMapping,Atom head,Set<Variable> frontier){
		Map<Integer,Integer> result = new HashMap<Integer,Integer>();
		for (int i=0;i<head.getPredicate().getArity();i++){
			if (frontier.contains(head.getTerm(i))){
				result.put(i, termMapping.get(head.getTerm(i)));
			}
		}
		return result;
	}

	/**
	 * 
	 * @param st a sharing type
	 * @param rule add to children and parents the links between st and the bags created by the application of rule on st. Note that it adds 
	 * bags even if they would be put higher in a derivation, for coding simplicity
	 */
	private void applyRule(final SharingType st,final Rule rule){
		final Atom body = rule.getBody().iterator().next();
		final Map<Term,Integer> mapping = isApplicable(st, body,rule.getFrontier());
		if (mapping!=null){
			final CloseableIteratorWithoutException<Atom> headIt = rule.getHead().iterator();
			//FIXME: this does not properly deal with non atomic head
			while (headIt.hasNext()){
				final Atom headAtom = headIt.next();
				final Map<Integer,Integer> headMapping = getPositionMapping(mapping, headAtom, rule.getFrontier());
				//FIXME: get more general case with repeated variables
				final SharingType toAdd = new SharingType(headAtom,rule.getFrontier());
				dt.addLink(st, toAdd, headMapping);
			}
		}
	}

	/**
	 * Add to children and parent the bags that correspond to application of rules on the atom of sharing type. Bags may be misplaced at this point (i.e.,
	 * marked as children of st, even though they should be put higher in the tree. Regaining this structure is done by the DerivationTreeSimplifier.
	 * @param st a sharing type
	 */
	private void applyRules(SharingType st){

		final Iterator<Rule> it = rules.getRulesByBodyPredicate(st.p).iterator();
		while (it.hasNext()){
			Rule rule = it.next();
			applyRule(st,rule);
		}
		this.saturated.add(st);
	}

	/**
	 * initializeMaps compute for each sharing type the rules that can be applied to its atom(s), and generate the corresponding bag
	 * Note that the generated bag may not be a child of the bag on which the rule is applied, if the frontier is mapped to shared terms 
	 */
	private void initializeMaps(){
		for (SharingType st:dt.getSharingTypes()){
			applyRules(st);
		}
	}


	/**
	 * 
	 * @param parentsSet
	 * @param child
	 * @param toAdd
	 * @return true if a sharing type has been moved higher up in the tree as a side effect.
	 */

	private boolean moveSharingTypeHigher(final Set<Pair<SharingType,Map<Integer,Integer>>> parentsSet,
			final Pair<SharingType,Map<Integer,Integer>> child,
			final Set<Pair<SharingType,Pair<SharingType,Map<Integer,Integer>>>> toAdd){
		boolean result = false;
		for (final Pair<SharingType,Map<Integer,Integer>> parent:parentsSet){
			//composition is well defined, as there is no obstruction to moving child as a child of parent
			final Map<Integer,Integer> composition = Util.composition(parent.getRight(),child.getRight());
			//check if the relation toAdd is not already known
			if (!dt.hasLink(parent.getLeft(), child.getLeft(), composition)){
				final Pair<SharingType,Map<Integer,Integer>> added = 
						new ImmutablePair<SharingType, Map<Integer,Integer>>(child.getLeft(), composition);
				toAdd.add(new ImmutablePair<SharingType, Pair<SharingType,Map<Integer,Integer>>>(parent.getLeft(), added));
				result = true;
			}
		}
		return result;
	}

	/**
	 * 
	 * @param st: a sharing type
	 * @param toAdd: a collector of links to be added 
	 * @return true if a sharing type currently put as a child of st should be instead put as a child of its parent.
	 */
	private boolean propagateSharingType(final SharingType st,
			final Set<Pair<SharingType,Pair<SharingType,Map<Integer,Integer>>>> toAdd){
		boolean result = false;
		//if a sharing type has not be expanded, expand it
		if (!this.saturated.contains(st)){
			applyRules(st);
		}
		//check if the mapping of the frontier contains a non shared positions
		for (final Pair<SharingType,Map<Integer,Integer>> child:dt.getChildren(st)){
			final Collection<Integer> obstructions = new HashSet<Integer>(child.getRight().values());
			obstructions.removeAll(st.sharedPositions);
			//put the sharing type higher in the tree if possible
			if (obstructions.isEmpty()){
				if (st.p.toString().substring(0,3).contains("p") && child.getLeft().p.toString().contains("t")){
					System.out.println("");
				}
				//beware of lazy evaluation of Boolean operators (method to be used as first operand)
				result = moveSharingTypeHigher(dt.getParents(st),child,toAdd) || result;
			}
		}
		return result;
	}

	/**
	 * allows to propagate information from child to parent
	 */
	public void saturate(){
		Set<SharingType> toExpand = new HashSet<SharingType>(dt.getSharingTypes());
		while (!toExpand.isEmpty()){
			final Set<SharingType> nextToExpand = new HashSet<SharingType>();
			final Set<Pair<SharingType,Pair<SharingType,Map<Integer,Integer>>>> toAdd = 
					new HashSet<Pair<SharingType,Pair<SharingType,Map<Integer,Integer>>>>();
			for (final SharingType st:toExpand){
				propagateSharingType(st,toAdd);
			}
			for (final Pair<SharingType,Pair<SharingType,Map<Integer,Integer>>> link:toAdd){
				dt.addLink(link.getLeft(), link.getRight().getLeft(), link.getRight().getRight());
				//nodes to which children have been added may still not be proper parents -- continue propagation
				nextToExpand.add(link.getLeft());
				nextToExpand.add(link.getRight().getLeft());
			}
			toExpand = nextToExpand;	
		}
	}




	@Override
	public String toString(){
		return dt.toString();
	}


}
