package fr.lri.lahdak.cqapri.db;

import java.util.ArrayList;
import java.util.List;

import edu.ntua.isci.common.lp.Clause;



public class ClauseQueue {

	private List<Clause> queries;
	private boolean finish;

	public ClauseQueue() {
		queries = new ArrayList<Clause>();

		finish = false;
	}

	public synchronized void addQuery(Clause SQLQuery) {
		queries.add(SQLQuery);
		notify();
	}

	public synchronized Clause getQuery() {

		if (!finish || !queries.isEmpty()) {
			if (!queries.isEmpty()) {
				return queries.remove(0);
			} else {
				try {
					wait();
				} catch(InterruptedException e) {
					e.printStackTrace();
				}

				if (!queries.isEmpty()) {
					return queries.remove(0);
				}
			}
		} 

		return null;
	}

	public synchronized void finish() {
		finish = true;
		notify();
	}

}
