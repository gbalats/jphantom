package org.clyze.jphantom.constraints.solvers;

import org.clyze.jphantom.util.MapFactory;
import java.util.*;
import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.alg.TransitiveClosure;
import static org.jgrapht.Graphs.*;

public class MultipleInheritanceSolver<V,E> extends AbstractSolver<V,E,Map<V,List<V>>>
{
    private final boolean minimize;

    ///////////////////// Constructors /////////////////////

    public MultipleInheritanceSolver(EdgeFactory<V,E> factory, boolean minimize)
    {
        super(factory, new MapFactory<V,List<V>>());
        this.minimize = minimize;
    }

    public MultipleInheritanceSolver(DirectedGraph<V,E> graph, boolean minimize)
    {
        super(graph, new MapFactory<V,List<V>>());
        this.minimize = minimize;
    }

    public MultipleInheritanceSolver(EdgeFactory<V,E> factory) {
        this(factory, true);
    }

    public MultipleInheritanceSolver(DirectedGraph<V,E> graph) {
        this(graph, true);
    }

    ///////////////////// Methods /////////////////////

    @Override
    @SuppressWarnings("unchecked")
    public MultipleInheritanceSolver<V,E> solve() throws UnsatisfiableStateException
    {
        return (MultipleInheritanceSolver<V,E>) super.solve();
    }


    /////////////////////// Constraint Solving ///////////////////////

    @Override
    protected void solve(DirectedGraph<V,E> graph) throws UnsatisfiableStateException
    {
        // Check for cycles in the interface graph
        if (new CycleDetector<>(graph).detectCycles())
            throw new GraphCycleException();

        // Remove redundant edges
        if (minimize) {
            // Compute transitive closure
            SimpleDirectedGraph<V,E> closure = new SimpleDirectedGraph<>(factory);

            addGraph(closure, graph);
            TransitiveClosure.INSTANCE.closeSimpleDirectedGraph(closure);

            for (E e : new HashSet<>(graph.edgeSet()))
            {
                V source = graph.getEdgeSource(e);
                V target = graph.getEdgeTarget(e);

                for (V neighbor : successorListOf(graph, source))
                {
                    if (neighbor.equals(target))
                        continue;

                    if (!removableEdge(source, target))
                        continue;

                    if (closure.containsEdge(neighbor, target)) {
                        graph.removeEdge(source, target);
                        break;
                    }
                }
            }
        }

        // Create solution
        for (V v : graph.vertexSet())
            solution.put(v, successorListOf(graph, v));
    }

    protected final boolean removableEdge(E edge, DirectedGraph<V,E> graph) {
        if (!graph.containsEdge(edge))
            throw new IllegalArgumentException();
        return removableEdge(graph.getEdgeSource(edge), graph.getEdgeTarget(edge));
    }

    protected final boolean removableEdge(E edge) {
        return removableEdge(edge, _graph);
    }

    protected boolean removableEdge(V source, V target) {
        return true;
    }
}

