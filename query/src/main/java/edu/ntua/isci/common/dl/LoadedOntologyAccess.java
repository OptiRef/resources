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

package edu.ntua.isci.common.dl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.QNameShortFormProvider;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.semanticweb.owlapi.util.AnnotationValueShortFormProvider;
import org.semanticweb.owlapi.util.OWLOntologySingletonSetProvider;

import edu.ntua.isci.common.lp.Atom;
import edu.ntua.isci.common.lp.Clause;
import edu.ntua.isci.common.lp.Predicate;

public class LoadedOntologyAccess implements BidirectionalShortFormProvider {

	public static String PRESENTATION_TYPE = "TYPE";
	public static String PRESENTATION_ANNOTATION = "ANNOTATION";
	
	public static int FULL_FORM = 0;
	public static int SIMPLE_SHORT_FORM = 1;
	public static int QNAME_FORM = 2;
	public static int ANNOTATION_FORM = 3;
	
	public static Map<String, Object> FULL_FORM_PROPERTIES = new HashMap<>();
	public static Map<String, Object> SIMPLE_SHORT_FORM_PROPERTIES = new HashMap<>();

	static {
		FULL_FORM_PROPERTIES.put(LoadedOntologyAccess.PRESENTATION_TYPE, LoadedOntologyAccess.FULL_FORM);
		SIMPLE_SHORT_FORM_PROPERTIES.put(LoadedOntologyAccess.PRESENTATION_TYPE, LoadedOntologyAccess.SIMPLE_SHORT_FORM);
	}

	
	private Map<String, Set<OWLEntity>> stringToEntityMap;
	private Map<OWLEntity, String> entityToStringMap;
	
	public LoadedOntologyAccess(LoadedOntology lo, Map<String, Object> props) {
		stringToEntityMap = new HashMap<>();
		entityToStringMap = new HashMap<>();

		OWLOntologyManager manager = lo.getOntologyManager();
		
		int type = (int)props.get(PRESENTATION_TYPE);
		
		ShortFormProvider sfp = null;  
		for (OWLOntology ont : manager.getOntologies()) {
			if (type == QNAME_FORM) {
				sfp = new QNameShortFormProvider(manager.getOntologyFormat(ont).asPrefixOWLOntologyFormat().getPrefixName2PrefixMap());
			} else if (type == SIMPLE_SHORT_FORM) {
				sfp = new SimpleShortFormProvider();
			} else if (type == ANNOTATION_FORM) {
				OWLAnnotationProperty ap = (OWLAnnotationProperty)props.get(PRESENTATION_ANNOTATION);
				String language = "en";
				
				ArrayList<OWLAnnotationProperty> al = new ArrayList<>();
				al.add(ap);
				
				ArrayList<String> lang = new ArrayList<>();
				lang.add(language);
				
				Map<OWLAnnotationProperty, List<String>> lm = new HashMap<>();
				lm.put(ap, lang);
				
				sfp = new AnnotationValueShortFormProvider(al, lm, new OWLOntologySingletonSetProvider(ont));
			} 

			for (OWLClass e : ont.getClassesInSignature()) {
				processEntity(sfp, e);
			}
	
			for (OWLObjectProperty e : ont.getObjectPropertiesInSignature()) {
				processEntity(sfp, e);
			}
			
			for (OWLDataProperty e : ont.getDataPropertiesInSignature()) {
				processEntity(sfp, e);
			}
			
			for (OWLNamedIndividual e : ont.getIndividualsInSignature()) {
				processEntity(sfp, e);
			}

			processEntity(sfp, manager.getOWLDataFactory().getOWLNothing());
			processEntity(sfp, manager.getOWLDataFactory().getOWLThing());
		}
	}
	
	public Comparator<OWLEntity> getComparator() {
		return new Comparator<OWLEntity>() {

			@Override
			public int compare(OWLEntity arg0, OWLEntity arg1) {
				return getShortForm(arg0).compareTo(getShortForm(arg1));
			}
			
		};
	}
	
	private void processEntity(ShortFormProvider sfp, OWLEntity e) {
		String s = handleIRI(sfp, e);
		Set<OWLEntity> set = stringToEntityMap.get(s);
		if (set == null) {
			set = new HashSet<>();
			stringToEntityMap.put(s, set);
		}
		set.add(e);
		entityToStringMap.put(e, s);
	}
	
	public String getShortForm(OWLEntity e) {
		return entityToStringMap.get(e);
	}

	public Set<OWLEntity> getEntities(String s) {
		Set<OWLEntity> res = stringToEntityMap.get(s);
		if (res == null) {
			res = new HashSet<>();
		}
		
		return res;
	}

