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

package edu.ntua.isci.qa.owl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectInverseOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import edu.ntua.isci.common.dl.LoadedOntology;
import edu.ntua.isci.common.dl.LoadedOntologyAccess;
import edu.ntua.isci.common.lp.Atom;
import edu.ntua.isci.common.lp.Clause;
import edu.ntua.isci.common.lp.Function;
import edu.ntua.isci.common.lp.FunctionalTerm;
import edu.ntua.isci.common.lp.Predicate;
import edu.ntua.isci.common.lp.Variable;

public class OWL2LogicTheory {

	public static int DL_LITE = 1;
	public static int ELHI = 2;
	public static int HORN_SHIQ = 3;
	
	private static String AUX_PREDICATE = "YAUXY";
	
	private static String V1 = "x";
	private static String V2 = "y";
	private static String V3 = "z";
	
	private int fcount;
	private int auxcount;
	private int eqcount;
	private ArrayList<Clause> rules;
	
	private ArrayList<OWLAxiom> ignoredAxioms;
	private ArrayList<OWLAxiom> usedAxioms;
	private ArrayList<OWLAxiom> actualAxioms;

	private OWLDataFactory factory;
	
	private LoadedOntology ontRef;
	private LoadedOntologyAccess loa;
	
	private int level;
	private boolean cgllr;
	
	private Set<String> usedEntities;
	private Set<String> auxNames;
	private Set<String> fauxNames;
	
	private Map<OWLClassExpression, OWLClass> auxMap;

	private String OWL_THING;
	
	public OWL2LogicTheory(LoadedOntology ontRef) {
		this.ontRef = ontRef;	
	}

	public boolean isAUXAtom(Atom a) {
		return auxNames.contains(a.getPredicate().getName());
	}

	private String getNewAUXPredicateName() {
		String aux = AUX_PREDICATE + auxcount++;
		
		while (usedEntities.contains(aux) || auxNames.contains(aux)) {
			aux = AUX_PREDICATE + auxcount++;
		}

		auxNames.add(aux);

		return aux;
		
	}
	
	private ArrayList<OWLAxiom> axiomQueue;
	private Set<OWLPropertyExpression<?, ?>> transitiveRoles;
	
	private String randomID;
	
	public ArrayList<Clause> clausify(LoadedOntologyAccess loa, boolean cgllr, int level) {
		this.loa = loa;
		this.level = level;
		this.cgllr = cgllr;
		if (cgllr) {
			this.level = DL_LITE;
		}
		
		randomID = dummyPrefix + "R" + (int)Math.floor(10000000*Math.random()) + "/";	
		
		fcount = 0;
		auxcount = 0;
		eqcount = 0;
		
		auxMap = new HashMap<>();
		
		rules = new ArrayList<Clause>();
		
		factory = new OWLDataFactoryImpl();
		OWL_THING = createPredicateName(factory.getOWLThing());

		ignoredAxioms = new ArrayList<>();
		usedAxioms = new ArrayList<>();
		
		actualAxioms = new ArrayList<>();
		
		auxNames = new HashSet<>();
		fauxNames = new HashSet<>();

		transitiveRoles = new HashSet<>();
		
		usedEntities = new HashSet<>();
		
		for (OWLOntology ont : ontRef.getOntologies()) {
			
			for (OWLEntity ent : ont.getSignature()) {
				usedEntities.add(createPredicateName(ent));
			}
			
			if (level == ELHI || level == HORN_SHIQ) {
				for (OWLTransitiveObjectPropertyAxiom ax :  ont.getAxioms(AxiomType.TRANSITIVE_OBJECT_PROPERTY)) {
					transitiveRoles.add(ax.getProperty());
					transitiveRoles.add(ax.getProperty().getInverseProperty());
				}
			}
	//		System.out.println(transitiveRoles);
			
			axiomQueue = new ArrayList<OWLAxiom>();
			for (OWLAxiom ax : ont.getLogicalAxioms()) {
	//			if (ax.toString().contains("http://org.semanticweb.owlapi/error")) {
	//				continue;
	//			}
	
				axiomQueue.add(ax);
			}
		}
		
		int i = 0;
		for (i = 0; i < axiomQueue.size(); i++) {
			processStr(axiomQueue.get(i));
		}
		
		System.out.println("USED AUX " + auxNames.size() + " : " + auxNames);

		removeUnusedAUX(rules);
		
		return rules;
	}

