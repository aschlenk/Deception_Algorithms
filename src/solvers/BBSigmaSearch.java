package solvers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import models.BranchVariable;
import models.DeceptionGame;
import models.ObservableConfiguration;
import models.ObservableEU;
import models.Systems;

public class BBSigmaSearch {
	
	DeceptionGame game;
	
	private double globalLB = -1000;
	
	private double runtime;
	
	private Map<Systems, Map<ObservableConfiguration, Double>> optimalStrategy;
	
	private int iterations =0;
	
	public BBSigmaSearch(DeceptionGame g){
		this.game = g;
	}
	
	public void solve() throws Exception{
		double start = System.currentTimeMillis();
		
		warmStart(200);
		
		//create root node
		BBSigmaNode root = new BBSigmaNode(game, 0);
		
		root.solve();
		
		ArrayList<BBSigmaNode> searchList = new ArrayList<BBSigmaNode>();
		
		if(root.isStrategyPure()){
			ObservableEU oeu = calculateMaxMinUtility(root.getDefenderStrategy());
			if(oeu.eu > globalLB){
				globalLB = oeu.eu;
				optimalStrategy = root.getDefenderStrategy();
			}
		}else{	
			searchList.add(root);
		}
		
		//System.out.println();
		//printCompactStrategy(root.getDefenderStrategy());
		//System.out.println();
		
		int index = 0;
		while(!searchList.isEmpty()){
			BBSigmaNode node = searchList.remove(0);
			
			if(node.getLB() < globalLB){
				//searchList.remove(0);
				continue;
			}
			
			//Need to create new children to explore, pick a variable n_k,tf to branch on
			//Find variable closest to 1 in strategy that has not been set
			BranchVariable var = node.expandChild();
			
			/**
			 * New code! Expand all children for a given system k
			 */
			//We will use BranchVariable to give us the system to expand on for exploratory nodes
//			for(ObservableConfiguration o : game.obs)
//				createChildNode(searchList, node, var.k, o);
			
			
			
			/**
			 * Old code for binary expansion
			 */
			//Create left and right child where we have n_k,tf <= 0 and n_k,tf >= 1
			binaryExpansion(searchList, node, var);
			

			//System.out.println();
			//System.out.println(searchList.toString());
			//System.out.println();
			
			//index++;
			//if(index > 6)
				//break;
			iterations++;
			if(iterations > 1 && iterations%30==0){
				System.out.println();
				System.out.println("Global LB: "+globalLB);
//				printCompactStrategy(optimalStrategy);
//				System.out.println();
//				System.out.println();
				System.out.println(searchList.toString());
				System.out.println();
			}
		}
		
		
		runtime = (System.currentTimeMillis()-start)/1000.0;
		
	}
	
	private void createChildNode(ArrayList<BBSigmaNode> searchList, BBSigmaNode node, Systems k, ObservableConfiguration o) throws Exception{
		//Create left and right child where we have n_k,tf <= 0 and n_k,tf >= 1
		BranchVariable var = new BranchVariable(k,o);
		Map<Systems, Map<ObservableConfiguration, Integer>> newConstraints = setConstraints(node.getConstraints(), var, 1);//new HashMap<Systems, Map<ObservableConfiguration, Integer>>(node.getConstraints());
		
		//System.out.println(newConstraints);
		
		//create child node and solve!
		BBSigmaNode child = new BBSigmaNode(game, newConstraints, node);
		
		child.solve();
		
		child.setNumOnes(node.getNumOnes()+1);
		
		//If we have pure strategies then fathom node and check if it gives a new global LB
		if(child.isStrategyPure()){
			ObservableEU oeu = calculateMaxMinUtility(child.getDefenderStrategy());
			if(oeu.eu > globalLB){
				globalLB = oeu.eu;
				optimalStrategy = child.getDefenderStrategy();
			}
		}else if(child.getLB() > globalLB){
			//searchList.add(left);	
			//insertNodeLevel(searchList, left);
			insertNodeLB(searchList, child);
		}
	}
	
