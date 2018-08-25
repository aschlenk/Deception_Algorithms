package solvers;

import java.util.List;
import java.util.Map;

import models.DeceptionGame;
import models.ObservableConfiguration;
import models.Systems;

public class BisectionAlgorithm {

	private DeceptionGame game;
	
	private Map<ObservableConfiguration, Integer> constraints = null;
	private Map<ObservableConfiguration, Integer> upperConstraints = null;
	private Map<ObservableConfiguration, Integer> lowerConstraints = null;
	private Map<Systems, Map<ObservableConfiguration, Integer>> fixedConstraints = null;

	private Map<Systems, ObservableConfiguration> setMaskings = null;
	
	private double epsilon = .001;
	
	private int iterations = 0;
	
	private double runtime;
	
	private Map<Systems, Map<ObservableConfiguration, Double>> defenderStrategy;
	
	private double UpperBound;
	private double LowerBound;
	private boolean lastFeasible;
	
	public BisectionAlgorithm(DeceptionGame g){
		this.game = g;
	}
	
	public BisectionAlgorithm(DeceptionGame g, Map<Systems, ObservableConfiguration> setMaskings){
		this.game = g;
		this.setMaskings = setMaskings;		
	}
	
//	public BisectionAlgorithm(DeceptionGame g, Map<ObservableConfiguration, Integer> constraints){
//		this.game = g;
//		this.constraints = constraints;
//	}
	
	public BisectionAlgorithm(DeceptionGame g, Map<Systems, Map<ObservableConfiguration, Integer>> constraints, boolean doesntmatter){
		this.game = g;
		this.fixedConstraints = constraints;
	}
	
	public BisectionAlgorithm(DeceptionGame g, Map<ObservableConfiguration, Integer> upperConstraints, Map<ObservableConfiguration, Integer> lowerConstraints){
		this.game = g;
		this.upperConstraints = upperConstraints;
		this.lowerConstraints = lowerConstraints;
	}
	
	public void solve() throws Exception{
		//System.out.println("Solving Bisection Algorithm");
		
		
		
		double lb = calculateLB();
		double ub = 0;
		
		double alpha = (ub + lb)/2.0;
		double width = ub-lb;
		
		//System.out.println("UB: "+0+" LB: "+lb);
		
		double start = System.currentTimeMillis();
		
		//check if LB is possible
		boolean feasible = solveFeasibilityProblem(lb);
		
		//Bisection Algorithm; continue trying to find max until width is sufficiently (epsilon) small
		while(width > epsilon){
			//System.out.println("Alpha: "+alpha+" Width: "+width);
			
			feasible = solveFeasibilityProblem(alpha);
			
			//System.out.println("Feasible: "+feasible);
			
			if(feasible){
				lb = alpha;
			}else{
				ub = alpha;
			}
			
			alpha = (ub + lb)/2.0;
			width = ub-lb;
			
			//System.out.println();
			//System.out.println("Alpha: "+alpha+" Width: "+width);
			
			lastFeasible = feasible;
			iterations++;
		}
		
		UpperBound = ub;
		LowerBound = lb;
		
		runtime = (System.currentTimeMillis()-start)/1000.0;	
		
		//printCompactStrategy(defenderStrategy);
		
//		System.out.println(game.configs.size()+", "+game.obs.size()+", "+game.machines.size()+", "+lb+", "+ub+", "+runtime+", "+iterations);
		
	}
	
	private boolean solveFeasibilityProblem(double alpha) throws Exception{
		//System.out.println("Solving Feasibility Problem");
		
		boolean feasible = false;
		
		//FeasibilityLP solver = new FeasibilityLP(game, alpha);
		FeasibilityLP solver = null;
		if(setMaskings != null)
			solver = new FeasibilityLP(game, alpha, setMaskings);
			
//		if(constraints != null)
//			solver = new FeasibilityLP(game, alpha, constraints);

		if(fixedConstraints != null)
			solver = new FeasibilityLP(game, alpha, fixedConstraints, true);
		
		if(lowerConstraints != null || upperConstraints != null)
			solver = new FeasibilityLP(game, alpha, upperConstraints, lowerConstraints);
		
		if(constraints == null && fixedConstraints == null && lowerConstraints == null && upperConstraints == null && setMaskings == null)
			solver = new FeasibilityLP(game, alpha);
		//System.out.println(constraints.toString());
		
		solver.solve();
		
		feasible = solver.getFeasible();
		
		if(feasible)
			defenderStrategy = solver.getDefenderStrategy();
		
		//clean up
		solver.deleteVars();
		
		return feasible;
	}
	
	private double calculateLB(){
		double lb = 0;
		for(Systems k : game.machines){
			if(k.f.utility < lb)
				lb = k.f.utility;
		}
		return lb;
	}
	
	public double getRuntime(){
		return runtime;
	}
	
	public int getIterations(){
		return iterations;
	}
	
	public double getUB(){
		return UpperBound;
	}
	
	public double getLB(){
		return LowerBound;
	}
	
	public boolean getLastFeasible(){
		return lastFeasible;
	}
	
	public Map<Systems, Map<ObservableConfiguration, Double>> getDefenderStrategy(){
		return defenderStrategy;
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
