package examples;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import OldSolvers.BBMarginalSearch;
import OldSolvers.BBSearch;
import OldSolvers.BBSigmaSearch;
import OldSolvers.GameSolverCuts;
import OldSolvers.HeuristicSolver;
import OldSolvers.LinearGameSolver;
import OldSolvers.MarginalSolver;
import OldSolvers.PureStrategySolver;
import Utilities.DeceptionGameHelper;
import models.DeceptionGame;
import models.ObservableConfiguration;
import models.Systems;
import solvers.Bisection;
import solvers.BisectionMILP;
import solvers.GameSolver;
import solvers.GreedyMaxMinSolver;
import solvers.NaiveSolver;
import solvers.UpperBoundMILP;

public class SimpleDeceptionAllSolvers {

	private static double maxUtil;

	public static void main(String[] args) throws Exception {

		// g.generateGame(3, 2, 3);
		int numConfigs = 1000;
		int numObs = 200;
		int numSystems = 200;
		// seed = 101 has some issues in returning the right strategy for
		// numSystems = 5, numObs = 2, and numConfigs = 5
		// seed = 103 creates a perfect case for when heuristic doesn't run
		// well, numS = 4, numO = 3, numC = 3
		// seed = 113 creates issues for objective MILP value, numS = 20, numO =
		// 10, numC = 20

		// whenever all machines can be assigned to same observable, heuristic
		// doesn't work, otherwise it seems to work

		// seed = 104, seed = 109, both give possible cases where greedy could
		// give less than 1/2 approximation
		long seed = 100;
		for (int i = 1; i <= 10; i++) {
			DeceptionGame g = new DeceptionGame();
			seed++;
			g.generateGame(numConfigs, numObs, numSystems, seed);
//			 g.printGame();

			System.out.println();
			System.out.println();

			// g.exportGame(1, 1);

			String dir = "C:/Users/Aaron Schlenker/workspace/CyberDeception/";

			// DeceptionGame game2 = new DeceptionGame();
			// game2.readInGame(dir, 1, 1);

//			 game2.printGame();

			// runSampleLinearGame(g, numConfigs, numObs, numSystems, seed);

			runSampleGame(g, numConfigs, numObs, numSystems, seed);

			// System.out.println();
			// System.out.println();
			// System.out.println();

//			 runSampleGameCuts(g, numConfigs, numObs, numSystems, seed);

			// runUniformEstimation(g, numConfigs, numObs, numSystems, seed);

			// runUniformEstimationCost(g, numConfigs, numObs, numSystems,
			// seed);

			// runRandomizedRounding(g, numConfigs, numObs, numSystems, seed);

			//Needs a lot of work, develop a more systematic algorithm
//			runRRInteger(g, numConfigs, numObs, numSystems, seed);

			// This might work, but the code is not correct right now!
			// I think the issue is with solving for the MILP w bounds whenever
			// we have a marginal w/ all N_o that are integers
			// runBBMarginalSearch(g, numConfigs, numObs, numSystems, seed);

			// runMarginalSolver(g, numConfigs, numObs, numSystems, seed);
			// runBisectionAlgorithm(g, numConfigs, numObs, numSystems, seed);

			runBisectionAlgorithmMILP(g, numConfigs, numObs, numSystems, seed);

			// runBBSearch(g, numConfigs, numObs, numSystems, seed);
			// There is an issue with returning a defender strategy when the
			// optimal utility is equal to the lower bound!
			// runGreedyMaxMinSolver(g);

			// runGreedyMaxMinSolver(g, 1000);
			// runGreedyMaxMinSolver(g, 1, true, 10);
			// runGreedyMaxMinSolver(g, 1, false);

			// runGreedyMaxMinSolverAllPermutations(g);

			// runBBSigmaSearch(g, numConfigs, numObs, numSystems, seed);

			System.out.println();
			System.out.println();
			// runHeuristicSolver(g);

			// Should write a greedymaxmin solver that works with a general set
			// of constraints
			// Also should incorporate a partial strategy then greedy max min
			// assigning the rest
		}

	}

