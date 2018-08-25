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

public class FeasibilityLP {
	
	private DeceptionGame model;
	
	private IloCplex cplex;
	
	private Map<Systems, Map<ObservableConfiguration, IloNumVar>> nMap;
	//private Map<ObservableConfiguation, IloNumVar> dMap;
	
	private Map<Systems, Map<ObservableConfiguration, Double>> defenderStrategy;
	
	private List<IloRange> constraints;
	
	private static final int MM = 100000;
	
	private double minUtility = Double.MAX_VALUE;
	
	private double runtime;
	
	private double defenderUtility;
	
	private Map<ObservableConfiguration, Integer> bounds = null;
	private Map<ObservableConfiguration, Integer> upperConstraints = null;
	private Map<ObservableConfiguration, Integer> lowerConstraints = null;
	private Map<Systems, Map<ObservableConfiguration, Integer>> fixedConstraints = null;
	
	private Map<Systems, ObservableConfiguration> setMaskings = null;
	
	private double alpha;
	private boolean feasible = true;
	
	public FeasibilityLP(DeceptionGame g, double alpha){
		this.model = g;
		this.alpha = alpha;
	}
	
	public FeasibilityLP(DeceptionGame g, double alpha, Map<Systems, ObservableConfiguration> setMaskings){
		this.model = g;
		this.alpha = alpha;
		this.setMaskings = setMaskings;
	}
		
//	public FeasibilityLP(DeceptionGame g, double alpha, Map<ObservableConfiguration, Integer> bounds){
//		this.model = g;
//		this.alpha = alpha;
//		this.bounds = bounds;
//	}
	
	public FeasibilityLP(DeceptionGame g, double alpha, Map<Systems, Map<ObservableConfiguration, Integer>> constraints, boolean doesntmatter){
		this.model = g;
		this.alpha = alpha;
		this.fixedConstraints = constraints;
	}
	
	public FeasibilityLP(DeceptionGame g, double alpha, Map<ObservableConfiguration, Integer> upperConstraints, Map<ObservableConfiguration, Integer> lowerConstraints){
		this.model = g;
		this.alpha = alpha;
		this.upperConstraints = upperConstraints;
		this.lowerConstraints = lowerConstraints;
	}
	
	private void loadProblem() throws IloException{
		nMap = new HashMap<Systems, Map<ObservableConfiguration, IloNumVar>>();
		
		cplex = new IloCplex();
		cplex.setName("FEASIBLELP");
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
		//	System.out.println("Problem is Infeasible");
			feasible = false;
			//writeProblem("Infeasible.lp");
			//writeProblem("Infeasible.sol.txt");
			//throw new Exception("Infeasible.");
		}else{
		//	System.out.println("Problem is Feasible for Alpha: "+alpha);
		}
//		writeProblem("FLP.lp");
			
//		System.out.println(feasible);
		if(getDefenderStrategy() != null)
			defenderStrategy = getDefenderStrategy();
		//defenderUtility = getDefenderPayoff();
		
		runtime = (System.currentTimeMillis()-start)/1000.0;
		
		//if(getDefenderStrategy() != null)
			//printStrategy(defenderStrategy);
			//printCompactStrategy(defenderStrategy);
			//printExpectedUtility(defenderStrategy);
		
		//System.out.println("Defender Utility: "+getDefenderPayoff());

		//System.out.println("Adversary Utility: "+getAdversaryPayoff());
		
