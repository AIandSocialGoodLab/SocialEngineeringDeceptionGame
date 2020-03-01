package solvers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import Models.Action;
import Models.SEModel;
import Models.Website;
import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.cplex.IloCplex;

public class OptimalSolver {
	
	private SEModel model_data;	
	private double runtime;
	private double numCompromised;
	private double presolveTime;
	private double algRuntime;
	private double defenderLPTime;
	private double attackerMilpTime;
	private double attackerGreedyTime;
//	private double defenderGetSolutionTime;
	private ArrayList<Double> defenderGetSolutionTime;
	private ArrayList<Double> attackerTimeList;
	private ArrayList<Double> defenderTimeList;
	private int keepTimeEvery;
	private int numIter;
	private int numMilpIter = 0;
	private int iterLimit;
	private int initBatch;
	private double eps = 0;
	HashMap<Website, Integer> actionAllTraffic = null;
	private double timeLimit;	
	private boolean equtil;
	HashMap<Website, Integer> actionOrgTraffic = null;
	/**
	 * Constructor for the optimization solver. 
	 * @param model - SE Model
	 */
	public OptimalSolver(SEModel model){
		model_data = model;
	}
	
	public OptimalSolver(SEModel model, boolean equtil){
		model_data = model;
		this.equtil = equtil;
	}
	
	/**
	 * 1. Generates all actions for the adversary.  
	 * 2. Solve an LP for the defender's optimal strategy ensuring each adversary action is a BR. 
	 * @throws IloException
	 */
	public void solve() throws IloException{
		double start = System.currentTimeMillis();
		
		//Pretime solve
		long prestart = System.currentTimeMillis();
		ArrayList<Action> actions = generateAllActions(model_data);
		
		removeAllSubsets(actions);
		
//		for(Action a : actions)
//			System.out.println(a.toString());
		
		//generate max effort vectors
		Map<Action, ArrayList<Map<Website, Integer>>> maxEffort = new HashMap<Action, ArrayList<Map<Website, Integer>>>();

		for (Action a : actions) {
			generateMaxEffortVectors(maxEffort, a);
		}
		
		presolveTime = (System.currentTimeMillis()-start)/1000.0;

//		for (Action a : maxEffort.keySet()) {
//			for (Map<Website, Integer> m : maxEffort.get(a)) {
//				System.out.println("Action " + a.id + " " + m.toString());
//
//			}
//		}
		
		long algstart = System.currentTimeMillis();
		
		double bestObjective = Double.MAX_VALUE;
		Map<Website, Double> bestDefenderStrategy = null;
		//added by Ryan
		Map<Website, Double> bestAttackerUtility = null;
		Double defenderLeftover = -1.0;
		Action optimal = null;
		Map<Website, Integer> oEffort = null;
		ArrayList<Action> optimalActions = new ArrayList<Action>();
		for(Action a : actions){
			for(Map<Website, Integer> map : maxEffort.get(a)){
				BestResponseLP lp = new BestResponseLP(model_data, maxEffort, actions, a, map, equtil);
				
				lp.solve();
				
				if(lp.getObjectiveValue() != null && lp.getObjectiveValue() < bestObjective - .0001){
					bestObjective = lp.getObjectiveValue();
					numCompromised = lp.getObjectiveValue();
					bestDefenderStrategy = lp.getDefenderStrategy();
					// added by Ryan
					bestAttackerUtility = lp.getAttackerUility();
					actionAllTraffic = getActionAllTraffic(a);
					actionOrgTraffic = getActionOrgTraffic(a);
					defenderLeftover = lp.getDefenderLeftover();
					optimal = a;
					oEffort = map;
					optimalActions.clear();
					optimalActions.add(a);
				}
				//ryan commented this out
//				else if(lp.getObjectiveValue() != null && lp.getObjectiveValue() < bestObjective + .0001){ //plus epsilon
//					if(!optimalActions.contains(a))
//						optimalActions.add(a);					
//				}
								
				//clean up
				lp.deleteVars();
				
				if((System.currentTimeMillis()-start)/1000.0 > 200){
					runtime = (System.currentTimeMillis()-start)/1000.0;
					numCompromised = bestObjective;
					algRuntime = (System.currentTimeMillis()-algstart)/1000.0;
					return;
				}
			}
		}
		
//		System.out.println("Best Objective: "+bestObjective);
//		System.out.println("Best defender action: "+bestDefenderStrategy);
//		//added by Ryan
//		System.out.println("Defender budget leftover: "+defenderLeftover);
//		System.out.println("Attacker utility: "+bestAttackerUtility);
//		System.out.println("Optimal Action: "+optimal);
//		System.out.println("Attacker Effort Budget: "+model_data.getAttackEffort());
//		System.out.println("Optimal Effort Vector: "+oEffort);
//		System.out.println("Action All Traffic: "+actionAllTraffic);
//		System.out.println("Action Org Traffic: "+actionOrgTraffic);
//		//commented out by Ryan
////		System.out.println(optimalActions);
////		System.out.println();
//		System.out.println("Runtime: "+((System.currentTimeMillis()-start)/1000.0));
//		System.out.println();
		actions.clear();
		
		runtime = (System.currentTimeMillis()-start)/1000.0;
		numCompromised = bestObjective;
		algRuntime = (System.currentTimeMillis()-algstart)/1000.0;
		
	}
	