	public OWLEntity getEntity(String s) {
		Set<OWLEntity> set = stringToEntityMap.get(s);
		if (set == null) {
			return null;
		} else {
			return set.iterator().next();
		}
	}

	public Set<String> getShortForms() {
		return stringToEntityMap.keySet();
	}

	private String handleIRI(ShortFormProvider sfp, OWLEntity obj) {
		if (sfp == null) {
			return obj.toString();
		} else {
			return sfp.getShortForm(obj);
		}
	}

	public void dispose() {
		// TODO Auto-generated method stub
	}

	public Clause expand(Clause c) {
		ArrayList<Atom> newBody = new ArrayList<Atom>();
		for (Atom a : c.getBody()) {
			Predicate p  = a.getPredicate();
			Predicate np = p; 
			
			OWLEntity ent = getEntity(p.getName());
			if (ent != null) {
				np = new Predicate(ent.toString(), p.getArity());
			}
			
			newBody.add(new Atom(np, a.getArguments()));
		}

		Atom head = c.getHead();
		
		Predicate p  = head.getPredicate();
		Predicate np = p; 
		
		OWLEntity ent = getEntity(p.getName());
		if (ent != null) {
			np = new Predicate(ent.getIRI().toString(), p.getArity());
		}
		
		return new Clause(new Atom(np, head.getArguments()), newBody);
	}

	public static Clause expand(Clause c, LoadedOntology[] loa) {
//		LoadedOntologyAccess[] f_loa = new LoadedOntologyAccess[loa.length];  
		LoadedOntologyAccess[] q_loa = new LoadedOntologyAccess[loa.length];
		LoadedOntologyAccess[] s_loa = new LoadedOntologyAccess[loa.length];
		
		ArrayList<Atom> newBody = new ArrayList<Atom>();
		for (Atom a : c.getBody()) {
			Predicate p  = a.getPredicate();
			Predicate np = p; 
			
			String name = p.getName();
			if (name.startsWith("<") && name.endsWith(">")) {
				
			} else if (name.indexOf(":") != -1) {
				Map<String, Object> props = new HashMap<>();
				props.put(PRESENTATION_TYPE, LoadedOntologyAccess.QNAME_FORM);
				for (int i = 0; i < loa.length; i++) {
					if (q_loa[i] == null) {
						q_loa[i] = new LoadedOntologyAccess(loa[i], props);
					}
					OWLEntity ent = q_loa[i].getEntity(name);
					if (ent != null) {
						np = new Predicate(ent.toString(), p.getArity());
						break;
					}
				}
			} else {
				Map<String, Object> props = new HashMap<>();
				props.put(PRESENTATION_TYPE, LoadedOntologyAccess.SIMPLE_SHORT_FORM);
				for (int i = 0; i < loa.length; i++) {
					if (s_loa[i] == null) {
						s_loa[i] = new LoadedOntologyAccess(loa[i], props);
					}
					OWLEntity ent = s_loa[i].getEntity(name);
					if (ent != null) {
						np = new Predicate(ent.toString(), p.getArity());
						break;
					}
				}
			}
			
			newBody.add(new Atom(np, a.getArguments()));
		}

		Atom head = c.getHead();
		
		Predicate p  = head.getPredicate();
		Predicate np = p; 
		
		String name = p.getName();
		if (name.startsWith("<") && name.endsWith(">")) {
			
		} else if (name.indexOf(":") != -1) {
			Map<String, Object> props = new HashMap<>();
			props.put(PRESENTATION_TYPE, LoadedOntologyAccess.QNAME_FORM);
			for (int i = 0; i < loa.length; i++) {
				if (q_loa[i] == null) {
					q_loa[i] = new LoadedOntologyAccess(loa[i], props);
				}
				OWLEntity ent = q_loa[i].getEntity(name);
				if (ent != null) {
					np = new Predicate(ent.toString(), p.getArity());
					break;
				}
			}
		} else {
			Map<String, Object> props = new HashMap<>();
			props.put(PRESENTATION_TYPE, LoadedOntologyAccess.SIMPLE_SHORT_FORM);
			for (int i = 0; i < loa.length; i++) {
				if (s_loa[i] == null) {
					s_loa[i] = new LoadedOntologyAccess(loa[i], props);
				}
				OWLEntity ent = s_loa[i].getEntity(name);
				if (ent != null) {
					np = new Predicate(ent.toString(), p.getArity());
					break;
				}
			}
		}
		
		return new Clause(new Atom(np, head.getArguments()), newBody);
	}


}
