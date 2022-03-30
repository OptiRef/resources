package fr.inria.cedar.compact.corechasefinitenesschecker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import fr.lirmm.graphik.graal.api.core.RuleSet;

/**
 * 
 * @author Michael Thomazo (Inria)
 * Stores the representation of a derivation tree.
 *
 */
public class DerivationTree {

	//children maps a sharing type to the sharing types it can be a parent of in a derivation tree
	final private HashMap<SharingType,Set<Pair<SharingType,Map<Integer,Integer>>>> children;
	//parents maps a sharing type to the sharing type it can be a child of in a derivation tree
	final private HashMap<SharingType,Set<Pair<SharingType,Map<Integer,Integer>>>> parents;
	//initial set of sharing types, those that can appear in an infinite path
	final private Set<SharingType> startingtype;

	public DerivationTree(final Set<SharingType> types){
		this.startingtype = types;
		this.children = new HashMap<SharingType,Set<Pair<SharingType,Map<Integer,Integer>>>>();
		this.parents = new HashMap<SharingType,Set<Pair<SharingType,Map<Integer,Integer>>>>();
	}

	public DerivationTree(RuleSet rules){
		final SharingTypeGenerator stg = new SharingTypeGenerator(rules);
		final Set<SharingType> types = new HashSet<SharingType>();
		stg.generateStartingSharingType(types);
		this.startingtype = types;
		this.children = new HashMap<SharingType,Set<Pair<SharingType,Map<Integer,Integer>>>>();
		this.parents = new HashMap<SharingType,Set<Pair<SharingType,Map<Integer,Integer>>>>();
		for (final SharingType st:this.startingtype){
			this.children.put(st, new HashSet<Pair<SharingType,Map<Integer,Integer>>>());
			this.parents.put(st, new HashSet<Pair<SharingType,Map<Integer,Integer>>>());
		}
	}

	/**
	 * 
	 * @param parent a sharing type
	 * @param child a sharing type
	 * @param link a link
	 * @return true if and only if parent can have a child of sharing type child with link in a derivation tree
	 */
	public boolean hasLink(SharingType parent, SharingType child, Map<Integer,Integer> link){
		if (!this.children.containsKey(parent))
			return false;
		return this.children.get(parent).contains(new ImmutablePair<SharingType,Map<Integer,Integer>>(child,link));
	}
	
	/**
	 * 
	 * @param parent a sharing type
	 * @param child a sharing type
	 * @param link a link from child to parent
	 * @return true if and only if the link from child to parent through link has been added
	 * This is the only way to add links in the derivation tree. It maintains the symmetry between this.parents and this.children maps
	 */
	public boolean addLink(SharingType parent, SharingType child, Map<Integer,Integer> link){
		if (!this.children.containsKey(parent)){
			this.children.put(parent, new HashSet<Pair<SharingType,Map<Integer,Integer>>>());
		}
		boolean result = this.children.get(parent).add(new ImmutablePair<SharingType,Map<Integer,Integer>>(child,link));

		if (!this.parents.containsKey(child)){
			this.parents.put(child, new HashSet<Pair<SharingType,Map<Integer,Integer>>>());
		}
		this.parents.get(child).add(new ImmutablePair<SharingType,Map<Integer,Integer>>(parent,link));

		return result;
	}

	/**
	 * 
	 * @param parent a sharing type
	 * @param child a sharing type
	 * @param link a link from child to parent
	 * @return true if and only if the link from child to parent through link has been added
	 * This is the only way to remove links in the derivation tree. It maintains the symmetry between this.parents and this.children maps
	 */
	public boolean removeLink(SharingType parent, SharingType child, Map<Integer,Integer> link){
		if (!this.hasLink(parent, child, link)){
			return false;
		}
		this.children.get(parent).remove(new ImmutablePair<SharingType,Map<Integer,Integer>>(child,link));
		this.parents.get(child).remove(new ImmutablePair<SharingType,Map<Integer,Integer>>(parent,link));
		return false;
	}
	

	/**
	 * 
	 * @param st a sharing type
	 * @return the set of sharing types that can be a child of st in a derivation tree and the corresponding link
	 */
	public Set<Pair<SharingType,Map<Integer,Integer>>> getChildren(SharingType st){
		if (!this.children.containsKey(st)){
			return new HashSet<Pair<SharingType,Map<Integer,Integer>>>();
		}
		else{
			return new HashSet<Pair<SharingType,Map<Integer,Integer>>>(this.children.get(st));
		}
	}
	
	/**
	 * 
	 * @param st a sharing type
	 * @return the set of sharing types that can be a parent of st in a derivation tree and the corresponding link
	 */
	public Set<Pair<SharingType,Map<Integer,Integer>>> getParents(SharingType st){
		if (!this.parents.containsKey(st)){
			return new HashSet<Pair<SharingType,Map<Integer,Integer>>>();
		}
		else{
			return new HashSet<Pair<SharingType,Map<Integer,Integer>>>(this.parents.get(st));
		}
	}
	
	/**
	 * 
	 * @return the set of sharing type that appears in our representation of derivation trees
	 */
	public Set<SharingType> getSharingTypes(){
		Set<SharingType> result = new HashSet<SharingType>();
		result.addAll(this.children.keySet());
		result.addAll(this.parents.keySet());
		return result;
	}
	
	@Override 
	public String toString(){
		final StringBuilder sb = new StringBuilder();
		sb.append("Children:\n");
		for (SharingType st: this.children.keySet()){
			sb.append(st.p + " " + st.sharedPositions + " " + st.partition  + "\n");
			for (Pair<SharingType,Map<Integer,Integer>> child:this.children.get(st)){
				sb.append("  " + child.getLeft().p 
						+ " " + child.getLeft().sharedPositions 
						+ " " + child.getLeft().partition
						+ " " + child.getRight() + "\n");
			}
			sb.append("\n");
		}
		return sb.toString();
	}
}

