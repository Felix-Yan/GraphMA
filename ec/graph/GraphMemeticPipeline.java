package ec.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ec.BreedingPipeline;
import ec.EvolutionState;
import ec.Individual;
import ec.util.Parameter;

public class GraphMemeticPipeline extends BreedingPipeline {
	GraphIndividual currentGraph = new GraphIndividual();

	@Override
	public Parameter defaultBase() {
		return new Parameter("graphmemeticpipeline");
	}

	@Override
	public int numSources() {
		return 1;
	}

	@Override
	public int produce(int min, int max, int start, int subpopulation,
			Individual[] inds, EvolutionState state, int thread) {
		GraphInitializer init = (GraphInitializer) state.initializer;
		int n = sources[0].produce(min, max, start, subpopulation, inds, state, thread);
		if (!(sources[0] instanceof BreedingPipeline)) {
			for(int q=start;q<n+start;q++)
				inds[q] = (Individual)(inds[q].clone());
		}

		if (!(inds[start] instanceof GraphIndividual))
			// uh oh, wrong kind of individual
			state.output.fatal("GraphAppendPipeline didn't get a GraphIndividual. The offending individual is in subpopulation "
					+ subpopulation + " and it's:" + inds[start]);
		// Perform mutation
		for(int q=start;q<n+start;q++) {
			GraphIndividual graph = (GraphIndividual)inds[q];
			GraphSpecies species = (GraphSpecies) graph.species;
			Object[] nodes = graph.nodeMap.values().toArray();
			// Select node from which to perform mutation
			Node selected = null;
			while (selected == null) {
				Node temp = (Node) nodes[init.random.nextInt( nodes.length )];
				//Do not allow mutations for start or end node
				if (!temp.getName().equals( "end" ) && !temp.getName().equals( "start" )) {
					selected = temp;
				}
			}
			// Find all nodes that should be locally searched and possibly replaced
			Set<Node> nodesToReplace = findNodesToRemove(selected);
			/*			Map<String,Node> changingDomain = new HashMap<String,Node>();
			for(Node node: nodesToReplace){
				changingDomain.put(node.getName(), node);
			}*/
			double bestFitness = 0;
			double currentBestFitness = 0;
			currentGraph = graph;
			//System.out.println("loop starts");//debug
			do{
				bestFitness = currentBestFitness;
				currentBestFitness = findFitness(nodesToReplace, init, state, currentGraph, subpopulation, thread);
//				System.out.println("best is: "+bestFitness);//debug
//				System.out.println(currentBestFitness);//debug
			}while(currentBestFitness > bestFitness);

			inds[q] = currentGraph;
		}
		return n;
	}


	/*
	 * This returns the best new fitness of the graph after a local search
	 */
	private double findFitness(Set<Node> domain, GraphInitializer init, EvolutionState state,
			GraphIndividual graph, int subpopulation, int thread){

		double currentFitness = 0;
		GraphIndividual bestGraph = new GraphIndividual();
		Node newMember = null;//The new node added in the subgraph
		Node replaced = null;//The old node replaced by the new node in the subgraph
		//debug
//		System.out.println("nodes to replace has size"+" "+domain.size());

		for (Node node : domain) {

			Set<Node> neighbours = findNeighbourNodes(node, init);
			for(Node neighbour: neighbours){
				GraphIndividual innerGraph = new GraphIndividual();
				graph.copyTo(innerGraph);
				replaceNode(node, neighbour, innerGraph, init);
				((GraphEvol)state.evaluator.p_problem).evaluate(state, innerGraph, subpopulation, thread);
				//newGraph.evaluated = false;//I think this is not necessary now

				double fitness = innerGraph.fitness.fitness();
				if(fitness > currentFitness){
					currentFitness = fitness;
					bestGraph = innerGraph;
					replaced = node;
					newMember = neighbour;
				}
			}

		}

		if(replaced!=null){
			currentGraph = bestGraph;
			domain.remove(replaced);
			domain.add(newMember);
			//System.out.println("replaced: "+replaced.getName());
			//System.out.println("added: "+newMember.getName());
		}
		return currentFitness;
	}

