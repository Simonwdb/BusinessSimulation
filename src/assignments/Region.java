package assignments;

import java.util.LinkedList;
import java.util.Random;

import umontreal.ssj.randvar.ExponentialGen;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.simevents.Event;

/**
 *
 * @author mctenthij
 */
public class Region {
    LinkedList<Accident> queue;     //Queue of the server
    LinkedList<Ambulance> idleAmbulances;  // Available ambulance
    double [] baseLocation;
    ArrivalProcess arrivalProcess;
    RandomStream locationStream;
    int regionID;
    
    public Region(double baseXCoordinate, double baseYCoordinate, RandomStream rng, double arrivalRate, RandomStream location, int rid) {
        queue = new LinkedList<>();
        idleAmbulances = new LinkedList<>();
        baseLocation = new double[2];
        baseLocation[0] = baseXCoordinate;
        baseLocation[1] = baseYCoordinate;
        arrivalProcess = new ArrivalProcess(rng,arrivalRate);
        locationStream = location;
        regionID = rid;
    }
    
    public void handleArrival() {
        // process a new arrival
    }
    
    public double[] drawLocation() {
        // determine the location of the accident
        double[] location = new double[2];
        
        // SB: locations of arriving emergency calls are chosen uniform in their region
        // SB: diameter of the hexagon (region) is 10km
        // SB: coordinates can be chosen uniform over the surface of the hexagon. 
        
        /*
         * even in NL:
         * de diameter is 10, dan lijkt mij dat de straal 5 is.
         * oppervlakte is dan: (3/2) * wortel(3) * 5^2
         */
        
        double surfaceRegion = (3./2) * Math.sqrt(3) * Math.pow(5, 2);
        
        // SB: how can we choose uniformly between zero and surface?
        
        location[0] = 0.0; // X-Coordinate of accident location
        location[1] = 0.0; // Y-Coordinate of accident location
        return location;
    }
    
    class ArrivalProcess extends Event {
        ExponentialGen arrivalTimeGen;
        double arrivalRate;

        public ArrivalProcess(RandomStream rng, double arrivalRate) {
            this.arrivalRate = arrivalRate;
            arrivalTimeGen = new ExponentialGen(rng, arrivalRate);
        }
        
        @Override
        public void actions() {
            double nextArrival = arrivalTimeGen.nextDouble();
            schedule(nextArrival);//Schedule this event after
            //nextArrival time units
            handleArrival();
        }
        
        public void init() {
            double nextArrival = arrivalTimeGen.nextDouble();
            schedule(nextArrival);//Schedule this event after
            //nextArrival time units
        }
    }
    
}
