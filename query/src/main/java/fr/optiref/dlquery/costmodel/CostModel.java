package fr.optiref.dlquery.costmodel;


import fr.optiref.dlquery.Connector;

public class CostModel {

	private Connector connec;
	private String query;
	private String[] views;
	private double maxCost;
	private double baseCost;

	public CostModel(Connector _connec, String _query) {
		this.connec = _connec;
		this.query = _query;
		//this.views = query.split("\n,\n-- ##############\n");
		this.baseCost = 0;
		initMap();

	}

	private void initMap() {
		int c = 0, maxC = 0;
		String view;
		String curCQ;
//		for (int i=0; i<views.length-1; i++) {
//			view = views[i];
//			c = 0;
			for (String cq : this.query.split("UNION")) {
				curCQ = cq;
				//System.out.println("#############################");
				//System.out.println(curCQ);
				if (curCQ.contains("as(")) {
					curCQ = curCQ.substring(curCQ.indexOf('(')+1, curCQ.length());  //split("as\\(")[1];
				}
				if (curCQ.contains(")")) {
					curCQ = curCQ.replace(")", "");
				}
				if(curCQ.contains("FROM")){
					curCQ = curCQ.split(" FROM ")[1];
				}
				if(curCQ.contains("WHERE")){
					curCQ = curCQ.split(" WHERE ")[0];
				}
				//System.out.println(curCQ);
				for (String atom : curCQ.split(",")) {
					atom = atom.split(" AS ")[0];
					c += connec.getRows(atom);
				}
			}
			if (c > maxC) {
				maxC = c;
				maxCost = c;
			}
			this.baseCost += c;
		//}

	}

	public double getCost() {
		return connec.getCdb() + Ceval() + Cunique() + Cjoin() + Cmat();
	}

	/*private int countCQAtoms(String cq) {
		int c = 0;
		String curCQ;
		curCQ = cq;
		if(curCQ.contains("FROM")){
			curCQ = curCQ.split(" FROM ")[1];
		}
		if(curCQ.contains("WHERE")){
			curCQ = curCQ.split(" WHERE ")[0];
		}
		for (String atom : curCQ.split(",")) {
			c += atomsMap.get(atom.split(" AS ")[0]);
		}
		return c;
	}

	private int countViewAtoms(String view) {
		int c = 0;
		for (String cq : view.split("UNION")) {
			if (cq.contains("as(")) {
				cq = cq.split("as(")[1];
			}
			if (cq.contains("a//)")) {
				cq = cq.replace(")", "");
			}
			c += countCQAtoms(cq);
		}
		return c;
	}
	*/

	private double Cunique() {
		return connec.getCl() * baseCost;
	}
	private double Ceval() {
		return (connec.getCj() + connec.getCt()) * baseCost;
	}

	private double Cjoin() {
		return connec.getCj() * baseCost;
	}

	private double Cmat() {
		return connec.getCm() * (baseCost -maxCost);
	}
}
