package solvers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import models.*;

public class GreedyMaxMinSolverOld {
	
	private DeceptionGame game;
	
	private Map<Systems, Map<ObservableConfiguration, Integer>> greedyStrategy;
	private Map<ObservableConfiguration, Double> euAllObs;
	
	private double defenderUtility;
	
	private double runtime;
	
	private boolean shuffle = false;
	
	public GreedyMaxMinSolverOld(DeceptionGame g){
		game = g;
		
	}
	
	public void solve(){
		double start = System.currentTimeMillis();
		
		//Calculate the f tildes which have the lowest expected utility
		euAllObs = calculateEUAllObs(game);
		for (ObservableConfiguration o : euAllObs.keySet()) {
			System.out.println("EU1(o" + o.id + "): " + euAllObs.get(o));
		}
		
		
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
		for(Systems k : game.machines)
			machinesLeft.add(k);
		
		//Should be sorted now
		if(!shuffle)
			Collections.sort(machinesLeft);
		else
			Collections.shuffle(machinesLeft);
		System.out.println();
		for(Systems k : machinesLeft)
			System.out.print(k.name+" ");
		System.out.println();

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
		
		
		/**
		 * 
		 * Right now we will correct the strategy here with switches! 
		 * Later this could be a separate function which will locally maximize some strategy by switching machines
		 */
		//locallyMaximizeSwitching();
		
		//Should also do a locally maximize swap from systems covered by an observable to the maxmin abservable
		//locallyMaximizeSwap();
		
		runtime = (System.currentTimeMillis()-start)/1000.0;
	}
	
	public void locallyMaximizeSwap(){
		//for all possible machines I could switch from one masking to another, switch if it improves the maxmin value
		//We are going to edit the greedy Strategy directly, this could be a bad idea
		
		
		while(true){
			double improvement = maximizeSwap();
			if(improvement <= 0)
				break;
		}
		
	}
	
	public double maximizeSwap(){
		double maxImprove = 0;
		
		Systems kswitch = null;
		ObservableConfiguration oswitch = null;
		boolean switchFromMaxMin = false;

		ObservableEU currentMaxMin = calculateMaxMinUtility(greedyStrategy);

		//System.out.println();
		//System.out.println("Current MaxMin: o" + currentMaxMin.o.id + " " + currentMaxMin.eu);

		// Start at min observable and test all switches to all other observables
		// For each observable \neq min observable:
		// 1. For all machines which I can swap from one to another, see if improvement is the best
		// 2. Need to set (greedy) strategy, and compute new maxmin from change (swap)
		//        (however, since this is a local change should be easier to complete)
		// 3. Only save change if it best so far, otherwise reverse it

		// Get all machines covered by \tilde{f}
		ArrayList<Systems> machinesMaxMin = new ArrayList<Systems>();
		for (Systems k : game.machines)
			if (greedyStrategy.get(k).get(currentMaxMin.o) != 0)
				machinesMaxMin.add(k);
		Collections.sort(machinesMaxMin);

		for (ObservableConfiguration o : game.obs) {
			// Skip over current lowest observable
			if (o.id == currentMaxMin.o.id)
				continue;

			//System.out.println("Testing Observable o" + o.id);

			// Get all machines covered by \tilde{f}
			ArrayList<Systems> sortedMachines = new ArrayList<Systems>();
			for (Systems k : game.machines)
				if (greedyStrategy.get(k).get(o) != 0)
					sortedMachines.add(k);
			Collections.sort(sortedMachines);

			if (sortedMachines.size() == 0) // If no machines should be nothing we can do, simple cases to check if true
				continue;

			// For each machine in observable o
			for (int i = 0; i < sortedMachines.size(); i++) {
				Systems k = sortedMachines.get(i);
				for(int j =  machinesMaxMin.size()-1; j > 0; j--){ //need to start at highest valued machine covered by maxmin
					Systems k1 = machinesMaxMin.get(j);
					
					if (!currentMaxMin.o.configs.contains(k.f) || !o.configs.contains(k1.f)) //if we can't cover skip
						continue;

					double tempImprove = 0;

					if (k.f.utility > currentMaxMin.eu && k1.f.utility < currentMaxMin.eu) {
						// assign k to be covered by maxmin
						tempImprove = calculateImprovementSwap(o, currentMaxMin, k, k1);
					} else
						break; // in this case, it is not possible to decrease eu for maxmin

					if (tempImprove > maxImprove) {
						maxImprove = tempImprove;
						oswitch = o;
						kswitch = k;
					}

					System.out.println();
					System.out.println("Swapping k" + k.id + " to o" +currentMaxMin.o.id);
					System.out.println("Improve: " + tempImprove);
					System.out.println();

					// We should be able to break after we can improve, as we have
					// switched the lowest valued machine possible from o
					//if (tempImprove > 0)
						break;
				}
			}
		}

		if(oswitch == null)
			return 0;
		
		if (!switchFromMaxMin) {
			greedyStrategy.get(kswitch).put(oswitch, greedyStrategy.get(kswitch).get(oswitch) - 1);
			greedyStrategy.get(kswitch).put(currentMaxMin.o, greedyStrategy.get(kswitch).get(currentMaxMin.o) + 1);
		} else {
			greedyStrategy.get(kswitch).put(oswitch, greedyStrategy.get(kswitch).get(oswitch) + 1);
			greedyStrategy.get(kswitch).put(currentMaxMin.o, greedyStrategy.get(kswitch).get(currentMaxMin.o) - 1);
		}

		printStrategy(greedyStrategy);
		printExpectedUtility(greedyStrategy);
		
		
		return maxImprove;
	}

