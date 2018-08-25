package solvers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import models.*;

public class GreedyMaxMinSolver {
	
	private DeceptionGame game;
	
	private Map<Systems, Map<ObservableConfiguration, Integer>> greedyStrategy;
	private Map<ObservableConfiguration, Double> euAllObs;
	
	private double defenderUtility;
	
	private double runtime;
	
	private boolean shuffle = false;
	
	private boolean sortDescending = true;
	
	private ArrayList<Systems> fixedOrdering = null;
	
	private boolean randomIndifferent = false;
	
	private double maxRuntime = 0;
	
	public GreedyMaxMinSolver(DeceptionGame g){
		game = g;
	}
	
	public void solve(){
		double start = System.currentTimeMillis();
		
		//Calculate the f tildes which have the lowest expected utility
		euAllObs = calculateEUAllObs(game);
//		for (ObservableConfiguration o : euAllObs.keySet()) {
//			System.out.println("EU1(o" + o.id + "): " + euAllObs.get(o));
//		}
		
		
		//Need to have an initial strategy set to all 0s
		greedyStrategy = new HashMap<Systems, Map<ObservableConfiguration, Integer>>();
		for(Systems k : game.machines){
			greedyStrategy.put(k, new HashMap<ObservableConfiguration, Integer>());
			for(ObservableConfiguration o : game.obs){
				greedyStrategy.get(k).put(o, 0);
			}
		}
		
		ArrayList<Systems> machinesLeft = new ArrayList<Systems>();
		if(fixedOrdering != null){
			for (Systems k : fixedOrdering)
				machinesLeft.add(k);
		}else{
			for (Systems k : game.machines)
				machinesLeft.add(k);
		}
			
		
		//Need a copy of the machines array for the game model, will keep track of machines left to assign
//		ArrayList<Systems> machinesLeft = new ArrayList<Systems>();
//		for (Systems k : game.machines)
//			machinesLeft.add(k);

		//Need to automatically assign machines which can only be covered by a single configuration
		assignMachinesOneMasking(machinesLeft);
		
		
		
		//Should be sorted now
		if(fixedOrdering == null){
			if(!shuffle){
				if(sortDescending)
					Collections.sort(machinesLeft);
				else
					Collections.sort(machinesLeft, Systems.utilityAscending);
			}else
				Collections.shuffle(machinesLeft);
		}
//		System.out.println();
//		for(Systems k : machinesLeft)
//			System.out.print(k.name+" ");
//		System.out.println();

		//For all k \ in K, assign it to the possible \sigma_k,\tilde{f} s.t. max_\sigma min_{\tilde{f}} Eu(\tilde{f})
		while(!machinesLeft.isEmpty()){
			Systems k = machinesLeft.remove(0);
//			System.out.println("Assigning k"+k.id);

			ObservableEU maxminConfig = assignMachineMaxMin(greedyStrategy, k);
//			ObservableConfiguration maxminConfig = assignMachineMaxMin(greedyStrategy, k);
			
//			System.out.println("Observable "+maxminConfig.o.id+"  EU: "+maxminConfig.eu);
			
			greedyStrategy.get(k).put(maxminConfig.o, greedyStrategy.get(k).get(maxminConfig.o)+1);
			
			//System.out.println("Best to assign k"+k.id+" to be covered by "+maxminConfig.id);
			//System.out.println();
		}
		
		//printStrategy(greedyStrategy);
		//printExpectedUtility(greedyStrategy);
		ObservableEU maxminUtil = calculateMaxMinUtility(greedyStrategy);
		defenderUtility = maxminUtil.eu;
		//System.out.println(greedyStrategy);
		
		runtime = (System.currentTimeMillis()-start)/1000.0;
	}
	
