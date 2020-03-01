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

public class AttackerDP {
	private SEModel model;
	private Map<Website, Double> defenderStrategy;
	private Map<Website, Integer> attackerStrategy;
	private Map<Website, Double> attackerEffort;
	private ArrayList<Website> websites;
	private Integer Ba;
	private Integer Be;
	private Double value;
	

	
	public AttackerDP(SEModel model, Map<Website, Double> defenderStrategy){
		this.model = model;
		this.defenderStrategy = defenderStrategy;
	}
	

	public void solve() {
		ArrayList<Website> websites = model.getWebsites();
		Ba = model.getAttackBudget();
		Be = model.getAttackEffort();
		int numWebsite = websites.size();
		
		Double[][][] M = new Double[numWebsite+1][Ba+1][Be+1];
		for(int i = 0; i < Ba+1; i++) {
			for(int j = 0; j < Be+1; j++) {
				M[0][i][j] = 0.0;
			}
		}
		
		
		for(int i = 1; i < numWebsite+1; i++) {
			Website w = websites.get(i-1);
			Double pi = w.orgtraffic * (1 - defenderStrategy.get(w)) / w.alltraffic;
			for(int ba = 0; ba < Ba+1; ba++) {
				for(int be = 0; be < Be+1; be++) {
					Double maxValue = M[i-1][ba][be];
					for(int qbar = 0; qbar <= w.alltraffic; qbar++) {
						if(ba - w.costToAttack >= 0 && be - qbar >= 0) {
							if(M[i-1][ba-w.costToAttack][be-qbar] + pi*qbar > maxValue) {
								maxValue = M[i-1][ba-w.costToAttack][be-qbar] + pi*qbar;
//								System.out.println("maxValue = " + maxValue);
							}
						}
					}
					M[i][ba][be] = maxValue;
				}
			}
		}
		value = M[numWebsite][Ba][Be];
		
//		List<WebsiteScore> ratios = new ArrayList<WebsiteScore>();
//		Double score;
//		attackerStrategy = new HashMap<Website, Integer>();
//		attackerEffort = new HashMap<Website, Double>();
//		for (Website w: websites) {
//			score = (w.orgtraffic * (1 - defenderStrategy.get(w)) / w.alltraffic)/w.costToAttack;
//			WebsiteScore ratio = new WebsiteScore(w, score);
//			ratios.add(ratio);
//		}
//		
//		Collections.sort(ratios, new Comparator<WebsiteScore>() {
//		    @Override
//		    public int compare(WebsiteScore w1, WebsiteScore w2) {
//		        return w1.score.compareTo(w2.score);
//		    }
//		});
//		
//		Website w;
//		int i = websites.size() - 1;
//		value = 0.0;
//		while (Ba > 0 && Be > 0 && i >= 0) {
//			w = ratios.get(i).website;
//			if(Ba >= w.costToAttack) {
//				attackerStrategy.put(w, 1);
//				if (Be >= w.alltraffic) {
//					attackerEffort.put(w, (double) w.alltraffic);
//					Be = Be - w.alltraffic;
//					value = value + w.orgtraffic * (1 - defenderStrategy.get(w));
//				}
//				else {
//					attackerEffort.put(w, (double) Be);
//					value = value + w.orgtraffic * (1 - defenderStrategy.get(w)) * Be / w.alltraffic;
//					Be = 0;
//				}
//				Ba = Ba - w.costToAttack;
//			}
//			i--;
//		}
		
		
		
	}
	
	public Map<Website, Integer> getAttackerStrategy() {
		return attackerStrategy;
	}

	public Map<Website, Double> getAttackerEffort() {
		return attackerEffort;
	}
	
	public double getValue(){
		return value;
	}
}
