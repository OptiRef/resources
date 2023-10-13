package fr.inria.cedar.compact.query;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import fr.inria.cedar.compact.mess.Util;
import fr.lirmm.graphik.graal.api.core.Atom;
import fr.lirmm.graphik.graal.api.core.ConjunctiveQuery;
import fr.lirmm.graphik.graal.api.core.Predicate;
import fr.lirmm.graphik.graal.api.core.Substitution;
import fr.lirmm.graphik.graal.api.core.Term;
import fr.lirmm.graphik.graal.api.core.Variable;
import fr.lirmm.graphik.graal.core.DefaultAtom;
import fr.lirmm.graphik.graal.core.TreeMapSubstitution;
import fr.lirmm.graphik.util.stream.CloseableIterator;
import fr.lirmm.graphik.util.stream.CloseableIteratorWithoutException;

/**
 * 
 * @author Michael Thomazo (INRIA)
 *
 */
public class SemiConjunctiveQueryLinkedList implements SemiConjunctiveQuery{

	//the disjunctions of the query
	List<CompactDisjunction> disjunctions;
	//the answer variable of the query
	List<Term> ansVar;

	public SemiConjunctiveQueryLinkedList(final ConjunctiveQuery query){
		this.disjunctions = new LinkedList<>();
		this.ansVar = new LinkedList<>();
		final CloseableIteratorWithoutException<Atom> it = query.getAtomSet().iterator();
		while (it.hasNext()){
			Atom a = it.next();
			CompactDisjunction disj = new CompactDisjunctionLinkedList(a);
			addDisjunction(disj);
		}
		final List<Term> cqAnsVar = query.getAnswerVariables();
		this.ansVar.addAll(cqAnsVar);
		final int arity = cqAnsVar.size();
		final Predicate ansPred = new Predicate("_ans",arity);
		final Atom ansAtom = new DefaultAtom(ansPred);
		for (int i=0;i<arity;i++){
			ansAtom.setTerm(i, cqAnsVar.get(i));
		}
		final CompactDisjunction ansDisj = new CompactDisjunctionLinkedList(ansAtom);
		addDisjunction(ansDisj);
	}

	public SemiConjunctiveQueryLinkedList(final SemiConjunctiveQuery s){
		this.disjunctions = new LinkedList<>();
		this.ansVar = s.getAnswerVariables();
		for (final CompactDisjunction disj:s.getDisjunctions()){
			this.disjunctions.add(disj);
		}
	}

	public SemiConjunctiveQueryLinkedList(final List<CompactDisjunction> disjs,
			final List<Term> ansVar) {
		this.disjunctions = new LinkedList<>(disjs);
		this.ansVar = ansVar;

	}

	@Override
	public List<Term> getAnswerVariables(){
		return this.ansVar;
	}
	@Override
	public void addDisjunction(final CompactDisjunction disj) {
		this.disjunctions.add(disj);

	}

	@Override
	public List<CompactDisjunction> getDisjunctions() {
		return this.disjunctions;
	}


	/**
	 * 
	 * @param terms a set of terms
	 * @return the index of disjunctions that contain at least a term of terms
	 */
	public Set<Integer> getDisjunctionIndex(final Set<Term> terms){
		final Set<Integer> result = new TreeSet<>();
		for (Term t:terms){
			result.addAll(this.getDisjunctionIndex(t));
		}
		return result;
	}

	/**
	 * 
	 * @param term a term
	 * @return the index of disjuctions containing term
	 */

	private Set<Integer> getDisjunctionIndex(final Term term){
		final Set<Integer> result = new TreeSet<>();
		for (int i=0;i<this.disjunctions.size();i++){
			if (this.disjunctions.get(i).getTerms().contains(term)){
				result.add(i);
			}
		}
		return result;
	}

	@Override
	public void clean(final String s){
		final Iterator<CompactDisjunction> disjIt = this.disjunctions.iterator();
		while (disjIt.hasNext()){
			final CompactDisjunction disj = disjIt.next();
			final CloseableIterator<Atom> it = disj.getAtoms().iterator();
			try{
				final Atom first = it.next();
				if (first.getPredicate().toString().contains(s)){
					disjIt.remove();
				}
			}
			catch (Exception e){
				System.err.println("There should be at least one atom");
				e.printStackTrace();
			}

		}
	}