	public void solveHardGMM(){
//		System.out.println("Solving Hard-GMM");
		//Solving this thing with costs!
		double start = System.currentTimeMillis();
		
		//Calculate the f tildes which have the lowest expected utility
		euAllObs = calculateEUAllObs(game);
//		for (ObservableConfiguration o : euAllObs.keySet()) {
//			System.out.println("EU1(o" + o.id + "): " + euAllObs.get(o));
//		}
		
		
		//Need to have an initial strategy set to all 0s
		greedyStrategy = new HashMap<Systems, Map<ObservableConfiguration, Integer>>();
		for(Systems k : game.machines){
			greedyStrategy.put(k, new HashMap<ObservableConfiguration, Integer>());
			for(ObservableConfiguration o : game.obs){
				greedyStrategy.get(k).put(o, 0);
			}
		}
		
		//Need a copy of the machines array for the game model, will keep track of machines left to assign
		ArrayList<Systems> machinesLeft = new ArrayList<Systems>();
		for (Systems k : game.machines)
			machinesLeft.add(k);

		int rBudget = game.Budget;
		
		//Need to automatically assign machines which can only be covered by a single configuration
//		assignMachinesOneMasking(machinesLeft); //Don't think this should make much of a difference, since we have to assign anyway
		rBudget = assignMachinesOneMasking(machinesLeft, rBudget); //Don't think this should make much of a difference, since we have to assign anyway
		
		//Should be sorted now
		if(!shuffle){
			if(sortDescending)
				Collections.sort(machinesLeft);
			else
				Collections.sort(machinesLeft, Systems.utilityAscending);
		}else
			Collections.shuffle(machinesLeft);

		//For all k \ in K, assign it to the possible \sigma_k,\tilde{f} s.t. max_\sigma min_{\tilde{f}} Eu(\tilde{f})

		
		while(!machinesLeft.isEmpty()){
			Systems k = machinesLeft.remove(0);
//			System.out.println("Assigning k"+k.id);

			ObservableEU maxminConfig = assignMachineMaxMin(greedyStrategy, k, rBudget); //This should solve for best Observable to add given it still admits feasible solution
//			ObservableEU maxminConfig = assignMachineMaxMinCost(greedyStrategy, k);
			
//			System.out.println("Observable "+maxminConfig.o.id+"  EU: "+maxminConfig.eu);
			
			rBudget -= maxminConfig.cost;
			
			if(rBudget < 0)
				System.out.println("BIG ERROR, BAD BUDGET CALCULATION");
			
			greedyStrategy.get(k).put(maxminConfig.o, greedyStrategy.get(k).get(maxminConfig.o)+1);
			
			//System.out.println("Best to assign k"+k.id+" to be covered by "+maxminConfig.id);
			//System.out.println();
		}
		
		//printStrategy(greedyStrategy);
		//printExpectedUtility(greedyStrategy);
		ObservableEU maxminUtil = calculateMaxMinUtility(greedyStrategy);
		defenderUtility = maxminUtil.eu;
		//System.out.println(greedyStrategy);
		
		runtime = (System.currentTimeMillis()-start)/1000.0;
	}
	