	private static void runRandomizedRounding(DeceptionGame g, int numConfigs, int numObservables, int numSystems,
			long seed) throws Exception {
		System.out.println();
		System.out.println("Running Randomized Rounding");
		System.out.println();
		// Need to load cplex libraries
		String cplexInputFile = "CplexConfig";

		DeceptionGameHelper.loadLibrariesCplex(cplexInputFile);

		Map<ObservableConfiguration, Integer> bounds = new HashMap<ObservableConfiguration, Integer>();

		Bisection alg = new Bisection(g);

		alg.solve();

		System.out.println("Utility: " + alg.getLB());

		// Need to take marginal from the Bisection Algorithm and randomly round
		// it to integers, or some intelligent rounding
		Map<Systems, Map<ObservableConfiguration, Double>> defenderStrategy = alg.getDefenderStrategy();

		double gap = 1;
		Systems sys = null;
		ObservableConfiguration obs = null;

		// Random version
		double utilBest = -1000;
		int numRuns = 50;
		Random r = new Random();
		for (int i = 0; i < numRuns; i++) {
			Map<Systems, Map<ObservableConfiguration, Double>> defenderStrategyTemp = new HashMap<Systems, Map<ObservableConfiguration, Double>>();
			for (Systems k : g.machines) {
				defenderStrategyTemp.put(k, new HashMap<ObservableConfiguration, Double>());
				for (ObservableConfiguration o : g.obs) {
					defenderStrategyTemp.get(k).put(o, defenderStrategy.get(k).get(o));
				}
			}

			while (!checkIfStrategyPure(defenderStrategyTemp)) {
				// Find n_k,tf that is closest to 1
				Map<Systems, Map<ObservableConfiguration, Double>> strategyGaps = new HashMap<Systems, Map<ObservableConfiguration, Double>>();
				for (Systems k : g.machines) {
					for (ObservableConfiguration o : g.obs) {
						if (defenderStrategy.get(k).get(o) >= 1.0) // only round
																	// non-integer
																	// values
							continue;

						if (defenderStrategy.get(k).get(o) > 0.0 && defenderStrategy.get(k).get(o) < 1) {
							if (strategyGaps.get(k) == null)
								strategyGaps.put(k, new HashMap<ObservableConfiguration, Double>());

							strategyGaps.get(k).put(o, defenderStrategy.get(k).get(o));
						}

					}
				}

				// Randomly pick system and randomly pick observable to fix
				int sysnum = r.nextInt(strategyGaps.keySet().size()) + 1;
				int index = 1;
				for (Systems k : strategyGaps.keySet()) {

					if (sysnum == index) {
						sys = k;
						int obsnum = r.nextInt(strategyGaps.get(k).keySet().size()) + 1;
						int index2 = 1;

						// Randomly pick Observable to set
						for (ObservableConfiguration o : strategyGaps.get(k).keySet()) {
							if (obsnum == index2) {
								obs = o;
								break;
							} else
								index2++;
						}
						break;
					} else
						index++;

				}

				// System.out.println();
				// printStrategy(alg.getDefenderStrategy());
				// System.out.println();
				// System.out.println("Changing K"+sys.id+" O"+obs.id);
				// System.out.println();

				// Set variable equal to 1, and all others equal to zero
				for (ObservableConfiguration o : g.obs) {
					if (o.id != obs.id)
						defenderStrategyTemp.get(sys).put(o, 0.0);
					else
						defenderStrategyTemp.get(sys).put(o, 1.0);
				}

				// printStrategy(alg.getDefenderStrategy());
			}

			// System.out.println("Utility Rounding:
			// "+GameSolver.calculateExpectedUtility1(g, defenderStrategyTemp));
			if (GameSolver.calculateExpectedUtility1(g, defenderStrategyTemp) > utilBest)
				utilBest = GameSolver.calculateExpectedUtility1(g, defenderStrategyTemp);

			// System.out.println();

			// printStrategy(alg.getDefenderStrategy());
			// printCompactObsStrategy(defenderStrategyTemp, g);
		}

		System.out.println();
		System.out.println("Best Util: " + utilBest);
		System.out.println();

		System.out.println(numConfigs + ", " + numObservables + ", " + numSystems + ", " + alg.getLB() + ", "
				+ alg.getUB() + ", " + alg.getRuntime() + ", " + alg.getIterations());

	}

