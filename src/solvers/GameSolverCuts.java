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
import models.Subset;
import models.Systems;

public class GameSolverCuts {
	
	private DeceptionGame model;
	
	private IloCplex cplex;
	
	private Map<Systems, Map<ObservableConfiguration, IloNumVar>> sigmaMap;
	private Map<Systems, Map<ObservableConfiguration, IloNumVar>> zMap;
	private Map<ObservableConfiguration, IloNumVar> wMap;
	private Map<ObservableConfiguration, Map<Subset, IloNumVar>> wMapSub;
	private IloNumVar dutility;
	//private Map<ObservableConfiguation, IloNumVar> dMap;
	
	private Map<Systems, Map<ObservableConfiguration, Integer>> defenderStrategy;
	
	private List<IloRange> constraints;
	
	private static final int MM = 100000;
	
	private double minUtility = Double.MAX_VALUE;
	
	private double runtime;
	
	private double defenderUtility;

	private Map<ObservableConfiguration, Integer> bounds;
	
	private double lowerBound = -100;
	
	private int maxSubsetSize = 0;
	
	private double maxRuntime=0;
	
	private boolean cutoff = false;
	private boolean setCosts = false;
	private double milpGap = 0;
	
	public GameSolverCuts(DeceptionGame g){
		this.model = g;
	}
	
	public GameSolverCuts(DeceptionGame g, Map<ObservableConfiguration, Integer> bounds){
		this.model = g;
		this.bounds = bounds;
	}
	
	private void loadProblem() throws IloException{
		sigmaMap = new HashMap<Systems, Map<ObservableConfiguration, IloNumVar>>();
		zMap = new HashMap<Systems, Map<ObservableConfiguration, IloNumVar>>();
		wMap = new HashMap<ObservableConfiguration, IloNumVar>();
		wMapSub = new HashMap<ObservableConfiguration, Map<Subset, IloNumVar>>();
		
		cplex = new IloCplex();
		cplex.setName("DECEPTION");
//		cplex.setParam(IloCplex.IntParam.RootAlg, IloCplex.Algorithm.Barrier);
		//cplex.setParam(IloCplex.IntParam.BarCrossAlg, IloCplex.Algorithm.None);
		//cplex.setParam(IloCplex.IntParam.BarAlg, 0);
		if(maxRuntime != 0)
			cplex.setParam(IloCplex.DoubleParam.TiLim, maxRuntime);

		if(milpGap > 0)
			cplex.setParam(IloCplex.DoubleParam.EpGap, milpGap);

//		cplex.setParam(IloCplex.IntParam.MIPInterval, 10);
//		cplex.setParam(IloCplex.IntParam.MIPDisplay, 2);
		cplex.setOut(null);
		
		initVars();
		initConstraints();
		initObjective();
	}
	
