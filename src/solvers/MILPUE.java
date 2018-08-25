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

public class MILPUE {

	private DeceptionGame model;

	private IloCplex cplex;

	private Map<Systems, Map<ObservableConfiguration, IloNumVar>> sigmaMap;
	private Map<Systems, IloNumVar> zMap;
	private IloNumVar dutility;

	private Map<Systems, Map<ObservableConfiguration, Integer>> defenderStrategy;

	private List<IloRange> constraints;

	private static final int MM = 100000;

	private double minUtility = Double.MAX_VALUE;

	private double runtime;

	private double defenderUtility;

	private Map<ObservableConfiguration, Integer> bounds;

	private double maxRuntime = 0;

	private boolean cutoff = false;
	
	private boolean feasible = true;

	ObservableConfiguration ostar;
	ArrayList<Systems> gammaprime;

	Map<ObservableConfiguration, Double> expectedUtilities;

	public MILPUE(DeceptionGame g) {
		this.model = g;
	}

	public MILPUE(DeceptionGame g, ObservableConfiguration ostar, ArrayList<Systems> gammaprime,
			Map<ObservableConfiguration, Double> expectedUtilities) {
		this.model = g;
		this.ostar = ostar;
		this.gammaprime = gammaprime;
		this.expectedUtilities = expectedUtilities;
	}

	private void loadProblem() throws IloException {
		sigmaMap = new HashMap<Systems, Map<ObservableConfiguration, IloNumVar>>();
		zMap = new HashMap<Systems, IloNumVar>();

		cplex = new IloCplex();
		cplex.setName("DECEPTION");
		// cplex.setParam(IloCplex.IntParam.RootAlg,
		// IloCplex.Algorithm.Barrier);
		// cplex.setParam(IloCplex.IntParam.BarCrossAlg,
		// IloCplex.Algorithm.None);
		// cplex.setParam(IloCplex.IntParam.BarAlg, 0);
		if (maxRuntime != 0)
			cplex.setParam(IloCplex.DoubleParam.TiLim, maxRuntime);

		cplex.setOut(null);

		initVars();
		initConstraints();
		initObjective();
	}

	public void solve() throws Exception {
		defenderStrategy = new HashMap<Systems, Map<ObservableConfiguration, Integer>>();
		// riskCategoryCoverage = new HashMap<AlertLevel, Map<Base, Double>>();
		// defenderPayoffs = new HashMap<AlertLevel, Double>();
		// adversaryPayoffs = new HashMap<AlertLevel, Double>();
		// adversaryStrategies = new HashMap<AlertLevel, Map<Base,
		// AttackMethod>>();

		double start = System.currentTimeMillis();

		loadProblem();

		cplex.solve();

		if (!cplex.isPrimalFeasible()) {
			writeProblem("Infeasible.lp");
			feasible = false;
			// writeProblem("Infeasible.sol.txt");
			//throw new Exception("Infeasible.");
		}

		// Print out z variables
		// printZVariables();
		// printSigmaVariables();

		writeProblem("MILPUE" + ostar.id + ".lp");

		if(feasible){
			defenderStrategy = getDefenderStrategy();
			defenderUtility = getDefenderPayoff();
		}
		
		runtime = (System.currentTimeMillis() - start) / 1000.0;

		if (cplex.getCplexTime() > maxRuntime)
			cutoff = true;

		// printStrategy(defenderStrategy);
		// printCompactStrategy(defenderStrategy);
		// printExpectedUtility(defenderStrategy);
		// riskCategoryCoverage = calculateRiskCategoryCoverage();
		// defenderPayoffs = getDefenderPayoffs();
		// adversaryPayoffs = getAdversaryPayoffs();
		// adversaryStrategies = getAdversaryStrategies();

		// System.out.println("Defender Utility: "+getDefenderPayoff());

		// System.out.println("Adversary Utility: "+getAdversaryPayoff());

		// System.out.println("Runtime: "+runtime);

	}

