package jamaica.unchecked;

import com.sun.source.util.*;
import com.sun.tools.javac.api.*;
import com.sun.tools.javac.util.*;
import java.lang.reflect.Method;

public class Unchecked implements Plugin {

    @Override
    public void init(final JavacTask task, final String... args) {
        try {
            // open access to the compiler packages
            // requires -J--add-opens=java.base/java.lang=ALL-UNNAMED
            Module unnamedModule = Unchecked.class.getModule();
            Module compilerModule = ModuleLayer.boot().findModule("jdk.compiler").get();
            Method opener = Module.class.getDeclaredMethod("implAddOpens", String.class, Module.class);
            opener.setAccessible(true);
            opener.invoke(compilerModule, "com.sun.tools.javac.api", unnamedModule);
            opener.invoke(compilerModule, "com.sun.tools.javac.util", unnamedModule);

            // check for nowarn parameter
            boolean warn = true;
            if (args.length > 0 && args[0].equals("nowarn")) {
                warn = false;
            } else if (args.length > 0) {
                throw new IllegalArgumentException(args[0] +
                       " is not a valid plugin parameter");
            }

            // load custom diagnostic handler
            Context context = ((BasicJavacTask) task).getContext();
            new UncheckedDiagnosticHandler((Log) context.get(Log.logKey), warn);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class UncheckedDiagnosticHandler extends Log.DiagnosticHandler {
        final Log log;
        final boolean warn;

        public UncheckedDiagnosticHandler(Log log, boolean warn) {
            this.log = log;
            this.warn = warn;
            install(log);
        }

        // convert checked exception errors to warnings
        @Override
        public void report(JCDiagnostic diag) {
            if (diag.getCode().startsWith("compiler.err.unreported.exception")) {
                if (warn) {
                    log.rawWarning((int) diag.getPosition(), "warning: unreported exception " +
                          diag.getArgs()[0] + " not caught or declared to be thrown");
                }
            } else if (!diag.getCode().equals("compiler.err.except.never.thrown.in.try")) {
                prev.report(diag);
            }
        }
    }

    @Override public String getName() { return "unchecked"; }
}