	private static void removeUnusedAUX(ArrayList<Clause> clauses) {
		while (true) {
			Set<String> headAUXPredicates = new HashSet<>();
			Set<String> bodyAUXPredicates = new HashSet<>();
			
			Set<String> headOnlyAUXPredicates = new HashSet<>();
			Set<String> bodyOnlyAUXPredicates = new HashSet<>();
			
			for (Clause r : clauses) {
				for (Atom a : r.getBody()) {
					int name = -1;
					try {
						name = Integer.parseInt(a.getPredicate().getName());
					} catch (Exception e) {} 
			
					if (a.getPredicate().getName().contains(AUX_PREDICATE) || name != -1) {
						bodyAUXPredicates.add(a.getPredicate().getName());
					}
				}

				Atom a = r.getHead();
				
				int name = -1;
				try {
					name = Integer.parseInt(a.getPredicate().getName());
				} catch (Exception e) {} 
		
				if (a.getPredicate().getName().contains(AUX_PREDICATE) || name != -1) {
					headAUXPredicates.add(a.getPredicate().getName());
				}
			}
			
			bodyOnlyAUXPredicates.addAll(bodyAUXPredicates);
			bodyOnlyAUXPredicates.removeAll(headAUXPredicates);

			headOnlyAUXPredicates.addAll(headAUXPredicates);
			headOnlyAUXPredicates.removeAll(bodyAUXPredicates);
			
			boolean remove = false;
			for (Iterator<Clause> it = clauses.iterator(); it.hasNext();) {
				Clause nc = it.next();
				
				if (headOnlyAUXPredicates.contains(nc.getHead().getPredicate().getName())) {
					it.remove();
					continue;
				}
				
				for (Atom a : nc.getBody()) {
					if (bodyOnlyAUXPredicates.contains(a.getPredicate().getName())) {
						it.remove();
						remove = true;
						break;
					}
				}
			}
			
			if (!remove) {
				break;
			}
		}		

	}

	public Set<String> getAUXNames() {
		return auxNames;
	}
	
	private static String dummyPrefix = "http://image.ntua.gr/owl2logictheory/";
	
	private OWLClass createNewClass(String s) {
		return factory.getOWLClass(IRI.create((randomID + s)));
	}

	private OWLObjectProperty createNewObjectProperty(String s) {
		return factory.getOWLObjectProperty(IRI.create((randomID + s)));
	}

	private String createPredicateName(OWLEntity ent) {
		String s = ent.toString();
		if (s.startsWith("<" + randomID)) {
			return s.substring(randomID.length() + 2,s.length() - 1);
		} else {
			return loa.getShortForm(ent);
		}
	}
	
	private Atom createConceptAtom(OWLClass c, String v) {
		return new Atom(new Predicate(createPredicateName(c), 1), new Variable(v));
	}

