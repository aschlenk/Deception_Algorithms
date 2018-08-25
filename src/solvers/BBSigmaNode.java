package solvers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import models.BranchVariable;
import models.DeceptionGame;
import models.ObservableConfiguration;
import models.Systems;

public class BBSigmaNode implements Comparable{
	
	private DeceptionGame game;
	
	private int level;
	private int numOnes;
	
	//Should be a 0-1 matrix for all variables we have set in the path
	private Map<Systems, Map<ObservableConfiguration, Integer>> constraints;
	private double epsilon = .0001;

	//variables to store information from running Bisection Algorithm
	private int iterations = 0;
	private double runtime;
	private double UpperBound;
	private double LowerBound;
	private Map<Systems, Map<ObservableConfiguration, Double>> defenderStrategy;
	
	private int childAssignmentConstraint = -1;
	private int maxAssignable;
	private int childrenExplored = 0;
	
	//These two variables only for when the node is a leaf
	private boolean isLeaf;
	private boolean isFeasible = false;
	
	private boolean childExplored = false;
	private boolean strategyPure = false;
	
	private BBSigmaNode parent;
	
	private Map<Systems, ArrayList<ObservableConfiguration>> branchVariables;
	
	public BBSigmaNode(DeceptionGame g, int level){
		this.game = g;
		this.level = level;
		
		constraints = new HashMap<Systems, Map<ObservableConfiguration, Integer>>();
		for(Systems k : game.machines)
			constraints.put(k, new HashMap<ObservableConfiguration, Integer>());
		
		branchVariables = new HashMap<Systems, ArrayList<ObservableConfiguration>>();
		for(Systems k : game.machines)
			branchVariables.put(k, new ArrayList<ObservableConfiguration>());
		
		isLeaf = false;
		numOnes = 0;
	}

	public BBSigmaNode(DeceptionGame g, Map<Systems, Map<ObservableConfiguration, Integer>> constraints, BBSigmaNode parent){
		this.game = g;
		this.constraints = constraints;
		//this.constraints = new HashMap<ObservableConfiguration, Integer>(contraints);
		this.parent = parent;
		this.level = parent.getLevel()+1;
		branchVariables = new HashMap<Systems, ArrayList<ObservableConfiguration>>();//parent.getBranchVariables());
		for(Systems k : game.machines)
			branchVariables.put(k, new ArrayList<ObservableConfiguration>());
		
		for(Systems k : parent.getBranchVariables().keySet()){
			for(ObservableConfiguration o : parent.getBranchVariables().get(k)){
				branchVariables.get(k).add(o);
			}
		}
		//I don't think this condition matter anymore... 
		//this condition needs to be changed such that we are only at a leaf if we have set |K| variables >= 1
		/*if(level == game.obs.size())
			isLeaf = true;
		else{
			isLeaf = false;
		}*/
	}
	
	/**
	 * Where all the magic happens
	 * @throws Exception 
	 */
	public void solve() throws Exception{
		//Run Bisection algorithm for node given the constraints on values of n_k,tf
		//if(!isLeaf){
			BisectionAlgorithm alg = new BisectionAlgorithm(game, constraints, true);
			
			alg.solve();
			
			defenderStrategy = alg.getDefenderStrategy();
			
			//System.out.println();
			//printCompactStrategy(defenderStrategy);
			//System.out.println();
			
			LowerBound = alg.getLB();
			UpperBound = alg.getUB();
			
			if(defenderStrategy != null)
				strategyPure = checkIfStrategyPure();
				
			//shouldn't have to deal with special case for leaf
		/*}else{
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
		}*/
		
		
	}
	
	private boolean checkIfStrategyPure(){
		for(Systems k : defenderStrategy.keySet()){
			for(ObservableConfiguration o : defenderStrategy.get(k).keySet()){
				if(defenderStrategy.get(k).get(o) >= .00001 && defenderStrategy.get(k).get(o) <= .99999)
					return false;
			}
		}
		return true;
	}
	
	public BranchVariable expandChild(){
		double gap = 1;
		Systems sys = null;
		ObservableConfiguration obs = null;
		
		//Find n_k,tf that is closest to 1
		for(Systems k : game.machines){
			for(ObservableConfiguration o : game.obs){
				if(branchVariables.get(k).contains(o))
					continue;
				
				if(defenderStrategy.get(k).get(o) >= 1.0) //only branch on non-integer values
					continue;
				
				double tempGap = 1-defenderStrategy.get(k).get(o);
				if(tempGap < gap){
					gap = tempGap;
					sys = k;
					obs = o;
				}
			}
		}
		
		BranchVariable v = new BranchVariable(sys, obs);
		
		return v;
	}
	
	public int getLevel(){
		return level;
	}

	@Override
	public int compareTo(Object o) {
		BBSigmaNode n1 = (BBSigmaNode) o;
		if(this.LowerBound > n1.LowerBound){
			return -1;
		}else if(this.LowerBound < n1.LowerBound){
			return 1;
		}else{
			return 0;
		}
	}
	
	public String toString(){
		return "O"+level+" : NumOnes "+numOnes+" : "+LowerBound;
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
	
	public Map<Systems, Map<ObservableConfiguration, Integer>> getConstraints(){
		return constraints;
	}
	
	public Map<Systems, ArrayList<ObservableConfiguration>> getBranchVariables(){
		return branchVariables;
	}
	
	public boolean isStrategyPure(){
		return strategyPure;
	}
	
	public int getNumOnes(){
		return numOnes;
	}
	
	public void setNumOnes(int num){
		numOnes = num;
	}

}
