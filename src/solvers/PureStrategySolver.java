package solvers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;
import models.DeceptionGame;
import models.ObservableConfiguration;
import models.Systems;

public class PureStrategySolver {
	
	private DeceptionGame model;
	
	private IloCplex cplex;
	
	private Map<Systems, Map<ObservableConfiguration, IloNumVar>> sigmaMap;
	private IloNumVar dutility;
	//private Map<ObservableConfiguation, IloNumVar> dMap;
	
	private Map<Systems, Map<ObservableConfiguration, Double>> defenderStrategy;
	
	private List<IloRange> constraints;
	
	private static final int MM = 100000;
	
	private double minUtility = Double.MAX_VALUE;
	
	private double runtime;
	
	private double defenderUtility;
	
	private Map<ObservableConfiguration, Integer> bounds;
	private boolean feasible = true;
	
	public PureStrategySolver(DeceptionGame g, Map<ObservableConfiguration, Integer> bounds){
		this.model = g;
		this.bounds = bounds;
	}
	
	private void loadProblem() throws IloException{
		sigmaMap = new HashMap<Systems, Map<ObservableConfiguration, IloNumVar>>();
		
		cplex = new IloCplex();
		cplex.setName("DECEPTION");
		//cplex.setParam(IloCplex.IntParam.RootAlg, IloCplex.Algorithm.Barrier);
		//cplex.setParam(IloCplex.IntParam.BarCrossAlg, IloCplex.Algorithm.None);
		//cplex.setParam(IloCplex.IntParam.BarAlg, 0);
		cplex.setOut(null);
		
		initVars();
		initConstraints();
		initObjective();
	}
	
	public void solve() throws Exception{
		defenderStrategy = new HashMap<Systems, Map<ObservableConfiguration, Double>>();
		//riskCategoryCoverage = new HashMap<AlertLevel, Map<Base, Double>>();
		//defenderPayoffs = new HashMap<AlertLevel, Double>();
		//adversaryPayoffs = new HashMap<AlertLevel, Double>();
		//adversaryStrategies = new HashMap<AlertLevel, Map<Base, AttackMethod>>();
	
		double start = System.currentTimeMillis();
		
		loadProblem();
			
		cplex.solve();
			
		if(!cplex.isPrimalFeasible()){
			//System.out.println("Pure Strategy is Infeasible");
			//writeProblem("Infeasible.lp");
			//writeProblem("Infeasible.sol.txt");
			//throw new Exception("Infeasible.");
			feasible = false;
		}
		
		//writeProblem("CDG.lp");
			
		if(feasible){
			defenderStrategy = getDefenderStrategy();
			defenderUtility = getDefenderPayoff();

			printCompactStrategy(defenderStrategy);
			System.out.println("Utility: "+defenderUtility);
		}
		runtime = (System.currentTimeMillis()-start)/1000.0;
		
		//printStrategy(defenderStrategy);
		//printExpectedUtility(defenderStrategy);
		//riskCategoryCoverage = calculateRiskCategoryCoverage();
		//defenderPayoffs = getDefenderPayoffs();
		//adversaryPayoffs = getAdversaryPayoffs();
		//adversaryStrategies = getAdversaryStrategies();
		
		//System.out.println("Defender Utility: "+getDefenderPayoff());

		//System.out.println("Adversary Utility: "+getAdversaryPayoff());
		
		//System.out.println("Runtime: "+runtime);
		
	}
	
	private void initVars() throws IloException{
		List<IloNumVar> varList = new ArrayList<IloNumVar>();
		
		//Create variables for defender's strategy
		for(Systems k : model.machines){
			sigmaMap.put(k, new HashMap<ObservableConfiguration, IloNumVar>());
			
			for(ObservableConfiguration o : model.obs){
				IloNumVar var = cplex.numVar(0, 1, IloNumVarType.Int, "sigma_k" +  k.id + "_o" + o.id);
				
				sigmaMap.get(k).put(o, var);
				varList.add(var);
			}
		}
		
		for(Systems k : model.machines){
			if(minUtility > k.f.utility){
				minUtility = k.f.utility;
			}
		}
		
		dutility = cplex.numVar(minUtility, 0, IloNumVarType.Float, "d");
		
		varList.add(dutility);
		
		IloNumVar[] v = new IloNumVar[varList.size()];

		cplex.add(varList.toArray(v));
	}
	
	private void initConstraints() throws IloException{
		constraints = new ArrayList<IloRange>();
		
		//Need to add utility constraints
		setUtilityConstraints();
		//Need to set single observable per system constraint
		sumObservableConfigurationRow();
		//Need to set bounds
		setObservableBounds();
		//Set 0 value constraints for observables that can't be assigned to a system
		setZeroConstraints();
		
		IloRange[] c = new IloRange[constraints.size()];

		cplex.add(constraints.toArray(c));
	}

