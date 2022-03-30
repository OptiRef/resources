package fr.inria.cedar.compact.nonlocalrewriter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import fr.inria.cedar.compact.mess.Util;
import fr.inria.cedar.compact.query.CompactDisjunction;
import fr.inria.cedar.compact.query.SemiConjunctiveQuery;
import fr.inria.cedar.compact.query.SemiConjunctiveQueryLinkedList;
import fr.lirmm.graphik.graal.api.core.Atom;
import fr.lirmm.graphik.graal.api.core.Rule;
import fr.lirmm.graphik.graal.api.core.Substitution;
import fr.lirmm.graphik.graal.api.core.Term;
import fr.lirmm.graphik.graal.api.core.Variable;
import fr.lirmm.graphik.graal.core.TreeMapSubstitution;
import fr.lirmm.graphik.util.stream.CloseableIterator;

/**
 * 
 * @author Michael Thomazo (INRIA)
 *
 */
public class PartialUnifierStorer {

	SemiConjunctiveQueryLinkedList scq;
	Rule rule;
	Set<Term> shared;
	List<List<Unifier>> unifierBuilder;


	public PartialUnifierStorer(SemiConjunctiveQuery s, Rule r){
		if (s instanceof SemiConjunctiveQueryLinkedList){
			this.scq = (SemiConjunctiveQueryLinkedList) s;
		}
		else{
			this.scq = new SemiConjunctiveQueryLinkedList(s);
		}
		this.rule = r;
		this.shared = s.getSharedTerms();
		this.unifierBuilder = new LinkedList<>();
	}


	public void printUnifiers(){
		for (int i=0;i<this.unifierBuilder.size();i++){
			System.out.println("Unifiers for index " + i);
			for (int j=0;j<this.unifierBuilder.get(i).size();j++){
				System.out.println(this.unifierBuilder.get(i).get(j));
			}
		}
	}
	/**
	 * 
	 * @param partialUnifiers: a list of partial unifiers to be merged
	 * @return the unifier result of the merge
	 */

	private Unifier mergeUnifiers(List<Unifier> partialUnifiers){
		if (partialUnifiers.size()==1){
			return partialUnifiers.get(0);
		}
		Unifier initUnif = partialUnifiers.get(0);
		Substitution subst = initUnif.getSubstitution();
		Set<Integer> disjs = new TreeSet<>();
		disjs.addAll(initUnif.getDisjunctionIndex());
		for (int i=1;i<partialUnifiers.size();i++){
			subst = subst.aggregate(partialUnifiers.get(i).getSubstitution());
			if (subst == null){
				return null;
			}
			disjs.addAll(partialUnifiers.get(i).getDisjunctionIndex());
		}
		Set<Term> sharedExist = new TreeSet<>();
		for (Term t:this.scq.getSharedTerms()){
			for (Term e:this.rule.getExistentials()){
				if (subst.createImageOf(t).equals(subst.createImageOf(e))){
					sharedExist.add(t);
				}
			}
		}

		return new 
				Unifier(initUnif.getQuery(),
						subst, 
						sharedExist, 
						disjs, 
						initUnif.getRule());
	}


	/**
	 * 
	 * @param disjs a set of indices of disjunctions, which must be unified additionally to those already unified by u to get a piece-based unifier
	 * @param u: a unifier 
	 * @return a list of lists of unifiers whose merging would be unifier unifying all disjunction unified by u and disjunctions from disjs
	 */
	private List<List<Unifier>> generateExpandedUnifiers(Set<Integer> disjs, Unifier u){
		List<List<Unifier>> expandedUnifiers = new ArrayList<>();
		List<Unifier> currentUnifier = new ArrayList<>();
		currentUnifier.add(u);
		expandedUnifiers.add(currentUnifier);
		for (Integer i:disjs){
			expandedUnifiers.add(this.unifierBuilder.get(i.intValue()));
		}
		return Util.computeCartesianProduct(expandedUnifiers);
	}


