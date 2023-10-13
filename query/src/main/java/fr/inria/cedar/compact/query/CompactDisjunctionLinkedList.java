package fr.inria.cedar.compact.query;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import fr.lirmm.graphik.graal.api.core.Atom;
import fr.lirmm.graphik.graal.api.core.AtomSet;
import fr.lirmm.graphik.graal.api.core.InMemoryAtomSet;
import fr.lirmm.graphik.graal.api.core.Substitution;
import fr.lirmm.graphik.graal.api.core.Term;
import fr.lirmm.graphik.graal.api.core.Variable;
import fr.lirmm.graphik.graal.core.TreeMapSubstitution;
import fr.lirmm.graphik.graal.core.atomset.LinkedListAtomSet;
import fr.lirmm.graphik.util.stream.CloseableIterator;
import fr.lirmm.graphik.util.stream.CloseableIteratorWithoutException;

/**
 * 
 * @author Michael Thomazo (INRIA)
 *
 */
public class CompactDisjunctionLinkedList implements CompactDisjunction {

	//the set of atoms in the disjunction
	InMemoryAtomSet atoms;
	//the set of terms appearing in the disjunction
	TreeSet<Term> terms;
	//the root of the disjunction, i.e. the atom from which all other atoms have been obtained through sequences of local unifications
	Atom root;

	public CompactDisjunctionLinkedList(final Atom a){
		this.atoms = new LinkedListAtomSet();
		this.atoms.add(a);
		this.terms = new TreeSet<>();
		this.terms.addAll(a.getTerms());
		this.root = a;
	}

	public CompactDisjunctionLinkedList(final InMemoryAtomSet a,final Atom r){
		this.atoms = a;
		this.root = r;
		this.terms = new TreeSet<>();
		final CloseableIteratorWithoutException<Atom> it = a.iterator();
		while(it.hasNext()){
			final Atom atom = it.next();
			this.terms.addAll(atom.getTerms());
		}
	}

	@Override
	public void addAtom(final Atom a){
		this.atoms.add(a);
		this.terms.addAll(a.getTerms());
	}

	@Override
	public AtomSet getAtoms(){
		return this.atoms;
	}

	@Override
	public boolean isAtomicDisjunction() {
		CloseableIteratorWithoutException<Atom> it = this.atoms.iterator();
		if (it.hasNext()){
			it.next();
			return !it.hasNext();
		}
		else
			return false;
	}



	@Override
	public boolean contains(final Atom a, final Collection<Term> fixedTerms){
		final CloseableIteratorWithoutException<Atom> it = this.atoms.iterator();
		while (it.hasNext()){
			final Atom entailing = it.next();
			if (contains(entailing, a, fixedTerms)){
				return true;
			}
		}
		return false;
	}

	//FIXME: shouldn't this be somewhere else?
	/**
	 * 
	 * @param entailing an 
	 * @param entailed
	 * @param terms
	 * @return true if and only if there is a homomorphism from entailed to entailing
	 * which is the identity on terms
	 */

	private static boolean contains(final Atom entailing, 
			final Atom entailed, 
			final Collection<Term> terms){
		int arity = entailed.getPredicate().getArity();
		final Substitution hm = new TreeMapSubstitution();
		if (entailing.getPredicate().equals(entailed.getPredicate())){
			final List<Term> entailedTerms = entailed.getTerms();
			for (int i=0;i<arity;i++){
				final Term entailedI = entailedTerms.get(i);
				final Term entailingI = entailing.getTerm(i);
				if (entailedI.isConstant()){
					if (!entailingI.equals(entailedI)){
						return false;
					}
				}
				else{
					if (terms.contains(entailedI)){
						if (!entailedI.equals(entailingI)){
							return false;
						}
					}
					else{
						if (hm.getTerms().contains(entailedI)){
							if (!entailingI.equals(hm.createImageOf(entailedI))){
								return false;
							}
						}
						else{
							hm.put((Variable) entailedI, entailingI);
						}
					}
				}
			}
			return true;
		}
		return false;

	}

	@Override
	public Collection<Term> getTerms() {
		return this.terms;
	}

	@Override
	public Atom getRoot(){
		return this.root;
	}

	/**
	 * 
	 * @param s
	 * @return an image of this obtained by applying s to each atom
	 */
	private CompactDisjunction buildImage(final Substitution s){
		//FIXME: should create a new constructor?
		CompactDisjunction result = null;
		try{
			CloseableIterator<Atom> thisIt = this.atoms.iterator();
			result = new CompactDisjunctionLinkedList(thisIt.next());
			while (thisIt.hasNext()){
				result.addAtom(s.createImageOf(thisIt.next()));
			}
		}
		catch (Exception e){
			e.printStackTrace();
		}
		return result;
	}

	@Override 
	public boolean entails(final CompactDisjunction other, 
			final Substitution s){
		final AtomSet otherAtoms = other.getAtoms();		
		final CloseableIterator<Atom> otherIt = otherAtoms.iterator();
		final Set<Term> fixed = s.getValues();
		final CompactDisjunction entailing = this.buildImage(s);
		try{
			while (otherIt.hasNext()){
				if (!entailing.contains(otherIt.next(), fixed)){
					return false;
				}
			}
		}
		catch (Exception e){
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public String toString(){
		final StringBuilder sb = new StringBuilder();
		final CloseableIteratorWithoutException<Atom> it = this.atoms.iterator();
		while (it.hasNext()){
			Atom a = it.next();
			sb.append(a.toString() + ". ");
		}
		return sb.toString();
	}
}

