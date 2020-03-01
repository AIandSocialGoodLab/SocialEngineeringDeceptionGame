package examples;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import Models.SEModel;
import ilog.concert.IloException;
import solvers.OptimalSolver;
import utilities.DeceptionGameHelper;

public class Experiments_new {
	
	private static ArrayList<ArrayList<Double>> defenderTimeLists;
	private static ArrayList<ArrayList<Double>> attackerTimeLists;
	
	private static double[][][] times;
	private static double[][][] values;
	/**
	 * Main accepts args to run the given experiment with an initial seed. 
	 * @param args
	 * @throws IOException 
	 * @throws IloException 
	 */
	public static void main(String[] args) throws IOException, IloException {
		//Parse command arguments, store in a simple structure to make referencing a bit easier and cleaner
//		CommandLineArgs expArgs = new CommandLineArgs();
//		expArgs.parseCommandLineArgs(args);
		int seed = 93;
		int numGames = 20;
		attackerTimeLists = new ArrayList<>();
		defenderTimeLists = new ArrayList<>();
		times = new double[5][7][numGames];
		values = new double[5][7][numGames];
		Ret ret;
		for(int i=2; i < 3; i++) {
			for(int j=12; j<numGames; j++) {
//				int n = 50 + 50 * i;
				int n = 4 + 2 * i;
				System.out.println("n: "+n+", j: "+j);
				SEModel model = new SEModel(seed+j, n);
				if(n <= 8) {
					ret = solveModel(model, true, false, false, false, false, seed+j);   // index 0: all actions
					times[0][i][j] = ret.time;
					values[0][i][j] = ret.value;
				}else {
					times[0][i][j] = 0.0;
					values[0][i][j] = 0.0;
				}
				if(n <= 12) {
					ret = solveModel(model, false, true, false, false, false, seed+j);   // index 1: max effort
					times[1][i][j] = ret.time;
					values[1][i][j] = ret.value;
				}else {
					times[1][i][j] = 0.0;
					values[1][i][j] = 0.0;
				}		
//				ret = solveModel(model, false, false, true, false, false, seed+j);   // index 2: CG
//				times[2][i][j] = ret.time;
//				values[2][i][j] = ret.value;
//				if(n <= 250) {
//					ret = solveModel(model, false, false, false, true, false, seed+j);   // index 3: CG Greedy
//					times[3][i][j] = ret.time;
//					values[3][i][j] = ret.value;
//				}else {
//					times[3][i][j] = 0.0;
//					values[3][i][j] = 0.0;					
//				}
//				ret = solveModel(model, false, false, false, false, true, seed+j);   // index 4: Relaxed LP
//				times[4][i][j] = ret.time;
//				values[4][i][j] = ret.value;
				
//				for(int k=0;k<5;k++) {
//					writeCSV(times[k], 5, numGames, "ijcai_output_small_plus/"+k+"times.csv");
//					writeCSV(values[k], 5, numGames, "ijcai_output_small_plus/"+k+"values.csv");
//				}
			}

		}
//		for(int k=0;k<5;k++) {
//			writeCSV(times[k], 5, numGames, "ijcai_output_small_plus/"+k+"times.csv");
//			writeCSV(values[k], 5, numGames, "ijcai_output_small_plus/"+k+"values.csv");
//		}
		

		

	}
	
