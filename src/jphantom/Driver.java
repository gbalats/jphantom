package jphantom;

import com.beust.jcommander.JCommander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.jar.*;
import jphantom.tree.*;
import jphantom.jar.*;
import jphantom.methods.*;
import jphantom.access.*;
import jphantom.adapters.*;
import jphantom.constraints.*;
import jphantom.constraints.solvers.*;
import jphantom.constraints.extractors.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import static jphantom.constraints.solvers.AbstractSolver.UnsatisfiableStateException;

public class Driver implements Types
{
    /* Fields */

    private final ClassHierarchy hierarchy;
    private final File outDir;
    private final Phantoms phantoms = Phantoms.V();
    private final Map<Type,ClassNode> nodes = new HashMap<>();

    private final static Logger logger = 
        LoggerFactory.getLogger(Driver.class);

    /* Constructors */
    
    public Driver(Path jar, Path dir) throws IOException {
        this(jar.toString(), dir.toFile());
    }

    public Driver(String jarname, File out) throws IOException
    {
        this.outDir = out;

        ClassHierarchy hierarchy = ClassHierarchies.fromJar(jarname);

        // Create Jar Input Stream
        JarInputStream jin = new JarInputStream(new FileInputStream(jarname));
        JarEntry entry;
        JarFile jarFile = new JarFile(jarname);

        try {
            /* List all JAR entries */
            while ((entry = jin.getNextJarEntry()) != null)
            {
                /* Skip directories */
                if (entry.isDirectory())
                    continue;

                /* Skip non-class files */
                if(!entry.getName().endsWith(".class"))
                    continue;

                logger.trace("Reading jar entry: {}", entry.getName());
                ClassReader reader = new ClassReader(jarFile.getInputStream(entry));
                reader.accept(new ClassPhantomExtractor(hierarchy), 0);

                // At this point, every phantom class has been extracted.
                // Moreover, our class hierarchy has been augmented so
                // that it contains all the library classes that are 
                // referenced in the jar.
                
                ClassNode node = new ClassNode();
                reader.accept(node, 0);
                nodes.put(Type.getObjectType(node.name), node);
            }
        } finally {
            jarFile.close();
            jin.close();
        }

        hierarchy = new UnmodifiableClassHierarchy(hierarchy);

        // Sanity check

        for (Type unknown : ClassHierarchies.unknownTypes(hierarchy))
            assert phantoms.contains(unknown);

        this.hierarchy = hierarchy;
    }
    

    /* Methods */

    public void run() throws IOException
    {        
        // Analyze

        TypeConstraintSolver solver = 
            new PruningSolver(
                new ConstraintStoringSolver(
                    new BasicSolver.Builder().hierarchy(hierarchy).build()));

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

        for (Constraint c : solver.getConstraints())
            logger.info("Constraint: {}", c);

        // Solve constraints
        ClassHierarchy solution;

        try {
            solution = solver.solve().getSolution();
        } catch (UnsatisfiableStateException exc) {
            throw new RuntimeException(exc);
        }

        logger.info("Found Solution: \n\n{}", new PrintableClassHierarchy(solution));

        // Add supertypes
        addSupertypes(solution);
        
        // Generate files
        phantoms.generateFiles(outDir);

        // Load required class methods of the types that comprise our solution
        fillLookupTable(solution);

        // Add missing methods
        addMissingMethods(
            solution, new MethodDeclarations(solution, phantoms.getLookupTable()));
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
    
    private void addMissingMethods(ClassHierarchy solution, MethodDeclarations declarations)
        throws IOException
    {
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

                File outFile = Phantoms.locationOf(outDir, p);

                ClassVisitor cw = new ClassWriter(0);                
                ClassVisitor cv = new MethodAdder(cw, m);
                ClassReader  cr = new ClassReader(new FileInputStream(outFile));
                
                cr.accept(cv, 0);
            }
        }
    }

    private static void deleteDirectory(Path dir) throws IOException
    {
        Path start = dir;

        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException
                {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException e)
                    throws IOException
                {
                    if (e == null) {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    } else {
                        // directory iteration failed
                        throw e;
                    }
                }
            });
    }

    /* Main */

    public static void main(String[] args) throws IOException
    {
        Options opt = Options.V();

        JCommander commander = new JCommander(opt, args);

        // Print usage and exit
        if (opt.getHelp()) {
            commander.usage();
            System.exit(1);
        }

        logger.debug("Options: \n{}", opt);

        Path injar = opt.getSource();
        Path outjar = opt.getTarget();
        Path classdir = opt.getDestinationDir();

        Files.deleteIfExists(outjar);
        Files.createDirectories(classdir);
        deleteDirectory(classdir); // remove old contents
        Files.createDirectories(classdir);

        new Driver(injar, classdir).run();

        logger.info("Creating complemented jar: " + outjar);
        new JarExtender(injar, outjar, classdir).extend();
        
        // Remove class files
        if (opt.purgeClassFiles()) {
            logger.info("Removing temporary class files...");
            deleteDirectory(classdir);
        }
    }
}
