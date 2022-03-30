package fr.inria.cedar.compact.corechasefinitenesschecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

/**
 * 
 * @author Michael Thomazo (INRIA)
 * Implementation of Tarjan's algorithm as described 
 * at https://en.wikipedia.org/wiki/Tarjan's_strongly_connected_components_algorithm
 *
 */
public class CycleDetector {

	
	DerivationTree dt;
	
	public CycleDetector(DerivationTree dt){
		this.dt = dt;
	}
	
	/**
	 * helper method for Tarjan algorithm. 
	 * @param s
	 * @param count
	 * @param index
	 * @param lowLink
	 * @param onStack
	 * @param S
	 * @param sccs
	 */
	private void strongConnect(SharingType s,
			Integer count,
			Map<SharingType,Integer> index, 
			Map<SharingType,Integer> lowLink, 
			Set<SharingType> onStack, ArrayList<SharingType> S,Set<Set<SharingType>> sccs){
		index.put(s, count);
		lowLink.put(s, count);
		count++;
		S.add(s);
		onStack.add(s);

		Set<Pair<SharingType,Map<Integer,Integer>>> successors = this.dt.getChildren(s);
		for (Pair<SharingType,Map<Integer,Integer>> succ:successors){
			if (!index.containsKey(succ.getLeft())){
				strongConnect(succ.getLeft(), count, index, lowLink, onStack, S,sccs);
				if (lowLink.get(succ.getLeft()) < lowLink.get(s)){
					lowLink.put(s, lowLink.get(succ.getLeft()));
				}
			}
			else{
				if (onStack.contains(succ.getLeft())){
					if (index.get(succ.getLeft()) < lowLink.get(s)){
						lowLink.put(s, index.get(succ.getLeft()));
					}
				}
			}
		}

		if (lowLink.get(s).equals(index.get(s))){
			int n = S.size()-1;
			Set<SharingType> scc = new HashSet<SharingType>();
			while (!S.get(n).equals(s)){
				scc.add(S.get(n));
				onStack.remove(S.get(n));
				S.remove(n);
				n--;
			}
			scc.add(S.get(n));
			onStack.remove(S.get(n));
			S.remove(n);
			sccs.add(scc);
		}

	}
	
	/**
	 * 
	 * @return true if and only if the dag of sharing types represented in dt is cyclic
	 */
	public boolean isCyclicByTarjan(){
		Integer count = 0;
		Map<SharingType,Integer> index = new HashMap<SharingType,Integer>();
		Map<SharingType,Integer> lowLink = new HashMap<SharingType,Integer>();
		Set<SharingType> onStack = new HashSet<SharingType>();
		ArrayList<SharingType> S = new ArrayList<SharingType>();
		Set<Set<SharingType>> sccs = new HashSet<Set<SharingType>>();
		for (SharingType s:dt.getSharingTypes()){
			if (!index.containsKey(s)){
				strongConnect(s,count,index,lowLink,onStack,S,sccs);
			}
		}

		for (Set<SharingType> scc:sccs){
			if (scc.size() > 1){
				System.out.println("An example of scc is:" + scc);
				return true;
			}
			SharingType st = scc.iterator().next();
			Set<Pair<SharingType,Map<Integer,Integer>>> children = dt.getChildren(st);
			for (Pair<SharingType,Map<Integer,Integer>> child:children){
				if (child.getLeft().equals(st)){
					System.out.println("An example of scc is" + scc);
					return true;
				}
			}
		}
		return false;
	}

}
