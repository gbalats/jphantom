package org.clyze.jphantom.jar;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.jar.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JarExtender
{
    private final Path in;
    private final Path out;
    private final Path dir;
    private final byte[] buffer = new byte[1024];
    private int bytesRead;

    private final Logger logger = 
        LoggerFactory.getLogger(JarExtender.class);

    public JarExtender(Path in, Path out, Path dir)
    {
        this.in = in;
        this.out = out;
        this.dir = dir;

        if (!Files.exists(in))
            throw new IllegalArgumentException();

        if (!Files.isDirectory(dir))
            throw new IllegalArgumentException("" + dir);
    }

    public void extend() throws IOException
    {

        try (JarFile injar = new JarFile(in.toFile())) {
            // Manifest jarManifest = injar.getManifest();

            try (JarOutputStream outjar = new JarOutputStream(
                    new FileOutputStream(out.toFile()))) {
                logger.info("Copying old entries...");

                // Copy the old jar
                for (JarEntry entry : Collections.list(injar.entries())) {
                    // Get an input stream for the entry.
                    InputStream entryStream = injar.getInputStream(entry);
                    entry.setCompressedSize(-1); // force java to recalculate compressed size, preventing an error 

                    // Read the entry and write it to the temp jar.
                    outjar.putNextEntry(entry);

                    while ((bytesRead = entryStream.read(buffer)) != -1)
                        outjar.write(buffer, 0, bytesRead);
                }

                // Add the complement files
                Files.walkFileTree(dir, new JarEntryCopyingVisitor(outjar));
            }
        }
    }

    private class JarEntryCopyingVisitor extends SimpleFileVisitor<Path>
    {
        private final JarOutputStream jar;

        public JarEntryCopyingVisitor(JarOutputStream jar) {
            this.jar = jar;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            throws IOException
        {

            try (InputStream stream = new FileInputStream(file.toFile())) {
                file = file.toAbsolutePath();
                file = dir.toAbsolutePath().relativize(file);
                logger.info("Adding entry: " + file);

                // Create a jar entry and add it to the temp jar.
                JarEntry entry = new JarEntry(file.toString().replace("\\", "/"));
                entry.setCompressedSize(-1); // force java to recalculate compressed size, preventing an error 
                jar.putNextEntry(entry);

                // Read the file and write it to the jar.
                while ((bytesRead = stream.read(buffer)) != -1)
                    jar.write(buffer, 0, bytesRead);
            }

            return FileVisitResult.CONTINUE;
        }
    }
}
