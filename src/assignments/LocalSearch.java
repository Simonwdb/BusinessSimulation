package assignments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import assignments.LocalSearch.State;
import cern.colt.Arrays;
import umontreal.ssj.rng.MRG32k3a;
import umontreal.ssj.simevents.Sim;
import umontreal.ssj.stat.TallyStore;
import umontreal.ssj.stat.list.ListOfStatProbes;

public class LocalSearch {
	// A state represents a solution and has an a-g value which correspond
    // with the Region 0-6 number of ambulance placements, respectively.
    
	class State {

        int aval;
        int bval;
        int cval;
        int dval;
        int eval;
        int fval;
        int gval;
        TallyStore values;

        public State(int a, int b, int c, int d, int e, int f, int g) {
            this.aval = a;
            this.bval = b;
            this.cval = c;
            this.dval = d;
            this.eval = e;
            this.fval = f;
            this.gval = g;
            this.values = new TallyStore("("+a+","+b+","+c+","+d+","+e+","+f+", "+g+")");
            this.values.init();
        }
        
        public State(int[] params) {
            this.aval = params[0];
            this.bval = params[1];
            this.cval = params[2];
            this.dval = params[3];
            this.eval = params[4];
            this.fval = params[5];
            this.gval = params[6];
            this.values = new TallyStore("("+aval+","+bval+","+cval+","+dval+","+eval+","+fval+", "+gval+")");
            this.values.init();
        }
        
        public int[] toAmbPlacements() {
        	// Returns the values in an array
        	int[] result = {aval,bval,cval,dval,eval,fval,gval};
        	return result;
        }
    }
	
	public HashMap<String,State> outputs;
	public int numAmbulances;
    Random rng = new Random();
	private double[] arrivalRates;
	private double serviceRate;
	private double stopTime;
	private int numRegions;
	private boolean serveOutsideBaseRegion;
	public int budget;

    static final double BIGM = 9999999999999.99;
	
	public LocalSearch(int numAmbulances, double[] arrivalRates, double serviceRate, double stopTime, int numRegions, boolean serveOutsideBaseRegion, int budget) {
		
		this.numAmbulances = numAmbulances;
		this.arrivalRates = arrivalRates;
		this.serviceRate = serviceRate;
		this.stopTime  = stopTime;
		this.numRegions = numRegions;
		this.serveOutsideBaseRegion = serveOutsideBaseRegion;
		this.budget = budget; // 5000
		
		setUpLocalSearch();
	}
	
	private void setUpLocalSearch() {
		// Perform basic setting up.
		createStates();
		rng.setSeed(0);
	}
	
	private void createStates() {
		// Takes around two minutes due to the large size of parameter options
		outputs = new HashMap<String,State>();
		int n = numAmbulances + 1; // usually 21

		for(int a = 0; a<n; a++)
			for(int b = 0; b<n; b++)
				for(int c = 0; c<n; c++)
					for(int d = 0; d<n; d++)
						for(int e = 0; e<n; e++)
							for(int f = 0; f<n; f++)
								for(int g = 0; g<n; g++)
								{
									int sum = a+b+c+d+e+f+g;
									if(sum == numAmbulances) {
										String key = computeKey(a,b,c,d,e,f,g);
										State state = new State(a,b,c,d,e,f,g);
										outputs.put(key, state);
									}
								}
	}
	
	private String computeKey(int a, int b, int c, int d, int e, int f, int g) {
		// Given 7 ambulance options, computes the corresponding key
		String result = "" + a;
		result += b;
		result += c;
		result += d;
		result += e;
		result += f;
		result += g;
		return result;
	}
	
	public State runLocalSearch() {

    	// perform Local Search
    	performLocalSearch();
    	
    	// Choose state with the lowest _r(pi), lowest sample average, based on localSearch
        State opt = selectOptimalState();
        printLocalSearchResults(opt); // added for our convenience
        
        return opt;
    }

