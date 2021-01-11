package example_assignments;

import java.util.ArrayList;

import umontreal.ssj.charts.HistogramChart;
import umontreal.ssj.randvar.ExponentialGen;
import umontreal.ssj.rng.MRG32k3a;
import umontreal.ssj.simevents.Accumulate;
import umontreal.ssj.simevents.Event;
import umontreal.ssj.simevents.Sim;
import umontreal.ssj.stat.TallyStore;

/**
* This class demonstrates the implementation of a M/M/1 queue using SSJ.
*
* @author  Joost Berkhout
*/
public class SimpleQueue {

    /**
     * Main method of the class to run experiments
     *
     * @param args Unused.
     */
    public static void main(String[] args) {

        // user input
        double maxSimTime = 12*3600; // simulation endtime (time unit is in seconds)
        double lambda = 2; // arrival rate
        double mu = 2.5; // service rate
        int N = 4; // queue length

        // start simulation
        SimpleQueue simpleQueue = new SimpleQueue(maxSimTime, lambda, mu);

        // report on results
        System.out.println(simpleQueue.statisticsSummary());
        HistogramChart histo = new HistogramChart("Histogram waiting times", simpleQueue.waitingTimes.getName(), "Frequency", simpleQueue.waitingTimes);
        histo.view(1000, 500);
    }

    // queue variables
    Customer currentCustomerInService;
    ArrayList<Customer> queue = new ArrayList<Customer>();
    double lambda;
    double mu;

    // RNGs
    ExponentialGen arrivalTimeGen;
    ExponentialGen serviceTimeGen;

    // stats counters
    TallyStore waitingTimes = new TallyStore("Customer waiting times"); // note that you could also use a Tally in this example; TallyStore also stores the individual observations
    TallyStore lostCustomers = new TallyStore("1 is a customer left");
    Accumulate queueLenghts = new Accumulate("Queue lengths"); // initial value at time 0 will be 0 by default (=empty system)

    public SimpleQueue(double maxSimTime, double lambda, double mu) {

        // init
        this.lambda = lambda;
        this.mu = mu;

        // init RNGs
        arrivalTimeGen = new ExponentialGen(new MRG32k3a(), lambda);
        serviceTimeGen = new ExponentialGen(new MRG32k3a(), mu);

        // initialize simulation
        Sim.init();
        new CustomerArrival().schedule(arrivalTimeGen.nextDouble());
        new StopSimulation().schedule(maxSimTime);

        // start simulation
        Sim.start();
    }

    class CustomerArrival extends Event {
        @Override
        public void actions() {
            // new customer arrives

            // generate new arriving customer
            Customer newCustomer = new Customer(Sim.time());

            // check whether new customer goes in service or in queue
            if (currentCustomerInService == null) {
                // no customer in service yet
                currentCustomerInService = newCustomer;
                currentCustomerInService.startService();
            }
            else {
            	if (queue.size() < N) {
            		lostCustomers.add(0.0);
            		queue.add(newCustomer);
            		queueLengths.update(queue.size());
            	}
            	
            	/*
            	// check if N < 4, to add the customer to the queue
                // customer enters the queue
            	if (queue.size() < 4) {
                    queue.add(newCustomer);
                    queueLenghts.update(queue.size()); // store statistic
            	}
            	
            	/*
                // customer enters the queue
                queue.add(newCustomer);
                queueLenghts.update(queue.size()); // store statistic
                */
            }

            // schedule a new arriving customer
            new CustomerArrival().schedule(arrivalTimeGen.nextDouble());
        }
    }

    public class Customer extends Event {

        double arrivalTime;
        double serviceTime;
        double startTimeService;
        double departureTime;
        double totalTimeSpend;
        double waitingTime;

        public Customer(double arrivalTime) {
            this.arrivalTime = arrivalTime;
        }

        // call this method when a customer starts its service
        void startService() {

            startTimeService = Sim.time();
            waitingTime = startTimeService - arrivalTime;
            serviceTime = serviceTimeGen.nextDouble();
            schedule(serviceTime); // schedule customer service completion

            // store statistic
            waitingTimes.add(waitingTime);
        }

        // event: customer service completion
        @Override
        public void actions() {

            // save statistics
            departureTime = Sim.time();
            totalTimeSpend = departureTime - arrivalTime;

            // remove customer as current customer
            currentCustomerInService = null;

            // look whether a customer from the queue can start service
            if (queue.size() > 0) {
                currentCustomerInService = queue.remove(0);
                currentCustomerInService.startService();
                queueLenghts.update(queue.size()); // store statistic
            }
        }
    }

    class StopSimulation extends Event {
        @Override
        public void actions() {
            Sim.stop();
        }
    }

    public String statisticsSummary() {

        // calculate some theoretical M/M/1 results
        double rho = lambda/mu;
        double theoryExpWaitingTime = rho/(mu-lambda);
        double theoryExpQueueLength = rho*rho/(1-rho);
        String theoryResults =
                "For comparison, it follows from theory that in steady state " +
                "(note that in this simulation the samples are not i.i.d.):" +
                " \nThe expected waiting is " + theoryExpWaitingTime + "\nThe expected queue length is " + theoryExpQueueLength;

        return theoryResults + "\n" + "\n" + waitingTimes.report() + queueLenghts.report() + lostCustomers.report() ;
    }
}
