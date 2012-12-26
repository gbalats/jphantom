package jphantom.param;

import java.util.*;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.beust.jcommander.*;
import static ch.qos.logback.classic.Level.*;

public class LevelValidator implements IStringConverter<Level>, IParameterValidator
{
    private static final Level[] levels = {OFF, ERROR, WARN, INFO, DEBUG, TRACE, ALL};

    @Override
    public Level convert(String value) {
        Level lvl =  levels[Integer.parseInt(value)];
        Logger logger = (Logger) LoggerFactory.getLogger("jphantom");
        logger.setLevel(lvl);
        return lvl;
    }

    @Override
    public void validate(String name, String value) throws ParameterException
    {
        int i, start = 0, end = levels.length - 1;

        try {
            i = Integer.parseInt(value);
        } catch (NumberFormatException exc) {
            throw new ParameterException(name + ": must be an integer " + value);
        }

        if (i < start || i > end)
            throw new ParameterException(
                name + ": " + value + " not in range (" + start + "," + end + ")");

        Logger logger = (Logger) LoggerFactory.getLogger("jphantom");
        logger.setLevel(levels[i]);
    }
}

