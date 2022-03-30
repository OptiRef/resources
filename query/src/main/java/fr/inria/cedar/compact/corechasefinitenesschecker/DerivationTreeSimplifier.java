package fr.inria.cedar.compact.corechasefinitenesschecker;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import fr.inria.cedar.compact.mess.Util;
import fr.lirmm.graphik.graal.api.core.Atom;
import fr.lirmm.graphik.graal.api.core.ConjunctiveQuery;
import fr.lirmm.graphik.graal.api.core.InMemoryAtomSet;
import fr.lirmm.graphik.graal.api.core.Substitution;
import fr.lirmm.graphik.graal.api.core.Term;
import fr.lirmm.graphik.graal.core.DefaultConjunctiveQuery;
import fr.lirmm.graphik.graal.core.atomset.LinkedListAtomSet;
import fr.lirmm.graphik.graal.homomorphism.SmartHomomorphism;
import fr.lirmm.graphik.util.stream.CloseableIterator;

public class DerivationTreeSimplifier {

	final DerivationTree dt;
	//will be removed after proper implementation of the detection of sibling redundancy
	//final RuleSet rules;

	public DerivationTreeSimplifier(final DerivationTree dt){
		this.dt = dt;
		//this.rules = null;
	}


	/**
	 * removeInexistantChildren cleans the maps parents and children by removing pair 
	 * corresponding do bags that are attached higher in the derivation tree 
	 */
	private void removeInexistantChildren(){
		//clean the derivation tree
		for (SharingType st:dt.getSharingTypes()){
			final Set<Pair<SharingType,Map<Integer,Integer>>> childLinks = dt.getChildren(st);
			final Set<Pair<SharingType,Map<Integer,Integer>>> toRemove = new HashSet<Pair<SharingType,Map<Integer,Integer>>>();
			for (Pair<SharingType,Map<Integer,Integer>> childLink:childLinks){
				if (st.sharedPositions.containsAll(childLink.getRight().values())){
					//the image of the shared variables of the checked children is included in shared variables of the parent
					toRemove.add(childLink);
				}
			}
			for (Pair<SharingType,Map<Integer,Integer>> rem:toRemove){
				dt.removeLink(st, rem.getLeft(), rem.getRight());
			}
		}
	}




