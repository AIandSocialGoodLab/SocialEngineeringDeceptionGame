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

public class simple2LP {
	
	private SEModel model;
	
//	solution 
	private Map<Website, Double> defenderStrategy;
	private double numCompromised;
	
	private Double objectiveValue; 
	
//	cplex variables
	private IloCplex cplex;

	
//	objective 
	private IloObjective objective;
//	private IloNumVar v;
	
//	decision variables
	private Map<Website, IloNumVar> defenderStratVars; // x
	private Map<Website, IloNumVar> dPlusVars; // d^+
	private IloNumVar zVar; // z
	
//	constraints
	private IloRange defenderBudgetConstraint;
	private ArrayList<IloRange> mainConstraints; // 
//	private ArrayList<IloRange> attackerBRConstraint; // 
	
	private double getSolutionTime;
	private int numConstraints;
	
	

	
	public simple2LP(SEModel model){
		this.model = model;
		this.numConstraints = 0;
		
		try {
			generateLP();
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void solve() throws IloException{
//		cplex.setParam(IloCplex.DoubleParam.EpMrk, 0.01);
		cplex.setOut(null);
		long getsolutiontimestart = System.currentTimeMillis();
		cplex.solve();
		getSolutionTime = (System.currentTimeMillis()-getsolutiontimestart)/1000.0;

		if(cplex.isPrimalFeasible()) {
			extractSolution();
		} else {
			System.out.println("Not Feasible");
		}
	}
	
	public void generateLP() throws IloException {
		cplex = new IloCplex();
		ArrayList<Website> websites = model.getWebsites();
		
		defenderStratVars = new HashMap<Website, IloNumVar>();	
		dPlusVars = new HashMap<Website, IloNumVar>();	
		mainConstraints = new ArrayList<IloRange>();
//		v = cplex.numVar(0, Double.MAX_VALUE, IloNumVarType.Float, "V");
		zVar = cplex.numVar(-Double.MAX_VALUE, Double.MAX_VALUE, IloNumVarType.Float, "Z");
		//initialize defender variables and dplus variables
		for(Website w : websites){
			defenderStratVars.put(w, cplex.numVar(0, 1, IloNumVarType.Float, "X_"+w.id));
			dPlusVars.put(w, cplex.numVar(0, Double.MAX_VALUE, IloNumVarType.Float, "d_"+w.id));
		}
		

//		defender budget constraint
		IloLinearNumExpr defenderBudgetExpr =  cplex.linearNumExpr();
		for (Website w:websites) {
			defenderBudgetExpr.addTerm(w.orgtraffic*w.costToAlter, defenderStratVars.get(w));
		}
		defenderBudgetConstraint = cplex.addLe(defenderBudgetExpr, model.getDefenderBudget());
		numConstraints++;
		
		IloLinearNumExpr expr;
		// main constraints
		for (Website w:websites) {
			expr = cplex.linearNumExpr();
//			double a = 3.0;
			expr.addTerm(1.0, dPlusVars.get(w));
			expr.addTerm(w.orgtraffic, defenderStratVars.get(w));
			expr.addTerm(1.0, zVar);
			cplex.addGe(expr, w.orgtraffic, "ss_"+numConstraints);
//			expr = cplex.sum(zVar, a);
//			expr = cplex.sum(cplex.sum(w.orgtraffic, cplex.prod(-w.orgtraffic, defenderStratVars.get(w))), zVar);
//			expr = cplex.diff(expr, dPlusVars.get(w));
//			mainConstraints.add((IloRange) cplex.ge(0.0, expr));
//			cplex.addLe(expr, 0.0, "ss_"+numConstraints);
			numConstraints++;
		}
		
		// objective function
		IloNumExpr objexpr = cplex.linearNumExpr(0.0);
		for (Website w:websites) {
			objexpr = cplex.sum(objexpr, dPlusVars.get(w));
		}
		objexpr = cplex.sum(objexpr, cplex.prod(model.getAttackBudget(), zVar));

		objective = cplex.addMinimize(objexpr);
		
//		IloRange[] c = new IloRange[mainConstraints.size()];
//
//		cplex.add(mainConstraints.toArray(c));
		
	}
	
	
	private void extractSolution() throws IloException {
		objectiveValue = cplex.getObjValue();
		defenderStrategy = new HashMap<Website, Double>();
		for (Website w : model.getWebsites()) {
			defenderStrategy.put(w, cplex.getValue(defenderStratVars.get(w)));			
		}
	}
	

	public Map<Website, Double> getDefenderStrategy() {
		return defenderStrategy;
	}

	public Double getObjectiveValue() {
		return objectiveValue;
	}
	
	public double getGetSolutionTime() {
		return getSolutionTime;
	}
	
	public void writeProblem(String filename) throws IloException{
		cplex.exportModel(filename);
	}
	
	public void writeSolution(String filename) throws IloException{
		cplex.writeSolution(filename);
	}

	
	public void deleteVars() throws IloException{
		defenderStratVars.clear(); // x
		dPlusVars.clear(); // d^+
		
		if(cplex != null)
			cplex.end();
		
		cplex = null;
	}
}
