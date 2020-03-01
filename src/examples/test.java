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

public class test {
	
	private static ArrayList<ArrayList<Double>> defenderTimeLists;
	private static ArrayList<ArrayList<Double>> attackerTimeLists;

	/**
	 * Main accepts args to run the given experiment with an initial seed. 
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		int numGames = 10;
		int numWebsites;
		int seed = 93;
		int minNumWebsites = 20;
		int maxNumWebsites = 20;
		int stepNumWebsites = 100;
		boolean solveAllActions = false;
		boolean solveMaxEffort = false;
		boolean solveCutGeneration = true;
		boolean solveCutGenerationGreedy = false;
		boolean solveSimple1 = false;
		boolean solveSimple2 = false;
		boolean solveRelaxedLP = false;
		CommandLineArgs expArgs = new CommandLineArgs();	
		
		for(numWebsites = minNumWebsites; numWebsites <= maxNumWebsites; numWebsites = numWebsites + stepNumWebsites) {
			expArgs.parseCommandLineArgs(numGames, numWebsites, seed, solveAllActions, solveMaxEffort, 
					solveCutGeneration, solveCutGenerationGreedy, solveSimple1, solveSimple2, solveRelaxedLP);
			attackerTimeLists = new ArrayList<>();
			defenderTimeLists = new ArrayList<>();
			//Generate the model with the initial seed
			for(int i=0; i<expArgs.numGames; i++) {
				System.out.println("ag game: "+i);
				SEModel model = new SEModel(expArgs.seed+i, expArgs.numWebsites);
				//Run the experiments for the given inputs
				if (expArgs.solveAllActions) {
					try {
						solveModel(model, true, false, false, false, false, false, false, expArgs.seed);
					} catch (IOException | IloException e) {
						e.printStackTrace();
					}
				} else if (expArgs.solveMaxEffort) {
					try {
						solveModel(model, false, true, false, false, false, false, false, expArgs.seed);
					} catch (IOException | IloException e) {
						e.printStackTrace();
					}
				} else if (expArgs.solveCutGeneration) {
					try {
						solveModel(model, false, false, true, false, false, false, false, expArgs.seed);
					} catch (IOException | IloException e) {
						e.printStackTrace();
					}
				}else if (expArgs.solveCutGenerationGreedy) {
					try {
						solveModel(model, false, false, false, true, false, false, false, expArgs.seed);
					} catch (IOException | IloException e) {
						e.printStackTrace();
					}
				}else if (expArgs.solveSimple1) {
					try {
						solveModel(model, false, false, false, false, true, false, false, expArgs.seed);
					} catch (IOException | IloException e) {
						e.printStackTrace();
					}
				}else if (expArgs.solveSimple2) {
					try {
						solveModel(model, false, false, false, false, false, true, false, expArgs.seed);
					} catch (IOException | IloException e) {
						e.printStackTrace();
					}
				}else if (expArgs.solveRelaxedLP) {
					try {
						solveModel(model, false, false, false, false, false, false, true, expArgs.seed);
					} catch (IOException | IloException e) {
						e.printStackTrace();
					}
				}
				
			}
			
			if(expArgs.solveCutGeneration || expArgs.solveCutGenerationGreedy) {
				for(int i=0; i<expArgs.numGames; i++) {
						String outputFile = setOutputFile(expArgs.solveAllActions, expArgs.solveMaxEffort, 
								expArgs.solveCutGeneration, expArgs.solveCutGenerationGreedy, 
								expArgs.solveSimple1, expArgs.solveSimple2, expArgs.solveRelaxedLP, expArgs.seed, expArgs.numWebsites);
						PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, true)));
						ArrayList<Double> attackerTimePeriod = new ArrayList<>();
						ArrayList<Double> defenderTimePeriod = new ArrayList<>();
						for (int j=0; j<attackerTimeLists.get(i).size() - 1; j++) {
							attackerTimePeriod.add(attackerTimeLists.get(i).get(j+1) - attackerTimeLists.get(i).get(j));
							defenderTimePeriod.add(defenderTimeLists.get(i).get(j+1) - defenderTimeLists.get(i).get(j));
						}
						w.println(String.join(",", attackerTimePeriod.toString()));
						w.println(String.join(",", defenderTimePeriod.toString()));
						w.close();
				}
			}
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
			boolean solveSimple1, boolean solveSimple2, boolean solveRelaxedLP, long seed) throws IOException, IloException{
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
		} else if (solveSimple1) {
			solver.solveSimple1();
		} else if (solveSimple2) {
			solver.solveSimple2();
		} else if (solveRelaxedLP) {
			solver.solveRelaxedLP();
		}
		
		//After solving model, write output to file
		String outputFile = setOutputFile(solveAllActions, solveMaxEffort, solveCutGeneration, solveCutGenerationGreedy, 
				solveSimple1, solveSimple2, solveRelaxedLP, seed, model.getWebsites().size());		
		PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, true)));
		if(solveSimple2 || solveSimple1 || solveRelaxedLP) {
			w.println("seed" +", "+"size" + ", " + "value" + ", "+ "total time");
		} else if(solveCutGenerationGreedy) {
			w.println("seed" +", "+"size" + ", " + "value" + ", "+ "total time" +", "+"initial time"+", "+ "attacker time" +", "+
		"defender time" +", "+ "num iter" + ", " + "num milp iter");
		} else {
			w.println("seed" +", "+"size" + ", " + "value" + ", "+ "total time" +", "+"initial time"+", "+ "attacker time" +", "+
		"defender time" +", "+ "num iter");
		}
		
		if(solveAllActions) {
			w.println(seed+", "+model.getWebsites().size() + ", " + solver.getNumCompromised() + ", "+ solver.getRuntime()+", "+solver.getPresolveTime()
				+", "+solver.getAlgRuntime());
		} else if (solveMaxEffort) {
			w.println(seed+", "+model.getWebsites().size() + ", " + solver.getNumCompromised() + ", "+ solver.getRuntime()+", "+solver.getPresolveTime()
				+", "+solver.getAlgRuntime());
		} else if (solveCutGeneration) {
			w.println(seed+", "+model.getWebsites().size() + ", " + solver.getNumCompromised() + ", "+ solver.getRuntime()+", "+solver.getPresolveTime()
				+", "+solver.getAttackerMilpTime()+", "+solver.getDefenderLPTime()+", "+solver.getNumIter());
			attackerTimeLists.add(solver.getAttackerTimeList());
			defenderTimeLists.add(solver.getDefenderTimeList());
		} else if (solveCutGenerationGreedy) {
			w.println(seed+", "+model.getWebsites().size() + ", " + solver.getNumCompromised() + ", "+ solver.getRuntime()+", "+solver.getPresolveTime()
			+", "+solver.getAttackerGreedyTime()+", "+solver.getDefenderLPTime()+", "+solver.getNumIter()+", "+solver.getNumMilpIter());
			attackerTimeLists.add(solver.getAttackerTimeList());
			defenderTimeLists.add(solver.getDefenderTimeList());
		} else if (solveSimple1) {
			w.println(seed+", "+model.getWebsites().size() + ", " + solver.getNumCompromised() + ", "+ solver.getRuntime());
//			attackerTimeLists.add(solver.getAttackerTimeList());
//			defenderTimeLists.add(solver.getDefenderTimeList());
		} else if (solveSimple2) {
			w.println(seed+", "+model.getWebsites().size() + ", " + solver.getNumCompromised() + ", "+ solver.getRuntime());
//			attackerTimeLists.add(solver.getAttackerTimeList());
//			defenderTimeLists.add(solver.getDefenderTimeList());
		} else if (solveRelaxedLP) {
			w.println(seed+", "+model.getWebsites().size() + ", " + solver.getNumCompromised() + ", "+ solver.getRuntime());
//			attackerTimeLists.add(solver.getAttackerTimeList());
//			defenderTimeLists.add(solver.getDefenderTimeList());
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
			boolean solveSimple1, boolean solveSimple2, boolean solveRelaxedLP, long seed, int numWebsites ) {
		String output = "";
		
		if(solveAllActions) {
			output = "test_output/AllActions_" + seed + "_" + numWebsites +".csv";
		} else if (solveMaxEffort) {
			output = "test_output/MaxEffort_" + seed + "_" + numWebsites +".csv";
		} else if (solveCutGeneration) {
			output = "heuristicmilp/CutGeneration_" + seed + "_" + numWebsites +".csv";
		} else if (solveCutGenerationGreedy) {
			output = "test_output/CutGenerationGreedy_" + seed + "_" + numWebsites +".csv";
		} else if (solveSimple1) {
			output = "test_output/Simple1_" + seed + "_" + numWebsites +".csv";
		} else if (solveSimple2) {
			output = "test_output/Simple2_" + seed + "_" + numWebsites +".csv";
		} else if (solveRelaxedLP) {
			output = "test_output/RelaxedLP_" + seed + "_" + numWebsites +".csv";
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
		private boolean solveSimple1;
		private boolean solveSimple2;
		private boolean solveRelaxedLP;
		
		public CommandLineArgs(){		}
		
		private boolean parseCommandLineArgs(String [] args){
			numGames = Integer.parseInt(args[0]);
			numWebsites = Integer.parseInt(args[1]);
			seed = Long.parseLong(args[2]);
			solveAllActions = Boolean.parseBoolean(args[3]);
			solveMaxEffort = Boolean.parseBoolean(args[4]);
			solveCutGeneration = Boolean.parseBoolean(args[5]);
			solveCutGenerationGreedy = Boolean.parseBoolean(args[6]);
			solveSimple1 = Boolean.parseBoolean(args[7]);
			solveSimple2 = Boolean.parseBoolean(args[8]);
			solveRelaxedLP = Boolean.parseBoolean(args[9]);
			return true;
		}
		
		private boolean parseCommandLineArgs(int numGames, int numWebsites, long seed, boolean solveAllActions, boolean solveMaxEffort, boolean solveCutGeneration,
				boolean solveCutGenerationGreedy, boolean solveSimple1, boolean solveSimple2, boolean solveRelaxedLP){
			this.numGames = numGames;
			this.numWebsites = numWebsites;
			this.seed = seed;
			this.solveAllActions = solveAllActions;
			this.solveMaxEffort = solveMaxEffort;
			this.solveCutGeneration = solveCutGeneration;
			this.solveCutGenerationGreedy = solveCutGenerationGreedy;
			this.solveSimple1 = solveSimple1;
			this.solveSimple2 = solveSimple2;
			this.solveRelaxedLP = solveRelaxedLP;
			return true;
		}
	}
	
}
