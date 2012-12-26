package jphantom.constraints.solvers;

import util.*;
import java.util.*;
import org.jgrapht.*;
import org.jgrapht.graph.*;

public abstract class AbstractSolver<V,E,S> implements Solver<V,E,S>
{
    protected boolean solved = false;
    protected S solution;
    private final Factory<S> solutionFactory;
    protected final EdgeFactory<V,E> factory;
    protected final DirectedGraph<V,E> graph;
    private DirectedGraph<V,E> unmodifiableGraph;

    ////////////// Constructors //////////////

    public AbstractSolver(EdgeFactory<V,E> factory, Factory<S> solutionFactory) {
        this(new SimpleDirectedGraph<V,E>(factory), solutionFactory);
    }

    public AbstractSolver(DirectedGraph<V,E> graph, Factory<S> solutionFactory) {
        this.solutionFactory = solutionFactory;
        this.factory = graph.getEdgeFactory();
        this.graph = graph;
        this.unmodifiableGraph = new UnmodifiableDirectedGraph<V,E>(graph);
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

        DirectedGraph<V,E> backup = new SimpleDirectedGraph<V,E>(factory);
        Graphs.addGraph(backup, graph);
        solution = solutionFactory.create();
        solve(backup);
        solved = true;
        return this;
    }

    @Override
    public void addConstraintEdge(V source, V target)
    {
        if (!graph.containsEdge(source, target))
            solved = false;

        if (!graph.containsVertex(source))
            graph.addVertex(source);

        if (!graph.containsVertex(target))
            graph.addVertex(target);

        graph.addEdge(source, target);
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