	/**
	 * 1. Generate all adversary actions
	 * 2. Solves the SE Model by enumerating all max effort vectors. 
	 * 3. Solve the LP with all max effort vectors generated. 
	 * @throws IloException
	 */
	public void solveEffort() throws IloException{
		double start = System.currentTimeMillis();
		attackerMilpTime = 0;
		defenderLPTime = 0;
		
		long prestart = System.currentTimeMillis();
		
		ArrayList<Action> actions = generateAllActions(model_data);

		removeAllSubsets(actions);

//		for(Action a : actions)
//			System.out.println(a.toString());
		
		//generate max effort vectors
		Map<Action, ArrayList<Map<Website, Integer>>> maxEffort = new HashMap<Action, ArrayList<Map<Website, Integer>>>();

//		int count = 0;
		for (Action a : actions) {
			generateMaxEffortVectors(maxEffort, a);
//			System.out.println("generate done: " + count + " out of " + actions.size());
//			count++;
		}

		
		ArrayList<Map<Website, Integer>> bestEffortVectors = new ArrayList<>();
		
		for(Action a : maxEffort.keySet()){
			for(Map<Website, Integer> map : maxEffort.get(a)){
				if(!bestEffortVectors.contains(map))
					bestEffortVectors.add(map);
			}
		}
		
		presolveTime = (System.currentTimeMillis()-prestart)/1000.0;
		
		long algstart = System.currentTimeMillis();
		
		//Only need to solve a single LP with all of these best effort vectors enumerated?
		EffortLP lp = new EffortLP(model_data, bestEffortVectors);
		
		lp.solve();
		
		actions.clear();
		maxEffort.clear();
		bestEffortVectors.clear();
		
//		double bestObjective = Double.MAX_VALUE;
//		Map<Website, Double> bestDefenderStrategy = null;
//		Action optimal = null;
//		Map<Website, Integer> oEffort = null;
		
//		System.out.println("Best Objective: "+lp.getObjectiveValue());
//		System.out.println(lp.getDefenderStrategy());

		System.out.println();
		System.out.println("Runtime: "+((System.currentTimeMillis()-start)/1000.0));
		
		runtime = (System.currentTimeMillis()-start)/1000.0;
		numCompromised = lp.getObjectiveValue();
		algRuntime = (System.currentTimeMillis()-algstart)/1000.0;
		
		//clean up
		lp.deleteVars();
		actions.clear();		
		maxEffort.clear();		
		bestEffortVectors.clear();
		
	}
	
