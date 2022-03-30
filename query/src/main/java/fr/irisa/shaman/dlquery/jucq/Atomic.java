package fr.irisa.shaman.dlquery.jucq;


public abstract class Atomic  {

	public abstract boolean  join(Atomic t);
	public abstract Atomic  copy();
	public abstract String  toDLP();
	public abstract boolean isConcept();
}
