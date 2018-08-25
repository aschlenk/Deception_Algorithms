package examples;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import Utilities.DeceptionGameHelper;
import models.DeceptionGame;
import models.ObservableConfiguration;
import models.Systems;
import solvers.BisectionAlgorithm;
import solvers.GameSolver;
import solvers.GameSolverCuts;
import solvers.GreedyMaxMinSolver;
import solvers.UniformEstimation;

public class Experiments {

	public static void main(String[] args) throws Exception {
		
		//numConfigs, numSystems, numObs, numGames, experimentNum
		//Experiment #2 is regular experiments with several variations to test greedy maxmin
		//Experiment #4 is with MILP w/ cuts
		//Experiment #8: Comparison of how often greedy maximin returns optimal solution: 10 and 20 systems varying OCs 2, 4, 6, 8, 10
//		for(int i=20; i<=20; i+=2)
			runExperiments(20, 20, 20, 30, 22);
		
		
	}

	public static void runExperiments(int numConfigs, int numSystems, int numObs, int numgames, int experimentnum)
			throws Exception {

		createGames(numConfigs, numSystems, numObs, numgames, experimentnum);
		
		double maxRuntime = 1800; //1800 is 15 minutes; 900 = solvertime * #cores
		
//		solveMILP(numConfigs, numSystems, numObs, numgames, experimentnum, maxRuntime, true);
		
//		solveMILPCut(numConfigs, numSystems, numObs, numgames, experimentnum, maxRuntime, true);
		
		int lowNumShuffles = 4000; 
		int highNumShuffles = 4000;
		
		double lambda = .1;
		
		for(int i=lowNumShuffles; i<=highNumShuffles; i+=500){
//			solveGreedyMaxMin(numConfigs, numSystems, numObs, numgames, experimentnum, highNumShuffles, false, true, lambda);
//			solveGreedyMaxMin(numConfigs, numSystems, numObs, numgames, experimentnum, i, false, false, lambda);
		}
		
//		solveUniformEstimation(numConfigs, numSystems, numObs, numgames, experimentnum);
		
		int numRuns = 1000;
//		solveRandomizedRounding(numConfigs, numSystems, numObs, numgames, experimentnum, numRuns);
		
		//RUns all permutations
//		solveGreedyMaxMin(numConfigs, numSystems, numObs, numgames, experimentnum, highNumShuffles, true);
		
		
		// String dir = "C:/Users/Aaron Schlenker/workspace/CyberDeception/";

		// DeceptionGame game2 = new DeceptionGame();
		// game2.readInGame(dir, 1, 1);

		// game2.printGame();

		// runSampleLinearGame(g, numConfigs, numObs, numSystems, seed);
		// runSampleGame(g, numConfigs, numObs, numSystems, seed,
		// experimentnum);
		// runMarginalSolver(g, numConfigs, numObs, numSystems, seed);
		// runBisectionAlgorithm(g, numConfigs, numObs, numSystems, seed);

		// runBBSearch(g, numConfigs, numObs, numSystems, seed);
		// There is an issue with returning a defender strategy when the optimal
		// utility is equal to the lower bound!
		// runBBSigmaSearch(g, numConfigs, numObs, numSystems, seed);

		System.out.println();
		System.out.println();
		// runHeuristicSolver(g);

	}

	public static void createGames(int numConfigs, int numSystems, int numObs, int numgames, int experimentnum)
			throws Exception {

		for (int i = 1; i <= numgames; i++) {
			DeceptionGame g = new DeceptionGame();

			long seed = System.currentTimeMillis();// 113;
			g.generateGame(numConfigs, numObs, numSystems, seed);
			// g.printGame();

			g.exportGame(i, experimentnum);
			
			System.gc();
			
		}
	}

