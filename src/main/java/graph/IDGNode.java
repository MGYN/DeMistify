package graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import backwardslicing.BackwardContext;

public interface IDGNode {

	public Set<IDGNode> getDependents();

	public int getUnsovledDependentsCount();

	public boolean hasSolved();

	public void solve();

	public boolean canBePartiallySolve();

	public List<BackwardContext> initIfHavenot();

	public boolean inited();

	public ArrayList<HashMap<Integer, HashMap<Integer, String>>> getResult();
}