	private Set<Atom> createFunctionalRoleAtoms(OWLObjectSomeValuesFrom ex, String v) {
		Set<Atom> atoms = new HashSet<Atom>();
		
		OWLObjectPropertyExpression p = ex.getProperty();
		OWLClassExpression f = ex.getFiller();
		
		if (cgllr) {
			if (f.isOWLThing()) {
				if (p instanceof OWLObjectProperty) {
					atoms.add(new Atom(new Predicate(createPredicateName((OWLObjectProperty)p), 2), new Variable(v), new FunctionalTerm(new Function("f_" + fcount, 1), new Variable(v))));
				} else if (p instanceof OWLObjectInverseOf) {
					atoms.add(new Atom(new Predicate(createPredicateName((OWLObjectProperty)((OWLObjectInverseOf)p).getInverse()), 2), new FunctionalTerm(new Function("f_" + fcount, 1), new Variable(v)), new Variable(v)));
				}
			} else if (f instanceof OWLClass) {
				OWLObjectProperty newProp = createNewObjectProperty(getNewAUXPredicateName());

				atoms.add(new Atom(new Predicate(createPredicateName(newProp), 2), new Variable(v), new FunctionalTerm(new Function("f_" + fcount, 1), new Variable(v))));

				Atom h1 = new Atom(new Predicate(createPredicateName((OWLClass)f),1), new Variable(v));
				Atom b1 = new Atom(new Predicate(createPredicateName(newProp),2), new Variable(V2), new Variable(v));
				
				Atom h2 = null;
				if (p instanceof OWLObjectProperty) {
					h2 = new Atom(new Predicate(createPredicateName((OWLObjectProperty)p), 2), new Variable(V2), new Variable(v)); 
				} else if (p instanceof OWLObjectInverseOf) {
					h2 = new Atom(new Predicate(createPredicateName((OWLObjectProperty)((OWLObjectInverseOf)p).getInverse()), 2), new Variable(v), new Variable(V2));
				}

				rules.add(new Clause(h1, b1));
				rules.add(new Clause(h2, b1));
			}
			
			fcount++;
			
		} else {
			if (p instanceof OWLObjectProperty) {
				atoms.add(new Atom(new Predicate(createPredicateName((OWLObjectProperty)p), 2), new Variable(v), new FunctionalTerm(new Function("f_" + fcount, 1), new Variable(v))));
			} else if (p instanceof OWLObjectInverseOf) {
				atoms.add(new Atom(new Predicate(createPredicateName((OWLObjectProperty)((OWLObjectInverseOf)p).getInverse()), 2), new FunctionalTerm(new Function("f_" + fcount, 1), new Variable(v)), new Variable(v)));
			}
			
			if (!f.isOWLThing() && f instanceof OWLClass) {
				Predicate pred = new Predicate(createPredicateName((OWLClass)f), 1);
				atoms.add(new Atom(pred, new FunctionalTerm(new Function("f_" + fcount, 1), new Variable(v))));
				if (pred.getName().contains("AUX")) {
					fauxNames.add(pred.getName());
				}
			}
			
			fcount++;
		}
		
		return atoms;
	}
	
	private Atom createHeadRoleAtom(OWLPropertyExpression<?, ?> p, String v1, String v2) {
		Atom atom = null;
		
		if (p instanceof OWLObjectProperty) {
			atom = new Atom(new Predicate(createPredicateName((OWLObjectProperty)p), 2), new Variable(v1), new Variable(v2));
		} else if (p instanceof OWLObjectInverseOf) {
			atom = new Atom(new Predicate(createPredicateName((OWLObjectProperty)((OWLObjectInverseOf)p).getInverse()), 2), new Variable(v2), new Variable(v1));
		}
		
		return atom;
	}
	
	private OWLClass createTransitive(OWLPropertyExpression<?, ?> p) {
		Predicate tmp = new Predicate(getNewAUXPredicateName(), 1);
		
		ArrayList<Atom> rbody = new ArrayList<Atom>();

		if (p instanceof OWLObjectProperty) {
			rbody.add(new Atom(new Predicate(createPredicateName((OWLObjectProperty)p), 2), new Variable(V1), new Variable(V2)));
		} else if (p instanceof OWLObjectInverseOf) {
			rbody.add(new Atom(new Predicate(createPredicateName((OWLObjectProperty)((OWLObjectInverseOf)p).getInverse()), 2), new Variable(V2), new Variable(V1)));
		}
		
		rbody.add(new Atom(tmp, new Variable(V2)));
		
		rules.add(new Clause(new Atom(tmp, new Variable(V1)), rbody));
		
		return createNewClass(tmp.getName());
	}

