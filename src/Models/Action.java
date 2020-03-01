package Models;

import java.util.ArrayList;
import java.util.HashMap;

public class Action {
	
	public ArrayList<Website> websites;
	
	public static int ID = 1;
	
	public int id;
	
	public Action(){
		websites = new ArrayList<Website>();
		id = ID;
		ID++;
	}
	
	public Action(ArrayList<Website> websites){
		this.websites = new ArrayList<Website>();
		for(Website w : websites)
			this.websites.add(w);
		id = ID;
		ID++;
	}
	
	public void addWebsite(Website w){
		websites.add(w);
	}

	public boolean equals(Object o2) {
		Action a1 = (Action) o2;
		if(websites.size() != a1.websites.size())
			return false;
		else{
			for(Website w : websites){
				if(!a1.websites.contains(w))
					return false;
			}
		}
		
		return true;
	}
	
	public boolean subset(Action a2){ //assumes |Websites| for a2 < |Websites| for this action
		for(Website w : a2.websites){
			if(!this.websites.contains(w))
				return false;
		}
				
		return true;
	}
	
	public String toString(){
		String s = id+": ";
		for(Website w : websites)
			s += w.name+", ";
		return s;
	}


}