	/**
	 * 1. Solved relaxed version of the problem to generate initial effort vector for adversary and defender strategy. 
	 * 2. Solve adversary BR MILP to current defender strategy. 
	 * 3. Solve defender effort LP to BR to adversaries max effort vectors. 
	 * 4. Repeat steps 2 and 3 until no additional max effort vectors are added. Reaches convergence once there are no new BRs. 
	 * @throws IloException
	 * @throws IOException 
	 */
	public void solveEffortCutGeneration() throws IloException, IOException{
		double start = System.currentTimeMillis();
		attackerMilpTime = 0;
		defenderLPTime = 0;
		defenderGetSolutionTime = new ArrayList<>();
	    iterLimit = 100;
	    timeLimit = 600;
	    keepTimeEvery = 40;
	    initBatch = 1;
	    attackerTimeList = new ArrayList<>();
	    defenderTimeList = new ArrayList<>();
	    
		long prestart = System.currentTimeMillis();
		
		//Need to solve the relaxed problem to get initial e vector
		StackelbergLP solver = new StackelbergLP(model_data);

		solver.solve();
		
		System.out.println();
		System.out.println("Relaxed LP Num Compromised: "+solver.getNumCompromised());
		System.out.println();
		
		Map<Website, Double> defenderStrategy = solver.getDefenderStrategy();
		
		solver.deleteVars();
		
//		Map<Website, Double> defenderStrategy = new HashMap<Website, Double>();
//		for(Website w:model_data.getWebsites()) {
//			defenderStrategy.put(w, 0.0);
//		}
		
		presolveTime = (System.currentTimeMillis()-prestart)/1000.0;
		
//		Map<Website, Double> attackerStrategy = solver.getAttackerStrategy();
//		Map<Website, Double> attackerEffort = solver.getAttackerEffort();
		
		long milpstart = System.currentTimeMillis();
		ArrayList<Map<Website, Integer>> bestEffortVectors = new ArrayList<>();
		Map<Website, Integer> effortVector;
		//solver attacker BR MILP
		AttackerMILP milp = new AttackerMILP(model_data, defenderStrategy);
		double milpBest = 0.0;
		while(milp.getFeasible() && bestEffortVectors.size() < initBatch && milp.NumberCompromised() >= milpBest) {
			milp.solve();
			effortVector = new HashMap<Website, Integer>();
			if(milp.getFeasible()) {
				for(Website w : milp.getAttackerEffort().keySet()){
					effortVector.put(w, (int)Math.floor(milp.getAttackerEffort().get(w)));
				}
				if(!bestEffortVectors.contains(effortVector)){
					bestEffortVectors.add(effortVector);
				}
				if(milp.NumberCompromised() >= milpBest - eps) {
					milpBest = milp.NumberCompromised();
				}
//				else {
//					break;
//				}
//				System.out.println("milp value = " + milp.NumberCompromised());
			}
			milp.addngConstraints(milp.getAttackerEffort());
		}
//		System.out.println("finish 1st milp, added strategies: "+bestEffortVectors.size());

		
//		ArrayList<Map<Website, Integer>> bestEffortVectors = new ArrayList<>();
//		
//		Map<Website, Integer> effortVector = new HashMap<Website, Integer>();
//		for(Website w : milp.getAttackerEffort().keySet()){
//			effortVector.put(w, (int)Math.floor(milp.getAttackerEffort().get(w)));
//		}
//		
//		bestEffortVectors.add(effortVector);
//		System.out.println("milp value = " + milp.NumberCompromised());
		milp.cleanUp();	
		
		
		
		attackerMilpTime += (System.currentTimeMillis()-milpstart)/1000.0;
		
//		for (Map<Website, Integer> m : bestEffortVectors) {
//			System.out.println(m.toString());
//		}

		long defenderlpstart = System.currentTimeMillis();
		
		//Get initial solution
		EffortLP lp = new EffortLP(model_data, bestEffortVectors);
		
		lp.solve();
		
		defenderStrategy = lp.getDefenderStrategy();
		
		double bestObjective = lp.getObjectiveValue();
		double improvement = 100;
//		System.out.println("lp value = " + bestObjective);
		lp.deleteVars();
		
		defenderLPTime += (System.currentTimeMillis()-defenderlpstart)/1000.0;
		lp = new EffortLP(model_data, bestEffortVectors);
//		System.out.println("Best Objective: "+bestObjective);
//		System.out.println(lp.getDefenderStrategy());
		boolean addedStrategy = true;
		numIter = 0;
		attackerTimeList.add(attackerMilpTime);
		defenderTimeList.add(defenderLPTime);
//		while(addedStrategy){
		while(addedStrategy && attackerMilpTime + defenderLPTime < timeLimit){
			milpstart = System.currentTimeMillis();
			//1. Solve for BR MILP to current x
			//2. Generate new max effort vector and add it to EffortLP
			//3. Check if objective changes: a) if yes update x and continue, b) if no break
			//solver attacker BR MILP
			milp = new AttackerMILP(model_data, defenderStrategy);
			
			milp.solve();
			
//			System.out.println("MILP: "+milp.NumberCompromised());
			
			effortVector = new HashMap<Website, Integer>();
			for(Website w : milp.getAttackerEffort().keySet()){
				double effort = milp.getAttackerEffort().get(w);
				effortVector.put(w, (int) effort);
			}
			
//			PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter("output/vectors.csv", true)));
//			w.println(String.join(",", effortVector.entrySet().toString()));
//			w.close();
			
//			lp.addConstraint(milp.NumberCompromised());
			
			
			if(!bestEffortVectors.contains(effortVector)){
				bestEffortVectors.add(effortVector);		
				addedStrategy = true;
			}else{
				addedStrategy = false;
//				PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter("output/vectors.csv", true)));
//				w.println(String.join(",", effortVector.entrySet().toString()));
//				w.close();
			}
//			System.out.println("milp value = " + milp.NumberCompromised());
			milp.cleanUp();
			
			
			attackerMilpTime += (System.currentTimeMillis()-milpstart)/1000.0;
			
			
			defenderlpstart = System.currentTimeMillis();
			if(addedStrategy) {
				Map<Website, Integer> removed = lp.addEffortVector(effortVector);
//				if(removed != null) {
//					bestEffortVectors.remove(removed);
//				}
			}
//			System.out.println("Numiter = "+numIter+", besteffortvectorsetsize = "+bestEffortVectors.size());
//			lp = new EffortLP(model_data, bestEffortVectors);
			
			lp.solve();
			defenderGetSolutionTime.add(lp.getGetSolutionTime());
			//should always be increasing
			improvement = lp.getObjectiveValue() - bestObjective;
			
			defenderStrategy = lp.getDefenderStrategy();
//			System.out.println("lp value = " + lp.getObjectiveValue());
			if(lp.getObjectiveValue() > bestObjective)
				bestObjective = lp.getObjectiveValue();
			
//			lp.deleteVars();

			defenderLPTime += (System.currentTimeMillis()-defenderlpstart)/1000.0;
			if(numIter % keepTimeEvery == 0) {
//				attackerTimeList.add(attackerMilpTime - attackerTimeList.get(attackerTimeList.size() - 1));
//				defenderTimeList.add(defenderLPTime - defenderTimeList.get(defenderTimeList.size() - 1));
				attackerTimeList.add(attackerMilpTime);
				defenderTimeList.add(defenderLPTime);
			}
			numIter++;
			
//			System.out.println(lp.getObjectiveValue());
//			System.out.println("Best Objective: "+bestObjective);
//			System.out.println(lp.getDefenderStrategy());
//			System.out.println();
			
			
			//if(!addedStrategy)
				//break;
		}
//		double eps = 0.001;
//		int covered = 0;
//		Map<Website, Double> defenderOptStrat = lp.getDefenderStrategy();
//		for(Website w:defenderOptStrat.keySet()) {
//			if(defenderOptStrat.get(w) > eps) {
//				covered++;
//			}
//		}
//		System.out.println("Defender covers websites: " + covered);
		lp.deleteVars();
		
		System.out.println("Best Objective: "+bestObjective);
//		System.out.println(lp.getDefenderStrategy());

		System.out.println();
//		System.out.println("Runtime: "+((System.currentTimeMillis()-start)/1000.0));
		
		runtime = (System.currentTimeMillis()-start)/1000.0;
		numCompromised = bestObjective;
		
		
	}
	

	
	
	
	/**
	 * same as solveEffortCutGeneration except 
	 * using attackerGreedy heuristic to solve attacker BR problem instead of MILP
	 * resort to MILP if attackerGreedy doesn't give new BR effort vector
	 * @throws IloException
	 */
	public void solveEffortCutGenerationGreedy() throws IloException{
		double start = System.currentTimeMillis();
		attackerGreedyTime = 0;
		defenderLPTime = 0;
//		defenderGetSolutionTime = 0;
		defenderGetSolutionTime = new ArrayList<>();
	    iterLimit = 100;
	    timeLimit = 600;
	    keepTimeEvery = 40;
	    
	    attackerTimeList = new ArrayList<>();
	    defenderTimeList = new ArrayList<>();
	    
	    long defenderlpsolvestart;
		
		long prestart = System.currentTimeMillis();
		
		//Need to solve the relaxed problem to get initial e vector
		StackelbergLP solver = new StackelbergLP(model_data);

		solver.solve();
		
		Map<Website, Double> defenderStrategy = solver.getDefenderStrategy();
		
		solver.deleteVars();
		
		presolveTime = (System.currentTimeMillis()-prestart)/1000.0;	
		long greedystart = System.currentTimeMillis();
		

		//replaced by Ryan, Greedy
		AttackerGreedy greedy = new AttackerGreedy(model_data, defenderStrategy, true, 0);
		greedy.solve();
		
//		ArrayList<Map<Website, Integer>> bestEffortVectors = new ArrayList<>();
		
		ArrayList<Map<Website, Integer>> bestEffortVectors = greedy.getAttackerEfforts();
		
//		System.out.println("current effort set size = "+bestEffortVectors.size());
		
		Map<Website, Integer> effortVector = new HashMap<Website, Integer>();
//		for(Website w : milp.getAttackerEffort().keySet()){
//			effortVector.put(w, (int)Math.floor(milp.getAttackerEffort().get(w)));
//		}
//		
//		bestEffortVectors.add(effortVector);

		attackerGreedyTime += (System.currentTimeMillis()-greedystart)/1000.0;

		long defenderlpstart = System.currentTimeMillis();
		
		//Get initial solution
		EffortLP lp = new EffortLP(model_data, bestEffortVectors);
		
		lp.solve();
		
		defenderStrategy = lp.getDefenderStrategy();
		
		double bestObjective = lp.getObjectiveValue();
		double improvement = 100;
		
		lp.deleteVars();
		
		defenderLPTime += (System.currentTimeMillis()-defenderlpstart)/1000.0;
		boolean addedStrategy = true;
		numIter = 0;
		numMilpIter = 0;
		lp = new EffortLP(model_data, bestEffortVectors);
		attackerTimeList.add(attackerGreedyTime);
		defenderTimeList.add(defenderLPTime);
		while(addedStrategy && attackerGreedyTime + defenderLPTime < timeLimit){
//		while(addedStrategy && numIter < iterLimit){
			greedystart = System.currentTimeMillis();
			//1. Solve for BR MILP to current x
			//2. Generate new max effort vector and add it to EffortLP
			//3. Check if objective changes: a) if yes update x and continue, b) if no break
			//solver attacker BR MILP
			
			// Replaced by Ryan, Greedy
			greedy = new AttackerGreedy(model_data, defenderStrategy, false, 0);
			
			greedy.solve();
			
//			System.out.println("MILP: "+milp.NumberCompromised());
			
			effortVector = new HashMap<Website, Integer>();
			for(Website w : greedy.getAttackerEffort().keySet()){
				double effort = greedy.getAttackerEffort().get(w);
				effortVector.put(w, (int) effort);
			}
			

			
			if(!bestEffortVectors.contains(effortVector)){
				bestEffortVectors.add(effortVector);		
				addedStrategy = true;
//				System.out.println("didn't invoke MILP, added strat");
//				System.out.println("current effort set size = "+bestEffortVectors.size());
//				if(bestEffortVectors.size() > iterLimit) {
//					bestEffortVectors.remove(50);
//				}
			}else{
				addedStrategy = false;
				AttackerMILP milp = new AttackerMILP(model_data, defenderStrategy);
				milp.solve();
				effortVector = new HashMap<Website, Integer>();
				numMilpIter++;
				for(Website w : milp.getAttackerEffort().keySet()){
					double effort = milp.getAttackerEffort().get(w);
					effortVector.put(w, (int) effort);
				}
				if(!bestEffortVectors.contains(effortVector)){
					bestEffortVectors.add(effortVector);		
					addedStrategy = true;
//					System.out.println("invoked MILP, added strat");
//					System.out.println("current effort set size = "+bestEffortVectors.size());
					milp.cleanUp();
				}else {
					addedStrategy = false;
//					System.out.println("invoked MILP, didn't add strat");
//					System.out.println("current effort set size = "+bestEffortVectors.size());
					milp.cleanUp();
				}
			}
			
			attackerGreedyTime += (System.currentTimeMillis()-greedystart)/1000.0;
			
//			for (Map<Website, Integer> m : bestEffortVectors) {
//				System.out.println(m.toString());
//			}
			
			defenderlpstart = System.currentTimeMillis();
			if(addedStrategy) {
				lp.addEffortVector(effortVector);
			}
			
//			lp = new EffortLP(model_data, bestEffortVectors);
			lp.solve();
//			defenderGetSolutionTime += lp.getGetSolutionTime();
			defenderGetSolutionTime.add(lp.getGetSolutionTime());
			//should always be increasing
			improvement = lp.getObjectiveValue() - bestObjective;
			
			defenderStrategy = lp.getDefenderStrategy();
			
			if(lp.getObjectiveValue() > bestObjective)
				bestObjective = lp.getObjectiveValue();
			
//			lp.deleteVars();

			defenderLPTime += (System.currentTimeMillis()-defenderlpstart)/1000.0;
			if(numIter % keepTimeEvery == 0) {
				attackerTimeList.add(attackerGreedyTime - attackerTimeList.get(attackerTimeList.size() - 1));
				defenderTimeList.add(defenderLPTime - defenderTimeList.get(defenderTimeList.size() - 1));
			}
			numIter++;
//			System.out.println(lp.getObjectiveValue());
//			System.out.println("Best Objective: "+bestObjective);
//			System.out.println(lp.getDefenderStrategy());
//			System.out.println();
			
			//if(!addedStrategy)
				//break;
		}
		
		lp.deleteVars();
		
		System.out.println("Best Objective: "+bestObjective);
//		System.out.println(lp.getDefenderStrategy());


//		System.out.println("Runtime: "+((System.currentTimeMillis()-start)/1000.0));
		System.out.println();
		runtime = (System.currentTimeMillis()-start)/1000.0;
		numCompromised = bestObjective;
		
		
	}
	
	
	
