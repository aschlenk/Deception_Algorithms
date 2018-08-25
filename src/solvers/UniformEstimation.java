package solvers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import models.Configuration;
import models.DeceptionGame;
import models.Systems;
import models.ObservableConfiguration;
import models.ObservableEU;


public class UniformEstimation {
	
	private DeceptionGame game;
	
	private Map<Systems, Map<ObservableConfiguration, Integer>> sigma;
	
	private double defenderUtility;
	private ObservableConfiguration tfCover;
	private boolean cost;
	
	public UniformEstimation(DeceptionGame game){
		this.game = game;
		cost = false;
		sigma = new HashMap<Systems, Map<ObservableConfiguration, Integer>>();
		for(Systems k : game.machines){
			sigma.put(k, new HashMap<ObservableConfiguration, Integer>());
			for(ObservableConfiguration o : game.obs){
				sigma.get(k).put(o, 0);
			}
		}
	}
	
	public UniformEstimation(DeceptionGame game, boolean cost){
		this.game = game;
		this.cost = cost;
		sigma = new HashMap<Systems, Map<ObservableConfiguration, Integer>>();
		for(Systems k : game.machines){
			sigma.put(k, new HashMap<ObservableConfiguration, Integer>());
			for(ObservableConfiguration o : game.obs){
				sigma.get(k).put(o, 0);
			}
		}
	}
	
	public void solve() throws Exception{
		if(cost)
			UECost();
		else
			UE1();
//			UE();
		
	}
	
	private void UECost() throws Exception{
		System.out.println("Running UE w Cost");
		
		//Need to get estimates of the utilities for all Observable Configurations!
		//Do uniform estimation
		Map<ObservableConfiguration, Double> expectedUtilities = calculateEUUniformEstimation();
		
		//initialize Gamma = empty set
		ArrayList<Systems> gamma = new ArrayList<Systems>();
		ObservableConfiguration tfstar = null;
		//K' = AllInducibleSystems(K)
		ArrayList<Systems> kprime = AllInducibleSystems(expectedUtilities);
		System.out.println("Kprime: "+kprime.toString());
		
//		System.out.println("Gamma'"+gamma.toString()+" : Average: "+Average(gamma));
		//Need to check each of these lines for all Observable Configurations that can cover k, depending on observable we may change L_1 and L_2
		for(ObservableConfiguration o : game.obs){
			//Check to see if it is possible to cover k with o and put it in the attack set gamma
			if(!checkIfAllInducible(expectedUtilities, o)){  //***** This is a check for P1
				System.out.println("Some systems not inducible");
				continue;
			}
			System.out.println("o"+o.id);
				
			//   initialize Gamma' to empty set
			ArrayList<Systems> gammaprime = new ArrayList<Systems>();
				
			//   Gamma' U L_1 => L_1 = InducedSystems(k); **** this is now P2
//			ArrayList<Systems> l1 = InducedSystems(expectedUtilities, kprime, k); 
			ArrayList<Systems> p2 = InducedSystems(expectedUtilities, kprime, o);
			gammaprime.addAll(p2);
			System.out.println(p2.toString());
	
//				System.out.println("l1: "+l1.toString());
			//   L_2 = InducibleSystems(k) ; ******** this is now P3
//			ArrayList<Systems> l2 = InducibleSystems(expectedUtilities, kprime, k);
			ArrayList<Systems> p3 = InducibleSystems(expectedUtilities, kprime, o);
			Collections.sort(p3);	
			System.out.println(p3.toString());
			
			boolean feasible = true;
			
			//Find P4 to set variables all to 0?!
			//if(gammaprime.size()==0){
				//Add systems from p3, if possible
			//}else{
				//Need to solve MILP w/ p2 in gammaprime and other variables free
				MILPUE solver = new MILPUE(game, o, gammaprime, expectedUtilities);
				
				solver.solve();
				
				feasible = solver.getFeasible();
				
				if(feasible){
					gammaprime = solver.getAttackSet();
					
					sigma = solver.getDefenderStrategy();
					
					System.out.println("Gamma '"+gammaprime.toString());
				}else{
					System.out.println("Not feasible for o"+o.id);
				}
			//}
					
			if(Average(gammaprime) > Average(gamma) && feasible){
				gamma = gammaprime;
				tfstar = o;
			}
		}
		
		//
		//return Gamma
			
		defenderUtility = Average(gamma);
		tfCover = tfstar;
		//return gamma;
		
//		System.out.println("Sigma: "+sigma);
	}
	
