package solvers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Models.SEModel;
import Models.Website;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

public class DefenderLP {
	
	private SEModel model;

	private Map<Website, Double> attackerEffort;
	
	private IloCplex cplex;
	
	private Map<Website, IloNumVar> xMap;
	
	private List<IloRange> constraints;
	
	private Map<Website, Double> defenderStrategy;
	
	private double expectedCompromised;
	private Map<Website, Double> fixedStrategy;
	
	private static final int MM = 100000;
	
	public DefenderLP(SEModel model, Map<Website, Double> attackerEffort){
		this.model = model;
		this.attackerEffort = attackerEffort;
		fixedStrategy = null;
	}
	
	public DefenderLP(SEModel model, Map<Website, Double> attackerEffort, Map<Website, Double> strategy){
		this.model = model;
		this.attackerEffort = attackerEffort;
		fixedStrategy = strategy;
	}
	
	public void solve() throws IloException{
		//Need to create LP and solve
		defenderStrategy = new HashMap<Website, Double>();
		
		loadProblem();
		
		cplex.solve();
		
		if(!cplex.isPrimalFeasible()){
//			writeProblem("Infeasible.txt");
			System.out.println("Infeasible.");
		}
		
//		writeProblem("SED_defender.lp");
		
		expectedCompromised = cplex.getObjValue();
		
		for(Website w : model.getWebsites()){
			defenderStrategy.put(w, cplex.getValue(xMap.get(w)));
		}
		
	}
	
	public Map<Website, Double> getDefenderStrategy() {
		return defenderStrategy;
	}

	private void loadProblem() throws IloException{
		xMap = new HashMap<Website, IloNumVar>();
		
		
		cplex = new IloCplex();
		cplex.setName("SEDeception");
		
		cplex.setOut(null);
		
		initVars();
		initConstraints();
		initObjective();
		
	}
	
	private void initVars() throws IloException{
		List<IloNumVar> varList = new ArrayList<IloNumVar>();
		
		//Create variables for attacker's effort
		for (Website w : model.getWebsites()) {
			IloNumVar var = cplex.numVar(0, 1, IloNumVarType.Float, "x_w" + w.id);

			xMap.put(w, var);
			varList.add(var);
		}

		IloNumVar[] v = new IloNumVar[varList.size()];

		cplex.add(varList.toArray(v));
		
	}
	
	private void initConstraints() throws IloException{
		constraints = new ArrayList<IloRange>();
		
		//Set attacker budget constraints
		setDefenderBudgetConstraints();
		//Set fixed strategy constraints
		if(fixedStrategy != null)
			setFixedStrategyConstraints();

		IloRange[] c = new IloRange[constraints.size()];

		cplex.add(constraints.toArray(c));
		
	}

	private void initObjective() throws IloException{
		IloNumExpr objective = cplex.constant(0.0);
		
		for(Website w : model.getWebsites()){
			double denom = 1.0/w.alltraffic;
			objective = cplex.sum(objective, cplex.prod(cplex.prod(w.orgtraffic, cplex.sum(attackerEffort.get(w), cplex.prod(-1.0, cplex.prod(xMap.get(w), attackerEffort.get(w))))), denom));
		}
		
		cplex.addMinimize(objective);
	}
	
	private void setDefenderBudgetConstraints() throws IloException {
		IloNumExpr expr = cplex.constant(0.0);
		
		for(Website w : model.getWebsites()){
			expr = cplex.sum(expr, cplex.prod(w.orgtraffic*w.costToAlter, xMap.get(w)));
		}
		
		constraints.add(cplex.le(expr, model.getDefenderBudget(), "DEF_BUDGET_CONST"));
	}
	
	private void setFixedStrategyConstraints() throws IloException{
		for(Website w : model.getWebsites()){
			IloNumExpr expr = xMap.get(w);
			
			constraints.add(cplex.ge(expr, fixedStrategy.get(w), "DEF_STRAT_CONST"));
		}
	}
	
	public double NumberCompromised(){
		return expectedCompromised;
	}
	
	public void cleanUp() throws IloException{
		xMap.clear();
		
		constraints.clear();
		
		if(cplex != null)
			cplex.end();
		//cplex.clearModel();
		
		cplex = null;
	}
	
	public void writeProblem(String filename) throws IloException{
		cplex.exportModel(filename);
	}
	
	public void writeSolution(String filename) throws IloException{
		cplex.writeSolution(filename);
	}

}
