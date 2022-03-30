package fr.inria.cedar.compact.sqltranslator;

import fr.inria.cedar.compact.query.SemiConjunctiveQuery;
import fr.inria.cedar.compact.query.UnionSemiConjunctiveQuery;

public class UnionSemiConjunctiveQueryTranslator {

	public static final String VIEW_CREATION = "CREATE TEMPORARY VIEW ";
	public static final String VIEW_DELETION = "DROP VIEW ";
	public static final String AS = "AS ";
	public static final String SELECT = "SELECT ";
	public static final String FROM = "FROM ";
	public static final String AND = "AND ";
	public static final String EQUALS = " = ";
	public static final String DOUBLEQUOTE = "\"";
	public static final String SIMPLEQUOTE = "'";
	public static final String UNION = "UNION ";
	public static final String WHERE = "WHERE ";
	public static final String SEMICOLON = ";";
	public static final String COMMA = ", ";
	public static final String PERIOD = ".";
	public static final String COLUMNNAME = "c";
	public static final String UNDERSCORE = "_";

	UnionSemiConjunctiveQuery uscq;
	public UnionSemiConjunctiveQueryTranslator(UnionSemiConjunctiveQuery q){
		this.uscq = q;
	}
	
	
	public String generateSQL(final String viewName){
		final StringBuilder sbView = new StringBuilder();
		final StringBuilder sbQuery = new StringBuilder();		
		int disjIndex = -1;
		for (final SemiConjunctiveQuery scq:this.uscq.getSCQs()){
			disjIndex++;
			final SemiConjunctiveQueryTranslator scqt = new SemiConjunctiveQueryTranslator(scq);
			final String[] subQueries = scqt.generatesSQL(viewName,disjIndex);
			sbView.append(subQueries[0]);
			sbQuery.append(subQueries[1]);
			sbQuery.delete(sbQuery.length()-1, sbQuery.length());
			sbQuery.append(" " + UnionSemiConjunctiveQueryTranslator.UNION);
		}
		sbQuery.delete(sbQuery.length()-7, sbQuery.length());
		sbQuery.append(UnionSemiConjunctiveQueryTranslator.SEMICOLON);
		return sbView.toString() + sbQuery.toString();
	}

}
