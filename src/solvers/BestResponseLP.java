package solvers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import Models.Action;
import Models.SEModel;
import Models.Website;
import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

public class BestResponseLP {
	
	private SEModel model_data;
	private Action br;
	private Map<Action, ArrayList<Map<Website, Integer>>> maxEffort;
	private ArrayList<Action> actions;
	private Map<Website, Integer> brEffortVector;
	
//	solution 
	private Map<Website, Double> defenderStrategy;
	private double numCompromised;
	// added by Ryan
	private Map<Website, Double> attackerUtility;
	
	private Double objectiveValue; 
	
//	cplex variables
	private IloCplex cplex;
	
//	objective 
	private IloObjective objective;
	private IloNumVar v;
	
//	decision variables
	private Map<Website, IloNumVar> defenderStratVars; // x
	
//	constraints
	private IloConstraint defenderBudgetConstraint;
	private ArrayList<IloConstraint> attackerObjectiveConstraint; // 
	private ArrayList<IloConstraint> attackerBRConstraint; // 
	// added by Ryan
	private ArrayList<IloConstraint> attackerUtilityConstraint;
	private IloNumVar u;
	private boolean equtil;
	
	public BestResponseLP(SEModel model, Map<Action, ArrayList<Map<Website, Integer>>> maxEffort, ArrayList<Action> actions, 
			Action br, Map<Website, Integer> brEffortVector, boolean equtil){
		model_data = model;
		this.maxEffort = maxEffort;
		this.actions = actions;		
		this.br = br;
		this.brEffortVector = brEffortVector;
		this.equtil = equtil;
	}
	
	public void solve() throws IloException{
		generateLP();
		
//		writeProblem("SE.lp");

		cplex.setOut(null);
		cplex.solve();
		
		if(cplex.isPrimalFeasible())
			extractSolution();
		
	}
	
	public void generateLP() throws IloException {
		cplex = new IloCplex();
		
		ArrayList<Website> websites = model_data.getWebsites();
		
		defenderStratVars = new HashMap<Website, IloNumVar>();
		
		attackerObjectiveConstraint = new ArrayList<IloConstraint>();
		attackerBRConstraint = new ArrayList<IloConstraint>();
		
		v = cplex.numVar(0, Double.MAX_VALUE, IloNumVarType.Float, "V");
		u = cplex.numVar(0, Double.MAX_VALUE, IloNumVarType.Float, "U");
		
		//initialize defender variables
		for(Website w : websites){
			defenderStratVars.put(w, cplex.numVar(0, 1, IloNumVarType.Float, "X_"+w.id));
		}
		

//		defender budget constraint
		IloLinearNumExpr defenderBudgetExpr =  cplex.linearNumExpr();
		for (Website w:websites) {
			defenderBudgetExpr.addTerm(w.orgtraffic*w.costToAlter, defenderStratVars.get(w));
		}
		defenderBudgetConstraint = cplex.addLe(defenderBudgetExpr, model_data.getDefenderBudget());
		
		
		//initialize objective constraints
		IloNumExpr expr = cplex.linearNumExpr(0.0);
		for (Website w : brEffortVector.keySet()) { // rhs
			double kw = (w.orgtraffic * brEffortVector.get(w)) / (double) w.alltraffic;
			expr = cplex.sum(expr, cplex.sum(kw, cplex.prod(-kw, defenderStratVars.get(w))));
		}
//		cplex.addGe(v, expr, "OBJ");
		cplex.addEq(v, expr, "OBJ");

	
		//initialize utility constraints
		if(equtil) {
			expr = cplex.linearNumExpr(0.0);
			for (Website w : brEffortVector.keySet()) { // rhs
				double kw = (w.orgtraffic * brEffortVector.get(w)) / (double) w.alltraffic;
				expr = cplex.prod(cplex.diff(1.0, defenderStratVars.get(w)), w.orgtraffic * brEffortVector.get(w) * brEffortVector.size() / (double) w.alltraffic);
				cplex.addEq(v, expr, "UTI" + w.id);
			}
		}
//		

		for (Action a : actions) {
			for (Map<Website, Integer> map2 : maxEffort.get(a)) {
				if(a.equals(br)){
					if(!maxEffort.equals(map2)){
						IloNumExpr expr1 = cplex.linearNumExpr(0.0);
						for (Website w : map2.keySet()) { // rhs
							double kw = (w.orgtraffic * map2.get(w)) / (double) w.alltraffic;
							expr1 = cplex.sum(expr1, cplex.sum(kw, cplex.prod(-kw, defenderStratVars.get(w))));
						}
						cplex.addGe(expr, expr1, "BR_" + br.id + "_" + a.id);
					}
				}else{
					IloNumExpr expr1 = cplex.linearNumExpr(0.0);
					for (Website w : map2.keySet()) { // rhs
						double kw = (w.orgtraffic * map2.get(w)) / (double) w.alltraffic;
						expr1 = cplex.sum(expr1, cplex.sum(kw, cplex.prod(-kw, defenderStratVars.get(w))));
					}
					cplex.addGe(expr, expr1, "BR_" + br.id + "_" + a.id);
				}
			}
			
		}
		
		objective = cplex.addMinimize(v);
		
	}
	
	private void extractSolution() throws IloException {
		objectiveValue = cplex.getObjValue();
		defenderStrategy = new HashMap<Website, Double>();
		
		for (Website w:model_data.getWebsites()) {
			defenderStrategy.put(w, cplex.getValue(defenderStratVars.get(w)));			
		}
		
		// added by Ryan
		attackerUtility = new HashMap<Website, Double>();
//		for (Website w:brEffortVector.keySet()) {
//			attackerUtility.put(w, (1-defenderStrategy.get(w)) * w.orgtraffic * brEffortVector.get(w) / (double) w.alltraffic);
////			attackerUtility.put(w, (double) brEffortVector.get(w));
//		}
		for (Website w:model_data.getWebsites()) {
			if(brEffortVector.containsKey(w)) {
				attackerUtility.put(w, (1-defenderStrategy.get(w)) * w.orgtraffic * brEffortVector.get(w) / (double) w.alltraffic);
			}
			else {
				attackerUtility.put(w, (1-defenderStrategy.get(w)) * w.orgtraffic * w.alltraffic / (double) w.alltraffic);
			}
//			attackerUtility.put(w, (double) brEffortVector.get(w));
		}
		
		
	}
	

	public Map<Website, Double> getDefenderStrategy() {
		return defenderStrategy;
	}
	
	// added by Ryan
	public Map<Website, Double> getAttackerUility() {
		return attackerUtility;
	}
	
	public double getDefenderLeftover() {
		Double defenderSum = 0.0;
		for (Website w:model_data.getWebsites()) {
			defenderSum = defenderSum + defenderStrategy.get(w) * w.orgtraffic * w.costToAlter;
		}
		return model_data.getDefenderBudget() - defenderSum;
	}

	public Double getObjectiveValue() {
		return objectiveValue;
	}
	
	public void writeProblem(String filename) throws IloException{
		cplex.exportModel(filename);
	}
	
	public void writeSolution(String filename) throws IloException{
		cplex.writeSolution(filename);
	}
	
	public void deleteVars() throws IloException{
		defenderStratVars.clear();; // x
		
//		constraints
		attackerObjectiveConstraint.clear();; // 
		attackerBRConstraint.clear();
		
		if(cplex != null)
			cplex.end();
		//cplex.clearModel();
		
		cplex = null;
	}
}
