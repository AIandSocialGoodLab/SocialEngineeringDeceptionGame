package examples;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import Models.SEModel;
import Models.Website;
import utilities.DeceptionGameHelper;
import ilog.concert.IloException;
import solvers.AttackerLP;
import solvers.AttackerMILP;
import solvers.DefenderLP;
import solvers.OptimalSolver;
import solvers.StackelbergLP;

public class SimpleExample3 {
	
	private static ArrayList<Double> vals;
	private static ArrayList<Double> eqvals;
	
	public static void main(String [] args) throws IloException, IOException{
		vals = new ArrayList<>();
		eqvals = new ArrayList<>();
		//These solvers do not work
		//Randomly generated model, input is initial seed
		for(int i=0; i<10; i++){
			SEModel model = new SEModel(55+i, 3);
	
//			model.printModel();
			
//			System.out.println();
//			System.out.println("Solving Full Optimization");
//			System.out.println();
			System.out.println("-------------------------");
//			solveModel(model, false, false, false, true, false, false);
			solveModel(model, false, false, true, false, false, false);
//			solveModel(model, true, false, false, true);
//			for(Website w: model.getWebsites()) {
//				System.out.println(w);
//				System.out.println(w.costToAlter);
//			}
//			System.out.println();
//			System.out.println();
//			System.out.println("-------------------------");
//			System.out.println("-------------------------");
//			System.out.println();
//			System.out.println();
//			System.out.println("Solving All Max Effort Optimization");
//			
//			solveModel(model, false, true, false);
			
//			System.out.println();
//			System.out.println();
//			System.out.println("-------------------------");
//			System.out.println("-------------------------");
//			System.out.println();
//			System.out.println();
//			System.out.println("Solving Cut Generation Optimization");
//			
//			solveModel(model, false, false, true);
			
//			System.out.println();
//			System.out.println();
//			System.out.println("-------------------------");
//			System.out.println("-------------------------");
//			System.out.println();
//			System.out.println("Solving Relaxed LP");

//			solveModelRelaxed(model);
			
//			System.out.println();
//			System.out.println();
//			System.out.println("-------------------------");
//			System.out.println("-------------------------");
//			System.out.println();
		
		}
//		ArrayList<Double> diff = new ArrayList<>();
//		double maxdiff = -1.0;
//		for(int i = 0; i < vals.size(); i++) {
//			diff.add(vals.get(i) - eqvals.get(i));
//			if(Math.abs(vals.get(i) - eqvals.get(i)) > maxdiff) {
//				maxdiff = Math.abs(vals.get(i) - eqvals.get(i));
//			};
//		}
//		System.out.println(String.join(",", diff.toString()));
//		System.out.println(maxdiff);
//		PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter("output.csv", true)));
//		w.println(String.join(",", diff.toString()));
//		w.close();
	}
	
	/**
	 * Wrapper function to run the desired optimization model for a given SE Model. 
	 * @param model - SE Model
	 * @param solveAllActions - Run Solver for all Action.
	 * @param solveMaxEffort - Run model with all max effort vectors.
	 * @param solveCutGeneration - Run model with max effort cut generation. 
	 * @throws IOException
	 * @throws IloException
	 */
	public static void solveModel(SEModel model, boolean solveAllActions, boolean solveMaxEffort, boolean solveCutGeneration, 
			boolean solveSimple1, boolean solveSimple2, boolean equtil) throws IOException, IloException{
		// Need to load cplex libraries
//		String cplexInputFile = "CplexConfig";
		 String cplexInputFile = "CplexConfigMac";

		DeceptionGameHelper.loadLibrariesCplex(cplexInputFile);
		
		OptimalSolver solver = new OptimalSolver(model, equtil);
		
		if(solveAllActions) {
			solver.solve();
			if(equtil) {
				eqvals.add(solver.getNumCompromised());
			}
			else {
				vals.add(solver.getNumCompromised());
			}
		} else if (solveMaxEffort) {
			solver.solveEffort();
		} else if (solveCutGeneration) {
			solver.solveEffortCutGeneration();
		} else if (solveSimple1) {
			solver.solveSimple1();
		} else if (solveSimple2) {
			solver.solveSimple2();
		}
	}
	
	/**
	 * This function solves the relaxed version of the SE model. 
	 * @param model
	 * @throws IOException
	 * @throws IloException
	 */
	public static void solveModelRelaxed(SEModel model) throws IOException, IloException{
		// Need to load cplex libraries
		String cplexInputFile = "CplexConfig";
//		String cplexInputFile = "CplexConfigMac";
		
		DeceptionGameHelper.loadLibrariesCplex(cplexInputFile);

		// These solvers do not work
		// Randomly generated model, input is initial seed
	
		StackelbergLP solver = new StackelbergLP(model);

		solver.solve();
		
		System.out.println();
		System.out.println("Num Compromised: "+solver.getNumCompromised());
		System.out.println();
		
		Map<Website, Double> attackerStrategy = solver.getAttackerStrategy();
		Map<Website, Double> attackerEffort = solver.getAttackerEffort();
		
		
		//Solve the Attacker Relaxed BR, see if we get integer values
		AttackerLP lp = new AttackerLP(model, solver.getDefenderStrategy());
		
		lp.solve();
		
		System.out.println();
		System.out.println("Solving Attacker LP BR");
		System.out.println("Num Compromised: "+lp.NumberCompromised());
		System.out.println(lp.getAttackerStrategy());
		System.out.println(lp.getAttackerEffort());
		System.out.println();
		
		lp.cleanUp();
		
		//Solve the Attacker Relaxed BR, see if we get integer values
		AttackerMILP milp = new AttackerMILP(model, solver.getDefenderStrategy());

		milp.solve();

		System.out.println();
		System.out.println("Solving Attacker MILP BR");
		System.out.println("Num Compromised: " + milp.NumberCompromised());
		System.out.println(milp.getAttackerStrategy());
		System.out.println(milp.getAttackerEffort());
		System.out.println();

		milp.cleanUp();
		
//		DefenderLP lp = new DefenderLP(model, attackerEffort);
//		
//		lp.solve();
//		
//		Map<Website, Double> defenderStrategy = lp.getDefenderStrategy();
//
//		System.out.println();
//		System.out.println("Compromised: " + lp.NumberCompromised());
//
//		System.out.println();
//		System.out.println("Defender Strategy");
//		System.out.println();
//		System.out.print("[");
//		for (Website w : model.getWebsites()) {
//			System.out.print(defenderStrategy.get(w) + ",");
//		}
//		System.out.print("]");
		
	}
	
	public static SEModel example1(){
		SEModel model = new SEModel();
		
		model.setAttackBudget(100);
		model.setAttackEffort(1200);
		model.setDefenderBudget(200);
		
		Website w1 = new Website(1000, 100, 50, 2);
		Website w2 = new Website(500, 100, 40, 1);
		Website w3 = new Website(2000, 100, 50, 3);
		ArrayList<Website> websites = new ArrayList<Website>();
		websites.add(w1);
		websites.add(w2);
		websites.add(w3);
		model.setWebsites(websites);
		
		return model;
	}

}
