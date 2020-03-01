package Models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import ilog.concert.IloException;
import solvers.removableCheck;

public class SEModel {

	public String name;
	private int DefenderBudget;
	private int AttackBudget;
	private int AttackEffort;
	private ArrayList<Website> websites;
	private int numWebsites;
	private HashMap<Website, Double> xMaxs;
	
	public static int ModelNumber = 1;
	
	public SEModel(){
		this.name = "Model "+ModelNumber;
		numWebsites = 10;
		ModelNumber++;
	}
	
	public SEModel(long seed, int numWebsites){
		this.name = "Model "+ModelNumber;
		this.numWebsites = numWebsites;
		generateRandomWebsitesOnly(seed);
		ModelNumber++;
	}
	
	public SEModel(int DefenderBudget, int AttackBudget, int AttackEffort, ArrayList<Website> websites){
		this.name = "Model "+ModelNumber;
		this.DefenderBudget = DefenderBudget;
		this.AttackBudget = AttackBudget;
		this.AttackEffort = AttackEffort;
		this.websites = websites;
		ModelNumber++;
	}
	
	private void generateRandomWebsitesOnly(long seed){
		this.name = "Model "+ModelNumber;
		
		Random r = new Random(seed);
		
		
		//For each website
		//All traffic in-between 350 and 750
		// Org Traffic in-between 50 and 100
		// cost to attack in-between 30-54
		// cost to alter in-between 1-2
		
		//Number of websites set to be 3
		websites = new ArrayList<Website>();
		for(int i=0; i<numWebsites; i++){
			Website w = new Website(seed+i);
			websites.add(w);
		}
		
//		for(int i=0; i<numWebsites; i++){
//			if(i % 4 == 0 || i % 4 == 1) {
//				Website w = new Website(seed+i, true);
//			}
//			else {
//				Website w = new Website(seed+i, false);
//			}
////			Website w = new Website(seed+i);
//			websites.add(w);
//		}
		
		//Budget in-between 100
		// When randomly generating budget, we set it in-between cost to alter
		// around at least 10% of all org traffic to about 70%
		int costChangeAll = 0;
		int totalTraffic = 0;
		int totalCostToAttack = 0;
		int maxCost = 0;
		int minAllTraffic = 100000;
		int minCostDefend = 100000;
		for(Website w : websites){
			costChangeAll += w.costToAlter*w.orgtraffic;
			totalTraffic += w.alltraffic;
			totalCostToAttack += w.costToAttack;
			
			if(w.alltraffic < minAllTraffic)
				minAllTraffic = w.alltraffic;
			if(w.costToAttack > maxCost)
				maxCost = w.costToAttack;
			if(w.costToAlter*w.orgtraffic < minCostDefend)
				minCostDefend = w.costToAlter*w.orgtraffic;
		}
		
		DefenderBudget = (int) Math.floor(r.nextInt((int)Math.floor(0.6*costChangeAll))+Math.floor(0.11*costChangeAll));
//		DefenderBudget = (int) Math.floor(r.nextInt((int)Math.floor(costChangeAll/(double)numWebsites/10)));
//		DefenderBudget = (int) Math.floor(r.nextInt((int)Math.floor(0.6*costChangeAll))+Math.floor(0.11*costChangeAll));
//		DefenderBudget = r.nextInt(minCostDefend/3);
		
		// Attacker budget in-between 20% to 80% of budget needed to attack all websites; at least has to be equal to max of all individual websites
		AttackBudget = Math.max((int) Math.floor(r.nextInt((int)Math.floor(0.7*totalCostToAttack))+Math.floor(0.10*totalCostToAttack)), maxCost);
//		AttackBudget = Math.max((int) Math.floor(r.nextInt(totalCostToAttack)), maxCost);
		// Ryan, smaller attack budget
//		AttackBudget = Math.max((int) Math.floor(r.nextInt(401)+500), maxCost);
//		AttackBudget = Math.max((int) Math.floor(r.nextInt(101)+100), maxCost);
//		AttackBudget = 5;
		
		
		// Effort in-between attacking 20% of all traffic to attacking 80% of all traffic
		AttackEffort = (int) Math.floor(r.nextInt((int)Math.floor(0.6*totalTraffic))+Math.floor(0.20*totalTraffic));
//		AttackEffort = (int) Math.floor(r.nextInt(totalTraffic));
		// Ryan, smaller attack budget
//		AttackEffort = (int) Math.floor(r.nextInt((int)Math.floor(0.001*totalTraffic)));
//		AttackEffort = 50;
//		AttackEffort = 1000000;
//		AttackEffort = r.nextInt(minAllTraffic);
		
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getDefenderBudget() {
		return DefenderBudget;
	}

	public void setDefenderBudget(int defenderBudget) {
		DefenderBudget = defenderBudget;
	}

	public int getAttackBudget() {
		return AttackBudget;
	}

	public void setAttackBudget(int attackBudget) {
		AttackBudget = attackBudget;
	}

	public int getAttackEffort() {
		return AttackEffort;
	}

	public void setAttackEffort(int attackEffort) {
		AttackEffort = attackEffort;
	}

	public ArrayList<Website> getWebsites() {
		return websites;
	}

	public void setWebsites(ArrayList<Website> websites) {
		this.websites = websites;
	}
	
	public void printModel(){
		System.out.println();
		
		System.out.println(name);
		System.out.println("Defender Budget: "+DefenderBudget);
		System.out.println("Attacker Budget: "+AttackBudget);
		System.out.println("Attacker Effort: "+AttackEffort);
		for(Website w : websites)
			System.out.println(w.toStringFull());
		
//		System.out.println("");
//		for (Website w : websites) {
//			System.out.printf("attacker expected coefficients for website %s:%.3f\n", w.name,
//					(((double) w.orgtraffic / w.alltraffic)));
//		}
		
		System.out.println();
	}
	
	// elimination for the original problem setting
//	public void cutModel() throws IloException {
//		xMaxs = new HashMap<Website, Double>();
//		for(Website w: websites) {
//			if(w.costToAlter * w.orgtraffic >= DefenderBudget) {
//				xMaxs.put(w, DefenderBudget/(double) (w.costToAlter * w.orgtraffic));
//			}
//		}
////		System.out.println("size of xMaxs"+xMaxs.size());
//		ArrayList<Website> websites_copy = new ArrayList<>();
//		for(Website w:websites) {
//			websites_copy.add(w);
//		}
//		for(Website w: websites_copy) {
////			if(!xMaxs.containsKey(w)) {
//				removableCheck check = new removableCheck(xMaxs, w, AttackEffort);
//				check.solve();
//				if(check.getFeasible()) {
//					removeWebsite(w);
////					System.out.println("website removed");
//				}
////			}
//		}
//		System.out.println("num websites removed: "+(websites_copy.size()-websites.size()));
//	}
	
	//elimination for the uniform attack cost setting
	public void cutModel() throws IloException {
		xMaxs = new HashMap<Website, Double>();
		for(Website w: websites) {
			if(w.costToAlter * w.orgtraffic >= DefenderBudget) {
				xMaxs.put(w, DefenderBudget/(double) (w.costToAlter * w.orgtraffic));
			}
		}
//		System.out.println("size of xMaxs"+xMaxs.size());
		ArrayList<Website> websites_copy = new ArrayList<>();
		for(Website w:websites) {
			websites_copy.add(w);
		}
		for(Website w: websites_copy) {
			for(Website u: xMaxs.keySet()) {
				if(w.orgtraffic/(double) w.alltraffic <= u.orgtraffic*(1-xMaxs.get(u))/u.alltraffic &&
						u.alltraffic >= w.alltraffic &&
						u.alltraffic >= AttackEffort) {
					removeWebsite(w);
					break;
//					System.out.println("website removed");
				}
			}
//			}
		}
		System.out.println("num websites removed: "+(websites_copy.size()-websites.size()));
	}
	
	public void removeWebsite(Website w) {
		websites.remove(w);
		xMaxs.remove(w);
		numWebsites--;
	}
	
	
	
}
