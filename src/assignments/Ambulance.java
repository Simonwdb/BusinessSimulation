package assignments;

import java.awt.geom.Point2D;

import umontreal.ssj.randvar.ExponentialGen;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.simevents.Event;
import umontreal.ssj.simevents.Sim;
import umontreal.ssj.stat.TallyStore;

/**
 * @author mctenthij
 * Edited by qvanderkaaij and jberkhout
 */
public class Ambulance extends Event {

	// ambulance variables
	int id;
	Region baseRegion;
	Accident currentAccident;
	boolean servesOutsideRegion;
	double drivingTimeHospitalToBase;

	// RNG
	ExponentialGen serviceTimeGen;

	// stats counters
	TallyStore waitTimeTally = new TallyStore("Waiting times");
	TallyStore serviceTimeTally = new TallyStore("Service times");
	TallyStore withinTargetTally = new TallyStore("Arrival within target");

	public Ambulance(int id, Region baseRegion, RandomStream serviceRandomStream, double serviceRate, boolean servesOutsideRegion) {
		this.id = id;
		currentAccident = null;
		this.baseRegion = baseRegion;
		serviceTimeGen = new ExponentialGen(serviceRandomStream, serviceRate);
		this.servesOutsideRegion = servesOutsideRegion;
		this.drivingTimeHospitalToBase = drivingTimeHospitalToBase();
	}

    public void startService(Accident accident, double arrivalTimeAtAccident) {
        System.out.println("Ambulance.startService method:");
    	currentAccident = accident;
        accident.serviceStarted(arrivalTimeAtAccident); 
    	// Ambulance has arrived, check the responsetime first!
    	checkResponseTime();
        
        double processTimeAtScene = serviceTimeGen.nextDouble(); 
        // should we notify that the accident person is now picked up?
        double drivingTime = this.drivingTimeToHospital(this.currentAccident);
        
        
        double serviceTime = processTimeAtScene + drivingTime; // calculate the time needed to process the accident and drive back to the base
        
        // DEBUG
        System.out.println(" +process time at scene is: " + processTimeAtScene); 
        System.out.println(" +driving to hospital time is: " + drivingTime);
        System.out.println("This totals a service time of");
        System.out.println(" +service time is: " + serviceTime);	
        

        double totalBusyTime = drivingTime + serviceTime; // we need to add the driving time TO the accident to update the Sim clock correctly!
        
        // DEBUG
        System.out.println(" +total busy time Ambulance: " +totalBusyTime+ "\n");
        
        schedule(totalBusyTime); // niet vergeten Idle
    }

	private void checkResponseTime() {
		// Response time is the time between arrival of emergency call and arrival of ambulance at scene.
		// Should be lower than Hospital.RESPONSE_TIME_TARGET (15)
		System.out.println("Checking Response Time...");
		double actualResponseTime = currentAccident.getWaitTime();
		System.out.println(" +response time is " + actualResponseTime);
		boolean withinTargetResponse = actualResponseTime <= Hospital.RESPONSE_TIME_TARGET;
		int indicator = 0;
		if(withinTargetResponse) {
			indicator = 1;
			System.out.println(" Within target of " + Hospital.RESPONSE_TIME_TARGET + "!\n");
		}
		this.withinTargetTally.add(indicator);
	}

	public void serviceCompleted() {
        // process the completed current accident: the ambulance brought the
        // patient to the hospital and is back at its base, what next?
    	double currTime = Sim.time(); // dit klopt eindelijk!
    	System.out.println("Service complete for Ambulance " + this.id);
    	System.out.println("Ambulance.serviceCompleted method: \n completionTime/currTime/sim.time() is: " + currTime);
    	System.out.println("This should be equal to: Acc. Arrival time + Total Busy Time");
    	System.out.println("Or: Acc. Arrival time + Response/driving time + Service time");
    	
    	this.currentAccident.completed(currTime);
    	
    	waitTimeTally.add(this.currentAccident.getWaitTime());
    	serviceTimeTally.add(this.currentAccident.getServiceTime());
    	
    	// SB: Do we need to update in this function that the currentAccident is now null?
    	this.currentAccident = null;
    	
    	// SB: Do we need to update that the ambulance is driving back from the hospital to their base?
    	
    	// SB: Do we need to update the ambulance to idle?
    	baseRegion.idleAmbulances.add(this);
    	System.out.println("SERVICE ACCIDENT COMPLETED");
    }
    
    private double euclideanDistance(double[] first, double[] second) {
    	double x1 = first[0];
    	double y1 = first[1];
    	double x2 = second[0];
    	double y2 = second[1];

    	double result = Point2D.distance(x1, y1, x2, y2);
        
    	return result;
    }

    // return Euclidean distance between accident and hospital
    public double drivingTimeToAccident(Accident cust) {
        // calculate the driving time from the baselocation of the ambulance to the accident location
    	double[] ambulanceBase = this.baseRegion.baseLocation;
    	double[] accidentBase = cust.getLocation();
    	
    	return euclideanDistance(ambulanceBase, accidentBase);
    }

    // return Euclidean distance between accident and hospital
    public double drivingTimeToHospital(Accident acc) {
        // calculate the driving time from accident location to the hospital
    	double[] accidentBase = acc.getLocation();
    	double[] hospitalBase = {0., 0.};	// SB: need to find a way to retrieve the location of the hospital 
        
    	return euclideanDistance(accidentBase, hospitalBase);
    }

	// return Euclidean distance from the hospital to the base
	public double drivingTimeHospitalToBase() {
        // calculate the driving time from the hospital to the base
		double[] hospitalBase = {0., 0.};	// SB: need to find a way to retrieve the location of the hospital
		double[] ambulanceBase = this.baseRegion.baseLocation;
		
		return euclideanDistance(hospitalBase, ambulanceBase);
	}

    // event: the ambulance is back at its base after completing service
    @Override
    public void actions() {
        serviceCompleted();
    }
}