	/*
	 * Replace the node with its neighbour in the graph.
	 */
	private void replaceNode(Node node, Node neighbour, GraphIndividual newGraph, GraphInitializer init){
		//do not replace end node
		if(node.getName().equals("end")) return;

		//do not replace a node by itself
		if(node.getName().equals(neighbour.getName())) return;

		//do not add the neighbour if the neighbour has already been in the graph. Do not allow duplicates
		if(newGraph.nodeMap.get(neighbour.getName()) != null) return;

		//this is to obtain the node with the name from the current graph
		Node graphNode = newGraph.nodeMap.get(node.getName());

		//debug
		if(graphNode == null){
			System.out.println("The selected node is "+node.getName());
			throw new NullPointerException("The node to be replaced does not exist in graph");
		}

		Set<Edge> outgoingEdges = new HashSet<Edge>();
		Set<Edge> incomingEdges = new HashSet<Edge>();

		//add the neighbour node to the graph
		newGraph.nodeMap.put(neighbour.getName(), neighbour);
		newGraph.considerableNodeMap.put(neighbour.getName(), neighbour);

		//remove incoming edges of the replaced node
		for (Edge e : graphNode.getIncomingEdgeList()) {
			Edge newEdge = e.cloneEdge(newGraph.nodeMap);
			incomingEdges.add( newEdge );
			e.getFromNode().getOutgoingEdgeList().remove( e );
			newGraph.edgeList.remove( e );
			newGraph.considerableEdgeList.remove( e );
		}

		//remove outgoingEdges to the neighbour node
		for (Edge e : graphNode.getOutgoingEdgeList()) {
			Edge newEdge = e.cloneEdge(newGraph.nodeMap);
			outgoingEdges.add( newEdge );
			e.getToNode().getIncomingEdgeList().remove( e );
			newGraph.edgeList.remove( e );
			newGraph.considerableEdgeList.remove( e );
		}

		neighbour.getOutgoingEdgeList().clear();//this removes all other unnecessary inherited edges
		neighbour.getIncomingEdgeList().clear();

		//give outgoingEdges to the neighbour node
		for(Edge e: outgoingEdges){
			//e.setFromNode(newGraph.nodeMap.get(neighbour.getName()));
			e.setFromNode(neighbour);
			neighbour.getOutgoingEdgeList().add(e);
			e.getToNode().getIncomingEdgeList().add(e);
			newGraph.edgeList.add(e);
			newGraph.considerableEdgeList.add(e);
		}

		//give incomingEdges to the neighbour node
		for(Edge e: incomingEdges){
			Set<String> nodeInputs = neighbour.getInputs();
			Set<String> edgeInputs = e.getIntersect();
			//check if the edge is still useful for the neighbour
			if(init.isIntersection(edgeInputs, nodeInputs)){
				e.setToNode(newGraph.nodeMap.get(neighbour.getName()));
				neighbour.getIncomingEdgeList().add(e);
				e.getFromNode().getOutgoingEdgeList().add( e );
				newGraph.edgeList.add(e);
				newGraph.considerableEdgeList.add(e);
			}
		}
		init.removeDanglingNodes(newGraph);
		//remove the node to be replaced
		newGraph.nodeMap.remove( graphNode.getName() );
		newGraph.considerableNodeMap.remove( graphNode.getName() );

	}

	/*
	 * This finds all the neighbouring nodes of the selected node. The neighbours can substitute the selected node without
	 * losing any functionality.
	 */
	private Set<Node> findNeighbourNodes(Node selected, GraphInitializer init){
		List <Edge> outgoingEdge = selected.getOutgoingEdgeList();

		//use the selected node inputs as the possible neighbour inputs
		Set<String> inputs = selected.getInputs();
		Set<String> outputs = new HashSet<String>();

		//use all the outputs in the outgoing edges as the required neighbour outputs
		for(Edge e: outgoingEdge){
			outputs.addAll(e.getIntersect());
		}

		Set<Node> nodeWithOutput = new HashSet<Node>();
		//The following finds out all the nodes that satisfy all the outputs
		for(String output: outputs){
			if(nodeWithOutput.isEmpty()){
				nodeWithOutput = new HashSet<Node>(init.taxonomyMap.get(output).servicesWithOutput);
			}
			else{
				Set<Node> nodeWithOutput2 = new HashSet<Node>(init.taxonomyMap.get(output).servicesWithOutput);
				nodeWithOutput = findIntersection(nodeWithOutput, nodeWithOutput2);
			}
		}

		Set<Node> neighbours = new HashSet<Node>();
		neighbours.addAll(nodeWithOutput);
		//This checks that all the neighbours can be satisfied by the given inputs
		for(Node node: nodeWithOutput){
			Set<String> nodeInput = node.getInputs();
			if(!isSubset(nodeInput, inputs)){
				neighbours.remove(node);
			}
		}

		return neighbours;
	}

	/*
	 * The following checks that the given set1 is a subset of set2.
	 */
	private boolean isSubset(Set<String> set1, Set<String> set2){
		for(String s: set1){
			if(!set2.contains(s)){
				return false;
			}
		}
		return true;
	}

	/*
	 * This gives the intersection of two set of nodes
	 */
	private Set<Node> findIntersection(Set<Node> set1, Set<Node> set2){
		Set<Node> intersection = new HashSet<Node>();
		for(Node n: set1){
			if(set2.contains(n)){
				intersection.add(n);
			}
		}
		return intersection;
	}

	private Set<Node> findNodesToRemove(Node selected) {
		Set<Node> nodes = new HashSet<Node>();
		_findNodesToRemove(selected, nodes);
		return nodes;

	}

	private void _findNodesToRemove(Node current, Set<Node> nodes) {
		nodes.add( current );
		for (Edge e: current.getOutgoingEdgeList()) {
			_findNodesToRemove(e.getToNode(), nodes);
		}
	}
}
