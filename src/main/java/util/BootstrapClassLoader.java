package jphantom.util;

import java.security.AccessController;
import java.security.PrivilegedAction;

public class BootstrapClassLoader extends ClassLoader {
    protected BootstrapClassLoader() {
        /*
         * The default classloader implementation will use the bootstrap loader
         * if it finds a null parent.
         */
        super(null);
    }

    private static final BootstrapClassLoader INSTANCE = 
        AccessController.doPrivileged(
            new PrivilegedAction<BootstrapClassLoader>() {
                public BootstrapClassLoader run() {
                    return new BootstrapClassLoader();
                }
            });

    public static BootstrapClassLoader v() { return INSTANCE; }
}