	private void UE1(){
		System.out.println("Running UE w/o Cost");
		
		//Need to get estimates of the utilities for all Observable Configurations!
		// Do uniform estimation
		Map<ObservableConfiguration, Double> expectedUtilities = calculateEUUniformEstimation();

		 System.out.println(expectedUtilities);

		// initialize Gamma = empty set
		ArrayList<Systems> gamma = new ArrayList<Systems>();
		ObservableConfiguration tfstar = null;
		// K' = AllInducibleSystems(K)
		ArrayList<Systems> kprime = AllInducibleSystems(expectedUtilities);
		// System.out.println("Kprime: "+kprime.toString());

		// System.out.println("Gamma'"+gamma.toString()+" : Average:
		// "+Average(gamma));
		// Need to check each of these lines for all Observable Configurations that can cover k, depending on observable we may change L_1 and L_2S
		for (ObservableConfiguration o : game.obs) {
			// Check to see if it is possible to cover all k with o as the observable for the attack set gamma
			if (!checkIfAllInducible(expectedUtilities, o)){
//				System.out.println("Some systems not inducible o"+o.id);
				continue;
			}

			// initialize Gamma' to empty set
			ArrayList<Systems> gammaprime = new ArrayList<Systems>();

			// Gamma' U L_1 => L_1 = InducedSystems(k)
			// ArrayList<Systems> l1 = InducedSystems(expectedUtilities, kprime, k);
			ArrayList<Systems> l1 = InducedSystems(expectedUtilities, kprime, o);
			gammaprime.addAll(l1);

//			 System.out.println("l1: "+l1.toString());
			 
			// L_2 = InducibleSystems(k)
			// ArrayList<Systems> l2 = InducibleSystems(expectedUtilities, kprime, k);
			ArrayList<Systems> l2 = InducibleSystems(expectedUtilities, kprime, o);
			Collections.sort(l2);
			
			// System.out.println("l2: "+l2.toString());
			// for k' \in L_2
			for (Systems k1 : l2) {
				// if(Avg(Gamma' U k') < Avg(Gamma'))
//				System.out.println("Average Gamma: "+Average(gammaprime)+" : Average Gamma w/ k: "+Average(gammaprime, k1));
				if (Average(gammaprime, k1) > Average(gammaprime)) {
					// Gamma' U k'
					gammaprime.add(k1);
				}
			}
			// System.out.println("K"+k.id+" O"+o.id+" EU: "+expectedUtilities.get(o)+" Gamma'"+gammaprime.toString()+"
			// : Average: "+Average(gammaprime));

			if (Average(gammaprime) > Average(gamma)) {
				gamma = gammaprime;
				tfstar = o;
			}
		}
		
		//
		// return Gamma

		defenderUtility = Average(gamma);
		 System.out.println("Gamma: "+gamma.toString()+" : "+defenderUtility);
		// Gamma is the final set the adversary will attack.
		// Should also return a strategy that achieves this value

		double minUtilitySet = 0;
		ObservableConfiguration oCover = null;
		/*
		 * for(Systems k : gamma){ sigma.get(k).put(tfstar, 1); }
		 */
		for (ObservableConfiguration o : game.obs) {
			boolean coverAll = true;

			for (Systems k : gamma) {
				if (!o.configs.contains(k.f)) {
					coverAll = false;
					break;
				}
			}

			// System.out.println("O"+o.id+" Cover all:"+coverAll+ "
			// EU:"+expectedUtilities.get(o));

			if (coverAll && expectedUtilities.get(o) < minUtilitySet) {
				// System.out.println("O"+o.id+" Min Utility Set:
				// "+minUtilitySet+" EU:"+expectedUtilities.get(o));
				minUtilitySet = expectedUtilities.get(o);
				if (oCover != null) {
					for (Systems k : gamma) {
						sigma.get(k).put(oCover, 0);
					}
				}
				oCover = o;
				for (Systems k : gamma) {
					sigma.get(k).put(o, 1);
				}
			}
		}
		// System.out.println("O"+oCover.id+" Min Utility Set: "+minUtilitySet);

		// To return a strategy we should assign all other systems such that
		// EU(sigma_k,tf) < EU(sigma_k,tf)
		// each k not in gamma, should be assigned an observable such that it's
		// value is less than the observable used for gamma

		for (Systems k : game.machines) {

			if (!gamma.contains(k)) {
				for (ObservableConfiguration o : game.obs) {
					// System.out.println("K"+k.id+" O"+o.id+" Min Util:
					// "+minUtilitySet);

					if (o.configs.contains(k.f)) {
						// System.out.println("O"+o.id+" Min Util:
						// "+minUtilitySet);
						if (expectedUtilities.get(o) > minUtilitySet) {
							sigma.get(k).put(o, 1);
							break;
						}
					}
				}

			}
		}

		tfCover = tfstar;
		// return gamma;

		// System.out.println("Sigma: "+sigma);
		
	}
	