	/**
	 * 
	 * @param disjs a set of indices of disjunctions, which must be unified additionally to those already unified by u to get a piece-based unifier
	 * @param u: a unifier 
	 * @return the set of potential unifiers unifying all disjunction unified by u and disjunctions from disjs
	 */
	private List<Unifier> checkedExpandedUnifiers(Set<Integer> disjs,Unifier u){
		List<List<Unifier>> expandedUnifiers = generateExpandedUnifiers(disjs,u);
		List<Unifier> mergedUnifiers = expandedUnifiers.stream()
				.map(l -> mergeUnifiers(l))
				.filter(e -> e!=null)
				.collect(Collectors.toList());
		return mergedUnifiers;
	}

	/**
	 * 
	 * @param u a unifier
	 * @return the set of unifiers piece based unifiers that can be obtained by expanding u until getting a piece-based unifier
	 */
	private List<Unifier> generateUnifiers(Unifier u){
		List<Unifier> result = new ArrayList<>();
		List<Unifier> toExpand = new ArrayList<>();
		toExpand.add(u);
		while (!toExpand.isEmpty()){
			List<Unifier> next = new ArrayList<>();
			for (Unifier unif:toExpand){
				Set<Integer> disjs = unif.needExpansion();
				if (!disjs.isEmpty()){
					next.addAll(checkedExpandedUnifiers(disjs, unif));
				}
				else{
					if (unif.isPieceBasedUnifier()){
						result.add(unif);
					}
				}
			}
			toExpand = next;
		}
		return result;
	}

	/**
	 * 
	 * @param t a term
	 * @return a list of unifiers that unifies all disjunctions where t appears
	 */
	private List<Unifier> initializeUnifiers(Term t){
		List<List<Unifier>> initUnifiers = new ArrayList<>();
		Set<Term> tSet = new TreeSet<>();
		tSet.add(t);
		Set<Integer> initIntegers = this.scq.getDisjunctionIndex(tSet);
		for (Integer i:initIntegers){
			initUnifiers.add(this.unifierBuilder.get(i));
		}
		return Util.computeCartesianProduct(initUnifiers).stream()
				.map(l -> mergeUnifiers(l))
				//.filter(l --> l!=null)
				.collect(Collectors.toList());
	}


	/**
	 * 
	 * @return the set of non local unifiers of this.scq with this.rule
	 */
	public List<Unifier> generateUnifiers(){
		List<Unifier> result = new ArrayList<>();
		for (Term t:this.shared){
			List<Unifier> initUnifiers = initializeUnifiers(t);
			for (Unifier u:initUnifiers){
				result.addAll(generateUnifiers(u));
			}
		}
		for (List<Unifier> unifs:this.unifierBuilder){
			for (Unifier unif:unifs){
				if (unif.isAlmostLocal()){
					result.add(unif);
				}
			}
		}
		return result;
	}


	/**
	 * populate unifierBuilder with all unifiers that are potentially part of a non-local unifier
	 */
	public void populateUnifierBuilder(){
		Atom headAtom = this.rule.getHead().iterator().next();
		//in each disjunction, look for atoms that unifier non locally
		for (int i=0;i<this.scq.getDisjunctions().size();i++){
			CompactDisjunction disj = this.scq.getDisjunctions().get(i);
			LinkedList<Unifier> disjUnifiers = new LinkedList<>();
			CloseableIterator<Atom> atomIt = disj.getAtoms().iterator();
			try{
				while (atomIt.hasNext()){
					Atom disjAtom = atomIt.next();
					if (!disjAtom.getPredicate().equals(headAtom.getPredicate())){
						continue;
					}
					Unifier unifier = unifiesAtom(disjAtom,
							headAtom,
							this.rule.getExistentials(),
							this.rule.getFrontier(),
							i);
					if (unifier!=null && !isLocal(unifier.getSubstitution()
							,this.rule.getExistentials(),this.shared)){
						disjUnifiers.add(unifier);
					}
				}
			}
			catch (Exception e){
				e.printStackTrace();
			}
			this.unifierBuilder.add(disjUnifiers);
		}
	}