	private static void solveUniformEstimation(int numConfigs, int numSystems, int numObs, int numGames,  int experimentnum) throws Exception{
		System.out.println("Runnning Greedy Max Min Solver");

		String dir = "C:/Users/Aaron Schlenker/workspace/CyberDeception/";

		for (int i = 1; i <= numGames; i++) {
			DeceptionGame game = new DeceptionGame();
			game.readInGame(dir, numConfigs, numObs, numSystems, i, experimentnum);

			// game.printGame();
			
			runUniformEstimation(game, numConfigs, numObs, numSystems, experimentnum);
			
//			System.out.println(numConfigs+", "+numObs+", "+numSystems+", "+solver.getDefenderUtility()+", "+solver.getRuntime());
			
			System.gc();
		}
	}
	
	private static void runUniformEstimation(DeceptionGame game, int numConfigs, int numObs, int numSystems,
			int experimentnum) throws Exception {
		
		//double start = System.currentTimeMillis();
		
		String output = "experiments/UE_" + experimentnum + "_" + numConfigs + "_" + numObs + "_" + numSystems+ ".csv";

		PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(output, true)));

		// Need to load cplex libraries
		String cplexInputFile = "CplexConfig";

		DeceptionGameHelper.loadLibrariesCplex(cplexInputFile);

		// Solve the MILP
//		GameSolver solverMILP = new GameSolver(game);

//		solverMILP.solve();
		
		//Do it with cuts!
		double highUtil = -100;
		
		for(int i=1; i<=100; i++){
			GreedyMaxMinSolver solver = new GreedyMaxMinSolver(game);
			
			solver.setShuffle(true);
			
			solver.solve();
			
			if(solver.getDefenderUtility() > highUtil)
				highUtil = solver.getDefenderUtility();
			
		}
		
		// Solve the MILP
		GameSolverCuts solverMILP = new GameSolverCuts(game);

		solverMILP.setGlobalLB(highUtil);

		solverMILP.setMaxSubsetSize(1);

		solverMILP.solve();
		
		double start = System.currentTimeMillis();
		
		UniformEstimation solver = new UniformEstimation(game);
		
		solver.solve();
		
		System.out.println("UE: "+GameSolver.calculateExpectedUtility(game, solver.getStrategy()));
		
		double runtime = (System.currentTimeMillis()-start)/1000.0;
		
		System.out.println(numConfigs+", "+numObs+", "+numSystems+", "+solver.getDefenderUtility()+", "+GameSolver.calculateExpectedUtility(game, solver.getStrategy())
				+", "+solverMILP.getDefenderPayoff()+", "+solver.calculateExpectedUtility(game, solverMILP.getDefenderStrategy())+","+runtime);
		
		w.println(numConfigs + ", " + numObs + ", " + numSystems + ", " + solver.getDefenderUtility()+", "+GameSolver.calculateExpectedUtility(game, solver.getStrategy()) 
				+", "+solverMILP.getDefenderPayoff()+", "+solver.calculateExpectedUtility(game, solverMILP.getDefenderStrategy())+ ", "+runtime);

		w.close();
		

		System.out.println();
	}
	
	private static void solveRandomizedRounding(int numConfigs, int numSystems, int numObs, int numGames,  int experimentnum, int numShuffles) throws Exception {
		System.out.println("Runnning Greedy Max Min Solver");

		String dir = "C:/Users/Aaron Schlenker/workspace/CyberDeception/";

		for (int i = 1; i <= numGames; i++) {
			DeceptionGame game = new DeceptionGame();
			game.readInGame(dir, numConfigs, numObs, numSystems, i, experimentnum);

			// game.printGame();
			
			runRandomizedRounder(game, numConfigs, numObs, numSystems, experimentnum, numShuffles);

//			System.out.println(numConfigs+", "+numObs+", "+numSystems+", "+solver.getDefenderUtility()+", "+solver.getRuntime());
			
			System.gc();
		}
	}
	
	private static void runRandomizedRounder(DeceptionGame game, int numConfigs, int numObservables, int numSystems, int experimentnum, int numRuns) throws Exception{

		String output = "experiments/RR_" + experimentnum + "_" + numConfigs + "_" + numObservables + "_" + numSystems + "_" + numRuns+ ".csv";

		PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(output, true)));

		double tUB = calculateUB(game);
		
		double bestUtil = -100;
		
		double start = System.currentTimeMillis();
		
