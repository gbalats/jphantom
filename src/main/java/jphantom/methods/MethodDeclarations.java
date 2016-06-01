package jphantom.methods;


import java.util.*;
import org.jgrapht.*;
import jphantom.Types;
import jphantom.hier.*;
import jphantom.hier.graph.*;
import org.objectweb.asm.Type;
import static org.jgrapht.Graphs.*;

public class MethodDeclarations implements Types
{
    private final ClassHierarchy hierarchy;
    private final MethodLookupTable mtable;
    private final Map<Type,Set<MethodSignature>> pending = new HashMap<>();
    private final Map<Type,Set<MethodSignature>> implemented = new HashMap<>();

    public MethodDeclarations(ClassHierarchy hierarchy, MethodLookupTable mtable)
    {
        // Make defensive copies
        this.hierarchy = new IncrementalClassHierarchy(hierarchy);
        this.mtable = new MethodLookupTable(mtable);

        for (Type t : hierarchy)
            if (!mtable.containsKey(t))
                throw new IllegalArgumentException();
        
        initializeClosures();
    }
    
    private final void initializeClosures()
    {
        // Dynamic Programming

        for (Type t : topologicalOrder(hierarchy))
        {
            Set<MethodSignature> direct = new HashSet<>(mtable.get(t));

            // Remove private methods
            for (Iterator<MethodSignature> it = direct.iterator(); it.hasNext();)
            {
                MethodSignature m = it.next();

                if (m.isPrivate())
                    it.remove();
            }

            assert direct != null;
            
            if (hierarchy.isInterface(t)) 
            {
                Set<MethodSignature> closure = new HashSet<>(direct);

                for (Type iface : hierarchy.getInterfaces(t)) {
                    assert pending.get(iface) != null : iface;
                    closure.addAll(pending.get(iface));
                }

                pending.put(t, closure);
            } else {
                Set<MethodSignature> impl = new HashSet<>();
                Set<MethodSignature> pend = new HashSet<>();

                for (MethodSignature m : direct)
                    (m.isAbstract() ? pend : impl).add(m);

                implemented.put(t, impl);
                pending.put(t, pend);

                // Add inherited methods

                Type sc = hierarchy.getSuperclass(t);

                if (sc != null)
                {
                    for (MethodSignature m : pending.get(sc))
                        addPending(t, m);

                    for (MethodSignature m : implemented.get(sc))
                        addImplemented(t, m);
                }

                for (Type iface : hierarchy.getInterfaces(t))
                    for (MethodSignature m : pending.get(iface))
                        addPending(t, m);
            }
        }
    }

    private boolean addImplemented(Type type, MethodSignature method)
    {
        if (!pending.get(type).contains(method))
            return implemented.get(type).add(method);
        return false;
    }

    private boolean addPending(Type type, MethodSignature method)
    {
        if (!pending.get(type).contains(method))
            return implemented.get(type).add(method);
        return false;
    }

    public Set<MethodSignature> getPending(Type type) {
        return pending.get(type);
    }

    public Set<MethodSignature> getImplemented(Type type) {
        return implemented.get(type);
    }

    private static List<Type> topologicalOrder(ClassHierarchy hierarchy)
    {
        Node root = Node.get(OBJECT);
        DirectedGraph<Node,Edge> graph = new GraphConverter(hierarchy).convert();
        
        List<Type> order = new ArrayList<>();
        Set<Node> unconstrained = new HashSet<>();
        unconstrained.add(root);
        
        while (!unconstrained.isEmpty())
        {
            Node n = unconstrained.iterator().next();
            unconstrained.remove(n);
            order.add(n.asType());

            for (Node m : predecessorListOf(graph, n))
            {
                graph.removeEdge(m, n);
                
                if (graph.outDegreeOf(m) == 0)
                    unconstrained.add(m);
            }
        }

        if (!graph.edgeSet().isEmpty())
            throw new AssertionError();

        return order;
    }
}
