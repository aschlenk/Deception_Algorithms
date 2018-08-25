package solvers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import models.DeceptionGame;
import models.MarginalObservable;
import models.ObservableConfiguration;
import models.Systems;

public class BBMarginalNode implements Comparable{

private DeceptionGame game;
		
	private double epsilon = .0001;

	//variables to store information from running Bisection Algorithm
	private int iterations = 0;
	private double runtime;
	private double UpperBound;
	private double LowerBound;
	private Map<Systems, Map<ObservableConfiguration, Double>> defenderStrategy;
	
	private int childrenExplored = 0;
	
	//These two variables only for when the node is a leaf
	private boolean isLeaf;
	private boolean isFeasible = false;
	
	private boolean strategyPure = false;
	
	private BBMarginalNode parent;
	
	public Map<ObservableConfiguration, Integer> upperBoundConstraints;
	public Map<ObservableConfiguration, Integer> lowerBoundConstraints;
	
	public BBMarginalNode(DeceptionGame g){
		this.game = g;
		
		upperBoundConstraints = new HashMap<ObservableConfiguration, Integer>();
		
		lowerBoundConstraints = new HashMap<ObservableConfiguration, Integer>();
		
	}
	
	public BBMarginalNode(DeceptionGame g, BBMarginalNode parent){
		this.game = g;
		
		upperBoundConstraints = new HashMap<ObservableConfiguration, Integer>();
		for(ObservableConfiguration o : parent.upperBoundConstraints.keySet()) //enforce all constraints from parent in child node
			upperBoundConstraints.put(o, parent.upperBoundConstraints.get(o));
		
		lowerBoundConstraints = new HashMap<ObservableConfiguration, Integer>();
		for(ObservableConfiguration o : parent.lowerBoundConstraints.keySet()) //enforce all constraints from parent in child node
			lowerBoundConstraints.put(o, parent.lowerBoundConstraints.get(o));
		
		
	}
	
	public BBMarginalNode(DeceptionGame g, BBMarginalNode parent, Map<ObservableConfiguration, Integer> constraintsUpper, Map<ObservableConfiguration, Integer> constraintsLower){
		this.game = g;
		this.parent = parent;
		
		upperBoundConstraints = new HashMap<ObservableConfiguration, Integer>();
		for(ObservableConfiguration o : constraintsUpper.keySet()) //enforce all constraints from parent in child node
			upperBoundConstraints.put(o, constraintsUpper.get(o));
		
		lowerBoundConstraints = new HashMap<ObservableConfiguration, Integer>();
		for(ObservableConfiguration o : constraintsLower.keySet()) //enforce all constraints from parent in child node
			lowerBoundConstraints.put(o, constraintsLower.get(o));
		
		
	}

	public void solve() throws Exception{
//		System.out.println("Solving Node");
		
		BisectionAlgorithm alg = new BisectionAlgorithm(game, upperBoundConstraints, lowerBoundConstraints);
		
		alg.solve();
		
		defenderStrategy = alg.getDefenderStrategy();
		
//		System.out.println(upperBoundConstraints);
//		System.out.println(lowerBoundConstraints);
//		
//		System.out.println();
//		printCompactStrategy(defenderStrategy);
//		System.out.println();
//		printStrategy(defenderStrategy);
		
		LowerBound = alg.getLB();
		UpperBound = alg.getUB();
		
		if(defenderStrategy != null)
			strategyPure = checkIfStrategyPure();
		//If strategy is pure we should never explore any more cuts for this node!
//		System.out.println("Defender Payoff: "+alg.getUB());
		
		
	}
	
	public MarginalObservable getNextChild(){
		if(strategyPure){
			System.out.println("This node is a pure strategy!");
			return null;
		}
		
//		System.out.println(defenderStrategy);
		
		//Need to find the observable to branch on!
		//1. First case we consider will get the observable with N_o that is greatest and branch on it
		double max = 0;
		ObservableConfiguration childConfig = null;
		for(ObservableConfiguration o : game.obs){
			double sum =0;
			for(Systems k : defenderStrategy.keySet()){
				sum += defenderStrategy.get(k).get(o);
			}
//			System.out.println("Checking Sum: "+sum);
			//First check if sum is integer, if it is don't put it as max
			if(Math.abs((Math.round(sum) - sum)) < .001 ){
				continue;
			}
			if(sum > max){
				max = sum;
				childConfig = o;
			}
		}
		
		MarginalObservable mo = null;
		if(childConfig != null)
			mo = new MarginalObservable(childConfig, max);
		else{ //This means all sums are integer but defender strategy is not a pure
			
			
		}
		
		return mo;
	}
	
	public Map<ObservableConfiguration, Integer> getBounds(){
		Map<ObservableConfiguration, Integer> bounds = new HashMap<ObservableConfiguration, Integer>();
		
		for(ObservableConfiguration o : game.obs){
			double sum =0;
			for(Systems k : defenderStrategy.keySet()){
				sum += defenderStrategy.get(k).get(o);
			}
			bounds.put(o, (int)Math.round(sum));
		}
		
		return bounds;
	}
	
	private boolean checkIfStrategyPure(){
		for(Systems k : defenderStrategy.keySet()){
			for(ObservableConfiguration o : defenderStrategy.get(k).keySet()){
				if(Math.abs(Math.round(defenderStrategy.get(k).get(o)) - defenderStrategy.get(k).get(o)) >= .0001)
					return false;
			}
		}
		return true;
	}
	
	public boolean isStrategyPure(){
		return strategyPure;
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
	
	public Map<ObservableConfiguration, Integer> getUpperConstraints(){
		return upperBoundConstraints;
	}

	public Map<ObservableConfiguration, Integer> getLowerConstraints(){
		return lowerBoundConstraints;
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
	
	@Override
	public int compareTo(Object o) {
		BBMarginalNode n1 = (BBMarginalNode) o;
		if(this.LowerBound > n1.LowerBound){
			return -1;
		}else if(this.LowerBound < n1.LowerBound){
			return 1;
		}else{
			return 0;
		}
	}
	
	public String toString(){
		return "UpperConstraints: "+upperBoundConstraints+" LowerConstraints: "+lowerBoundConstraints+" : "+LowerBound;
	}
	
	public static void printStrategy(Map<Systems, Map<ObservableConfiguration, Double>> strat){
		for(Systems k : strat.keySet()){
			System.out.print("K"+k.id+": ");
			for(ObservableConfiguration o : strat.get(k).keySet()){
				System.out.print("TF"+o.id+" : "+strat.get(k).get(o)+" ");
			}
			System.out.println();
		}
	}
	
}
