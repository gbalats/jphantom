package jphantom.exc;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

public class IllegalBytecodeException extends RuntimeException
{
    protected static String prefix = "\n   ";
    protected static final long serialVersionUID = 783453457L;
    private final Type clazz;
    private final Method method;

    protected IllegalBytecodeException(Builder builder)
    {
        super(builder.msg, builder.cause);
        this.clazz = builder.clazz;
        this.method = builder.method;
    }

    @Override
    public String getLocalizedMessage()
    {
        StringBuilder builder = new StringBuilder();
        Throwable cause = getCause();
        String msg = getMessage();

        if (msg != null)
            builder.append(msg).append(", ");

        builder
            .append("occurred while analyzing")
            .append(prefix).append("Class: ")
            .append(clazz.getClassName());

        if (method != null)
            builder.append(prefix)
                .append("Method: ")
                .append(method);

        if (cause != null)
            builder.append(prefix)
                .append("Caused by: ")
                .append(cause);

        return builder.toString();
    }

    public static class Builder
    {
        private final Type clazz;
        private Method method = null;
        private Throwable cause = null;
        private String msg = null;

        public Builder(Type clazz) {
            this.clazz = clazz;
        }

        public Builder(String name) {
            // internal name
            this.clazz = Type.getObjectType(name);
        }

        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        public Builder message(String msg) {
            this.msg = msg;
            return this;
        }

        public Builder method(Method method) {
            this.method = method;
            return this;
        }

        public Builder method(String name, String desc) {
            return method(new Method(name, desc));
        }

        public IllegalBytecodeException build() {
            return new IllegalBytecodeException(this);
        }
    }
}
