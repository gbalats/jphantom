package jphantom.util;

public class BootstrapClassLoader extends ClassLoader {
    protected BootstrapClassLoader() {
        /*
         * The default classloader implementation will use the bootstrap loader
         * if it finds a null parent.
         */
        super(null);
    }

    private static final BootstrapClassLoader INSTANCE = 
        new BootstrapClassLoader();

    public static BootstrapClassLoader v() { return INSTANCE; }
}
