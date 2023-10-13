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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.UnknownOWLOntologyException;


import edu.ntua.isci.common.lp.Atom;
import edu.ntua.isci.common.lp.Clause;
import edu.ntua.isci.common.lp.Predicate;
import edu.ntua.isci.common.lp.Variable;

public class LoadedOntology {

	private OWLOntology ontology;
	private OWLOntologyManager manager;
	private String ontologyString;
	
	private IRI iri;

	public static LoadedOntology createFromString(String string) throws Exception {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new StringDocumentSource(string));
		
		return new LoadedOntology(manager, ontology, string);
	}

	public static LoadedOntology createFromPath(String path) throws Exception {
		return createFromIRI(IRI.create(path));
	}
	
	public static LoadedOntology createFromIRI(IRI iri) throws Exception {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(iri);

		StringBuilder result = new StringBuilder();
		try (InputStream in = iri.toURI().toURL().openStream();
		    BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			String line;
			while((line = reader.readLine()) != null) {
			    result.append(line);
			}
		}
		
		LoadedOntology lo = new LoadedOntology(manager, ontology, result.toString());
		lo.iri = iri;
		
		return lo;
	}

	public LoadedOntology(OWLOntologyManager manager, OWLOntology ontology) throws Exception {
		this.manager = manager;
		this.ontology = ontology;
		this.ontologyString = null;
	}
	
	public LoadedOntology(OWLOntologyManager manager, OWLOntology ontology, String ontologyString) throws Exception {
		this.manager = manager;
		this.ontology = ontology;
		this.ontologyString = ontologyString;
	}
	
	public String serialize(Map<String, Object> properties) throws Exception {
		if (ontologyString == null) {
			ontologyString = toString(new RDFXMLOntologyFormat());
		}
		return ontologyString;
	}
	
	public String getOntologyString() {
		return ontologyString;
	}
	
	public IRI getIRI() {
		return iri;
	}
	
	public Set<OWLOntology> getOntologies() {
		return manager.getOntologies();
	}
	
	
	public OWLOntology getMainOntology() {
		return ontology;
	}
	

	public OWLOntologyManager getOntologyManager() {
		return manager;
	}

	public String toString(OWLOntologyFormat format) throws UnknownOWLOntologyException, OWLOntologyStorageException {
		StringDocumentTarget string = new StringDocumentTarget();

		manager.saveOntology(ontology, format, string);
	
		return string.toString();
	}

	public ArrayList<Clause> getAtomClauses() {
		ArrayList<Clause> res = new ArrayList<>();
		
		for (OWLClass c : ontology.getClassesInSignature()) {
			if (!c.isOWLThing()) {
				res.add(new Clause(new Atom(new Predicate(Predicate.QUERY_PREDICATE_NAME, 1), new Variable("x")), new Atom(new Predicate(c.toString(), 1), new Variable("x"))));
			}
		}
		
		for (OWLObjectProperty c : ontology.getObjectPropertiesInSignature()) {
			res.add(new Clause(new Atom(new Predicate(Predicate.QUERY_PREDICATE_NAME, 2), new Variable("x"), new Variable("y")), new Atom(new Predicate(c.toString(), 2), new Variable("x"), new Variable("y"))));
			res.add(new Clause(new Atom(new Predicate(Predicate.QUERY_PREDICATE_NAME, 1), new Variable("x")), new Atom(new Predicate(c.toString(), 2), new Variable("x"), new Variable("y"))));
			res.add(new Clause(new Atom(new Predicate(Predicate.QUERY_PREDICATE_NAME, 1), new Variable("x")), new Atom(new Predicate(c.toString(), 2), new Variable("y"), new Variable("x"))));
		}
		
		return res;
	}
}
