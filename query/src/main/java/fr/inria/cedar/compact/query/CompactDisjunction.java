package fr.inria.cedar.compact.query;

import java.util.Collection;

import fr.lirmm.graphik.graal.api.core.Atom;
import fr.lirmm.graphik.graal.api.core.AtomSet;
import fr.lirmm.graphik.graal.api.core.Substitution;
import fr.lirmm.graphik.graal.api.core.Term;


/**
 * Representation of a disjunction of atoms for Compact
 * @author Michael Thomazo (INRIA)
 * 
 */
public interface CompactDisjunction {

	
	/**
	 * @param a
	 * adds a to the disjunction
	 */
	public void addAtom(Atom a);
	
	
	/**
	 * @return the set of atoms contained in the disjunction 
	 */
	public AtomSet getAtoms();
	
	/**
	 * @return true if and only if the disjunction contains a single atom
	 */
	
	public boolean isAtomicDisjunction();
	/**
	 * 
	 * @return the set of terms in the disjunction
	 */
	
	public Collection<Term> getTerms();
	/**
	 * @param a an atom
	 * @param terms a set of terms
	 * @return true if the disjunction contains an atom that equals a up to a homomorphism that is the identity on terms
	 * 
	 */
	
	public boolean contains(Atom a,Collection<Term> t);
	
	/**
	 * 
	 * @return a root of the disjunction, i.e., the atom from which all the other atoms can be obtained through local unifications
	 */
	public Atom getRoot();
	

	/**
	 * 
	 * @param other a compactdisjunction
	 * @param s a substitution
	 * @return true if and only if other is entailed by this, and such that the images defined by s are respected
	 */
	public boolean entails(CompactDisjunction other,Substitution s);
	@Override
	public String toString();
}
