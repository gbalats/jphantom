package org.clyze.jphantom.constraints.solvers;

import java.util.*;
import org.jgrapht.*;
import static com.google.common.collect.Iterables.concat;

public abstract class LayeringSolver<V,E> extends MultipleInheritanceSolver<V,E>
{
    private final Set<E> special;
    private final Map<V,List<V>> projections;

    public LayeringSolver(
        DirectedGraph<V,E> graph, Set<E> special, Map<V,List<V>> projections, boolean minimize)
    {
        super(graph, minimize);

        for (E e : special)
            if (!graph.containsEdge(e))
                throw new IllegalArgumentException();
            else {
                V source = graph.getEdgeSource(e);

                if (!projections.containsKey(source))
                    throw new IllegalArgumentException("" + e);
                else if (projections.get(source).isEmpty())
                    throw new IllegalArgumentException("" + e);
            }

        this.special = special;
        this.projections = projections;
    }

    public final List<V> projectionsOf(V source) {
        return projections.get(source);
    }

    private Map<V,Integer> stratify(final DirectedGraph<V,E> graph) throws UnsatisfiableStateException
    {
        Set<E> pathEdges = new HashSet<>(graph.edgeSet());
        Set<E> specialEdges = new HashSet<>();
        Set<V> next = graph.vertexSet();

        List<Set<V>> strata = new LinkedList<>(Collections.singletonList(next));

        while(true)
        {
            Set<V> prev = next;
            next = new HashSet<>();

            strata.add(next);
            
            for (E e : concat(pathEdges, specialEdges))
                next.add(graph.getEdgeTarget(e));

            if (next.isEmpty())
                break;
            else if (next.size() == prev.size())
                throw new UnsatisfiableStateException();
            else
                assert next.size() < prev.size();

            for (Iterator<E> it = pathEdges.iterator(); it.hasNext();)
            {
                E e = it.next();
                    
                V source = graph.getEdgeSource(e);
                V target = graph.getEdgeTarget(e);
                    
                if (next.contains(source)) {
                    assert next.contains(target); 
                } else {
                    it.remove();

                    if (special.contains(e))
                        specialEdges.add(e);
                }
            }

            for (Iterator<E> it = specialEdges.iterator(); it.hasNext();)
            {
                E e = it.next();
                V source = graph.getEdgeSource(e);
                V target = graph.getEdgeTarget(e);
                
                for (V proj : projectionsOf(source))
                    if (!next.contains(proj)) {
                        it.remove();
                        break;
                    }
            }
        }

        Map<V,Integer> strataValues = new HashMap<>();

        for (ListIterator<Set<V>> it = strata.listIterator(); it.hasNext();)
        {
            int i = it.nextIndex();
            Set<V> stratum = it.next();

            for(V v : stratum)
                strataValues.put(v, i);
        }
        
        for (V v : graph.vertexSet())
            assert strataValues.containsKey(v);

        return strataValues;
    }

    @Override
    public void addConstraintEdge(V source, V target) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void solve(DirectedGraph<V,E> graph) throws UnsatisfiableStateException
    {
        Map<V,Integer> strata = stratify(graph);

    DIRECT:
        for (E e : special) {
            V source = graph.getEdgeSource(e);
            V target = graph.getEdgeTarget(e);

            for (V proj : projectionsOf(source)) {
                int pStratum = strata.get(proj);
                int tStratum = strata.get(target);

                if (pStratum < tStratum) {
                    graph.removeEdge(source, target);
                    graph.addEdge(proj, target);
                    continue DIRECT;
                }
            }
            throw new AssertionError();
        }

        super.solve(graph);
    }
}