	private Atom createBodyRoleAtom(OWLPropertyExpression<?, ?> p, String v1, String v2) {

		if (p instanceof OWLObjectProperty) {
			return new Atom(new Predicate(createPredicateName((OWLObjectProperty)p), 2), new Variable(v1), new Variable(v2));
		} else if (p instanceof OWLObjectInverseOf) {
			return new Atom(new Predicate(createPredicateName((OWLObjectProperty)((OWLObjectInverseOf)p).getInverse()), 2), new Variable(v2), new Variable(v1));
		}
		
		return null;
	}

	public void add(OWLAxiom ax) {
//		System.out.println(">>> " + ax);
		usedAxioms.add(ax);
	}

	public void add(Clause c) {

		if (!c.getHead().getPredicate().getName().equals(OWL_THING)) {
			rules.add(c);
		}
	}
	
	public ArrayList<OWLAxiom> getIgnoredAxioms() {
		return ignoredAxioms;
	}

	public OWLOntology getUsedOntology(OWLOntologyManager manager) throws Exception {
		
		OWLOntology ontology = manager.createOntology();
		
//		FileWriter fw = new FileWriter("c:/tmp/ttt_axioms.txt");
		
		for (OWLAxiom ax : actualAxioms) {
//			fw.write(" >> " + ax + "\n");			
			manager.addAxiom(ontology, ax);
		}

//		fw.close();
		for (OWLOntology ont : ontRef.getOntologies()) {
			for (OWLDataProperty dp : ont.getDataPropertiesInSignature()) {
				manager.addAxiom(ontology, factory.getOWLDeclarationAxiom(dp));
			}
		}
		
		return ontology;
	}
	
	private void addToQueue(OWLAxiom ax) {
		axiomQueue.add(ax);
	}
	
