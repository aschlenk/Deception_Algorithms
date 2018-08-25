package solvers;

import java.util.ArrayList;

import models.Configuration;
import models.ObservableConfiguration;
import models.State;
import models.Strategy;
import models.Systems;

public class generateStates {
	
	ArrayList<Configuration> configs;
	ArrayList<ObservableConfiguration> obs;
	ArrayList<Systems> systems;
	ArrayList<Integer> numConfigs;
	ArrayList<Integer> numObs;
	Strategy strat;
	ArrayList<State> possibleStates;
	
	public generateStates(ArrayList<Configuration> configs,	ArrayList<ObservableConfiguration> obs, ArrayList<Systems> systems, 
			ArrayList<Integer> numConfigs, ArrayList<Integer> numObs, Strategy strat){
		this.configs = configs;
		this.obs = obs;
		this.systems = systems;
		this.numConfigs = numConfigs;
		this.numObs = numObs;
		this.strat = strat;
		possibleStates = new ArrayList<State>();
	}
	
	public void generate(){
		State s = new State();
		for(Systems k : systems){
			for(Configuration f : strat.strat.get(k).configs){
				s.addSystemConfig(k, f);
			}
		}
		
	}
	

}