	private void initVars() throws IloException {
		List<IloNumVar> varList = new ArrayList<IloNumVar>();

		// Create variables for defender's strategy
		for (Systems k : model.machines) {
			sigmaMap.put(k, new HashMap<ObservableConfiguration, IloNumVar>());

			for (ObservableConfiguration o : model.obs) {
				IloNumVar var = cplex.numVar(0, 1, IloNumVarType.Int, "sigma_k" + k.id + "_o" + o.id);

				sigmaMap.get(k).put(o, var);
				varList.add(var);
			}
		}

		// create z variables
		for (Systems k : model.machines) {
			// zMap.put(k, new HashMap<ObservableConfiguration, IloNumVar>());

			IloNumVar var = cplex.numVar(-MM, MM, IloNumVarType.Float, "Z_k" + k.id + "_o" + ostar.id);

			zMap.put(k, var);
			varList.add(var);

		}

		for (Systems k : model.machines) {
			if (minUtility > k.f.utility) {
				minUtility = k.f.utility;
			}
		}

		dutility = cplex.numVar(minUtility, 0, IloNumVarType.Float, "d");

		varList.add(dutility);

		IloNumVar[] v = new IloNumVar[varList.size()];

		cplex.add(varList.toArray(v));
	}

	private void initConstraints() throws IloException {
		constraints = new ArrayList<IloRange>();

		// Need to add utility constraints
		setUtilityConstraints();
		// Need to add constraints to ensure ubar is low enough for all systems
		setObservableUtilityConstraints();
		// Need to set single observable per system constraint
		sumObservableConfigurationRow();
		// Need to set z constraints
		setZConstraints();
		// Set 0 value constraints for observables that can't be assigned to a system
		setZeroConstraints();
		
		//Set 1 constraints for all systems in GammaPrime
		setGammaPrimeConstraints();

		setBudgetConstraint();

		IloRange[] c = new IloRange[constraints.size()];

		cplex.add(constraints.toArray(c));
	}

	private void initObjective() throws IloException {
		// Should be correct objective
		IloNumExpr expr = dutility;// cplex.constant(0.0)

		// for(Systems k : model.machines){
		// expr = cplex.sum(expr, cplex.prod(sigmaMap.get(k).get(ostar),
		// k.f.utility));
		// }

		cplex.addMaximize(expr);
	}

	private void setUtilityConstraints() throws IloException {
		// This is going to be in terms of the z contraints for each \tilde{f}
		// \in \tilde{F}
		IloNumExpr expr = cplex.constant(0.0);

		for (Systems k : model.machines) {
			expr = cplex.sum(expr, cplex.diff(zMap.get(k), cplex.prod(k.f.utility, sigmaMap.get(k).get(ostar))));
		}

		constraints.add(cplex.le(expr, 0.0, "UTILITY_TF_" + ostar.id));
		
		IloNumExpr expr1 = cplex.constant(0.0);
		
		for(Systems k : model.machines){
			expr1 = cplex.sum(expr1, sigmaMap.get(k).get(ostar));
		}
		
		constraints.add(cplex.ge(expr1, 1.0, "MIN_AS_o"+ostar.id));

	}
	
	private void setGammaPrimeConstraints() throws IloException{
		for(Systems k : gammaprime){
			IloNumExpr expr = sigmaMap.get(k).get(ostar);
			
			constraints.add(cplex.eq(expr, 1.0, "GP_K"+k.id));
		}
		
	}

	private void setObservableUtilityConstraints() throws IloException {
		// This is to ensure that all systems are masked with an o which is
		// smaller than or equal to ostar
		for (Systems k : model.machines) {
			IloNumExpr expr = cplex.constant(0.0);

			for (ObservableConfiguration o : model.obs) {
				if (o.configs.contains(k.f))
					expr = cplex.sum(expr, cplex.prod(sigmaMap.get(k).get(o), expectedUtilities.get(o)));
			}

			constraints.add(cplex.ge(expr, expectedUtilities.get(ostar), "UTILITY_OF_" + k.id));
		}

	}

	private void setBudgetConstraint() throws IloException {
		IloNumExpr expr = cplex.constant(0.0);
		for (Systems k : model.machines) {
			for (ObservableConfiguration o : model.obs) {

				if (o.configs.contains(k.f)) { // If we can cover it add the
												// value to the budget
												// constraint
					expr = cplex.sum(expr, cplex.prod(model.costFunction.get(k.f).get(o), sigmaMap.get(k).get(o)));
				}
			}
		}

		constraints.add(cplex.le(expr, model.Budget, "BUDGET_CONST"));
	}

