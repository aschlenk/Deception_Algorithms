package solvers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import models.BranchVariable;
import models.DeceptionGame;
import models.MarginalObservable;
import models.ObservableConfiguration;
import models.ObservableEU;
import models.Systems;

public class BBMarginalSearch {
	
	DeceptionGame game;
	
	private double globalLB = -1000;
	
	private double runtime;
	
	private Map<Systems, Map<ObservableConfiguration, Double>> optimalStrategy;
	
	private int iterations = 0;
	
	public BBMarginalSearch(DeceptionGame g){
		this.game = g;
	}
	
	public void solve() throws Exception{
		double start = System.currentTimeMillis();
		
		warmStart(200);
		
		//create root node
		BBMarginalNode root = new BBMarginalNode(game);
		
		root.solve();
		
		ArrayList<BBMarginalNode> searchList = new ArrayList<BBMarginalNode>();
		
		if(root.isStrategyPure()){
			ObservableEU oeu = calculateMaxMinUtility(root.getDefenderStrategy());
			if(oeu.eu > globalLB){
				globalLB = oeu.eu;
				optimalStrategy = root.getDefenderStrategy();
			}
		}else{	
			searchList.add(root);
		}
		
//		System.out.println();
//		printCompactStrategy(root.getDefenderStrategy());
//		System.out.println();

//		int index = 0;
		while (!searchList.isEmpty()) {
			BBMarginalNode node = searchList.remove(0);

			if (node.getLB() < globalLB) {
				// searchList.remove(0);
				continue;
			}

			// Need to create new children to explore, pick an observable o to branch on
			// Find observable with max N_o
			MarginalObservable var = node.getNextChild();
			
			
			// Create left and right child where we have N_o <= \ceil(N_o) and N_o >= \floor(N_o)
			// binaryExpansion(searchList, node, var);
			if(var != null){
//				System.out.println("O"+var.o.id+" floor(N_o):"+Math.floor(var.assigned)+" ceil(N_o):"+Math.ceil(var.assigned));
				childExpansion(searchList, node, var);
			}else{ // This means the sum of observables is integer but the strategy sigma is not pure
				//Solve the MILP with the bounds on the observables!
				Map<ObservableConfiguration, Integer> bounds = node.getBounds();
				GameSolver solver = new GameSolver(game, bounds);
				
				solver.solve();
				
//				System.out.println("Solving MILP w bounds: "+solver.getDefenderPayoff());
			}
			
			// System.out.println();
			// System.out.println(searchList.toString());
			// System.out.println();

			// index++;
			// if(index > 6)
			// break;
			iterations++;
			if (iterations > 1 && iterations % 30 == 0) {
				System.out.println();
				System.out.println("Global LB: " + globalLB);
				// printCompactStrategy(optimalStrategy);
				// System.out.println();
				// System.out.println();
				System.out.println(searchList.toString());
				System.out.println();
			}
		}

		runtime = (System.currentTimeMillis() - start) / 1000.0;
		
	}
	
	private void childExpansion(ArrayList<BBMarginalNode> searchList, BBMarginalNode node, MarginalObservable var) throws Exception{
		// Create left and right child where we have N_o >= \ceil(N_o) and N_o <= \floor(N_o)
		Map<ObservableConfiguration, Integer> constraintsUpperLeft = setConstraints(node.getUpperConstraints(), var, (int)Math.floor(var.assigned), true);
		Map<ObservableConfiguration, Integer> constraintsLowerLeft = setConstraints(node.getLowerConstraints(), var, (int)Math.floor(var.assigned), false);
		Map<ObservableConfiguration, Integer> constraintsUpperRight = setConstraints(node.getUpperConstraints(), var, (int)Math.ceil(var.assigned), false);
		Map<ObservableConfiguration, Integer> constraintsLowerRight = setConstraints(node.getLowerConstraints(), var, (int)Math.ceil(var.assigned), true);
		
//		System.out.println();
//		System.out.println("Upper Left: "+constraintsUpperLeft);//.get(var.k).get(var.o));
//		System.out.println("Lower Left: "+constraintsLowerLeft);//.get(var.k).get(var.o));
//		System.out.println("Upper Right: "+constraintsUpperRight);//.get(var.k).get(var.o));
//		System.out.println("Lower Right: "+constraintsLowerRight);//.get(var.k).get(var.o));
//		System.out.println();
//		
		//create left and right Child nodes and solve!
		BBMarginalNode left = new BBMarginalNode(game, node, constraintsUpperLeft, constraintsLowerLeft);
		BBMarginalNode right = new BBMarginalNode(game, node, constraintsUpperRight, constraintsLowerRight);
		
		left.solve();
		right.solve();
		
		
		//*******Up to here we are good I think! ******
		
		//If we have pure strategies then fathom node and check if it gives a new global LB
		if(left.isStrategyPure()){
			ObservableEU oeu = calculateMaxMinUtility(left.getDefenderStrategy());
			if(oeu.eu > globalLB){
				globalLB = oeu.eu;
				optimalStrategy = left.getDefenderStrategy();
			}
		}else if(left.getLB() > globalLB){
			searchList.add(left);	
			//insertNodeLevel(searchList, left);
//			insertNodeLB(searchList, left);
		}
		//System.out.println("Left Level: "+left.getLevel()+" Utility: "+left.getLB());
		
		if(right.isStrategyPure()){ //At a leaf we should have a pure strategy! so don't need to check
			ObservableEU oeu = calculateMaxMinUtility(right.getDefenderStrategy());
			if(oeu.eu > globalLB){
				globalLB = oeu.eu;
				optimalStrategy = right.getDefenderStrategy();
			}
		}else if(right.getLB() > globalLB){
			searchList.add(right);		
			//insertNodeLevel(searchList, left);
//			insertNodeLB(searchList, right);		
		}
		//System.out.println("Right Level: "+right.getLevel()+" Utility: "+right.getLB());
		
		//System.out.println("Right Level: "+right.getLevel()+" Utility: "+right.getLB());
		
		Collections.sort(searchList); //This makes it best first search
		//Might want to run depth first search for a certain number of iterations, then do best first
		
	}
	