	public void solveUB() throws IloException, IOException{
		double start = System.currentTimeMillis();
		attackerMilpTime = 0;
		defenderLPTime = 0;
		defenderGetSolutionTime = new ArrayList<>();
	    iterLimit = 100;
	    timeLimit = 80;
	    keepTimeEvery = 40;
	    attackerTimeList = new ArrayList<>();
	    defenderTimeList = new ArrayList<>();
	    double eps = 0.001;
	    
	    boolean feasible = true;
		double bestObjective = 1000000.0;
		numIter = 0;
		StackelbergLP solver = new StackelbergLP(model_data);
		System.out.println("===================");
		ArrayList<Map<Website, Integer>> bestEffortVectors = new ArrayList<>();
		Map<Website, Integer> effortVector;
	    while(feasible && attackerMilpTime + defenderLPTime < timeLimit) {
	    	long prestart = System.currentTimeMillis();
			solver.solve();	
			System.out.println("+++++++++++");
			System.out.println("Num Compromised LP: "+solver.getNumCompromised());
			System.out.println();
			if(!solver.feasible() || numIter > 10) {
				break;
			}
			Map<Website, Double> defenderStrategy = solver.getDefenderStrategy();
			Map<Website, Double> lpAttackerStrategy = solver.getAttackerStrategy();
			System.out.println(lpAttackerStrategy);
//			solver.deleteVars();
			
			defenderLPTime += (System.currentTimeMillis()-prestart)/1000.0;
			
//			Map<Website, Double> attackerStrategy = solver.getAttackerStrategy();
//			Map<Website, Double> attackerEffort = solver.getAttackerEffort();
			
			long milpstart = System.currentTimeMillis();
			AttackerMILP milp = new AttackerMILP(model_data, defenderStrategy);		
			milp.solve();			
			System.out.println();
			System.out.println("Num Compromised MILP: "+milp.NumberCompromised());
			System.out.println();	
			effortVector = new HashMap<Website, Integer>();
			for(Website w : milp.getAttackerEffort().keySet()){
				effortVector.put(w, (int)Math.floor(milp.getAttackerEffort().get(w)));
			}		
//			bestEffortVectors.add(effortVector);
//			if(!bestEffortVectors.contains(effortVector)){
//				bestEffortVectors.add(effortVector);		
//				feasible = true;
//			}else{
//				feasible = false;
//			}
			
			Map<Website, Integer> milpAttackerStrategy = milp.getAttackerStrategy();
			System.out.println(milpAttackerStrategy);
		
			attackerMilpTime += (System.currentTimeMillis()-milpstart)/1000.0;
			if(milp.NumberCompromised() < bestObjective)
				bestObjective = milp.NumberCompromised();
			milp.cleanUp();	
			
			Map<Website, Integer> attackerStrategy = milp.getAttackerStrategy();
			int attackCost = 0;
			int maxAttackCost = 0;
			for(Website w: model_data.getWebsites()) {
				attackCost += w.costToAttack*attackerStrategy.get(w);
				maxAttackCost = Math.max(maxAttackCost, w.costToAttack*(1-attackerStrategy.get(w)));
			}
			System.out.println("AttackBudget = "+model_data.getAttackBudget()+ ", AttackBudgetUsed = "+attackCost+", maxAttackCost = "+maxAttackCost);
			
			
			
			
			solver.addConstraint(effortVector, bestObjective-eps);
			if(numIter % keepTimeEvery == 0) {
//				attackerTimeList.add(attackerMilpTime - attackerTimeList.get(attackerTimeList.size() - 1));
//				defenderTimeList.add(defenderLPTime - defenderTimeList.get(defenderTimeList.size() - 1));
				attackerTimeList.add(attackerMilpTime);
				defenderTimeList.add(defenderLPTime);
			}
			numIter++;
	    }
	    
		runtime = (System.currentTimeMillis()-start)/1000.0;
		numCompromised = bestObjective;
	}
	    
	    
	    
