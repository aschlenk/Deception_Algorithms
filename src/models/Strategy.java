package models;

import java.util.HashMap;
import java.util.Map;

public class Strategy {
	
	public Map<Systems, ObservableConfiguration> strat;
	
	public Strategy(){
		strat = new HashMap<Systems, ObservableConfiguration>();
	}
	
	public void setObservable(Systems k, ObservableConfiguration o){
		if(!o.configs.contains(k.f)){
			System.out.println("System "+k.name+" cannot use "+o.name);
		}else{
			if(strat.containsKey(k))
				System.out.println("System "+k.name+" already has "+o.name);
			else
				strat.put(k, o);
		}
	}

	public void printStrategy(){
		for(Systems k : strat.keySet()){
			System.out.println("System "+k.name+" : "+strat.get(k));
		}
	}
	
}