	/**
	 * 
	 * @param child a sharing type and its link with parent
	 * @param parent a sharing type
	 * @return true if and only if there is a folding from child to parent
	 */
	private boolean isChildRedundant(final Pair<SharingType,Map<Integer,Integer>> child,
			final SharingType parent){

		//creating the atomset for the parent, freezing shared variables
		final Pair<Atom,Map<Integer,Term>> atomCreator = parent.createAtom(child.getRight().values());
		final InMemoryAtomSet parentAtomSet = new LinkedListAtomSet();
		final Atom parentAtom = atomCreator.getLeft();
		parentAtomSet.add(parentAtom);

		//creating the atomset for the child, with the same freezing
		final Atom childAtom = child.getLeft().createAtom(atomCreator.getRight(),child.getRight());
		final InMemoryAtomSet childAtomSet = new LinkedListAtomSet();
		childAtomSet.add(childAtom);
		final ConjunctiveQuery cqf = new DefaultConjunctiveQuery(childAtomSet); 

		//checking the entailment
		final CloseableIterator<Substitution> results;
		try {
			results = SmartHomomorphism.instance().execute(cqf, parentAtomSet);
			return results.hasNext();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return false;
	}



	/**
	 * 
	 * @param st a sharing type
	 * @param toRemove a set of parent child relationships which are child redundant
	 * populate toRemove with all the pairs st,stChild where stChild is child redundant with respect to st
	 */
	private void enforcingChildSafety(final SharingType st,
			final Set<Pair<SharingType,Pair<SharingType,Map<Integer,Integer>>>> toRemove){
		for (Pair<SharingType,Map<Integer,Integer>> stChildren:dt.getChildren(st)){
			if (isChildRedundant(stChildren,st)){
				toRemove.add(new ImmutablePair<SharingType, Pair<SharingType,Map<Integer,Integer>>>(st, stChildren));
			}
		}
	}

	/**
	 * ensure that the derivation trees that are represented by this.children are child safe
	 */
	private void enforcingChildSafety(){
		final Set<Pair<SharingType,Pair<SharingType,Map<Integer,Integer>>>> toRemove = 
				new HashSet<Pair<SharingType,Pair<SharingType,Map<Integer,Integer>>>>();
		//gathering child safety violations
		for (SharingType st:dt.getSharingTypes()){
			enforcingChildSafety(st,toRemove);
		}

		//removing child safety violations
		for (Pair<SharingType,Pair<SharingType,Map<Integer,Integer>>> removedElement:toRemove){
			dt.removeLink(removedElement.getLeft(),
					removedElement.getRight().getLeft(),
					removedElement.getRight().getRight());
		}
	}



	/**
	 * 
	 * @param root the current atom on which we want to map toRemove
	 * @param toRemove the atom to be mapped. Never changes in recursive calls
	 * @param pos2Term a mapping from the positions of the original root to freezed terms
	 * @param pos2pos a mapping from the position of root to the positions of the original root
	 * @param checked a set of pairs (st,link) for which entailment has already been checked
	 * @return true if and only if there is a mapping from toRemove to root or one of its descendant
	 *  that is not "equivalent" (same sharing type, same link to the original atom) to one already in checked.
	 */

	private boolean mapsToDescendant(final SharingType root, 
			final ConjunctiveQuery toRemove, 
			final Map<Integer,Term> pos2Term, 
			final Map<Integer,Integer> pos2pos,
			final Set<Pair<SharingType,Map<Integer,Integer>>> checked){
		final Pair<SharingType,Map<Integer,Integer>> descendant = new ImmutablePair<SharingType,Map<Integer,Integer>>(root,pos2pos);
		if (checked.contains(descendant)){
			return false;
		}
		checked.add(descendant);
		//create the atom set of the atom of root
		final Atom rootAtom = root.createAtom(pos2Term, pos2pos);
		final InMemoryAtomSet rootAtomSet = new LinkedListAtomSet();
		rootAtomSet.add(rootAtom);

		//check if toRemove maps to root
		final CloseableIterator<Substitution> results;
		try {
			results = SmartHomomorphism.instance().execute(toRemove, rootAtomSet);
			if (results.hasNext()){
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		//reaching this point means that reRemove does not map to the atom of root
		boolean result = false;

		//we check for all children of root
		for (Pair<SharingType,Map<Integer,Integer>> child:dt.getChildren(root)){
			final Map<Integer,Integer> childPos2Pos = Util.composition(pos2pos, child.getRight());
			result = result || mapsToDescendant(child.getLeft(), toRemove, pos2Term, childPos2Pos, checked);
		}
		return result;
	}


	/**
	 * 
	 * @param parent a sharing type
	 * @param siblingToRemove a child of parent
	 * @param sibling a child of parent
	 * @return true if and only if siblingToRemove is sibling redundant w.r.t. sibling, in the binary case. 
	 */
	//TODO: remove this function when the Narycase is checked thoroughly
	/*
	boolean isSiblingRedundantBinary(final SharingType parent,
			final Pair<SharingType,Map<Integer,Integer>> siblingToRemove,
			final Pair<SharingType,Map<Integer,Integer>> sibling){
		boolean result = false;
		final Set<Integer> freezedPositions = new HashSet<Integer>(siblingToRemove.getRight().values());
		freezedPositions.addAll(sibling.getRight().values());
		final Pair<Atom,Map<Integer,Term>> atomCreator = parent.createAtom(freezedPositions);
		final Atom siblingToRemoveAtom = siblingToRemove.getLeft().createAtom(atomCreator.getRight(), siblingToRemove.getRight());
		final Atom siblingAtom = sibling.getLeft().createAtom(atomCreator.getRight(), sibling.getRight());
		final InMemoryAtomSet siblingToRemoveAtomSet = new LinkedListAtomSet();
		siblingToRemoveAtomSet.add(siblingToRemoveAtom);
		final InMemoryAtomSet siblingAtomSet = new LinkedListAtomSet();
		siblingAtomSet.add(siblingAtom);
		final ConjunctiveQuery cqf = new DefaultConjunctiveQuery(siblingToRemoveAtomSet); 
		final CloseableIterator<Substitution> results;
		try {
			results = SmartHomomorphism.instance().execute(cqf, siblingAtomSet);
			return results.hasNext();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return result;
	}
	 */

	//TODO: test this function!
	/**
	 * 
	 * @param parent a sharing type
	 * @param siblingToRemove a sharing type, child of parent, together with the link function
	 * @param sibling a sharing type, child of parent, together with the link function
	 * @return true if and only if siblingToRemove can be folded to the descendant of sibling (including itself) by being the identity on terms of parent
	 */
	private boolean isSiblingRedundantNary(final SharingType parent,
			final Pair<SharingType,Map<Integer,Integer>> siblingToRemove,
			final Pair<SharingType,Map<Integer,Integer>> sibling){

		//freezes the variables appearing in parent and at least one of the two siblings considered
		final Set<Integer> freezedPositions = new HashSet<Integer>(siblingToRemove.getRight().values());
		freezedPositions.addAll(sibling.getRight().values());
		final Pair<Atom,Map<Integer,Term>> atomCreator = parent.createAtom(freezedPositions);

		//generate the query for the atom to potentially remove
		final Atom siblingToRemoveAtom = siblingToRemove.getLeft().createAtom(atomCreator.getRight(), siblingToRemove.getRight());
		final InMemoryAtomSet siblingToRemoveAtomSet = new LinkedListAtomSet();
		siblingToRemoveAtomSet.add(siblingToRemoveAtom);
		final ConjunctiveQuery cqf = new DefaultConjunctiveQuery(siblingToRemoveAtomSet); 

		//checks if cqf maps to a descendant of sibling (including sibling)
		return mapsToDescendant(sibling.getLeft(), 
				cqf, 
				atomCreator.getRight(), 
				sibling.getRight(), 
				new HashSet<Pair<SharingType,Map<Integer,Integer>>>());
	}




	/**
	 * 
	 * @param st a sharing type
	 * @param toRemove a set of pairs, which is filed by auxiliary methods to contain children of st that can be removed 
	 * due to the presence of another child
	 */
	private void enforcingSiblingSafety(final SharingType st,
			final Set<Pair<SharingType,Pair<SharingType,Map<Integer,Integer>>>> toRemove){
		for (Pair<SharingType,Map<Integer,Integer>> stChildren:dt.getChildren(st)){
			if (!toRemove.contains(new ImmutablePair<SharingType,Pair<SharingType,Map<Integer,Integer>>>(st,stChildren))){
				//stChildren is not known to be sibling redundant
				for (Pair<SharingType,Map<Integer,Integer>> stChildrenBis:dt.getChildren(st)){
					if (!stChildren.equals(stChildrenBis)&&
							!toRemove.contains(new ImmutablePair<SharingType,Pair<SharingType,Map<Integer,Integer>>>(st,stChildrenBis))){
						//do not remove a node because of itself or because of a removed sibling
						if (isSiblingRedundantNary(st,stChildren,stChildrenBis)){
							toRemove.add(new ImmutablePair<SharingType, Pair<SharingType,Map<Integer,Integer>>>(st, stChildren));
						}
					}
				}
			}
		}
	}

	/**
	 * ensure that the derivation trees that are represented by this.children are sibling safe
	 */
	private void enforcingSiblingSafety(){
		final Set<Pair<SharingType,Pair<SharingType,Map<Integer,Integer>>>> toRemove = 
				new HashSet<Pair<SharingType,Pair<SharingType,Map<Integer,Integer>>>>();
		//populating the toRemove set
		for (SharingType st:dt.getSharingTypes()){
			enforcingSiblingSafety(st,toRemove);
		}

		//removing the elements of toRemove
		for (Pair<SharingType,Pair<SharingType,Map<Integer,Integer>>> removedElement:toRemove){
			dt.removeLink(removedElement.getLeft(),
					removedElement.getRight().getLeft(),
					removedElement.getRight().getRight());
		}
	}

	/**
	 *  makes this.dt a syntactically correct child safe sibling safe derivation tree.
	 */
	public void simplify(){
		removeInexistantChildren();
		enforcingChildSafety();
		enforcingSiblingSafety();
	}

}