	private void UE(){
		System.out.println("Running UE w/o Cost");
		
		//Need to get estimates of the utilities for all Observable Configurations!
		// Do uniform estimation
		Map<ObservableConfiguration, Double> expectedUtilities = calculateEUUniformEstimation();

		// System.out.println(expectedUtilities);

		// initialize Gamma = empty set
		ArrayList<Systems> gamma = new ArrayList<Systems>();
		ObservableConfiguration tfstar = null;
		// K' = AllInducibleSystems(K)
		ArrayList<Systems> kprime = AllInducibleSystems(expectedUtilities);
		// System.out.println("Kprime: "+kprime.toString());

		// System.out.println("Gamma'"+gamma.toString()+" : Average:
		// "+Average(gamma));
		// for k \in K'
		for (Systems k : kprime) {

			// Need to check each of these lines for all Observable
			// Configurations that can cover k, depending on observable we may
			// change L_1 and L_2S
			for (ObservableConfiguration o : game.obs) {
				if (!o.configs.contains(k.f))
					continue;

				// Check to see if it is possible to cover k with o and put it
				// in the attack set gamma
				if (!checkIfInducible(expectedUtilities, kprime, k, o))
					continue;

				// initialize Gamma' to empty set
				ArrayList<Systems> gammaprime = new ArrayList<Systems>();
				gammaprime.add(k);

				// Gamma' U L_1 => L_1 = InducedSystems(k)
				// ArrayList<Systems> l1 = InducedSystems(expectedUtilities,
				// kprime, k);
				ArrayList<Systems> l1 = InducedSystems(expectedUtilities, kprime, k, o);
				gammaprime.addAll(l1);

				// System.out.println("l1: "+l1.toString());
				// L_2 = InducibleSystems(k)
				// ArrayList<Systems> l2 = InducibleSystems(expectedUtilities,
				// kprime, k);
				ArrayList<Systems> l2 = InducibleSystems(expectedUtilities, kprime, k, o);

				// System.out.println("l2: "+l2.toString());
				// for k' \in L_2
				for (Systems k1 : l2) {
					// if(Avg(Gamma' U k') < Avg(Gamma'))
					if (Average(gammaprime, k1) > Average(gammaprime)) {
						// Gamma' U k'
						gammaprime.add(k1);
					}
				}
				// System.out.println("K"+k.id+" O"+o.id+" EU:
				// "+expectedUtilities.get(o)+" Gamma'"+gammaprime.toString()+"
				// : Average: "+Average(gammaprime));

				if (Average(gammaprime) > Average(gamma)) {
					gamma = gammaprime;
					tfstar = o;
				}
			}
		}
		//
		// return Gamma

		defenderUtility = Average(gamma);
		// System.out.println("Gamma: "+gamma.toString()+" : "+defenderUtility);
		// Gamma is the final set the adversary will attack.
		// Should also return a strategy that achieves this value

		double minUtilitySet = 0;
		ObservableConfiguration oCover = null;
		/*
		 * for(Systems k : gamma){ sigma.get(k).put(tfstar, 1); }
		 */
		for (ObservableConfiguration o : game.obs) {
			boolean coverAll = true;

			for (Systems k : gamma) {
				if (!o.configs.contains(k.f)) {
					coverAll = false;
					break;
				}
			}

			// System.out.println("O"+o.id+" Cover all:"+coverAll+ "
			// EU:"+expectedUtilities.get(o));

			if (coverAll && expectedUtilities.get(o) < minUtilitySet) {
				// System.out.println("O"+o.id+" Min Utility Set:
				// "+minUtilitySet+" EU:"+expectedUtilities.get(o));
				minUtilitySet = expectedUtilities.get(o);
				if (oCover != null) {
					for (Systems k : gamma) {
						sigma.get(k).put(oCover, 0);
					}
				}
				oCover = o;
				for (Systems k : gamma) {
					sigma.get(k).put(o, 1);
				}
			}
		}
		// System.out.println("O"+oCover.id+" Min Utility Set: "+minUtilitySet);

		// To return a strategy we should assign all other systems such that
		// EU(sigma_k,tf) < EU(sigma_k,tf)
		// each k not in gamma, should be assigned an observable such that it's
		// value is less than the observable used for gamma

		for (Systems k : game.machines) {

			if (!gamma.contains(k)) {
				for (ObservableConfiguration o : game.obs) {
					// System.out.println("K"+k.id+" O"+o.id+" Min Util:
					// "+minUtilitySet);

					if (o.configs.contains(k.f)) {
						// System.out.println("O"+o.id+" Min Util:
						// "+minUtilitySet);
						if (expectedUtilities.get(o) > minUtilitySet) {
							sigma.get(k).put(o, 1);
							break;
						}
					}
				}

			}
		}

		tfCover = tfstar;
		// return gamma;

		// System.out.println("Sigma: "+sigma);
		
	}
	
