package fr.inria.cedar.compact.sqltranslator;

import java.util.List;

import fr.inria.cedar.compact.query.CompactDisjunction;
import fr.lirmm.graphik.graal.api.core.Atom;
import fr.lirmm.graphik.graal.api.core.Term;
import fr.lirmm.graphik.util.stream.CloseableIterator;

public class DisjunctionTranslator {

	CompactDisjunction disj;
	List<Term> ansVar;
	String name;

	public DisjunctionTranslator(CompactDisjunction d,List<Term> selectedVars,String viewName){
		this.disj = d;
		this.ansVar = selectedVars;
		this.name = viewName;
	}


	public String generateCreateView(){
		StringBuilder sb = new StringBuilder();
		sb.append(UnionSemiConjunctiveQueryTranslator.VIEW_CREATION 
				+ this.name + " (");
		int columnIndex = -1;
		for (int i=0;i<this.ansVar.size();i++){
			columnIndex++;
			sb.append(UnionSemiConjunctiveQueryTranslator.COLUMNNAME 
					+ columnIndex 
					+ UnionSemiConjunctiveQueryTranslator.COMMA);
		}
		sb.delete(sb.length()-2, sb.length());
		sb.append(") ");
		sb.append(UnionSemiConjunctiveQueryTranslator.AS);
		return sb.toString();
	}


	public String generatesSQL(){
		StringBuilder sb = new StringBuilder();
		sb.append(generateCreateView());
		CloseableIterator<Atom> it = this.disj.getAtoms().iterator();
		try{
			while (it.hasNext()){
				Atom a = it.next();
				if (!a.getPredicate().toString().contains("aux_")){
					sb.append(new AtomicQueryTranslator(a,this.ansVar).generateSQLQuery());
					sb.append(" ");
					sb.append(UnionSemiConjunctiveQueryTranslator.UNION);
				}
			}
		}
		catch (Exception e){
			e.printStackTrace();
		}
		if (sb.length() > 8){
			sb.delete(sb.length()-6, sb.length());
		}
		sb.append(UnionSemiConjunctiveQueryTranslator.SEMICOLON);
		return sb.toString();
	}
}