	private static void runRRInteger(DeceptionGame g, int numConfigs, int numObservables, int numSystems, long seed)
			throws Exception {
		System.out.println();
		System.out.println("Running Randomized Rounding w/ Integers");
		System.out.println();
		// Need to load cplex libraries
		String cplexInputFile = "CplexConfig";

		DeceptionGameHelper.loadLibrariesCplex(cplexInputFile);

		Map<Systems, ObservableConfiguration> setMaskings = new HashMap<Systems, ObservableConfiguration>();
		
		// Gets a marginal strategy
		// BisectionAlgorithm alg = new BisectionAlgorithm(g);
		//
		// alg.solve();

		// System.out.println("Utility: "+alg.getLB());

		// Need to take marginal from the Bisection Algorithm and randomly round
		// it to integers, or some intelligent rounding
		// Map<Systems, Map<ObservableConfiguration, Double>> defenderStrategy =
		// alg.getDefenderStrategy();

		// Loop to find next OC to set for a given system, each iteration should
		// fix one machine to have a particular configuration
		Map<Systems, Map<ObservableConfiguration, Double>> defenderStrategy = null;
		Systems sys = null;
		ObservableConfiguration obs = null;
		Systems sysOld = null;
		ObservableConfiguration obsOld = null;

		double utilBest = -1000;

		Random r = new Random();

		int index1 = 0;
		while (true) {
			sys = null;
			obs = null;

			// Gets a marginal strategy
			Bisection alg = new Bisection(g, setMaskings);

			alg.solve();

//			System.out.println("Utility: " + alg.getLB());

			// Need to take marginal from the Bisection Algorithm and randomly
			// round it to integers, or some intelligent rounding
			defenderStrategy = alg.getDefenderStrategy();

			//If strategy is pure we are done
			if (checkIfStrategyPure(defenderStrategy)) {
				System.out.println("Breaking the loop. Pure Strategy Found!");
				break;
			}

			// Find n_k,tf that is closest to 1
			Map<Systems, Map<ObservableConfiguration, Double>> strategyGaps = new HashMap<Systems, Map<ObservableConfiguration, Double>>();
			for (Systems k : g.machines) {
				for (ObservableConfiguration o : g.obs) {
					if (defenderStrategy.get(k).get(o) >= 1.0){ // only round non-integer values
//						if(!setMaskings.containsKey(k))
//							setMaskings.put(k, o);
						
						continue;
					}

					if (defenderStrategy.get(k).get(o) > 0.0 && defenderStrategy.get(k).get(o) < 1) {
						if (strategyGaps.get(k) == null)
							strategyGaps.put(k, new HashMap<ObservableConfiguration, Double>());

						strategyGaps.get(k).put(o, defenderStrategy.get(k).get(o));
					}

				}
			}

			// Randomly pick system and randomly pick observable to fix
			//Need to ensure the pick satisfies the budget constraint
			int sysnum = r.nextInt(strategyGaps.keySet().size()) + 1;
			int index = 1;
			
			for (Systems k : strategyGaps.keySet()) {
				if (sysnum == index) {
					sys = k;
					boolean assigned = false;
					while(!assigned){
						int obsnum = r.nextInt(strategyGaps.get(k).keySet().size()) + 1;
						int index2 = 1;
	
						// Randomly pick Observable to set
						for (ObservableConfiguration o : strategyGaps.get(k).keySet()) {
							if (obsnum == index2) {
								if(satisfyBudget(g, defenderStrategy, sys, o)){
									assigned = true;
									obs = o;
									break;
								}
							} else
								index2++;
						}
					}

					break;
				} else
					index++;

			}

			setMaskings.put(sys, obs);

//			System.out.println();
//			printStrategy(alg.getDefenderStrategy());
//			System.out.println();
//			System.out.println("Changing K" + sys.id + " O" + obs.id);
//			System.out.println();
//
//			System.out.println(strategyConstants);

			// System.out.println("Utility Rounding:
			// "+GameSolver.calculateExpectedUtility1(g, defenderStrategyTemp));
			// if(GameSolver.calculateExpectedUtility1(g, defenderStrategy) >
			// utilBest)
			// utilBest = GameSolver.calculateExpectedUtility1(g,
			// defenderStrategy);

			// System.out.println();

			// printStrategy(alg.getDefenderStrategy());
			// printCompactObsStrategy(defenderStrategyTemp, g);
		}

		if (GameSolver.calculateExpectedUtility1(g, defenderStrategy) > utilBest)
			utilBest = GameSolver.calculateExpectedUtility1(g, defenderStrategy);

		System.out.println();
		System.out.println("Best Util: " + utilBest);
		satisfyBudget(g, defenderStrategy);
		System.out.println();

		System.out.println(numConfigs + ", " + numObservables + ", " + numSystems);
		// +", "+alg.getLB()+", "+alg.getUB()+", "+alg.getRuntime()+", "+alg.getIterations());


		System.out.println(checkStrategy(g, defenderStrategy));
		
		 printStrategy(defenderStrategy);
		// printCompactObsStrategy(defenderStrategyTemp, g);
		
	}
	
	private static boolean checkStrategy(DeceptionGame g, Map<Systems, Map<ObservableConfiguration, Double>> strategy){
		//Check assignment constraints
		for(Systems k : strategy.keySet()){
			for(ObservableConfiguration o : strategy.get(k).keySet()){
				if(strategy.get(k).get(o) > 0.0){
					if(!o.configs.contains(k.f))
						return false;
				}	
			}
		}
		
		return satisfyBudget(g, strategy);
	}
	
	private static boolean satisfyBudget(DeceptionGame g, Map<Systems, Map<ObservableConfiguration, Double>> strategy){
		double budget = g.Budget;
		
		double cost = 0;
		//calculate budget
		for(Systems k : strategy.keySet()){
			for(ObservableConfiguration o : strategy.get(k).keySet()){
				cost += strategy.get(k).get(o)*g.costFunction.get(k.f).get(o);
			}
			
		}
		
		System.out.println("Cost: "+cost+"  Budget: "+budget);
		
		if(cost <= budget)
			return true;
		else
			return false;
		
	}
	
	private static boolean satisfyBudgetInt(DeceptionGame g, Map<Systems, Map<ObservableConfiguration, Integer>> strategy){
		double budget = g.Budget;
		
		double cost = 0;
		//calculate budget
		for(Systems k : strategy.keySet()){
			for(ObservableConfiguration o : strategy.get(k).keySet()){
				cost += strategy.get(k).get(o)*g.costFunction.get(k.f).get(o);
			}
			
		}
		
		System.out.println("Cost: "+cost+"  Budget: "+budget);
		
		if(cost <= budget)
			return true;
		else
			return false;
		
	}
	
