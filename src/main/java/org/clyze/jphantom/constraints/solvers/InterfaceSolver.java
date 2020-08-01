package org.clyze.jphantom.constraints.solvers;

import org.clyze.jphantom.util.Factory;
import java.util.*;
import org.jgrapht.*;
import org.jgrapht.graph.*;

public abstract class InterfaceSolver<V,E,S> extends AbstractSolver<V,E,S>
{
    private final Strategy<V,E> strategy;
    private final V root;
    protected final boolean minimize;
    protected SingleInheritanceSolver<V,E> classSolver;
    protected MultipleInheritanceSolver<V,E> ifaceSolver;
    private Set<V> classes;

    protected InterfaceSolver(Builder<V,E,S> builder)
    {
        super(builder.graph, builder.factory);
        this.minimize = builder.minimize;
        this.strategy = builder.strategy;
        this.root = builder.root;
    }

    ////////////////// Builder ///////////////////

    public static abstract class Builder<V,E,S> {
        private boolean minimize = true;
        private Strategy<V,E> strategy;
        private final DirectedGraph<V,E> graph;
        private final Factory<S> factory;
        private final V root;

        public Builder(V root, Factory<S> factory, DirectedGraph<V,E> graph)
        {
            this.root = root;
            this.strategy = new MinClassesStrategy<>(root);
            this.factory = factory;
            this.graph = graph;
        }

        public Builder(V root, Factory<S> factory, EdgeFactory<V,E> efactory)
        {
            this(root, factory, new SimpleDirectedGraph<>(efactory));
        }

        public Builder<V,E,S> minimize(boolean min) {
            this.minimize = min;
            return this;
        }

        public Builder<V,E,S> strategy(Strategy<V,E> strategy) {
            this.strategy = strategy;
            return this;
        }

        public abstract InterfaceSolver<V,E,S> build();
    }

    //////////////////  Methods ///////////////////

    @Override
    @SuppressWarnings("unchecked")
    public InterfaceSolver<V,E,S> solve() throws UnsatisfiableStateException
    {
        return (InterfaceSolver<V,E,S>) super.solve();
    }

    public void markClass(V vertex)
    {
        // Add to constraint graph
        _graph.addVertex(vertex);

        // Mark class
        strategy.markClass(vertex);
    }

    public void markInterface(V vertex)
    {
        // Add to constraint graph
        _graph.addVertex(vertex);

        // Mark interface
        strategy.markInterface(vertex);
    }

    protected boolean inClasses(V vertex)
    {
        if (classes == null)
            throw new IllegalStateException();

        return classes.contains(vertex);
    }

    @Override
    protected final void solve(DirectedGraph<V,E> graph) throws UnsatisfiableStateException
    {
        // Class Subset
        classes = strategy.classSubsetOf(graph);

        // Interface Graph
        DirectedGraph<V,E> igraph =
                new SimpleDirectedGraph<>(graph.getEdgeFactory());
                
        // Fill interface graph
        for (E e : new HashSet<>(graph.edgeSet()))
        {
            V source = graph.getEdgeSource(e);
            V target = graph.getEdgeTarget(e);
            
            if (!classes.contains(target)) {         // Interface supertype
                // Move interface edges
                igraph.addVertex(source);
                igraph.addVertex(target);
                igraph.addEdge(source, target);
            } else if (!classes.contains(source)) {  // Interface type, Class supertype
                assert target.equals(root);
            } else { continue; }                     // Class type, Class supertype

            graph.removeEdge(source, target);
        }

        // Remove interfaces completely
        for (V v : new HashSet<>(graph.vertexSet()))
            if (!classes.contains(v))
            {
                assert graph.edgesOf(v).isEmpty();
                graph.removeVertex(v);
                igraph.addVertex(v); 
            }

        // Solve the graphs separately

        solveClassGraph(graph);
        solveInterfaceGraph(igraph);

        // Sanity Check

        for (V v : graph.vertexSet())
            if (classes.contains(v))
                assert classSolver.getSolution().containsKey(v) || 
                    v.equals(root) : v;
            else
                assert ifaceSolver.getSolution().containsKey(v) : v;

        // Synthesize solutions

        synthesize();
        classes = null;
    }

    protected void solveClassGraph(DirectedGraph<V,E> graph) 
        throws UnsatisfiableStateException
    {
        classSolver = new SingleInheritanceSolver<>(graph, root);
        classSolver.solve();
    }

    protected void solveInterfaceGraph(DirectedGraph<V,E> graph)
        throws UnsatisfiableStateException
    {
        ifaceSolver = new MultipleInheritanceSolver<>(graph, minimize);
        ifaceSolver.solve();
    }

    protected abstract void synthesize();


    ////////////////// Type Resolving Strategy ///////////////////

    public interface Strategy<V,E>
    {
        Set<V> classSubsetOf(DirectedGraph<V,E> graph) throws GraphCycleException;
        void markClass(V vertex);
        void markInterface(V vertex);
    }
}