	private Map<ObservableConfiguration, Integer> setConstraints(Map<ObservableConfiguration, Integer> parentConstraints, MarginalObservable var, int value, boolean settingBound){
		Map<ObservableConfiguration, Integer> newConstraints = new HashMap<ObservableConfiguration, Integer>();
		
		for(ObservableConfiguration o : parentConstraints.keySet()){
				newConstraints.put(o, parentConstraints.get(o));
		}
		
		if(settingBound){
			//If we set an upper bound value, then 
			newConstraints.put(var.o, value);
		}
		
		return newConstraints;
	}
	
	private void warmStart(int numShuffles){
		//run Maximin Solver before creating root
		//Use as global lower bound and the defender strategy
		//System.out.println("Runnning Greedy Max Min Solver");
		
		GreedyMaxMinSolver solver = new GreedyMaxMinSolver(game);
		
		solver.setShuffle(false);
		
		solver.solve();
		
		globalLB = solver.getDefenderUtility();
		Map<Systems, Map<ObservableConfiguration, Double>> strat = new HashMap<Systems, Map<ObservableConfiguration, Double>>();
		for(Systems k : solver.getGreedyStrategy().keySet()){
			strat.put(k, new HashMap<ObservableConfiguration, Double>());
			for(ObservableConfiguration o : solver.getGreedyStrategy().get(k).keySet()){
				strat.get(k).put(o, (double)solver.getGreedyStrategy().get(k).get(o));
			}
		}
		optimalStrategy = strat;
		
		for(int i=1; i<=numShuffles; i++){
			GreedyMaxMinSolver solver2 = new GreedyMaxMinSolver(game);
			
			solver2.setShuffle(true);
			
			solver2.solve();
			
			if(solver2.getDefenderUtility() > globalLB){
				globalLB = solver2.getDefenderUtility();
				Map<Systems, Map<ObservableConfiguration, Double>> strategy = new HashMap<Systems, Map<ObservableConfiguration, Double>>();
				for(Systems k : solver2.getGreedyStrategy().keySet()){
					strat.put(k, new HashMap<ObservableConfiguration, Double>());
					for(ObservableConfiguration o : solver2.getGreedyStrategy().get(k).keySet()){
						strat.get(k).put(o, (double)solver2.getGreedyStrategy().get(k).get(o));
					}
				}
				optimalStrategy = strategy;
				System.out.println("New LB: "+globalLB);
			}
		}
		
	}
	
	private ObservableEU calculateMaxMinUtility(Map<Systems, Map<ObservableConfiguration, Double>> strategy) {
		double maxmin = 0;
		ObservableConfiguration key = null;
		
		for(ObservableConfiguration o : game.obs){
			double expectedU = 0;
			double total = 0;
			for(Systems k : strategy.keySet()){
				expectedU += strategy.get(k).get(o)*k.f.utility;
				total += strategy.get(k).get(o);
			}
			
			if(maxmin > (expectedU/total)){
				maxmin = (expectedU/total);
				key = o;
			}
		}
		ObservableEU o1 = new ObservableEU(key, maxmin);
		
		return o1;
	}
	
	public double getPayoff(){
		return globalLB;
	}

	public Map<Systems, Map<ObservableConfiguration, Double>> getDefenderStrategy(){
		return optimalStrategy;
	}
	
	public double getRuntime(){
		return runtime;
	}
	
	public static void printCompactStrategy(Map<Systems, Map<ObservableConfiguration, Double>> strat){
		for(Systems k : strat.keySet()){
			System.out.print("K"+k.id+": ");
			for(ObservableConfiguration o : strat.get(k).keySet()){
				if(strat.get(k).get(o)>0)
					System.out.print("TF"+o.id+" : "+strat.get(k).get(o)+" ");
			}
			System.out.println();
		}
	}

}