	private void binaryExpansion(ArrayList<BBSigmaNode> searchList, BBSigmaNode node, BranchVariable var) throws Exception{
		//Create left and right child where we have n_k,tf <= 0 and n_k,tf >= 1
		Map<Systems, Map<ObservableConfiguration, Integer>> constraintsLeft = setConstraints(node.getConstraints(), var, 0);
		Map<Systems, Map<ObservableConfiguration, Integer>> constraintsRight = setConstraints(node.getConstraints(), var, 1);
		
		//System.out.println();
		//System.out.println(constraintsLeft.toString());//.get(var.k).get(var.o));
		//System.out.println(constraintsRight.toString());//.get(var.k).get(var.o));
		//System.out.println();
		
		//create left and right Child nodes and solve!
		BBSigmaNode left = new BBSigmaNode(game, constraintsLeft, node);
		BBSigmaNode right = new BBSigmaNode(game, constraintsRight, node);
		
		left.solve();
		right.solve();
		
		left.setNumOnes(node.getNumOnes());
		right.setNumOnes(node.getNumOnes()+1);
		
		//If we have pure strategies then fathom node and check if it gives a new global LB
		if(left.isStrategyPure()){
			ObservableEU oeu = calculateMaxMinUtility(left.getDefenderStrategy());
			if(oeu.eu > globalLB){
				globalLB = oeu.eu;
				optimalStrategy = left.getDefenderStrategy();
			}
		}else if(left.getLB() > globalLB){
			//searchList.add(left);	
			//insertNodeLevel(searchList, left);
			insertNodeLB(searchList, left);
		}
		//System.out.println("Left Level: "+left.getLevel()+" Utility: "+left.getLB());
		
		if(right.isStrategyPure()){ //At a leaf we should have a pure strategy! so don't need to check
			ObservableEU oeu = calculateMaxMinUtility(right.getDefenderStrategy());
			if(oeu.eu > globalLB){
				globalLB = oeu.eu;
				optimalStrategy = right.getDefenderStrategy();
			}
		}else if(right.getLB() > globalLB){
			//searchList.add(right);		
			//insertNodeLevel(searchList, left);
			insertNodeLB(searchList, right);		
		}
		//System.out.println("Right Level: "+right.getLevel()+" Utility: "+right.getLB());
		
		//System.out.println("Right Level: "+right.getLevel()+" Utility: "+right.getLB());
		
		//Collections.sort(searchList); //This makes it best first search
		//Might want to run depth first search for a certain number of iterations, then do best first
		
		
		//If leaf may need to update the global LB
		/*if(right.isLeaf()){
			if (right.getLB() > globalLB && right.isFeasible()) {
				globalLB = right.getLB();
				optimalStrategy = right.getDefenderStrategy();
			}
		}*/
	}
	
	private int insertNodeLB(ArrayList<BBSigmaNode> searchList, BBSigmaNode node){
		//System.out.println("Inserting by LB");
		//System.out.println(searchList.toString());
		//System.out.println(node.toString());
		//System.out.println();
		int insertIndex = searchList.size()-1;
		
		if(searchList.isEmpty()){
			searchList.add(node);
			return 0;
		}
			
		if(searchList.get(0).getLB() < node.getLB()){ //insert at front if level is greater than first element
			insertIndex = 0;
		}else{
			int start = searchList.size();
			int end = searchList.size();
			
			for(int i=0; i<searchList.size(); i++){
				if(searchList.get(i).getLB() <= node.getLB()){
					start = i;
					break;
				}
			}
			
			for(int i=start; i<searchList.size(); i++){
				if(searchList.get(i).getLB() < node.getLB()){ //go until we reach a node with a different resource number
					end = i;
					break;
				}
			}
			
			//System.out.println("Start: "+start+" End: "+end);
			//insertIndex = findInsertIndex(0, nodesToExplore.size()-1, child.getUpperBound());
			insertIndex = findInsertIndexOnes(searchList, node, start, end);
		}
		
		//System.out.println("Inserting: "+node.toString()+" at "+insertIndex);
	
		searchList.add(insertIndex, node);
		return 0;
		
	}
	
	private int findInsertIndexOnes(ArrayList<BBSigmaNode> searchList, BBSigmaNode node, int lowerIndex, int upperIndex){
		if(lowerIndex==upperIndex){
			return lowerIndex;
		}
		
		if(upperIndex <= lowerIndex){
			if(node.getNumOnes() > searchList.get(lowerIndex).getNumOnes()){
				return lowerIndex;
			}else{
				return lowerIndex+1;
			}
		}
		
		int midIndex = (upperIndex+lowerIndex)/2;
		
		if(searchList.get(midIndex).getNumOnes() == node.getNumOnes()){
			return midIndex;
		}
		
		if(searchList.get(midIndex).getNumOnes() < node.getNumOnes()){
			//System.out.println("Lower Insert");
			return findInsertIndexOnes(searchList, node, lowerIndex, midIndex-1);
		}

		//System.out.println("Upper Insert");
		return findInsertIndexOnes(searchList, node, midIndex+1, upperIndex);
		
	}
	
