package com.sun.tools.javac.comp;

import com.sun.source.util.*;
import com.sun.source.tree.*;
import com.sun.tools.javac.api.*;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.jvm.*;
import com.sun.tools.javac.main.*;
import com.sun.tools.javac.processing.*;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.JCDiagnostic.*;
import com.sun.tools.javac.tree.TreeMaker;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.io.InputStream;
import jdk.internal.misc.Unsafe;

public class Unchecked implements Plugin {

    @Override
    public void init(final JavacTask task, final String... args) {
        try {
            // open access to compiler internals, bypassing module restrictions
            Module unnamedModule = Unchecked.class.getModule();
            Module compilerModule = ModuleLayer.boot().findModule("jdk.compiler").get();
            Module baseModule = ModuleLayer.boot().findModule("java.base").get();
            Method open = Module.class.getDeclaredMethod("implAddOpens", String.class, Module.class);
            Field f = Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true);
            Unsafe unsafe = (Unsafe) f.get(null);
            unsafe.putBoolean(open, 12, true); // make implAddOpens public
            for (String packg : new String[] {
                    "com.sun.tools.javac.api",
                    "com.sun.tools.javac.code",
                    "com.sun.tools.javac.comp",
                    "com.sun.tools.javac.jvm",
                    "com.sun.tools.javac.main",
                    "com.sun.tools.javac.processing",
                    "com.sun.tools.javac.tree",
                    "com.sun.tools.javac.util"}) {
                open.invoke(compilerModule, packg, unnamedModule);
            }
            open.invoke(baseModule, "java.lang", unnamedModule);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // patch extended logger into the compiler context as late as possible
        task.addTaskListener(new TaskListener() {
            public void started(TaskEvent e) {
                if (e.getKind().equals(TaskEvent.Kind.ANALYZE)) {
                    try {
                        // check for nowarn parameter
                        boolean warn = true;
                        if (args.length > 0 && args[0].equals("nowarn")) {
                            warn = false;
                        } else if (args.length > 0) {
                            throw new IllegalArgumentException(args[0] + 
                                   " is not a valid plugin parameter");
                        }

                        // patch into the compiler state
                        Context context = ((BasicJavacTask) task).getContext();
                        Object log = reload(UncheckedLog.class, context)
                                .getDeclaredMethod("instance", Context.class, boolean.class)
                                .invoke(null, context, warn);
                        inject(JavaCompiler.class, "log", log, context);
                        inject(Flow.class, "log", log, context);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
            public void finished(TaskEvent e) {}
        });
    }

    // reload a class using the jdk.compiler classloader
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

    // get the singleton of a class for a given context
    Object instance(Class<?> klass, Context context) throws Exception {
        return klass.getDeclaredMethod("instance", Context.class).invoke(null, context);
    }

    // use reflection to inject components into final/private fields
    void inject(Class klass, String field, Object value, Context context) throws Exception {
        Field f = klass.getDeclaredField(field);
        f.setAccessible(true);
        f.set(instance(klass, context), value);
    }

    public static class UncheckedLog extends Log {
        boolean warn = true;

        protected UncheckedLog(Context context, boolean warn) {
            super(context);
            this.warn = warn;
        }
        public static UncheckedLog instance(Context context, boolean warn) {
            context.put(logKey, (Log) null);
            return new UncheckedLog(context, warn);
        }

        // convert checked exception errors to warnings, or suppress
        @Override
        public void report(JCDiagnostic it) {
            if (it.getCode().startsWith("compiler.err.unreported.exception")) {
                if (warn) {
                    rawWarning((int) it.getPosition(), "warning: unreported exception " + 
                          it.getArgs()[0] + " not caught or declared to be thrown");
                }
            } else {
                super.report(it);
            }
        }
    }

    @Override public String getName() { return "unchecked"; }
}