	private boolean processStr(OWLAxiom descr) {

//		if (!(descr instanceof OWLDeclarationAxiom)) {
//			System.out.println(" : " + descr);
//		}
			
		if (descr instanceof OWLEquivalentClassesAxiom) {
			for (OWLSubClassOfAxiom ax : ((OWLEquivalentClassesAxiom)descr).asOWLSubClassOfAxioms()) {
				addToQueue(ax);
			}
			return true;
			
		} else if (descr instanceof OWLSubClassOfAxiom) {
			OWLClassExpression subClass = ((OWLSubClassOfAxiom)descr).getSubClass();
			OWLClassExpression supClass = ((OWLSubClassOfAxiom)descr).getSuperClass();

			if (supClass.isOWLThing() || subClass.isOWLNothing()) {
				return true;
			} else if (supClass instanceof OWLObjectIntersectionOf) {
				for (OWLClassExpression ex : ((OWLObjectIntersectionOf)supClass).getOperands()) {
					addToQueue(factory.getOWLSubClassOfAxiom(subClass, ex));
				}
				return true;
			} else if (supClass instanceof OWLObjectAllValuesFrom) {
				OWLObjectAllValuesFrom cDescr = (OWLObjectAllValuesFrom)supClass;
				OWLObjectPropertyExpression supProp = cDescr.getProperty().getInverseProperty();
				OWLClassExpression filler = cDescr.getFiller();

				if (!filler.isOWLThing()) {
					addToQueue(factory.getOWLSubClassOfAxiom(factory.getOWLObjectSomeValuesFrom(supProp, subClass), filler));
				}
				return true;
			} else if (supClass instanceof OWLObjectMinCardinality){
				OWLObjectMinCardinality minCard = (OWLObjectMinCardinality)supClass;
				OWLObjectPropertyExpression minProp = minCard.getProperty();
				OWLClassExpression filler = minCard.getFiller();
				
				if (minCard.getCardinality() == 1) {
					addToQueue(factory.getOWLSubClassOfAxiom(subClass, factory.getOWLObjectSomeValuesFrom(minProp, filler)));
				} else {
					for (int i = 0; i < minCard.getCardinality(); i++) {
						OWLClass newClass = createNewClass(getNewAUXPredicateName()); 
						addToQueue(factory.getOWLSubClassOfAxiom(subClass, factory.getOWLObjectSomeValuesFrom(minProp, newClass)));
						addToQueue(factory.getOWLSubClassOfAxiom(newClass, filler));
					}
				}
				return true;
			}
			
			ArrayList<Atom> body = new ArrayList<Atom>();
			OWLClassExpression actualSubClass;

			if (subClass instanceof OWLClass) {
				body.add(createConceptAtom((OWLClass)subClass, V1));
				actualSubClass = subClass;
				
			} else if (subClass instanceof OWLObjectSomeValuesFrom) {
				OWLObjectSomeValuesFrom cDescr = (OWLObjectSomeValuesFrom)subClass;
				OWLObjectPropertyExpression subProp = cDescr.getProperty();
				OWLClassExpression filler = cDescr.getFiller();
				
				if (level == DL_LITE && !filler.isOWLThing()) {
					ignoredAxioms.add(descr);
					return false;
				}
				
				body.add(createBodyRoleAtom(subProp, V1, V2));

				if (!filler.isOWLThing()) {
					if (transitiveRoles.contains(subProp)) {
						OWLClass transClass = createTransitive(subProp);
						
						body.add(new Atom(new Predicate(createPredicateName(transClass), 1), new Variable(V2)));
						actualSubClass = factory.getOWLObjectSomeValuesFrom(subProp, transClass);

						addToQueue(factory.getOWLSubClassOfAxiom(getNewSupClassFor(filler), transClass));
						
					} else {
						Set<OWLClassExpression> newClasses = new HashSet<OWLClassExpression>();
						newClasses.add(filler);

						OWLClassExpression cex = handleClassIntersection(newClasses, body, V2); 
						if (cex == null) {
							ignoredAxioms.add(descr);
							return false;
						}  else {
							actualSubClass = factory.getOWLObjectSomeValuesFrom(subProp, cex);
						}
					}
				} else {
					actualSubClass = cDescr;
				}
				
			} else if (subClass instanceof OWLObjectUnionOf) {
				for (OWLClassExpression ex : ((OWLObjectUnionOf)subClass).getOperands()) {
					addToQueue(factory.getOWLSubClassOfAxiom(ex, supClass));
				}
				return true;
				
			} else if (subClass instanceof OWLObjectIntersectionOf) {
				if (level == DL_LITE) {
					ignoredAxioms.add(descr);
					return false;
				}
				OWLClassExpression cex = handleClassIntersection(((OWLObjectIntersectionOf)subClass).getOperands(), body, V1); 
				if (cex == null) {
					ignoredAxioms.add(descr);
					return false;
				} else {
					actualSubClass = cex;
				}
				
			} else if (subClass instanceof OWLObjectMinCardinality) {
				OWLObjectMinCardinality minCard = (OWLObjectMinCardinality)subClass;
				if (minCard.getCardinality() == 1) {
					addToQueue(factory.getOWLSubClassOfAxiom(factory.getOWLObjectSomeValuesFrom(minCard.getProperty(), minCard.getFiller()), supClass));
					return true;
				}
				return false;
			} else {
				ignoredAxioms.add(descr);
				return false;
			}
			
			Set<Atom> head = new HashSet<Atom>();
			OWLClassExpression actualSupClass;
			
			if (supClass instanceof OWLClass) {
				head.add(createConceptAtom((OWLClass)supClass, V1));
				actualSupClass = supClass;
			} else if (supClass instanceof OWLObjectSomeValuesFrom) {
				OWLObjectSomeValuesFrom cDescr = (OWLObjectSomeValuesFrom)supClass;
				OWLObjectPropertyExpression supProp = ((OWLObjectSomeValuesFrom)supClass).getProperty();
				OWLClassExpression filler = cDescr.getFiller();

				if (filler.isOWLThing() || filler instanceof OWLClass) {
					head.addAll(createFunctionalRoleAtoms((OWLObjectSomeValuesFrom)supClass, V1));
					actualSupClass = supClass;
				} else {
					OWLClass newClass = createNewClass(getNewAUXPredicateName());
					OWLObjectSomeValuesFrom tmp = factory.getOWLObjectSomeValuesFrom(supProp, newClass);

					head.addAll(createFunctionalRoleAtoms(tmp, V1));
					
					actualSupClass = tmp;
					
					addToQueue(factory.getOWLSubClassOfAxiom(newClass, filler));
					
				}
			} else if (supClass instanceof OWLObjectMaxCardinality) {
				OWLObjectMaxCardinality cDescr = (OWLObjectMaxCardinality)supClass;
				
				if (cDescr.getCardinality() == 1 && level == HORN_SHIQ) {
					OWLObjectPropertyExpression supProp = cDescr.getProperty();
					OWLClassExpression filler = cDescr.getFiller();
					
					head.add(createBodyRoleAtom(createNewObjectProperty(Predicate.EQUAL_PREDICATE_NAME + (eqcount++)), V2, V3));

					body.add(createBodyRoleAtom(supProp, V1, V2));
					body.add(createBodyRoleAtom(supProp, V1, V3));

					if (filler.isOWLThing()) {
						actualSupClass = supClass;
					} else if (filler instanceof OWLClass) {
						body.add(createConceptAtom((OWLClass)filler, V2));	
						body.add(createConceptAtom((OWLClass)filler, V3));
						actualSupClass = supClass;
					} else {
						
						OWLClass newClass = createNewClass(getNewAUXPredicateName());
						OWLObjectMaxCardinality tmp = factory.getOWLObjectMaxCardinality(1, supProp, newClass);

						body.add(createConceptAtom((OWLClass)newClass, V2));	
						body.add(createConceptAtom((OWLClass)newClass, V3));

						actualSupClass = tmp;
						
						addToQueue(factory.getOWLSubClassOfAxiom(filler, newClass));
					}
				} else {
					ignoredAxioms.add(descr);
					return false;
				}
			} else {
				ignoredAxioms.add(descr);
				return false;
			}

			add(descr);

			if (actualSupClass instanceof OWLObjectSomeValuesFrom && actualSubClass instanceof OWLObjectSomeValuesFrom) {
				addToQueue(factory.getOWLSubClassOfAxiom(getNewSupClassFor(actualSubClass), actualSupClass));
					
				return true;
			}
			
			for (Atom a : head) {
				add(new Clause(a, body));
			}
			
			actualAxioms.add(factory.getOWLSubClassOfAxiom(actualSubClass, actualSupClass));
			
			return true;
			
		} else if (descr instanceof OWLEquivalentObjectPropertiesAxiom) {
			for (OWLSubObjectPropertyOfAxiom ax : ((OWLEquivalentObjectPropertiesAxiom)descr).asSubObjectPropertyOfAxioms()) {
				addToQueue(ax);
			}
			return true;
			
		} else if (descr instanceof OWLSubObjectPropertyOfAxiom) {
			OWLPropertyExpression<?, ?> subProp = ((OWLSubObjectPropertyOfAxiom)descr).getSubProperty();
			OWLPropertyExpression<?, ?> supProp = ((OWLSubObjectPropertyOfAxiom)descr).getSuperProperty();
		
			add(descr);
			add(new Clause(createHeadRoleAtom(supProp, V1, V2), createBodyRoleAtom(subProp, V1, V2)));				
			
			actualAxioms.add(descr);
			
			return true;
			
		} else if (descr instanceof OWLObjectPropertyDomainAxiom) {
			OWLObjectPropertyExpression prop = ((OWLObjectPropertyDomainAxiom)descr).getProperty();
			OWLClassExpression domain = ((OWLObjectPropertyDomainAxiom)descr).getDomain();
			
			addToQueue(factory.getOWLSubClassOfAxiom(factory.getOWLObjectSomeValuesFrom(prop, factory.getOWLThing()), domain));
			return true;
			
		} else if (descr instanceof OWLObjectPropertyRangeAxiom) {
			OWLObjectPropertyExpression prop = ((OWLObjectPropertyRangeAxiom)descr).getProperty();
			OWLClassExpression range = ((OWLObjectPropertyRangeAxiom)descr).getRange();

			addToQueue(factory.getOWLSubClassOfAxiom(factory.getOWLObjectSomeValuesFrom(prop.getInverseProperty(), factory.getOWLThing()), range));
			return true;
			
		} else if (descr instanceof OWLInverseObjectPropertiesAxiom) {
			OWLObjectProperty first = ((OWLInverseObjectPropertiesAxiom)descr).getFirstProperty().asOWLObjectProperty();
			OWLObjectProperty second = ((OWLInverseObjectPropertiesAxiom)descr).getSecondProperty().asOWLObjectProperty();
			
			add(descr);
			add(new Clause(createHeadRoleAtom(first, V1, V2), createBodyRoleAtom(second.getInverseProperty(), V1, V2)));
			add(new Clause(createHeadRoleAtom(second, V1, V2), createBodyRoleAtom(first.getInverseProperty(), V1, V2)));
		
			actualAxioms.add(descr);
			
			return true;
		} else if (descr instanceof OWLSymmetricObjectPropertyAxiom) {
			add(descr);
			
			for (OWLSubObjectPropertyOfAxiom ax : ((OWLSymmetricObjectPropertyAxiom)descr).asSubPropertyAxioms()) {
				addToQueue(ax);
				break;
			}
			return true;
		} else if (descr instanceof OWLTransitiveObjectPropertyAxiom) {
			if (level == ELHI || level == HORN_SHIQ) {
				add(descr);
				return true;
			} else {
				ignoredAxioms.add(descr);
				return false;
			}
		} else if (!(descr instanceof OWLClassAssertionAxiom) && !(descr instanceof OWLObjectPropertyAssertionAxiom) &&
			       !(descr instanceof OWLAnnotationAssertionAxiom) && !(descr instanceof OWLDataPropertyAssertionAxiom) &&
			       !(descr instanceof OWLDeclarationAxiom)) {
			ignoredAxioms.add(descr);
			return false;
		} else {
			return false;
		}

	}

	
	private OWLClass getNewSupClassFor(OWLClassExpression c) {
		OWLClass newClass = auxMap.get(c);
		if (newClass == null) {
			newClass = createNewClass(getNewAUXPredicateName());
			addToQueue(factory.getOWLSubClassOfAxiom(c, newClass));
			auxMap.put(c, newClass);
		}
		
		return newClass;
	}

