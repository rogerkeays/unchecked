package jamaica.unchecked;

import com.sun.source.util.*;
import com.sun.source.tree.*;
import com.sun.tools.javac.api.*;
import com.sun.tools.javac.comp.*;
import com.sun.tools.javac.main.*;
import com.sun.tools.javac.processing.*;
import com.sun.tools.javac.util.*;
import java.lang.reflect.*;
import java.io.InputStream;

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
            opener.invoke(compilerModule, "com.sun.tools.javac.api", unnamedModule);
            opener.invoke(compilerModule, "com.sun.tools.javac.comp", unnamedModule);
            opener.invoke(compilerModule, "com.sun.tools.javac.main", unnamedModule);
            opener.invoke(compilerModule, "com.sun.tools.javac.processing", unnamedModule);
            opener.invoke(compilerModule, "com.sun.tools.javac.util", unnamedModule);

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
            Object log = reload(UncheckedLog.class, context)
                    .getDeclaredMethod("instance", Context.class, boolean.class)
                    .invoke(null, context, warn);
            for (Class component : new Class[] {
                    JavaCompiler.class,
                    Annotate.class,
                    Analyzer.class,
                    ArgumentAttr.class,
                    Attr.class,
                    Check.class,
                    DeferredAttr.class,
                    Enter.class,
                    Flow.class,
                    Infer.class,
                    LambdaToMethod.class,
                    Lower.class,
                    Modules.class,
                    MemberEnter.class,
                    Operators.class,
                    Resolve.class,
                    TypeEnter.class,
                    TransTypes.class,
                    JavacProcessingEnvironment.class }) {
                inject(component, "log", log, context);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // reload a declared class using the jdk.compiler classloader
    // this is necessary to be considered part of the same package
    // otherwise we cannot override package/protected methods
    Class<?> reload(Class klass, Context context) throws Exception {
        InputStream is = Unchecked.class.getClassLoader().getResourceAsStream(
                klass.getName().replace('.', '/') + ".class");
        byte[] bytes = new byte[is.available()];
        is.read(bytes);
        Method defineClass = ClassLoader.class.getDeclaredMethod("defineClass", new Class[] {
                String.class, byte[].class, int.class, int.class });
        defineClass.setAccessible(true);
        try {
            return (Class) defineClass.invoke(JavaCompiler.class.getClassLoader(),
                    klass.getName(), bytes, 0, bytes.length);
        } catch (InvocationTargetException e) {
            return klass; // jshell hack: class already reloaded, but no way to tell
        }
    }

    // use reflection to inject components into final/private fields
    void inject(Class klass, String field, Object value, Context context) throws Exception {
        Field f = klass.getDeclaredField(field);
        f.setAccessible(true);
        f.set(instance(klass, context), value);
    }

    // get the singleton of a class for a given context
    Object instance(Class<?> klass, Context context) throws Exception {
        return klass.getDeclaredMethod("instance", Context.class).invoke(null, context);
    }

    public static class UncheckedLog extends Log {
        boolean warn = true;

        protected UncheckedLog(Context context, boolean warn) {
            super(context);
            this.warn = warn;
        }

        public static UncheckedLog instance(Context context, boolean warn) {
            Log current = (Log) context.get(logKey);
            if (current != null && current instanceof UncheckedLog) {
                ((UncheckedLog) current).warn = warn;
                return (UncheckedLog) current;
            } else {
                // superclass constructor will register the singleton
                context.put(logKey, (Log) null);
                return new UncheckedLog(context, warn);
            }
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
