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
		System.out.println("Region.handleArrival method:\n new accident at location: \n [" + location[0] + ", " + location[1] +"] \n at Sim time: " + currTime + "\n");
		
		
		// 27-01 addition
		// SB: checking if there is a queue or not; if there is a queue the newly created accident needs to be added to the queue and one from the queue needs to be removed, to start the service
		// Allen idle ambulances vanuit de centrale kunnen helpen.
		// By service complete pas vanaf de queue halen
		Ambulance amb = getAmbulanceAvailable();
		boolean noAmbAvailable = (amb == null);
		if(noAmbAvailable) {
			this.queue.add(accident);	
//			queueAccident(accident);	SB: hiervoor hoeft toch geen aparte method voor aangemaakt te worden? Dit lijkt mij enigzins overbodig
		}
		else 
			handleAccident(amb,accident);
    }

	private Ambulance getAmbulanceAvailable() {
		// Check if there are ambulances available to process this accident, if yes retrieve it!
		// TODO: als andere regios mogen helpen, moet deze methode dat ook regelen!
    	Ambulance result = this.idleAmbulances.pollFirst();
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
		double currTime = accident.getArrivalTime();
		double arrivalTimeAtAccident = drivingTime + currTime;
		System.out.println("Region.handleAccident method: \n Driving time to accident is: " + drivingTime + ", \n accident.getArrivalTime() (Sim) is: " + currTime + 
							",\n service starts at time: " + arrivalTimeAtAccident + "\n");
		amb.startService(accident, arrivalTimeAtAccident);
		// 28-01 SB: is het mogelijk om vanuit hier de sim.time() up te daten? dat is wat hier nog ontbreekt lijkt me
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