	public void solveSimple2() throws IloException, IOException{
		double start = System.currentTimeMillis();
	    timeLimit = 600;
		simple2LP solver = new simple2LP(model_data);
		solver.solve();
		runtime = (System.currentTimeMillis()-start)/1000.0;
		numCompromised = solver.getObjectiveValue();
//		System.out.println("simple2LP result: " + numCompromised);
	}
	
	public void solveSimple1() throws IloException, IOException{
		double start = System.currentTimeMillis();
	    timeLimit = 600;
		simple1LP solver = new simple1LP(model_data);
		solver.solve();
		runtime = (System.currentTimeMillis()-start)/1000.0;
		numCompromised = solver.getObjectiveValue();
		System.out.println("simple1LP result: " + numCompromised);
	}
	
	public void solveRelaxedLP() throws IloException, IOException{
		double start = System.currentTimeMillis();
		attackerMilpTime = 0;
		defenderLPTime = 0;
		defenderGetSolutionTime = new ArrayList<>();
	    iterLimit = 100;
	    timeLimit = 600;
	    keepTimeEvery = 40;
	    attackerTimeList = new ArrayList<>();
	    defenderTimeList = new ArrayList<>();
		
		//Need to solve the relaxed problem to get initial e vector
		StackelbergLP solver = new StackelbergLP(model_data);
		solver.solve();	
		Map<Website, Double> defenderStrategy = solver.getDefenderStrategy();	
		solver.deleteVars();
		

		//solver attacker BR MILP
		AttackerMILP milp = new AttackerMILP(model_data, defenderStrategy);
		milp.solve();
		double bestObjective = milp.NumberCompromised();
		Map<Website, Integer> effortVector = new HashMap<Website, Integer>();
		for(Website w : milp.getAttackerEffort().keySet()){
			double effort = milp.getAttackerEffort().get(w);
			effortVector.put(w, (int) effort);
		}
		PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter("output/vectors.csv", true)));
		w.println(String.join(",", effortVector.entrySet().toString()));
		w.close();
		milp.cleanUp();	

		System.out.println("Best Objective: "+bestObjective);
		System.out.println();
		runtime = (System.currentTimeMillis()-start)/1000.0;
		numCompromised = bestObjective;
	}	
	
	
	
	
	
	
	private void removeAllSubsets(ArrayList<Action> actions){
		//start with back of list and remove from start
		int index = actions.size()-1;
		
		while(index > 0){
			Action a = actions.get(index);
			
//			System.out.println("Testing :"+a.toString());
			
			int size = actions.size();
			
			for(int i=0; i<index; i++){
//				System.out.println(actions.get(i).toString());
				if(a.subset(actions.get(i))){
//					System.out.println("removing "+actions.get(i));
					actions.remove(i);
					i--;
					size--;
					index--;
				}
			}
			index--;
		}
		
	}
	
	public void generateMaxEffortVectors(Map<Action, ArrayList<Map<Website, Integer>>> maxEffort, Action a){
		ArrayList<Map<Website, Integer>> vectors = new ArrayList<Map<Website, Integer>>();
		maxEffort.put(a, vectors);
		
		Map<Website, Integer> eV = new HashMap<Website, Integer>();
		
//		for(int i=0; i<a.websites.size(); i++)
			effortUtil(vectors, eV, a, model_data.getAttackEffort(), a.websites);
		
	}
	
	public void effortUtil(ArrayList<Map<Website, Integer>> vectors, Map<Website, Integer> eV, Action a, int effort, ArrayList<Website> websitesLeft){
		// If current combination is ready to be added to list
		if (effort == 0 || websitesLeft.size() == 0) {
			vectors.add(eV);
			return;
		}

		for (Website w : websitesLeft) {
			Map<Website, Integer> eV1 = new HashMap<Website, Integer>(eV);
			if(effort >= w.alltraffic && !eV1.containsKey(w)){
				eV1.put(w, w.alltraffic);
				ArrayList<Website> wL1 = new ArrayList<Website>(websitesLeft);
				wL1.remove(w);
				effortUtil(vectors, eV1, a, effort - w.alltraffic, wL1);
			}else{
				eV1.put(w, effort);
				ArrayList<Website> wL1 = new ArrayList<Website>(websitesLeft);
				wL1.remove(w);
				effortUtil(vectors, eV1, a, 0, wL1);
			}
			
		}
		
	}
	
	public static ArrayList<Action> generateAllActions(SEModel model){
		ArrayList<Action> actions = new ArrayList<Action>();
		
		//Recursively build all actions
		Action a = new Action();
		
		for(int i=0; i<model.getWebsites().size(); i++)
			combinationUtil(model, actions, a, 0, model.getWebsites().size()-1, 0, i);
		
		
		return actions;
	}
	
	private static void combinationUtil(SEModel model, ArrayList<Action> actions, Action a, int start, int end, int index, int r){
		//If current combination is ready to be added to list
		if(index == r){
			int cost = 0;
			for(Website w : a.websites)
				cost += w.costToAttack;
			
			if(cost <= model.getAttackBudget() && !actions.contains(a) && a.websites.size() > 0)
				actions.add(a);
				
		}		
		
		for(int i=start; i<=end && end-i+1 >= r-index; i++ ){
			Action a1 = new Action(a.websites);
			a1.addWebsite(model.getWebsites().get(i));
			combinationUtil(model, actions, a1, i+1, end, index+1, r);
		}
		
	}
	

	public SEModel getModel_data() {
		return model_data;
	}

	public void setModel_data(SEModel model_data) {
		this.model_data = model_data;
	}

	public double getRuntime() {
		return runtime;
	}

	public double getNumCompromised() {
		return numCompromised;
	}
	
	public double getPresolveTime() {
		return presolveTime;
	}

	public double getAlgRuntime() {
		return algRuntime;
	}

	public double getDefenderLPTime() {
		return defenderLPTime;
	}