	private int insertNodeLevel(ArrayList<BBSigmaNode> searchList, BBSigmaNode node){
		int insertIndex = 0;
		
		if(searchList.isEmpty()){
			searchList.add(node);
			return 0;
		}
			
		if(searchList.get(0).getNumOnes() < node.getNumOnes()){ //insert at front if level is greater than first element
			insertIndex = 0;
		}else{
			int start = 0;
			int end = 0;
			
			for(int i=0; i<searchList.size(); i++){
				if(searchList.get(i).getNumOnes() == node.getNumOnes()){
					start = i;
					break;
				}
			}
			
			for(int i=start+1; i<searchList.size(); i++){
				if(searchList.get(i).getNumOnes() != node.getNumOnes()){ //go until we reach a node with a different resource number
					end = i;
					break;
				}
			}
			
			//insertIndex = findInsertIndex(0, nodesToExplore.size()-1, child.getUpperBound());
			insertIndex = findInsertIndex(searchList, node, start, end);
		}
	
		searchList.add(insertIndex, node);
		return 0;
		
	}
	
	private int findInsertIndex(ArrayList<BBSigmaNode> searchList, BBSigmaNode node, int lowerIndex, int upperIndex){
		if(lowerIndex==upperIndex)
			return lowerIndex;
		
		if(upperIndex <= lowerIndex){
			if(node.getLB() > searchList.get(lowerIndex).getLB()){
				return lowerIndex;
			}else{
				return lowerIndex+1;
			}
		}
		
		int midIndex = (upperIndex+lowerIndex)/2;
		
		if(searchList.get(midIndex).getLB() == node.getLB()){
			//System.out.println("Equal");
			return midIndex;
		}
		
		if(searchList.get(midIndex).getLB() < node.getLB()){
			//System.out.println("Lower Insert");
			return findInsertIndex(searchList, node, lowerIndex, midIndex-1);
		}

		//System.out.println("Upper Insert");
		return findInsertIndex(searchList, node, midIndex+1, upperIndex);
		
	}
	
	private Map<Systems, Map<ObservableConfiguration, Integer>> setConstraints(Map<Systems, Map<ObservableConfiguration, Integer>> parentConstraints, BranchVariable var, int value){
		Map<Systems, Map<ObservableConfiguration, Integer>> newConstraints = new HashMap<Systems, Map<ObservableConfiguration, Integer>>();
		for(Systems k : game.machines)
			newConstraints.put(k, new HashMap<ObservableConfiguration, Integer>());
		
		for(Systems k : parentConstraints.keySet()){
			for(ObservableConfiguration o : parentConstraints.get(k).keySet()){
				newConstraints.get(k).put(o, parentConstraints.get(k).get(o));
			}
		}
		
		newConstraints.get(var.k).put(var.o, value);
		if(value == 1){
			for(ObservableConfiguration o : game.obs){
				if(o.id == var.o.id)
					continue;
				newConstraints.get(var.k).put(o, 0);
			}
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
			
//			System.out.println();
//			
//			System.out.println(g.configs.size()+", "+g.obs.size()+", "+g.machines.size()+", "+solver.getDefenderUtility()+","+
//							solver.calculateMaxMinUtility(solver.getGreedyStrategy()).eu+", "+solver.getRuntime());
//			System.out.println();
//			
//			printCompactStrategy(solver.getGreedyStrategy(), g);
			
//			printStrategy2(solver.getGreedyStrategy());
		}
		
		
//		GreedyMaxMinSolver solver = new GreedyMaxMinSolver(game);
//		
//		solver.solve();
//		
//		globalLB = solver.getDefenderUtility();
//		Map<Systems, Map<ObservableConfiguration, Double>> strat = new HashMap<Systems, Map<ObservableConfiguration, Double>>();
//		for(Systems k : solver.getGreedyStrategy().keySet()){
//			strat.put(k, new HashMap<ObservableConfiguration, Double>());
//			for(ObservableConfiguration o : solver.getGreedyStrategy().get(k).keySet()){
//				strat.get(k).put(o, (double)solver.getGreedyStrategy().get(k).get(o));
//			}
//		}
//		optimalStrategy = strat;
		
		//System.out.println("Utility: "+globalLB);
		//System.out.println();
		
		
	}
	
	public Map<Systems, Map<ObservableConfiguration, Double>> getDefenderStrategy(){
		return optimalStrategy;
	}

	public double getRuntime(){
		return runtime;
	}
	
	public double getGlobalLB(){
		//Will be optimal utility at end of search
		return globalLB;
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

}
