package org.clyze.jphantom;

import java.nio.file.*;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.objectweb.asm.Opcodes;
import org.slf4j.LoggerFactory;
import org.kohsuke.args4j.*;
import org.kohsuke.args4j.spi.*;
import static ch.qos.logback.classic.Level.*;

public class Options {
    private final static FileSystem fs = FileSystems.getDefault();
    private final static Logger logger = (Logger) LoggerFactory.getLogger("jphantom");
    private final static Level[] levels = {OFF, ERROR, WARN, INFO, DEBUG, TRACE, ALL};
    private final static Options INSTANCE = new Options();
    public static final int ASM_VER = Opcodes.ASM9;

    public static Options V() {
        return INSTANCE;
    }

    protected Options() {}

    static {
        logger.setLevel(INSTANCE.logLevel);
    }

    @Option(name = "-v",
            aliases = { "--log", "--verbose" }, 
            handler = LevelOptionHandler.class,
            usage = "Level of verbosity")
    private Level logLevel = INFO;

    @Option(name = "--debug", usage = "Debug mode")
    private boolean debug = false;

    @Option(name = "-d", 
            metaVar = "<dir>",
            usage = "Phantom-classes destination directory", 
            handler = DirectoryOptionHandler.class)
    private Path outDir = fs.getPath("out/phantoms/");

    @Option(name = "--save-class-files", usage = "Save phantom class files")
    private boolean saveClasses = false;

    @Option(name = "--help", usage = "Help")
    private boolean help = false;

    @Option(name = "-o", metaVar = "<outjar>",
            usage = "the destination path of the complemented jar", 
            handler = PathOptionHandler.class)
    private Path target = fs.getPath("out.jar");

    @Argument(required = true, metaVar = "<injar>",
              usage = "the jar to be complemented", 
              handler = PathOptionHandler.class)
    private Path source;

    public Path getDestinationDir() {
        return outDir;
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

    public Level getLevel() {
        return logLevel;
    }

    public boolean purgeClassFiles() {
        return !saveClasses;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        Path outDir = getDestinationDir();

        builder.append("  Destination directory: ").append(outDir).append('\n');
        builder.append("  Logging Level: ").append(logLevel).append('\n');
        builder.append("  Source Jar File: ").append(source).append('\n');
        builder.append("  Target Jar File: ").append(target).append('\n');

        return builder.toString();
    }

    public static class PathOptionHandler extends OptionHandler<Path>
    {
        public PathOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super Path> setter) {
            super(parser, option, setter);
        }

        @Override
        public int parseArguments(Parameters params) throws CmdLineException {
            try {
                Path path = fs.getPath(params.getParameter(0));
                
                setter.addValue(path);
            } catch (InvalidPathException exc) {
                throw new CmdLineException(owner, exc);
            }
            return 1;
        }

        @Override
        public String getDefaultMetaVariable() {
            return "<path>";
        }
    }

    public static class DirectoryOptionHandler extends PathOptionHandler
    {
        public DirectoryOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super Path> setter) {
            super(parser, option, setter);
        }

        @Override
        public int parseArguments(Parameters params) throws CmdLineException {
            try {
                Path dir = fs.getPath(params.getParameter(0));
            
                if (Files.exists(dir) && !Files.isDirectory(dir))
                    throw new CmdLineException(
                        owner, "'" + params.getParameter(0) + "' is not a directory");    

                setter.addValue(dir);
            } catch (InvalidPathException exc) {
                throw new CmdLineException(owner, exc);
            }
            return 1;
        }

        @Override
        public String getDefaultMetaVariable() {
            return "<dir>";
        }
    }

    public static class LevelOptionHandler extends OptionHandler<Level>
    {
        public LevelOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super Level> setter) {
            super(parser, option, setter);
        }

        @Override
        public int parseArguments(Parameters params) throws CmdLineException {
            try {
                int i, start = 0, end = levels.length - 1;
                
                i = Integer.parseInt(params.getParameter(0));

                if (i < start || i > end)
                    throw new CmdLineException(
                        owner, i + " out of [" + start + "," + end + "] range");

                Level lvl = levels[i];

                setter.addValue(lvl);
                assert Driver.logger != null;
                logger.setLevel(lvl);
            } catch (NumberFormatException exc) {
                throw new CmdLineException(owner, exc);
            }
            return 1;
        }

        @Override
        public String getDefaultMetaVariable() {
            return "N";
        }
    }
}
