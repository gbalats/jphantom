package jphantom.param;

import java.util.*;
import java.nio.file.*;
import com.beust.jcommander.*;

public class PathConverter implements IStringConverter<Path>
{
    private FileSystem fs = FileSystems.getDefault();

    @Override
    public Path convert(String value) {
        return fs.getPath(value);
    }
}
