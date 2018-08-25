package examples;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import Utilities.DeceptionGameHelper;
import models.Configuration;
import models.DeceptionGame;
import models.ObservableConfiguration;
import models.Strategy;
import models.Systems;
import solvers.GameSolver;
import solvers.generateStates;

public class SimpleDeception {

	public static void main(String[] args) throws Exception {
		runExperiment(50, 51, 5, 2, 3, 1, 5, 6, 1, 5);
	}

	public static void runExperiment(int cs, int ce, int ci, int os, int oe, int oi, int ss, int se, int si, int numRuns) throws Exception {
		// Need to load cplex libraries
		String cplexInputFile = "CplexConfig";

		DeceptionGameHelper.loadLibrariesCplex(cplexInputFile);

		for (int numConfigs = cs; numConfigs < ce; numConfigs += ci) {
			for (int numSystems = ss; numSystems < se; numSystems += si) {
				for (int numObservables = os; numObservables < oe; numObservables+=oi) {
					//System.out.println("NumConfigs: " + numConfigs + " numObservables: " + numObservables
							//+ " numSystems: " + numSystems);
					
					for(int i=0; i<numRuns; i++)		
						runSampleGame(numConfigs, numObservables, numSystems, System.currentTimeMillis());
				}
			}
		}

	}

	public static void runSampleGame(int numConfigs, int numObservables, int numSystems, long seed) throws Exception {
		boolean verbose = false;

		DeceptionGame g = new DeceptionGame();
		// g.generateGame(3, 2, 3);
		g.generateGame(numConfigs, numObservables, numSystems, seed);
		if (verbose)
			g.printGame();

		// Solve the MILP
		GameSolver solver = new GameSolver(g);

		solver.solve();

		String output = "experiments/CDG_"+numConfigs+"_"+numObservables+"_"+numSystems+".csv";
		
		PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(output, true)));
		
		double tUB = calculateUB(g);
		
		System.out.println(numConfigs+", "+numObservables+", "+numSystems+", "+solver.getUtility()+", "+solver.getRuntime()+", "+tUB);
		
		w.println(numConfigs+", "+numObservables+", "+numSystems+", "+solver.getUtility()+", "+solver.getRuntime());
		
		w.close();
	}

	public static double calculateUB(DeceptionGame g){
		int totalU = 0;
		for(Systems k : g.machines){
			totalU += k.f.utility;
		}
		return ((double)(totalU)/(double)g.machines.size());
	}
	
	public static void runSimpleGame() throws Exception {
		// generate configurations
		ArrayList<Configuration> configs = new ArrayList<Configuration>();
		configs = generateConfigurations();

		// generate observable configurations
		ArrayList<ObservableConfiguration> observables = new ArrayList<ObservableConfiguration>();
		observables = generateObservables(configs);

		// generate systems
		ArrayList<Systems> systems = new ArrayList<Systems>();
		systems = generateSystems(configs);

		// generate Strategy
		Strategy s = new Strategy();
		s.setObservable(systems.get(0), observables.get(0));
		s.setObservable(systems.get(1), observables.get(0));
		s.setObservable(systems.get(2), observables.get(1));

		s.printStrategy();

		ArrayList<Integer> numConfigs = new ArrayList<Integer>();
		numConfigs.add(1);
		numConfigs.add(0);
		numConfigs.add(1);
		numConfigs.add(1);

		ArrayList<Integer> numObs = new ArrayList<Integer>();
		numObs.add(2);
		numObs.add(1);

		DeceptionGame g = new DeceptionGame(configs, observables, systems);

		// Find possible states
		// generateStates g = new generateStates(configs, observables, systems,
		// numConfigs, numObs, s);

		// Need to load cplex libraries
		String cplexInputFile = "CplexConfig";

		DeceptionGameHelper.loadLibrariesCplex(cplexInputFile);

		// Solve the MILP
		GameSolver solver = new GameSolver(g);

		solver.solve();

	}

	private static ArrayList<Systems> generateSystems(ArrayList<Configuration> c) {
		ArrayList<Systems> systems = new ArrayList<Systems>();
		Systems k1 = new Systems(c.get(0));
		Systems k2 = new Systems(c.get(2));
		Systems k3 = new Systems(c.get(3));
		systems.add(k1);
		systems.add(k2);
		systems.add(k3);

		return systems;
	}

	private static ArrayList<ObservableConfiguration> generateObservables(ArrayList<Configuration> configs) {
		ArrayList<ObservableConfiguration> o = new ArrayList<ObservableConfiguration>();
		ObservableConfiguration f1 = new ObservableConfiguration();
		ObservableConfiguration f2 = new ObservableConfiguration();
		o.add(f1);
		o.add(f2);
		// add configurations
		f1.addConfiguration(configs.get(0));
		f1.addConfiguration(configs.get(1));
		f1.addConfiguration(configs.get(2));

		f2.addConfiguration(configs.get(1));
		f2.addConfiguration(configs.get(2));
		f2.addConfiguration(configs.get(3));

		return o;
	}

	private static ArrayList<Configuration> generateConfigurations() {
		ArrayList<Configuration> c = new ArrayList<Configuration>();
		Configuration f1 = new Configuration(-10);
		Configuration f2 = new Configuration(-10);
		Configuration f3 = new Configuration(0);
		Configuration f4 = new Configuration(-6);
		c.add(f1);
		c.add(f2);
		c.add(f3);
		c.add(f4);

		return c;
	}

}
