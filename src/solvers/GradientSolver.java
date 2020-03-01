package solvers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import Models.SEModel;
import Models.Website;
import ilog.concert.IloException;
import utilities.DeceptionGameHelper;

public class GradientSolver {
	
	SEModel model;

	
	public static void gradientSolver(SEModel model) throws IOException, IloException{
		//Gradient solver : f(x) = \sum_W t_w (1-x_w) e_w / t^all_w
		// 1. start at x_0 = 0 vector
		// 2. solve adversary MILP(x)
		// 3. compute gradient of E(x,e,y)
		// 4. Update x^{t} := x^{t-1} - alpha grad f(x^{t-1}) : grad f(x^{t-1}) is equal to t_w e_w / t^all_w
		// 5. Needs to compute a projection from the current point x^{t} to the closest point staisfying the constraints
		
		// Need to load cplex libraries
		String cplexInputFile = "CplexConfig";

		// g.setRandomBudget();

		DeceptionGameHelper.loadLibrariesCplex(cplexInputFile);

		// Should have randomly generated model paramters with 3 websites and
		// with defender budget, adversary effort and budget fixed
//		SEModel model = new SEModel(101);
//		SEModel model = example1();

		model.printModel();
		
		// Initial Strategy
		Map<Website, Double> strategy = new HashMap<Website, Double>();
		for (Website w : model.getWebsites()) {
			strategy.put(w, 0.00);
		}

		double compromised = 10000;
		double improvement = 100;
		double epsilon = .0001;
		
		double alpha = .001;
		
		printObjectiveCoefficients(model);

		printMaxCompromise(model,strategy);
		
		Map<Website, Integer> attackerStrategy = new HashMap<Website, Integer>();
		Map<Website, Double> attackerEffort = new HashMap<Website, Double>();
		
		while(improvement > epsilon){
//			System.out.println();
//			System.out.println("Running MILP");
//			System.out.println();
	
			AttackerMILP milp = new AttackerMILP(model, strategy);
	
			milp.solve();
	
			attackerStrategy = milp.getAttackerStrategy();
			attackerEffort = milp.getAttackerEffort();
	
			double compromisedMILP = milp.NumberCompromised();
			
//			System.out.println("Compromised: " + milp.NumberCompromised());
	
//			System.out.println();
//			System.out.println("Defender Strategy");
//			System.out.println();
//			System.out.print("[");
//			for (Website w : model.getWebsites()) {
//				System.out.print(strategy.get(w) + ",");
//			}
//			System.out.print("]");
	
//			System.out.println();
//			System.out.println("Attacker Strategy");
//			System.out.print("[");
//			for (Website w : model.getWebsites()) {
//				System.out.print(attackerStrategy.get(w) + ",");
//			}
//			System.out.print("]");
	
//			System.out.println();
//			System.out.println("Attacker Effort");
//			System.out.print("[");
//			for (Website w : model.getWebsites()) {
//				System.out.print(attackerEffort.get(w) + ",");
//			}
//			System.out.print("]");
			
			improvement = compromised - milp.NumberCompromised();
			compromised = milp.NumberCompromised();
			
			milp.cleanUp();
			
			if(!checkBudget(model, strategy)){
				System.out.println("Over Budget");
				break;
			}
			
			//Compute gradient of f(x) and move in that direction by step size
			//ensure that moving in that direction does not violate budget constraint
			updateDefenderStrategy(model, strategy, attackerEffort, alpha);

//			System.out.println(strategy);
				
		}
		
		System.out.println("Compromised: " + compromised);
		
		
		System.out.println();
		System.out.println("Defender Strategy");
		System.out.println();
		System.out.print("[");
		for (Website w : model.getWebsites()) {
			System.out.print(strategy.get(w) + ",");
		}
		System.out.print("]");
		System.out.println();
		
		System.out.println();
		System.out.println("Attacker Strategy");
		System.out.print("[");
		for (Website w : model.getWebsites()) {
			System.out.print(attackerStrategy.get(w) + ",");
		}
		System.out.print("]");

		System.out.println();
		System.out.println("Attacker Effort");
		System.out.print("[");
		for (Website w : model.getWebsites()) {
			System.out.print(attackerEffort.get(w) + ",");
		}
		System.out.print("]");
		System.out.println();
		
		//Print out coefficients of objective function
		printObjectiveCoefficients(model, strategy);
		
		printMaxCompromise(model,strategy);
		
	}
	

	
	private static void printMaxCompromise(SEModel model, Map<Website, Double> strategy){
		for(Website w : model.getWebsites()){
			double maxCompromise = (double)w.orgtraffic;
			double gradient = (double)w.orgtraffic*strategy.get(w);
			System.out.println(w.name+"  "+(maxCompromise - gradient));
//			System.out.println((gradient));
		}	
	}
	
	private static void printObjectiveCoefficients(SEModel model) {
		for(Website w : model.getWebsites()){
			double gradient = ((((double)w.orgtraffic)/((double)w.alltraffic)));
//			System.out.println(w.name+"  "+(gradient));
			System.out.println((gradient));
		}		
		
	}
	
	private static void printObjectiveCoefficients(SEModel model, Map<Website, Double> strategy) {
		for(Website w : model.getWebsites()){
			double gradient = ((((double)w.orgtraffic)/((double)w.alltraffic)))-((((double)w.orgtraffic/(double)w.alltraffic))*strategy.get(w));
//			System.out.println(w.name+"  "+(gradient));
			System.out.println((gradient));
		}		
		
	}
	
	public static void updateDefenderStrategy(SEModel model, Map<Website, Double> strategy, Map<Website, Double> attackerEffort, double alpha){
		boolean update = false;
		//Update x according to gradient
		Map<Website, Double> tempStrategy = new HashMap<Website, Double>();
		while(!update){
			if(alpha < .0001) //Keep decreasing step size because no budget, need to break
				break;
			
			for(Website w : model.getWebsites()){ //This computes gradient and updates defender's strategy
				double gradient = (-1.0*(((double)w.orgtraffic*attackerEffort.get(w))/((double)w.alltraffic)))/(double)w.orgtraffic;
//				System.out.println(w.name+"  "+gradient);
				tempStrategy.put(w, strategy.get(w)-alpha*gradient);
			}
			if(checkBudget(model, tempStrategy))
				update = true;
			else{
				tempStrategy = new HashMap<Website, Double>();
				alpha = alpha/2; //If budget not satisfied, then we updated too much, reset alpha and try again
			}
		}
		
		if(alpha > .0001){
			for(Website w : model.getWebsites())
				strategy.put(w, tempStrategy.get(w));
		}
		
	}
	
	public static boolean checkBudget(SEModel model, Map<Website, Double> strategy){
		double budgetUsed = 0;
//		System.out.println(strategy.toString());
		for(Website w : model.getWebsites()){
//			System.out.println(w.toString());
			budgetUsed += strategy.get(w)*w.costToAlter*w.orgtraffic;
		}
		if(budgetUsed <= model.getDefenderBudget())
			return true;
		else return false;
	}
}
