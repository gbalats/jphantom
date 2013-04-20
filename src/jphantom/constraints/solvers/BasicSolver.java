package jphantom.constraints.solvers;

import util.*;
import java.util.*;
import jphantom.*;
import jphantom.exc.*;
import jphantom.tree.*;
import jphantom.tree.closure.*;
import jphantom.constraints.*;
import org.jgrapht.*;
import org.objectweb.asm.Type;

import static org.jgrapht.Graphs.*;

public class BasicSolver extends InterfaceSolver<Type,SubtypeConstraint,ClassHierarchy>
    implements Types, TypeConstraintSolver
{
    private boolean initialized = false;
    protected ClassHierarchy hierarchy;
    private ClassHierarchy.Snapshot closure = null;

    ///////////////////// Constructors /////////////////////

    protected BasicSolver(Builder builder) {
        super(builder);
        this.hierarchy = builder.hierarchy;
    }

    ///////////////////// Builder /////////////////////

    public static class Builder
        extends InterfaceSolver.Builder<Type,SubtypeConstraint,ClassHierarchy>
    {
        private ClassHierarchy hierarchy = new IncrementalClassHierarchy();

        public Builder(DirectedGraph<Type,SubtypeConstraint> graph) {
            super(OBJECT, defaultFactory(), graph);
        }

        public Builder() {
            super(OBJECT, defaultFactory(), SubtypeConstraint.factory);
        }

        public Builder hierarchy(ClassHierarchy hierarchy)
        {
            this.hierarchy = hierarchy;
            return this;
        }

        @Override
        public BasicSolver build() {
            return new BasicSolver(this);
        }

        private static Factory<ClassHierarchy> defaultFactory() {
            return new Factory<ClassHierarchy>() {
                @Override public ClassHierarchy create() {
                    return new IncrementalClassHierarchy();
                }
            };
        }
    }

    ///////////////////// Constraints /////////////////////

    @Override
    public void visit(SubtypeConstraint constraint) {
        addConstraintEdge(constraint.subtype, constraint.supertype);
    }

    @Override
    public void visit(IsanInterfaceConstraint constraint)
    {
        try {
            // Mark interface
            markInterface(constraint.type);
        } catch (ConflictingTypeException exc) {
            throw new InsolvableConstraintException(constraint);
        }
    }

    @Override
    public void visit(IsaClassConstraint constraint)
    {
        try {
            // Mark class
            markClass(constraint.type);
        } catch (ConflictingTypeException exc) {
            throw new InsolvableConstraintException(constraint);
        }
    }

    ///////////////////// Methods /////////////////////

    @Override
    public ClassHierarchy getHierarchy() {
        return hierarchy;
    }

    @Override
    public void setHierarchy(ClassHierarchy hierarchy) {
        this.hierarchy = hierarchy;
    }

    @Override
    public Collection<Constraint> getConstraints() {
        throw new UnsupportedOperationException();
    }

    private BasicSolver initialized()
    {
        // Compute transitive closure
        closure = new CopyingSnapshot(hierarchy);

        // Add class hierarchy as graph edges
        for (Type clazz : hierarchy) {
            // Add interface edges and mark interface vertices
            for (Type iface : hierarchy.getInterfaces(clazz)) {
                addConstraintEdge(clazz, iface);
                markInterface(iface);
            }

            // Mark class and superclass vertices
            if (hierarchy.isInterface(clazz))
                markInterface(clazz);
            else
                markClass(clazz);

            Type sc = hierarchy.getSuperclass(clazz);

            if (!clazz.equals(OBJECT)) {
                addConstraintEdge(clazz, sc);
                markClass(sc);
            } else {
                assert sc == null;
            }
        }
        initialized = true;
        return this;
    }

    @Override
    public BasicSolver solve() throws UnsatisfiableStateException {
        initialized();
        return (BasicSolver) super.solve();
    }

    @Override
    protected void solveClassGraph(DirectedGraph<Type,SubtypeConstraint> graph) 
        throws UnsatisfiableStateException
    {
        if (!initialized)
            throw new IllegalStateException();

        for (Type v : graph.vertexSet()) {
            assert inClasses(v) : v;
            if (hierarchy.contains(v))
                assert !hierarchy.isInterface(v) : v;
        }

        // Add pseudo-edges to enforce correct supertypes
        for (SubtypeConstraint e : new HashSet<>(graph.edgeSet()))
        {
            Type source = graph.getEdgeSource(e);
            final Type target = graph.getEdgeTarget(e);

            while (hierarchy.contains(source)) {
                assert !hierarchy.isInterface(source);

                final Type p = hierarchy.getSuperclass(source);

                if (p == null) {
                    assert source.equals(OBJECT);
                    throw new InsolvableConstraintException(e);
                }
 
                assert graph.containsEdge(source, p) : p;

                if (p.equals(target))
                    break;

                addConstraintEdge(source = p, target);
            }
        }

        // Create specialized single inheritance solver
        // that prioritizes direct subclasses
        classSolver = new SingleInheritanceSolver<Type,SubtypeConstraint>(graph, OBJECT)
            {
                private Random rand = new Random(System.currentTimeMillis());

                @Override
                protected Deque<Type> order(Set<Type> unconstrained, Type parent)
                {
                    Deque<Type> ordered = new LinkedList<>();
                    List<Type> rest = new LinkedList<>();

                    for (Type t : unconstrained)
                        if (hierarchy.contains(t))
                        {
                            Type sc = hierarchy.getSuperclass(t);

                            if (!sc.equals(parent)) {
                                SubtypeConstraint impliedEdge = _graph.getEdgeFactory().
                                    createEdge(t, parent);

                                throw new CrossoverConstraintException(impliedEdge, sc);
                            }
                            ordered.addLast(t);
                        } else {
                            rest.add(t);
                        }

                    // Randomize the remaining nodes order
                    Collections.shuffle(rest, rand);

                    ordered.addAll(rest);
                    return ordered;
                }
            };

        // Las Vegas Style
        while (true) {
            try {
                classSolver.solve();
                break;
            } catch (CrossoverConstraintException exc) {
                continue;
            }
        }
    }

    @Override
    protected void solveInterfaceGraph(DirectedGraph<Type,SubtypeConstraint> graph)
        throws UnsatisfiableStateException
    {
        if (!initialized)
            throw new IllegalStateException();

        Set<SubtypeConstraint> fixedSource = new HashSet<>();
        Map<Type,List<Type>> domains = new HashMap<>();

        for (SubtypeConstraint e : new HashSet<>(graph.edgeSet()))
        {
            Type source = graph.getEdgeSource(e);
            Type target = graph.getEdgeTarget(e);

            // Skip phantom-source edges
            if (!hierarchy.contains(source))
                continue;

            // Leave direct edges
            if (hierarchy.getInterfaces(source).contains(target))
                continue;

            try {
                // Check if edge is already satisfied
                if (!closure.isSubtypeOf(source, target))
                    throw new UnsatisfiableStateException();

                // Remove transitively satisfied path-edge
                graph.removeEdge(source, target);
            } catch (IncompleteSupertypesException ign) {
                // Add path-edge since it can be satisfied eventually
                fixedSource.add(e);

                // Compute Projections
                try {
                    // Must include all supertypes, including classes
                    closure.getAllSupertypes(source);

                    // No phantom projections => insolvable constraint
                    throw new InsolvableConstraintException(e);

                } catch (IncompleteSupertypesException exc) {
                    List<Type> projections = new ArrayList<>();
                    
                    assert !exc.getSupertypes().isEmpty();

                    for (Type t : exc.getSupertypes())
                        if (!hierarchy.contains(t))
                            projections.add(t);

                    // Phantom superclass might be missing, if no other interface
                    // constraints were associated with it.
                    // We can just add the relevant vertex into the iface-graph.
                    for (Type t : projections)
                        if (!graph.containsVertex(t)) {
                            graph.addVertex(t);
                            break;
                        }

                    for (Type t : projections)
                        assert graph.containsVertex(t) : t;
                    
                    domains.put(source, projections);
                }
            }
        }

        ifaceSolver = new LayeringSolver<Type,SubtypeConstraint>(graph, fixedSource, domains, minimize) {
            @Override 
            protected boolean removableEdge(Type source, Type target)
            {
                if (hierarchy.contains(source) && 
                    hierarchy.getInterfaces(source).contains(target))
                    return false;
                return super.removableEdge(source, target);
            }
        }.solve();
        
        // ifaceSolver = new RecursiveSolver(graph, minimize).solve();
    }

    /////////////////////// Solution Synthesis ///////////////////////

    protected void synthesize()
    {
        Map<Type,Type> classSolution = classSolver.getSolution();
        Map<Type,List<Type>> ifaceSolution = ifaceSolver.getSolution();

        // Add interface graph to output
        for (Map.Entry<Type,List<Type>> entry : ifaceSolution.entrySet())
        {
            Type v = entry.getKey();
            Type[] ifaces = entry.getValue().toArray(new Type[0]);

            if (inClasses(v)) {
                assert classSolution.containsKey(v);
                solution.addClass(v, classSolution.get(v), ifaces);
            } else {
                assert !classSolution.containsKey(v);
                solution.addInterface(v, ifaces);
            }
        }

        // Add remaining classes to output
        for (Map.Entry<Type,Type> entry : classSolution.entrySet()) {
            Type v = entry.getKey();
            Type sc = entry.getValue();

            if (solution.contains(v)) {
                assert solution.getSuperclass(v).equals(sc);
                continue;
            }
            solution.addClass(v, sc, new Type[0]);
        }

        // Make solution immutable
        solution = new UnmodifiableClassHierarchy(solution);

        // Validate Solution
        validateSolution();
    }

    /////////////////////// Validation ///////////////////////

    private void validateSolution()
    {
        // Sanity check: sound w.r.t the given hierarchy

        for (Type c : hierarchy)
        {
            Type sc1 = solution.getSuperclass(c);
            Type sc2 = hierarchy.getSuperclass(c);
            
            Set<Type> ifaces1 = solution.getInterfaces(c);
            Set<Type> ifaces2 = hierarchy.getInterfaces(c);

            assert sc1 == sc2 || sc1.equals(sc2) : c;
            assert ifaces1.equals(ifaces2) : c;
        }

        // Sanity check: complete hierarchy (no types missing)

        assert ClassHierarchies.unknownTypes(solution).isEmpty();
    }

    ///////////////////// Nested Classes /////////////////////

    public class CrossoverConstraintException extends InsolvableConstraintException
    {
        protected static final long serialVersionUID = 8467345638453745L;
        private final Type root;

        private CrossoverConstraintException(SubtypeConstraint constraint, Type root)
        {
            super(constraint);
            this.root = root;
        }

        public ClassHierarchy getHierarchy() { return hierarchy; }
        public Type getRoot() { return root; }
    }

    private class RecursiveSolver extends MultipleInheritanceSolver<Type,SubtypeConstraint>
    {
        private final Map<Type,List<Type>> domains = new HashMap<>();
        private Queue<Pair<Type,Type>> constraints;

        RecursiveSolver(DirectedGraph<Type,SubtypeConstraint> graph, boolean minimize)
        { super(graph, minimize); }


        @Override
        public void addConstraintEdge(Type source, Type target) {
            throw new UnsupportedOperationException();
        }

        private List<Type> domainOf(Type source) {
            if (!domains.containsKey(source)) {
                try {
                    closure.getAllSupertypes(source);
                    domains.put(source, Collections.<Type>emptyList());
                } catch (IncompleteSupertypesException exc) {
                    List<Type> domain = new ArrayList<>();

                    for (Type t : exc.getSupertypes())
                        if (!hierarchy.contains(t))
                            domain.add(t);

                    assert !domain.isEmpty();
                    domains.put(source, domain);
                } 
            }
            return domains.get(source);
        }

        @Override
        protected void solve(DirectedGraph<Type,SubtypeConstraint> graph) 
            throws UnsatisfiableStateException
        {
            // Remove constraints from fixed source vertices

            constraints = new LinkedList<>();

            for (SubtypeConstraint e : new HashSet<>(graph.edgeSet()))
            {
                Type source = graph.getEdgeSource(e);
                Type target = graph.getEdgeTarget(e);

                // Skip phantom-source edges
                if (!hierarchy.contains(source))
                    continue;

                // Leave direct edges
                if (hierarchy.getInterfaces(source).contains(target))
                    continue;

                // Remove edge
                graph.removeEdge(source, target);

                // Check if edge is already satisfied
                try {
                    if (!closure.isSubtypeOf(source, target))
                        throw new UnsatisfiableStateException();
                } catch (IncompleteSupertypesException ign) {
                    // Add constraint since it can be satisfied eventually
                    constraints.add(new Pair<Type,Type>(source, target));
                }
            }

            if (!solveAux(graph))
                throw new UnsatisfiableStateException();
        }

        private boolean solveAux(DirectedGraph<Type,SubtypeConstraint> graph)
        {
            // Base Case

            if (constraints.isEmpty()) {
                try {
                    solution = 
                        new MultipleInheritanceSolver<Type,SubtypeConstraint>(
                            graph, minimize) {
                        
                            @Override 
                            protected boolean removableEdge(Type source, Type target)
                            {
                                if (hierarchy.contains(source) && 
                                    hierarchy.getInterfaces(source).contains(target))
                                    return false;
                                return super.removableEdge(source, target);
                            }
                        }.solve().getSolution();
                } catch (UnsatisfiableStateException exc) {
                    return false;
                }
                return true;
            }

            // Recursive Case

            Pair<Type,Type> edge = constraints.poll();
            Type source = edge.fst, target = edge.snd;
            List<Type> domain = domainOf(source);
            
            // Try every value in the domain
            for (Type t : domain) {
                if (!graph.containsVertex(t))
                    graph.addVertex(t);
                graph.addEdge(t, target);             
                if (solveAux(graph)) { return true; }
                graph.removeEdge(t, target);
            }

            return false;
        }
    }
}
