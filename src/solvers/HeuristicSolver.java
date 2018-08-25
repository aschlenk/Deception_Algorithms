package solvers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import models.Configuration;
import models.DeceptionGame;
import models.ObservableConfiguration;
import models.ObservableEU;
import models.Systems;

public class HeuristicSolver {
	
	private DeceptionGame g;
	
	public HeuristicSolver(DeceptionGame g){
		this.g = g;
	}
	
	public void solve(){
		//Find all sets of observables that have to have configurations in them
		Map<Configuration, ObservableConfiguration> minCon = new HashMap<Configuration, ObservableConfiguration>();
		for(Configuration f : g.configs){
			if(f.obsConfigs.size() == 1){
				minCon.put(f, f.obsConfigs.get(0));
			}
		}
		
		//minimum set cover of problem is size of minCon map
		Map<Configuration, Map<ObservableConfiguration, Integer>> heurStrategy = new HashMap<Configuration, Map<ObservableConfiguration, Integer>>();
		for(Configuration f : g.configs){
			heurStrategy.put(f, new HashMap<ObservableConfiguration, Integer>());
			for(ObservableConfiguration o : g.obs){
				heurStrategy.get(f).put(o, 0);
			}
		}
		for(Configuration f : minCon.keySet()){
			//heurStrategy.put(f, new HashMap<ObservableConfiguration, Integer>());
			heurStrategy.get(f).put(minCon.get(f), calculateNumMachines(f)); //should put total number of machines with configuration 
		}
		
		//System.out.println(heurStrategy);

		//Calculate the f tildes which have the lowest expected utility
		Map<ObservableConfiguration, Double> euAllObs = calculateEUAllObs(g);//new HashMap<ObservableConfiguration, Double>();
		for(ObservableConfiguration o : euAllObs.keySet()){
			System.out.println("EU1(o"+o.id+"): "+euAllObs.get(o));
		}
		
		//Now we need to randomly assign the rest of the configurations 1 by 1 to observables until N_{\tilde{f}} is equal for all
		//Add all machines to sets f tilde with lowest value first
		double max = -1000;
		for(ObservableConfiguration o : euAllObs.keySet()){
			if(euAllObs.get(o) > max){
				max = euAllObs.get(o);
			}
		}
		
		ArrayList<ObservableEU> sortedObs = new ArrayList<ObservableEU>();
		for(ObservableConfiguration o : euAllObs.keySet()){
			ObservableEU o1 = new ObservableEU(o, euAllObs.get(o));
			sortedObs.add(o1);
		}
		Collections.sort(sortedObs);
		
		//for(ObservableEU o : sortedObs)
			//System.out.println(o.o.id+": "+o.eu);
	
		Map<Systems, Boolean> assigned = new HashMap<Systems, Boolean>();
		for(Systems k : g.machines){
			assigned.put(k, false);
		}
		
		for(ObservableEU oeu : sortedObs){
			for(Systems k : g.machines){
				if(oeu.o.configs.contains(k.f)){
					if(assigned.get(k) == false){
						if(heurStrategy.get(k.f).get(oeu.o) != null)
							heurStrategy.get(k.f).put(oeu.o, heurStrategy.get(k.f).get(oeu.o)+1);
						else
							heurStrategy.get(k.f).put(oeu.o, 1);
						assigned.put(k, true);
					}
				}
			}
		}
		
		printConfigStrategy(heurStrategy);
		
		//Have an initial assignment, now we to improve utility
		//Now we need to either swap or switch until we can't improve
		swap(heurStrategy);
		
		double expectedUtility = calculateExpectedUtility(heurStrategy);
		System.out.println("EU: "+expectedUtility);
		
	}
	
	public double calculateExpectedUtility(Map<Configuration, Map<ObservableConfiguration, Integer>> heurStrategy){
		// calculate utility for each configuration
		ArrayList<ObservableEU> obsEU = new ArrayList<ObservableEU>();
		for (ObservableConfiguration o : g.obs) {
			double eu = 0;
			double tot = 0;
			for (Configuration f : heurStrategy.keySet()) {
				eu += heurStrategy.get(f).get(o) * f.utility;
				tot += heurStrategy.get(f).get(o);
			}
			ObservableEU oeu = new ObservableEU(o, (eu / tot));
			obsEU.add(oeu);
		}
		Collections.sort(obsEU);
		
		//System.out.println(obsEU.get(obsEU.size()-1).eu);
		if(!Double.isNaN(obsEU.get(obsEU.size()-1).eu))
			return obsEU.get(obsEU.size()-1).eu;
		else
			return obsEU.get(0).eu;
	}
	