	private static boolean satisfyBudget(DeceptionGame g, Map<Systems, Map<ObservableConfiguration, Double>> strategy, Systems sys, ObservableConfiguration obs){
		double Budget = g.Budget;
		
		Map<ObservableConfiguration, Double> temps = new HashMap<ObservableConfiguration, Double>();
		
		for(ObservableConfiguration o : strategy.get(sys).keySet()){
			temps.put(o, strategy.get(sys).get(o));
			strategy.get(sys).put(o, 0.0);
		}
		
		strategy.get(sys).put(obs, 1.0);
		
		double cost = 0;
		//calculate budget
		for(Systems k : strategy.keySet()){
			for(ObservableConfiguration o : strategy.get(k).keySet()){
				cost += strategy.get(k).get(o)*g.costFunction.get(k.f).get(o);
			}
			
		}
		
		for(ObservableConfiguration o : strategy.get(sys).keySet()){
			strategy.get(sys).put(o, temps.get(o));
		}
		
		if(cost <= Budget)
			return true;
		else
			return false;
		
	}

	/*
	 * Expects strategies that should be in-between [0,1] for all entries
	 */
	private static boolean checkIfStrategyPure(Map<Systems, Map<ObservableConfiguration, Double>> defenderStrategy) {
		for (Systems k : defenderStrategy.keySet()) {
			for (ObservableConfiguration o : defenderStrategy.get(k).keySet()) {
				if (defenderStrategy.get(k).get(o) >= .00001 && defenderStrategy.get(k).get(o) <= .99999)
					return false;
			}
		}
		return true;
	}

	private static void runUniformEstimationCost(DeceptionGame g, int numConfigs, int numObservables, int numSystems,
			long seed) throws Exception {
		NaiveSolver solver = new NaiveSolver(g, true);

		solver.solve();

		System.out.println("UE: " + GameSolver.calculateExpectedUtility(g, solver.getStrategy()) + " "
				+ solver.getDefenderUtility());

		// if(GameSolver.calculateExpectedUtility(g, solver.getStrategy()) >
		// maxUtil){
		// System.out.println();
		// System.out.println();
		// System.out.println();
		// System.out.println();
		// System.out.println("BUG IN THE CODE");
		// System.out.println();
		// System.out.println();
		// System.out.println();
		// System.out.println();
		// }

		printCompactStrategy(solver.getStrategy(), g);
		//
		printStrategy2(solver.getStrategy());

	}

	private static void runUniformEstimation(DeceptionGame g, int numConfigs, int numObservables, int numSystems,
			long seed) throws Exception {
		NaiveSolver solver = new NaiveSolver(g);

		solver.solve();

		System.out.println("UE: " + GameSolver.calculateExpectedUtility(g, solver.getStrategy()) + " "
				+ solver.getDefenderUtility());

		printCompactStrategy(solver.getStrategy(), g);
		//
		printStrategy2(solver.getStrategy());

	}

	private static void runBBMarginalSearch(DeceptionGame g, int numConfigs, int numObservables, int numSystems,
			long seed) throws Exception {

		System.out.println("Running BB Marginal Search");
		System.out.println();
		// Need to load cplex libraries
		String cplexInputFile = "CplexConfig";

		DeceptionGameHelper.loadLibrariesCplex(cplexInputFile);

		BBMarginalSearch search = new BBMarginalSearch(g);

		search.solve();

		printCompactStrategy2(search.getDefenderStrategy(), g);

		// printCompactStrategy(search.getDefenderStrategy());

		System.out.println(numConfigs + ", " + numObservables + ", " + numSystems + ", " + search.getPayoff() + ", "
				+ search.getRuntime());

		System.out.println();
	}

	private static void runBBSigmaSearch(DeceptionGame g, int numConfigs, int numObservables, int numSystems, long seed)
			throws Exception {

		System.out.println("Running BB Sigma Search");
		System.out.println();
		// Need to load cplex libraries
		String cplexInputFile = "CplexConfig";

		DeceptionGameHelper.loadLibrariesCplex(cplexInputFile);

		BBSigmaSearch search = new BBSigmaSearch(g);

		search.solve();

		printCompactStrategy2(search.getDefenderStrategy(), g);

		// printCompactStrategy(search.getDefenderStrategy());

		System.out.println(numConfigs + ", " + numObservables + ", " + numSystems + ", " + search.getGlobalLB() + ", "
				+ search.getRuntime());

		System.out.println();
	}

	private static void runBBSearch(DeceptionGame g, int numConfigs, int numObservables, int numSystems, long seed)
			throws Exception {

		System.out.println("Running BB Search");
		System.out.println();
		// Need to load cplex libraries
		String cplexInputFile = "CplexConfig";

		DeceptionGameHelper.loadLibrariesCplex(cplexInputFile);

		BBSearch search = new BBSearch(g);

		search.solve();

		printCompactStrategy(search.getDefenderStrategy());

		System.out.println(numConfigs + ", " + numObservables + ", " + numSystems + ", " + search.getGlobalLB() + ", "
				+ search.getRuntime());

		System.out.println();
	}

