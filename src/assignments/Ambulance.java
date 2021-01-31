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
    	if (Hospital.DEBUG_MODE) {System.out.println("Ambulance.startService method:");}
    	
    	currentAccident = accident;
        accident.serviceStarted(arrivalTimeAtAccident); 
    	// Ambulance has arrived at accident location, check the responsetime first!
    	checkResponseTime();
    	
    	waitTimeTally.add(this.currentAccident.getWaitTime());
    	
        
        double processTimeAtScene = serviceTimeGen.nextDouble(); 
        double drivingTime = this.drivingTimeToHospital(this.currentAccident);
        
        
        double serviceTime = processTimeAtScene + drivingTime; // calculate the time needed to process the accident and drive back to the base
        
        if (Hospital.DEBUG_MODE) {
	        // DEBUG
	        System.out.println(" +process time at scene is: " + processTimeAtScene); 
	        System.out.println(" +driving to hospital time is: " + drivingTime);
	        System.out.println("This totals a service time of");
	        System.out.println(" +service time is: " + serviceTime);	
        }

        double totalBusyTime = drivingTime + serviceTime; // we need to add the driving time TO the accident to update the Sim clock correctly!
        
        if (Hospital.DEBUG_MODE) {
	        // DEBUG
	        System.out.println(" +total busy time Ambulance: " +totalBusyTime+ "\n");
	        System.out.println("Sim.time: " +Sim.time());
	        System.out.println("So completion will be handled at time : " + (Sim.time() + totalBusyTime));
        }
        
        serviceTimeTally.add(serviceTime);
        
        schedule(totalBusyTime); // niet vergeten Idle
    }

	private void checkResponseTime() {
		// Response time is the time between arrival of emergency call and arrival of ambulance at scene.
		// Should be lower than Hospital.RESPONSE_TIME_TARGET (15)
		if (Hospital.DEBUG_MODE) {System.out.println("Checking Response Time...");}
		double actualResponseTime = currentAccident.getWaitTime();
		if (Hospital.DEBUG_MODE) {System.out.println(" +response time is " + actualResponseTime);}
		boolean withinTargetResponse = actualResponseTime <= Hospital.RESPONSE_TIME_TARGET;
		
		int indicator = 0;
		if(withinTargetResponse) {
			indicator = 1;
			if (Hospital.DEBUG_MODE) {System.out.println(" Within target of " + Hospital.RESPONSE_TIME_TARGET + "!\n");}
		} else {
			if (Hospital.DEBUG_MODE) {System.out.println("NOT WITHIN TARGET OF 15! \n");}
		}
		
		this.withinTargetTally.add(indicator);
	}

	public void serviceCompleted() {
        // process the completed current accident: the ambulance brought the
        // patient to the hospital and is back at its base, what next?
    	double currTime = Sim.time(); 
    	
    	if (Hospital.DEBUG_MODE) {
	    	System.out.println("Service complete for Ambulance " + this.id);
	    	System.out.println("SIM TIME END/completionTime/currTime is: " + currTime);
	    	System.out.println("This should be equal to: Acc. Arrival time + Total Busy Time");
	    	System.out.println("Or: Acc. Arrival time + Response/driving time + Service time");
    	}
    	
    	
    	this.currentAccident.completed(currTime);
    	
    	this.currentAccident = null;
    	if (Hospital.DEBUG_MODE) {System.out.println("SERVICE ACCIDENT COMPLETED \n");}
    	this.baseRegion.wrapUpService(this);
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
    public double drivingTimeToAccident(Accident acc) {
        // calculate the driving time from the baselocation of the ambulance to the accident location
    	double[] ambulanceBase = this.baseRegion.baseLocation;
    	double[] accidentBase = acc.getLocation();
    	
    	return euclideanDistance(ambulanceBase, accidentBase);
    }

    // return Euclidean distance between accident and hospital
    public double drivingTimeToHospital(Accident acc) {
        // calculate the driving time from accident location to the hospital
    	double[] accidentBase = acc.getLocation();
    	double[] hospitalBase = {0., 0.};
        
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