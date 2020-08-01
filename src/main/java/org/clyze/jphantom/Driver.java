package org.clyze.jphantom;

import org.clyze.jphantom.adapters.ClassPhantomExtractor;
import org.clyze.jphantom.hier.ClassHierarchies;
import org.clyze.jphantom.hier.ClassHierarchy;
import org.clyze.jphantom.hier.UnmodifiableClassHierarchy;
import org.clyze.jphantom.jar.JarExtender;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

public class Driver implements Types
{
    /* Fields */

    protected final static Logger logger =
            LoggerFactory.getLogger(Driver.class);

    private final JPhantom phantom;
    private final File outDir;

    /* Constructors */
    
    public Driver(Path jar, Path dir) throws IOException {
        this(jar.toString(), dir.toFile());
    }

    public Driver(String jarname, File outDir) throws IOException
    {
        this.outDir = outDir;

        ClassHierarchy hierarchy = ClassHierarchies.fromJar(jarname);
        ClassMembers members = ClassMembers.fromJar(jarname, hierarchy);

        // Create Jar Input Stream

        JarInputStream jin = new JarInputStream(new FileInputStream(jarname));
        JarEntry entry;
        JarFile jarFile = new JarFile(jarname);
        Map<Type, ClassNode> nodes = new HashMap<>();
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
                reader.accept(new ClassPhantomExtractor(hierarchy, members), 0);

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

        phantom = new JPhantom(nodes, hierarchy, members);
    }
    

    /* Methods */

    public void run() throws IOException {
        // Generate phantom classes
        phantom.run();

        // Write generated classes to output directory
        generateFiles(outDir);
    }

    public List<File> generateFiles(File outDir) throws IOException
    {
        List<File> files = new LinkedList<>();

        for (Map.Entry<Type, byte[]> e : phantom.getGenerated().entrySet())
        {
            Type key = e.getKey();
            byte[] bytes = e.getValue();

            // Dump the class in a file

            File outFile = locationOf(outDir, key);

            if (!outFile.getParentFile().isDirectory() &&
                    !outFile.getParentFile().mkdirs())
                throw new IOException("" + outFile.getParentFile());

            try (DataOutputStream dout = new DataOutputStream(
                    new FileOutputStream(outFile))) {
                dout.write(bytes);
                dout.flush();
            }
            files.add(outFile);
        }
        return files;
    }

    public static File locationOf(File outDir, Type type) {
        return new File(outDir, type.getClassName().replace('.', '/') + ".class");
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

    private static void usage(CmdLineParser parser) {
        System.err.print("java -jar <jphantom> ");
        parser.printSingleLineUsage(System.err);
        System.err.println("\n");
        parser.printUsage(System.err);
        System.exit(1);
    }

    /* Main */

    public static void main(String[] args) throws IOException
    {
        Options bean = Options.V();

        CmdLineParser parser = new CmdLineParser(bean);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            // handling of wrong arguments
            System.err.println("Error: " + e.getMessage() + "\nUsage:\n");
            usage(parser);
        }

        // Print usage and exit
        if (bean.getHelp())
            usage(parser);

        logger.debug("Options: \n{}", bean);

        Path injar = bean.getSource();
        Path outjar = bean.getTarget();
        Path classdir = bean.getDestinationDir();

        Files.deleteIfExists(outjar);
        Files.createDirectories(classdir);
        deleteDirectory(classdir); // remove old contents
        Files.createDirectories(classdir);

        new Driver(injar, classdir).run();

        logger.info("Creating complemented jar: " + outjar);
        new JarExtender(injar, outjar, classdir).extend();
        
        // Remove class files
        if (bean.purgeClassFiles()) {
            logger.info("Removing temporary class files...");
            deleteDirectory(classdir);
        }
    }
}
