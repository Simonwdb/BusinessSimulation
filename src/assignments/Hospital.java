package assignments;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.Random;

import umontreal.ssj.rng.MRG32k3a;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.simevents.Event;
import umontreal.ssj.simevents.Sim;
import umontreal.ssj.stat.StatProbe;
import umontreal.ssj.stat.Tally;
import umontreal.ssj.stat.list.ListOfStatProbes;

/**
 * Models the hospital and its regions from Assignment 4.
 *
 * @author mctenthij
 * Edited by qvanderkaaij and jberkhout
 */
public class Hospital {
	
	public static boolean DEBUG_MODE = false;

	// SB: can we place this here?
	final static double DIAMETER = 10;
	
	// hospital variables
	public static double RESPONSE_TIME_TARGET = 15.0; // in minutes
	int numAmbulances;
	Ambulance[] ambulances;
	int numRegions;
	Region[] regions;
	double[] arrivalRates; // for all regions
	double serviceRate; // at accidents
	int[] ambulancePlacements; // ambulance placement strategy
	double stopTime;

	// RNG for seeds
	Random rng = new Random(); // for replication purposes you could set a seed

	// stats counters
	Tally serviceTimeTally;
	Tally waitTimeTally;
	Tally withinTargetTally;
	ListOfStatProbes<StatProbe> listStatsTallies;

	public Hospital(int numAmbulances, double[] arrivalRates, double serviceRate, double stopTime, int numRegions, boolean serveOutsideBaseRegion, int[] ambulancePlacements) {

		// set a seed for replication purposes
		setSeed();
		// set hospital variables
		this.numAmbulances = numAmbulances;
		ambulances = new Ambulance[numAmbulances];
		this.numRegions = numRegions;
		regions = new Region[numRegions];
		this.arrivalRates = arrivalRates;
		this.serviceRate = serviceRate;
		this.stopTime = stopTime;
		this.ambulancePlacements = ambulancePlacements;

		// create regions
		createRegions();

		// create and assign ambulances to regions
		createAssignAmbulances(serveOutsideBaseRegion);

		// create Tallies
		waitTimeTally = new Tally("Waiting time");
		serviceTimeTally = new Tally("Service time");
		withinTargetTally = new Tally("Arrival within target");

		// add Tallies in ListOfStatProbes for later reporting
		listStatsTallies = new ListOfStatProbes<>("Stats for Tallies");
		listStatsTallies.add(waitTimeTally);
		listStatsTallies.add(serviceTimeTally);
		listStatsTallies.add(withinTargetTally);
	}

	private void setSeed() {
		// Set some seed for replication purposes
		rng.setSeed(0);
	}
	
    private void createRegions() {
		// Create the regions when constructing Hospital object
		for (int j = 0; j < numRegions; j++) {
			double[] baseLocation = determineRegionLocation(j);
			RandomStream arrivalRandomStream = getStream();
			RandomStream locationRandomStream = getStream();
			regions[j] = new Region(j, baseLocation, arrivalRandomStream, arrivalRates[j], locationRandomStream, regions, numRegions);
		}
	}
    
	private void createAssignAmbulances(boolean serveOutsideBaseRegion) {
		// create and assign ambulances to regions
		for (int i = 0; i < numAmbulances; i++) {
			int region = determineBaseRegion(i);
			RandomStream serviceRandomStream = getStream();
			Ambulance ambulance = new Ambulance(i, regions[region], serviceRandomStream, serviceRate, serveOutsideBaseRegion);
			ambulances[i] = ambulance;
			regions[region].idleAmbulances.add(ambulance); // initially the ambulance is idle
		}
	}

	// returns region index to which the ambulance should be assigned
    public int determineBaseRegion(int ambulanceNumber) {
        // this function must be adjusted

        // use ambulancePlacements to return the right base region index for
        // the ambulance with ambulanceNumber
    	
    	// SB: in the previous file from canvas, this function was already filled
    	// SB: below in multiple comment-line is the code from that previous file
    	/*
    	 *     public int determineBaseRegion(int ambulanceNumber) {
			        // This function can be altered to test different ambulance placements
			        return ambulanceNumber % numRegions;
			    }
    	 */

    	// SB: can we use the code from above?
    	
        return ambulanceNumber % this.numRegions;
    }

    // returns the location coordinates of the base of region j
    public double[] determineRegionLocation(int j) {
		// Determine only the center location region (case: j = 0)
        double[] location = new double[2];
        double K = DIAMETER / 2;
        double r = (K * Math.sqrt(3)) / 2;
        
        if (j == 0) {
        	location[0] = 0.0; 
        	location[1] = 0.0;
        } else if (j == 1) {
        	location[0] = 0.0;
        	location[1] = -2 * r;
        } else if (j == 2) {
        	location[0] = 1.5 * K;
        	location[1] = -1 * r;
        } else if (j == 3) {
        	location[0] = 1.5 * K;
        	location[1] = r;
        } else if (j == 4) {
        	location[0] = 0.0;
        	location[1] = 1.5 * K;
        } else if (j == 5) {
        	location[0] = -1.5 * K;
        	location[1] = r;
        } else if (j == 6) {
        	location[0] = -1.5 * K;
        	location[1] = -1 * r;
        } 
        else
        	location = null;
        
		return location;
    }