	/**
	 * 
	 * @param subst a substitution
	 * @param existentials a set of terms that are the existential variables of the rule whose head is unifier
	 * @param sharedTerms a set of shared terms
	 * @return true iff subst correspond to a local unification
	 */
	public boolean isLocal(Substitution subst, Set<Variable> existentials, Set<Term> sharedTerms){
		if (!Util.isLinear(this.rule)){
			return false;
		}
		for (Term t1:sharedTerms){
			Term t1im = subst.createImageOf(t1);
			if (t1im.isConstant()){
				return false;
			}
			for (Term t2:sharedTerms){
				if (t1im.equals(subst.createImageOf(t2))&& !t1.equals(t2)){
					return false;
				}
			}
			for (Term t2:existentials){
				if (t1im.equals(subst.createImageOf(t2))){
					return false;
				}
			}
		}
		for (Term t1:existentials){
			Term t1im = subst.createImageOf(t1);
			if (t1im.isConstant()){
				return false;
			}
			for (Term t2:existentials){
				if (t1im.equals(subst.createImageOf(t2))){
					if (!t1.equals(t2)){
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * 
	 * @param disjAtom 
	 * @param headAtom
	 * @param existentials: the set of existential variables to be considered
	 * @return null if there is no valid unifier between disjAtom and headAtom, otherwise
	 * a pair containing this unifier and the set of terms of disjAtom that are unified with
	 * an existential variable of headAtom
	 */

	private Unifier unifiesAtom(Atom disjAtom,
			Atom headAtom, 
			Set<Variable> existentials, 
			Set<Variable> frontier, 
			int disjIndex){
		Substitution mgu = mostGeneralUnifier(disjAtom,headAtom);
		Set<Term> sharedExist = new TreeSet<>();

		if (checkValidityOfMostGeneralUnifier(mgu, existentials, frontier)){
			for (Term t:existentials){
				for (Term s:this.scq.getSharedTerms()){
					if (mgu.createImageOf(t).equals(mgu.createImageOf(s))){
						sharedExist.add(s);
					}
				}
			}
		}
		else{
			return null;
		}
		Set<Integer> disjIndexes = new TreeSet<>();
		disjIndexes.add(Integer.valueOf(disjIndex));
		Unifier result = new Unifier(this.scq,
				mgu,
				sharedExist,
				disjIndexes,
				this.rule);
		return result;
	}

	/**
	 * 
	 * @param disjAtom
	 * @param headAtom
	 * @return the most general unifier between disjAtom and headAtom
	 */
	public static Substitution mostGeneralUnifier(Atom disjAtom, Atom headAtom){
		Substitution mgu = new TreeMapSubstitution();
		int arity = headAtom.getPredicate().getArity();
		for (int i=0;i<arity;i++){
			Substitution toCompose = new TreeMapSubstitution();
			if (headAtom.getTerm(i).isConstant()){
				if (disjAtom.getTerm(i).isConstant()){
					if (!headAtom.getTerm(i).equals(disjAtom.getTerm(i))){
						return null;
					}
				}
				else{
					toCompose.put((Variable) disjAtom.getTerm(i), headAtom.getTerm(i));
				}
			}
			else{
				toCompose.put((Variable) headAtom.getTerm(i), disjAtom.getTerm(i));
			}
			mgu = mgu.aggregate(toCompose);	
		}
		return mgu;
	}

	/**
	 * 
	 * @param mgu
	 * @param existentials
	 * @param frontier
	 * @return checks whether mgu unifies two existentials, or an existential and a constant, or an existential and a frontier variable
	 */
	public static boolean checkValidityOfMostGeneralUnifier(final Substitution mgu, final Set<Variable> existentials, final Set<Variable> frontier){
		for (Term t:existentials){
			if (mgu.createImageOf(t).isConstant()){
				return false;
			}
			for (Term t1:existentials){
				if (!t1.equals(t)&&mgu.createImageOf(t).equals(mgu.createImageOf(t1))){
					return false;
				}
			}
			for (Term t1:frontier){
				if (mgu.createImageOf(t).equals(mgu.createImageOf(t1))){
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public String toString(){
		String res = "";
		res = res + this.scq.toString();
		res = res + this.rule.toString();
		res = res + this.shared.toString();
		for (List<Unifier> list: this.unifierBuilder){
			for (Unifier pair:list){
				res = res + pair.toString() + " \n";
			}
		}
		return res;
	}
}
