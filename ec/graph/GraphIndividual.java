package ec.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ec.Individual;
import ec.simple.SimpleFitness;
import ec.util.Parameter;

/**
 * A GraphIndividual is a DAG solution to the web servcice composition problem.
 * @author yanlong
 *
 */
public class GraphIndividual extends Individual {
	int count = 0;//debug

	/* nodeMap's values contains all the nodes in the graph.
	 * It is constructed in GraphSpecies by connectCandidateToGraphByInputs.
	 */
	public Map<String, Node> nodeMap = new HashMap<String, Node>();
	public Map<String, Node> considerableNodeMap= new HashMap<String, Node>();
	public List<Edge> edgeList = new ArrayList<Edge>();
	public List<Edge> considerableEdgeList = new ArrayList<Edge>();
	public Set<Node> unused;
	public int longestPathLength;
	public int numAtomicServices;

	public GraphIndividual(){
		super();
		super.fitness = new SimpleFitness();
		super.species = new GraphSpecies();
	}

	public GraphIndividual(Set<Node> unused) {
		super();
		super.fitness = new SimpleFitness();
		super.species = new GraphSpecies();
		this.unused = unused;
	}

	@Override
	public Parameter defaultBase() {
		return new Parameter("graphindividual");
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof GraphIndividual) {
			return toString().equals(other.toString());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	/**
	 * A graphic representation of this candidate can be generated by saving this description to a .dot file and
	 * running the command "dot -Tpng filename.dot -o filename.png"
	 */
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("digraph g {");
		for(Edge e: edgeList) {
			builder.append(e);
			builder.append("; ");
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Copies this graph structure to another GraphIndividual object.
	 *
	 * @param other
	 */
    public void copyTo(GraphIndividual other) {
    	other.nodeMap.clear();
    	other.considerableNodeMap.clear();
    	other.edgeList.clear();
    	other.considerableEdgeList.clear();
        for (Node n : nodeMap.values()) {
            Node newN = n.clone();
            other.nodeMap.put( newN.getName(), newN );
            other.considerableNodeMap.put( newN.getName(), newN );
        }
        for (Edge e: edgeList) {
        	Edge newE = e.cloneEdge(other.nodeMap);
            other.edgeList.add(newE);
            other.considerableEdgeList.add( newE );
            newE.getFromNode().getOutgoingEdgeList().add( newE );
            newE.getToNode().getIncomingEdgeList().add( newE );
        }
    }

    /**
     * This checks all the nodes in the edgeList are also in the nodemap
     * @return
     */
    public boolean validation(){
    	for (Edge e : edgeList) {
			Node fromNode = nodeMap.get(e.getFromNode().getName());
			if(fromNode == null){
				return false;
			}
			if(fromNode != e.getFromNode()){
				return false;
			}
			Node toNode = nodeMap.get(e.getToNode().getName());
			if(toNode == null){
				return false;
			}
			if(toNode != e.getToNode()){
				return false;
			}
			for (Node g : nodeMap.values()) {
				if (!g.getName().equals("end") && g.getOutgoingEdgeList().isEmpty())
					return false;
			}

		}
    	return true;
    }
}
