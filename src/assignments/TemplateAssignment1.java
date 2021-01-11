package assignments;

import java.awt.Color;
import java.io.FileReader;
import java.io.IOException;
import umontreal.ssj.charts.EmpiricalChart;
import umontreal.ssj.charts.EmpiricalSeriesCollection;
import umontreal.ssj.probdist.EmpiricalDist;
import umontreal.ssj.stat.Tally;
import java.io.PrintStream;

/**
 * @author mctenthij
 * Edited by qvanderkaaij and jberkhout
 */
public class TemplateAssignment1 {
	
	// LCG parameters (notation from slides used)
	double seed = 0;
	double m = 1048576;	//2^20
	double a = 233;
	double c = 65431;

	int raceTo = 5; // number of games to win the game
	double winThreshold = 0.5; // winning probability of a player

	LCG prng;
	EmpiricalDist durationDist;

	PrintStream out;

	TemplateAssignment1() {
		out = new PrintStream(System.out);
	}

	/* DO NOT CHANGE THE CODE IN QUESTION1 AND QUESTION2 BELOW */
	
	public double[] Question1(double givenSeed, int numOutputs, boolean normalize) {
		prng = new LCG(givenSeed,a,c,m);
		double[] result = new double[numOutputs];
		for (int i = 0; i < numOutputs; i++) {
			result[i] = prng.generateNext(normalize);
		}
		return result;
	}

	public EmpiricalDist Question2(String csvFile) {
		EmpiricalDist myDist = getDurationDist(csvFile);
		return myDist;
	}

	public void plotEmpiricalCDF() {
		// Use EmpiricalChart to plot the CDF
		EmpiricalChart chart = new EmpiricalChart("The ECDF of the game length", "x values of a game length", "ECDF value F(x)", durationDist.getParams());
		chart.view(1000, 500);
	}

	public Tally Question3() {
		Tally durations = new Tally();
		// Simulate the matches and add the duration to the Tally.
		int nrOfSimulations = 5000;
		for (int i = 0; i < nrOfSimulations; i++) {
			durations.add(simulateMatch(raceTo));
		}
		
		/* 
		
		About the difference between Tally and TallyStore:
		In addition to Tally, TallyStore also stores all the individual observations. Tally only keeps track of statistics such as the mean and the standard deviation and only (efficiently) updates those when a new individual observation is recorded, without storing this individual observation. This saves memory and computation time as no list of observations has to be maintained. On the other hand, this rules out the possibility to study individual observations afterwards, for example to check for outliers.
		
		For this assignment, Tally is sufficient. See the SSJ documentation for more details.

		*/

		return durations;
	}

	public double simulateMatch(int raceTo) {
		//simulate match and duration of match
		double matchDuration = 0;
		
		int player1 = 0;
		int player2 = 0;
		
		while (player1 < raceTo && player2 < raceTo) {
			double winChance = prng.generateNext(true);
			matchDuration += durationDist.inverseF(winChance);
			if (winChance < winThreshold) {
				player1 ++;
			} else {
				player2 ++;
			}
		}
		return matchDuration;
	}

	public EmpiricalDist getDurationDist(String csvFile) {
		//import csv file, sort the values and compute empirical distribution
		try {
			FileReader file = new FileReader(csvFile);
			//file is already sorted in Excel
			durationDist = new EmpiricalDist(file);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return durationDist;
	}

	/*  ONLY CHANGE generateNext in the LCG class */
	public class LCG {
		public double seed;
		public final double m;
		public final double a;
		public final double c;

		public double lastOutput;

		public LCG(double seed, double a,double c,double m){
			this.seed = seed;
			this.m = m;
			this.a = a;
			this.c = c;

			this.lastOutput = seed;
		}

		public double generateNext(boolean normalize){
			// implement the pseudo-code algorithm here. Your code should be able to return both normalized and regular numbers based on the value of normalize.
			double result = (this.a * this.lastOutput + this.c) % this.m;
			this.lastOutput = result;
			
			return normalize ? (result + 1) / (this.m + 1) : result;
			
		}

		public void setSeed(double newSeed) {
			this.seed = newSeed;
		}
	}

	public void start() {
		// This is your test function. During grading we will execute the function that are called here directly.
		double givenSeed = seed;
		int numOutputs = 3;
		
		// Run Question 1: once regularly and once normalized 
		double[] outputRegRNG = Question1(givenSeed, numOutputs, false);
		double[] outputNormRNG = Question1(givenSeed, numOutputs, true);
		for (int i = 0; i < numOutputs; i++) {
			out.println("Regular:" + outputRegRNG[i]);
			out.println("Normalized:" + outputNormRNG[i]);
		}
		
		// Name of CSV file is read and passed on to Question2 for loading 
		String csvFile = "game_lengths.csv";
		EmpiricalDist myDist = Question2(csvFile);
		
		// Quantiles are printed and the ECDF plotted
		out.println(myDist.inverseF(0.0));
		out.println(myDist.inverseF(0.25));
		out.println(myDist.inverseF(0.5));
		out.println(myDist.inverseF(0.75));
		out.println(myDist.inverseF(1.0));
		plotEmpiricalCDF();
		
		// Run Question 3
		Tally durations = Question3();
		out.println(durations.report());
	}

	public static void main(String[] args){
		new TemplateAssignment1().start();
	}
}