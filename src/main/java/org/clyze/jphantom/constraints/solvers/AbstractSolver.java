package org.clyze.jphantom.constraints.solvers;

import org.clyze.jphantom.util.Factory;
import org.jgrapht.*;
import org.jgrapht.graph.*;

public abstract class AbstractSolver<V,E,S> implements Solver<V,E,S>
{
    protected boolean solved = false;
    protected S solution;
    private final Factory<S> solutionFactory;
    protected final EdgeFactory<V,E> factory;
    protected final DirectedGraph<V,E> _graph;
    private final DirectedGraph<V,E> unmodifiableGraph;

    ////////////// Constructors //////////////

    public AbstractSolver(EdgeFactory<V,E> factory, Factory<S> solutionFactory) {
        this(new SimpleDirectedGraph<>(factory), solutionFactory);
    }

    public AbstractSolver(DirectedGraph<V,E> graph, Factory<S> solutionFactory) {
        this.solutionFactory = solutionFactory;
        this.factory = graph.getEdgeFactory();
        this._graph = graph;
        this.unmodifiableGraph = new UnmodifiableDirectedGraph<>(graph);
    }

    //////////////// Methods ////////////////

    @Override
    public DirectedGraph<V,E> getConstraintGraph() {
        return unmodifiableGraph;
    }

    @Override
    public S getSolution() {
        if (!solved)
            throw new IllegalStateException();
        return solution;
    }

    protected abstract void solve(DirectedGraph<V,E> graph)
        throws UnsatisfiableStateException;

    @Override
    public AbstractSolver<V,E,S> solve() throws UnsatisfiableStateException
    {
        if (solved) { return this; }

        DirectedGraph<V, E> backup = new SimpleDirectedGraph<>(factory);
        Graphs.addGraph(backup, _graph);
        solution = solutionFactory.create();
        solve(backup);
        solved = true;
        return this;
    }

    @Override
    public void addConstraintEdge(V source, V target)
    {
        if (!_graph.containsEdge(source, target))
            solved = false;

        if (!_graph.containsVertex(source))
            _graph.addVertex(source);

        if (!_graph.containsVertex(target))
            _graph.addVertex(target);

        _graph.addEdge(source, target);
    }


    ///////////////// Exceptions /////////////////

    protected static class GraphCycleException 
        extends UnsatisfiableStateException
    {
        protected final static long serialVersionUID = 2368453345L;

        protected GraphCycleException() {
            super();
        }
    }
}