//	public double getDefenderGetSolutionTime() {
//		return defenderGetSolutionTime;
//	}
	
	public ArrayList<Double> getDefenderGetSolutionTime() {
		return defenderGetSolutionTime;
	}
	
	public double getAttackerMilpTime() {
		return attackerMilpTime;
	}
	
	public double getAttackerGreedyTime() {
		return attackerGreedyTime;
	}
	
	public ArrayList<Double> getAttackerTimeList(){
		return attackerTimeList;
	}
	
	public ArrayList<Double> getDefenderTimeList(){
		return defenderTimeList;
	}
	
	public double getNumIter() {
		return numIter;
	}
	
	public double getNumMilpIter() {
		return numMilpIter;
	}
	
	// added by Ryan
	public HashMap<Website, Integer> getActionAllTraffic(Action a) {
		actionAllTraffic = new HashMap<Website, Integer>();
		for (Website w:a.websites) {
			actionAllTraffic.put(w, w.alltraffic);
		}
		return actionAllTraffic;
	}
	
	public HashMap<Website, Integer> getActionOrgTraffic(Action a) {
		actionOrgTraffic = new HashMap<Website, Integer>();
		for (Website w:a.websites) {
			actionOrgTraffic.put(w, w.orgtraffic);
		}
		return actionOrgTraffic;
	}

}