	public void solveSoftGMM(double lambda){
		//Solving this thing with costs!
		double start = System.currentTimeMillis();
		
		//Need to have an initial strategy set to all 0s
		greedyStrategy = new HashMap<Systems, Map<ObservableConfiguration, Integer>>();
		for(Systems k : game.machines){
			greedyStrategy.put(k, new HashMap<ObservableConfiguration, Integer>());
			for(ObservableConfiguration o : game.obs){
				greedyStrategy.get(k).put(o, 0);
			}
		}
		
		//Need a copy of the machines array for the game model, will keep track of machines left to assign
		ArrayList<Systems> machinesLeft = new ArrayList<Systems>();
		for (Systems k : game.machines)
			machinesLeft.add(k);

		int rBudget = game.Budget;	
		
		//Need to automatically assign machines which can only be covered by a single configuration
//		assignMachinesOneMasking(machinesLeft); //Don't think this should make much of a difference, since we have to assign anyway
		rBudget = assignMachinesOneMasking(machinesLeft, rBudget); //Don't think this should make much of a difference, since we have to assign anyway
		
		//Should be sorted now
		if (!shuffle) {
			if (sortDescending)
				Collections.sort(machinesLeft);
			else
				Collections.sort(machinesLeft, Systems.utilityAscending);
		} else
			Collections.shuffle(machinesLeft);
		

		
		while(!machinesLeft.isEmpty()){
			Systems k = machinesLeft.remove(0);
//			System.out.println("Assigning k"+k.id);

			ObservableEU maxminConfig = assignMachineMaxMinSoft(greedyStrategy, k, rBudget, lambda); //This should solve for best Observable to add given it still admits feasible solution
//			ObservableEU maxminConfig = assignMachineMaxMinCost(greedyStrategy, k);
			
//			System.out.println("Observable "+maxminConfig.o.id+"  EU: "+maxminConfig.eu);
			rBudget -= maxminConfig.cost;
			
			greedyStrategy.get(k).put(maxminConfig.o, greedyStrategy.get(k).get(maxminConfig.o)+1);
			
			//System.out.println("Best to assign k"+k.id+" to be covered by "+maxminConfig.id);
			//System.out.println();
		}
		
		double budgetUsed = calculateBudgetUsed(greedyStrategy);
		if(budgetUsed > game.Budget){
			System.out.println("Budget: "+278+" Used: "+budgetUsed);
		}
		
		//printStrategy(greedyStrategy);
		//printExpectedUtility(greedyStrategy);
		ObservableEU maxminUtil = calculateMaxMinUtility(greedyStrategy);
		defenderUtility = maxminUtil.eu;
		//System.out.println(greedyStrategy);
		
		runtime = (System.currentTimeMillis()-start)/1000.0;
		
		
	}
	
	private double calculateBudgetUsed(Map<Systems, Map<ObservableConfiguration, Integer>> strategy){
		double used = 0;
		for(Systems k : strategy.keySet()){
			for(ObservableConfiguration o : strategy.get(k).keySet()){
				if(strategy.get(k).get(o) > 0){
					used += game.costFunction.get(k.f).get(o);
//					System.out.println(game.costFunction.get(k.f).get(o));
				}
			}
		}
		return used;
	}
	
	private void assignMachinesOneMasking(ArrayList<Systems> machinesLeft){
		//for each machine determine if it must be assigned to a single masking
		for(Systems k : game.machines){
			int sum = 0;
			ObservableConfiguration assign = null;
			for(ObservableConfiguration o : game.obs){
				if(o.configs.contains(k.f)){
					sum++;
					assign = o;
				}
			}
			if(sum == 1){
				greedyStrategy.get(k).put(assign, 1);
				machinesLeft.remove(k);
			}
		}
		
	}
	
	private int assignMachinesOneMasking(ArrayList<Systems> machinesLeft, int rBudget){
		//for each machine determine if it must be assigned to a single masking
		for(Systems k : game.machines){
			int sum = 0;
			ObservableConfiguration assign = null;
			for(ObservableConfiguration o : game.obs){
				if(o.configs.contains(k.f)){
					sum++;
					assign = o;
				}
			}
			if(sum == 1){
				greedyStrategy.get(k).put(assign, 1);
//				System.out.println("COst: "+game.costFunction.get(k.f).get(assign));
				rBudget -= game.costFunction.get(k.f).get(assign);
				machinesLeft.remove(k);
			}
		}
		return rBudget;
	}
		
	private double calculateImprovement(ObservableConfiguration o, ObservableEU maxmin, Systems k){
		double improvement = 0;
		
		//calculate EU for o and maxmin given changes
		greedyStrategy.get(k).put(o, greedyStrategy.get(k).get(o)-1);
		greedyStrategy.get(k).put(maxmin.o, greedyStrategy.get(k).get(maxmin.o)+1);
		
		double euo = calculateExpectedUtility(o, greedyStrategy);
		double eumaxmin = calculateExpectedUtility(maxmin.o, greedyStrategy);
		
		//System.out.println("EUO: "+euo+"  MaxMin: "+eumaxmin);

		//change the greedyStrategy back to original
		greedyStrategy.get(k).put(o, greedyStrategy.get(k).get(o)+1);
		greedyStrategy.get(k).put(maxmin.o, greedyStrategy.get(k).get(maxmin.o)-1);
		
		if(euo < maxmin.eu)
			return (euo-maxmin.eu); //negative improvement, means we got worse
		
		if(euo > eumaxmin)
			improvement = euo-maxmin.eu;
		else
			improvement = eumaxmin - maxmin.eu;
		
		return improvement;
	}
	
