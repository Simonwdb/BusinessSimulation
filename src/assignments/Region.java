package assignments;

import java.util.LinkedList;

import umontreal.ssj.charts.ScatterChart;
import umontreal.ssj.charts.XYChart;
import umontreal.ssj.charts.XYLineChart;
import umontreal.ssj.randvar.ExponentialGen;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.simevents.Event;

/**
 * @author A van Oostveen
 * Edited by J Berkhout
 */
public class Region {

    // region variables
	LinkedList<Accident> queue; // for the unaddressed accidents
	LinkedList<Ambulance> idleAmbulances;
	double[] baseLocation; // of the particular region
	ArrivalProcess arrivalProcess;
	int regionID;
	Region[] regions;
	int numRegions;

	RandomStream locationStream; // random number generator for locations
    
	public Region(int id, double[] baseLocation, RandomStream arrivalRandomStream, double arrivalRate, RandomStream locationRandomStream, Region[] regionArray, int numRegions) {

		// set region variables
		queue = new LinkedList<>();
		idleAmbulances = new LinkedList<>();
		this.baseLocation = baseLocation;
		regionID = id;
		this.regions = regionArray;
		this.numRegions = numRegions;

		// set random streams
		arrivalProcess = new ArrivalProcess(arrivalRandomStream, arrivalRate);
		locationStream = locationRandomStream;
//		drawLocationsTest(); // test!
	}
    
    private void drawLocationsTest() {
		// Testing method for sampling points inside hexagon
    	double[][] points = getTestLocationDrawingPoints();
//    	double[][] boundaries = getTestLocationBoundaries();
//    	XYChart chart = new ScatterChart("Test Hexagon", "X", "Y", points, boundaries);
//    	chart.setAutoRange00(true, true); // Axes pass through (0,0)
//    	chart.view(800,500);

	}

	private double[][] getTestLocationDrawingPoints() {
		// Get a lot of testpoints for the hexagon drawing
		final int N = 10000;
		// unfortunately, drawing points is flipped the other way round
		double[][] points = new double[2][N];
		for (int i = 0; i < N; i++) {
			double[] cxy = drawLocation();
			points[0][i] = cxy[0];
			points[1][i] = cxy[1];
		}
		return points;

	}

	public void handleArrival() {
        // create and process a new accident
    	// SB: possible steps to be taken:
    	// 			- adding location to a new accident
    	//			- check for idle ambulance
    	//				- if idle, check if queue has elements
		//					- if queue, remove accident from queue and start service, append new created accident to queue
    	//					- if not queue, start with new created accident
    	//				- if not idle, append new created accident to queue
    	
    	// SB: am i missing some steps here? I think that's the way to handle an arrival?
		
		// SB: trying with the assumption that there always will be a idle ambulance
		double[] location = drawLocation();
		double arrivalTime = arrivalProcess.arrivalRate;
		Accident accident = new Accident(arrivalTime, location, this.regionID);
		
		for (int i = 0; i < this.idleAmbulances.size(); i++) {
			Ambulance amb = this.idleAmbulances.remove(i);
			double arrivalTimeAtAccident = amb.drivingTimeToAccident(accident);
			amb.startService(accident, arrivalTimeAtAccident);
			break;
		}

    }

    // returns a random location inside the region
    public double[] drawLocation() {
    	// Draw a random point from the central hexagon
    	double[] randomHexPoint = drawLocationHexCentre();
    	// For abbreviation and clarity
    	double X = randomHexPoint[0];
    	double Y = randomHexPoint[1];
    	double cx = baseLocation[0];
    	double cy = baseLocation[1];
    	// Scale this point to the current region, by translating according to centre
    	double[] result = {(X+cx),(Y+cy)};
    	return result;
    }

	private double[] drawLocationHexCentre() {
		// Draw a random point from the central hexagon
    	// Step 1. Pick one of the three rhombuses at random.
    	double[][] vectorList = getAllSpanVectors();
    	// Choose a rhombus index uniform randomly
    	int irhom = chooseRandomRhombus();
    	// get corresponding spanning vectors of the rhombus
    	double[] svector1 = vectorList[irhom];
    	// Account for overflow on the index (by adding 1 and modulo 3) for the other spanning vector
    	int iv2 = (irhom+1)%3;
    	double[] svector2 = vectorList[iv2];
    	
    	// abbreviate x and y values of the vectors v1,v2 that span up the rhombus chosen
    	double v1_x = svector1[0];
    	double v1_y = svector1[1];
    	double v2_x = svector2[0];
    	double v2_y = svector2[1];
    	
    	// Step 2. Pick a random point inside this rhombus
    	// Scale the vectors by an Uniform random value to get a random point in the rhombus
    	double A = locationStream.nextDouble();
    	double B = locationStream.nextDouble();
    	
    	// Scale the spanning vectors by random values to obtain a random point in this rhombus
    	double X = A*v1_x + B*v2_x;
    	double Y = A*v1_y + B*v2_y;
    	double[] result = {X,Y};
    	return result;
	}

	private double[][] getAllSpanVectors() {
		// Return three vectors spanning up the rhombuses (see report)
		double ytop = 2.5 * Math.sqrt(3);
		double ybottom = -ytop;
		// create vectors, {x,y}
		double[] vector0 = {-5,0};
		double[] vector1 = {2.5,ytop};
		double[] vector2 = {2.5,ybottom};
		double[][] vectorlist = {vector0,vector1,vector2};
		return vectorlist;
	}
	
	private int chooseRandomRhombus() {
		// Choose one of the three rhombuses in this hexagon (0, 1 or 2)
		int result = locationStream.nextInt(0, 2);
		return result;
	}

	class ArrivalProcess extends Event {

		ExponentialGen arrivalTimeGen;
		double arrivalRate;

		public ArrivalProcess(RandomStream rng, double arrivalRate) {
			this.arrivalRate = arrivalRate;
			arrivalTimeGen = new ExponentialGen(rng, arrivalRate);
		}

		// event: new customer arrival at the store
		@Override
		public void actions() {
			handleArrival();
			double nextArrival = arrivalTimeGen.nextDouble();
			schedule(nextArrival); // schedule a new arrival event
		}

		public void init() {
			double nextArrival = arrivalTimeGen.nextDouble();
			schedule(nextArrival); // schedule a first new arrival
		}
	}
}
