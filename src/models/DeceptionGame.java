package models;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import java.util.Map;

public class DeceptionGame {

	public ArrayList<Configuration> configs;
	public ArrayList<ObservableConfiguration> obs;
	public ArrayList<Systems> machines;
	public ArrayList<Integer> numConfigs;
	public ArrayList<Integer> numObs;
	public Map<Configuration, Map<ObservableConfiguration, Integer>> costFunction;
	public int Budget;

	public DeceptionGame() {
		configs = new ArrayList<Configuration>();
		obs = new ArrayList<ObservableConfiguration>();
		machines = new ArrayList<Systems>();
		costFunction = new HashMap<Configuration, Map<ObservableConfiguration, Integer>>();
	}

	public DeceptionGame(ArrayList<Configuration> configs, ArrayList<ObservableConfiguration> obs,
			ArrayList<Systems> machines) {
		this.configs = configs;
		this.obs = obs;
		this.machines = machines;
	}

	public void generateGame(int numConfigs, int numObs, int numSystems) {
		long seed = System.currentTimeMillis();
		Random r = new Random(seed);

		for (int i = 0; i < numConfigs; i++) {
			Configuration f1 = new Configuration(-1 * r.nextInt(11));
			configs.add(f1);
		}

		for (int i = 0; i < numObs; i++) {
			ObservableConfiguration o1 = new ObservableConfiguration();
			obs.add(o1);

			// add configurations that observable can mask
			for (int j = 0; j < configs.size(); j++) {
				if (r.nextInt(2) == 1) {
					o1.addConfiguration(configs.get(j));
				}
			}
		}
		
		//calculate backward mapping for observables that a configuration can be masked with
		for(ObservableConfiguration o : obs){
			for(Configuration f : o.configs){
				f.addObservable(o);
			}
		}

		// Create a random number for configuration and assign it to system k
		for (int i = 0; i < numSystems; i++) {
			int configNum = r.nextInt(numConfigs);
			Systems k1 = new Systems(configs.get(configNum));
			machines.add(k1);
		}

	}

	public void generateGame(int numConfigs, int numObs, int numSystems, long seed) {
		Random r = new Random(seed);

		for (int i = 0; i < numConfigs; i++) {
			Configuration f1 = new Configuration(-1 * r.nextInt(10)-1);
			configs.add(f1);
		}

		for (int i = 0; i < numObs; i++) {
			ObservableConfiguration f1 = new ObservableConfiguration();
			obs.add(f1);

			// add configurations that observable can mask
			for (int j = 0; j < configs.size(); j++) {
				if (r.nextInt(2) == 1) {
					f1.addConfiguration(configs.get(j));
				}
			}
		}
		
		//check to see if all of the configurations can be covered and cover them if not
		correctObservables(r);

		// Create a random number and assign it
		for (int i = 0; i < numSystems; i++) {
			int configNum = r.nextInt(numConfigs);
			Systems k1 = new Systems(configs.get(configNum));
			machines.add(k1);
		}

		
		costFunction = new HashMap<Configuration, Map<ObservableConfiguration, Integer>>();
		//Randomly generate cost matrix
		for(Configuration f : configs){
			costFunction.put(f, new HashMap<ObservableConfiguration, Integer>());
			for(ObservableConfiguration o : obs){
				if(o.configs.contains(f)){
					int cost = r.nextInt(100)+1; //Cost randomly generated between 1 to 100
					costFunction.get(f).put(o, cost);
				}else{
					costFunction.get(f).put(o, 10000);
				}
			}
		}
		
		//Budget randonmly generated between |K|(1/16 to 1/8)*100
		//calculate min budget needed
		int totalMinCost = 0;
		for(Systems k : machines){
			int minCost = 10000;
			for(ObservableConfiguration o : obs){
				if(o.configs.contains(k.f)){
					if(costFunction.get(k.f).get(o) < minCost)
						minCost = costFunction.get(k.f).get(o);
				}
			}
			totalMinCost += minCost;
		}
		
		//calculate min budget needed
		int totalMaxCost = 0;
		for (Systems k : machines) {
			int maxCost = 0;
			for (ObservableConfiguration o : obs) {
				if (o.configs.contains(k.f)) {
					if (costFunction.get(k.f).get(o) > maxCost)
						maxCost = costFunction.get(k.f).get(o);
				}
			}
			totalMaxCost += maxCost;
		}
//		System.out.println("Min Cost: "+totalMinCost);
//		System.out.println("Max Cost: "+totalMaxCost);
		int size = (int)(numSystems/4);
		int value;
		if(totalMaxCost-totalMinCost == 0)
			value = 0;
		else
			value = r.nextInt(totalMaxCost-totalMinCost);
		Budget = totalMinCost+value;
		
		
	}
	
