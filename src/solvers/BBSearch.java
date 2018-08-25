package solvers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import models.*;

public class BBSearch {
	
	DeceptionGame game;
	
	private double globalLB = -1000;
	
	private double runtime;
	
	private Map<Systems, Map<ObservableConfiguration, Double>> optimalStrategy;
	
	public BBSearch(DeceptionGame g){
		this.game = g;
	}
	
	public void solve() throws Exception{
		double start = System.currentTimeMillis();
		
		warmStart();
		
		//create root node
		BBNode root = new BBNode(game, 0);
		
		root.solve();
		
		ArrayList<BBNode> searchList = new ArrayList<BBNode>();
		searchList.add(root);
		
		printCompactStrategy(root.getDefenderStrategy());
		
		int index = 0;
		while(!searchList.isEmpty()){
			//should be solved, but now we need to expand the children
			BBNode n = searchList.get(0);
			
			System.out.println("Exploring: "+n.getLB()+" Global LB: "+globalLB);
			
			if(n.getLB() < globalLB){
				searchList.remove(0);
				continue;
			}
			
			int childLevel = n.getLevel()+1;
			//int childConstraint = n.nextChild();
			int childConstraint = n.heuristicChild();
			
			//cannot expand anymore children
			if(childConstraint == -1){
				searchList.remove(0);
				continue;
			}
			
			Map<ObservableConfiguration, Integer> assignmentConstraints = new HashMap<ObservableConfiguration, Integer>(n.getConstraints());
			
			ObservableConfiguration childObs = findChildObservable(childLevel);
				
			assignmentConstraints.put(childObs, childConstraint);
			
			System.out.println();
			System.out.println("Level: "+childLevel+" Constraint: "+childConstraint);
			System.out.println(assignmentConstraints.toString());
			System.out.println();
			
			//create Child node and solve!
			BBNode child = new BBNode(game, assignmentConstraints, childLevel);
			
			child.solve();
			
			if(child.getLB() > globalLB && !child.isLeaf()){
				searchList.add(child);
				
				Collections.sort(searchList);
				
				System.out.println("Level: "+childLevel+" Utility: "+child.getLB());
				
			}
			
			//If leaf may need to update the global LB; could also save these strategies to do a convex hull
			if(child.isLeaf()){
				if (child.getLB() > globalLB && child.isFeasible()) {
					globalLB = child.getLB();
					optimalStrategy = child.getDefenderStrategy();
				}
			}
		}
		
		runtime = (System.currentTimeMillis()-start)/1000.0;
		
	}
	
	private void warmStart(){
		//run Maximin Solver before creating root
		//Use as global lower bound and the defender strategy
		//System.out.println("Runnning Greedy Max Min Solver");
		
		GreedyMaxMinSolver solver = new GreedyMaxMinSolver(game);
		
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
		
		//System.out.println("Utility: "+globalLB);
		//System.out.println();
		
		
	}
	
	private ObservableConfiguration findChildObservable(int childLevel){
		ObservableConfiguration childObs = null;
		
		for(ObservableConfiguration o : game.obs){
			if(o.id == childLevel){
				childObs = o;
				break;
			}
		}
		
		return childObs;
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
}
