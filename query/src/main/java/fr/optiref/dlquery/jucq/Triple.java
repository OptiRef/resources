package fr.optiref.dlquery.jucq;

public class Triple extends Atomic{

	private String s, p, o;
	public Triple(String _s, String _p, String _o) {
		this.s = _s;
		this.p = _p;
		this.o = _o;

	}
	private String clean(String ob) {
		if(ob.contains(":")) {
			ob = ob.split(":")[1];
		}
		return ob;
	}
	public Triple(String t) {
		String[] triple;

		if(t.contains(",")) {
			this.p = t.split("\\(")[0];
			this.s = t.split("\\(")[1].split(",")[0];
			this.o = t.split(",")[1].replace(")","");
		}else {
			this.p = "type";
			this.s = t.split("\\(")[1].replace(")","");
			this.o = t.split("\\(")[0];
		}


	}


	public Triple copy() {
		return new Triple(this.s, this.p, this.o);
	}
	public String getS() {
		return s;
	}
	public void setS(String s) {
		this.s = s;
	}
	public String getP() {
		return p;
	}
	public void setP(String p) {
		this.p = p;
	}
	public String getO() {
		return o;
	}
	public void setO(String o) {
		this.o = o;
	}

	boolean joinOnS(Triple t) {

		return this.s.equals(t.getS());
	}

	boolean joinOnO(Triple t) {
		return this.o.equals(t.getO());
	}
	public boolean join(Atomic a) {
		Triple t = (Triple) a;
		return joinOnS(t) || joinOnO(t);
	}
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;

		Triple other = (Triple) obj;
		if (!o.equals(other.getO()))
			return false;
		if (!p.equals(other.getP()))
			return false;
		if (!s.equals(other.getS()))
			return false;
		return true;
	}
	public int hashCode() {
		// TODO Auto-generated method stub
		final int prime = 31;
	    int result = 1;
	    result = prime * result + s.hashCode();
	    result = prime * result + p.hashCode();
	    result = prime * result + o.hashCode();
	    return result;
	}
	@Override
	public String toString() {
		return String.format("<%s %s %s>", this.s, this.p, this.o);
	}

	public String toDLP() {
		if(p.equals("type")) {
			return String.format("%s(%s)", o,s);
		}else {
			return String.format("%s(%s,%s)", p,s,o);
		}

	}

	@Override
	public boolean isConcept() {
		// TODO Auto-generated method stub
		return this.p.equals("type");
	}


}
