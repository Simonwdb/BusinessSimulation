package assignments;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;
import umontreal.ssj.probdist.NormalDist;
import umontreal.ssj.probdist.StudentDist;
import umontreal.ssj.rng.MRG32k3a;
import umontreal.ssj.simevents.Sim;
import umontreal.ssj.stat.TallyStore;

/**
 *
 * @author mctenthij
 * Edited by qvanderkaaij and jberkhout
 */
public class TemplateAssignment3 {

    // optimization variables
    State[] outputs;
    int numStates;
    int budget;
    int xmin;
    int ymin;
    int xmax;
    int ymax;

    // threshold queue variables
    int k;
    int K;
    double lambda;
    double avgService;
    double avgHighService;
    double stopTime;

    Random rng = new Random();

    static final double BIGM = 9999999999999.99;

    public TemplateAssignment3(int xmin, int xmax, int ymin, int ymax, int budget, double lambda, double muLow, double muHigh, double stopTime, int k, int K) {
        
    	// check how many states are possible
    	int xrange = xmax - xmin + 1;
        int yrange = ymax - ymin + 1;
        numStates = xrange*yrange;
        outputs = new State[numStates];

        // create states and store them in outputs[]
        for (int i = 0; i < xrange; i++) {
            for (int j = 0; j < yrange; j++) {
                State state = new State(i+xmin,j+ymin);
                outputs[(yrange)*i+j] = state;
            }
        }

        // set optimization variables
        this.budget = budget;
        this.ymin = ymin;
        this.xmin = xmin;
        this.ymax = ymax;
        this.xmax = xmax;

        // set threshold queue variables
        this.k = k;
        this.K = K;
        this.lambda = lambda;
        this.avgService = muLow;
        this.avgHighService = muHigh;
        this.stopTime = stopTime;
    }

	// A state represents a solution and has an x and y value which correspond
    // with the k and K value, respectively.
    class State {

        int xval;
        int yval;
        TallyStore values;

        public State(int x, int y) {
            this.xval = x;
            this.yval = y;
            this.values = new TallyStore("("+x+","+y+")");
            this.values.init();
        }
    }

    // Calculates the index position of a state in the output array.
    public int calcPos(int x, int y) {
        return (x-xmin)*(ymax-ymin+1)+y-ymin;
    }

    // Returns the optimal state after using a ranking algorithm based on the average costs
    public State selectOptimalState() {
        double minimum = BIGM;
        State min = null;
        for (int i = 0; i < numStates; i++) {
            if (outputs[i].values.numberObs() > 0) {
                if (outputs[i].values.average() < minimum) {
                    minimum = outputs[i].values.average();
                    min = outputs[i];
                }
            }
        }
        return min;
    }

    public State getState(int[] val) {
        int pos = calcPos(val[0],val[1]);
        return outputs[pos];
    }

    // Use this method to create a new, random MRGG32k3a variable.
    public MRG32k3a getStream() {
        long[] seed = new long[6];

        //TO DO: Fill the long[] with random seeds, for example, using rng
        for (int i = 0; i < seed.length; i++) {
        	seed[i] = (long) rng.nextInt();
        }
        
        MRG32k3a myrng = new MRG32k3a();
        myrng.setSeed(seed);
        return myrng;
    }

    public void runSingleRun(int k, int K) {

        // init random sources
    	MRG32k3a arrival = getStream();
        MRG32k3a service = getStream();
        
        runSimulation(k,K, arrival, service);
    }
    
    public void runSimulation(int k, int K, MRG32k3a arrival, MRG32k3a service) {
        
    	// init simulation and create threshold queue
        Sim.init();
        ThresholdQueue model = new ThresholdQueue(lambda, avgService, avgHighService, stopTime, k, K, arrival, service);
       
        // get results and add to the right state
        double result = model.getAverageCosts().average();
        int i = calcPos(k, K);
        outputs[i].values.add(result);
		
	}

	public void runDoubleRunCRN(int k, int K, int k2, int K2) {

        // init random sources
    	MRG32k3a arrival = getStream();
        MRG32k3a service = getStream();
        
        // Use the same random values (CRN)
        runSimulation(k,K,arrival, service);
        runSimulation(k2,K2,arrival,service);
    }

