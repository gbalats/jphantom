package jphantom.hier.graph;

import org.jgrapht.*;

public class Edge {
    private final Node from;
    private final Node to;

    protected Edge(Node from, Node to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public boolean equals(Object other)
    {
        if (other == this)
            return true;
        if (!(other instanceof Edge))
            return false;
        Edge o = (Edge) other;
        return from.equals(o.from) && to.equals(o.to);
    }

    @Override
    public int hashCode()
    {
        int result = 17;
        result = 31 * result + from.hashCode();
        result = 31 * result + to.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "(" + from + " <: " + to + ")";
    }

    public static final EdgeFactory<Node,Edge> factory = new EdgeFactory<Node,Edge>()
    {
        @Override public Edge createEdge(Node from, Node to)
        {
            return new Edge(from,to);
        }
    };
}