	private void setZConstraints() throws IloException {
		// Four constraints to add for each z variable in zMap

		// (1) ubar{u_{delta}} leq z_{k,\tilde{f}} leq 0
		for (Systems k : model.machines) {
			// Left side of contraint
			IloNumExpr expr = cplex.constant(0.0);
			expr = cplex.sum(expr, cplex.diff(minUtility, zMap.get(k)));

			constraints.add(cplex.le(expr, 0, "Z1_LEFT_K" + k.id + "_O" + ostar.id));

			// Right side of constraint
			IloNumExpr expr1 = zMap.get(k);
			// expr = cplex.sum(expr, cplex.diff(minUtility,
			// zMap.get(k).get(o)));

			constraints.add(cplex.le(expr1, 0, "Z1_RIGHT_K" + k.id + "_O" + ostar.id));
		}

		// (2) ubar{u_{\delta}} * sigma_{k,\tilde{f}} leq 0
		for (Systems k : model.machines) {
			IloNumExpr expr = cplex.constant(0.0);
			expr = cplex.sum(expr, cplex.prod(minUtility, sigmaMap.get(k).get(ostar)));
			expr = cplex.diff(expr, zMap.get(k));

			constraints.add(cplex.le(expr, 0, "Z2_K" + k.id + "_O" + ostar.id));
		}

		// (3) u_{\delta} leq z_{k,\tilde{f}} leq u_[\delta} - (1 -
		// sigma_{k,\tilde{f}}) ubaru_{\delta}
		// z_k,\tilde{f} -d + ubar{u_\delta}\simga_k,\tilde{f} \leq
		// -ubar{u_{\delta}}
		for (Systems k : model.machines) {
			// Left side of contraint
			IloNumExpr expr = cplex.constant(0.0);
			expr = cplex.sum(expr, cplex.diff(dutility, zMap.get(k)));

			constraints.add(cplex.le(expr, 0, "Z3_LEFT_K" + k.id + "_O" + ostar.id));

			// Right side of constraint
			IloNumExpr expr1 = cplex.diff(zMap.get(k), dutility);// zMap.get(k).get(o);
			// expr1 = cplex.diff(expr, zMap.get(k).get(o));//dutility);
			// expr1 = cplex.sum(expr1, -minUtility);
			expr1 = cplex.sum(expr1, cplex.prod(minUtility, sigmaMap.get(k).get(ostar)));
			// expr1 = cplex.sum(expr, cplex.diff(minUtility,
			// cplex.prod(minUtility, sigmaMap.get(k).get(o))));

			constraints.add(cplex.le(expr1, -minUtility, "Z3_RIGHT_K" + k.id + "_O" + ostar.id));
		}

	}

	private void setZeroConstraints() throws IloException {
		// Might be able to do this a lot more efficiently, but it should work
		// for now
		for (Systems k : model.machines) {
			for (ObservableConfiguration o : model.obs) {
				if (!o.configs.contains(k.f)) {
					// if(!k.f.obsConfigs.contains(o)){
					IloNumExpr expr = sigmaMap.get(k).get(o);

					constraints.add(cplex.eq(expr, 0, "ZERO_K" + k.id + "_O" + o.id));
				}
			}
		}
	}

	private void sumObservableConfigurationRow() throws IloException {
		for (Systems k : model.machines) {
			IloNumExpr expr = cplex.constant(0.0);

			for (ObservableConfiguration o : model.obs) {
				expr = cplex.sum(expr, sigmaMap.get(k).get(o));
			}

			constraints.add(cplex.eq(1.0, expr, "EQ_K" + k.id));
		}
	}

	private void printSigmaVariables() throws UnknownObjectException, IloException {
		for (Systems k : model.machines) {
			for (ObservableConfiguration o : model.obs) {
				System.out.println(sigmaMap.get(k).get(o).getName() + ": " + cplex.getValue(sigmaMap.get(k).get(o)));
			}
		}
	}

	public void writeProblem(String filename) throws IloException {
		cplex.exportModel(filename);
	}

	public void writeSolution(String filename) throws IloException {
		cplex.writeSolution(filename);
	}

	public double getDefenderPayoff() throws IloException {
		double utility = Average(getAttackSet());// cplex.getValue(dutility);

		return utility;
	}

	public double getAdversaryPayoff() throws IloException {
		return -1.0 * getDefenderPayoff();
	}

