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

public class AttackerMILP {
	
	private SEModel model;
	private Map<Website, Double> defenderStrategy;
	
	private IloCplex cplex;
	
	private Map<Website, IloNumVar> yMap;
	private Map<Website, IloNumVar> eMap;
	
	private List<IloRange> constraints;
	
	private Map<Website, Integer> attackerStrategy;
	private Map<Website, Double> attackerEffort;

	private double expectedCompromised;
	
	private static final int MM = 100000;
	
	private IloNumExpr objective;
	private double greedyTime;
	private boolean feasible;
	
	public AttackerMILP(SEModel model, Map<Website, Double> defenderStrategy) throws IloException{
		this.model = model;
		this.defenderStrategy = defenderStrategy;
		this.greedyTime = 0.0;
		this.feasible = true;
		this.expectedCompromised = 0.0;
		//Need to create LP and solve
		attackerStrategy = new HashMap<Website, Integer>();
		attackerEffort = new HashMap<Website, Double>();
		
		loadProblem();
	}
	
	public void solve() throws IloException{

		cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, 0.00001);
		try {
			cplex.solve();
		} catch (IloException e) {
			// TODO Auto-generated catch block
			feasible = false;
			System.out.println("Infeasible.");
		}
		
		if(!cplex.isPrimalFeasible()){
//			writeProblem("Infeasible.txt");
			feasible = false;
			System.out.println("Infeasible.");
			return;
		}
		
//		writeProblem("SED.lp");
		
		expectedCompromised = cplex.getObjValue();
		
		for(Website w : model.getWebsites()){
			attackerStrategy.put(w, (int) cplex.getValue(yMap.get(w)));
			attackerEffort.put(w, cplex.getValue(eMap.get(w)));
		}
		
	}
	
	public Map<Website, Integer> getAttackerStrategy() {
		return attackerStrategy;
	}

	public Map<Website, Double> getAttackerEffort() {
		return attackerEffort;
	}

	private void loadProblem() throws IloException{
		yMap = new HashMap<Website, IloNumVar>();
		eMap = new HashMap<Website, IloNumVar>();
		
		
		cplex = new IloCplex();
		cplex.setName("SEDeception");
		
		cplex.setOut(null);
		
		initVars();
		initObjective();
		initConstraints();

		
	}
	
	private void initVars() throws IloException{
		List<IloNumVar> varList = new ArrayList<IloNumVar>();
		
		//Create variables for attacker's strategy
		for (Website w : model.getWebsites()) {
			IloNumVar var = cplex.numVar(0, 1, IloNumVarType.Int, "y_w" + w.id);

			yMap.put(w, var);
			varList.add(var);
		}
		
		//Create variables for attacker's effort
		for (Website w : model.getWebsites()) {
			IloNumVar var = cplex.numVar(0, w.alltraffic, IloNumVarType.Float, "e_w" + w.id);

			eMap.put(w, var);
			varList.add(var);
		}

		IloNumVar[] v = new IloNumVar[varList.size()];

		cplex.add(varList.toArray(v));
		
	}
	
	private void initConstraints() throws IloException{
		constraints = new ArrayList<IloRange>();
		
		//Set attacker budget constraints
		setAttackerBudgetConstraints();
		//Set defender effort constraints
		setAttackerEffortConstraints();
		//Set website effort constraints
		setWebsiteEffortConstraints();
		double greedyStart = System.currentTimeMillis();
//		setGreedyValueConstraints();
		greedyTime = (System.currentTimeMillis()-greedyStart)/1000.0;
		

		IloRange[] c = new IloRange[constraints.size()];

		cplex.add(constraints.toArray(c));
		
	}

	private void setGreedyValueConstraints() throws IloException {
		for(int i=0; i < 6; i++) {
			if(i==0 || i==1 || i==5) {
				AttackerGreedy greedy = new AttackerGreedy(model, defenderStrategy, false, i);
				greedy.solve();
				constraints.add(cplex.ge(objective, greedy.getValue(), "ATT_VALUE_GREEDY"+i));
			}
		}
	}
	
	
	public void addngConstraints(Map<Website, Double> prevAttackerEffort) throws IloException {
		IloNumExpr expr = cplex.constant(0.0);
		
		for(Website w : model.getWebsites()){
			if(prevAttackerEffort.get(w) == w.alltraffic) {
				expr = cplex.sum(expr, cplex.diff(w.alltraffic, eMap.get(w)));
			}
			else if(prevAttackerEffort.get(w) == 0) {
				expr = cplex.sum(expr, eMap.get(w));
			}
			else {
//				System.out.println("WRONG SOLUTION VALUE~!~!!!!");
			}
		}
		
		constraints.add(cplex.addGe(expr, 1.0, "ng"));
	}

	private void initObjective() throws IloException{
		objective = cplex.constant(0.0);
		
		for(Website w : model.getWebsites()){
			double denom = 1.0/w.alltraffic;
			objective = cplex.sum(objective, cplex.prod(cplex.prod(w.orgtraffic, cplex.sum(eMap.get(w), cplex.prod(-1.0, cplex.prod(defenderStrategy.get(w), eMap.get(w))))), denom));
		}
		
		cplex.addMaximize(objective);
	}
	
	private void setAttackerBudgetConstraints() throws IloException {
		IloNumExpr expr = cplex.constant(0.0);
		
		for(Website w : model.getWebsites()){
			expr = cplex.sum(expr, cplex.prod(w.costToAttack, yMap.get(w)));
		}
		
		constraints.add(cplex.le(expr, model.getAttackBudget(), "ATT_BUDGET_CONST"));
	}

	private void setAttackerEffortConstraints() throws IloException {
		IloNumExpr expr = cplex.constant(0.0);
		
		for(Website w : model.getWebsites()){
			expr = cplex.sum(expr, eMap.get(w));
		}
		
		constraints.add(cplex.le(expr, model.getAttackEffort(), "ATT_EFFORT_CONST"));
	}
	
	private void setWebsiteEffortConstraints() throws IloException {
		for(Website w : model.getWebsites()){
			IloNumExpr expr = eMap.get(w);
			
			expr = cplex.sum(expr, cplex.prod(-w.alltraffic, yMap.get(w)));

			constraints.add(cplex.le(expr, 0, "WEBSITE_EFFORT_W"+w.id));
		}		
	}
	
	public double NumberCompromised(){
		return expectedCompromised;
	}
	
	public double getGreedyTime(){
		return greedyTime;
	}
	
	public boolean getFeasible(){
		return feasible;
	}
	
	public void cleanUp() throws IloException{
		yMap.clear();
		eMap.clear();
		
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