    public State runRankingSelection(int initialRuns, double alpha) {
    	
    	int firstRun = initialRuns / this.numStates;
    	
    	for (int i = 0; i < firstRun; i++) {
    		for (int j = 0; j < this.numStates; j++) {
    			runSingleRun(this.outputs[j].xval, this.outputs[j].yval);
    		}
    	}
    	
    	HashSet<State> I = selectCandidateSolutions(alpha);
    	
    	int lengthOfI = I.size();
    	State[] candidates = I.toArray(new State[lengthOfI]);
    	
    	
    	int secondRun = (this.budget - initialRuns) / candidates.length;
    	
    	for (int i = 0; i < secondRun; i++) {
    		for (int j = 0; j < this.numStates; j++) {
    			runSingleRun(candidates[i].xval, candidates[i].yval);
    		}
    	}
    	
    	State opt = selectOptimalState();
    	
    	return opt;
    }

    public HashSet<State> selectCandidateSolutions(double alpha) {

        HashSet<State> I = new HashSet();
        ArrayList<Double> avg = new ArrayList<Double>();
        ArrayList<Double> var = new ArrayList<Double>();
        
        int simulationAmount = (this.budget / 2) / this.numStates;
        
        for (int i = 0; i < this.numStates; i++) {
        	avg.add(this.outputs[i].values.average());
        	var.add(this.outputs[i].values.variance());
        }
        
        StudentDist dist = new StudentDist(this.numStates - 1);
        double val = dist.inverseF(1 - alpha);
        
        for (int i = 0; i < this.numStates; i++) {
        	for (int j = 1; j < this.numStates - 1; j++) {
        		double avgCost1 = Math.abs(avg.get(i));
        		double avgCost2 = Math.abs(avg.get(j));
        		double stDev1 = Math.abs(Math.sqrt(var.get(i)));
        		double stDev2 = Math.abs(Math.sqrt(var.get(j)));
        		
        		double t_test = Math.abs(avgCost1 - avgCost2) / (Math.sqrt(Math.pow(stDev1, 2) / simulationAmount + Math.pow(stDev2, 2) / simulationAmount));
        		
        		if (t_test < val) {
        			int[] coords = {0,0};
        			if (this.outputs[i].values.average() < this.outputs[j].values.average()) {
        				coords[0] = this.outputs[i].xval;
        				coords[1] = this.outputs[i].yval;
        			} else {
        				coords[0] = this.outputs[j].xval;
        				coords[1] = this.outputs[j].yval;
        			}
        			State c = getState(coords);
        			I.add(c);
        		}
        	}
        }

        // find all candidate solutions for the ranking and selection method
        return I;
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

    public State selectRandomStart() {
        // select a random state
        int i = randOutputIndex(); 
        State state = outputs[i];

        return state;
    }
    
    private int randOutputIndex() {
		// Return a random index from the outputs array
        MRG32k3a rand = getStream();
        int l = 0;			   // lowest possible index
        int u = numStates - 1; // largest possible index
        int i = rand.nextInt(l,u);
        return i;
	}
    
    private int randOutputIndex2() {
    	// not used: slower version of randOutputIndex
		// Return a random index from the outputs array
        MRG32k3a rand1 = getStream();
        MRG32k3a rand2 = getStream();
        int rx = rand1.nextInt(xmin, xmax);
        int ry = rand2.nextInt(ymin, ymax);
        int i = calcPos(rx,ry);
        return i;
	}

	public State selectRandomNeighbor(State state) {
        State neighbor;    
        // get all neighbors (denoted by their unique indices corresponding to a certain k and K (or xval and yval))
        List<Integer> ineighbors = getAllNeighbors(state);
        // select a random one from the list
        int ri = pickRandom(ineighbors);
        // return the correct state corresponding to this index
        neighbor = outputs[ri];
        return neighbor;
    }
    
	private int pickRandom(List<Integer> list) {
		// Pick a random number from this list
		int li = 0; 				 // always a lower bound index of this list
		int ui = list.size() - 1;    // last possible index of this list
		
		MRG32k3a rand = getStream();
		int ri = rand.nextInt(li, ui); // random index between li and ui
		int relement = list.get(ri);   // retrieve the element at this index ri and return it
		return relement;
	}

	private List<Integer> getAllNeighbors(State state) {
		// return all neighbors in the list, denote them by their unique index (used in outputs[])
		List<Integer> result = new ArrayList<Integer>();
		// abbreviate the k and K of this state
		int x = state.xval; // k of this state
		int y = state.yval; // K of this state
		
		// set correct lower and upper bounds for neighbors
		// xmax/xmin/ymin/ymax checks are done to ensure correct "corner neighbors"
		// e.g. xl is the lower bound for x neighbor, if x = xmin, xl is also equal to xmin instead of x-1, etc.
		int xl = Math.max(x-1, xmin);
		int xu = Math.min(x+1, xmax);
		int yl = Math.max(y-1, ymin);
		int yu = Math.min(y+1, ymax);
		
		// Add every neighbor in the "neighbourhood" (within xl,xu,yl,yu) to the list
		for(int nx = xl; nx<=xu; nx++)
			for(int ny = yl; ny<=yu; ny++) {
				// do not include the current state as a "neighbor"!
				boolean sameascurr = (nx == x) && (ny == y);
				if(sameascurr) // if so, skip this iteration
					continue;
				
				// if this is a correct neighbor, calculate its unique index
				int ni = calcPos(nx, ny);
				// and add it to the result array
				result.add(ni);
			}
		return result;
	}


	private void runSingleRunState(State pi) {
		// Wrapper method: Performs a 'run' for a certain state
		int k = pi.xval;
		int K = pi.yval;
		
		runSingleRun(k, K);
	}
	
	public State selectBestState(State current, State neighbor) {

    	// return best state
		double rcurrent = current.values.average();
		double rneighbor = neighbor.values.average();
		
		// If the sample average of the neighbor is better (which means: lower! as in lower costs), select neighbor
		if(rneighbor <= rcurrent)
			return neighbor;
		// Otherwise, stay at current state
		else
			return current;
    }
	
    private void printLocalSearchResults(State opt) {
		// Prints out the k and K of this "optimal" state
    	System.out.println("Local search results - best state found is:");
        System.out.println("k =");
        System.out.println(opt.xval);
        System.out.println("K =");
        System.out.println(opt.yval);
		// and the average costs found for this choice of threshold settings
        System.out.println("Average costs per time unit (long-run):");
        System.out.println(opt.values.average());
	}
    public double[] simulateCommonRandomNumbersRun(int k2, int K2){
        double[] results = new double[2];

        // perform CRN on (k,K) and (k2,K2) as parameters, average costs is result per run
        runDoubleRunCRN(k, K, k2, K2);
        
        // compute indices for outputs
        int i1 = calcPos(k, K);
        int i2 = calcPos(k2, K2);
        
        // We perform only two runs now, one for (k,K), one for (k2,K2)
        double result1 = outputs[i1].values.average(); // note that this can be used for more runs, as with one run the average is equal to the (only) value in values itself.
        double result2 = outputs[i2].values.average();
        
        // So:  results[0] = average costs per run with CRN & (k,K)
        // And: results[1] = average costs per run with CRN & (k2, K2)
        
        results[0] = result1;
        results[1] = result2;
        
        printCRNResults(results); // added for our convenience
        
        return results;
    }

    private void printCRNResults(double[] results) {
		// Prints out the results
    	System.out.println("Results of CRN runs - for given k and K");
    	System.out.println("Average costs per run (with CRN) for");
    	System.out.println("(k=5, K=20):");
    	System.out.println(results[0]);
    	System.out.println("And (k2=10, K2=20):");
    	System.out.println(results[1]);
	}

	public static void main(String[] args) {
        int k = 5;             			 // k-threshold for queue
        int K = 20;             			 // K-threshold for queue (do we need to set this to 20 or to 10 -> see "switch to muH = 4"
        int k2 = 10;            			 // k-threshold for alternative queue
        int K2 = 20;            			 // K-threshold for alternative queue
        double lambda = 3./2;     			 //service rate	
        double muLow = 2;				 // average low service time
        double muHigh = 4;    			 // average high service time
        double stopTime = 10000;     	 // Simulation endtime (seconds)

        int xmin = 5;					 // Lowest possible value for k
        int xmax = 10;					 // Highest possible value for k
        int ymin = 10;					 // Lowest possible value for K	
        int ymax = 20;					 // Highest possible value for K
        int budget = 5000;				 // Budget for the initial runs
        
        int initialRuns = 2500;			  // initial runs for the Ranking and selection method
        double alpha = 0.05; 			  // alpha value for the Ranking and selection method
        
        // Note that the results are printed inside their respective methods
        //TemplateAssignment3 crn = new TemplateAssignment3(xmin, xmax, ymin, ymax, budget, lambda, muLow, muHigh, stopTime, k, K);
        //double results[] = crn.simulateCommonRandomNumbersRun(k2,K2);

        //TemplateAssignment3 optimization = new TemplateAssignment3(xmin, xmax, ymin, ymax, budget, lambda, muLow, muHigh, stopTime, k, K);
        //optimization.runLocalSearch();

        TemplateAssignment3 optimization2 = new TemplateAssignment3(xmin, xmax, ymin, ymax, budget, lambda, muLow, muHigh, stopTime, k, K);
        optimization2.runRankingSelection(initialRuns, alpha);
    }
}