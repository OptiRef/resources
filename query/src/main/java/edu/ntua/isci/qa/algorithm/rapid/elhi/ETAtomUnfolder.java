/*Copyright 2011, 2013, 2015 Alexandros Chortaras

 This file is part of Rapid.

 Rapid is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Rapid is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Rapid.  If not, see <http://www.gnu.org/licenses/>.*/

package edu.ntua.isci.qa.algorithm.rapid.elhi;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import edu.ntua.isci.qa.algorithm.rapid.AtomUnfolder;
import edu.ntua.isci.qa.algorithm.rapid.AtomUnfolding;
import edu.ntua.isci.qa.algorithm.rapid.AtomUnfoldingResult;
import edu.ntua.isci.qa.algorithm.rapid.FlattenerExtra;
import edu.ntua.isci.qa.algorithm.rapid.ProperRenamedAtom;
import edu.ntua.isci.qa.algorithm.rapid.dllite.DLAtomUnfolding;
import edu.ntua.isci.qa.lp.theory.FCTheory;
import edu.ntua.isci.common.lp.Atom;
import edu.ntua.isci.common.lp.Term;
import edu.ntua.isci.common.utils.Counter;

public class ETAtomUnfolder extends AtomUnfolder {

	private Counter newUsedBoundVarsCounter; 
	private Counter newUsedUnboundVarsCounter;
	
	private Map<Atom, AtomUnfolding> map;
	
	private Map<Atom, VUnfoldStructure> cmap;
	
	private ETFlattener flattener;

	public ETAtomUnfolder(FCTheory lp, ETFlattener flattener) {
		super(lp);
		this.flattener = flattener;
		
		reset();
	}
	
	public void reset() {
		map = new TreeMap<Atom, AtomUnfolding>();
		
		cmap = new TreeMap<>();

		newUsedBoundVarsCounter = new Counter(1); 
		newUsedUnboundVarsCounter = new Counter(1);
	}
	
	public VUnfoldStructure getFullyComputedVUS(Atom atom) {
		if (isDatalog() || isUnfold()) {
			return cmap.get(atom);
		} else {
			return null;
		}
	}

	public boolean isUnfold() {
		return false;
	}

	public boolean isDatalog() {
		return flattener instanceof ETDatalogFlattener;
	}
	
	private AtomUnfoldingResult getNormalUnfolding(Atom atom, Collection<? extends Term> boundTerms, Atom head, FlattenerExtra extra) {
		
		ProperRenamedAtom pra = new ProperRenamedAtom(atom, boundTerms);
		Atom searchAtom = pra.getRenamedAtom();
		
		AtomUnfolding unf = map.get(searchAtom);
		
		if (unf == null) {
			unf = computeFullUnfolding(searchAtom, boundTerms, head, extra);
			map.put(searchAtom, unf);
		}
		
		return new AtomUnfoldingResult(unf, pra.getSubstitution());
	}

	
	private AtomUnfolding computeFullUnfolding(Atom searchAtom, Collection<? extends Term> boundTerms, Atom head, FlattenerExtra extra) {
		AtomUnfolding unf;
		
		VUnfoldStructure vus = cmap.get(searchAtom);
		if (vus == null) {
			vus = new VUnfoldStructure(!isDatalog());

			Map<Atom, VUnfoldStructure> tmpMap = new TreeMap<Atom, VUnfoldStructure>();
			tmpMap.put(searchAtom, vus);
			
			if (vus.unfold(program, searchAtom, newUsedBoundVarsCounter, newUsedUnboundVarsCounter, tmpMap, this)) {
				while (vus.iunfold(program, newUsedBoundVarsCounter, newUsedUnboundVarsCounter, tmpMap, this));
			}

			if (isDatalog() || isUnfold()) {
				for (Map.Entry<Atom, VUnfoldStructure> entry : tmpMap.entrySet()) {
					cmap.put(entry.getKey(), entry.getValue());
				}
			}
			
			unf = flattener.createAtomUnfolding(vus, tmpMap, createHead(head, boundTerms), extra);

		} else {
			if (isDatalog()) {
				unf = new DLAtomUnfolding(new Atom[0], vus.getFunctionMaps());
			} else {
				Map<Atom, VUnfoldStructure> tmpMap = new TreeMap<Atom, VUnfoldStructure>();
				tmpMap.put(searchAtom, vus);
				vus.traverse(tmpMap);
				
				unf = flattener.createAtomUnfolding(vus, tmpMap, createHead(head, boundTerms), extra);
			}
		}
			
		return unf;
	}
	
	public AtomUnfoldingResult getUnfolding(Atom atom, Collection<? extends Term> boundTerms, Atom head, FlattenerExtra extra) {
		return getNormalUnfolding(atom, boundTerms, head, extra);
	}
	
	public Atom[] datalogComputeUnfolding(Atom atom, Collection<? extends Term> boundTerms, Atom head, FlattenerExtra extra) {
		if (isDatalog()) {
			getUnfolding(atom, boundTerms, head, extra);
			
			return new Atom[0];
			
		} else {
			return ((DLAtomUnfolding)getUnfolding(atom, boundTerms, head, extra).getUnfolding()).getUnfoldings();
		}
	}
	
	public AtomUnfoldingResult getUnfolding(Set<Atom> atom, Collection<? extends Term> boundTerms, Atom head, FlattenerExtra extra) {
		if (atom.size() == 1) {
			return getUnfolding(atom.iterator().next(), boundTerms, head, extra);
		} else {
			throw new RuntimeException("Method not implemented");
		}
	}


	private Atom createHead(Atom head, Collection<? extends Term> boundTerms) {
		Atom sHead = null;
		if (head != null && !head.getPredicate().equals(queryPredicate)) {
			sHead = new ProperRenamedAtom(head, boundTerms).getRenamedAtom();
		}
		
		return sHead;
	}
	
}
