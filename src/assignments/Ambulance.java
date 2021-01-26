package assignments;

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
        currentAccident = accident;
        accident.serviceStarted(arrivalTimeAtAccident);
        double serviceTimeAtScene = serviceTimeGen.nextDouble();

        // SB: busyServing = processing time (exponential distribution with mu = 1) plus driving time to hospital from accident, this sentence comes from assignment pdf
        double busyServing = serviceTimeGen.nextDouble() + this.drivingTimeToHospital(this.currentAccident); // calculate the time needed to process the accident and drive back to the base
        
        schedule(busyServing); // after busyServing it becomes idle again
    }

    public void serviceCompleted() {
        // process the completed current accident: the ambulance brought the
        // patient to the hospital and is back at its base, what next?
    	this.currentAccident.completed(Sim.time());
    	
    	// SB: calculating the response time with arrival time of the accident and drivingTimeToAccident
    	double actualResponseTime = this.currentAccident.getArrivalTime() + this.drivingTimeToAccident(this.currentAccident);
    	
    	if (actualResponseTime <= 15) {	// SB: how can we access the RESPONSE_TIME_TARGET in Hospital.java
    		withinTargetTally.add(1);
    	} else {
    		withinTargetTally.add(0);
    	}
    	
    	waitTimeTally.add(this.currentAccident.getWaitTime());
    	serviceTimeTally.add(this.currentAccident.getServiceTime());
    	
    	// SB: Do we need to update in this function that the currentAccident is now null?
    	this.currentAccident = null;
    	
    	// SB: Do we need to update that the ambulance is driving back from the hospital to their base?
    	
    	// SB: Do we need to update the ambulance to idle?
    	
    }
    
    private double euclideanDistance(double[] first, double[] second) {
    	double result = Math.sqrt(Math.pow(second[0] - first[0], 2) + Math.pow(second[1] - first[1], 2));
        
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
    public double drivingTimeToHospital(Accident cust) {
        // calculate the driving time from accident location to the hospital
    	double[] accidentBase = cust.getLocation();
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