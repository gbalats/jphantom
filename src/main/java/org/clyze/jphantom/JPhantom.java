package org.clyze.jphantom;

import org.clyze.jphantom.access.ClassAccessStateMachine;
import org.clyze.jphantom.access.FieldAccessStateMachine;
import org.clyze.jphantom.access.MethodAccessStateMachine;
import org.clyze.jphantom.adapters.InnerClassAdapter;
import org.clyze.jphantom.adapters.InterfaceAdder;
import org.clyze.jphantom.adapters.InterfaceTransformer;
import org.clyze.jphantom.adapters.MethodAdder;
import org.clyze.jphantom.adapters.PhantomAdder;
import org.clyze.jphantom.adapters.SuperclassAdapter;
import org.clyze.jphantom.constraints.Constraint;
import org.clyze.jphantom.constraints.extractors.TypeConstraintExtractor;
import org.clyze.jphantom.constraints.solvers.BasicSolver;
import org.clyze.jphantom.constraints.solvers.ConstraintStoringSolver;
import org.clyze.jphantom.constraints.solvers.PruningSolver;
import org.clyze.jphantom.constraints.solvers.Solver;
import org.clyze.jphantom.constraints.solvers.TypeConstraintSolver;
import org.clyze.jphantom.hier.ClassHierarchies;
import org.clyze.jphantom.hier.ClassHierarchy;
import org.clyze.jphantom.hier.PrintableClassHierarchy;
import org.clyze.jphantom.hier.UnmodifiableClassHierarchy;
import org.clyze.jphantom.methods.MethodDeclarations;
import org.clyze.jphantom.methods.MethodSignature;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class JPhantom {
    protected final static Logger logger =
            LoggerFactory.getLogger(Driver.class);
    private final Phantoms phantoms = Phantoms.V();
    private final Map<Type, ClassNode> nodes;
    private final ClassHierarchy hierarchy;
    private final ClassMembers members;
    private Map<Type, byte[]> generated;

    public JPhantom(Map<Type, ClassNode> nodes, ClassHierarchy hierarchy, ClassMembers members) {
        this.nodes = nodes;
        this.hierarchy = new UnmodifiableClassHierarchy(hierarchy);
        this.members = members;

        // Resolve all phantom supertypes so far

        final SignatureVisitor visitor = new PhantomAdder(
                hierarchy, members, phantoms);

        for (Type unknown : ClassHierarchies.unknownTypes(hierarchy))
            new SignatureReader("" + unknown).acceptType(visitor);

        // Sanity check

        for (Type unknown : ClassHierarchies.unknownTypes(hierarchy))
            assert phantoms.contains(unknown);
    }

    public void run() throws IOException
    {
        // Analyze

        TypeConstraintSolver solver =
                new ConstraintStoringSolver(
                        new BasicSolver.Builder().hierarchy(hierarchy).build());

        // Prune unrelated types before feeding them to the solver
        solver = new PruningSolver(solver);

        TypeConstraintExtractor extractor = new TypeConstraintExtractor(solver);

        for (ClassNode node : nodes.values()) {
            try {
                extractor.visit(node);
            } catch (AnalyzerException e) {
                throw new RuntimeException(e);
            }
        }

        // Additional constraints
        for (Constraint c : FieldAccessStateMachine.v().getConstraints())
            c.accept(solver);
        for (Constraint c : MethodAccessStateMachine.v().getConstraints())
            c.accept(solver);
        for (Constraint c : ClassAccessStateMachine.v().getConstraints())
            c.accept(solver);

        for (Constraint c : solver.getConstraints())
            logger.info("Constraint: {}", c);

        // Solve constraints
        ClassHierarchy solution;

        try {
            solution = solver.solve().getSolution();
        } catch (Solver.UnsatisfiableStateException exc) {
            throw new RuntimeException(exc);
        }

        logger.info("Found Solution: \n\n{}", new PrintableClassHierarchy(solution));

        // Add supertypes
        addSupertypes(solution);

        // Generate files
        generated = phantoms.generateClasses();

        // Load required class methods of the types that comprise our solution
        fillLookupTable(solution);

        // Add missing methods
        addMissingMethods(solution, new MethodDeclarations(solution, phantoms.getLookupTable()));

        // Add inner/outer relations (compiler will not be happy if the inner/outer attributes are missing)
        addInnerOuterRelations();
    }

    private void fillLookupTable(ClassHierarchy solution) throws IOException
    {
        for (Type t : solution)
        {
            // Phantom Type
            if (phantoms.contains(t))
                continue;

            ClassVisitor visitor = phantoms.getLookupTable().new CachingAdapter();

            // Input Type
            if (nodes.containsKey(t)) {
                nodes.get(t).accept(visitor);
                continue;
            }

            // Library Type
            new ClassReader(t.getInternalName()).accept(visitor, 0);
        }
    }

    private void addSupertypes(ClassHierarchy solution)
    {
        for (Type p : solution)
        {
            if (hierarchy.contains(p))
                continue;

            if (!Options.V().isSoftFail())
                assert phantoms.contains(p) : p;

            // Get top class visitor
            Transformer tr = phantoms.getTransformer(p);

            assert tr.top != null;

            // Chain a superclass / interface adapter

            tr.top = solution.isInterface(p) ?
                    new InterfaceTransformer(tr.top) :
                    new SuperclassAdapter(tr.top, solution.getSuperclass(p));

            // Chain an interface adder

            tr.top = new InterfaceAdder(tr.top, solution.getInterfaces(p));
        }
    }

    private void addMissingMethods(ClassHierarchy solution, MethodDeclarations declarations) {
        for (Type p : phantoms)
        {
            Set<MethodSignature> pending = declarations.getPending(p);

            if (pending == null) {
                assert !solution.contains(p);
                continue;
            }

            if (pending.isEmpty())
                continue;

            for (MethodSignature m : pending)
            {
                logger.debug("Adding method {} to \"{}\"", m, p.getClassName());

                // Chain a method-adder adapter

                ClassWriter cw = new ClassWriter(0);
                ClassVisitor cv = new MethodAdder(cw, m);

                ClassReader cr = new ClassReader(generated.get(p));
                cr.accept(cv, 0);

                generated.put(p, cw.toByteArray());
            }
        }
    }

    private void addInnerOuterRelations() {
        Map<String, Type> typeLookup = phantoms.stream()
                .collect(Collectors.toMap(Type::getInternalName, t -> t));
        for (Type p : phantoms)
        {
            String name = p.getInternalName();
            int innerIndex = name.lastIndexOf('$');
            if (innerIndex <= 0)
                continue;

            try {
                String outer = name.substring(0, innerIndex);
                ClassReader cr = new ClassReader(generated.get(p));
                int access = cr.getAccess();

                ClassWriter cw = new ClassWriter(0);

                // Add outer class
                ClassVisitor cv = new InnerClassAdapter(cw, name, access);
                cr.accept(cv, 0);
                generated.put(p, cw.toByteArray());


                // Add inner class
                p = typeLookup.get(outer);
                if (p != null) {
                    cw = new ClassWriter(0);
                    cv = new InnerClassAdapter(cw, name, access);
                    cr = new ClassReader(generated.get(p));
                    cr.accept(cv, 0);
                    generated.put(p, cw.toByteArray());
                }
            } catch (Throwable t) {}
        }
    }

    public Map<Type, byte[]> getGenerated() {
        return generated;
    }
}
