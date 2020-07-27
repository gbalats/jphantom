package org.clyze.jphantom.util;

import java.util.*;
import org.jgrapht.Graphs;
import org.jgrapht.DirectedGraph;

public class GraphUtils
{
    private GraphUtils() {
        throw new AssertionError();
    }

    public static <G extends DirectedGraph<V,E>,V,E> G getSubgraph(
        G baseGraph, 
        Factory<G> factory,
        Set<? extends V> vertexSubset, 
        Set<? extends E> edgeSubset) 
    {
        G subgraph = factory.create();

        // Argument check

        if (vertexSubset == null)
            vertexSubset = baseGraph.vertexSet();

        for (V vertex : vertexSubset)
            if (!baseGraph.containsVertex(vertex))
                throw new IllegalArgumentException();

        if (edgeSubset == null) {
            Set<E> tmp = new HashSet<>();

            for (V vertex : vertexSubset)
                tmp.addAll(baseGraph.edgesOf(vertex));

            edgeSubset = tmp;
        }

        for (E edge : edgeSubset)
            if (!baseGraph.containsEdge(edge) || 
                !vertexSubset.contains(baseGraph.getEdgeSource(edge)) ||
                !vertexSubset.contains(baseGraph.getEdgeTarget(edge)))
                throw new IllegalArgumentException();

        Graphs.addAllVertices(subgraph, vertexSubset);
        Graphs.addAllEdges(subgraph, baseGraph, edgeSubset);

        return subgraph;
    }    
}