	private void correctObservables(Random r){
		for(Configuration f : configs){
			int sum = 0;
			for(ObservableConfiguration o : obs){
				if(o.configs.contains(f)){
					sum++;
				}
			}
			
			if(sum == 0){
				int oNum = r.nextInt(obs.size());//new Random().nextInt(obs.size());
				obs.get(oNum).addConfiguration(f);
//				System.out.println("Config "+f.id+" not covered. Assigned obs "+obs.get(oNum).id);
			}
			sum = 0;
		}
	}
	
	public void printGame(){
		for(Systems k : machines){
			System.out.println(k.toString());
		}
		
		for(ObservableConfiguration o : obs){
			System.out.println(o.toString());
		}
		
		for(Configuration f : configs){
			System.out.println(f.toString());
		}
		
//		System.out.println(costFunction);
		for(Configuration f : configs){

//			System.out.println(costFunction.get(f));
			for(ObservableConfiguration o : obs){
//				System.out.println(f.id+ " "+o.id+" "+costFunction.get(f).get(o));
				if(costFunction.get(f).get(o) != 10000)
					System.out.println(f.id+" , "+o.id+" : "+costFunction.get(f).get(o));
			}
		}
		
	}

	public void readInGame(String dir, int numConfigs, int numObs, int numSystems, int gamenum, int experimentnum) throws FileNotFoundException{
		//Need to read in main file to set configurations, observables and systems
		Scanner s = new Scanner(new File("input/experiment"+experimentnum+"/GameFile_"+numConfigs+"_"+numObs+"_"+numSystems+"_"+gamenum+"_"+experimentnum+".txt"));
		
		//read in configurations file
		String configFile = s.nextLine();
		readInConfigurations(dir, configFile, gamenum, experimentnum);
			
		//System.out.println(configs);
		
		//read in observables
		String observableFile = s.nextLine();
		readInObservables(dir, observableFile, gamenum, experimentnum);
		
//		System.out.println(obs);
		
		//read in systems
		String systemfile = s.nextLine();
		readInSystems(dir, systemfile, gamenum, experimentnum);
		
//		System.out.println(machines);
		
		//Costs
		String costsfile = s.nextLine();
		readInCosts(dir, systemfile, gamenum, experimentnum);
		
		s.close();
		
	}

	public void readInGame(String dir, int numConfigs, int numObs, int numSystems, int gamenum, int experimentnum, boolean readCosts) throws FileNotFoundException{
		//Need to read in main file to set configurations, observables and systems
		Scanner s = new Scanner(new File("input/experiment"+experimentnum+"/GameFile_"+numConfigs+"_"+numObs+"_"+numSystems+"_"+gamenum+"_"+experimentnum+".txt"));
		
		//read in configurations file
		String configFile = s.nextLine();
		readInConfigurations(dir, configFile, gamenum, experimentnum);
			
		//System.out.println(configs);
		
		//read in observables
		String observableFile = s.nextLine();
		readInObservables(dir, observableFile, gamenum, experimentnum);
		
//		System.out.println(obs);
		
		//read in systems
		String systemfile = s.nextLine();
		readInSystems(dir, systemfile, gamenum, experimentnum);
		
//		System.out.println(machines);
		
		//Costs
		String costfile = s.nextLine();
//		System.out.println(costfile);
		readInCosts(dir, costfile, gamenum, experimentnum);
		
		s.close();
		
	}
	