	//should be good, check
	private double calculateImprovement(ObservableEU maxmin, ObservableConfiguration o, Systems k){ 
		double improvement = 0;
		
		//calculate EU for o and maxmin given changes
		greedyStrategy.get(k).put(o, greedyStrategy.get(k).get(o)+1);
		greedyStrategy.get(k).put(maxmin.o, greedyStrategy.get(k).get(maxmin.o)-1);
		
		double euo = calculateExpectedUtility(o, greedyStrategy);
		double eumaxmin = calculateExpectedUtility(maxmin.o, greedyStrategy);

		//System.out.println("EUO: "+euo+"  MaxMin: "+eumaxmin);
		
		//change the greedyStrategy back to original
		greedyStrategy.get(k).put(o, greedyStrategy.get(k).get(o)-1);
		greedyStrategy.get(k).put(maxmin.o, greedyStrategy.get(k).get(maxmin.o)+1);
		
		if(euo < maxmin.eu)
			return (euo-maxmin.eu); //negative improvement, means we got worse
		
		if(euo > eumaxmin)
			improvement = euo-maxmin.eu;
		else
			improvement = eumaxmin - maxmin.eu;
		
		return improvement;
	}
	
	private double calculateExpectedUtility(ObservableConfiguration o, Map<Systems, Map<ObservableConfiguration, Integer>> strategy){
		double totUt = 0;
		double tot = 0;
		for(Systems k1 : greedyStrategy.keySet()){
			totUt += greedyStrategy.get(k1).get(o)*k1.f.utility;
			tot += greedyStrategy.get(k1).get(o);
		}
		
		return (totUt/tot);
	}
	
	private ObservableEU assignMachineMaxMin(Map<Systems, Map<ObservableConfiguration, Integer>> greedyStrategy, Systems k){
		ObservableConfiguration key = null;
		double maxmin = -1000;
		
		for(int i=0; i<game.obs.size(); i++){
			//System.out.println("Config: "+k.f.id);
			//System.out.println(game.obs.get(i).toString());
			
			if(!game.obs.get(i).configs.contains(k.f)) //Make this backwards checkable, i.e., for each configuration which observables can mask it
				continue;
			
			//assign it to \tilde{f}_i
			greedyStrategy.get(k).put(game.obs.get(i), greedyStrategy.get(k).get(game.obs.get(i))+1);
			ObservableEU o1 = calculateMinObservable(greedyStrategy);
			greedyStrategy.get(k).put(game.obs.get(i), greedyStrategy.get(k).get(game.obs.get(i))-1);
			
//			System.out.println("o"+game.obs.get(i).id+" "+o1.toString());
//			System.out.println("o"+game.obs.get(i).id+" EU: "+euAllObs.get(game.obs.get(i)));
			//if(key != null)
				//System.out.println("key o"+key.id+" EU: "+euAllObs.get(key));
				
			
			
			if(o1.eu > maxmin && key == null){
				maxmin = o1.eu;
				key = game.obs.get(i);
			}else if(o1.eu > maxmin){// && euAllObs.get(key) < euAllObs.get(game.obs.get(i))){  //key is not null here
				//have to do >= bc better observable may be available
				maxmin = o1.eu;
				key = game.obs.get(i);
			}else{
				//Calculate the f tildes which have the lowest expected utility
				//Need to calculate this on the fly
				euAllObs = calculateEUAllObs(game, greedyStrategy);
//				for (ObservableConfiguration o : euAllObs.keySet()) {
//					System.out.println("EU1(o" + o.id + "): " + euAllObs.get(o));
//				}
//				
				if(randomIndifferent){
					if(o1.eu == maxmin ){
						int flip = (new Random()).nextInt(2);
						if(flip == 1){
							maxmin = o1.eu;
							key = game.obs.get(i);
						}
					}
//					if((new Random()))
				}else{
					if(o1.eu == maxmin && euAllObs.get(key) < euAllObs.get(game.obs.get(i))){
						maxmin = o1.eu;
						key = game.obs.get(i);
					}
				}
			}
			//System.out.println();
		}
		
//		System.out.println("Assigning k"+k.id+" to o"+key.id+" "+maxmin);
		
//		System.out.println();
		
		ObservableEU sol = new ObservableEU(key, maxmin);
		
		return sol;
		//return key;
	}
	
