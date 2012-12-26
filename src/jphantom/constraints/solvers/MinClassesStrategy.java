package jphantom.constraints.solvers;

import util.*;
import java.util.*;
import org.jgrapht.*;
import org.objectweb.asm.Type;

import static util.Utils.*;
import static org.jgrapht.Graphs.*;
import static jphantom.constraints.solvers.InterfaceSolver.GraphCycleException;

public class MinClassesStrategy<V,E> implements InterfaceSolver.Strategy<V,E>
{
    private final Map<V,Colored<V>> colored = newMap();
    private final V root;
    private DirectedGraph<V,E> graph;

    MinClassesStrategy(V root) {
        this.root = root;
    }

    protected Colored<V> getColored(V vertex)
    {
        if (!colored.containsKey(vertex))
            colored.put(vertex, new Colored<V>(vertex));

        return colored.get(vertex);
    }

    @Override public void markClass(V vertex) {
        getColored(vertex).setType(Colored.Type.CLASS);
    }

    @Override public void markInterface(V vertex) {
        getColored(vertex).setType(Colored.Type.INTERFACE);
    }

    @Override
    public Set<V> classSubsetOf(DirectedGraph<V,E> graph) throws GraphCycleException
    {
        Map<V,Colored<V>> backup = newMap(colored);

        try {
            this.graph = graph;

            for (V v : colored.keySet())
                if (!graph.containsVertex(v))
                    throw new IllegalArgumentException();

            determineTypes();

            Set<V> classes = newSet();

            for (Colored<V> v : colored.values())
                if (v.getType() == Colored.Type.CLASS)
                    classes.add(v.get());

            for (V v : graph.vertexSet())
                assert getColored(v).resolved();

            return classes;
        } finally {
            colored.clear();
            colored.putAll(backup);
        }
    }

    private void mark(V vertex, final Colored.Type type) throws GraphCycleException
    {
        getColored(vertex).setType(type);
        getColored(vertex).color = Colored.Color.GREY;

        Collection<V> touched = (type == Colored.Type.INTERFACE) ? 
            successorListOf(graph, vertex) : predecessorListOf(graph, vertex);
     
        // Special root handling

        if (type == Colored.Type.INTERFACE)
            touched.remove(root);

        if (vertex.equals(root))
            touched = Collections.emptyList();
   
        // Recursion

        for (V next : touched) {
            switch(getColored(next).color) {
            case WHITE:
                mark(next, type);
                break;
            case GREY:
                throw new GraphCycleException();
            case BLACK:
                getColored(next).setType(type);
                break;
            }
        }

        getColored(vertex).color = Colored.Color.BLACK;
    }

    private void determineTypes() throws GraphCycleException
    {
        Set<V> rootSet = newSet();

        for (V v : graph.vertexSet())
        {
            if (getColored(v).resolved())
                rootSet.add(v);

            getColored(v).color = Colored.Color.WHITE;
        }

        for (V v : rootSet)
            if (getColored(v).color == Colored.Color.WHITE)
                mark(v, getColored(v).getType());
    
        // Make unresolved types interfaces
        for (V v : graph.vertexSet())
            if (!getColored(v).resolved())
                getColored(v).setType(Colored.Type.INTERFACE);
    }
}