	private double Average(ArrayList<Systems> set){
		if(set.size()==0)
			return -1000;
		
		double average = 0;
		for(Systems k : set){
			average += k.f.utility;
		}
		average = average/(double)set.size();
		return average;
	}
	
	private double Average(ArrayList<Systems> set, Systems k1){
		double average = k1.f.utility;
		for(Systems k : set){
			average += k.f.utility;
		}
		average = average/(set.size()+1.0);
		return average;
	}
	
	private ArrayList<Systems> InducibleSystems(Map<ObservableConfiguration, Double> EUs, ArrayList<Systems> kprime, ObservableConfiguration o2){
		ArrayList<Systems> l2 = new ArrayList<Systems>();
		
		double minUtilk = EUs.get(o2);
//		for(ObservableConfiguration o : game.obs){
//			if(o.configs.contains(k.f)){
//				if(EUs.get(o) < minUtilk){
//					minUtilk = EUs.get(o);
//				}
//			}
//			
//		}
//		System.out.println("MinutilK"+k.id+" : "+minUtilk);
		
		for(Systems k1 : kprime){
			
			double maxUtilk1 = -100;
			for(ObservableConfiguration o : game.obs){
				if(o.configs.contains(k1.f)){
					if(EUs.get(o) > maxUtilk1){
						maxUtilk1 = EUs.get(o);
					}
				}
				
			}

//			System.out.println("MaxUtilK"+k1.id+" : "+maxUtilk1);
			
			if(maxUtilk1 > minUtilk){
				if(o2.configs.contains(k1.f))
					l2.add(k1);
			}
			
		}
		
		
		return l2;
	}
	
