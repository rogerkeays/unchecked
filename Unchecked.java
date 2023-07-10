package jamaica.unchecked;

import com.sun.source.util.*;
import com.sun.tools.javac.api.*;
import com.sun.tools.javac.comp.*;
import com.sun.tools.javac.util.*;
import java.lang.reflect.*;
import java.util.Map;

public class Unchecked implements Plugin {

    @Override
    public void init(final JavacTask task, final String... args) {
        try {
            // open access to the compiler packages
            // requires -J--add-opens=java.base/java.lang=ALL-UNNAMED
            Module unnamedModule = Unchecked.class.getModule();
            Module compilerModule = ModuleLayer.boot().findModule("jdk.compiler").get();
            Module baseModule = ModuleLayer.boot().findModule("java.base").get();
            Method opener = Module.class.getDeclaredMethod("implAddOpens", String.class, Module.class);
            opener.setAccessible(true);
            for (String packg : new String[] {
                    "com.sun.tools.javac.api",
                    "com.sun.tools.javac.code",
                    "com.sun.tools.javac.comp",
                    "com.sun.tools.javac.jvm",
                    "com.sun.tools.javac.main",
                    "com.sun.tools.javac.model",
                    "com.sun.tools.javac.parser",
                    "com.sun.tools.javac.processing",
                    "com.sun.tools.javac.util",
                    "com.sun.tools.javac.util" }) {
                opener.invoke(compilerModule, packg, unnamedModule);
            }

            // check for nowarn parameter
            boolean warn = true;
            if (args.length > 0 && args[0].equals("nowarn")) {
                warn = false;
            } else if (args.length > 0) {
                throw new IllegalArgumentException(args[0] +
                       " is not a valid plugin parameter");
            }

            // patch into the compiler context
            Context context = ((BasicJavacTask) task).getContext();
            context.put(Log.logKey, (Log) null);
            Object log = reload(UncheckedLog.class)
                    .getConstructor(Context.class, boolean.class)
                    .newInstance(context, warn);
            Map singletons = (Map) getProtected(context, "ht");
            for (Object component : singletons.values()) {
                try {
                    if (component != null) setProtected(component, "log", log);
                } catch (NoSuchFieldException e) {}
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // reload a declared class using the jdk.compiler classloader
    // this is necessary to be considered part of the same package
    // otherwise we cannot override package/protected methods
    private Class<?> reload(Class klass) throws Exception {
        java.io.InputStream is = Unchecked.class.getClassLoader().getResourceAsStream(
                klass.getName().replace('.', '/') + ".class");
        byte[] bytes = new byte[is.available()];
        is.read(bytes);
        Method defineClass = ClassLoader.class.getDeclaredMethod("defineClass", new Class[] {
                String.class, byte[].class, int.class, int.class });
        defineClass.setAccessible(true);
        try {
            return (Class) defineClass.invoke(Context.class.getClassLoader(),
                    klass.getName(), bytes, 0, bytes.length);
        } catch (InvocationTargetException e) {
            return klass; // jshell hack: class already reloaded, but no way to tell
        }
    }

    // get a value from an inaccessible field
    private Object getProtected(Object object, String field) throws Exception {
        Field f = object.getClass().getDeclaredField(field);
        f.setAccessible(true);
        return f.get(object);
    }

    // set a value for an inaccessible field
    private void setProtected(Object object, String field, Object value) throws Exception {
        Field f = object.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(object, value);
    }

    public static class UncheckedLog extends Log {
        boolean warn = true;

        public UncheckedLog(Context context, boolean warn) {
            super(context); // will register singleton
            this.warn = warn;
        }

        // convert checked exception errors to warnings, or suppress
        @Override
        public void report(JCDiagnostic diag) {
            if (diag.getCode().startsWith("compiler.err.unreported.exception")) {
                if (warn) {
                    rawWarning((int) diag.getPosition(), "warning: unreported exception " +
                          diag.getArgs()[0] + " not caught or declared to be thrown");
                }
            } else {
                super.report(diag);
            }
        }
    }

    @Override public String getName() { return "unchecked"; }
}