	/**
	 * Swap k from o to maxmin for k1
	 * @param o
	 * @param maxmin
	 * @param k - machine to be swapped to maxmin from o
	 * @param k1 - machine to be swapped to o from maxmin
	 * @return
	 */
	public double calculateImprovementSwap(ObservableConfiguration o, ObservableEU maxmin, Systems k, Systems k1){
		double improvement = 0;
		
		//calculate EU for o and maxmin given changes
		greedyStrategy.get(k).put(o, greedyStrategy.get(k).get(o)-1);
		greedyStrategy.get(k).put(maxmin.o, greedyStrategy.get(k).get(maxmin.o)+1);
		greedyStrategy.get(k1).put(o, greedyStrategy.get(k1).get(o)+1);
		greedyStrategy.get(k1).put(maxmin.o, greedyStrategy.get(k1).get(maxmin.o)-1);
		
		double euo = calculateExpectedUtility(o, greedyStrategy);
		double eumaxmin = calculateExpectedUtility(maxmin.o, greedyStrategy);

		//change the greedyStrategy back to original
		greedyStrategy.get(k).put(o, greedyStrategy.get(k).get(o)+1);
		greedyStrategy.get(k).put(maxmin.o, greedyStrategy.get(k).get(maxmin.o)-1);
		greedyStrategy.get(k1).put(o, greedyStrategy.get(k1).get(o)-1);
		greedyStrategy.get(k1).put(maxmin.o, greedyStrategy.get(k1).get(maxmin.o)+1);
		
		if(euo < maxmin.eu)
			return (euo-maxmin.eu); //negative improvement, means we got worse
		
		if(euo > eumaxmin)
			improvement = euo-maxmin.eu;
		else
			improvement = eumaxmin - maxmin.eu;
		
		return improvement;
	}
	
	public void locallyMaximizeSwitching(){
		//for all possible machines I could switch from one masking to another, switch if it improves the maxmin value
		//We are going to edit the greedy Strategy directly, this could be a bad idea
		
		//maximizeSwitch();
		int index = 0;
		while(true){
			double improvement = maximizeSwitch();
			//if(improvement <= 0)
			if(index > 1)
				break;
			index++;
		}
				
	}
	
