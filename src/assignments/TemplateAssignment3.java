package assignments;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Arrays;
import umontreal.ssj.probdist.NormalDist;
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
    	/* 
    	 * From slide 22 of lecture 3 "R&S approach (budget m and alpha between (0,1))"
    	 * 
    	 * |S| = k finite and small <- S = this.k?
    	 * 
    	 * 1) 	simulate every solution n times <- n = initialRuns? 
    	 * 		and calculate averages rn(pi) and variances s^2n(pi)
    	 * 
    	 * 2)	perform pairwise t-test for (pi, pi') to see if pi is significantly
    	 * 		"outperformed" by a pi', discard outperformed solutions
    	 * 
    	 * 3)	Divide remaining simulation budget m - n * |S| over the remaining candidate
    	 * 		solutions I
    	 * 
    	 * note:	alpha_* = 1 - (1 - a) ^ |S| - 1 / 2	
    	 */

    	// own calculations
    	
    	double S = (double) this.k;	// is this correct ?
    	
    	// are we allowed to import the Math class?
    	
    	// calculation from slide 22
    	// alpha_* = 1 - (1 - a) ^ |S| - 1 / 2	
    	double alpha_ = 1 - Math.pow((1- alpha), 1 / (S - 1)); 
    	System.out.println("alpha_ : " + alpha_);
    	
    	// end
    	
    	// simulation every solution
    	for (int i = 0; i < initialRuns; i++) {
    		
    	}
    	
    	
        // perform initial runs

        HashSet<State> I = selectCandidateSolutions(alpha);

        // perform rest of the runs

        State opt = selectOptimalState();

        return opt;
    }

    public HashSet<State> selectCandidateSolutions(double alpha) {
        HashSet<State> I = new HashSet();

        // find all candidate solutions for the ranking and selection method
        return I;
    }

    public State runLocalSearch() {

    	// perform Local Search
    	performLocalSearch();
    	
    	// Choose state with the lowest _r(pi), lowest sample average, based on localSearch
        State opt = selectOptimalState();

        return opt;
    }

    private void performLocalSearch() {
		// Initialisation
    	State currpi = selectRandomStart(); // keeps track of current state, start somewhere random
    	int m = budget; 					// keep track of the simulation budget
    	// NB: sample outputs are already reset in main(), as a new object of TemplateAssignment3 is created
    	// this means we do not have to initialise sample averages / r, for certain states/solutions
    	
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
        MRG32k3a rand1 = getStream();
        MRG32k3a rand2 = getStream();
        int rx = rand1.nextInt(xmin, xmax);
        int ry = rand2.nextInt(ymin, ymax);
        int i = calcPos(rx,ry);
        return i;
	}

	public State selectRandomNeighbor(State state) {
        State neighbor = null; // Temporary: nog niet af dus returnt null. TODO: dit vervangen later, ook voor andere methoden.
        
        // get all neighbors
        List<State> neighbors = getAllNeighbors(state);
        // select a random one from the list

        return neighbor;
    }
    
	private List<State> getAllNeighbors(State state) {
		// TODO Auto-generated method stub
		int x = state.xval;
		int y = state.yval;
		
		// return all neighbors in array
		return null;
	}

	private int getkNeighbor(int x) {
		// TODO Auto-generated method stub
		if (x == xmax)
			return (xmax-1);
		if (x == xmin)
			return (xmin+1);
		else {
			int nx = getNeighborNoChecks(x);
			return nx;
		}
	}

	private int getNeighborNoChecks(int n) {
		// Get +1 or -1, or just the same
		MRG32k3a rand = getStream();
		int zeroorone = rand.
		return 0;
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
        
        // average costs per time unit kan je halen uit de outputs array, door eerst k en K te vertalen
        
        // So:  results[0] = average costs per run with CRN & (k,K)
        // And: results[1] = average costs per run with CRN & (k2, K2)
        // TODO: We should also print these results somewhere, we can do this in main for example, by assigning the results to a variable and printing this.
        
        results[0] = result1;
        results[1] = result2;

        return results;
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
        
        TemplateAssignment3 crn = new TemplateAssignment3(xmin, xmax, ymin, ymax, budget, lambda, muLow, muHigh, stopTime, k, K);
        double results[] = crn.simulateCommonRandomNumbersRun(k2,K2);
        System.out.println(Arrays.toString(results));

        TemplateAssignment3 optimization = new TemplateAssignment3(xmin, xmax, ymin, ymax, budget, lambda, muLow, muHigh, stopTime, k, K);
        optimization.runLocalSearch();

        TemplateAssignment3 optimization2 = new TemplateAssignment3(xmin, xmax, ymin, ymax, budget, lambda, muLow, muHigh, stopTime, k, K);
        optimization2.runRankingSelection(initialRuns, alpha);
    }
}