	private void initObjective() throws IloException{
		//Should be correct objective
		IloNumExpr expr = dutility;
		
		cplex.addMaximize(expr);
	}

	private void setUtilityConstraints() throws IloException {
		//This is going to be in terms of the z contraints for each \tilde{f} \in \tilde{F}
		for(ObservableConfiguration o : model.obs){
			IloNumExpr expr = cplex.constant(0.0);
			expr = cplex.sum(expr, cplex.prod(bounds.get(o), dutility));
			
			for(Systems k : model.machines){
				expr = cplex.diff(expr, cplex.prod(k.f.utility, sigmaMap.get(k).get(o)));
			}
			
			constraints.add(cplex.le(expr, 0.0, "UTILITY_TF_"+o.id));
		}
		
	}
	
	private void setObservableBounds() throws IloException{
		for(ObservableConfiguration o : bounds.keySet()){
			IloNumExpr expr = cplex.constant(0.0);
			
			for(Systems k : model.machines){
				expr = cplex.sum(expr, sigmaMap.get(k).get(o));
			}
			constraints.add(cplex.eq(expr, bounds.get(o), "EQUAL_o"+o.id));
		}
		
		
	}

	private void setZeroConstraints() throws IloException {
		//Might be able to do this a lot more efficiently, but it should work for now
		for(Systems k : model.machines){
			for(ObservableConfiguration o : model.obs){
				if(!o.configs.contains(k.f)){
				//if(!k.f.obsConfigs.contains(o)){
					IloNumExpr expr = sigmaMap.get(k).get(o);
					
					constraints.add(cplex.eq(expr, 0, "ZERO_K"+k.id+"_O"+o.id));
				}
			}
		}
	}

	private void sumObservableConfigurationRow() throws IloException {
		for(Systems k : model.machines){
			IloNumExpr expr = cplex.constant(0.0);
			
			for(ObservableConfiguration o : model.obs){
				expr = cplex.sum(expr, sigmaMap.get(k).get(o));
			}
			
			constraints.add(cplex.eq(1.0, expr, "EQ_K"+k.id));
		}
	}

	public void writeProblem(String filename) throws IloException{
		cplex.exportModel(filename);
	}
	
	public void writeSolution(String filename) throws IloException{
		cplex.writeSolution(filename);
	}
	
	public double getDefenderPayoff() throws IloException{
		double utility = cplex.getValue(dutility);
		
		return utility;
	}
	
	public double getAdversaryPayoff() throws IloException{
		return -1.0*getDefenderPayoff();
	}
	
	public Map<Systems, Map<ObservableConfiguration, Double>> getDefenderStrategy() throws UnknownObjectException, IloException{
		Map<Systems, Map<ObservableConfiguration, Double>> strat = new HashMap<Systems, Map<ObservableConfiguration, Double>>();
		
		for(Systems k : sigmaMap.keySet()){
			strat.put(k, new HashMap<ObservableConfiguration, Double>());
			for(ObservableConfiguration o : sigmaMap.get(k).keySet()){
				strat.get(k).put(o, cplex.getValue(sigmaMap.get(k).get(o))); 
				//System.out.println(k.id+"  :  "+o.id+"  :  "+((int) cplex.getValue(sigmaMap.get(k).get(o))));
			}
		}
		
		return strat;
	}
	
	public void printStrategy(Map<Systems, Map<ObservableConfiguration, Double>> strat){
		for(Systems k : strat.keySet()){
			System.out.print("K"+k.id+": ");
			for(ObservableConfiguration o : strat.get(k).keySet()){
				System.out.print("TF"+o.id+" : "+strat.get(k).get(o)+" ");
			}
			System.out.println();
		}
	}
	
	public void printCompactStrategy(Map<Systems, Map<ObservableConfiguration, Double>> strat){
		for(Systems k : strat.keySet()){
			System.out.print("K"+k.id+": ");
			for(ObservableConfiguration o : strat.get(k).keySet()){
				if(strat.get(k).get(o)>0)
					System.out.print("TF"+o.id+" : "+strat.get(k).get(o)+" ");
			}
			System.out.println();
		}
	}
	
	public double getRuntime(){
		return runtime;
	}
	
	public double getUtility(){
		return defenderUtility;
	}
	
	public void printExpectedUtility(Map<Systems, Map<ObservableConfiguration, Double>> strategy){
		double expectedU = 0;
		double total = 0;;
		for(ObservableConfiguration o : model.obs){
			for(Systems k : strategy.keySet()){
				expectedU += strategy.get(k).get(o)*k.f.utility;
				total += strategy.get(k).get(o);
			}
			System.out.println("EU(o"+o.id+"): "+(expectedU/total));
			expectedU = 0;
			total=0;
		}
		
	}
	
	public boolean isFeasible(){
		return feasible;
	}
	
}
