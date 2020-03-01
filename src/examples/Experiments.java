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

public class Experiments {
	
	private static ArrayList<ArrayList<Double>> defenderTimeLists;
	private static ArrayList<ArrayList<Double>> attackerTimeLists;

	/**
	 * Main accepts args to run the given experiment with an initial seed. 
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		//Parse command arguments, store in a simple structure to make referencing a bit easier and cleaner
		CommandLineArgs expArgs = new CommandLineArgs();
		expArgs.parseCommandLineArgs(args);
		attackerTimeLists = new ArrayList<>();
		defenderTimeLists = new ArrayList<>();
		//Generate the model with the initial seed
		for(int i=0; i<expArgs.numGames; i++) {
//		for(int i=6; i<7; i++) {
			SEModel model = new SEModel(expArgs.seed+i, expArgs.numWebsites);
			
			//Run the experiments for the given inputs
			if (expArgs.solveAllActions) {
				try {
					solveModel(model, true, false, false, false, false, expArgs.seed);
				} catch (IOException | IloException e) {
					e.printStackTrace();
				}
			} else if (expArgs.solveMaxEffort) {
				try {
					solveModel(model, false, true, false, false, false, expArgs.seed);
				} catch (IOException | IloException e) {
					e.printStackTrace();
				}
			} else if (expArgs.solveCutGeneration) {
				try {
					solveModel(model, false, false, true, false, false, expArgs.seed);
				} catch (IOException | IloException e) {
					e.printStackTrace();
				}
			}else if (expArgs.solveCutGenerationGreedy) {
				try {
					solveModel(model, false, false, false, true, false, expArgs.seed);
				} catch (IOException | IloException e) {
					e.printStackTrace();
				}
			}else if (expArgs.solveUB) {
				try {
					solveModel(model, false, false, false, false, true, expArgs.seed);
				} catch (IOException | IloException e) {
					e.printStackTrace();
				}
			}
			
		}
		
		for(int i=0; i<expArgs.numGames; i++) {
//			for(int j=0; j<attackerTimeLists.get(i).size(); j++) {
				String outputFile = setOutputFile(expArgs.solveAllActions, expArgs.solveMaxEffort, expArgs.solveCutGeneration, expArgs.solveCutGenerationGreedy,
						expArgs.solveUB, expArgs.seed, expArgs.numWebsites);
				PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, true)));
//				System.out.println(String.join(",", attackerTimeLists.get(i).toString()));
//				ArrayList<Double> defenderTimeRatio = new ArrayList<>();
				ArrayList<Double> attackerTimePeriod = new ArrayList<>();
				ArrayList<Double> defenderTimePeriod = new ArrayList<>();
				for (int j=0; j<attackerTimeLists.get(i).size() - 1; j++) {
//					defenderTimeRatio.add(defenderTimeLists.get(i).get(j)/(defenderTimeLists.get(i).get(j) + attackerTimeLists.get(i).get(j)));
					attackerTimePeriod.add(attackerTimeLists.get(i).get(j+1) - attackerTimeLists.get(i).get(j));
					defenderTimePeriod.add(defenderTimeLists.get(i).get(j+1) - defenderTimeLists.get(i).get(j));
				}
				w.println(String.join(",", attackerTimePeriod.toString()));
				w.println(String.join(",", defenderTimePeriod.toString()));
//				w.println(String.join(",", defenderTimeRatio.toString()));
				w.close();
//			}
		}
		

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
	public static void solveModel(SEModel model, boolean solveAllActions, boolean solveMaxEffort, boolean solveCutGeneration, boolean solveCutGenerationGreedy,
			boolean solveUB, long seed) throws IOException, IloException{
		// Need to load cplex libraries
//		String cplexInputFile = "CplexConfig";
		String cplexInputFile = "CplexConfigMac";

		DeceptionGameHelper.loadLibrariesCplex(cplexInputFile);
		
		OptimalSolver solver = new OptimalSolver(model);
		

		
		if(solveAllActions) {
			solver.solve();
		} else if (solveMaxEffort) {
			solver.solveEffort();
		} else if (solveCutGeneration) {
			solver.solveEffortCutGeneration();
		} else if (solveCutGenerationGreedy) {
			solver.solveEffortCutGenerationGreedy();
		} else if (solveUB) {
			solver.solveUB();
		}
		
		//After solving model, write output to file
		String outputFile = setOutputFile(solveAllActions, solveMaxEffort, solveCutGeneration, solveCutGenerationGreedy, 
				solveUB, seed, model.getWebsites().size());
		
		PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, true)));
		w.println("seed" +", "+"size" + ", " + "value" + ", "+ "total time" +", "+"initial time"+", "+ "attacker time" +", "+ "defender time" +", "+ "num iter");
//		w.println("seed" +", "+"size" + ", " + "value" + ", "+ "total time" +", "+"initial time"+", "+ "attacker time" +", "+ "defender time" +", "
//+ "defender get solution time" + ", " + "num iter");
		if(solveAllActions) {
			w.println(seed+", "+model.getWebsites().size() + ", " + solver.getNumCompromised() + ", "+ solver.getRuntime()+", "+solver.getPresolveTime()
				+", "+solver.getAlgRuntime());
		} else if (solveMaxEffort) {
			w.println(seed+", "+model.getWebsites().size() + ", " + solver.getNumCompromised() + ", "+ solver.getRuntime()+", "+solver.getPresolveTime()
				+", "+solver.getAlgRuntime());
		} else if (solveCutGeneration) {
			w.println(seed+", "+model.getWebsites().size() + ", " + solver.getNumCompromised() + ", "+ solver.getRuntime()+", "+solver.getPresolveTime()
				+", "+solver.getAttackerMilpTime()+", "+solver.getDefenderLPTime()+", "+solver.getNumIter());
//			w.println(String.join(",", solver.getDefenderGetSolutionTime().toString()));
			attackerTimeLists.add(solver.getAttackerTimeList());
			defenderTimeLists.add(solver.getDefenderTimeList());
		} else if (solveCutGenerationGreedy) {
			w.println(seed+", "+model.getWebsites().size() + ", " + solver.getNumCompromised() + ", "+ solver.getRuntime()+", "+solver.getPresolveTime()
			+", "+solver.getAttackerGreedyTime()+", "+solver.getDefenderLPTime()+", "+solver.getNumIter());
//			w.println(String.join(",", solver.getDefenderGetSolutionTime().toString()));
			attackerTimeLists.add(solver.getAttackerTimeList());
			defenderTimeLists.add(solver.getDefenderTimeList());
		} else if (solveUB) {
			w.println(seed+", "+model.getWebsites().size() + ", " + solver.getNumCompromised() + ", "+ solver.getRuntime()+", "+0
			+", "+solver.getAttackerGreedyTime()+", "+solver.getDefenderLPTime()+", "+solver.getNumIter());
//			w.println(String.join(",", solver.getDefenderGetSolutionTime().toString()));
			attackerTimeLists.add(solver.getAttackerTimeList());
			defenderTimeLists.add(solver.getDefenderTimeList());
		}
		
		w.close();
		
		
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
			return true;
		}
	}
	
}
