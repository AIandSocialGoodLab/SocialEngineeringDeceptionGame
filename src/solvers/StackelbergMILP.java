package solvers;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

import Models.SEModel;
import Models.Website;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloConstraint;
import ilog.cplex.IloCplex;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;


public class StackelbergMILP {
	private SEModel model_data;
	
//	solution 
	private Map<Website, Double> attackerStrategy;
	private Map<Website, Double> attackerEffort;
	private Map<Website, Double> defenderStrategy;
	private double numCompromised;
	
	private Double objectiveValue; 
	
//	cplex variables
	private IloCplex cplex;
	
//	objective 
	private IloObjective objective;
	
//	decision variables
	private Map<Website, IloNumVar> defenderStratVars; // x
	
	private IloNumVar attackerStratBudgetDualVar; // lambda
	private IloNumVar attackerEffortBudgetDualVar; // mu
	private Map<Website, IloNumVar> attackerEffortAllocationDualVars; // gamma_w
	private Map<Website, IloNumVar> attackerEffortUBDualVars; // alpha_w
	private Map<Website, IloNumVar> attackerStratUBDualVars; // beta_w
	
//	constraints
	private IloConstraint defenderBudgetConstraint;
	private Map<Website, IloConstraint> attackerStratDualConstraint; // y_w
	private Map<Website, IloConstraint> attackerEffortDualConstraint; // e_w
//  Ryan added constraints
	private Map<Website, IloConstraint> attackerEffortDualZeroConstraint; 
	private boolean feasible;
	
	public StackelbergMILP(SEModel model_data) throws IloException {
		this.model_data = model_data;
		this.feasible = true;
		generateLP();
	}
	
	public void solve() throws IloException {
		
		
		cplex.setOut(null);
		
		cplex.solve();
		if(cplex.isPrimalFeasible()) {
			extractSolution();
		}
		else {
			feasible = false;
		}

		
		numCompromised = 0;
		for(Website w : model_data.getWebsites())
			numCompromised += (((double)w.orgtraffic/w.alltraffic)*(1-defenderStrategy.get(w))*attackerEffort.get(w));
//		System.out.printf("found solution with value: %.2f\n",objectiveValue);
//		System.out.println("");
		
//		for (Website w:model_data.getWebsites()) {
//			System.out.printf("defender strategy for website %s:%.3f\n", w.name, defenderStrategy.get(w));
//		}
//		System.out.println("");
//		for (Website w:model_data.getWebsites()) {
//			System.out.printf("attacker strategy for website %s:%.2f\n", w.name, attackerStrategy.get(w));
//		}
//		System.out.println("");
//		for (Website w:model_data.getWebsites()) {
//			System.out.printf("attacker effort for website %s:%.3f\n", w.name, attackerEffort.get(w));
//		}
//		System.out.println("");
//		for (Website w:model_data.getWebsites()) {
//			System.out.printf("attacker expected # compromised for website %s:%.2f\n", w.name, 
//					(((double)w.orgtraffic/w.alltraffic)*(1-defenderStrategy.get(w))*attackerEffort.get(w)));
//		}
//		System.out.println("");
//		for (Website w:model_data.getWebsites()) {
//			System.out.printf("attacker expected coefficients for website %s:%.3f\n", w.name, 
//					(((double)w.orgtraffic/w.alltraffic)*(1-defenderStrategy.get(w))));
//		}
	}
	
	private void extractSolution() throws IloException {
		objectiveValue = cplex.getObjValue();
		attackerStrategy = new HashMap<Website, Double>();
		attackerEffort = new HashMap<Website, Double>();
		defenderStrategy = new HashMap<Website, Double>();
		
		for (Website w:model_data.getWebsites()) {
			defenderStrategy.put(w, cplex.getValue(defenderStratVars.get(w)));
			attackerStrategy.put(w, cplex.getDual((IloRange) attackerStratDualConstraint.get(w)));
			attackerEffort.put(w, cplex.getDual((IloRange) attackerEffortDualConstraint.get(w)));
			
		}
		
	}
	
