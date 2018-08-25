package solvers;

import java.util.HashMap;
import java.util.Map;

import models.DeceptionGame;
import models.ObservableConfiguration;
import models.Systems;

public class BBNode implements Comparable {

	private DeceptionGame game;
	
	private int level;
	
	private Map<ObservableConfiguration, Integer> constraints;
	private double epsilon = .0001;

	private int iterations = 0;
	private double runtime;
	private double UpperBound;
	private double LowerBound;
	
	private int childAssignmentConstraint = -1;
	private int maxAssignable;
	private int childrenExplored = 0;
	
	private boolean isLeaf;
	private boolean isFeasible = false;
	
	private boolean childExplored = false;

	private Map<Systems, Map<ObservableConfiguration, Double>> defenderStrategy;
	
	public BBNode(DeceptionGame g, int level){
		this.game = g;
		this.level = level;
		constraints = new HashMap<ObservableConfiguration, Integer>();
		maxAssignable = calculateMaxAssignable(level+1);
		isLeaf = false;
	}

	public BBNode(DeceptionGame g, Map<ObservableConfiguration, Integer> constraints, int level){
		this.game = g;
		this.constraints = constraints;
		//this.constraints = new HashMap<ObservableConfiguration, Integer>(contraints);
		this.level = level;
		if(level == game.obs.size())
			isLeaf = true;
		else{
			isLeaf = false;
			maxAssignable = calculateMaxAssignable(level+1);
		}
	}
	
	
	public void solve() throws Exception{
		if(!isLeaf){
			BisectionAlgorithm alg = new BisectionAlgorithm(game);//, constraints);
			
			alg.solve();
			
			defenderStrategy = alg.getDefenderStrategy();
			
			//printCompactStrategy(defenderStrategy);
			
			LowerBound = alg.getLB();
			UpperBound = alg.getUB();
		}else{
			//System.out.println(constraints.toString());
			//System.out.println();
			PureStrategySolver solver = new PureStrategySolver(game, constraints);
			solver.solve();
			
			if(solver.isFeasible()){
				isFeasible = true;
				//System.out.println("I am feasible "+solver.getDefenderPayoff());
				defenderStrategy = solver.getDefenderStrategy();
				
				LowerBound = solver.getDefenderPayoff();
				UpperBound = solver.getDefenderPayoff();
			}
		}
	}
	
	public int nextChild(){		
		if(childAssignmentConstraint == maxAssignable)
			return -1;
		else
			return ++childAssignmentConstraint;
	}
	
	public int heuristicChild(){
		//Find the assignment constraint for the child level

		//System.out.println();
		//System.out.println("O"+(level+1)+" Assignment: "+childAssignmentConstraint+" Max: "+maxAssignable+" Children Explored: "+childrenExplored);
		//System.out.println();
		
		if(childExplored == false){
			double assignment = 0;
			for(Systems k : game.machines){
				for(ObservableConfiguration o : game.obs){
					if(o.id == level+1){
						assignment += defenderStrategy.get(k).get(o);
					}
				}
			}
			System.out.println("Assignment: "+assignment);
			childAssignmentConstraint = (int) Math.round(assignment);
			childrenExplored++;
			childExplored = true;
			return childAssignmentConstraint;
		}else{
			return -1;
		}
			/*if(childAssignmentConstraint == maxAssignable)
				childAssignmentConstraint = 0;
			else
				childAssignmentConstraint++;
			
			childrenExplored++;
			
			if(childrenExplored > maxAssignable+1) //should not be able to explore anymore children
				return -1;
			
			return childAssignmentConstraint;
		}*/
	}
	
	//Create heuristic expansion
	
	public Map<ObservableConfiguration, Integer> getConstraints(){
		return constraints;
	}
	
	public int getLevel(){
		return level;
	}

	@Override
	public int compareTo(Object o) {
		BBNode n1 = (BBNode) o;
		if(this.LowerBound > n1.LowerBound){
			return -1;
		}else if(this.LowerBound < n1.LowerBound){
			return 1;
		}else{
			return 0;
		}
	}
	
	public String toString(){
		return "O"+level+" : "+LowerBound;
	}
	
	private int calculateMaxAssignable(int observable){
		int assignable = 0;
		ObservableConfiguration o = null;
		for(ObservableConfiguration o1 : game.obs){
			if(o1.id == observable){
				o = o1;
			}
		}
		
		for(Systems k : game.machines){
			if(o.configs.contains(k.f))
				assignable++;
		}
		return assignable;		
	}
	
	public boolean isLeaf(){
		return isLeaf;
	}
	
	public double getLB(){
		return LowerBound;
	}
	
	public double getUB(){
		return UpperBound;
	}
	
	public Map<Systems, Map<ObservableConfiguration, Double>> getDefenderStrategy(){
		return defenderStrategy;
	}
	
	public boolean isFeasible(){
		return isFeasible;
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