	private ObservableEU assignMachineMaxMinSoft(Map<Systems, Map<ObservableConfiguration, Integer>> greedyStrategy, Systems k, int rBudget, double lambda){
		ObservableConfiguration key = null;
		double maxmin = -1000;
		
		int minBudgetRequired = calculateMinBudget(greedyStrategy, k); //should assume that k is assigned
//		System.out.println("Min Budget Required: "+minBudgetRequired+" Budget: "+rBudget); 
		
		ArrayList<ObservableEU> observableUtilities = new ArrayList<ObservableEU>();
		
		for(int i=0; i<game.obs.size(); i++){
			//System.out.println("Config: "+k.f.id);
			//System.out.println(game.obs.get(i).toString());
			
			if(!game.obs.get(i).configs.contains(k.f)) //Make this backwards checkable, i.e., for each configuration which observables can mask it
				continue;
			
			//assign it to \tilde{f}_i
			greedyStrategy.get(k).put(game.obs.get(i), greedyStrategy.get(k).get(game.obs.get(i))+1);
			ObservableEU o1 = calculateMinObservable(greedyStrategy); //calculates the minimax value
			greedyStrategy.get(k).put(game.obs.get(i), greedyStrategy.get(k).get(game.obs.get(i))-1);
						
//			System.out.println("rBudget: "+rBudget+" Cost: "+game.costFunction.get(k.f).get(game.obs.get(i)));
			if((rBudget-game.costFunction.get(k.f).get(game.obs.get(i))) > minBudgetRequired){
				observableUtilities.add(new ObservableEU(game.obs.get(i), o1.eu, game.costFunction.get(k.f).get(game.obs.get(i))));
			}
			
		}
		
		//Need to create two separate orderings for ObservedUtilities array
		//Sample with probability according to weight from two lists
		
		//Ordered list of observables by utility
		ArrayList<ObservableEU> utilities = new ArrayList<ObservableEU>();
		Map<ObservableConfiguration, Double> utilitiesMap = new HashMap<ObservableConfiguration, Double>();
		for(ObservableEU oeu : observableUtilities){
			utilities.add(oeu);
			utilitiesMap.put(oeu.o, oeu.eu);
		}
		
		Collections.sort(utilities);
//		System.out.println(utilities.toString());
		
		//Do probabilistic sampling of system and observable configuration		
		ArrayList<ObservableEU> weightsNormalized = new ArrayList<ObservableEU>();
		for(ObservableEU oe : utilities){
			//e^(-lambda e ^ u_i)
			double value = Math.pow(Math.E, -lambda*(-1*oe.eu));//*Math.pow(Math.E, -1*oe.eu));
			ObservableEU oe1 = new ObservableEU(oe.o, value, game.costFunction.get(k.f).get(oe.o));
			weightsNormalized.add(oe1);
		}
//		System.out.println("Normalized: "+weightsNormalized.toString());
		
		double sum = 0;
		for(ObservableEU oe : weightsNormalized){
			sum += oe.eu;
		}
		
		//Normalize values for probabilities
		for(ObservableEU oe : weightsNormalized){
			oe.eu = oe.eu/sum;
		}
		
		Collections.sort(weightsNormalized);
//		System.out.println("Normalized: "+weightsNormalized.toString());
		
		double r = new Random().nextDouble();
		double total = 0;
		int indexObs = 1;
//		System.out.println("Random: "+r);
//		ObservableConfiguration obs=null;
		for(ObservableEU oe : weightsNormalized){
			total += oe.eu;
			if(total > r){
				//maxmin is equal to the utility of the observable that is chosen
				maxmin = utilitiesMap.get(oe.o);
				key = oe.o;
				break;
			}
//			System.out.println("total: "+total);
		}
//		System.out.println("Assigning k"+k.id+" to o"+key.id+" "+maxmin);
		
//		System.out.println();
		
//		System.out.println(key);
//		System.out.println(maxmin);
		
		ObservableEU sol = new ObservableEU(key, maxmin, game.costFunction.get(k.f).get(key));
		
		return sol;
		//return key;
	}
	