    // SB: redundant method
	private double[] naiveDetermineLocation(int j) {
		// Determine only the center location region (case: j = 0)
        double[] location = new double[2];
        double K = DIAMETER / 2;
        double r = (K * Math.sqrt(3)) / 2;
        
        // SB: can't come up with a smart for loop to assign the locations. TO: klopt, mag je hardcoden!
        if (j == 0) {
        	location[0] = 0.0; // X-Coordinate of centre location
        	location[1] = 0.0; // Y-Coordinate of centre location
        } else if (j == 1) {
        	location[0] = 0.0;
        	location[1] = -2 * r;
        } else if (j == 2) {
        	location[0] = 1.5 * K;
        	location[1] = -1 * r;
        } else if (j == 3) {
        	location[0] = 1.5 * K;
        	location[1] = r;
        } else if (j == 4) {
        	location[0] = 0.0;
        	location[1] = 1.5 * K;
        } else if (j == 5) {
        	location[0] = -1.5 * K;
        	location[1] = r;
        } else if (j == 6) {
        	location[0] = -1.5 * K;
        	location[1] = -1 * r;
        } 
        else
        	location = null;
        
		return location;
	}

	public ListOfStatProbes simulateOneRun() {

		Sim.init();

		// reset stats counters
		listStatsTallies.init();

		// set first events
		for (int j = 0; j < numRegions; j++) {
			regions[j].arrivalProcess.init(); // schedules first arrival region j
		}
		new StopEvent().schedule(stopTime); // schedule stopping time

		// start simulation
		System.out.println("START SIMULATION");
		Sim.start();
		System.out.println("END SIMULATION");
		// TODO: check: Wordt dit pas gedaan nadat de sim klaar is??
		// combine results in the Hospital tallies : aparte methode van maken
		for (int k = 0; k < numAmbulances; k++) {
			for (double obs: ambulances[k].serviceTimeTally.getArray()) {
				serviceTimeTally.add(obs);
			}
			for (double obs: ambulances[k].waitTimeTally.getArray()) {
				waitTimeTally.add(obs);
			}
			for (double obs: ambulances[k].withinTargetTally.getArray()) {
				withinTargetTally.add(obs);
			}
		}

		return listStatsTallies;
	}

	// generate a random stream based on a random seed
	public MRG32k3a getStream() {
		long[] seed = new long[6];
		for (int i =0;i<seed.length;i++) {
			seed[i] = (long) rng.nextInt();
		}
		MRG32k3a myrng = new MRG32k3a();
		myrng.setSeed(seed);
		return myrng;
	}

	// stop simulation by scheduling this event
	class StopEvent extends Event {
		@Override
		public void actions() {
			Sim.stop();
		}
	}

    public static void main(String[] args) {

        // hospital variables
		int numAmbulances = 20;
		double[] arrivalRates = {1./15, 1./15, 1./15, 1./15, 1./15, 1./15, 1./15}; // arrival rates per region
		double serviceRate = 1.0;
		double stopTime = 10000; // simulation endtime (minutes)
		boolean serveOutsideBaseRegion = false; // if true, ambulances serve outside their base regions, false otherwise
		/*

//		simulate ambulance placement 0: only central region
		int numRegions = 1;
//		miscchien aanpassen, 20 is vrij veel misschien
		int[] ambulancePlacements = {20, 0, 0, 0, 0, 0, 0}; // should be of the length numRegions and with a total sum of numAmbulances
		Hospital hospital = new Hospital(numAmbulances, arrivalRates, serviceRate, stopTime, numRegions, serveOutsideBaseRegion, ambulancePlacements);
		hospital.simulateOneRunAndReport();
		*/

		int numRegions = 7; // reset number of regions
		
		// simulate ambulance placement 1
		int[] ambulancePlacements1 = {1, 4, 2, 4, 1, 3, 5}; // should be of the length numRegions and with a total sum of numAmbulances
		Hospital hospital = new Hospital(numAmbulances, arrivalRates, serviceRate, stopTime, numRegions, serveOutsideBaseRegion, ambulancePlacements1);
		hospital.simulateOneRunAndReport();
		// simulate ambulance placement 2
//		int[] ambulancePlacements2 = {1, 3, 3, 4, 1, 4, 4}; // should be of the length numRegions and with a total sum of numAmbulances
//		hospital = new Hospital(numAmbulances, arrivalRates, serviceRate, stopTime, numRegions, serveOutsideBaseRegion, ambulancePlacements2);
//		hospital.simulateOneRunAndReport();

		// further optimization experiments can be done here
		double[] arrivalRates2 = {1./15, 1./15, 1./15, 1./15, 1./15, 1./15, 1./15};
		int[] ambulancePlacements3 = {2,3,3,3,3,3,3}; // should be of the length numRegions and with a total sum of numAmbulances
		Hospital hospital3 = new Hospital(numAmbulances, arrivalRates2, serviceRate, stopTime, numRegions, serveOutsideBaseRegion, ambulancePlacements3);
		hospital3.simulateOneRunAndReport();
		
    }

	private void simulateOneRunAndReport() {
		// Simulates one run and reports the results.
		ListOfStatProbes stats = simulateOneRun();
		printReport(stats);
		
	}

	private void printReport(ListOfStatProbes stats) {
		// Given a list of stats from a simulation, prints ("reports") important results to the console.
		// below prints are just an example
		System.out.println(stats.getName());
		System.out.println(stats.report());
		// Q for later: Is the result of simulateOneRun enough to print?
		
	}
}