	private void performLocalSearch() {
		// Initialization
    	State currpi = selectRandomStart(); // keeps track of current state, start somewhere random
    	int m = budget; 					// keep track of the simulation budget
    	// NB: sample outputs are already reset in main(), as a new object of TemplateAssignment3 is created
    	// this means we do not have to initialize sample averages / r, for certain states/solutions
    	
    	while (m>0) {
    		State neighborpi = selectRandomNeighbor(currpi);
    		// Simulate output for both states and update the current sample average for both (done by runSingleRun, through output values average)
    		runSingleRunState(currpi);
    		runSingleRunState(neighborpi);
    		currpi = selectBestState(currpi, neighborpi); // move to "best" solution of the two
    		m = m-2; 									  // update simulation budget with # simulation budget left (two states visited)
    	}
    	
    	// NB: does not return anything, best state is selected by method selectOptimalState in runLocalSearch
	}
	
    // Returns the optimal state after using a ranking algorithm based on the average costs
    public State selectOptimalState() {
        double maximum = -BIGM;
        State max = null;
        // Needs to be changed to for all in HashMap, instead of outputs array
        /*
        for (int i = 0; i < numStates; i++) {
            if (outputs[i].values.numberObs() > 0) {
                if (outputs[i].values.average() > maximum) {
                    maximum = outputs[i].values.average();
                    max = outputs[i];
                }
            }
        }
        */
        return max;
    }

    public State selectRandomStart() {
        // select a random state
        String i = randOutputIndex(); 
        State state = outputs.get(i);

        return state;
    }
    
    private String randOutputIndex() {
		// Return a random index from the outputs array
    	return "2333333";
        /*MRG32k3a rand = getStream();
        int l = 0;			   // lowest possible index
        int u = numStates - 1; // largest possible index
        int i = rand.nextInt(l,u);
        return i;*/
	}
    

	public State selectRandomNeighbor(State state) {
        State neighbor;    
        // get all neighbors (denoted by their unique indices corresponding to a certain k and K (or xval and yval))
        List<String> ineighbors = getAllNeighbors(state);
        // select a random one from the list
        String ri = pickRandom(ineighbors);
        // return the correct state corresponding to this index
        neighbor = outputs.get(ri);
        return neighbor;
    }
    
	private String pickRandom(List<String> list) {
		// Pick a random String from this list
		int li = 0; 				 // always a lower bound index of this list
		int ui = list.size() - 1;    // last possible index of this list
		
		MRG32k3a rand = getStream();
		int ri = rand.nextInt(li, ui); // random index between li and ui
		String relement = list.get(ri);   // retrieve the element at this index ri and return it
		return relement;
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

	private List<String> getAllNeighbors(State state) {
		// return all neighbors in the list, denote them by their unique index (used in outputs[])
		List<String> result = new ArrayList<String>();
		
		// set correct lower and upper bounds, 0 and 20

		
		// Add every neighbor in the "neighbourhood" to the list
		// Neighbourhood is defined by placing an ambulance in another region
		// for every neighbor
		// {
		//		result.add(ni);
		// }
		return result;
	}


	private void runSingleRunState(State pi) {
		// Wrapper method: Performs a 'run' for a certain state
		int[] ambPlacements = pi.toAmbPlacements();
		
		runSingleRun(ambPlacements);
	}
	
    public void runSingleRun(int[] ambPlacements) {

        // We need to do something extra here
        
        runSimulation(ambPlacements);
    }
    
    public void runSimulation(int[] ambPlacements) {
        
    	Hospital h = new Hospital(numAmbulances, arrivalRates, serviceRate, stopTime, numRegions, serveOutsideBaseRegion, ambPlacements);
    	ListOfStatProbes ls = h.simulateOneRun();
    	// update outputs with averages of target score
		
	}
	
	public State selectBestState(State current, State neighbor) {

    	// return best state
		double rcurrent = current.values.average();
		double rneighbor = neighbor.values.average();
		
		// If the sample average of the neighbor is better (which means: higher! as in higher target score), select neighbor
		if(rneighbor >= rcurrent)
			return neighbor;
		// Otherwise, stay at current state
		else
			return current;
    }
	
    private void printLocalSearchResults(State opt) {
		// Prints out the k and K of this "optimal" state
    	System.out.println("Local search results - best state found is:");
        System.out.println(Arrays.toString(opt.toAmbPlacements()));
		// and the average costs found for this choice of threshold settings
        System.out.println("Average costs per time unit (long-run):");
        System.out.println(opt.values.average());
	}
}

