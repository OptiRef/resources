package fr.lri.lahdak.cqapri.db;

public class QueryResult {

	private String[] result;

	public QueryResult(int arity) {
		result = new String[arity];
	}

	public void set(int i, String v) {
		result[i] = v;
	}

	public int hashCode() {
		int hc = 0;
		for (int i = 0; i < result.length; i++) {
			hc += result[i].hashCode();
		}
		return hc;
	}

	public boolean equals(Object obj) {
		if (result.length != ((QueryResult)obj).result.length) {
			return false;
		}

		for (int i = 0; i < result.length; i++) {
			if (!(result[i].equals(((QueryResult)obj).result[i]))) {
				return false;
			}
		}

		return true;
	}

	public String toString() {
		String s =  "(";
		for (int i = 0; i < result.length; i++) {
			if (i > 0) {
				s += ", ";
			}
			s += result[i];
		}
		s += ")";

		return s;
	}

	public String[] getContents() {
		return result;
	}
}