	private ArrayList<Systems> InducibleSystems(Map<ObservableConfiguration, Double> EUs, ArrayList<Systems> kprime, Systems k, ObservableConfiguration o2){
		ArrayList<Systems> l2 = new ArrayList<Systems>();
		
		double minUtilk = EUs.get(o2);
//		for(ObservableConfiguration o : game.obs){
//			if(o.configs.contains(k.f)){
//				if(EUs.get(o) < minUtilk){
//					minUtilk = EUs.get(o);
//				}
//			}
//			
//		}
//		System.out.println("MinutilK"+k.id+" : "+minUtilk);
		
		for(Systems k1 : kprime){
			if(k1.id == k.id)
				continue;
			
			double maxUtilk1 = -100;
			for(ObservableConfiguration o : game.obs){
				if(o.configs.contains(k1.f)){
					if(EUs.get(o) > maxUtilk1){
						maxUtilk1 = EUs.get(o);
					}
				}
				
			}

//			System.out.println("MaxUtilK"+k1.id+" : "+maxUtilk1);
			
			boolean canEqual = false;
			if(maxUtilk1 > minUtilk){
				//TODO: This was changed, maybe change it back!
				/*for(ObservableConfiguration o : game.obs){
					if(o.configs.contains(k1.f)){
							
						if(EUs.get(o2) == EUs.get(o)){
							canEqual = true;
							break;
						}
					}
				}*/
				
				//if(canEqual)
				if(o2.configs.contains(k1.f))
					l2.add(k1);
			}
			
		}
		
		
		return l2;
	}
	
	private ArrayList<Systems> InducibleSystems(Map<ObservableConfiguration, Double> EUs, ArrayList<Systems> kprime, Systems k){
		ArrayList<Systems> l2 = new ArrayList<Systems>();
		
		double minUtilk = 0;
		for(ObservableConfiguration o : game.obs){
			if(o.configs.contains(k.f)){
				if(EUs.get(o) < minUtilk){
					minUtilk = EUs.get(o);
				}
			}
			
		}
//		System.out.println("MinutilK"+k.id+" : "+minUtilk);
		
		for(Systems k1 : kprime){
			if(k1.id == k.id)
				continue;
			
			double maxUtilk1 = -100;
			for(ObservableConfiguration o : game.obs){
				if(o.configs.contains(k1.f)){
					if(EUs.get(o) > maxUtilk1){
						maxUtilk1 = EUs.get(o);
					}
				}
				
			}

//			System.out.println("MaxUtilK"+k1.id+" : "+maxUtilk1);
			
			boolean canEqual = false;
			if(maxUtilk1 > minUtilk){
				outer : for(ObservableConfiguration o : game.obs){
					
					for(ObservableConfiguration o1 : game.obs){
						
						if(o.configs.contains(k.f) && o1.configs.contains(k1.f)){
							
							if(EUs.get(o) == EUs.get(o1)){
								canEqual = true;
								break outer;
							}
						}
					}
				}
				if(canEqual)
					l2.add(k1);
			}
			
		}
		
		
		return l2;
	}
	
	private ArrayList<Systems> InducedSystems(Map<ObservableConfiguration, Double> EUs, ArrayList<Systems> kprime, ObservableConfiguration o1){
		ArrayList<Systems> l1 = new ArrayList<Systems>();
		
		double minUtilk = EUs.get(o1);
//		for(ObservableConfiguration o : game.obs){
//			if(o.configs.contains(k.f)){
//				if(EUs.get(o) < minUtilk){
//					minUtilk = EUs.get(o);
//				}
//			}
//			
//		}
//		System.out.println("MinutilK"+k.id+" : "+minUtilk);
		
		for(Systems k1 : kprime){
		
			double maxUtilk1 = -100;
			for(ObservableConfiguration o : game.obs){
				if(o.configs.contains(k1.f)){
					if(EUs.get(o) > maxUtilk1){
						maxUtilk1 = EUs.get(o);
					}
				}
				
			}

//			System.out.println("MaxUtilK"+k1.id+" : "+maxUtilk1);
			
			if(maxUtilk1 <= minUtilk){
				l1.add(k1);
			}
		}
		
		return l1;
	}
	
