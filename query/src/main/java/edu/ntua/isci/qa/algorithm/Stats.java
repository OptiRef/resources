/*Copyright 2011, 2013, 2015 Alexandros Chortaras

 This file is part of Rapid.

 Rapid is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Rapid is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Rapid.  If not, see <http://www.gnu.org/licenses/>.*/

package edu.ntua.isci.qa.algorithm;

import java.util.ArrayList;

public class Stats {
	public double qrewriteTime;
	public double qcheckTime;
	public double qtotalTime;
	public int qrewriteSize;
	public int qfinalSize;
	public double qmatchTime;
	public double qmatchSize;
	
	public int iter;
	
	public Stats() {
		qrewriteTime = 0;
		qcheckTime = 0;
		qtotalTime = 0;
		qrewriteSize= 0;
		qfinalSize = 0;
		qmatchTime = 0;
		qmatchSize = 0;
		
		iter = 0;
	}

	public void addIterationTimes(double rTime, double cTime, double mTime) {
		qrewriteTime += rTime;
		qcheckTime += cTime;
		qmatchTime += mTime;
		
		qtotalTime = qrewriteTime + qcheckTime;
		
		iter++;
	}
	
	public void finishIteration(int rSize, int fSize, double mSize) {
		qrewriteTime = qrewriteTime/(double)iter;
		qcheckTime = qcheckTime/(double)iter;
		qmatchTime = qmatchTime/(double)iter;

		qtotalTime = qrewriteTime + qcheckTime;
		
		qrewriteSize = rSize;
		qfinalSize = fSize;
		
		qmatchSize = mSize;
	}
	
	public static double[] stats(ArrayList<? extends Number> data) {
		double sum = 0;
	
		for (Number t : data) {
			sum += t.doubleValue();
		}
		
		double mean = sum/data.size();
		
		double sqDiff = 0;
		
		for (Number t : data) {
			sqDiff += (t.doubleValue() - mean)*(t.doubleValue() - mean);
		}
		
		double var = Math.sqrt(sqDiff/(data.size() - 1));
		
		
		return new double[] {mean, var};
		
	}
}