	@Override
	public List<CompactDisjunction> getDisjunction(final Set<Term> terms){
		final Set<Integer> disjIndex = getDisjunctionIndex(terms);
		final List<CompactDisjunction> result = new ArrayList<>();
		for (Integer i:disjIndex){
			result.add(this.disjunctions.get(i));
		}
		return result;
	}


	@Override
	public Set<Term> getSharedTerms() {
		final TreeSet<Term> terms = new TreeSet<>();
		final TreeSet<Term> result = new TreeSet<>();

		for (final CompactDisjunction disj:this.disjunctions){
			for (final Term t:disj.getTerms()){
				if (!terms.add(t)){
					result.add(t);
				}
			}
		}
		return result;
	}

	/**
	 * 
	 * @param s a substitution
	 * @param source an atom
	 * @param target an atom
	 * @return null if there is no homomorphism from source to target that extends s, and such a substitution otherwise
	 */
	private static Substitution extendedSubst(final Substitution s,
			final Atom source,
			final Atom target){
		final Substitution result = new TreeMapSubstitution(s);
		if (!source.getPredicate().equals(target.getPredicate())){
			return null;
		}
		for (int i=0;i<source.getPredicate().getArity();i++){
			final Term sourceTerm = source.getTerm(i);
			final Term targetTerm = target.getTerm(i);
			if (result.getTerms().contains(sourceTerm)){
				if (!result.createImageOf(sourceTerm).equals(targetTerm)){
					return null;
				}
			}
			else{
				if (sourceTerm.isVariable())
					result.put((Variable) sourceTerm, targetTerm);
			}
		}
		return result;
	}

	//TODO: check this function, and under which condition it provides a correct implementation
	@Override
	public boolean entails(final SemiConjunctiveQuery other) {
		if (other.getAnswerVariables().size()!=this.getAnswerVariables().size()){
			return false;
		}
		//FIXME: change implementation not to rely on indexes of disjunctions
		final SemiConjunctiveQueryLinkedList toCheck = new SemiConjunctiveQueryLinkedList(other);
		//get root Atoms of other
		final List<Atom> roots = new ArrayList<>();
		for (final CompactDisjunction disj:other.getDisjunctions()){
			roots.add(disj.getRoot());
		}
		List<Substitution> partialResults = new ArrayList<>();
		//TODO: check that repeated answer variables do not cause problems
		Substitution initSubst = new TreeMapSubstitution();
		for (int i=0;i<this.getAnswerVariables().size();i++){
			// TODO: check if there is no constants in answer variables (it is allowed by Dlgp)
			initSubst.put((Variable)this.getAnswerVariables().get(i), other.getAnswerVariables().get(i));
		}
		//as a constraint, the answer variables should be mapped to corresponding answer variables
		partialResults.add(initSubst);
		//check that each disjunction is more general than an atom of root
		for (CompactDisjunction disj:this.getDisjunctions()){
			final List<Substitution> extendedResults = new ArrayList<>();
			for (Substitution s:partialResults){
				for (int i=0;i<roots.size();i++){
					try{
						CloseableIterator<Atom> atomIt = disj.getAtoms().iterator();
						//for each atom in the considered disjunction, try to expand the substitution
						while (atomIt.hasNext()){
							Atom thisAtom = atomIt.next();
							Substitution sExtended = new TreeMapSubstitution(s);
							sExtended = extendedSubst(sExtended,thisAtom,roots.get(i));
							if (sExtended!=null){
								if (!Util.isInjective(sExtended)){
									if (disj.entails(toCheck.getDisjunctions().get(i),sExtended)){
										extendedResults.add(sExtended);
									}
								}
								else{
									extendedResults.add(sExtended);
								}
							}
						}
					}
					catch(Exception e){
						e.printStackTrace();
					}
				}
			}
			if (extendedResults.isEmpty()){
				return false;
			}
			partialResults = new ArrayList<>();
			partialResults.addAll(extendedResults);
		}
		return !partialResults.isEmpty();
	}

	@Override
	public String toString(){
		final StringBuilder sb = new StringBuilder();
		sb.append(this.ansVar + "\n");
		for (CompactDisjunction disj:this.disjunctions){
			sb.append(disj.toString()  + " \n");
		}
		return sb.toString();
	}


}
