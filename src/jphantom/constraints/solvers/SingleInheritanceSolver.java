package jphantom.constraints.solvers;

import util.MapFactory;
import java.util.*;
import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.alg.ConnectivityInspector;

public class SingleInheritanceSolver<V,E> extends AbstractSolver<V,E,Map<V,V>>
{
    protected final V root;

    ///////////////////// Constructors /////////////////////

    public SingleInheritanceSolver(EdgeFactory<V,E> factory, V root) {
        super(factory, new MapFactory<V,V>());
        this.root = root;
        this.graph.addVertex(root);
    }

    public SingleInheritanceSolver(DirectedGraph<V,E> graph, V root) {
        super(graph, new MapFactory<V,V>());
        this.root = root;
        this.graph.addVertex(root);
        
        for (V v : this.graph.vertexSet())
            if (!v.equals(root))
                this.graph.addEdge(v, root);
    }

    ///////////////////// Methods /////////////////////

    @Override
    public void addConstraintEdge(V source, V target)
    {
        super.addConstraintEdge(source, target);
        graph.addEdge(source, root);
        graph.addEdge(target, root);        
    }
    
    private DirectedGraph<V,E> getComponent(DirectedGraph<V,E> graph, V node)
    {
        UndirectedGraph<V,E> undirectedView = new AsUndirectedGraph<V,E>(graph);

        Set<V> nodes = new ConnectivityInspector<V,E>(undirectedView).connectedSetOf(node);
        DirectedGraph<V,E> subgraph = new DirectedSubgraph<V,E>(graph, nodes, null);
        DirectedGraph<V,E> result = new SimpleDirectedGraph<V,E>(graph.getEdgeFactory());
        Graphs.addGraph(result, subgraph);

        return result;
    }

    private void placeUnder(V top, DirectedGraph<V,E> graph)
        throws GraphCycleException
    {
        // Remove vertex and remaining incoming edges
        graph.removeVertex(top);

        // Compute unconstrained nodes

        final NavigableSet<V> unconstrained = newSet(top);

        for (V vertex : graph.vertexSet())
            if (graph.outDegreeOf(vertex) == 0)
                unconstrained.add(vertex);

        while (!unconstrained.isEmpty())
        {
            // Remove an unconstrained node
            V next = unconstrained.pollFirst();

            // Skip if next was visited in another component
            // of one of its neighbors
            if (!graph.containsVertex(next))
                continue;

            // Add subtype edge
            assert !solution.containsKey(next);
            solution.put(next, top);

            DirectedGraph<V,E> subgraph = getComponent(graph, next);

            // Remove subgraph from constraint graph

            graph.removeAllEdges(subgraph.edgeSet());

            for (V vertex : subgraph.vertexSet()) {
                assert graph.edgesOf(vertex).isEmpty();
                graph.removeVertex(vertex);
            }

            // Recursion
            placeUnder(next, subgraph);
        }

        // Sanity check
        if (!graph.edgeSet().isEmpty())
            throw new GraphCycleException();
    }

    @Override
    @SuppressWarnings("unchecked")
    public SingleInheritanceSolver<V,E> solve() throws UnsatisfiableStateException
    {
        return (SingleInheritanceSolver<V,E>) super.solve();
    }

    @Override
    protected void solve(DirectedGraph<V,E> graph) throws UnsatisfiableStateException
    {
        placeUnder(root, graph);
        assert graph.vertexSet().isEmpty();
    }

    protected NavigableSet<V> newSet(V prev) {
        return new TreeSet<V>();
    }
}