	private static void runBisectionAlgorithmMILP(DeceptionGame g, int numConfigs, int numObservables, int numSystems,
			long seed) throws Exception {

		System.out.println();
		System.out.println("Running Bisection Algorithm MILP");
		System.out.println();
		// Need to load cplex libraries
		String cplexInputFile = "CplexConfig";

		DeceptionGameHelper.loadLibrariesCplex(cplexInputFile);

		// System.out.println(bounds.toString());
		// System.out.println();

		BisectionMILP alg = new BisectionMILP(g);

		alg.setMaxRuntime(600);
		
		alg.setEpsilon(0.5);
		
		alg.setGapTolerance(1);

		alg.solve();

		// printStrategy(alg.getDefenderStrategy());
		// printCompactObsStrategy(alg.getDefenderStrategy(), g);

		System.out.println(numConfigs + ", " + numObservables + ", " + numSystems + ", " + alg.getLB() + ", "
				+ alg.getUB() + ", " + alg.getRuntime() + ", " + alg.getIterations());

	}

	private static void runBisectionAlgorithm(DeceptionGame g, int numConfigs, int numObservables, int numSystems,
			long seed) throws Exception {

		System.out.println();
		System.out.println("Running Bisection Algorithm");
		System.out.println();
		// Need to load cplex libraries
		String cplexInputFile = "CplexConfig";

		DeceptionGameHelper.loadLibrariesCplex(cplexInputFile);

		// Map<ObservableConfiguration, Integer> bounds = new
		// HashMap<ObservableConfiguration, Integer>();

		for (ObservableConfiguration o : g.obs) {
			// if(o.id == 2)
			// bounds.put(o, 8);
			// else if(o.id == 7)
			// bounds.put(o, 6);
			/*
			 * else if(o.id == 9) bounds.put(o, 5); else bounds.put(o, 0);
			 */
		}

		// System.out.println(bounds.toString());
		// System.out.println();

		Bisection alg = new Bisection(g);

		alg.solve();

		// printStrategy(alg.getDefenderStrategy());
		printCompactObsStrategy(alg.getDefenderStrategy(), g);

		System.out.println(numConfigs + ", " + numObservables + ", " + numSystems + ", " + alg.getLB() + ", "
				+ alg.getUB() + ", " + alg.getRuntime() + ", " + alg.getIterations());

	}

	public static void runGreedyMaxMinSolver(DeceptionGame g) {
		System.out.println("Runnning Greedy Max Min Solver");

		double highUtil = -100;

		for (int i = 1; i <= 1; i++) {
			GreedyMaxMinSolver solver = new GreedyMaxMinSolver(g);

			solver.setShuffle(false);

			solver.setDescending(true);

			solver.solve();

			if (solver.getDefenderUtility() > highUtil)
				highUtil = solver.getDefenderUtility();

			// System.out.println();
			////
			// System.out.println(g.configs.size()+", "+g.obs.size()+",
			// "+g.machines.size()+", "+solver.getDefenderUtility()+","+
			// solver.calculateMaxMinUtility(solver.getGreedyStrategy()).eu+",
			// "+solver.getRuntime());
			// System.out.println();
			//
			/*
			 * if(solver.getDefenderUtility() < maxUtil*2){
			 * System.out.println("We have a problem!");
			 * System.out.println("Greedy: "+solver.getDefenderUtility()); }
			 */

			highUtil = solver.getDefenderUtility();

			// printCompactStrategy(solver.getGreedyStrategy(), g);

			// printStrategy2(solver.getGreedyStrategy());

			GreedyMaxMinSolver solver1 = new GreedyMaxMinSolver(g);

			solver1.setShuffle(false);

			solver1.setDescending(false);

			solver1.solve();

			if (solver1.getDefenderUtility() > highUtil)
				highUtil = solver1.getDefenderUtility();

			if (solver1.getDefenderUtility() < maxUtil * 2) {
				System.out.println("We have a problem!");
				System.out.println("Greedy: " + solver.getDefenderUtility());
			}

			if (solver1.getDefenderUtility() > highUtil)
				highUtil = solver1.getDefenderUtility();
			// printCompactStrategy(solver.getGreedyStrategy(), g);

			// printStrategy2(solver1.getGreedyStrategy());
		}

		System.out.println();
		//
		System.out.println(g.configs.size() + ", " + g.obs.size() + ", " + g.machines.size() + ", " + highUtil);
		System.out.println();

		// System.out.println("Util: "+highUtil);

	}

	public static void runGreedyMaxMinSolver(DeceptionGame g, int numShuffles, int maxRuntime) {
		System.out.println("Runnning Greedy Max Min Solver");

		double highUtil = -100;

		// System.out.println(g.costFunction.toString());
		double start = System.currentTimeMillis();
		double runtime = 0;

		for (int i = 1; i <= numShuffles; i++) {
			GreedyMaxMinSolver solver = new GreedyMaxMinSolver(g);

			// solver.setDescending(true);
			solver.setShuffle(true);
			solver.setRandomIndifferent();

			// System.out.println("Budget: "+g.Budget);
			// solver.solve();

			solver.solveHardGMM();

			// solver.solveSoftGMM();

			if (solver.getDefenderUtility() > highUtil)
				highUtil = solver.getDefenderUtility();

			// System.out.println();
			//
			// System.out.println(g.configs.size()+", "+g.obs.size()+",
			// "+g.machines.size()+", "+solver.getDefenderUtility()+","+
			// solver.calculateMaxMinUtility(solver.getGreedyStrategy()).eu+",
			// "+solver.getRuntime());
			// System.out.println();

			// if(solver.getDefenderUtility() < maxUtil*2){
			// System.out.println("We have a problem!");
			// System.out.println("Greedy: "+solver.getDefenderUtility());
			// }

			// printCompactStrategy(solver.getGreedyStrategy(), g);

			// printStrategy2(solver.getGreedyStrategy());

			runtime = (System.currentTimeMillis() - start) / 1000.0;

			if (runtime > maxRuntime)
				break;
		}

		System.out.println("Util: " + highUtil);

	}

