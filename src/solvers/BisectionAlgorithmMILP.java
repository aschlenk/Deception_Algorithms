package solvers;

import java.util.Map;

import models.DeceptionGame;
import models.ObservableConfiguration;
import models.Systems;

public class BisectionAlgorithmMILP {
	
	private DeceptionGame game;

	private double epsilon = .001;
	
	private int iterations = 0;
	
	private double runtime;
	
	private Map<Systems, Map<ObservableConfiguration, Double>> defenderStrategy;
	
	private double UpperBound;
	private double LowerBound;
	private boolean lastFeasible;
	private double gapTolerance = 0.0;
	
	private double maxRuntime;

	/**
	 * Creates a new instance of the Bisection Algorithm solver for a deception game given as input. 
	 * @param g
	 */
	public BisectionAlgorithmMILP(DeceptionGame g){
		this.game = g;
	}
	
	public void solve() throws Exception{
		//System.out.println("Solving Bisection Algorithm");
		
		double lb = calculateLB();
		double ub = calculateUB(game);
		
		double alpha = (ub + lb)/2.0;
		double width = ub-lb;
		
		//System.out.println("UB: "+0+" LB: "+lb);
		
		double start = System.currentTimeMillis();
		
		//Bisection Algorithm; continue trying to find max until width is sufficiently (epsilon) small
		while(width > epsilon){
			//System.out.println("Alpha: "+alpha+" Width: "+width);
			
			boolean feasible;
			
			if(maxRuntime == 0)
				feasible = solveFeasibilityProblem(alpha, gapTolerance);
			else{
				double runtimeLeft = maxRuntime-((System.currentTimeMillis()-start)/1000.0);
				feasible = solveFeasibilityProblem(alpha, runtimeLeft, gapTolerance);
			}
			
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
			
			System.out.println("Current UB: "+ub+" Current LB: "+lb+" Current Runtime: "+((System.currentTimeMillis()-start)/1000.0));
			
			if((System.currentTimeMillis()-start)/1000.0 > maxRuntime){
				break;
			}
			
		}
		
		UpperBound = ub;
		LowerBound = lb;
		
		runtime = (System.currentTimeMillis()-start)/1000.0;	
		
		//printCompactStrategy(defenderStrategy);
		
//		System.out.println(game.configs.size()+", "+game.obs.size()+", "+game.machines.size()+", "+lb+", "+ub+", "+runtime+", "+iterations);
		
	}
	
	private boolean solveFeasibilityProblem(double alpha, double gapTolerance) throws Exception{
		//System.out.println("Solving Feasibility Problem");
		
		boolean feasible = false;
		
		//FeasibilityLP solver = new FeasibilityLP(game, alpha);
		FeasibilityMILP solver = new FeasibilityMILP(game, alpha);
		
		solver.setGapTolerance(gapTolerance);
		
		solver.solve();
		
		feasible = solver.getFeasible();
		
		if(feasible)
			defenderStrategy = solver.getDefenderStrategy();
		
		//clean up
		solver.deleteVars();
		
		return feasible;
	}
	
	private boolean solveFeasibilityProblem(double alpha, double maxRuntime, double gapTolerance) throws Exception{
		//System.out.println("Solving Feasibility Problem");
		
		boolean feasible = false;
		
		//FeasibilityLP solver = new FeasibilityLP(game, alpha);
		FeasibilityMILP solver = new FeasibilityMILP(game, alpha);
		
		solver.setGapTolerance(gapTolerance);
		
		solver.setMaxRuntime(maxRuntime);
		
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
	
	public static double calculateUB(DeceptionGame g){
		int totalU = 0;
		for(Systems k : g.machines){
			totalU += k.f.utility;
		}
		return ((double)(totalU)/(double)g.machines.size());
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
	
	public void setMaxRuntime(double max){
		maxRuntime = max;
	}
	
	public void setGapTolerance(double gap){
		gapTolerance = gap;
	}
	
	public void setEpsilon(double epsilon){
		this.epsilon = epsilon;
	}
	
}
