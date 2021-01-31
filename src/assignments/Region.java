package assignments;

import java.util.Arrays;
import java.util.LinkedList;

import umontreal.ssj.charts.ScatterChart;
import umontreal.ssj.charts.XYChart;
import umontreal.ssj.charts.XYLineChart;
import umontreal.ssj.randvar.ExponentialGen;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.simevents.Event;
import umontreal.ssj.simevents.Sim;

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
		//drawLocationsTest(); // test!
		
	}
    
    private void drawLocationsTest() {
		// Testing method for sampling points inside hexagon
    	double[][] points = getTestLocationDrawingPoints();
    	//double[][] boundaries = getTestLocationBoundaries();
    	XYChart chart = new ScatterChart("Test Hexagon", "X", "Y", points);
    	chart.setAutoRange00(true, true); // Axes pass through (0,0)
    	chart.view(800,500);

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

		
		// SB: trying with the assumption that there always will be a idle ambulance
		// Get current accident
		double currTime = Sim.time();
		double[] location = drawLocation();
		Accident accident = new Accident(currTime, location, this.regionID);
		if (Hospital.DEBUG_MODE) {
			// DEBUG
	    	System.out.println("NEW ACCIDENT in Region " + regionID);
			System.out.println("Region.handleArrival method:");
			System.out.println("TIME OF ACCIDENT: " + currTime);
			System.out.println(" new accident at location: ");
			System.out.println(" [" + location[0] + ", " + location[1] +"] \n");
		}
		
		// 27-01 addition
		// SB: checking if there is a queue or not; if there is a queue the newly created accident needs to be added to the queue and one from the queue needs to be removed, to start the service
		// Allen idle ambulances vanuit de centrale kunnen helpen.
		// By service complete pas vanaf de queue halen
		Ambulance amb = getAmbulanceAvailable(accident);
		boolean noAmbAvailable = (amb == null);
		if(noAmbAvailable) {
			
			if (Hospital.DEBUG_MODE) {System.out.println("Added to queue!!!");}
			this.queue.add(accident);	
		}
		else
			handleAccident(amb,accident);
    }

	private Ambulance getAmbulanceAvailable(Accident accident) {
		// Check if there are ambulances available to process this accident, if yes retrieve it!
		// TODO: als andere regios mogen helpen, moet deze methode dat ook regelen!
		
		// base case retrieve the ambulance from this.idleAmbulances (when ambulances can't help outside their region)
    	Ambulance result = this.idleAmbulances.pollFirst();
    	if(result == null)
    		return null;
    	if(!result.servesOutsideRegion)
    		return result;
    	
    	// SB: when result = null, it can't be used to make comparisons with other outside regions ambulances
    	double distance = (result == null) ? 1000.0 : result.drivingTimeToAccident(accident);

    	// second case: if ambulances can help outside their regions
    	for (int i = 0; i < this.regions.length; i++) {
    		if (this.regions[i].idleAmbulances.size() > 0) {
	    		if (this.regions[i].idleAmbulances.peekFirst().servesOutsideRegion) {
	    			if (distance > this.regions[i].idleAmbulances.peekFirst().drivingTimeToAccident(accident)) {
	    				result = this.regions[i].idleAmbulances.pollFirst();
	    				distance = result.drivingTimeToAccident(accident);
	    			}
	    		}
    		}
    	}
    	return result;
    	// NB! TODO: this ambulance is now no longer on the list of idle ambulances and needs to be kept track of!
	}
	

    private void queueAccident(Accident accident) {
		// Store the accident in the queue, so it can be helped later, when an ambulance is available
    	this.queue.add(accident);
	}
    
	private void handleAccident(Ambulance amb, Accident accident) {
		// Handle this accident with this ambulance directly!
		double drivingTime = amb.drivingTimeToAccident(accident); // houden we hier rekening met de huidige tijd?
		double currTime = Sim.time(); // klopt dit?
		double arrivalTimeAtAccident = drivingTime + currTime;
		
		if (Hospital.DEBUG_MODE) {
			System.out.println("Region.handleAccident method:");
			System.out.println(" Current time is: " + currTime);
			System.out.println(" +driving time to accident is: " + drivingTime);
			System.out.println(" So service will start at time: " + arrivalTimeAtAccident);				
	    	System.out.println(" Ambulance " + amb.id + " will handle this accident \n");
		}
		amb.startService(accident, arrivalTimeAtAccident);
		
//		wrapUpService(amb);
	}

	public void wrapUpService(Ambulance amb) {
		// TODO Auto-generated method stub
		// Try next in queue
		Accident qacc = this.queue.pollFirst();
		boolean nextinqueue = qacc != null;
		if(nextinqueue) { // if there is accident waiting, handle directly!
			if (Hospital.DEBUG_MODE) {
		    	System.out.println("QUEUED ACCIDENT in Region " + regionID);
				System.out.println("Region.handleArrival method:");
				System.out.println("SIM TIME START: " + Sim.time());
				System.out.println(" old accident at location: ");
				System.out.println(" [" + qacc.getLocation()[0] + ", " + qacc.getLocation()[1] +"] \n");
				System.out.println(" ACCIDENT TIME: " + qacc.getArrivalTime());
			}
			handleAccident(amb,qacc);
		}
		else // set ambulance to idle
	    	this.idleAmbulances.add(amb);
	}

	// returns a random location inside the region
    public double[] drawLocation() {
    	// Draw a random point from the central hexagon
    	double[] randomHexPoint = drawLocationHexCentre();
    	// For abbreviation and clarity
    	double X = randomHexPoint[0];
    	double Y = randomHexPoint[1];
    	double cx = this.baseLocation[0];
    	double cy = this.baseLocation[1];
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
