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

public class LinearGameSolver {
	
	private DeceptionGame model;
	
	private IloCplex cplex;
	
	private Map<Systems, Map<ObservableConfiguration, IloNumVar>> sigmaMap;
	private Map<Systems, Map<ObservableConfiguration, IloNumVar>> zMap;
	private IloNumVar dutility;
	//private Map<ObservableConfiguation, IloNumVar> dMap;
	
	private Map<Systems, Map<ObservableConfiguration, Double>> defenderStrategy;
	
	private List<IloRange> constraints;
	
	private static final int MM = 100000;
	
	private double minUtility = Double.MAX_VALUE;
	
	private double runtime;
	
	private double defenderUtility;
	
	public LinearGameSolver(DeceptionGame g){
		this.model = g;
	}
	
	private void loadProblem() throws IloException{
		sigmaMap = new HashMap<Systems, Map<ObservableConfiguration, IloNumVar>>();
		zMap = new HashMap<Systems, Map<ObservableConfiguration, IloNumVar>>();
		
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
			writeProblem("Infeasible.lp");
			//writeProblem("Infeasible.sol.txt");
			throw new Exception("Infeasible.");
		}
		
		writeProblem("CDG.lp");
			
		defenderStrategy = getDefenderStrategy();
		defenderUtility = getDefenderPayoff();
		
		runtime = (System.currentTimeMillis()-start)/1000.0;
		
		//printStrategy(defenderStrategy);
//		printCompactStrategy(defenderStrategy);
		printCompactStrategy(defenderStrategy, model);
		printExpectedUtility(defenderStrategy);
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
				IloNumVar var = cplex.numVar(0, 1, IloNumVarType.Float, "n_k" +  k.id + "_o" + o.id);
				
				sigmaMap.get(k).put(o, var);
				varList.add(var);
			}
		}
		
		//create z variables
		for(Systems k : model.machines){
			zMap.put(k, new HashMap<ObservableConfiguration, IloNumVar>());
			
			for(ObservableConfiguration o : model.obs){
				//TODO: check this value! Might be better bounds although it might not matter
				IloNumVar var = cplex.numVar(-MM, MM, IloNumVarType.Float, "Z_k" +  k.id + "_o" + o.id); 
				
				zMap.get(k).put(o, var);
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
		//Need to set z constraints
		setZConstraints();
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
			
			for(Systems k : model.machines){
				expr = cplex.sum(expr, cplex.diff(zMap.get(k).get(o), cplex.prod(k.f.utility, sigmaMap.get(k).get(o))));
			}
			
			constraints.add(cplex.le(expr, 0.0, "UTILITY_TF_"+o.id));
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

	private void setZConstraints() throws IloException {
		//Four constraints to add for each z variable in zMap
		
		//(1) ubar{u_{delta}} leq z_{k,\tilde{f}} leq 0
		for(Systems k : model.machines){
			for(ObservableConfiguration o : model.obs){
				//Left side of contraint
				IloNumExpr expr = cplex.constant(0.0);
				expr = cplex.sum(expr, cplex.diff(minUtility, zMap.get(k).get(o)));
				
				constraints.add(cplex.le(expr, 0, "Z1_LEFT_K"+k.id+"_O"+o.id));
				
				//Right side of constraint
				IloNumExpr expr1 = zMap.get(k).get(o);
				//expr = cplex.sum(expr, cplex.diff(minUtility, zMap.get(k).get(o)));
				
				constraints.add(cplex.le(expr1, 0, "Z1_RIGHT_K"+k.id+"_O"+o.id));
			}
		}
		
		// (2) ubar{u_{\delta}} * sigma_{k,\tilde{f}} leq 0
		for (Systems k : model.machines) {
			for (ObservableConfiguration o : model.obs) {
				IloNumExpr expr = cplex.constant(0.0);
				expr = cplex.sum(expr, cplex.prod(minUtility, sigmaMap.get(k).get(o)));
				expr = cplex.diff(expr, zMap.get(k).get(o));

				constraints.add(cplex.le(expr, 0, "Z2_K" + k.id + "_O" + o.id));
			}
		}

		// (3) u_{\delta} leq z_{k,\tilde{f}} leq u_[\delta} - (1 - sigma_{k,\tilde{f}}) ubaru_{\delta}
		// z_k,\tilde{f} -d + ubar{u_\delta}\simga_k,\tilde{f}  \leq -ubar{u_{\delta}}
		for (Systems k : model.machines) {
			for (ObservableConfiguration o : model.obs) {
				// Left side of contraint
				IloNumExpr expr = cplex.constant(0.0);
				expr = cplex.sum(expr, cplex.diff(dutility, zMap.get(k).get(o)));

				constraints.add(cplex.le(expr, 0, "Z3_LEFT_K" + k.id + "_O" + o.id));

				// Right side of constraint
				IloNumExpr expr1 = cplex.diff(zMap.get(k).get(o), dutility);//zMap.get(k).get(o);
				//expr1 = cplex.diff(expr, zMap.get(k).get(o));//dutility);
				//expr1 = cplex.sum(expr1, -minUtility);
				expr1 = cplex.sum(expr1, cplex.prod(minUtility, sigmaMap.get(k).get(o)));
				//expr1 = cplex.sum(expr, cplex.diff(minUtility, cplex.prod(minUtility, sigmaMap.get(k).get(o))));
				
				constraints.add(cplex.le(expr1, -minUtility, "Z3_RIGHT_K" + k.id + "_O" + o.id));
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
	
	public static void printCompactStrategy(Map<Systems, Map<ObservableConfiguration, Double>> strat, DeceptionGame g){

		for(ObservableConfiguration o : g.obs){
			double sum =0;
			for(Systems k : strat.keySet()){
				sum += strat.get(k).get(o);
			}
			System.out.println("O"+o.id+": "+sum);
//			System.out.println();
			/*for(Systems k : strat.keySet()){
				if(strat.get(k).get(o) > .999)
					System.out.print("k"+k.id+" ");
			}
			System.out.println();*/
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
	
}
