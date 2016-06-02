package org.clyze.jphantom.hier.closure;

import java.util.*;
import org.clyze.jphantom.hier.*;
import org.clyze.jphantom.hier.graph.*;
import org.clyze.jphantom.util.BootstrapClassLoader;
import org.objectweb.asm.Type;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.alg.TransitiveClosure;
import static org.jgrapht.Graphs.*;

/** @author George Balatsouras */
public class CopyingSnapshot extends PseudoSnapshot
{
    private final DirectedGraph<Node,Edge> graph;
    private final SimpleDirectedGraph<Node,Edge> closedGraph = 
        new SimpleDirectedGraph<Node,Edge>(Edge.factory);

    public CopyingSnapshot(ClassHierarchy other)
    {
        // Make defensive copy
        super(new IncrementalClassHierarchy(other));

        // Try to add missing types
        new Importer(hierarchy, BootstrapClassLoader.v()).execute();

        // Create graph representation
        this.graph = new GraphConverter(hierarchy).convert();

        // Compute the transitive closure of the class hierarchy
        addGraph(closedGraph, graph);
        TransitiveClosure.INSTANCE.closeSimpleDirectedGraph(closedGraph);
    }

    @Override
    public void addClass(Type clazz, Type superclass, Type[] interfaces) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addInterface(Type iface, Type[] superInterfaces) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Type> getAllSupertypes(Type obj)
        throws IncompleteSupertypesException
    {
        Set<Type> supertypes = new HashSet<>();
        
        for (Node n : successorListOf(closedGraph, Node.get(obj)))
            supertypes.add(n.asType());

        for (Type s : supertypes)
            if (!hierarchy.contains(s))
                throw new IncompleteSupertypesException(supertypes);

        return supertypes;
    }

    // TODO: extends AbstactSnapshot, implement remaining methods
}