	private void readInCosts(String dir, String costFile, int gamenum, int experimentnum) throws FileNotFoundException{
		int index = costFile.lastIndexOf(' ');
		costFile = dir+costFile.substring(index+1);
//		System.out.println(costFile);
		
		Scanner s1 = new Scanner(new File(costFile));
		s1.useDelimiter(",");
		s1.nextLine(); //skip over header
		while(s1.hasNext()){
			String first = s1.next();
			if(first.equalsIgnoreCase("Budget")){
				Budget = Integer.parseInt(s1.next());
				s1.nextLine();
				continue;
			}
			
			int fid = Integer.parseInt(first);
			Configuration f = null;
			for(Configuration f1 : configs){
				if(f1.id == fid){
					f = f1;
					break;
				}
			}
			int oid = Integer.parseInt(s1.next());
			ObservableConfiguration o = null;
			for(ObservableConfiguration o1 : obs){
				if(o1.id == oid){
					o = o1;
					break;
				}
			}
			int cost = Integer.parseInt(s1.next());
			s1.nextLine();
			
			if(costFunction.get(f) == null){
				costFunction.put(f, new HashMap<ObservableConfiguration, Integer>());
				costFunction.get(f).put(o, cost);
			}else
				costFunction.get(f).put(o, cost);
		}
		
		s1.close();
		
		
	}
	
	private void readInSystems(String dir, String systemFile, int gamenum, int experimentnum) throws FileNotFoundException{
		int index = systemFile.lastIndexOf(' ');
		systemFile = dir+systemFile.substring(index+1);
//		System.out.println(systemFile);
		
		Scanner s1 = new Scanner(new File(systemFile));
		s1.useDelimiter(",");
		s1.nextLine(); //skip over header
		while(s1.hasNext()){
			int sysid = Integer.parseInt(s1.next());
			int conf = Integer.parseInt(s1.next());
			s1.nextLine();
			
			Systems k = new Systems(sysid, findConfiguration(conf));//configs.get(conf-1));
			machines.add(k);
		}
		
		s1.close();
		
		
	}
	
	private void readInObservables(String dir, String observableFile, int gamenum, int experimentnum) throws FileNotFoundException{
		int index = observableFile.lastIndexOf(' ');
		observableFile = dir+observableFile.substring(index+1);
//		System.out.println(observableFile);
		
		Scanner s1 = new Scanner(new File(observableFile));
		s1.useDelimiter(",");
		s1.nextLine(); //skip over header
		while(s1.hasNext()){
			int obsid = Integer.parseInt(s1.next());
			String maskable = s1.next();
			s1.nextLine();
			
			ObservableConfiguration o = new ObservableConfiguration(obsid);
			//Need to parse maskable
			Scanner s2 = new Scanner(maskable);
			s2.useDelimiter("-");
			while(s2.hasNext()){
				int f = s2.nextInt();
				o.addConfiguration(findConfiguration(f));
				//o.addConfiguration(configs.get(f-1));
			}
			obs.add(o);
			//configs.add(f);
		}
		
		s1.close();
	}
	
	private Configuration findConfiguration(int id){
		for(Configuration f : configs){
			if(f.id == id)
				return f;
		}
		return null; //not found
	}
	
	private void readInConfigurations(String dir, String configFile, int gamenum, int experimentnum) throws FileNotFoundException{
		int index = configFile.lastIndexOf(' ');
		configFile = dir+configFile.substring(index+1);
//		System.out.println(configFile);
		
		Scanner s1 = new Scanner(new File(configFile));
		s1.useDelimiter(",");
		s1.nextLine(); //skip over header
		while(s1.hasNext()){
			int configid = Integer.parseInt(s1.next());
			int util = Integer.parseInt(s1.next());
			s1.nextLine();
			Configuration f = new Configuration(configid, util); //config id starts at 1 as well!
			configs.add(f);
		}
		
		s1.close();
	}
	
	public void exportGame(int gamenum, int experimentnum) throws IOException{
		//Need to create file for configurations
		exportConfigurations(gamenum, experimentnum);
		
		//Need to create file for observables
		exportObservables(gamenum, experimentnum);
		
		//Need to create file for systems!
		exportSystems(gamenum, experimentnum);
		
		//Need to create file for cost function
		exportCosts(gamenum, experimentnum);
		
		//Create main file that stores the names to lookup other files
		createMainFile(gamenum, experimentnum);
		
	}
	
