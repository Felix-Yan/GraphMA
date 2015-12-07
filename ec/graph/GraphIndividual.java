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
    	System.out.println("==================");//debug
        for (Node n : nodeMap.values()) {
            Node newN = n.clone();
            other.nodeMap.put( newN.getName(), newN );
            other.considerableNodeMap.put( newN.getName(), newN );
        }
        //may not be useful later
       /* for (Edge e: edgeList) {
            Edge newE = new Edge(e.getIntersect());
            other.edgeList.add(newE);
            other.considerableEdgeList.add( newE );
            Node newFromNode = other.nodeMap.get( e.getFromNode().getName() );
            newE.setFromNode( newFromNode );
            newFromNode.getOutgoingEdgeList().add( newE );
            Node newToNode = other.nodeMap.get( e.getToNode().getName() );
            //debug
            if(!other.nodeMap.containsKey(e.getToNode().getName())){
            	System.out.println("The node is not in the other graph");
            }
            newE.setToNode( newToNode );
            newToNode.getIncomingEdgeList().add( newE );
        }*/
        for (Edge e: edgeList) {
        	Edge newE = e.cloneEdge(other.nodeMap);
            other.edgeList.add(newE);
            other.considerableEdgeList.add( newE );
            //Node newFromNode = other.nodeMap.get( newE.getFromNode().getName() );
            newE.getFromNode().getOutgoingEdgeList().add( newE );
           // Node newToNode = other.nodeMap.get( newE.getToNode().getName() );
            //debug
            if(!other.nodeMap.containsKey(e.getToNode().getName())){
            	System.out.println("The node is not in the other graph");
            }
            newE.getToNode().getIncomingEdgeList().add( newE );
        }
        System.out.println("dummy");
    }

    /**
     * This clones the GraphIndividual to the given argument.
     * @param other
     */
   /* public void clone(GraphIndividual other){
    	for (Node n : nodeMap.values()) {
            Node newN = n.clone();
            other.nodeMap.put( newN.getName(), newN );
        }
    	for (Node n : considerableNodeMap.values()) {
            Node newN = n.clone();
            other.considerableNodeMap.put( newN.getName(), newN );
        }
    	 for (Edge e: edgeList) {
             Edge newE = new Edge(e.getIntersect());
             other.edgeList.add(newE);
             Node newFromNode = other.nodeMap.get( e.getFromNode().getName() );
             newE.setFromNode( newFromNode );
             newFromNode.getOutgoingEdgeList().add( newE );
             Node newToNode = other.nodeMap.get( e.getToNode().getName() );
             newE.setToNode( newToNode );
             newToNode.getIncomingEdgeList().add( newE );
         }
    	 for (Edge e: considerableEdgeList) {
             Edge newE = new Edge(e.getIntersect());
             other.considerableEdgeList.add(newE);
             Node newFromNode = other.considerableNodeMap.get( e.getFromNode().getName() );
             newE.setFromNode( newFromNode );
             newFromNode.getOutgoingEdgeList().add( newE );
             Node newToNode = other.considerableNodeMap.get( e.getToNode().getName() );
             newE.setToNode( newToNode );
             newToNode.getIncomingEdgeList().add( newE );
         }
    }*/
}
