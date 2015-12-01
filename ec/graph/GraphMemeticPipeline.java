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


			double bestFitness = 0;
			double currentBestFitness = 0;

			do{
				bestFitness = currentBestFitness;
				currentBestFitness = findFitness(nodesToReplace, init, state, graph, subpopulation, thread);

			}while(currentBestFitness > bestFitness);

		}
		return n;
	}

	/*
	 * This returns the best new fitness of the graph after a local search
	 */
	private double findFitness(Set<Node> nodesToReplace, GraphInitializer init, EvolutionState state,
			GraphIndividual graph, int subpopulation, int thread){

		double currentFitness = 0;

		for (Node node : nodesToReplace) {
			Set<Node> neighbours = findNeighbourNodes(node, init);
			for(Node neighbour: neighbours){

				replaceNode(node, neighbour, graph, init);

				((GraphEvol)state.evaluator.p_problem).evaluate(state, graph, subpopulation, thread);
				graph.evaluated = false;

				double fitness = graph.fitness.fitness();
				if(fitness > currentFitness){
					currentFitness = fitness;
				}
			}

		}
		return currentFitness;
	}

	/*
	 * Replace the node with its neighbour in the graph.
	 */
	private void replaceNode(Node node, Node neighbour, GraphIndividual graph, GraphInitializer init){

		Set<Edge> outgoingEdges = new HashSet<Edge>();
		Set<Edge> incomingEdges = new HashSet<Edge>();

		//remove the node to be replaced
		graph.nodeMap.remove( node.getName() );
		graph.considerableNodeMap.remove( node.getName() );

		//remove edges of the replaced node
		for (Edge e : node.getIncomingEdgeList()) {
			incomingEdges.add( e );
			e.getFromNode().getOutgoingEdgeList().remove( e );
			graph.edgeList.remove( e );
			graph.considerableEdgeList.remove( e );
		}
		for (Edge e : node.getOutgoingEdgeList()) {
			outgoingEdges.add( e );
			e.getToNode().getIncomingEdgeList().remove( e );
			graph.edgeList.remove( e );
			graph.considerableEdgeList.remove( e );
		}

		//give outgoingEdges to the neighbour node
		for(Edge e: outgoingEdges){
			e.setFromNode(neighbour);
			neighbour.getOutgoingEdgeList().add(e);
			graph.edgeList.add(e);
			graph.considerableEdgeList.add(e);
		}

		//give incomingEdges to the neighbour node
		for(Edge e: incomingEdges){
			Set<String> nodeInputs = neighbour.getInputs();
			Set<String> edgeInputs = e.getIntersect();
			//check if the edge is still useful for the neighbour
			if(init.isIntersection(edgeInputs, nodeInputs)){
				e.setToNode(neighbour);
				neighbour.getIncomingEdgeList().add(e);
				graph.edgeList.add(e);
				graph.considerableEdgeList.add(e);
			}
		}

		init.removeDanglingNodes(graph);

		//add the neighbour to the graph
		graph.nodeMap.put(neighbour.getName(), neighbour);
		graph.considerableNodeMap.put(neighbour.getName(), neighbour);

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

		Set<Node> neighbour = init.getRelevantServices(inputs, outputs);

		return null;
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
