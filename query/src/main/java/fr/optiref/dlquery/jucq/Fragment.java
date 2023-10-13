package fr.optiref.dlquery.jucq;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Fragment{

	private Set<Atomic> atoms;
	private Set<String> headvars;
	private int fid;
	private static int fCounter = 0;
	/*public Fragment() {
		atoms = new HashSet<Atomic>();
		headvars = new ArrayList<String>();

	}*/

	public Fragment(Set<Atomic>  _atoms) {
		atoms = _atoms;
		fid = fCounter;
		fCounter++;
		atoms = new HashSet<Atomic>();
		for (Atomic atom : _atoms) {
			atoms.add(atom.copy());
		}
	}

	public Fragment(Set<Atomic>  _atoms, int _fid) {
		//atoms = _atoms;
		fid = _fid;
		atoms = new HashSet<Atomic>();
		for (Atomic atom : _atoms) {
			atoms.add(atom.copy());
		}
	}
	public void setHeadvars(Set<String> headvars) {
		this.headvars = headvars;
	}
	public Set<String> getHeadvars() {
		return headvars;
	}
	public Fragment copy() {
		return new Fragment(this.atoms, fid);
	}

	public int size() {
		return this.atoms.size();
	}

	public void add(Atomic t) {
		this.atoms.add(t);
	}

	public void addAll(Collection<Atomic> c) {
		this.atoms.addAll(c);
	}

	public void remove(Atomic t) {
		this.atoms.remove(t);
	}

	public boolean connected(Atomic t) {

		for (Atomic atom : atoms) {
			if(t.join(atom)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasatom(Atomic t) {
		for (Atomic atom : atoms) {
			if(t.equals(atom)) {
				return true;
			}
		}
		return false;
	}
	public Set<Atomic> getatoms() {
		return atoms;
	}
	public boolean include(Fragment other) {
		int counter = 0;
		for (Atomic atomic : other.getatoms()) {
			if(hasatom(atomic)) {
				counter++;
			}
		}
		return counter == other.size();
	}
	public int getFid() {
		return fid;
	}
	/*public boolean include(Fragment other) {

		Set<Atomic> _atoms = other.getatoms();
		_atoms.retainAll(atoms);
		return _atoms.size()>0;
	}*/
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Fragment other = (Fragment) obj;
		/*Set<Atomic> oatoms = other.getatoms();
		if(oatoms.size() != atoms.size())
			return false;
		for (Atomic atom : oatoms) {
			if(!hasatom(atom)) {
				return false;
			}
		}*/

		return other.getFid() == this.fid;

	}
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		final int prime = 31;
	    int result = 1;
	    for (Atomic atomic : atoms) {
	    	result = prime * result + atomic.hashCode();
		}

	    return result;
	}

	@Override
	public String toString() {
		return  String.join("; ", this.atoms.toString());
	}
	public float getCost() {
		float cost = 0;

		return cost;
	}

	public List<String>  freeVariables() {
		List<String> headVars = new ArrayList<String>();
		for(Atomic a:atoms) {
			if(a.getClass() == Triple.class) {
				Triple triple = (Triple)a;
				if(triple.getP().equals("type")) {
					if(!headVars.contains(triple.getS()) && triple.getS().contains("?") )  {
						headVars.add(triple.getS());
					}

				}else {
					if(!headVars.contains(triple.getS())  && triple.getS().contains("?")){
						headVars.add(triple.getS());
					}
					if(!headVars.contains(triple.getO())  && triple.getO().contains("?")){
						headVars.add(triple.getO());
					}
				}

			}
		}

		return headVars;
	}


	public String bodyToDLP() {
		List<String> dlpatoms = new ArrayList<String>();
		for (Atomic atom : atoms) {
			dlpatoms.add(atom.toDLP());
		}
		return String.join(", ", dlpatoms);
	}


}