	public static void runGreedyMaxMinSolver(DeceptionGame g, int numShuffles, boolean fixed, int maxRuntime) {
		System.out.println("Runnning Greedy Max Min Solver");

		double highUtil = -100;

		// System.out.println(g.costFunction.toString());
		double start = System.currentTimeMillis();
		double runtime = 0;

		for (int i = 1; i <= numShuffles; i++) {
			GreedyMaxMinSolver solver = new GreedyMaxMinSolver(g);

			// solver.setDescending(true);
			solver.setShuffle(true);
			solver.setRandomIndifferent();

			// System.out.println("Budget: "+g.Budget);
			// solver.solve();

			if (fixed)
				solver.solveHardGMM();
			else
				solver.solveSoftGMM(10);

			if (solver.getDefenderUtility() > highUtil)
				highUtil = solver.getDefenderUtility();

			// System.out.println();
			//
			// System.out.println(g.configs.size()+", "+g.obs.size()+",
			// "+g.machines.size()+", "+solver.getDefenderUtility()+","+
			// solver.calculateMaxMinUtility(solver.getGreedyStrategy()).eu+",
			// "+solver.getRuntime());
			// System.out.println();

			// if(solver.getDefenderUtility() < maxUtil*2){
			// System.out.println("We have a problem!");
			// System.out.println("Greedy: "+solver.getDefenderUtility());
			// }

			// printCompactStrategy(solver.getGreedyStrategy(), g);

			// printStrategy2(solver.getGreedyStrategy());

			runtime = (System.currentTimeMillis() - start) / 1000.0;

			if (runtime > maxRuntime)
				break;
		}

		System.out.println("Util: " + highUtil);

	}

	public static void runGreedyMaxMinSolverAllPermutations(DeceptionGame g) {
		System.out.println("Runnning Greedy Max Min Solver All Permutations");

		double highUtil = -100;

		ArrayList<Systems> systems1 = new ArrayList<Systems>();
		for (Systems k : g.machines)
			systems1.add(k);

		ArrayList<ArrayList<Systems>> allPermutations = generatePerm(systems1);

		// System.out.println();
		// for(ArrayList<Systems> a1 : allPermutations){
		// System.out.println(a1.toString());
		// }
		// System.out.println();

		for (int i = 0; i < allPermutations.size(); i++) {
			GreedyMaxMinSolver solver = new GreedyMaxMinSolver(g);

			solver.setFixedOrdering(allPermutations.get(i));
			// solver.setShuffle(true);
			solver.setRandomIndifferent();

			solver.solve();

			if (solver.getDefenderUtility() > highUtil)
				highUtil = solver.getDefenderUtility();

			// System.out.println();
			//
			// System.out.println(g.configs.size()+", "+g.obs.size()+",
			// "+g.machines.size()+", "+solver.getDefenderUtility()+","+
			// solver.calculateMaxMinUtility(solver.getGreedyStrategy()).eu+",
			// "+solver.getRuntime());
			// System.out.println();

			/*
			 * if(solver.getDefenderUtility() < maxUtil*2){
			 * System.out.println("We have a problem!");
			 * System.out.println("Greedy: "+solver.getDefenderUtility()); }
			 */

			// printCompactStrategy(solver.getGreedyStrategy(), g);

			// printStrategy2(solver.getGreedyStrategy());
		}

		System.out.println("Util: " + highUtil);

	}

	public static <E> ArrayList<ArrayList<Systems>> generatePerm(ArrayList<Systems> original) {
		if (original.size() == 0) {
			ArrayList<ArrayList<Systems>> result = new ArrayList<ArrayList<Systems>>();
			result.add(new ArrayList<Systems>());
			return result;
		}
		Systems firstElement = original.remove(0);
		ArrayList<ArrayList<Systems>> returnValue = new ArrayList<ArrayList<Systems>>();
		ArrayList<ArrayList<Systems>> permutations = generatePerm(original);
		for (ArrayList<Systems> smallerPermutated : permutations) {
			for (int index = 0; index <= smallerPermutated.size(); index++) {
				ArrayList<Systems> temp = new ArrayList<Systems>(smallerPermutated);
				temp.add(index, firstElement);
				returnValue.add(temp);
			}
		}
		return returnValue;
	}

	public static void runHeuristicSolver(DeceptionGame g) {

		System.out.println("Running Heuristic Solver");

		HeuristicSolver solver = new HeuristicSolver(g);

		solver.solve();

	}

