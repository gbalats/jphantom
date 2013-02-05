package jphantom.constraints.solvers;

import java.util.*;
import org.jgrapht.*;
import com.google.common.base.Predicate;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterators.getNext;
import static com.google.common.collect.Sets.filter;
import static com.google.common.collect.Sets.difference;

public abstract class LayeringSolver<V,E> extends MultipleInheritanceSolver<V,E>
{
    private Set<E> special;

    public LayeringSolver(DirectedGraph<V,E> graph, Set<E> special, boolean minimize) {
        super(graph, minimize);

        for (E e : special)
            if (!graph.containsEdge(e))
                throw new IllegalArgumentException();

        this.special = special;
    }
    
    protected abstract List<V> projectionsOf(V source);

    private List<Set<V>> stratify(final DirectedGraph<V,E> graph) throws UnsatisfiableStateException
    {
        Queue<Set<V>> strata = new LinkedList<>(Arrays.asList(graph.vertexSet()));

        Set<E> pathEdges = new HashSet<>(graph.edgeSet());
        Set<E> directEdges = new HashSet<>();

        while(true)
        {
            final Set<V> prev = strata.peek();
            final Set<V> next = new HashSet<V>();

            strata.offer(next);
            
            for (E e : concat(pathEdges, directEdges))
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
                    continue;
                }

                it.remove();

                if (special.contains(e))
                    directEdges.add(e);
            }

            directEdges = filter(
                directEdges, 
                new Predicate<E>()
                {
                    @Override public boolean apply(E e) {
                        V source = graph.getEdgeSource(e);
                        V target = graph.getEdgeTarget(e);

                        for (V proj : projectionsOf(source))
                            if (!next.contains(proj))
                                return false;

                        return true;
                    }
                });
        }

        List<Set<V>> filtered = new ArrayList<>(strata.size());

        for (Iterator<Set<V>> it = strata.iterator(); it.hasNext();)
        {
            Set<V> stratum = it.next();
            Set<V> nextStratum = getNext(it, Collections.<V>emptySet());

            filtered.add(difference(stratum, nextStratum).immutableCopy());
        }

        return filtered;
    }

}