	private ObservableEU assignMachineMaxMin(Map<Systems, Map<ObservableConfiguration, Integer>> greedyStrategy, Systems k, int rBudget){
		ObservableConfiguration key = null;
		double maxmin = -1000;
		
		int minBudgetRequired = calculateMinBudget(greedyStrategy, k); //should assume that k is assigned
//		System.out.println("Min Budget Required: "+minBudgetRequired); 
		
		ArrayList<ObservableEU> observableUtilities = new ArrayList<ObservableEU>();
		
		for(int i=0; i<game.obs.size(); i++){
			//System.out.println("Config: "+k.f.id);
			//System.out.println(game.obs.get(i).toString());
			
			if(!game.obs.get(i).configs.contains(k.f)) //Make this backwards checkable, i.e., for each configuration which observables can mask it
				continue;
			
			//assign it to \tilde{f}_i
			greedyStrategy.get(k).put(game.obs.get(i), greedyStrategy.get(k).get(game.obs.get(i))+1);
			ObservableEU o1 = calculateMinObservable(greedyStrategy); //calculates the minimax value
			greedyStrategy.get(k).put(game.obs.get(i), greedyStrategy.get(k).get(game.obs.get(i))-1);
			
//			System.out.println("o"+game.obs.get(i).id+" "+o1.toString());
//			System.out.println("o"+game.obs.get(i).id+" EU: "+euAllObs.get(game.obs.get(i))+" maxmin:"+maxmin);
//			if(key != null)
//				System.out.println("key o"+key.id+" EU: "+euAllObs.get(key));
//			System.out.println("Cost: "+game.costFunction.get(k.f).get(game.obs.get(i))+" O"+game.obs.get(i).id+" EU:"+o1.eu);
//			System.out.println("rBudget: "+rBudget+" : "+(rBudget-game.costFunction.get(k.f).get(game.obs.get(i)))+" : "+minBudgetRequired);
			
			if(o1.eu > maxmin && key == null){
				if((rBudget-game.costFunction.get(k.f).get(game.obs.get(i))) >= minBudgetRequired){
					maxmin = o1.eu;
					key = game.obs.get(i);
				}
			}else if(o1.eu > maxmin){// && euAllObs.get(key) < euAllObs.get(game.obs.get(i))){  //key is not null here
				if((rBudget-game.costFunction.get(k.f).get(game.obs.get(i))) >= minBudgetRequired){
					// have to do >= bc better observable may be available
					maxmin = o1.eu;
					key = game.obs.get(i);
				}
			} else {
				// Calculate the f tildes which have the lowest expected utility
				// Need to calculate this on the fly
//				euAllObs = calculateEUAllObs(game, greedyStrategy); //this would calculate the wrong value now!
				//
				
				if(randomIndifferent){
					if(o1.eu == maxmin ){
						if((rBudget-game.costFunction.get(k.f).get(game.obs.get(i))) >= minBudgetRequired){
							int flip = (new Random()).nextInt(2);
							if(flip == 1){
								maxmin = o1.eu;
								key = game.obs.get(i);
							}
						}
					}
				}else{
					if (o1.eu == maxmin && euAllObs.get(key) < euAllObs.get(game.obs.get(i))) {
						if((rBudget-game.costFunction.get(k.f).get(game.obs.get(i))) >= minBudgetRequired){
							maxmin = o1.eu;
							key = game.obs.get(i);
						}
					}
				}
			}
			observableUtilities.add(new ObservableEU(o1.o, o1.eu, game.costFunction.get(k.f).get(game.obs.get(i))));
			
			
			//System.out.println();
		}
		
//		System.out.println("Assigning k"+k.id+" to o"+key.id+" "+maxmin);
//		
//		System.out.println();
		
		ObservableEU sol = new ObservableEU(key, maxmin, game.costFunction.get(k.f).get(key));
		
		return sol;
		//return key;
	}
	
