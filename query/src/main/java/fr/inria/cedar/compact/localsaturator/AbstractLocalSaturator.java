package fr.inria.cedar.compact.localsaturator;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import fr.inria.cedar.compact.mess.Util;
import fr.inria.cedar.compact.query.CompactDisjunction;
import fr.inria.cedar.compact.query.SemiConjunctiveQuery;
import fr.lirmm.graphik.graal.api.core.Atom;
import fr.lirmm.graphik.graal.api.core.Predicate;
import fr.lirmm.graphik.graal.api.core.Rule;
import fr.lirmm.graphik.graal.api.core.Substitution;
import fr.lirmm.graphik.graal.api.core.Term;
import fr.lirmm.graphik.graal.api.core.Variable;
import fr.lirmm.graphik.graal.api.core.VariableGenerator;
import fr.lirmm.graphik.graal.core.DefaultAtom;
import fr.lirmm.graphik.graal.core.TreeMapSubstitution;
import fr.lirmm.graphik.util.stream.CloseableIterator;

public abstract class AbstractLocalSaturator implements LocalSaturator {

	private final SemiConjunctiveQuery scq;
	private final Collection<Term> shared;
	private final VariableGenerator varGen;


	public AbstractLocalSaturator(SemiConjunctiveQuery toSaturate, VariableGenerator vg){
		this.scq = toSaturate;
		this.shared = toSaturate.getSharedTerms();
		this.varGen = vg;
	}
	/**
	 * 
	 * @param atom: an atom to be rewritten
	 * @param rule
	 * @return null if there is no possible local rewriting of atom with respect to rule, and a substitution corresponding to the (unique) local
	 * unification of atom with respect to rule otherwise
	 */
	Substitution localUnifier(Atom atom, Rule rule){
		//assumes that both body and head are atomic, and no constant in the rules
		Substitution pi = new TreeMapSubstitution();
		Atom headAtom = rule.getHead().iterator().next();
		Predicate headPred = headAtom.getPredicate();

		//Checking if there is a local unification, and storing it in pi if this is the case
		if (!atom.getPredicate().equals(headPred)){
			return null;
		}
		int arity = headPred.getArity();
		for (int i=0;i<arity;i++){
			Term rule_term_i = headAtom.getTerm(i);
			Term atom_term_i = atom.getTerm(i);
			boolean is_shared = this.shared.contains(atom_term_i);
			if (rule.getExistentials().contains(rule_term_i)){
				if (is_shared || atom_term_i.isConstant()){
					return null;
				}
				if (pi.getTerms().contains(rule_term_i)){
					if (!pi.createImageOf(rule_term_i).equals(atom_term_i)){
						return null;
					}
				}
				else{
					pi.put((Variable) rule_term_i, atom_term_i);
				}
			}
			else{
				if (pi.getTerms().contains(rule_term_i)){
					if (!pi.createImageOf(rule_term_i).equals(atom_term_i)){
						return null;
					}
				}
				else{
					if (rule_term_i.isVariable())
						pi.put((Variable) rule_term_i, atom_term_i);
				}
			}
		}
		return pi;
	}

	/**
	 * 
	 * @param rule rule that has a local unifier with an atom by pi
	 * @param pi substitution corresponding to the local unifier
	 * @return the (up to isomorphism) atom obtained by backward chaining
	 */
	Atom localRewriting(Rule rule, Substitution pi){
		Atom bodyAtom = rule.getBody().iterator().next();
		Atom result = new DefaultAtom(bodyAtom.getPredicate());
		int arity = bodyAtom.getPredicate().getArity();
		for (int i=0;i<arity;i++){
			Term t = bodyAtom.getTerm(i);
			if (pi.getTerms().contains(t)){
				result.setTerm(i, pi.createImageOf(t));
			}
			else{
				Variable iIm = this.varGen.getFreshSymbol();
				result.setTerm(i, iIm);	
				if (t.isVariable())
					pi.put((Variable) t, iIm);
			}
		}
		return result;
	}

	@Override
	public void saturateInPlace(){
		boolean changed = true;
		while (changed){
			changed = false;
			for (CompactDisjunction disj:this.scq.getDisjunctions()){
				Collection<Atom> toAdd = new LinkedList<>();
				CloseableIterator<Atom> it = disj.getAtoms().iterator();
				try{
					while (it.hasNext()){
						final Atom atom = it.next();
						final Iterator<Rule> itRule = this.getRelevantRules(atom.getPredicate());
						while (itRule.hasNext()){
							Rule rule = itRule.next();
							if (Util.isLinear(rule)){
								Substitution pi = this.localUnifier(atom, rule);
								if (pi != null){
									toAdd.add(this.localRewriting(rule, pi));
								}
							}
						}	
					}
				} catch (Exception e){
					e.printStackTrace();
				}
				for (Atom potential:toAdd){
					if (!disj.contains(potential, this.shared)){
						disj.addAtom(potential);
						changed = true;
					}
				}
			}
		}
	}

	@Override
	public SemiConjunctiveQuery getSCQ(){
		return this.scq;
	}

	/**
	 * @param p: a predicate p
	 * @return a superset of the rules that may lead to a local unification with an atom of predicate p
	 */

	protected abstract Iterator<Rule> getRelevantRules(Predicate p);

}
