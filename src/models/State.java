package models;

import java.util.HashMap;
import java.util.Map;

public class State {
	
	public Map<Systems, Configuration> trueState;
	public static int ID = 1;
	public int id;

	public State(){
		this.id = ID;
		ID++;
		trueState = new HashMap<Systems, Configuration>();
	}
	
	public void addSystemConfig(Systems k, Configuration f){
		if(trueState.containsKey(k))
			System.out.println("System "+k.name+" already has "+f.name);
		else
			trueState.put(k, f);
	}
	
}
