package jphantom.tree;

import jphantom.tree.graph.*;
import org.jgrapht.DirectedGraph;
import org.objectweb.asm.Type;
import static org.jgrapht.Graphs.*;

public class PrintableClassHierarchy extends ForwardingClassHierarchy
{
    private static final String step = "  ";
    private static final Node ROOT = Node.get(OBJECT);
    private DirectedGraph<Node,Edge> graph = null;
    private StringBuilder builder = null;

    public PrintableClassHierarchy(ClassHierarchy hierarchy)
    {
        super(hierarchy);
    }

    private boolean isInterface(Node n) {
        return isInterface(n.asType());
    }

    @Override
    public void addClass(Type clazz, Type superclass, Type[] interfaces)
    {
        // Invalidate string representation
        builder = null;

        // Add class
        super.addClass(clazz, superclass, interfaces);
    }

    @Override
    public void addInterface(Type iface, Type[] superInterfaces)
    {
        // Invalidate string representation
        builder = null;

        // Add interface
        super.addInterface(iface, superInterfaces);
    }

    @Override
    public String toString() 
    {
        if (builder != null)
            return builder.toString();

        if (!ClassHierarchies.unknownTypes(hierarchy).isEmpty())
            throw new IllegalStateException();

        // Convert class hierarchy to graph
        this.graph = new GraphConverter(hierarchy).convert();

        // Create string builder
        this.builder = new StringBuilder();

        return appendHierarchy().toString();
    }

    private StringBuilder appendClassTree(Node root) {
        return appendClassTree(root, "");
    }

    private StringBuilder appendInterfaceTree(Node root, Node parent) {
        return appendInterfaceTree(root, parent, "");
    }

    private StringBuilder appendClassTree(Node root, String prefix)
    {
        if (isInterface(root)) { return builder;}

        builder
            .append(prefix)
            .append("* class ")
            .append(root);

        if (graph.outDegreeOf(root) > 1) 
        {    
            builder.append(" (implements ");

            for (Node succ : successorListOf(graph, root))
                if (isInterface(succ))
                    builder.append(succ).append(", ");

            builder.setLength(builder.length() - 2);
            builder.append(")");
        }

        builder.append('\n');

        for (Node pred : predecessorListOf(graph, root))
            appendClassTree(pred, prefix + step);

        return builder;
    }

    private StringBuilder appendInterfaceTree(Node root, Node parent, String prefix)
    {
        if (!isInterface(root)) { return builder; }

        builder
            .append(prefix)
            .append("* interface ")
            .append(root);

        if (graph.outDegreeOf(root) > 2)
        {
            builder.append(" (also extends ");

            for (Node succ : successorListOf(graph, root))
                if (isInterface(succ) && !succ.equals(parent))
                    builder.append(succ).append(", ");

            builder.setLength(builder.length() - 2);
            builder.append(")");
        }

        builder.append('\n');

        for (Node pred : predecessorListOf(graph, root))
            appendInterfaceTree(pred, root, prefix + "  ");

        return builder;
    }




    private StringBuilder appendHierarchy()
    {
        builder.append("Class Hierarchy\n\n");

        appendClassTree(ROOT);

        builder.append("\n\nInterface Hierarchy\n\n");

        for (Node pred : predecessorListOf(graph, ROOT))
            if (isInterface(pred) && graph.outDegreeOf(pred) <= 1)
                appendInterfaceTree(pred, ROOT);

        return builder;
    }

}
