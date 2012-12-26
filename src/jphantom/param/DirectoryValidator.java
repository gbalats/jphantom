package jphantom.param;

import java.util.*;
import java.nio.file.*;
import com.beust.jcommander.*;

public class DirectoryValidator implements IParameterValidator
{
    private FileSystem fs = FileSystems.getDefault();

    @Override
    public void validate(String name, String value) throws ParameterException
    {
        try {
            Path dir = fs.getPath(value);

            if (Files.exists(dir) && !Files.isDirectory(dir))
                throw new ParameterException(value + ": not a directory");
        } catch (InvalidPathException exc) {
            throw new ParameterException(value + ": not a valid path");
        }
    }     
}

