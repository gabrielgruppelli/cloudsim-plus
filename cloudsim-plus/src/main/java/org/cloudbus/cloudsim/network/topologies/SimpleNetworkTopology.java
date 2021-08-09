package org.cloudbus.cloudsim.network.topologies;

import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.network.DelayMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class SimpleNetworkTopology implements NetworkTopology {
    private static final Logger LOGGER = LoggerFactory.getLogger(BriteNetworkTopology.class.getSimpleName());

    /**
     * The id to use for the next node to be created in the network.
     */
    private int nextIdx;

    private boolean networkEnabled;

    /**
     * A matrix containing the delay between every pair of nodes in the network.
     */
    private DelayMatrix delayMatrix;

    private double[][] bwMatrix;

    /**
     * @see #getTopologicalGraph()
     */
    private TopologicalGraph graph;

    /**
     * Each key is a CloudSim entity and each value the corresponding entity ID.
     */
    private Map<SimEntity, Integer> entitiesMap;


    public SimpleNetworkTopology() {
        entitiesMap = new HashMap<>();
        bwMatrix = new double[0][0];
        graph = new TopologicalGraph();
        delayMatrix = new DelayMatrix();
    }

    /**
     * Generates the matrices used internally to set latency and bandwidth
     * between elements.
     */
    private void generateMatrices() {
        // creates the delay matrix
        delayMatrix = new DelayMatrix(getTopologicalGraph(), false);

        // creates the bw matrix
        bwMatrix = createBwMatrix(getTopologicalGraph(), false);

        networkEnabled = true;
    }

    /**
     * Creates the matrix containing the available bandwidth between every pair
     * of nodes.
     *
     * @param graph topological graph describing the topology
     * @param directed true if the graph is directed; false otherwise
     * @return the bandwidth graph
     */
    private double[][] createBwMatrix(final TopologicalGraph graph, final boolean directed) {
        final int nodes = graph.getNumberOfNodes();

        final double[][] mtx = new double[nodes][nodes];

        // cleanup matrix
        for (int i = 0; i < nodes; i++) {
            for (int j = 0; j < nodes; j++) {
                mtx[i][j] = 0.0;
            }
        }

        for (final TopologicalLink edge : graph.getLinksList()) {
            mtx[edge.getSrcNodeID()][edge.getDestNodeID()] = edge.getLinkBw();
            if (!directed) {
                mtx[edge.getDestNodeID()][edge.getSrcNodeID()] = edge.getLinkBw();
            }
        }

        return mtx;
    }

    @Override
    public void addLink(final SimEntity src, final SimEntity dest, final double bandwidth, final double latency) {
        if (graph == null) {
            graph = new TopologicalGraph();
        }

        if (entitiesMap == null) {
            entitiesMap = new HashMap<>();
        }

        // maybe add the nodes
        addNodeMapping(src);
        addNodeMapping(dest);

        // generate a new link
        graph.addLink(new TopologicalLink(entitiesMap.get(src), entitiesMap.get(dest), (float) latency, (float) bandwidth));

        generateMatrices();
    }

    @Override
    public void removeLink(SimEntity src, SimEntity dest) {
        throw new UnsupportedOperationException("Removing links is not yet supported on BriteNetworkTopologies");
    }

    private void addNodeMapping(final SimEntity entity) {
        if (entitiesMap.putIfAbsent(entity, nextIdx) == null) {
            graph.addNode(new TopologicalNode(nextIdx));
            nextIdx++;
        }
    }

    @Override
    public double getDelay(final SimEntity src, final SimEntity dest) {
        if (!networkEnabled) {
            return 0.0;
        }

        try {
            return delayMatrix.getDelay(entitiesMap.getOrDefault(src, -1), entitiesMap.getOrDefault(dest, -1));
        } catch (ArrayIndexOutOfBoundsException e) {
            return 0.0;
        }
    }

    /**
     * Checks if the network simulation is working.
     */
    public boolean isNetworkEnabled() {
        return networkEnabled;
    }

    /**
     * Gets the Topological Graph of the network.
     * @return
     */
    public TopologicalGraph getTopologicalGraph() {
        return graph;
    }

    /**
     * Gets a<b>copy</b> of the matrix containing the bandwidth between every pair of nodes in the
     * network.
     */
    public double[][] getBwMatrix() {
        return Arrays.copyOf(bwMatrix, bwMatrix.length);
    }
}