	public static void runMarginalSolver(DeceptionGame g, int numConfigs, int numObservables, int numSystems, long seed)
			throws Exception {
		boolean verbose = false;

		// Need to load cplex libraries
		String cplexInputFile = "CplexConfig";

		DeceptionGameHelper.loadLibrariesCplex(cplexInputFile);

		// Need to create bounds!
		// Solve the MILP
		// GameSolver solver1 = new GameSolver(g);

		// solver1.solve();

		double tUB = calculateUB(g);

		// System.out.println(numConfigs+", "+numObservables+", "+numSystems+",
		// "+solver1.getUtility()+", "+solver1.getRuntime()+", "+tUB);

		// Map<ObservableConfiguration, Integer> bounds = getBounds(g,
		// solver1.getDefenderStrategy());

		Map<ObservableConfiguration, Integer> bounds = new HashMap<ObservableConfiguration, Integer>();

		for (ObservableConfiguration o : g.obs) {
			if (o.id == 6)
				bounds.put(o, 5);
			else if (o.id == 7)
				bounds.put(o, 10);
			else if (o.id == 9)
				bounds.put(o, 5);
			else
				bounds.put(o, 0);
		}

		// Solve for optimal pure, given bounds
		// PureStrategySolver solver2 = new PureStrategySolver(g, bounds);
		GameSolver solver2 = new GameSolver(g, bounds);

		solver2.solve();

		System.out.println(numConfigs + ", " + numObservables + ", " + numSystems + ", " + solver2.getUtility() + ", "
				+ solver2.getRuntime() + ", " + tUB);

		// Solve for the marginal, given bounds
		// GameSolver solver = new GameSolver(g);
		MarginalSolver solver = new MarginalSolver(g, bounds);

		solver.solve();

		System.out.println(numConfigs + ", " + numObservables + ", " + numSystems + ", " + solver.getUtility() + ", "
				+ solver.getRuntime() + ", " + tUB);

		// GameSolver solver = new GameSolver(g);
		// UpperBoundMILP solver3 = new UpperBoundMILP(g);

		// solver3.solve();

		// System.out.println(numConfigs+", "+numObservables+", "+numSystems+",
		// "+solver3.getUtility()+", "+solver3.getRuntime()+", "+tUB);

	}

	public static void runSampleLinearGame(DeceptionGame g, int numConfigs, int numObservables, int numSystems,
			long seed) throws Exception {
		boolean verbose = false;

		// Need to load cplex libraries
		String cplexInputFile = "CplexConfig";

		DeceptionGameHelper.loadLibrariesCplex(cplexInputFile);

		// Solve the MILP
		// GameSolver solver = new GameSolver(g);
		LinearGameSolver solver = new LinearGameSolver(g);

		solver.solve();

		// String output =
		// "experiments/CDG_"+numConfigs+"_"+numObservables+"_"+numSystems+".csv";

		// PrintWriter w = new PrintWriter(new BufferedWriter(new
		// FileWriter(output, true)));

		double tUB = calculateUB(g);

		System.out.println(numConfigs + ", " + numObservables + ", " + numSystems + ", " + solver.getUtility() + ", "
				+ solver.getRuntime() + ", " + tUB);

		// w.println(numConfigs+", "+numObservables+", "+numSystems+",
		// "+solver.getUtility()+", "+solver.getRuntime());

		// w.close();
	}

	public static void runSampleGameCuts(DeceptionGame g, int numConfigs, int numObservables, int numSystems, long seed)
			throws Exception {
		double highUtil = -100;

		// g.printGame();
		// g.setRandomBudget();

		// for(int i=1; i<=1000; i++){
		// GreedyMaxMinSolver solver = new GreedyMaxMinSolver(g);
		//
		// solver.setShuffle(true);
		//
		// solver.solveHardGMM();
		//// solver.solve();
		//
		// if(solver.getDefenderUtility() > highUtil)
		// highUtil = solver.getDefenderUtility();
		//
		//
		// }
		// System.out.println("Highest Utility: "+highUtil);

		// Solve the MILP
		GameSolver warm = new GameSolver(g);

		warm.setCosts(true);

		warm.setMilpGap(.05);

		warm.solve();

		highUtil = warm.getDefenderPayoff();

		System.out.println("Running MILP w Cuts");
		System.out.println();

		boolean verbose = false;

		// Need to load cplex libraries
		String cplexInputFile = "CplexConfig";

		DeceptionGameHelper.loadLibrariesCplex(cplexInputFile);

		// g.setRandomBudget();

		// System.out.println("Budget: "+g.Budget);

		for (int i = 1; i <= 1; i++) {
			// Solve the MILP
			GameSolverCuts solver = new GameSolverCuts(g);

			solver.setGlobalLB(highUtil);
			// solver.setGlobalLB(maxUtil);

			solver.setCosts(true);

			solver.setMaxSubsetSize(i);

			solver.solve();

			double tUB = calculateUB(g);

			maxUtil = solver.getUtility();

			System.out.println(numConfigs + ", " + numObservables + ", " + numSystems + ", " + solver.getUtility()
					+ ", " + solver.getRuntime() + ", " + tUB);

			// printCompactStrategy(solver.getDefenderStrategy(), g);

			// printStrategy2(solver.getDefenderStrategy());

			solver.deleteVars();

			// System.out.println();
			System.out.println();
		}
	}

