package examples;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Models.SEModel;
import Models.Website;
import utilities.DeceptionGameHelper;
import ilog.concert.IloException;
import solvers.AttackerDP;
import solvers.AttackerGreedy;
import solvers.AttackerLP;
import solvers.AttackerMILP;
import solvers.DefenderLP;
import solvers.OptimalSolver;
import solvers.StackelbergLP;

public class SimpleExample2 {
	private static int numHeuristics = 1;
	private static double[][][] ratio;

	public static void main(String [] args) throws IloException, IOException{
		int numExp = 10;
		int numWebsites;
		int seed = 93;
		int minNumWebsites = 50000;
		int maxNumWebsites = 100000;
		int stepNumWebsites = 50000;
		int webcounter = 0;
		ratio = new double[numHeuristics][(maxNumWebsites-minNumWebsites)/stepNumWebsites+1][numExp];
		for(int i = 0; i < numHeuristics; i++) {
			webcounter = 0;
			for(numWebsites = minNumWebsites; numWebsites <= maxNumWebsites; 
					numWebsites = numWebsites + stepNumWebsites) {
				for(int j=0; j<numExp; j++){
					SEModel model = new SEModel(seed+j, numWebsites);
					ratio[i][webcounter][j] = solveModelRelaxed(model, i, numWebsites, j, seed);
				}
				webcounter++;
			}
		}
//		double[] meanratio = new double[numHeuristics];
//		webcounter = 0;
//		for(int i = 0; i < numHeuristics; i++) {
//			webcounter = 0;
//			for(numWebsites = minNumWebsites; numWebsites <= maxNumWebsites; 
//					numWebsites = numWebsites + stepNumWebsites) {
//				for(int j=0; j<numExp; j++){
//					SEModel model = new SEModel(seed+j, numWebsites);
//					meanratio[i] += 1.0/(((maxNumWebsites-minNumWebsites)/stepNumWebsites+1)*numExp) 
//							*ratio[i][webcounter][j];
//				}
//				webcounter++;
//			}
//		}
//		
//		System.out.println(Arrays.toString(meanratio));
		
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
	public static void solveModel(SEModel model, boolean solveAllActions, boolean solveMaxEffort, boolean solveCutGeneration) throws IOException, IloException{
		// Need to load cplex libraries
		String cplexInputFile = "CplexConfig";
//		 String cplexInputFile = "CplexConfigMac";

		DeceptionGameHelper.loadLibrariesCplex(cplexInputFile);
		
		OptimalSolver solver = new OptimalSolver(model);
		
		if(solveAllActions) {
			solver.solve();
		} else if (solveMaxEffort) {
			solver.solveEffort();
		} else if (solveCutGeneration) {
			solver.solveEffortCutGeneration();
		}
	}
	
	/**
	 * This function solves the relaxed version of the SE model. 
	 * @param model
	 * @throws IOException
	 * @throws IloException
	 */
	public static double solveModelRelaxed(SEModel model, int heuIndex, int numWebsites,
			int gameIndex, int seed) throws IOException, IloException{
		// Need to load cplex libraries
//		String cplexInputFile = "CplexConfig";
		String cplexInputFile = "CplexConfigMac";
//		List<AttackerGreedy> attackers = new ArrayList<AttackerGreedy>();
		
		DeceptionGameHelper.loadLibrariesCplex(cplexInputFile);

//		model.cutModel();
		// These solvers do not work
		// Randomly generated model, input is initial seed
	
		StackelbergLP solver = new StackelbergLP(model);
		solver.solve();
		Map<Website, Double> defenderStrategy = solver.getDefenderStrategy();
		Map<Website, Double> attackerStrategy = solver.getAttackerStrategy();
		Map<Website, Double> attackerEffort = solver.getAttackerEffort();


		double milpStart = System.currentTimeMillis();
		AttackerMILP milp = new AttackerMILP(model, solver.getDefenderStrategy());
		milp.solve();
		double milpValue = milp.NumberCompromised();
		milp.cleanUp();
		double milpTime = (System.currentTimeMillis()-milpStart)/1000.0;
		
		double dpStart = System.currentTimeMillis();
		AttackerGreedy greedy = new AttackerGreedy(model, solver.getDefenderStrategy(), false, heuIndex);
		greedy.solve();
		double greedyValue = greedy.getValue();
		double greedyTime = (System.currentTimeMillis()-dpStart)/1000.0;
		
		String outputFile = "1shot_output/" + numWebsites + ".csv";
		PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, true)));
		w.println("seed" +", "+ "heuIndex" +", "+ "size" + ", " + "value" + ", "+ "total time" + ", "
		+ "milp value" + ", " + "milp time" + ", " + "approx ratio"+ ", " + "error");
		w.println(seed+", "+ heuIndex + ", " + numWebsites + ", " + greedyValue + ", " + greedyTime + ", " 
				+ milpValue + ", " + milpTime + ", " + greedyValue/milpValue + ", " + (milpValue-greedyValue)/milpValue);
		w.close();
		return greedyValue/milpValue;
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