	private ArrayList<Systems> InducedSystems(Map<ObservableConfiguration, Double> EUs, ArrayList<Systems> kprime, Systems k, ObservableConfiguration o1){
		ArrayList<Systems> l1 = new ArrayList<Systems>();
		
		double minUtilk = EUs.get(o1);
//		for(ObservableConfiguration o : game.obs){
//			if(o.configs.contains(k.f)){
//				if(EUs.get(o) < minUtilk){
//					minUtilk = EUs.get(o);
//				}
//			}
//			
//		}
//		System.out.println("MinutilK"+k.id+" : "+minUtilk);
		
		for(Systems k1 : kprime){
			if(k1.id == k.id)
				continue;
		
			double maxUtilk1 = -100;
			for(ObservableConfiguration o : game.obs){
				if(o.configs.contains(k1.f)){
					if(EUs.get(o) > maxUtilk1){
						maxUtilk1 = EUs.get(o);
					}
				}
				
			}

//			System.out.println("MaxUtilK"+k1.id+" : "+maxUtilk1);
			
			if(maxUtilk1 <= minUtilk){
				l1.add(k1);
			}
		}
		
		return l1;
	}
	
	private boolean checkIfInducible(Map<ObservableConfiguration, Double> EUs, ArrayList<Systems> kprime, Systems k, ObservableConfiguration o1){
		boolean check = true;
		
		double minUtilk = EUs.get(o1);
		
//		System.out.println("Utility: "+minUtilk);
		
		for(Systems k1 : kprime){
			if(k1.id == k.id)
				continue;
		
			double maxUtilk1 = -100;
			for(ObservableConfiguration o : game.obs){
				if(o.configs.contains(k1.f)){
					if(EUs.get(o) > maxUtilk1){
						maxUtilk1 = EUs.get(o);
					}
				}
				
			}

//			System.out.println("MaxUtilK"+k1.id+" : "+maxUtilk1);
			
			if(maxUtilk1 < minUtilk){
				check = false;
			}else if(maxUtilk1 == minUtilk){
				if(!o1.configs.contains(k1.f))
					check = false;
			}
		}
		
		return check;
		
	}
	
	private boolean checkIfAllInducible(Map<ObservableConfiguration, Double> EUs, ObservableConfiguration o1){
		boolean check = true;
		
		double minUtilk = EUs.get(o1);
		
//		System.out.println("Utility o"+o1.id+" : "+minUtilk);
		
		for(Systems k1 : game.machines){		
			double maxUtilk1 = -100;
			for(ObservableConfiguration o : game.obs){
				if(o.configs.contains(k1.f)){
					if(EUs.get(o) > maxUtilk1){
						maxUtilk1 = EUs.get(o);
					}
				}
				
			}

//			System.out.println("MinUtilK"+k1.id+" : "+maxUtilk1);
			
			if(maxUtilk1 < minUtilk){
				check = false;
			}else if(maxUtilk1 == minUtilk){
				if(!o1.configs.contains(k1.f))
					check = false;
			}
		}
		
		return check;
		
	}
	
