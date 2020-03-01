package solvers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Models.SEModel;
import Models.Website;
import Models.WebsiteScore;
import ilog.concert.IloException;

public class AttackerGreedy {
	
	private boolean initial;
	private SEModel model;
	private Map<Website, Double> defenderStrategy;
	private Map<Website, Integer> attackerStrategy;
	private Map<Website, Integer> attackerEffort;
	private ArrayList<Map<Website, Integer>> attackerEfforts;
	private ArrayList<Website> websites;
	private Integer Ba;
	private Integer Be;
	private Integer greedyChoice;
	private Double value;
	

	
	public AttackerGreedy(SEModel model, Map<Website, Double> defenderStrategy, boolean initial, int greedyChoice){
		this.model = model;
		this.defenderStrategy = defenderStrategy;
		this.initial = initial;
		this.greedyChoice = greedyChoice;
	}
	

	public void solve() {
		ArrayList<Website> websites = model.getWebsites();
		Ba = model.getAttackBudget();
		Be = model.getAttackEffort();
		List<WebsiteScore> ratios = new ArrayList<WebsiteScore>();
		Double score;
		attackerStrategy = new HashMap<Website, Integer>();
		attackerEffort = new HashMap<Website, Integer>();
		for (Website w: websites) {
			switch(greedyChoice) {
			case 0:
				score = (w.orgtraffic * (1 - defenderStrategy.get(w)) / w.alltraffic)/(w.costToAttack);
				break;
			case 1:
				score = (w.orgtraffic * (1 - defenderStrategy.get(w)) / w.alltraffic)/(w.costToAttack/Ba+1/Be);
				break;
			case 2:
				score = (w.orgtraffic * (1 - defenderStrategy.get(w)) / w.alltraffic)/(Math.pow(w.costToAttack/Ba, 2)+1/Be);
				break;
			case 3:
				score = (w.orgtraffic * (1 - defenderStrategy.get(w)) / w.alltraffic)/(w.costToAttack/Ba+Math.pow(1/Be, 2));
				break;
			case 4:
				score = (w.orgtraffic * (1 - defenderStrategy.get(w)) / w.alltraffic)/(Math.pow(w.costToAttack/Ba, 2)+Math.pow(1/Be, 2));
				break;
			case 5:
				score = (w.orgtraffic * (1 - defenderStrategy.get(w)) / w.alltraffic);
				break;
			default:
				score = (w.orgtraffic * (1 - defenderStrategy.get(w)) / w.alltraffic);
			}
//			score = (w.orgtraffic * (1 - defenderStrategy.get(w)) / w.alltraffic)/(w.costToAttack/Ba+1/Be);
//			score = (w.orgtraffic * (1 - defenderStrategy.get(w)) / w.alltraffic)/(w.costToAttack);
			WebsiteScore ratio = new WebsiteScore(w, score);
			ratios.add(ratio);
			attackerStrategy.put(w, 0);
			attackerEffort.put(w, 0);
		}
		
		Collections.sort(ratios, new Comparator<WebsiteScore>() {
		    @Override
		    public int compare(WebsiteScore w1, WebsiteScore w2) {
		        return w1.score.compareTo(w2.score);
		    }
		});

		int i = websites.size() - 1;
		value = 0.0;

		while (Ba > 0 && Be > 0 && i >= 0) {
			Website w;
			w = ratios.get(i).website;
			if(Ba >= w.costToAttack) {
				attackerStrategy.put(w, 1);
				if (Be >= w.alltraffic) {
					attackerEffort.put(w, w.alltraffic);
					Be = Be - w.alltraffic;
					value = value + w.orgtraffic * (1 - defenderStrategy.get(w));
				}
				else {
					attackerEffort.put(w, Be);
					value = value + w.orgtraffic * (1 - defenderStrategy.get(w)) * Be / w.alltraffic;
					Be = 0;
				}
				Ba = Ba - w.costToAttack;
			}
			i--;
		}
		
		if(initial) {
//			System.out.println("finished best response, i = "+i);
		}
		
		attackerEfforts = new ArrayList<>();
		attackerEfforts.add(attackerEffort);
		
		// warm-up for cut generation: generating lots of effort vectors in the initial iteration of cut generation
//		int startindex = i;
		while(initial && i >= 0) {
//			System.out.println("begin outer while loop, i = "+i);
//			startindex = i;
//			value = 0.0;
			Ba = model.getAttackBudget();
			Be = model.getAttackEffort();
			attackerStrategy = new HashMap<Website, Integer>();
			attackerEffort = new HashMap<Website, Integer>();
			
			while (Ba > 0 && Be > 0 && i >= 0) {
				Website w;
				w = ratios.get(i).website;
				if(Ba >= w.costToAttack) {
					attackerStrategy.put(w, 1);
					if (Be >= w.alltraffic) {
						attackerEffort.put(w, w.alltraffic);
						Be = Be - w.alltraffic;
//						value = value + w.orgtraffic * (1 - defenderStrategy.get(w));
					}
					else {
						attackerEffort.put(w, Be);
//						value = value + w.orgtraffic * (1 - defenderStrategy.get(w)) * Be / w.alltraffic;
						Be = 0;
					}
					Ba = Ba - w.costToAttack;
				}
				i--;
			}
			attackerEfforts.add(attackerEffort);
		}
		
		// bad heuristic, no longer used
//		if(initial) {
//			int skip1, skip2;
//			for (skip1 = 0; skip1 <= Math.min(10, websites.size()-1); skip1++){
//				for(skip2 = skip1; skip2 <= Math.min(10, websites.size()-1); skip2++) {
//					i = websites.size() - 1;
//					value = 0.0;
//					Ba = model.getAttackBudget();
//					Be = model.getAttackEffort();
//					attackerStrategy = new HashMap<Website, Integer>();
//					attackerEffort = new HashMap<Website, Integer>();
//					for (Website w: websites) {
//						attackerStrategy.put(w, 0);
//						attackerEffort.put(w, 0);
//					}
//					while (Ba > 0 && Be > 0 && i >= 0) {
//						Website w = ratios.get(i).website;
//						if(Ba >= w.costToAttack && i != skip1 && i != skip2) {
//							attackerStrategy.put(w, 1);
//							if (Be >= w.alltraffic) {
//								attackerEffort.put(w, w.alltraffic);
//								Be = Be - w.alltraffic;
//								value = value + w.orgtraffic * (1 - defenderStrategy.get(w));
//							}
//							else {
//								attackerEffort.put(w, Be);
//								value = value + w.orgtraffic * (1 - defenderStrategy.get(w)) * Be / w.alltraffic;
//								Be = 0;
//							}
//							Ba = Ba - w.costToAttack;
//						}
//						i--;
//					}
//					attackerEfforts.add(attackerEffort);
//				}
//			}	
//		}
		
		
		
	}
	
	public Map<Website, Integer> getAttackerStrategy() {
		return attackerStrategy;
	}

	public Map<Website, Integer> getAttackerEffort() {
		return attackerEffort;
	}
	
	public ArrayList<Map<Website, Integer>> getAttackerEfforts() {
		return attackerEfforts;
	}
	
	public double getValue(){
		return value;
	}


}



