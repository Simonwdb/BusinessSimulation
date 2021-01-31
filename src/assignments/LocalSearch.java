package assignments;

import java.util.HashMap;
import java.util.Random;

import assignments.LocalSearch.State;
import umontreal.ssj.rng.MRG32k3a;
import umontreal.ssj.stat.TallyStore;

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
    }
	
	public HashMap<String,State> outputs;
	public int numAmbulances;
	public int budget;
    Random rng = new Random();

    static final double BIGM = 9999999999999.99;
	
	public LocalSearch(int numAmbulances, int budget) {
		this.numAmbulances = numAmbulances;
		this.budget = budget; // 5000
		createStates();
		rng.setSeed(0);
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
	
	private void createStates() {
		// Takes around two minutes due to the large size of parameter options
		outputs = new HashMap<String,State>();
		int n = numAmbulances + 1; // usually 21
		
		//ooutputs = new State[n][n][n][n][n][n][n];

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
		// TODO Auto-generated method stub
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
}

