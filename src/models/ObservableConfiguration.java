package models;

import java.util.ArrayList;

public class ObservableConfiguration {
	
	public static int ID = 1;
	public String name;
	public double utility;
	public int id;
	public ArrayList<Configuration> configs;
	
	public ObservableConfiguration(){
		name = "f~"+ID;
		id = ID;
		ID++;
		configs = new ArrayList<Configuration>();
	}
	
	public ObservableConfiguration(int id){
		name = "f~"+ID;
		this.id = id;
		ID++;
		configs = new ArrayList<Configuration>();
	}
	
	public ObservableConfiguration(double utility){
		name = "f~"+ID;
		this.utility = utility;
		id = ID;
		ID++;
	}
	
	public ObservableConfiguration(String name){
		this.name = name;
		configs = new ArrayList<Configuration>();
	}
	
	public void assignUtility(double utility){
		this.utility = utility;
	}
	
	public void addConfigurations(ArrayList<Configuration> c){
		for(Configuration f : c){
			configs.add(f);
		}
	}
	
	public void addConfiguration(Configuration f){
		configs.add(f);
	}
	
	public String toString(){
		String output = name+" Configs: ";
		for(Configuration f : configs){
			output += f.name+", ";
		}
		return output;
	}
	
}
