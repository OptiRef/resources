package fr.inria.cedar.compact.sqltranslator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;


import fr.inria.cedar.compact.query.CompactDisjunction;
import fr.inria.cedar.compact.query.SemiConjunctiveQuery;
import fr.lirmm.graphik.graal.api.core.Atom;
import fr.lirmm.graphik.graal.api.core.Term;

public class SemiConjunctiveQueryTranslator {

	SemiConjunctiveQuery scq;
	ArrayList<CompactDisjunction> disjs;
	//ArrayList<Atom> atoms;
	ArrayList<Term> ansAndSharedVars;


	public static String getTableName(Atom a){
		StringBuilder sb = new StringBuilder();
		String pred = a.getPredicate().toString();
		sb.append(UnionSemiConjunctiveQueryTranslator.DOUBLEQUOTE 
				+ pred 
				+ UnionSemiConjunctiveQueryTranslator.DOUBLEQUOTE);
		sb.delete(sb.length()-3, sb.length()-1);
		return sb.toString();
	}

	/**
	 * 
	 * @param a an atom
	 * @param t a term of a
	 * @param alias the alias of the table containing a
	 * @return the string a.i, where is a position where the term t can be found 
	 */

	public static String getAttributeName(Atom a,Term t, final String alias){
		StringBuilder sb = new StringBuilder();
		sb.append(alias);
		sb.append(UnionSemiConjunctiveQueryTranslator.PERIOD);
		for (int i=0;i<a.getPredicate().getArity();i++){
			if (a.getTerm(i).equals(t)){
				sb.append(UnionSemiConjunctiveQueryTranslator.COLUMNNAME);
				sb.append(i);
				return sb.toString();
			}
		}
		//FIXME: an exception should be thrown
		System.err.println("The term should be present");
		return null;
	}

	public SemiConjunctiveQueryTranslator(SemiConjunctiveQuery s){
		this.scq = s;
		this.disjs = new ArrayList<>();
		for (CompactDisjunction disj:s.getDisjunctions()){
			this.disjs.add(disj);
		}
		//this.atoms = new ArrayList<>();
		Set<Term> ansSet = new TreeSet<>();
		ansSet.addAll(s.getSharedTerms());
		ansSet.addAll(s.getAnswerVariables());
		this.ansAndSharedVars = new ArrayList<>(ansSet);
	}


	//FIXME: does not work with Boolean queries
	/**
	 * 
	 * @param term2Attribute maps (at least) each answer variable to a set of positions that contain it 
	 * @return a corresponding select clause
	 */
	public String generateSelectClause(Map<Term, Set<String>> term2Attribute){
		StringBuilder sb = new StringBuilder();
		sb.append(UnionSemiConjunctiveQueryTranslator.SELECT);
		for (Term t:this.scq.getAnswerVariables()){
			if (!term2Attribute.containsKey(t)){
				System.err.println("the answer variable should come from somewhere");
			}
			else{
				Set<String> positions = term2Attribute.get(t);
				String pos = positions.iterator().next();
				sb.append(pos + UnionSemiConjunctiveQueryTranslator.COMMA);
			}
		}
		sb.delete(sb.length()-2, sb.length()-1);
		return sb.toString();
	}

	/**
	 * 
	 * @param tables: a set of string being either view names or aliases definitions 
	 * @return the corresponding from clause
	 */
	public static String generateFromClause(Set<String> tables){
		StringBuilder sb = new StringBuilder();
		sb.append(UnionSemiConjunctiveQueryTranslator.FROM);
		for (String s:tables){
			sb.append(s);
			sb.append(UnionSemiConjunctiveQueryTranslator.COMMA);
		}
		sb.delete(sb.length()-2, sb.length());
		sb.append(" ");
		return sb.toString();
	}


	/**
	 * 
	 * @param term2Attribute: a map which associated to each term of the query a of positions whose value should be made equal in the where clause
	 * @return the corresponding where clause
	 */
	public static String generateWhereClause(Map<Term, Set<String>> term2Attribute){
		StringBuilder sb = new StringBuilder();
		boolean whereAdded = false;
		for (Term t:term2Attribute.keySet()){
			Set<String> tPos = term2Attribute.get(t);
			if (tPos.size()>1){
				if (whereAdded == false){
					sb.append(UnionSemiConjunctiveQueryTranslator.WHERE);
					whereAdded = true;
				}
				Iterator<String> it = tPos.iterator();
				String firstPos = it.next();
				while (it.hasNext()){
					sb.append(firstPos 
							+ UnionSemiConjunctiveQueryTranslator.EQUALS 
							+ it.next());
					sb.append(" ");
					sb.append(UnionSemiConjunctiveQueryTranslator.AND);
				}
			}
		}
		if (whereAdded){
			sb.delete(sb.length()-5,sb.length());
		}
		return sb.toString();
	}