	private ArrayList<Systems> InducedSystems(Map<ObservableConfiguration, Double> EUs, ArrayList<Systems> kprime, Systems k){
		ArrayList<Systems> l1 = new ArrayList<Systems>();
		
		double minUtilk = 0;
		for(ObservableConfiguration o : game.obs){
			if(o.configs.contains(k.f)){
				if(EUs.get(o) < minUtilk){
					minUtilk = EUs.get(o);
				}
			}
			
		}
//		System.out.println("MinutilK"+k.id+" : "+minUtilk);
		
		for(Systems k1 : kprime){
			if(k1.id == k.id)
				continue;
		
			double maxUtilk1 = -100;
			for(ObservableConfiguration o : game.obs){
				if(o.configs.contains(k1.f)){
					if(EUs.get(o) > maxUtilk1){
						maxUtilk1 = EUs.get(o);
					}
				}
				
			}

//			System.out.println("MaxUtilK"+k1.id+" : "+maxUtilk1);
			
			if(maxUtilk1 == minUtilk){
				l1.add(k1);
			}
		}
		
		return l1;
	}
	
	private ArrayList<Systems> AllInducibleSystems(Map<ObservableConfiguration, Double> EUs){
		ArrayList<Systems> kprime = new ArrayList<Systems>();
		
		double minmaxUtil = 0;
		for(Systems k : game.machines){
			double maxUtil = -100;
			for(ObservableConfiguration o : game.obs){
				if(o.configs.contains(k.f)){
					if(EUs.get(o) > maxUtil){
						maxUtil = EUs.get(o);
					}
				}
				
			}
//			System.out.println("K"+k.id+" Max Util: "+maxUtil);
			if(maxUtil < minmaxUtil)
				minmaxUtil = maxUtil;
		}
//		System.out.println("Max Min EU: "+minmaxUtil);
		
		for(Systems k : game.machines){
			double minUtilk = 0;
			for(ObservableConfiguration o : game.obs){
				if(o.configs.contains(k.f)){
					if(EUs.get(o) < minUtilk){
						minUtilk = EUs.get(o);
					}
				}
				
			}
			if(minUtilk <= minmaxUtil){
				kprime.add(k);
			}
		}
		
		return kprime;
	}
	
	private Map<ObservableConfiguration, Double> calculateEUUniformEstimation(){
		Map<ObservableConfiguration, Double> estimates = new HashMap<ObservableConfiguration, Double>();
		
		for(ObservableConfiguration o : game.obs){
			double util = 0;
			int size = 0;
			for(Configuration f : game.configs){
				if(o.configs.contains(f)){
					util += f.utility;
					size++;
				}
			}
			estimates.put(o, (util/(double)size));
		}
		
		
		return estimates;
	}
	
	public double getDefenderUtility(){
		return defenderUtility;
	}
	
	public Map<Systems, Map<ObservableConfiguration, Integer>> getStrategy(){
		return sigma;
	}

	public double calculateExpectedUtility(DeceptionGame game, Map<Systems, Map<ObservableConfiguration, Integer>> strategy){
		Map<ObservableConfiguration, Double> expectedUtilities = calculateEUUniformEstimation();
		ArrayList<ObservableEU> obsEU = new ArrayList<ObservableEU>();
		
		for(ObservableConfiguration o : expectedUtilities.keySet()){
			ObservableEU oeu = new ObservableEU(o, expectedUtilities.get(o));
			obsEU.add(oeu);
		}
		
		Collections.sort(obsEU, ObservableEU.utilityAscending);
		
		System.out.println(obsEU.toString());
		for(ObservableEU oeu : obsEU){
			//if strategy has at least one machine assigned to observable
			boolean covered = false;
			for(Systems k : game.machines){
				if(strategy.get(k).get(oeu.o) > 0){
					covered=true;
					break;
				}
			}
			System.out.println("Covered: "+covered+" O"+oeu.o.id);
			
			if(!covered)
				continue;
			
			double expectedU = 0;
			double total = 0;
			for(Systems k : strategy.keySet()){
				expectedU += strategy.get(k).get(oeu.o)*k.f.utility;
				total += strategy.get(k).get(oeu.o);
			}
	//		System.out.println("EU(o"+o.id+"): "+(expectedU/total));
			expectedU = expectedU/total;
			return expectedU;
		}
		return 1000;
	}
	
}