	private double maximizeSwitch(){
		//Need to find machine in all \tilde{f} that would increase the maxmin value by the most O(|K|)
		double maxImprove = 0;
		Systems kswitch = null;
		ObservableConfiguration oswitch = null;
		boolean switchFromMaxMin = false;

		ObservableEU currentMaxMin = calculateMaxMinUtility(greedyStrategy);

		//System.out.println();
		//System.out.println("Current MaxMin: o" + currentMaxMin.o.id + " " + currentMaxMin.eu);

		// Start at min observable and test all switches to all other observables
		// For each observable \neq min observable:
		// 1. For all machines which I can switch from one to another, see if improvement is the best
		// 2. Need to set (greedy) strategy, and compute new maxmin from change
		//        (however, since this is a local change should be easier to complete)
		// 3. Only save change if it best so far, otherwise reverse it

		// Get all machines covered by \tilde{f}
		ArrayList<Systems> machinesMaxMin = new ArrayList<Systems>();
		for (Systems k : game.machines)
			if (greedyStrategy.get(k).get(currentMaxMin.o) != 0)
				machinesMaxMin.add(k);
		Collections.sort(machinesMaxMin);

		for (ObservableConfiguration o : game.obs) {
			// Skip over current lowest observable
			if (o.id == currentMaxMin.o.id)
				continue;

			//System.out.println("Testing Observable o" + o.id);

			// Get all machines covered by \tilde{f}
			ArrayList<Systems> sortedMachines = new ArrayList<Systems>();
			for (Systems k : game.machines)
				if (greedyStrategy.get(k).get(o) != 0)
					sortedMachines.add(k);
			Collections.sort(sortedMachines);

			if (sortedMachines.size() == 0) // If no machines should be nothing
											// we can do, simple cases to check
											// if true
				continue;

			// For each machine in observable o
			for (int i = 0; i < sortedMachines.size(); i++) {
				Systems k = sortedMachines.get(i);
				if(!currentMaxMin.o.configs.contains(k.f))
					continue;
				
				double tempImprove = 0;

				if (k.f.utility > currentMaxMin.eu) {
					// assign k to be covered by maxmin
					tempImprove = calculateImprovement(o, currentMaxMin, k);
				} else
					break; // in this case, it is not possible to decrease eu
							// for maxmin

				if (tempImprove > maxImprove) {
					maxImprove = tempImprove;
					oswitch = o;
					kswitch = k;
				}

				//System.out.println();
				//System.out.println("Switching k" + k.id + " to o" + currentMaxMin.o.id);
				//System.out.println("Improve: " + tempImprove);
				//System.out.println();

				// We should be able to break after we can improve, as we have
				// switched the lowest valued machine possible from o
				if (tempImprove > 0)
					break;
			}

			// Need to check for all machines covered by current MaxMin
			// observable start highest, then go lowest since we want to switch maxMachines first
			for (int i = machinesMaxMin.size() - 1; i > 0; i--) { 
				Systems k = machinesMaxMin.get(i);
				if(!o.configs.contains(k.f))
					continue;
				
				double tempImprove = 0;

				if (k.f.utility < currentMaxMin.eu) {
					// assign k to be covered by o
					tempImprove = calculateImprovement(currentMaxMin, o, k);
				} else
					break; // in this case, it is not possible to decrease eu
							// for maxmin

				if (tempImprove > maxImprove) {
					maxImprove = tempImprove;
					oswitch = o;
					kswitch = k;
					switchFromMaxMin = true;
				}

				//System.out.println();
				//System.out.println("Switching k" + k.id + " to o" + o.id);
				//System.out.println("Improve: " + tempImprove);
				//System.out.println();

				// We should be able to break after we can improve, as we have
				// switched the highest valued machine possible from maxmin
				// observable
				if (tempImprove > 0)
					break;

			}

			//if(oswitch != null)
				//System.out.println("Observable to Switch o" + oswitch.id + " : k" + kswitch.id);
		}

		if(oswitch == null)
			return 0;
		
		if (!switchFromMaxMin) {
			greedyStrategy.get(kswitch).put(oswitch, greedyStrategy.get(kswitch).get(oswitch) - 1);
			greedyStrategy.get(kswitch).put(currentMaxMin.o, greedyStrategy.get(kswitch).get(currentMaxMin.o) + 1);
		} else {
			greedyStrategy.get(kswitch).put(oswitch, greedyStrategy.get(kswitch).get(oswitch) + 1);
			greedyStrategy.get(kswitch).put(currentMaxMin.o, greedyStrategy.get(kswitch).get(currentMaxMin.o) - 1);
		}

		printStrategy(greedyStrategy);
		printExpectedUtility(greedyStrategy);
		
		return maxImprove;
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
				for (ObservableConfiguration o : euAllObs.keySet()) {
					System.out.println("EU1(o" + o.id + "): " + euAllObs.get(o));
				}
				
				if(o1.eu == maxmin && euAllObs.get(key) < euAllObs.get(game.obs.get(i))){
					maxmin = o1.eu;
					key = game.obs.get(i);
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
		System.out.println("Not Assigned: "+notAssigned);
		
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
	
}
