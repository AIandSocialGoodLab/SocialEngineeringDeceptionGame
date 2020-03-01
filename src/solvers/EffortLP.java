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

public class EffortLP {
	
	private SEModel model;
	private ArrayList<Map<Website, Integer>> bestEffortVectors;
	
//	solution 
	private Map<Website, Double> defenderStrategy;
	private double numCompromised;
	
	private Double objectiveValue; 
	
//	cplex variables
	private IloCplex cplex;

	
//	objective 
	private IloObjective objective;
	private IloNumVar v;
	
//	decision variables
	private Map<Website, IloNumVar> defenderStratVars; // x
	
//	constraints
//	private IloConstraint defenderBudgetConstraint;
//	private ArrayList<IloConstraint> attackerObjectiveConstraint; // 
//	private ArrayList<IloConstraint> attackerBRConstraint; // 
	private IloRange defenderBudgetConstraint;
	private ArrayList<IloRange> attackerObjectiveConstraint; // 
	private ArrayList<IloRange> attackerBRConstraint; // 
	
	private double getSolutionTime;
	private int numConstraints;
	
	

	
	public EffortLP(SEModel model, ArrayList<Map<Website, Integer>> bestEffortVectors){
		this.model = model;
		this.bestEffortVectors = bestEffortVectors;
		this.numConstraints = 0;
		
		try {
			generateLP();
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void solve() throws IloException{
		
		
//		generateLP();
//		long getsolutiontimestart = System.currentTimeMillis();
//		writeProblem("SE_effort.lp");
//		getSolutionTime = (System.currentTimeMillis()-getsolutiontimestart)/1000.0;
		cplex.setParam(IloCplex.DoubleParam.EpMrk, 0.01);
//		cplex.setParam(IloCplex.Param.TimeLimit, 100);   //comment out later
		cplex.setOut(null);
//		cplex.setParam(IloCplex.IntParam.RootAlg, 2);
		long getsolutiontimestart = System.currentTimeMillis();
		cplex.solve();
		getSolutionTime = (System.currentTimeMillis()-getsolutiontimestart)/1000.0;

		if(cplex.isPrimalFeasible())
			extractSolution();
		
		int maxSize = (int) numConstraints/4;
//		maxSize = 10;
		if(attackerBRConstraint.size()>maxSize && attackerBRConstraint.size()>500) {
			Map<Website, Integer> removed;
			IloRange[] c = new IloRange[attackerBRConstraint.size()];
			attackerBRConstraint.toArray(c);
			double[] slacks = new double[attackerBRConstraint.size()];
//			cplex.solve();
			slacks = cplex.getSlacks(c);
			int minIndex = 0;
			double minSlack = 0.0;
			for(int i=0; i<attackerBRConstraint.size(); i++) {
				if(slacks[i] < minSlack) {
					minIndex = i;
					minSlack = slacks[i];
				}
			}
			if(minSlack < 0) {
				cplex.remove(attackerBRConstraint.get(minIndex));
				attackerBRConstraint.remove(minIndex);
				removed = bestEffortVectors.remove(minIndex);
//				return removed;
			}
		}

	}
	
	public void generateLP() throws IloException {
		cplex = new IloCplex();
//		cplex.setParam(IloCplex.IntParam.RootAlg, IloCplex.Algorithm.Barrier);
		
//		cplex.setParam(IloCplex.DoubleParam.EpMrk, 0.01);
		ArrayList<Website> websites = model.getWebsites();
		
		defenderStratVars = new HashMap<Website, IloNumVar>();
		
//		attackerBRConstraint = new ArrayList<IloConstraint>();
		attackerBRConstraint = new ArrayList<IloRange>();
		v = cplex.numVar(0, Double.MAX_VALUE, IloNumVarType.Float, "V");
		
		//initialize defender variables
		for(Website w : websites){
			defenderStratVars.put(w, cplex.numVar(0, 1, IloNumVarType.Float, "X_"+w.id));
		}
		

//		defender budget constraint
		IloLinearNumExpr defenderBudgetExpr =  cplex.linearNumExpr();
		for (Website w:websites) {
			defenderBudgetExpr.addTerm(w.orgtraffic*w.costToAlter, defenderStratVars.get(w));
		}
		defenderBudgetConstraint = cplex.addLe(defenderBudgetExpr, model.getDefenderBudget());
		numConstraints++;
		
		
		//initialize objective constraints
		int index = 1;
		for (Map<Website, Integer> map : bestEffortVectors) { // rhs
			IloNumExpr expr = cplex.linearNumExpr(0.0);
			for(Website w : map.keySet()){
				double kw = (w.orgtraffic * map.get(w)) / (double) w.alltraffic;
				expr = cplex.sum(expr, cplex.sum(kw, cplex.prod(-kw, defenderStratVars.get(w))));
			}
//			attackerBRConstraint.add(cplex.addGe(v, expr, "OBJ_"+index));
			attackerBRConstraint.add((IloRange) cplex.ge(v, expr, "OBJ_"+index));
			index++;
			numConstraints++;
		}
		
		objective = cplex.addMinimize(v);
		
		IloRange[] c = new IloRange[attackerBRConstraint.size()];

		cplex.add(attackerBRConstraint.toArray(c));
		
	}
	
	public Map<Website, Integer> addEffortVector(Map<Website, Integer> newEffortVector) throws IloException {
		IloNumExpr expr = cplex.linearNumExpr(0.0);
		for(Website w : newEffortVector.keySet()){
			double kw = (w.orgtraffic * newEffortVector.get(w)) / (double) w.alltraffic;
			expr = cplex.sum(expr, cplex.sum(kw, cplex.prod(-kw, defenderStratVars.get(w))));
		}
//		attackerBRConstraint.add(cplex.addGe(v, expr, "OBJ_"+(numConstraints)));
		attackerBRConstraint.add((IloRange) cplex.addGe(v, expr, "OBJ_"+(numConstraints)));
		numConstraints++;
//		bestEffortVectors.add(newEffortVector);
//		int maxSize = (int) numConstraints/2;
////		maxSize = 10;
//		if(attackerBRConstraint.size()>maxSize && maxSize > 2) {
//			Map<Website, Integer> removed;
//			IloRange[] c = new IloRange[attackerBRConstraint.size()];
//			attackerBRConstraint.toArray(c);
//			double[] slacks = new double[attackerBRConstraint.size()];
//			cplex.solve();
//			slacks = cplex.getSlacks(c);
//			int minIndex = 0;
//			double minSlack = 0.0;
//			for(int i=0; i<attackerBRConstraint.size(); i++) {
//				if(slacks[i] < minSlack) {
//					minIndex = i;
//					minSlack = slacks[i];
//				}
//			}
//			if(minSlack < 0) {
//				cplex.remove(attackerBRConstraint.get(minIndex));
//				attackerBRConstraint.remove(minIndex);
//				removed = bestEffortVectors.remove(minIndex);
//				return removed;
//			}
//		}
		return null;
	}
	
//	public void addConstraint(Map<Website, Integer> newEffortVector, double prevValue) throws IloException {
//		IloNumExpr expr = cplex.linearNumExpr(0.0);
//		for(Website w : newEffortVector.keySet()){
//			double kw = (w.orgtraffic * newEffortVector.get(w)) / (double) w.alltraffic;
//			expr = cplex.sum(expr, cplex.sum(kw, cplex.prod(-kw, defenderStratVars.get(w))));
//		}
//		cplex.addGe(prevValue, expr);
//	}
	
	public void addConstraint(double prevValue) throws IloException {
		cplex.addGe(prevValue, v);
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
		defenderStratVars.clear();; // x
		
//		constraints
//		attackerObjectiveConstraint.clear();; // 
//		attackerBRConstraint.clear();
		
		if(cplex != null)
			cplex.end();
		//cplex.clearModel();
		
		cplex = null;
	}
}
