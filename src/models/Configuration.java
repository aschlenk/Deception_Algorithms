package models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Configuration {
	
	public static int ID = 1;
	public String name;
	public int utility;
	public int id;
	
	public Set<ObservableConfiguration> observables;
	public ArrayList<ObservableConfiguration> obsConfigs;
	
	public Configuration(int utility){
		name = "f"+ID;
		this.utility = utility;
		id = ID;
		ID++;
		observables = new HashSet<ObservableConfiguration>();
//		obsConfigs = new ArrayList<>();
	}
	
	public Configuration(int id, int utility){
		this.utility = utility;
		this.id = id;
		name = "f"+id;
		ID++;
	}
	
	public Configuration(String name, int utility){
		this.name = name;
		this.utility = utility;
	}
	
	public void setObservableConfigurations(ArrayList<ObservableConfiguration> obsConfigs){
		for(ObservableConfiguration f : obsConfigs){
			obsConfigs.add(f);
		}
	}
	
	public void setObservableConfigurations(Set<ObservableConfiguration> obsConfigs){
		for(ObservableConfiguration o : obsConfigs){
			observables.add(o);
		}
	}
	
	public void addObservable(ObservableConfiguration f){
		obsConfigs.add(f);
		observables.add(f);
	}
	
	public String toString(){
		return name;//+" Utility: "+utility;
	}

}
