package models;

import java.util.ArrayList;

public class Subset {
	
	public ArrayList<Systems> set;
	public static int ID =1;
	public int id;
	public double subsetU;
	
	public Subset(ArrayList<Systems> set){
		this.set = set;
		this.id = ID;
		ID++;
		setSubsetUtility();
	}
	
	private void setSubsetUtility(){
		subsetU = 0;
		for(Systems k : set){
			subsetU += k.f.utility;
		}
	}
	
	public String toString(){
		return "s"+id+" "+set.toString();
	}

}