		//System.out.println("Runtime: "+runtime);
		
	}
	
	private void initVars() throws IloException{
		List<IloNumVar> varList = new ArrayList<IloNumVar>();
		
		//Create variables for defender's strategy
		for(Systems k : model.machines){
			nMap.put(k, new HashMap<ObservableConfiguration, IloNumVar>());
			
			for(ObservableConfiguration o : model.obs){
				IloNumVar var = cplex.numVar(0, 1, IloNumVarType.Float, "n_k" +  k.id + "_o" + o.id);
				
				nMap.get(k).put(o, var);
				varList.add(var);
			}
		}
		
		IloNumVar[] v = new IloNumVar[varList.size()];

		cplex.add(varList.toArray(v));
	}
	
	private void initConstraints() throws IloException{
		constraints = new ArrayList<IloRange>();
		
		//Need to add utility constraints
		setUtilityConstraints();
		
		//Need to set single observable per system constraint
		sumObservableConfigurationRow();
		
		//Set constraints for bounds on N_{\tilde{f}}
		setAssignmentConstraints();
		
		//sum over all machines for a marginal is equal to N_{\tilde{f}}
		setObservableSum();
		
		//Set 0 value constraints for observables that can't be assigned to a system
		setZeroConstraints();
		
		//set strategy Constraints if they have anything in them
		setStrategyFixedMaskings();
		
		if(true)//setting budget constraint; needs to be changed if not supposed to have budget
			setBudgetConstraint();
		
		IloRange[] c = new IloRange[constraints.size()];

		cplex.add(constraints.toArray(c));
	}

	private void initObjective() throws IloException{
		//Should be correct objective
		IloNumExpr expr = cplex.constant(1.0);
		
		cplex.addMaximize(expr);
	}

	private void setUtilityConstraints() throws IloException {
		//This is going to be in terms of the z contraints for each \tilde{f} \in \tilde{F}
		for(ObservableConfiguration o : model.obs){
			IloNumExpr expr = cplex.constant(0.0);
			
			for(Systems k : model.machines){
				expr = cplex.sum(expr, cplex.prod(alpha, nMap.get(k).get(o)));
			}
			
			for(Systems k : model.machines){
				expr = cplex.diff(expr, cplex.prod(k.f.utility, nMap.get(k).get(o)));
			}
			
			
			constraints.add(cplex.le(expr, 0.0, "UTILITY_TF_"+o.id));
		}
		
	}
	
	private void setBudgetConstraint() throws IloException{

		IloNumExpr expr = cplex.constant(0.0);
		for(Systems k : model.machines){
			for(ObservableConfiguration o : model.obs){
				
				if(o.configs.contains(k.f)){ //If we can cover it add the value to the budget constraint
					expr = cplex.sum(expr, cplex.prod(model.costFunction.get(k.f).get(o), nMap.get(k).get(o)));
				}
			}	
		}
		
		constraints.add(cplex.le(expr, model.Budget, "BUDGET_CONST"));
		
	}
	
	private void setStrategyFixedMaskings() throws IloException{
		
		if(setMaskings != null){
			for(Systems k : setMaskings.keySet()){
				for(ObservableConfiguration o : model.obs){	
//					System.out.println("Setting K"+k.id+" O"+o.id);
					if(setMaskings.get(k).id == o.id){
						IloNumExpr expr = nMap.get(k).get(o);
						
						constraints.add(cplex.eq(expr, 1.0, "ST_CONS_K"+k.id+"_O"+o.id));
					}else{
						IloNumExpr expr = nMap.get(k).get(o);
						
						constraints.add(cplex.eq(expr, 0.0, "ST_CONS_K"+k.id+"_O"+o.id));
					}
				}
				
			}
		}
		
	}
	
	private void setAssignmentConstraints() throws IloException{
		//System.out.println("Setting Assignment Constraints");
		if(bounds != null){
			for(ObservableConfiguration o : bounds.keySet()){
				//System.out.println("O"+o.id);
				IloNumExpr expr = cplex.constant(0.0);
				
				for(Systems k : model.machines){
					expr = cplex.sum(expr, nMap.get(k).get(o));
				}
	
				constraints.add(cplex.eq(expr, bounds.get(o), "NUM_TF_"+o.id));
			}
		}
		
		if(fixedConstraints != null){
			for(Systems k : fixedConstraints.keySet()){
				for(ObservableConfiguration o : fixedConstraints.get(k).keySet()){
					//System.out.println("O"+o.id);
					IloNumExpr expr = nMap.get(k).get(o);
		
					if(fixedConstraints.get(k).get(o) == 0){
						constraints.add(cplex.le(expr, 0, "NUM_K"+k.id+"_TF_"+o.id));
					}else{ //should be equal to 1
						constraints.add(cplex.ge(expr, 1, "NUM_K"+k.id+"_TF_"+o.id));
					}
				}
			}
		}
		
		if(upperConstraints != null){
			for(ObservableConfiguration o : upperConstraints.keySet()){
				//System.out.println("O"+o.id);
				IloNumExpr expr = cplex.constant(0.0);
				
				for(Systems k : model.machines){
					expr = cplex.sum(expr, nMap.get(k).get(o));
				}
	
				constraints.add(cplex.le(expr, upperConstraints.get(o), "UBCONSTRAINT_TF_"+o.id));
			}
		}
		
		if(lowerConstraints != null){
			for(ObservableConfiguration o : lowerConstraints.keySet()){
				//System.out.println("O"+o.id);
				IloNumExpr expr = cplex.constant(0.0);
				
				for(Systems k : model.machines){
					expr = cplex.sum(expr, nMap.get(k).get(o));
				}
	
				constraints.add(cplex.ge(expr, lowerConstraints.get(o), "LBCONSTRAINT_TF_"+o.id));
			}
			
		}
	}
	
	private void setObservableSum() throws IloException{
		/*for(ObservableConfiguration o : model.obs){
			IloNumExpr expr = cplex.constant(0.0);
			
			for(Systems k : model.machines){
				expr = cplex.sum(expr, nMap.get(k).get(o));
			}
			
			expr = cplex.diff(expr, numObsMap.get(o));
			
			constraints.add(cplex.eq(expr, 0.0, "EQUAL_o"+o.id));
		}*/
	}
	
	private void setAllMachinesCovered() throws IloException{
		/*IloNumExpr expr = cplex.constant(0.0);
		for(ObservableConfiguration o : model.obs){
			expr = cplex.sum(expr, numObsMap.get(o));
		}
		constraints.add(cplex.eq(expr, model.machines.size(), "SUM_NUM"));*/
	}

	private void setZeroConstraints() throws IloException {
		//Might be able to do this a lot more efficiently, but it should work for now
		for(Systems k : model.machines){
			for(ObservableConfiguration o : model.obs){
				if(!o.configs.contains(k.f)){
				//if(!k.f.obsConfigs.contains(o)){
					IloNumExpr expr = nMap.get(k).get(o);
					
					constraints.add(cplex.eq(expr, 0, "ZERO_K"+k.id+"_O"+o.id));
				}
			}
		}
	}

	private void sumObservableConfigurationRow() throws IloException {
		for(Systems k : model.machines){
			IloNumExpr expr = cplex.constant(0.0);
			
			for(ObservableConfiguration o : model.obs){
				expr = cplex.sum(expr, nMap.get(k).get(o));
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
		return 1;
	}
	
	public double getAdversaryPayoff() throws IloException{
		return -1.0*getDefenderPayoff();
	}
	
	public Map<Systems, Map<ObservableConfiguration, Double>> getDefenderStrategy() throws UnknownObjectException, IloException{
		Map<Systems, Map<ObservableConfiguration, Double>> strat = new HashMap<Systems, Map<ObservableConfiguration, Double>>();
		
		if(feasible){
			for(Systems k : nMap.keySet()){
				strat.put(k, new HashMap<ObservableConfiguration, Double>());
				for(ObservableConfiguration o : nMap.get(k).keySet()){
					strat.get(k).put(o, cplex.getValue(nMap.get(k).get(o))); 
					//System.out.println(k.id+"  :  "+o.id+"  :  "+((int) cplex.getValue(sigmaMap.get(k).get(o))));
				}
			}
			
			return strat;
		}else{
			return null;
		}
	}
	
	private int calculateMaxAssignable(ObservableConfiguration o){
		int assignable = 0;
		for(Systems k : model.machines){
			if(o.configs.contains(k.f))
				assignable++;
		}
		return assignable;		
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
	
	public boolean getFeasible(){
		return feasible;
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
	
	public void deleteVars() throws IloException{
		nMap.clear();
		
		constraints.clear();
		
		if(cplex != null)
			cplex.end();
		//cplex.clearModel();
		
		cplex = null;
	}
	
	public Map<Systems, ObservableConfiguration> getStrategyFixedMaskings(){
		return setMaskings;
	}
	
}