	public static void runSampleGame(DeceptionGame g, int numConfigs, int numObservables, int numSystems, long seed)
			throws Exception {
		System.out.println("Running MILP");
		System.out.println();

		boolean verbose = false;

		// Need to load cplex libraries
		String cplexInputFile = "CplexConfig";

		// g.setRandomBudget();

		DeceptionGameHelper.loadLibrariesCplex(cplexInputFile);

//		System.out.println("Budget: " + g.Budget);

		// Solve the MILP
		GameSolver solver = new GameSolver(g);

		solver.setCosts(true);

		solver.setMaxRuntime(120);
		
		solver.setMilpGap(0.05);

		solver.solve();

		maxUtil = solver.getDefenderPayoff();

		// String output =
		// "experiments/CDG_"+numConfigs+"_"+numObservables+"_"+numSystems+".csv";

		// PrintWriter w = new PrintWriter(new BufferedWriter(new
		// FileWriter(output, true)));

		double tUB = calculateUB(g);

		maxUtil = solver.getUtility();

		System.out.println(numConfigs + ", " + numObservables + ", " + numSystems + ", " + solver.getUtility() + ", "
				+ solver.getRuntime() + ", " + tUB);

		// w.println(numConfigs+", "+numObservables+", "+numSystems+",
		// "+solver.getUtility()+", "+solver.getRuntime());

		// w.close();

		// printCompactStrategy(solver.getDefenderStrategy(), g);

//		 printStrategy2(solver.getDefenderStrategy());
		 
//		satisfyBudgetInt(g, solver.getDefenderStrategy());

		solver.deleteVars();

		// System.out.println();
		System.out.println();
	}

	public static Map<ObservableConfiguration, Integer> getBounds(DeceptionGame g,
			Map<Systems, Map<ObservableConfiguration, Integer>> strategy) {
		Map<ObservableConfiguration, Integer> bounds = new HashMap<ObservableConfiguration, Integer>();

		for (ObservableConfiguration o : g.obs) {
			int sum = 0;
			for (Systems k : g.machines) {
				sum += strategy.get(k).get(o);
			}
			bounds.put(o, sum);
		}

		return bounds;
	}

	public static double calculateUB(DeceptionGame g) {
		int totalU = 0;
		for (Systems k : g.machines) {
			totalU += k.f.utility;
		}
		return ((double) (totalU) / (double) g.machines.size());
	}

	public static void printCompactStrategy(Map<Systems, Map<ObservableConfiguration, Double>> strat) {
		for (Systems k : strat.keySet()) {
			System.out.print("K" + k.id + ": ");
			for (ObservableConfiguration o : strat.get(k).keySet()) {
				if (strat.get(k).get(o) > 0)
					System.out.print("TF" + o.id + " : " + strat.get(k).get(o) + " ");
			}
			System.out.println();
		}
	}

	public static void printCompactObsStrategy(Map<Systems, Map<ObservableConfiguration, Double>> strat,
			DeceptionGame g) {

		for (ObservableConfiguration o : g.obs) {
			double sum = 0;
			for (Systems k : strat.keySet()) {
				sum += strat.get(k).get(o);
			}
			System.out.println("O" + o.id + ": " + sum);
		}

	}

	public static void printCompactStrategy(Map<Systems, Map<ObservableConfiguration, Integer>> strat,
			DeceptionGame g) {

		for (ObservableConfiguration o : g.obs) {
			double sum = 0;
			for (Systems k : strat.keySet()) {
				sum += strat.get(k).get(o);
			}
			System.out.println("O" + o.id + ": " + sum);
			// System.out.println();
			for (Systems k : strat.keySet()) {
				if (strat.get(k).get(o) > .999)
					System.out.print("k" + k.id + " ");
			}
			System.out.println();
		}

	}

	public static void printCompactStrategy2(Map<Systems, Map<ObservableConfiguration, Double>> strat,
			DeceptionGame g) {

		for (ObservableConfiguration o : g.obs) {
			double sum = 0;
			for (Systems k : strat.keySet()) {
				sum += strat.get(k).get(o);
			}
			System.out.println("O" + o.id + ": " + sum);
		}

	}

	public static void printStrategy(Map<Systems, Map<ObservableConfiguration, Double>> strat) {
		for (Systems k : strat.keySet()) {
			System.out.print("K" + k.id + ": ");
			for (ObservableConfiguration o : strat.get(k).keySet()) {
				System.out.print("TF" + o.id + " : " + strat.get(k).get(o) + " ");
			}
			System.out.println();
		}
	}

	public static void printStrategy2(Map<Systems, Map<ObservableConfiguration, Integer>> strat) {
		for (Systems k : strat.keySet()) {
			System.out.print("K" + k.id + ": ");
			for (ObservableConfiguration o : strat.get(k).keySet()) {
				System.out.print("TF" + o.id + " : " + strat.get(k).get(o) + " ");
			}
			System.out.println();
		}
	}
}