	private int calculateMinBudget(Map<Systems, Map<ObservableConfiguration, Integer>> greedyStrategy){
		// calculate min budget needed
		int totalMinCost = 0;
		for (Systems k : game.machines) {
			//check if the system is currently covered
			boolean covered = false;
			for(ObservableConfiguration o : game.obs){
				if(greedyStrategy.get(k).get(o)>0){
					covered = true;
					break;
				}
			}
			
			if(!covered){
				int minCost = 10000;
				for (ObservableConfiguration o : game.obs) {
					if (o.configs.contains(k.f)) {
						if (game.costFunction.get(k.f).get(o) < minCost)
							minCost = game.costFunction.get(k.f).get(o);
					}
				}
				totalMinCost += minCost;
			}
		}
		
		return totalMinCost;
	}
	
	private int calculateMinBudget(Map<Systems, Map<ObservableConfiguration, Integer>> greedyStrategy, Systems dontConsider){
		// calculate min budget needed
		int totalMinCost = 0;
		for (Systems k : game.machines) {
			//check if the system is currently covered
			boolean covered = false;
			for(ObservableConfiguration o : game.obs){
				if(greedyStrategy.get(k).get(o)>0){
					covered = true;
					break;
				}
			}
			
			if(!covered && k.id != dontConsider.id){
				int minCost = 10000;
				for (ObservableConfiguration o : game.obs) {
					if (o.configs.contains(k.f)) {
						if (game.costFunction.get(k.f).get(o) < minCost)
							minCost = game.costFunction.get(k.f).get(o);
					}
				}
				totalMinCost += minCost;
			}
		}
		
		return totalMinCost;
	}
	
	public ObservableEU calculateMinObservable(Map<Systems, Map<ObservableConfiguration, Integer>> greedyStrategy){
		double min = 0;
		ObservableConfiguration minkey = null;
		
		for(ObservableConfiguration o : game.obs){
			double totUt = 0;
			double tot = 0;
			for(Systems k : greedyStrategy.keySet()){
				totUt += greedyStrategy.get(k).get(o)*k.f.utility;
				tot += greedyStrategy.get(k).get(o);
			}
			
			if((totUt/tot) < min){
				min = (totUt/tot);
				minkey = o;
			}
		}
		
		ObservableEU obsMin = new ObservableEU(minkey, min);
		defenderUtility = obsMin.eu;
		
		return obsMin;
	}
	
	public ArrayList<ObservableEU> calculateMinObservableList(Map<Systems, Map<ObservableConfiguration, Integer>> greedyStrategy){
		ArrayList<ObservableEU> list = new ArrayList<ObservableEU>();
		//double min = 0;
		//ObservableConfiguration minkey = null;
		
		for(ObservableConfiguration o : game.obs){
			double totUt = 0;
			double tot = 0;
			for(Systems k : greedyStrategy.keySet()){
				totUt += greedyStrategy.get(k).get(o)*k.f.utility;
				tot += greedyStrategy.get(k).get(o);
			}
			
//			if((totUt/tot) < min){
//				min = (totUt/tot);
//				minkey = o;
//			}

			ObservableEU oeu = new ObservableEU(o, totUt/tot);
			list.add(oeu);
		}
		
		//ObservableEU obsMin = new ObservableEU(minkey, min);
//		defenderUtility = obsMin.eu;
		
		return list;
	}
	
