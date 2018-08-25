package examples;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import Utilities.DeceptionGameHelper;
import models.Configuration;
import models.DeceptionGame;
import models.ObservableConfiguration;
import models.Systems;
import solvers.GameSolver;

public class GenerateHoneyScripts {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		DeceptionGame g = generateGameInstance();
		

		// Need to load cplex libraries
		String cplexInputFile = "CplexConfig";

		// g.setRandomBudget();

		DeceptionGameHelper.loadLibrariesCplex(cplexInputFile);

//		System.out.println("Budget: " + g.Budget);

		// Solve the MILP
		GameSolver solver = new GameSolver(g);
		
		solver.setCosts(false);
		
		solver.setMaxRuntime(120);
		
		solver.setMilpGap(0.00);

		solver.solve();

		double maxUtil = solver.getDefenderPayoff();
		
		Map<Systems, Map<ObservableConfiguration, Integer>> strategy = solver.getDefenderStrategy();

		printHoneyDScript(strategy);

	}
	
	public static DeceptionGame generateGameInstance(){
		DeceptionGame g = new DeceptionGame();
		
		Configuration c1 = new Configuration("winxp", -8);
		Configuration c2 = new Configuration("win2008", -3);
		Configuration c3 = new Configuration("win2k", -3);
		Configuration c4 = new Configuration("win7pro", -15);
		Configuration c5 = new Configuration("win7ent", -15);
		Configuration c6 = new Configuration("openwrt", -8);
		Configuration c7 = new Configuration("xbox", -2);
		Configuration c8 = new Configuration("winxpemb", -6);
		Configuration c9 = new Configuration("avayagw", -2);
		Configuration c10 = new Configuration("freebsd", -6);
		Configuration c11 = new Configuration("ubuntu8", -15);
		Configuration c12 = new Configuration("slackware", -15);

		g.configs.add(c1);
		g.configs.add(c2);
		g.configs.add(c3);
		g.configs.add(c4);
		g.configs.add(c5);
		g.configs.add(c6);
		g.configs.add(c7);
		g.configs.add(c8);
		g.configs.add(c9);
		g.configs.add(c10);
		g.configs.add(c11);
		g.configs.add(c12);
		
		//Create Observable Configurations
		ObservableConfiguration oc1 = new ObservableConfiguration(c1.name);
		oc1.configs.add(c1);
		oc1.configs.add(c2);
		oc1.configs.add(c3);
		oc1.configs.add(c4);
		oc1.configs.add(c5);
		oc1.configs.add(c6);
		ObservableConfiguration oc2 = new ObservableConfiguration(c2.name);
		oc2.configs = oc1.configs;
		ObservableConfiguration oc3 = new ObservableConfiguration(c3.name);
		oc3.configs = oc1.configs;
		ObservableConfiguration oc4 = new ObservableConfiguration(c4.name);
		oc4.configs = oc1.configs;
		ObservableConfiguration oc5 = new ObservableConfiguration(c5.name);
		oc5.configs = oc1.configs;
		ObservableConfiguration oc6 = new ObservableConfiguration(c6.name);
		oc6.configs = oc1.configs;
		
		ObservableConfiguration oc7 = new ObservableConfiguration(c7.name);
		oc7.configs.add(c7);
		
		ObservableConfiguration oc8 = new ObservableConfiguration(c8.name);
		oc8.configs.add(c8);
		oc8.configs.add(c11);
		oc8.configs.add(c12);
		

		ObservableConfiguration oc9 = new ObservableConfiguration(c9.name);
		oc9.configs.add(c9);

		ObservableConfiguration oc10 = new ObservableConfiguration(c10.name);
		oc10.configs.add(c10);
		

		ObservableConfiguration oc11 = new ObservableConfiguration(c11.name);
		oc11.configs = oc8.configs;
		ObservableConfiguration oc12 = new ObservableConfiguration(c12.name);
		oc12.configs = oc8.configs;
		
		g.obs.add(oc1);
		g.obs.add(oc2);
		g.obs.add(oc3);
		g.obs.add(oc4);
		g.obs.add(oc5);
		g.obs.add(oc6);
		g.obs.add(oc7);
		g.obs.add(oc8);
		g.obs.add(oc9);
		g.obs.add(oc10);
		g.obs.add(oc11);
		g.obs.add(oc12);
		
		//generate systems
		// Create a random number and assign it
		for (int i = 1; i <= 12; i++) {
			Systems k1 = new Systems(g.configs.get(i-1));
			g.machines.add(k1);
		}
		
		g.costFunction = new HashMap<Configuration, Map<ObservableConfiguration, Integer>>();
		
		for(Configuration c : g.configs){
			g.costFunction.put(c, new HashMap<ObservableConfiguration, Integer>());
			
			for(ObservableConfiguration oc : g.obs){
					g.costFunction.get(c).put(oc, 0);
			}
		}
		
		return g;
	}
	
	public static void printHoneyDScript(Map<Systems, Map<ObservableConfiguration, Integer>> strategy) throws IOException{
		//Beginning of file with personailities and ethernets should always be the same
		//Only need to change the bindings according to the optimal deception strategy and the current machines
		String output = "deceptiveExperiment.conf";
		PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(output, true)));

		for(Systems k : strategy.keySet()){
			for(ObservableConfiguration oc : strategy.get(k).keySet())
			if(k.id <= 10){
				if(strategy.get(k).get(oc) == 1)
					w.println("bind 192.168.0.10"+(k.id-1)+" "+oc.name);
			}else{
				if(strategy.get(k).get(oc) == 1)
					w.println("bind 192.168.0.1"+k.id+" "+oc.name);
			}
		}
		
		w.close();
	}
	

}