	private void createMainFile(int gamenum, int experimentnum) throws IOException{
		String filename = "input/experiment"+experimentnum+"/GameFile_"+configs.size()+"_"+obs.size()+"_"+machines.size()+"_"+gamenum+"_"+experimentnum+".txt";
		PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
		
		w.println("Configurations = input/experiment"+experimentnum+"/configurations/Configurations_"+configs.size()+"_"+obs.size()+"_"+machines.size()+"_"+gamenum+"_"+experimentnum+".csv");
		w.println("Observables = input/experiment"+experimentnum+"/observables/Observables_"+configs.size()+"_"+obs.size()+"_"+machines.size()+"_"+gamenum+"_"+experimentnum+".csv");
		w.println("Systems = input/experiment"+experimentnum+"/systems/Systems_"+configs.size()+"_"+obs.size()+"_"+machines.size()+"_"+gamenum+"_"+experimentnum+".csv");
		w.println("Costs = input/experiment"+experimentnum+"/costs/Costs_"+configs.size()+"_"+obs.size()+"_"+machines.size()+"_"+gamenum+"_"+experimentnum+".csv");
		
		w.close();
		
	}
	
	private void exportSystems(int gamenum, int experimentnum) throws IOException{
		String filename = "input/experiment"+experimentnum+"/systems/Systems_"+configs.size()+"_"+obs.size()+"_"+machines.size()+"_"+gamenum+"_"+experimentnum+".csv";
		PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
		
		//Header line
		w.println("System Num, Configuration");
		
		//Might print out of order, maybe want to change this so id = 1 first, id = 2 second...
		for(Systems k : machines)
			w.println(k.id+","+k.f.id+",");
		
		w.close();
		
	}
	
	private void exportCosts(int gamenum, int experimentnum) throws IOException{
		String filename = "input/experiment"+experimentnum+"/costs/Costs_"+configs.size()+"_"+obs.size()+"_"+machines.size()+"_"+gamenum+"_"+experimentnum+".csv";
		PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
		
		//Header line
		w.println("Configuration, ObservableConfiguration, Cost");
		
		//Might print out of order, maybe want to change this so id = 1 first, id = 2 second...
		for(Configuration f : configs){
			for(ObservableConfiguration o : obs){
				w.println(f.id+","+o.id+","+costFunction.get(f).get(o)+",");
			}
		}
		
		w.println("Budget,"+Budget+",");
		
		w.close();
		
	}
	
	private void exportObservables(int gamenum, int experimentnum) throws IOException{
		String filename = "input/experiment"+experimentnum+"/observables/Observables_"+configs.size()+"_"+obs.size()+"_"+machines.size()+"_"+gamenum+"_"+experimentnum+".csv";
		PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
		
		//Header line
		w.println("Observable Num, Coverable Configurations");
		
		//Might print out of order, maybe want to change this so id = 1 first, id = 2 second...
		for(ObservableConfiguration o : obs){
			w.print(o.id+",");
			for(Configuration f : o.configs){
				w.print(f.id+"-");
			}
			w.println(",");
		}
		
		w.close();
		
	}
	
	private void exportConfigurations(int gamenum, int experimentnum) throws IOException{
		String filename = "input/experiment"+experimentnum+"/configurations/Configurations_"+configs.size()+"_"+obs.size()+"_"+machines.size()+"_"+gamenum+"_"+experimentnum+".csv";
		PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
		
		//Header line
		w.println("Config Num, Utility");
		
		//Might print out of order, maybe want to change this so id = 1 first, id = 2 second...
		for(Configuration f : configs)
			w.println(f.id+","+f.utility+",");
		
		w.close();
		
	}
	
	public void setRandomBudget(){
		//Budget randonmly generated between |K|(1/16 to 1/8)*100
		// calculate min budget needed
		int totalMinCost = 0;
		for (Systems k : machines) {
			int minCost = 10000;
			for (ObservableConfiguration o : obs) {
				if (o.configs.contains(k.f)) {
					if (costFunction.get(k.f).get(o) < minCost)
						minCost = costFunction.get(k.f).get(o);
				}
			}
			totalMinCost += minCost;
		}

		// calculate min budget needed
		int totalMaxCost = 0;
		for (Systems k : machines) {
			int maxCost = 0;
			for (ObservableConfiguration o : obs) {
				if (o.configs.contains(k.f)) {
					if (costFunction.get(k.f).get(o) > maxCost)
						maxCost = costFunction.get(k.f).get(o);
				}
			}
			totalMaxCost += maxCost;
		}
//		System.out.println("Min Cost: " + totalMinCost);
//		System.out.println("Min Cost: " + totalMaxCost);
		int value = (new Random()).nextInt(totalMaxCost - totalMinCost);
		Budget = totalMinCost + value;
	}

}