	public ObservableEU calculateMaxMinUtility(Map<Systems, Map<ObservableConfiguration, Integer>> strategy) {
		double maxmin = 0;
		ObservableConfiguration key = null;
		
		for(ObservableConfiguration o : game.obs){
			double expectedU = 0;
			double total = 0;
			for(Systems k : strategy.keySet()){
				expectedU += strategy.get(k).get(o)*k.f.utility;
				total += strategy.get(k).get(o);
			}
			
			if(maxmin > (expectedU/total)){
				maxmin = (expectedU/total);
				key = o;
			}
		}
		ObservableEU o1 = new ObservableEU(key, maxmin);
	
		
		return o1;
	}
	
	private Map<ObservableConfiguration, Double> calculateEUAllObs(DeceptionGame g, Map<Systems, Map<ObservableConfiguration, Integer>> strategy){
		//find systems that haven't been assigned
		ArrayList<Systems> notAssigned = new ArrayList<Systems>();
		for(Systems k : g.machines){
			int sum = 0;
			for(ObservableConfiguration o : g.obs){
				sum += strategy.get(k).get(o);
			}
			if(sum == 0)
				notAssigned.add(k);
		}
//		System.out.println("Not Assigned: "+notAssigned);
		
		Map<ObservableConfiguration, Double> euAllObs = new HashMap<ObservableConfiguration, Double>();
		
		for(ObservableConfiguration o : g.obs){
			double euObs = 0;
			double totalObs = 0;
			//Add values from strategy
			for(Systems k : g.machines){
				euObs += k.f.utility*strategy.get(k).get(o);
				totalObs += strategy.get(k).get(o);
			}
			
			//Add values from machines I can cover that aren't covered
			for(Systems k : notAssigned){
				//If we can mask k with o then add it to set
				if(o.configs.contains(k.f)){
					euObs += k.f.utility;
					totalObs++;
				}
			}
			euAllObs.put(o, (euObs/totalObs));
		}
		
		
		return euAllObs;
	}
	
	private Map<ObservableConfiguration, Double> calculateEUAllObs(DeceptionGame g){
		Map<ObservableConfiguration, Double> euAllObs = new HashMap<ObservableConfiguration, Double>();
		
		for(ObservableConfiguration o : g.obs){
			double euObs = 0;
			double totalObs = 0;
			for(Systems k : g.machines){
				//If we can mask k with o then add it to set
				if(o.configs.contains(k.f)){
					euObs += k.f.utility;
					totalObs++;
				}
			}
			euAllObs.put(o, (euObs/totalObs));
		}
		
		return euAllObs;
	}
	
	public void printStrategy(Map<Systems, Map<ObservableConfiguration, Integer>> strat){
		System.out.println();
		for(Systems k : strat.keySet()){
			System.out.print("K"+k.id+": ");
			for(ObservableConfiguration o : strat.get(k).keySet()){
				System.out.print("TF"+o.id+" : "+strat.get(k).get(o)+" ");
			}
			System.out.println();
		}
		System.out.println();
	}
	
	public void printExpectedUtility(Map<Systems, Map<ObservableConfiguration, Integer>> strategy){
		double expectedU = 0;
		double total = 0;
		System.out.println();
		for(ObservableConfiguration o : game.obs){
			for(Systems k : strategy.keySet()){
				expectedU += strategy.get(k).get(o)*k.f.utility;
				total += strategy.get(k).get(o);
			}
			System.out.println("EU(o"+o.id+"): "+(expectedU/total));
			expectedU = 0;
			total=0;
		}
		System.out.println();
	}
	
	public Map<Systems, Map<ObservableConfiguration, Integer>> getGreedyStrategy(){
		return greedyStrategy;
	}
	
	public double getDefenderUtility(){
		return defenderUtility;
	}
	
	public double getRuntime(){
		return runtime;
	}
	
	public void setShuffle(boolean shuffle){
		this.shuffle = shuffle;
	}
	
	public void setDescending(boolean value){
		sortDescending = value;
	}
	
	public void setFixedOrdering(ArrayList<Systems> ordering){
		fixedOrdering = ordering;
	}
	
	public void setRandomIndifferent(){
		randomIndifferent = true;
	}
	
	public void setMaxRuntime(int maxRuntime){
		this.maxRuntime = maxRuntime;
	}
}