	private void generateLP() throws IloException {
		cplex = new IloCplex();
		
		ArrayList<Website> websites = model_data.getWebsites();
		defenderStratVars = new HashMap<Website, IloNumVar>();
		attackerEffortAllocationDualVars = new HashMap<Website, IloNumVar>();
		attackerEffortUBDualVars = new HashMap<Website, IloNumVar>();
		attackerStratUBDualVars = new HashMap<Website, IloNumVar>();
		
		attackerStratDualConstraint = new HashMap<Website, IloConstraint>();
		attackerEffortDualConstraint = new HashMap<Website, IloConstraint>();
		
		attackerEffortDualZeroConstraint = new HashMap<Website, IloConstraint>();
		
//		initialize singleton dual variables for budget
		attackerStratBudgetDualVar = cplex.numVar(0, Double.MAX_VALUE, IloNumVarType.Float, "attacker_strat_budget_dual_var");
		attackerEffortBudgetDualVar = cplex.numVar(0, Double.MAX_VALUE, IloNumVarType.Float, "attacker_effort_budget_dual_var");
		
//		initialize variables for each website
		for (Website w:websites) {
			defenderStratVars.put(w, cplex.numVar(0, 1, IloNumVarType.Float, "defender_strat_var"));
			attackerEffortAllocationDualVars.put(w, cplex.numVar(0, Double.MAX_VALUE, IloNumVarType.Float, "attacker_effort_allocation_dual_vars"));
			attackerEffortUBDualVars.put(w, cplex.numVar(0, Double.MAX_VALUE, IloNumVarType.Float, "attacker_effort_UB_dual_vars"));
			attackerStratUBDualVars.put(w, cplex.numVar(0, Double.MAX_VALUE, IloNumVarType.Float, "attacker_strat_UB_dual_vars"));	
		}
		
//		defender budget constraint
		IloLinearNumExpr defenderBudgetExpr =  cplex.linearNumExpr();
		for (Website w:websites) {
			defenderBudgetExpr.addTerm(w.orgtraffic*w.costToAlter, defenderStratVars.get(w));
		}
		defenderBudgetConstraint = cplex.addLe(defenderBudgetExpr, model_data.getDefenderBudget());
		
//		attacker strategy dual constraint
		for (Website w:websites) {
			attackerStratDualConstraint.put(w, 
					cplex.addGe(
							cplex.sum(cplex.prod(w.costToAttack, attackerStratBudgetDualVar),
										cplex.prod(-w.alltraffic, attackerEffortAllocationDualVars.get(w)),
										attackerStratUBDualVars.get(w)
									), 0)
					);
		}
		
//		attacker effort dual constraint
		for (Website w:websites) {
			attackerEffortDualConstraint.put(w, 
					cplex.addGe(
//							lhs
							cplex.sum(attackerEffortBudgetDualVar,
									attackerEffortAllocationDualVars.get(w),
									attackerEffortUBDualVars.get(w)
									), 
//							>= rhs
							cplex.sum(w.orgtraffic*1.0/w.alltraffic,
									cplex.prod(-w.orgtraffic*1.0/w.alltraffic, defenderStratVars.get(w))
									)
							)
					);
		}
		
//		// Ryan added remove attackerEffortUBDualVars
		for (Website w:websites) {
//			attackerEffortDualZeroConstraint.put(w, 
//					cplex.addEq(attackerStratUBDualVars.get(w), 0));
//			attackerEffortDualZeroConstraint.put(w, 
//					cplex.addGe(attackerStratUBDualVars.get(w), 0));
			attackerEffortDualZeroConstraint.put(w, 
					cplex.addEq(attackerEffortUBDualVars.get(w), 0));
		}	
		
//		create objective
		IloLinearNumExpr objective_expr = cplex.linearNumExpr();
		objective_expr.addTerm(model_data.getAttackBudget(), attackerStratBudgetDualVar);
		objective_expr.addTerm(model_data.getAttackEffort(), attackerEffortBudgetDualVar);
		
		for (Website w:websites) {
			objective_expr.addTerm(Math.min(w.alltraffic, model_data.getAttackEffort()), attackerEffortUBDualVars.get(w));
			objective_expr.addTerm(1, attackerStratUBDualVars.get(w));
		}
			
		objective = cplex.addMinimize(objective_expr);
	}
	
	
	public void addConstraint(Map<Website, Integer> newEffortVector, double prevValue) throws IloException {
		IloNumExpr expr = cplex.linearNumExpr(0.0);
		for(Website w : newEffortVector.keySet()){
			double kw = (w.orgtraffic * newEffortVector.get(w)) / (double) w.alltraffic;
			expr = cplex.sum(expr, cplex.sum(kw, cplex.prod(-kw, defenderStratVars.get(w))));
		}
		cplex.addGe(prevValue, expr);
	}
	
	public Map<Website, Double> getAttackerStrategy() {
		return attackerStrategy;
	}

	public Map<Website, Double> getAttackerEffort() {
		return attackerEffort;
	}

	public Map<Website, Double> getDefenderStrategy() {
		return defenderStrategy;
	}

	public Double getObjectiveValue() {
		return objectiveValue;
	}
	
	public Double getNumCompromised(){
		return numCompromised;
	}
	
	public boolean feasible() {
		return feasible;
	}
	
	public void deleteVars() throws IloException{
		defenderStratVars.clear();; // x
		
//		constraints
		defenderStratVars.clear();; // x
		
		attackerEffortAllocationDualVars.clear(); // gamma_w
		attackerEffortUBDualVars.clear(); // alpha_w
		attackerStratUBDualVars.clear(); // beta_w
		
//		constraints
		attackerStratDualConstraint.clear(); // y_w
		attackerEffortDualConstraint.clear(); // e_w
		
		if(cplex != null)
			cplex.end();
		//cplex.clearModel();
		
		cplex = null;
	}
}