//		System.out.println();
//		System.out.println("Running Bisection Algorithm");
//		System.out.println();
		// Need to load cplex libraries
		String cplexInputFile = "CplexConfig";

		DeceptionGameHelper.loadLibrariesCplex(cplexInputFile);
		
		Map<ObservableConfiguration, Integer> bounds = new HashMap<ObservableConfiguration, Integer>();
		
		BisectionAlgorithm alg = new BisectionAlgorithm(game);
		
		alg.solve();
		
		//Need to take marginal from the Bisection Algorithm and randomly round it to integers, or some intelligent rounding
		Map<Systems, Map<ObservableConfiguration, Double>> defenderStrategy = alg.getDefenderStrategy();
		
		double gap = 1;
		Systems sys = null;
		ObservableConfiguration obs = null;
		
		//Random version
		double utilBest = -1000;
		Random r = new Random();
		for(int i=0; i<numRuns; i++){
			Map<Systems, Map<ObservableConfiguration, Double>> defenderStrategyTemp = new HashMap<Systems, Map<ObservableConfiguration, Double>>();
			for(Systems k : game.machines){
				defenderStrategyTemp.put(k, new HashMap<ObservableConfiguration, Double>());
				for(ObservableConfiguration o : game.obs){
					defenderStrategyTemp.get(k).put(o, defenderStrategy.get(k).get(o));
				}
			}
			
			while(!checkIfStrategyPure(defenderStrategyTemp)){
				//Find n_k,tf that is closest to 1
				Map<Systems, Map<ObservableConfiguration, Double>> strategyGaps = new HashMap<Systems, Map<ObservableConfiguration, Double>>();
				for(Systems k : game.machines){
					for(ObservableConfiguration o : game.obs){
						if(defenderStrategy.get(k).get(o) >= 1.0) //only round non-integer values
							continue;
						
						if(defenderStrategy.get(k).get(o) > 0.0 && defenderStrategy.get(k).get(o) < 1){
							if(strategyGaps.get(k) == null)
								strategyGaps.put(k, new HashMap<ObservableConfiguration, Double>());
							
							strategyGaps.get(k).put(o, defenderStrategy.get(k).get(o));
						}
							
					}
				}
				
				//Randomly pick system and randomly pick observable to fix
				int sysnum = r.nextInt(strategyGaps.keySet().size())+1;
				int index = 1;
				for(Systems k : strategyGaps.keySet()){
					
					if(sysnum == index){
						sys = k;
						int obsnum = r.nextInt(strategyGaps.get(k).keySet().size())+1;
						int index2 = 1;
						
						//Randomly pick Observable to set
						for(ObservableConfiguration o : strategyGaps.get(k).keySet()){
							if(obsnum == index2){
								obs = o;
								break;
							}else
								index2++;
						}
						break;
					}else
						index++;
	
				}
				
				//Set variable equal to 1, and all others equal to zero
				for(ObservableConfiguration o : game.obs){
					if(o.id != obs.id)
						defenderStrategyTemp.get(sys).put(o, 0.0);
					else
						defenderStrategyTemp.get(sys).put(o, 1.0);
				}
	
//				printStrategy(alg.getDefenderStrategy());
			}
			
			if(GameSolver.calculateExpectedUtility1(game, defenderStrategyTemp) > utilBest)
				utilBest = GameSolver.calculateExpectedUtility1(game, defenderStrategyTemp);
			
		}
		
		System.out.println();
		System.out.println("Best Util: "+utilBest);
		System.out.println();
		
		
		System.out.println(numConfigs+", "+numObservables+", "+numSystems+", "+alg.getLB()+", "+alg.getUB()+", "+alg.getRuntime()+", "+alg.getIterations());
		
		
		double runtime = (System.currentTimeMillis()-start)/1000.0;
		
		System.out.println(numConfigs+", "+numObservables+", "+numSystems+", "+utilBest+", "+runtime);
		
		w.println(numConfigs + ", " + numObservables + ", " + numSystems + ", " + utilBest + ", "
				+ runtime);

		w.close();
		

		System.out.println();
	}
	
	/*
	 * Expects strategies that should be in-between [0,1] for all entries
	 */
	private static boolean checkIfStrategyPure(Map<Systems, Map<ObservableConfiguration, Double>> defenderStrategy){
		for(Systems k : defenderStrategy.keySet()){
			for(ObservableConfiguration o : defenderStrategy.get(k).keySet()){
				if(defenderStrategy.get(k).get(o) >= .00001 && defenderStrategy.get(k).get(o) <= .99999)
					return false;
			}
		}
		return true;
	}
	

	private static void solveGreedyMaxMin(int numConfigs, int numSystems, int numObs, int numGames,  int experimentnum, int numShuffles, 
			boolean allPermutations, boolean fixedCosts, double lambda) throws IOException {
		System.out.println("Runnning Greedy Max Min Solver");

		String dir = "C:/Users/Aaron Schlenker/workspace/CyberDeception/";

		for (int i = 1; i <= numGames; i++) {
			DeceptionGame game = new DeceptionGame();
			game.readInGame(dir, numConfigs, numObs, numSystems, i, experimentnum, fixedCosts);

			// game.printGame();
			if(!allPermutations)
				runGreedyMaxMin(game, numConfigs, numObs, numSystems, experimentnum, numShuffles, fixedCosts, lambda);
			else
				runGreedyMaxMinAllPermutations(game, numConfigs, numObs, numSystems, experimentnum, numShuffles, fixedCosts, lambda);
			
//			System.out.println(numConfigs+", "+numObs+", "+numSystems+", "+solver.getDefenderUtility()+", "+solver.getRuntime());
			
			System.gc();
		}
	}
	
	private static void runGreedyMaxMin(DeceptionGame game, int numConfigs, int numObservables, int numSystems, int experimentnum, int numShuffles, 
			boolean fixedCosts, double lambda) throws IOException{

		String output = "experiments/GMM_" + experimentnum + "_" + numConfigs + "_" + numObservables + "_" + numSystems + "_" + numShuffles+"_"+fixedCosts
				+"_"+lambda+ ".csv";

		PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(output, true)));

		double tUB = calculateUB(game);
		
		double bestUtil = -100;
		
		double start = System.currentTimeMillis();
		
		for(int i=1; i<=numShuffles; i++){

			GreedyMaxMinSolver solver = new GreedyMaxMinSolver(game);

			solver.setShuffle(true);
			solver.setRandomIndifferent(); //This seems to be good to have if we have enough shuffles we test!
			
//			System.out.println("Budget: "+g.Budget);
//			solver.solve();
			if(fixedCosts)
				solver.solveHardGMM();
			else
				solver.solveSoftGMM(lambda);
			
			//solver.solve();
			
			if(solver.getDefenderUtility() > bestUtil)
				bestUtil = solver.getDefenderUtility();
			
		}
		
		double runtime = (System.currentTimeMillis()-start)/1000.0;
		
		System.out.println(numConfigs+", "+numObservables+", "+numSystems+", "+bestUtil+", "+runtime);
		
		w.println(numConfigs + ", " + numObservables + ", " + numSystems + ", " + bestUtil + ", "
				+ runtime);

		w.close();
		

		System.out.println();
	}
	
	private static void runGreedyMaxMinAllPermutations(DeceptionGame game, int numConfigs, int numObservables, int numSystems, int experimentnum, 
			int numShuffles, boolean fixedCosts, double lambda) throws IOException{

		String output = "experiments/GMMAP_" + experimentnum + "_" + numConfigs + "_" + numObservables + "_" + numSystems + "_" + numShuffles
				+"_"+fixedCosts+"_"+lambda+ ".csv";

		PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(output, true)));

		double tUB = calculateUB(game);
		
		double bestUtil = -100;
		
		double start = System.currentTimeMillis();
		
		ArrayList<Systems> systems1 = new ArrayList<Systems>();
		for(Systems k : game.machines)
			systems1.add(k);
		
		ArrayList<ArrayList<Systems>> allPermutations = generatePerm(systems1);
		
		
		for(int i=0; i<allPermutations.size(); i++){

			for(int j=0; j<4; j++){ //run each permutation a few times
				GreedyMaxMinSolver solver = new GreedyMaxMinSolver(game);
	
				solver.setFixedOrdering(allPermutations.get(i));
				//solver.setShuffle(true);
				solver.setRandomIndifferent();
				
				if(fixedCosts)
					solver.solveHardGMM();
				else
					solver.solveSoftGMM(lambda);
	//				solver.solve();
				
				if(solver.getDefenderUtility() > bestUtil)
					bestUtil = solver.getDefenderUtility();
			}
		}
		
		double runtime = (System.currentTimeMillis()-start)/1000.0;
		
		System.out.println(numConfigs+", "+numObservables+", "+numSystems+", "+bestUtil+", "+runtime);
		
		w.println(numConfigs + ", " + numObservables + ", " + numSystems + ", " + bestUtil + ", "
				+ runtime);

		w.close();
		

		System.out.println();
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
	
	public static void solveMILP(int numConfigs, int numSystems, int numObs, int numgames, int experimentnum, double maxRuntime, boolean costs) throws Exception {
		String dir = "C:/Users/Aaron Schlenker/workspace/CyberDeception/";
		
		for(int i=1; i<=numgames; i++){
			DeceptionGame game = new DeceptionGame();
//			System.out.println(costs);
			if(costs)
				game.readInGame(dir, numConfigs, numObs, numSystems, i, experimentnum, costs);
			else
				game.readInGame(dir, numConfigs, numObs, numSystems, i, experimentnum);
			
			
//			game.printGame();

			runMILP(game, numConfigs, numObs, numSystems, experimentnum, maxRuntime, costs);

			Thread.sleep(100);
			
			System.gc();
		}
	}
	
	public static void solveMILPCut(int numConfigs, int numSystems, int numObs, int numgames, int experimentnum, double maxRuntime, boolean costs) throws Exception {
		String dir = "C:/Users/Aaron Schlenker/workspace/CyberDeception/";
		
		for(int i=1; i<=numgames; i++){
			DeceptionGame game = new DeceptionGame();
			if(costs)
				game.readInGame(dir, numConfigs, numObs, numSystems, i, experimentnum, costs);
			else
				game.readInGame(dir, numConfigs, numObs, numSystems, i, experimentnum);
				
			
			// game.printGame();

			runMILPCut(game, numConfigs, numObs, numSystems, experimentnum, maxRuntime, costs);

			Thread.sleep(100);
			
			System.gc();
		}
	}
	
	public static void runMILPCut(DeceptionGame g, int numConfigs, int numObservables, int numSystems, int experimentnum, double maxRuntime, boolean costs)
			throws Exception {
		double start = System.currentTimeMillis();
		
		double highUtil = -100;
		
		for(int i=1; i<=200; i++){
			GreedyMaxMinSolver solver = new GreedyMaxMinSolver(g);
			
			solver.setShuffle(true);
			solver.setRandomIndifferent();
			solver.solveHardGMM();
			
			if(solver.getDefenderUtility() > highUtil)
				highUtil = solver.getDefenderUtility();
			
		}
		
//		for(int i=1; i<=200; i++){
//			GreedyMaxMinSolver solver = new GreedyMaxMinSolver(g);
//			
//			solver.setShuffle(true);
//			solver.setRandomIndifferent();
//			solver.solveSoftGMM();
//			
//			if(solver.getDefenderUtility() > highUtil)
//				highUtil = solver.getDefenderUtility();
//			
//		}
		
		System.out.println("Running MILP w Cuts");
		System.out.println();
		
		boolean verbose = false;

		// Need to load cplex libraries
		String cplexInputFile = "CplexConfig";

		DeceptionGameHelper.loadLibrariesCplex(cplexInputFile);

		// Solve the MILP
		GameSolverCuts solver = new GameSolverCuts(g);

		solver.setGlobalLB(highUtil);

		solver.setMaxSubsetSize(1);
		
		solver.setMaxRuntime(maxRuntime);
		
		solver.setCosts(costs);
		
		solver.solve();

		String output = "experiments/MILPCut1_" + experimentnum + "_" + numConfigs + "_" + numObservables + "_" + numSystems
				+ ".csv";
		
		PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(output, true)));

		double tUB = calculateUB(g);

		System.out.println(numConfigs + ", " + numObservables + ", " + numSystems + ", " + solver.getUtility() + ", "
				+ solver.getRuntime() + ", " + tUB);

		w.println(numConfigs+", "+numObservables+", "+numSystems+", "+solver.getUtility()+", "+solver.getRuntime());
		
		w.close();

		// printCompactStrategy(solver.getDefenderStrategy(), g);

		// printStrategy2(solver.getDefenderStrategy());

		solver.deleteVars();

		// System.out.println();
		System.out.println();
	}

	public static void runMILP(DeceptionGame g, int numConfigs, int numObservables, int numSystems, int experimentnum, double maxRuntime, boolean costs)
			throws Exception {
		System.out.println("Running MILP");
		System.out.println();

		boolean verbose = false;

		// Need to load cplex libraries
		String cplexInputFile = "CplexConfig";

		DeceptionGameHelper.loadLibrariesCplex(cplexInputFile);

		// Solve the MILP
		GameSolver solver = new GameSolver(g);

		solver.setCosts(costs);
		
		solver.solve();

		//version with cutoff
		
		String output = "experiments/MILP_" + experimentnum + "_" + numConfigs + "_" + numObservables + "_" + numSystems+"_"+costs
				+ ".csv";

		PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(output, true)));

		double tUB = calculateUB(g);

		System.out.println(numConfigs + ", " + numObservables + ", " + numSystems + ", " + solver.getUtility() + ", "
				+ solver.getRuntime() + ", " + tUB);

		w.println(numConfigs + ", " + numObservables + ", " + numSystems + ", " + solver.getUtility() + ", "
				+ solver.getRuntime());

		w.close();

		// printCompactStrategy(solver.getDefenderStrategy(), g);

		System.out.println();
		System.out.println();
	}

	public static void runSampleGame(DeceptionGame g, int numConfigs, int numObservables, int numSystems,
			int experimentnum) throws Exception {
		System.out.println("Running MILP");
		System.out.println();

		boolean verbose = false;

		// Need to load cplex libraries
		String cplexInputFile = "CplexConfig";

		DeceptionGameHelper.loadLibrariesCplex(cplexInputFile);

		// Solve the MILP
		GameSolver solver = new GameSolver(g);

		solver.solve();

		String output = "experiments/CDG_" + experimentnum + "_" + numConfigs + "_" + numObservables + "_" + numSystems
				+ ".csv";

		PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(output, true)));

		double tUB = calculateUB(g);

		System.out.println(numConfigs + ", " + numObservables + ", " + numSystems + ", " + solver.getUtility() + ", "
				+ solver.getRuntime() + ", " + tUB);

		w.println(numConfigs + ", " + numObservables + ", " + numSystems + ", " + solver.getUtility() + ", "
				+ solver.getRuntime());

		w.close();

		printCompactStrategy(solver.getDefenderStrategy(), g);

		System.out.println();
		System.out.println();
	}

	public static double calculateUB(DeceptionGame g) {
		int totalU = 0;
		for (Systems k : g.machines) {
			totalU += k.f.utility;
		}
		return ((double) (totalU) / (double) g.machines.size());
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

}
