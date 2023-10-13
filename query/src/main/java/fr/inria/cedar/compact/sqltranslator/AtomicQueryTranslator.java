package fr.inria.cedar.compact.sqltranslator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import fr.lirmm.graphik.graal.api.core.Atom;
import fr.lirmm.graphik.graal.api.core.Term;

public class AtomicQueryTranslator {

	private Map<Term,Set<Integer>> positions;
	private Map<Integer,Term> constants;
	public final Atom atom;
	public final List<Term> terms;
	public final String alias;
	
	public AtomicQueryTranslator(Atom a,List<Term> ans){
		this.positions = new TreeMap<>();
		this.constants = new TreeMap<>();
		this.atom = a;
		this.terms = ans;
		this.alias = SemiConjunctiveQueryTranslator.getTableName(a);
		initializeMaps();
	}
	
	public AtomicQueryTranslator(Atom a,List<Term> ans, String alias){
		this.positions = new TreeMap<>();
		this.constants = new TreeMap<>();
		this.atom = a;
		this.terms = ans;
		this.alias = alias;
		initializeMaps();
	}

	private void initializeMaps(){
		for (int i=0;i<this.atom.getPredicate().getArity();i++){
			Term termI = this.atom.getTerm(i);
			if (termI.isConstant()){
				this.constants.put(i, termI);
			}
			else{
				if (this.positions.containsKey(termI)){
					this.positions.get(termI).add(i);
				}
				else{
					Set<Integer> posTermI = new HashSet<>();
					posTermI.add(i);
					this.positions.put(termI,posTermI);
				}
			}
		}
	}


	
	public String generateSelectClause(){

		StringBuilder sb = new StringBuilder();
		sb.append(UnionSemiConjunctiveQueryTranslator.SELECT);
		for (Term t:this.terms){
			sb.append(SemiConjunctiveQueryTranslator.getAttributeName(this.atom, t, this.alias));
			sb.append(UnionSemiConjunctiveQueryTranslator.COMMA);
		}
		//TODO: add deletion
		sb.delete(sb.length()-2, sb.length()-1);
		return sb.toString();
	}
	
	public String generateFromClause(){
		StringBuilder sb = new StringBuilder();
		sb.append(UnionSemiConjunctiveQueryTranslator.FROM);
		sb.append(SemiConjunctiveQueryTranslator.getTableName(this.atom));
		sb.append(" ");
		sb.append(UnionSemiConjunctiveQueryTranslator.AS);
		sb.append(this.alias);
		return sb.toString();
	}
	
	public String generateWhereClause(){
		StringBuilder sb = new StringBuilder();
		//retrieve equality constraint
		List<Pair<Integer,Integer>> equalityConstraints = new ArrayList<>();
		for (Set<Integer> indices:this.positions.values()){
			if (indices.size()>1){
				Iterator<Integer> it = indices.iterator();
				Integer i = it.next();
				while (it.hasNext()){
					equalityConstraints.add(new ImmutablePair<>(i,it.next()));
				}
			}
		}
		boolean whereAdded = false;
		if (!equalityConstraints.isEmpty()||!this.constants.keySet().isEmpty()){
			sb.append(UnionSemiConjunctiveQueryTranslator.WHERE);
			whereAdded = true;
		}
		for (Pair<Integer,Integer> equality:equalityConstraints){
			sb.append(this.alias
					+ UnionSemiConjunctiveQueryTranslator.PERIOD 
					+ equality.getLeft());
			sb.append(UnionSemiConjunctiveQueryTranslator.EQUALS);
			sb.append(this.alias 
					+ UnionSemiConjunctiveQueryTranslator.PERIOD 
					+ equality.getRight());
			sb.append(" ");
			sb.append(UnionSemiConjunctiveQueryTranslator.AND);
		}

		for (Integer i:this.constants.keySet()){
			sb.append(this.alias +
					UnionSemiConjunctiveQueryTranslator.PERIOD +
					UnionSemiConjunctiveQueryTranslator.COLUMNNAME +  i);
			sb.append(UnionSemiConjunctiveQueryTranslator.EQUALS);
			sb.append(UnionSemiConjunctiveQueryTranslator.SIMPLEQUOTE
					+ this.constants.get(i) 
					+ UnionSemiConjunctiveQueryTranslator.SIMPLEQUOTE);
			sb.append(" ");
			sb.append(UnionSemiConjunctiveQueryTranslator.AND);
		}
		if (whereAdded){
			sb.delete(sb.length()-5,sb.length()-1);
		}
		return sb.toString();
	}


	public String generateSQLQuery(){
		StringBuilder sb = new StringBuilder();
		sb.append(generateSelectClause());
		sb.append(generateFromClause());
		sb.append(generateWhereClause());
		return sb.toString();
	}
}
