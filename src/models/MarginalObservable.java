package models;

public class MarginalObservable {
	
	public ObservableConfiguration o;
	public double assigned;
	
	public MarginalObservable(ObservableConfiguration o, double assigned){
		this.o = o;
		this.assigned = assigned;
	}
	
	public String toString(){
		return "O"+o.id+" : "+assigned;
	}

}
