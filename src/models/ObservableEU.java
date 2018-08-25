package models;

import java.util.Comparator;

public class ObservableEU implements Comparable{
	
	public ObservableConfiguration o;
	public double eu;
	public double cost;
	
	public ObservableEU(ObservableConfiguration o, double eu){
		this.o = o;
		this.eu = eu;
	}
	
	public ObservableEU(ObservableConfiguration o, double eu, double cost){
		this.o = o;
		this.eu = eu;
		this.cost = cost;
	}

	@Override
	public int compareTo(Object arg0) {
		ObservableEU o1 = (ObservableEU) arg0;
		if(this.eu > o1.eu){
			return -1;
		}else if(this.eu == o1.eu){
			return 0;
		}else{
			return 1;
		}
	}
	
	public static Comparator utilityAscending = new Comparator() {
		
	    @Override
	    public int compare(Object obj1, Object obj2) {
	        if (!(obj1 instanceof ObservableEU) || !(obj2 instanceof ObservableEU)){
	            throw new ClassCastException("Invalid object");
	        }
	        else {
	        	ObservableEU o1 = (ObservableEU)obj1;
	        	ObservableEU o2 = (ObservableEU)obj2;
	        	if(o1.eu-o2.eu > 0)
	        		return 1;
	        	else if(o1.eu == o2.eu)
	        		return 0;
	        	else
	        		return -1;
//	            return o1.eu-o2.eu;
	        }
	}
	};
	
	public static Comparator costAscending = new Comparator() {
		
	    @Override
	    public int compare(Object obj1, Object obj2) {
	        if (!(obj1 instanceof ObservableEU) || !(obj2 instanceof ObservableEU)){
	            throw new ClassCastException("Invalid object");
	        }
	        else {
	        	ObservableEU o1 = (ObservableEU)obj1;
	        	ObservableEU o2 = (ObservableEU)obj2;
	        	if(o1.cost-o2.cost > 0)
	        		return 1;
	        	else if(o1.cost == o2.cost)
	        		return 0;
	        	else
	        		return -1;
//	            return o1.eu-o2.eu;
	        }
	}
	};
	
	
	public String toString(){
		return "o"+o.id+" : "+eu+" : "+cost;
	}

}