	private OWLClassExpression handleClassIntersection(Set<OWLClassExpression> classes, ArrayList<Atom> body, String vb) {
		Set<OWLClassExpression> iClasses = new HashSet<>();
		
		for (OWLClassExpression c : classes) {
			if (c instanceof OWLClass) {
				body.add(createConceptAtom((OWLClass)c, vb));
				iClasses.add(c);
			} else if (c instanceof OWLObjectSomeValuesFrom || c instanceof OWLObjectIntersectionOf) {
				OWLClass newClass = getNewSupClassFor(c);
				body.add(new Atom(new Predicate(createPredicateName(newClass), 1), new Variable(vb)));
				iClasses.add(newClass);
			} else if (c instanceof OWLObjectUnionOf) {
				OWLClass newClass = createNewClass(getNewAUXPredicateName());
				body.add(new Atom(new Predicate(createPredicateName(newClass), 1), new Variable(vb)));
				iClasses.add(newClass);
				for (OWLClassExpression ce : ((OWLObjectUnionOf)c).getOperands()) {
					addToQueue(factory.getOWLSubClassOfAxiom(ce, newClass));
				}
			} else if (c instanceof OWLObjectMinCardinality && ((OWLObjectMinCardinality)c).getCardinality()==1) {
				OWLClass newClass = getNewSupClassFor(c);
				body.add(new Atom(new Predicate(createPredicateName(newClass), 1), new Variable(vb)));
				iClasses.add(newClass);
			} else {
				return null;
			}
		}
					
		if (iClasses.size() == 1) {
			return iClasses.iterator().next();
		} else {
			return factory.getOWLObjectIntersectionOf(iClasses);
		}
	}
	
}