	public static void writeCSV(double[][] results, int dim1, int dim2, String fname) throws IOException {
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < dim1; i++)//for each row
		{
		   for(int j = 0; j < dim2; j++)//for each column
		   {
		      builder.append(results[i][j]+"");//append to the output string
		      if(j < dim2 - 1)//if this is not the last row element
		         builder.append(",");//then add comma (if you don't like commas you can use spaces)
		   }
		   builder.append("\n");//append new line at the end of the row
		}
		BufferedWriter writer = new BufferedWriter(new FileWriter(fname));
		writer.write(builder.toString());//save the string representation of the board
		writer.close();
	}
	
	
	/**
	 * Wrapper function to run the desired optimization model for a given SE Model and run the optimal solver with different configurations. 
	 * @param model - SE Model
	 * @param solveAllActions - Run Solver for all Action.
	 * @param solveMaxEffort - Run model with all max effort vectors.
	 * @param solveCutGeneration - Run model with max effort cut generation. 
	 * @throws IOException
	 * @throws IloException
	 */
	public static Ret solveModel(SEModel model, boolean solveAllActions, boolean solveMaxEffort, boolean solveCutGeneration, boolean solveCutGenerationGreedy,
			boolean solveRelaxedLP, long seed) throws IOException, IloException{
		// Need to load cplex libraries
//		String cplexInputFile = "CplexConfig";
		String cplexInputFile = "CplexConfigMac";

		DeceptionGameHelper.loadLibrariesCplex(cplexInputFile);
		
		OptimalSolver solver = new OptimalSolver(model);
		
//		solver.solveEffortCutGeneration();
		
		if(solveAllActions) {
			solver.solve();
		} else if (solveMaxEffort) {
			solver.solveEffort();
		} else if (solveCutGeneration) {
			solver.solveEffortCutGeneration();
		} else if (solveCutGenerationGreedy) {
			solver.solveEffortCutGenerationGreedy();
		} else if (solveRelaxedLP) {
			solver.solveRelaxedLP();
		}
		
		double value = solver.getNumCompromised();
		double time = solver.getRuntime();
		Ret ret = new Ret(time, value);
		return ret;
		
		
	}	
	
	/**
	 * 
	 * @param solveAllActions
	 * @param solveMaxEffort
	 * @param solveCutGeneration
	 * @param seed
	 * @param numWebsites
	 * @return
	 */
	private static String setOutputFile(boolean solveAllActions, boolean solveMaxEffort, boolean solveCutGeneration, boolean solveCutGenerationGreedy,
			boolean solveUB, long seed, int numWebsites ) {
		String output = "";
		
		if(solveAllActions) {
			output = "output/AllActions_" + seed + "_" + numWebsites +".csv";
		} else if (solveMaxEffort) {
			output = "output/MaxEffort_" + seed + "_" + numWebsites +".csv";
		} else if (solveCutGeneration) {
			output = "output/CutGeneration_" + seed + "_" + numWebsites +".csv";
		} else if (solveCutGenerationGreedy) {
			output = "output/CutGenerationGreedy_" + seed + "_" + numWebsites +".csv";
		} else if (solveUB) {
			output = "output/UB_" + seed + "_" + numWebsites +".csv";
		}
		
		return output;
	}
	
	
	private static class Ret {
		   public double time;
		   public double value;
		   
		   public Ret(double time, double value){
			   this.time = time;
			   this.value = value;
		   }
		}
		
	/**
	 * 
	 *
	 */
	private static class CommandLineArgs{
		private int numGames;
		private int numWebsites;
		private long seed;
		private boolean solveAllActions;
		private boolean solveMaxEffort;
		private boolean solveCutGeneration;
		private boolean solveCutGenerationGreedy;
		private boolean solveUB;
		private int numBudget;
		
		public CommandLineArgs(){		}
		
		private boolean parseCommandLineArgs(String [] args){
			numGames = Integer.parseInt(args[0]);
			numWebsites = Integer.parseInt(args[1]);
			seed = Long.parseLong(args[2]);
			solveAllActions = Boolean.parseBoolean(args[3]);
			solveMaxEffort = Boolean.parseBoolean(args[4]);
			solveCutGeneration = Boolean.parseBoolean(args[5]);
			solveCutGenerationGreedy = Boolean.parseBoolean(args[6]);
			solveUB = Boolean.parseBoolean(args[7]);
			numBudget = Integer.parseInt(args[8]);
			return true;
		}
	}
	
}