	public void swap(Map<Configuration, Map<ObservableConfiguration, Integer>> heurStrategy){
		//calculate utility for each configuration
		ArrayList<ObservableEU> obsEU = new ArrayList<ObservableEU>();
		for(ObservableConfiguration o : g.obs){
			double eu = 0;
			double tot = 0;
			for(Configuration f : heurStrategy.keySet()){
				eu += heurStrategy.get(f).get(o)*f.utility;
				tot += heurStrategy.get(f).get(o);
			}
			ObservableEU oeu = new ObservableEU(o, (eu/tot));
			obsEU.add(oeu);
		}
		Collections.sort(obsEU);
		
		int size = obsEU.size()-1;
		boolean canSwap = false;
		int index = 0;
		while(obsEU.get(0).eu > obsEU.get(size).eu){ //expected utility between observables is greater than 0, meaning it might be possible to improve
			//need to find a swap
			Configuration fi = null;
			for(Configuration f : heurStrategy.keySet()){
				double maxUtility = -100;
				
				if(heurStrategy.get(f).get(obsEU.get(0).o) > 0){
					if(obsEU.get(size).o.configs.contains(f)){ //If I can cover configuration w last observable
						if(f.utility > maxUtility){
							fi = f;
							maxUtility = f.utility;
						}
					}
				}
			}
			
			System.out.println(fi);
			
			Configuration fj = null;
			for(Configuration f : heurStrategy.keySet()){
				double minUtility = 0;
				
				if(heurStrategy.get(f).get(obsEU.get(size).o) > 0){
					if(obsEU.get(0).o.configs.contains(f)){ //If I can cover configuration w last observable
						if(f.utility < minUtility){
							fj = f;
							minUtility = f.utility;
						}
					}
				}
			}
			
			System.out.println(fj);
			//swap coverage
			
			if(fi != null && fj != null)
				canSwap = true;
			
			boolean switchConfigs = true;
			
			if(canSwap){
				if(fi.utility == fj.utility)
					switchConfigs = true;
				else{
					switchConfigs = false;
					//so the swap and not the switch
				}
			}
			
			
			boolean cantSwitch1 = false;
			boolean cantSwitch2 = false;
			if(switchConfigs){//do a switch instead
				if(fi != null){ //switch fi to be covered by obsEU.get(size)
					if(fi.utility > obsEU.get(size).eu){
						heurStrategy.get(fi).put(obsEU.get(0).o, heurStrategy.get(fi).get(obsEU.get(0).o)-1);
						heurStrategy.get(fi).put(obsEU.get(size).o, heurStrategy.get(fi).get(obsEU.get(size).o)+1);
					}else{
						cantSwitch1 = true;
						cantSwitch2 = true;
					}
				}else if(fj != null){ //canSwap2
					if(fj.utility < obsEU.get(size).eu){
						heurStrategy.get(fj).put(obsEU.get(0).o, heurStrategy.get(fj).get(obsEU.get(0).o)+1);
						heurStrategy.get(fj).put(obsEU.get(size).o, heurStrategy.get(fj).get(obsEU.get(size).o)-1);
					}else
						cantSwitch1 = true;
						cantSwitch2 = true;
				}else{
					cantSwitch1 = true;
					cantSwitch2 = true;
					//I think do nothing
					
				}
			}
			
			obsEU.clear();
			for(ObservableConfiguration o : g.obs){
				double eu = 0;
				double tot = 0;
				for(Configuration f : heurStrategy.keySet()){
					eu += heurStrategy.get(f).get(o)*f.utility;
					tot += heurStrategy.get(f).get(o);
				}
				ObservableEU oeu = new ObservableEU(o, (eu/tot));
				obsEU.add(oeu);
			}
			Collections.sort(obsEU);

			for(ObservableEU o : obsEU)
				System.out.println(o.o.id+": "+o.eu);
			
			printConfigStrategy(heurStrategy);
			
			index++;
			System.out.println("Run "+index);
			if(cantSwitch1 && cantSwitch2)
				break;
			
			if(index == 5)
				break;
			
			//if(!canSwap1 && !canSwap2)
			canSwap = false;
		}
		
		
		
	}
	
	public Map<ObservableConfiguration, Double> calculateEUAllObs(DeceptionGame g){
		Map<ObservableConfiguration, Double> euAllObs = new HashMap<ObservableConfiguration, Double>();
		
		double euObs = 0;
		double totalObs = 0;
		for(ObservableConfiguration o : g.obs){
			for(Systems k : g.machines){
				//If we can mask k with o then add it to set
				if(o.configs.contains(k.f)){
					euObs += k.f.utility;
					totalObs++;
				}
			}
			euAllObs.put(o, (euObs/totalObs));
			euObs = 0;
			totalObs = 0;
		}
		
		return euAllObs;
	}
	
	public int calculateNumMachines(Configuration f){
		int total = 0;
		for(Systems k : g.machines){
			if(k.f.id == f.id)
				total += 1;
		}
		return total;
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
	
	public void printConfigStrategy(Map<Configuration, Map<ObservableConfiguration, Integer>> strat){
		for(Configuration f : strat.keySet()){
			System.out.print("F"+f.id+": ");
			for(ObservableConfiguration o : strat.get(f).keySet()){
				System.out.print("O"+o.id+" : "+strat.get(f).get(o)+" ");
			}
			System.out.println();
		}
	}

}
