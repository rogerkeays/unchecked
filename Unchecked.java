package com.sun.tools.javac.comp;

import com.sun.source.util.*;
import com.sun.source.tree.*;
import com.sun.tools.javac.api.*;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.JCDiagnostic.Warning;
import com.sun.tools.javac.tree.TreeMaker;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.io.InputStream;
import jdk.internal.misc.Unsafe;

public class Unchecked implements Plugin {
    @Override 
    public void init(JavacTask task, String... args) {
        try {
            // open access to compiler internals, bypassing module restrictions
            Module unnamedModule = Unchecked.class.getModule();
            Module compilerModule = ModuleLayer.boot().findModule("jdk.compiler").get();
            Module baseModule = ModuleLayer.boot().findModule("java.base").get();
            Method open = Module.class.getDeclaredMethod("implAddOpens", String.class, Module.class);
            Field f = Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true);
            Unsafe unsafe = (Unsafe) f.get(null);
            unsafe.putBoolean(open, 12, true); // make implAddOpens public
            open.invoke(compilerModule, "com.sun.tools.javac.api", unnamedModule);
            open.invoke(compilerModule, "com.sun.tools.javac.comp", unnamedModule);
            open.invoke(compilerModule, "com.sun.tools.javac.main", unnamedModule);
            open.invoke(compilerModule, "com.sun.tools.javac.tree", unnamedModule);
            open.invoke(compilerModule, "com.sun.tools.javac.util", unnamedModule);
            open.invoke(baseModule, "java.lang", unnamedModule);

            // patch extended classes into the compiler context
            Context context = ((BasicJavacTask) task).getContext();
            Object chk = instance(reload(NoCheck.class, context), context);
            Object log = instance(reload(UncheckedLog.class, context), context);
            inject(JavaCompiler.class, "log", log, context);
            inject(Flow.class, "chk", chk, context);
            inject(Flow.class, "log", log, context);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // reload a class using the jdk.compiler classloader
    // this is necessary to be considered part of the same package
    // otherwise we cannot override package/protected methods
    Class reload(Class klass, Context context) throws Exception {
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

    public static class NoCheck extends Check {
        protected NoCheck(Context context) {
            super(context);
        }
        public static NoCheck instance(Context context) {
            context.put(checkKey, (Check) null); // superclass constructor will put it back
            return new NoCheck(context);
        }

        // treat all exceptions as unchecked
        @Override boolean isUnchecked(Type t) { return true; }
    }

    public static class UncheckedLog extends Log {
        Context context;

        protected UncheckedLog(Context context) {
            super(context);
            this.context = context;
        }
        public static UncheckedLog instance(Context context) {
            context.put(logKey, (Log) null); // superclass constructor will put it back
            return new UncheckedLog(context);
        }

        // suppress invalid warnings
        @Override
        public void warning(DiagnosticPosition pos, Warning warningKey) {
            if (!warningKey.key().startsWith("compiler.warn.unreachable.catch")) {
                super.warning(pos, warningKey);
            }
        }
    }

    @Override public String getName() { return "unchecked"; }
}
