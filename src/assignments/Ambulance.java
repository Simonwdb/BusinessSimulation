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
    	// arr time accident klopt niet??? sim time lijkt niet geupdate te worden!!
        currentAccident = accident;
        
        
        // 28-01 SB: we roepen hier Sim.time(), maar gebruiken deze verder niet?
//        double currSimTime = Sim.time();
        
        double serviceTimeAtScene = serviceTimeGen.nextDouble(); // is dit zo?
        // should we notify that the accident person is now picked up?
        double drivingTimeBack = this.drivingTimeToHospital(this.currentAccident);
        double busyServing = serviceTimeAtScene + drivingTimeBack; // calculate the time needed to process the accident and drive back to the base
        System.out.println("Ambulance.startService method: service time at scene is: " + serviceTimeAtScene + 
        					", driving to hospital time is: " + drivingTimeBack + ", busy serving time is: " + busyServing + "\n");	
        accident.serviceStarted(arrivalTimeAtAccident); // klopt dit? moet je dit nog ophogen met de tijd huidig
        schedule(busyServing); // after busyServing it becomes idle again
        // SB: i think this method is correct
    }

    public void serviceCompleted() {
        // process the completed current accident: the ambulance brought the
        // patient to the hospital and is back at its base, what next?
    	double currTime = Sim.time(); // dit klopt niet gek genoeg
    	
    	
    	// SB: calculating the response time with arrival time of the accident and drivingTimeToAccident
    	double arrivalTime = this.currentAccident.getArrivalTime();
    	double drivingTimeToAccident = this.drivingTimeToAccident(this.currentAccident);
    	double actualResponseTime =  arrivalTime + drivingTimeToAccident;
    	if (actualResponseTime <= Hospital.RESPONSE_TIME_TARGET) {	// Check if response time is below or above target
    		withinTargetTally.add(1);
    	} else {
    		withinTargetTally.add(0);
    	}
    	
    	System.out.println("Ambulance.serviceCompleted method: sim.time() is: " + currTime + ", arrivalTime is: " + arrivalTime + ", drivingTimeToAccident is: " + drivingTimeToAccident
    						+ ", actualResponseTime is: " + actualResponseTime + "\n");
    	
    	this.currentAccident.completed(currTime);
    	
    	waitTimeTally.add(this.currentAccident.getWaitTime());
    	serviceTimeTally.add(this.currentAccident.getServiceTime());
    	
    	// SB: Do we need to update in this function that the currentAccident is now null?
    	this.currentAccident = null;
    	
    	// SB: Do we need to update that the ambulance is driving back from the hospital to their base?
    	
    	// SB: Do we need to update the ambulance to idle?
    	baseRegion.idleAmbulances.add(this);
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