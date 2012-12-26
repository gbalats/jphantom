package jphantom;

import jphantom.param.*;
import java.util.*;
import java.nio.file.*;
import com.beust.jcommander.*;

public class Options {

    private final static FileSystem fs = FileSystems.getDefault();
    private final Path outDirDefault = new PathConverter().convert("out/phantoms/");

    private final static Options INSTANCE = new Options();

    public static Options V() {
        return INSTANCE;
    }

    protected Options() {}

    @Parameter(names = { "-log", "-verbose" }, 
               description = "Level of verbosity",
               validateWith = LevelValidator.class)
    private Integer lvl = 3;

    @Parameter(names = "-debug", description = "Debug mode")
    private boolean debug = false;

    @Parameter
    private List<String> parameters = new ArrayList<>();

    @Parameter(names = { "-d", "--outputDirectory" }, 
               description = "Class File Directory (implies --save-classes)", 
               converter = PathConverter.class, 
               validateWith = DirectoryValidator.class)
    private Path outDir;

    @Parameter(names = "--save-class-files", description = "Save class files")
    private boolean saveClasses = false;

    @Parameter(names = "--help", description = "Help")
    private boolean help;

    @Parameter(names = { "-t", "--target"}, 
               description = "Destination", 
               // TODO: validateWith = JarValidator.class,
               converter = PathConverter.class)
    private Path target = fs.getPath("out.jar");

    @Parameter(names = {"-s", "--source"}, 
               description = "Source", 
               // TODO: validateWith = JarValidator.class,
               required = true,
               converter = PathConverter.class)
    private Path source;

    public Path getDestinationDir() {
        return outDir == null ? outDirDefault : outDir;
    }

    public Path getSource() {
        return source;
    }

    public Path getTarget() {
        return target;
    }

    public boolean getHelp() {
        return help;
    }

    public int getLevel() {
        return lvl;
    }

    public boolean purgeClassFiles() {
        return !saveClasses && outDir == null;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        Path outDir = getDestinationDir();

        builder.append("  Destination directory: ").append(outDir).append('\n');
        builder.append("  Logging Level: ").append(lvl).append('\n');
        builder.append("  Source Jar File: ").append(source).append('\n');
        builder.append("  Target Jar File: ").append(target).append('\n');

        return builder.toString();
    }

}