	public String[] generatesSQL(String view, Integer disjIndex){
		StringBuilder sbView = new StringBuilder();
		StringBuilder sbQuery = new StringBuilder();
		int disjCount = -1;
		Map<Term, Set<String>> term2Attribute = new TreeMap<>();
		Set<String> tables = new TreeSet<>();
		StringBuilder insideConditions = new StringBuilder();
		for (CompactDisjunction disj:this.disjs){
			disjCount++;
			String fromPartViewOrTableName = "";
			String viewOrTableName = "";
			//find positions where joins are necessary;
			//FIXME: work only if all positions of the root atoms are kept in the views
			ArrayList<Term> shared = new ArrayList<>();
			Atom a = disj.getRoot();
			//get the set of join positions
			shared = new ArrayList<Term>(this.ansAndSharedVars);
			shared.retainAll(disj.getTerms());
			for (Term t:shared){
				if (!term2Attribute.containsKey(t)){
					term2Attribute.put(t, new TreeSet<>());
				}
			}


			// if only one atom in the disjunction, do not create a view but create an alias for that atom
			if (disj.isAtomicDisjunction()){
				String attributeTableName = SemiConjunctiveQueryTranslator.getTableName(a);
				viewOrTableName = attributeTableName.substring(0,attributeTableName.length()-1) + "_" + disjCount + "\"";
				fromPartViewOrTableName = attributeTableName + " " + UnionSemiConjunctiveQueryTranslator.AS + " " + viewOrTableName;
				AtomicQueryTranslator asqlt = new  AtomicQueryTranslator(a, this.ansAndSharedVars,viewOrTableName);
				String localCondition = asqlt.generateWhereClause();
				if (!localCondition.isEmpty()){
					insideConditions.append(localCondition.substring(6));
					insideConditions.append(" " + UnionSemiConjunctiveQueryTranslator.AND);
				}
			}
			// if several atoms, create a view
			//TODO: should be common table expressions instead of views
			else{
				fromPartViewOrTableName = view 
						+ UnionSemiConjunctiveQueryTranslator.UNDERSCORE 
						+ disjIndex 
						+ UnionSemiConjunctiveQueryTranslator.UNDERSCORE 
						+ disjCount;
				viewOrTableName = fromPartViewOrTableName;
				DisjunctionTranslator dt = new DisjunctionTranslator(disj,shared,fromPartViewOrTableName);
				sbView.append(dt.generatesSQL());
				sbView.append("\n\n");
			}
			for (Term t:shared){
				term2Attribute.get(t).add(viewOrTableName 
						+ UnionSemiConjunctiveQueryTranslator.PERIOD 
						+ UnionSemiConjunctiveQueryTranslator.COLUMNNAME + a.indexOf(t));
			}
			tables.add(fromPartViewOrTableName);
		}

		sbQuery.append(generateSelectClause(term2Attribute));
		sbQuery.append(generateFromClause(tables));
		String whereClause = generateWhereClause(term2Attribute);
		String localCond = insideConditions.toString();
		//System.out.println("Where clause is " + whereClause);
		//System.out.println("localCond is" + localCond);
		if (whereClause.isEmpty()){
			if (!localCond.isEmpty()){
				sbQuery.append(" " + UnionSemiConjunctiveQueryTranslator.WHERE + " " + localCond.substring(0,localCond.length()-6));
			}
		}
		else{
			sbQuery.append(whereClause);
			if (!localCond.isEmpty()){
				sbQuery.append(" " + UnionSemiConjunctiveQueryTranslator.AND + " " + localCond.substring(0,localCond.length()-6));
			}
		}
		sbQuery.append(UnionSemiConjunctiveQueryTranslator.SEMICOLON);


		String[] result = new String[2];
		result[0] = sbView.toString();
		result[1] = sbQuery.toString();

		return result;
	}
}