	public void solve() throws Exception{
		defenderStrategy = new HashMap<Systems, Map<ObservableConfiguration, Integer>>();
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
		
		//Print out z variables
		//printZVariables();
		//printSigmaVariables();
		
		writeProblem("CDGCut.lp");
			
		defenderStrategy = getDefenderStrategy();
		defenderUtility = getDefenderPayoff();
		
		runtime = (System.currentTimeMillis()-start)/1000.0;
		
		if(cplex.getCplexTime() > maxRuntime)
			cutoff = true;
		
		
		//printStrategy(defenderStrategy);
		//printCompactStrategy(defenderStrategy);
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
		
		//Initialize w map
//		for(ObservableConfiguration o : model.obs){
//			IloNumVar var = cplex.numVar(0, 1, IloNumVarType.Int, "w_o" + o.id);
//			
//			wMap.put(o, var);
//			varList.add(var);
//		}
		
		for(Systems k : model.machines){
			if(minUtility > k.f.utility){
				minUtility = k.f.utility;
			}
		}
		
		double tUB = calculateUB(model);
		
//		dutility = cplex.numVar(minUtility, 0, IloNumVarType.Float, "d");
		dutility = cplex.numVar(minUtility, tUB, IloNumVarType.Float, "d");
		
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
		//Need to set bounds
		//setObservableBounds();
		//Set 0 value constraints for observables that can't be assigned to a system
		setZeroConstraints();
		
		if(setCosts){
			setBudgetConstraint();
		}
		
		//Need to set constraints for w variables (Only sets of size 1 for right now!)
		for(int i =1; i<=maxSubsetSize; i++){
			setWConstraints(i);
		}
//		setWConstraints(1);
//		
//		setWConstraints(2);
//		
//		setWConstraints(3);
		
		//Set constraints for all systems that have U(k) > LB 
//		setTwoSetConstraints();
		
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
	
	private void setBudgetConstraint() throws IloException{

		IloNumExpr expr = cplex.constant(0.0);
		for(Systems k : model.machines){
			for(ObservableConfiguration o : model.obs){
				
				if(o.configs.contains(k.f)){ //If we can cover it add the value to the budget constraint
					expr = cplex.sum(expr, cplex.prod(model.costFunction.get(k.f).get(o), sigmaMap.get(k).get(o)));
				}
			}	
		}
		
		constraints.add(cplex.le(expr, model.Budget, "BUDGET_CONST"));
		
	}
	
//	private void setTwoSetConstraints() throws IloException {
//		//For each observable we need to add constraint for all k \in K, where U(k) > LB
//		for(ObservableConfiguration o : model.obs){
//			
//			for(Systems k : model.machines){
//				
//				if(o.configs.contains(k.f)){
//					
//					if(k.f.utility < lowerBound){
//						boolean machineExists = findTwoSetBelowAverage(o,k);
//						ArrayList<Systems> systemsBelowLB = findMachinesBelowLB(o); //Find systems that can lower avg utility <= LB 
//						
//						IloNumExpr expr = cplex.constant(0.0);
//						
//						//It is possible there is not a single other machine which can lower the avg of k and k' >= LB
//						
//						if(systemsBelowLB.size() > 0){
//							for(Systems k1 : systemsBelowLB){
//								expr = cplex.sum(expr, sigmaMap.get(k1).get(o));
//							}
//							
//							if(machineExists) //we know there exists a single machine which can lower the avg below lb
//								expr = cplex.diff(expr, sigmaMap.get(k).get(o)); 
//							else //there does not exist a single machine which can lower avg below lb
//								expr = cplex.diff(expr, cplex.prod(2.0, sigmaMap.get(k).get(o))); 
//							
//							constraints.add(cplex.ge(expr, 0, "AVG_O"+o.id+"_K"+k.id));
//						}
//					}
//				}
//				
//			}
//			
//		}
//		
//	}
	
//	private boolean findTwoSetBelowAverage(ObservableConfiguration o, Systems k){
//		//ArrayList<Systems> machinesObservable = new ArrayList<Systems>();
//		
//		boolean exists = false;
//		for(Systems k1 : model.machines){
//			if(o.configs.contains(k1.f)){
//				double average = (k.f.utility + k1.f.utility)/2.0;
//				if(average >= lowerBound){
//					exists = true;
//					//machinesObservable.add(k1);
//				}
//			}
//		}
//		
//		return exists;
//		//return machinesObservable;
//	}
	
//	private ArrayList<Systems> findMachinesBelowLB(ObservableConfiguration o){
//		ArrayList<Systems> machinesObservable = new ArrayList<Systems>();
//		
//		for(Systems k : model.machines){
//			if(o.configs.contains(k.f)){
//				if(k.f.utility >= lowerBound){
//					machinesObservable.add(k);
//				}
//			}
//		}
//		
//		return machinesObservable;
//	}
	
	//TODO: Need to make sure these constraints now initialize for a subset of size l
	private void setWConstraints(int l) throws IloException {
		//K^' := {k : k coverable by o and U(k) < LB}
		ArrayList<Systems> kprime = new ArrayList<Systems>();
		//K^'' := {k : k coverable by o and U(k) >= LB}
		ArrayList<Systems> kpprime = new ArrayList<Systems>();
		
		for(ObservableConfiguration o : model.obs){
			wMapSub.put(o, new HashMap<Subset, IloNumVar>());
			
			kprime = findKPrime(o);
			kpprime = findKPPrime(o);
			
//			System.out.println();
//			System.out.println("f~"+o.id+"  "+kprime.toString());
//			System.out.println("f~"+o.id+"  "+kpprime.toString());
//			System.out.println();
//			
			if(kpprime.size() == 0){
				//All integer variables sigma_k,o = 0 for machines in kprime (since we cannot lower the average value of those machines with this observable o)
				for(Systems k : kprime){
					IloNumExpr expr = sigmaMap.get(k).get(o);
					
					constraints.add(cplex.eq(expr, 0, "ZERO_K"+k.id+"_O"+o.id));
				}
				
				continue;
			}else if(kpprime.size() == 0){
				continue;
			}
			
			// Form all subsets of size l in K^'
			//Enforce a constraint of the following type:
			// -> if sigma_k,o = 1 \forall k\in s (where s is the subset) then
			//      \sum_{k\in K^'} >= (min set size for k needed in K^'' to make Avg(L + k) >= LB
			ArrayList<Subset> subsets = new ArrayList<Subset>();
			boolean [] b = new boolean[kprime.size()];
//			subsets = findAllSubsets(kprime, l, ); //l = size of subset
			findAllSubsets(subsets, kprime, l, 0, 0, b); //l = size of subset
			
//			System.out.println();
//
//			System.out.println("O"+o.id+" Size: "+l+" Number of subsets: "+subsets.size());
//			
//			System.out.println("O"+o.id+" Size: "+l+" Subsets: "+subsets);
			
			List<IloNumVar> varList = new ArrayList<IloNumVar>();
			
			//Initialize w map with subsets of a certain size
			for (Subset s : subsets) {
				IloNumVar var = cplex.numVar(0, 1, IloNumVarType.Int, "w_o" + o.id+"_s"+s.id);

				wMapSub.get(o).put(s, var);
				varList.add(var);
			}
			
			IloNumVar[] v = new IloNumVar[varList.size()];

			cplex.add(varList.toArray(v));
			
			
			//***********Make sure to go back and do general sizes for all subsets
			
			
			//This constraint is forced for all subsets S
			for(Subset s : subsets){
				//We can enforce this by introducing binary variable w_i,s and the 3 constraints:
				
				//Constraint 1: \sum_{k \in L} sigma_k,o <= (l-1)+w_i
				IloNumExpr expr = cplex.constant(0.0);

				for (Systems k : s.set) {
					expr = cplex.sum(expr, sigmaMap.get(k).get(o));
				}

				expr = cplex.diff(expr, cplex.sum(l - 1, wMapSub.get(o).get(s)));

				constraints.add(cplex.le(expr, 0, "W1_Constraint_O" + o.id+"_s"+s.id));				
				
				//Constraint 2: \sum_{k \in L} sigma_k,o >= l*w_i
				IloNumExpr expr1 = cplex.constant(0.0);

				for (Systems k : s.set) {
					expr1 = cplex.sum(expr1, sigmaMap.get(k).get(o));
				}

				expr1 = cplex.diff(expr1, cplex.prod(l, wMapSub.get(o).get(s)));

				constraints.add(cplex.ge(expr1, 0, "W2_Constraint_O" + o.id+"_s"+s.id));
				
				//Constraint 3: \sum_{k \in K^'} sigma_k,o >= (min set size)*w_i

				int minsetsize = findMinSetSize(kpprime, s);
				
				if(minsetsize != -1){
					IloNumExpr expr2 = cplex.constant(0.0);
	
					for (Systems k : kpprime) {
						expr2 = cplex.sum(expr2, sigmaMap.get(k).get(o));
					}
	
	//				System.out.println("Set: "+s.set);
	//				System.out.println("LowerBound: "+lowerBound+" Set: "+s.set+" "+minsetsize);
	//				System.out.println();
					expr2 = cplex.diff(expr2, cplex.prod(minsetsize, wMapSub.get(o).get(s)));
	
					constraints.add(cplex.ge(expr2, 0, "W3_Constraint_O" + o.id+"_s"+s.id));
				}else{
					//Need to add 2 constraints
					// 1) (1-w_i) -M \leq \sum_{k\in K''} \sigma_{k,o}
					// 2) \sum_{k\in K''} \sigma_{k,o} \leq (1-w_i) M
					IloNumExpr expr2 = cplex.diff(1, wMapSub.get(o).get(s));//cplex.constant(0.0);
					expr2 = cplex.prod(expr2, -1000);
					
					for (Systems k : kpprime) {
						expr2 = cplex.diff(expr2, sigmaMap.get(k).get(o));
					}
					
					constraints.add(cplex.le(expr2, 0, "W3_Constraint1_O" + o.id+"_s"+s.id));
					
					IloNumExpr expr3 = cplex.constant(0.0);
					
					for (Systems k : kpprime) {
						expr3 = cplex.sum(expr3, sigmaMap.get(k).get(o));
					}
					
					expr3 = cplex.sum(expr3, cplex.diff(-1.0, cplex.prod(cplex.diff(1, wMapSub.get(o).get(s)), 1000)));//cplex.constant(0.0);					
					
					constraints.add(cplex.le(expr3, 0, "W3_Constraint2_O" + o.id+"_s"+s.id));
					
//					System.out.println();
//					System.out.println("For o"+o.id+" Not possible to assign "+s.toString());
//					System.out.println();
				}
	
			}
			
			
		}
		
	}
	
	/** 
	 * Finds minimum set size needed of systems in kpprime. If possible, return size
	 * o/w returns -1 if it is not possible to use systems in kpprime
	 * @param kpprime
	 * @param s
	 * @return
	 */
	private int findMinSetSize(ArrayList<Systems> kpprime, Subset s){
		int msize = 0;
		
//		System.out.println(kpprime.toString()+" "+s.set.toString());
		
		//This should be descending order by defender (so highest valued (magnitude) first)
		Collections.sort(kpprime);
		
		double average = (kpprime.get(0).f.utility+s.subsetU)/(1.0+s.set.size());
		if(average >= lowerBound)
			return 1;
			
		while(average < lowerBound){
			msize++;
			if(msize >= kpprime.size())
				return -1;
			
			average = 0;
			double denom = s.set.size()+(msize+1);
			double num = s.subsetU;
			for(int i=0; i<=msize; i++){
				num += kpprime.get(i).f.utility;
			}
			
			average = (num)/(denom);
			
//			System.out.println("Subset u: "+s.subsetU+" num: "+num+" den: "+denom+" avg: "+average);
			
			if(average >= lowerBound)
				return msize+1;
			
		}
		
		return msize;
	}
	
	private ArrayList<Subset> findAllSubsets(ArrayList<Systems> kprime, int size){
		ArrayList<Subset> subsets = new ArrayList<Subset>();
		
		int id = 0;
		//TODO: Make this work for general sizes next after getting first step
		if(size == 1){
			for(Systems k : kprime){
				id++;
				ArrayList<Systems> subarray = new ArrayList<Systems>();
				
				subarray.add(k);
				
				Subset sub = new Subset(subarray);
				subsets.add(sub);
			}
		}else if(size == 2){

			for(int i=0; i<kprime.size()-1; i++){
				for(int j=i+1; j<kprime.size(); j++){
					id++;
					ArrayList<Systems> subarray = new ArrayList<Systems>();
					subarray.add(kprime.get(i));
					subarray.add(kprime.get(j));
					
					Subset sub = new Subset(subarray);
					subsets.add(sub);
				}
			}
			
		}
		
		return subsets;
	}
	
	private void findAllSubsets(ArrayList<Subset> subsets, ArrayList<Systems> kprime, int size, int start, int currLen, boolean [] used){
//		ArrayList<Subset> subsets = new ArrayList<Subset>();
		
		if(currLen == size){
			ArrayList<Systems> subarray = new ArrayList<Systems>();
			
			for(int i=0; i< kprime.size(); i++){
				if(used[i]== true){
					subarray.add(kprime.get(i));
				}
			}
			
			Subset sub = new Subset(subarray);
			subsets.add(sub);
			
			return;
		}
		
		if(start == kprime.size())
			return; //does nothing
		
		
		//For every index we have two options
		//1. Either we select it, means put true in used[] and make currlen+1
		used[start]=true;
		findAllSubsets(subsets, kprime, size, start+1, currLen+1, used);
		//2. Or we dont select it, means put false in used[]  and dont increase currLen
		used[start]=false;
		findAllSubsets(subsets, kprime, size, start+1, currLen, used);
		
		
//		return subsets;
		
	}
	
	private ArrayList<Systems> findKPrime(ObservableConfiguration o){
		ArrayList<Systems> kprime = new ArrayList<Systems>();
		
		for(Systems k : model.machines){
			if(o.configs.contains(k.f)){ //can be covered
				if(k.f.utility < lowerBound){
					kprime.add(k);
				}
			}
		}
		
		return kprime;
	}
	
	private ArrayList<Systems> findKPPrime(ObservableConfiguration o){
		ArrayList<Systems> kpprime = new ArrayList<Systems>();
		
		for(Systems k : model.machines){
			if(o.configs.contains(k.f)){ //can be covered
				if(k.f.utility >= lowerBound){
					kpprime.add(k);
				}
			}
		}
		
		return kpprime;
	}

	private ArrayList<Systems> findForceCoverMachines(ObservableConfiguration o){
		ArrayList<Systems> kprime = new ArrayList<Systems>();
		
		for(Systems k : model.machines){
			if(o.configs.contains(k.f)){ //can be covered
				if(k.f.utility >= lowerBound){
					kprime.add(k);
				}
			}
		}
		
		return kprime;
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
	
	private void setObservableBounds() throws IloException{
		for(ObservableConfiguration o : bounds.keySet()){
			IloNumExpr expr = cplex.constant(0.0);
			
			for(Systems k : model.machines){
				expr = cplex.sum(expr, sigmaMap.get(k).get(o));
			}
			constraints.add(cplex.eq(expr, bounds.get(o), "EQUAL_o"+o.id));
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
	
	private void printZVariables() throws UnknownObjectException, IloException {
		for(Systems k : model.machines){
			for(ObservableConfiguration o : model.obs){
				System.out.println(zMap.get(k).get(o).getName()+": "+cplex.getValue(zMap.get(k).get(o)));
			}
		}
	}
	
	private void printSigmaVariables() throws UnknownObjectException, IloException {
		for(Systems k : model.machines){
			for(ObservableConfiguration o : model.obs){
				System.out.println(sigmaMap.get(k).get(o).getName()+": "+cplex.getValue(sigmaMap.get(k).get(o)));
			}
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
	
	public Map<Systems, Map<ObservableConfiguration, Integer>> getDefenderStrategy() throws UnknownObjectException, IloException{
		Map<Systems, Map<ObservableConfiguration, Integer>> strat = new HashMap<Systems, Map<ObservableConfiguration, Integer>>();
		
		for(Systems k : sigmaMap.keySet()){
			strat.put(k, new HashMap<ObservableConfiguration, Integer>());
			for(ObservableConfiguration o : sigmaMap.get(k).keySet()){
				if(cplex.getValue(sigmaMap.get(k).get(o)) > 1-.00001 && cplex.getValue(sigmaMap.get(k).get(o)) < 1+.00001){
					//strat.get(k).put(o, (int) cplex.getValue(sigmaMap.get(k).get(o))); 
					strat.get(k).put(o, 1);//(int) cplex.getValue(sigmaMap.get(k).get(o))); 
				}else{
					strat.get(k).put(o, (int) cplex.getValue(sigmaMap.get(k).get(o))); 
				}
				//System.out.println(k.id+"  :  "+o.id+"  :  "+((int) cplex.getValue(sigmaMap.get(k).get(o))));
			}
		}
		
		return strat;
	}
	
	public void printStrategy(Map<Systems, Map<ObservableConfiguration, Integer>> strat){
		for(Systems k : strat.keySet()){
			System.out.print("K"+k.id+": ");
			for(ObservableConfiguration o : strat.get(k).keySet()){
				System.out.print("TF"+o.id+" : "+strat.get(k).get(o)+" ");
			}
			System.out.println();
		}
	}
	
	public void printCompactStrategy(Map<Systems, Map<ObservableConfiguration, Integer>> strat){
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
	
	public static double calculateUB(DeceptionGame g){
		int totalU = 0;
		for(Systems k : g.machines){
			totalU += k.f.utility;
		}
		return ((double)(totalU)/(double)g.machines.size());
	}
	
	public void printExpectedUtility(Map<Systems, Map<ObservableConfiguration, Integer>> strategy){
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
	
	public void setMaxSubsetSize(int size){
		maxSubsetSize = size;
	}
	
	public void deleteVars() throws IloException{
		sigmaMap.clear();
		zMap.clear();
		
		constraints.clear();
		
		if(cplex != null)
			cplex.end();
		//cplex.clearModel();
		
		cplex = null;
	}
	
	public void setGlobalLB(double lb){
		this.lowerBound = lb;
	}
	
	public void setMaxRuntime(double maxR){
		maxRuntime = maxR;
	}
	

	public boolean isCutoff(){
		return cutoff;
	}
	

	public void setCosts(boolean setIt){
		setCosts = setIt;
	}

	public void setMilpGap(double gap){
		this.milpGap = gap;
	}
}