	public Map<Systems, Map<ObservableConfiguration, Integer>> getDefenderStrategy()
			throws UnknownObjectException, IloException {
		Map<Systems, Map<ObservableConfiguration, Integer>> strat = new HashMap<Systems, Map<ObservableConfiguration, Integer>>();

		for (Systems k : sigmaMap.keySet()) {
			strat.put(k, new HashMap<ObservableConfiguration, Integer>());
			for (ObservableConfiguration o : sigmaMap.get(k).keySet()) {
				if (cplex.getValue(sigmaMap.get(k).get(o)) > 1 - .00001
						&& cplex.getValue(sigmaMap.get(k).get(o)) < 1 + .00001) {
					// strat.get(k).put(o, (int)
					// cplex.getValue(sigmaMap.get(k).get(o)));
					strat.get(k).put(o, 1);// (int)
											// cplex.getValue(sigmaMap.get(k).get(o)));
				} else {
					strat.get(k).put(o, (int) cplex.getValue(sigmaMap.get(k).get(o)));
				}
				// System.out.println(k.id+" : "+o.id+" : "+((int)
				// cplex.getValue(sigmaMap.get(k).get(o))));
			}
		}

		return strat;
	}

	public void printStrategy(Map<Systems, Map<ObservableConfiguration, Integer>> strat) {
		for (Systems k : strat.keySet()) {
			System.out.print("K" + k.id + ": ");
			for (ObservableConfiguration o : strat.get(k).keySet()) {
				System.out.print("TF" + o.id + " : " + strat.get(k).get(o) + " ");
			}
			System.out.println();
		}
	}

	public void printCompactStrategy(Map<Systems, Map<ObservableConfiguration, Integer>> strat) {
		for (Systems k : strat.keySet()) {
			System.out.print("K" + k.id + ": ");
			for (ObservableConfiguration o : strat.get(k).keySet()) {
				if (strat.get(k).get(o) > 0)
					System.out.print("TF" + o.id + " : " + strat.get(k).get(o) + " ");
			}
			System.out.println();
		}
	}

	public double getRuntime() {
		return runtime;
	}

	public double getUtility() {
		return defenderUtility;
	}

	public void printExpectedUtility(Map<Systems, Map<ObservableConfiguration, Integer>> strategy) {
		double expectedU = 0;
		double total = 0;
		;
		for (ObservableConfiguration o : model.obs) {
			for (Systems k : strategy.keySet()) {
				expectedU += strategy.get(k).get(o) * k.f.utility;
				total += strategy.get(k).get(o);
			}
			System.out.println("EU(o" + o.id + "): " + (expectedU / total));
			expectedU = 0;
			total = 0;
		}

	}

	public static double calculateExpectedUtility(DeceptionGame game,
			Map<Systems, Map<ObservableConfiguration, Integer>> strategy) {
		double expectedU = 0;
		double total = 0;
		double minUtil = 0;
		for (ObservableConfiguration o : game.obs) {
			for (Systems k : strategy.keySet()) {
				expectedU += strategy.get(k).get(o) * k.f.utility;
				total += strategy.get(k).get(o);
			}
			// System.out.println("EU(o"+o.id+"): "+(expectedU/total));
			if ((expectedU / total) < minUtil) {
				minUtil = (expectedU / total);
			}
			expectedU = 0;
			total = 0;
		}
		return minUtil;
	}

	public static double calculateExpectedUtility1(DeceptionGame game,
			Map<Systems, Map<ObservableConfiguration, Double>> strategy) {
		double expectedU = 0;
		double total = 0;
		double minUtil = 0;
		for (ObservableConfiguration o : game.obs) {
			for (Systems k : strategy.keySet()) {
				expectedU += strategy.get(k).get(o) * k.f.utility;
				total += strategy.get(k).get(o);
			}
			// System.out.println("EU(o"+o.id+"): "+(expectedU/total));
			if ((expectedU / total) < minUtil) {
				minUtil = (expectedU / total);
			}
			expectedU = 0;
			total = 0;
		}
		return minUtil;
	}

	public void deleteVars() throws IloException {
		sigmaMap.clear();

		constraints.clear();

		if (cplex != null)
			cplex.end();
		// cplex.clearModel();

		cplex = null;
	}

	public void setMaxRuntime(double maxR) {
		maxRuntime = maxR;
	}

	public boolean isCutoff() {
		return cutoff;
	}

	private double Average(ArrayList<Systems> set) {
		if (set.size() == 0)
			return -1000;

		double average = 0;
		for (Systems k : set) {
			average += k.f.utility;
		}
		average = average / (double) set.size();
		return average;
	}

	public ArrayList<Systems> getAttackSet() {
		ArrayList<Systems> gamma = new ArrayList<Systems>();
		for (Systems k : model.machines) {
			if (defenderStrategy.get(k).get(ostar) > 0)
				gamma.add(k);
		}
		return gamma;
	}
	
	public boolean getFeasible(){
		return feasible;
	}
}
