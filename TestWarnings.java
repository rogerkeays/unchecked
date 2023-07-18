
import java.io.*;
import java.util.function.*;
import java.util.concurrent.Callable;

public class TestWarnings {
    private static final byte[] STARS = new byte[] { 42, 42, 42 };

    public static void main(String [] args) {

        // unchecked resource handling
        OutputStream s2 = new ByteArrayOutputStream();
        s2.write((byte) 0);
        s2.close(); // still use finally in real code, please

        // procedural code ignoring exceptions
        assert new String(STARS, "UTF-8").equals("***");

        // functional code ignoring exceptions
        ((Runnable) () -> { new String(new byte[] { 42, 42, 42 }, "UTF-8"); }).run();
        assert ((Callable<Integer>) (() -> 1)).call().equals(1);
        try { ((Runnable) () -> declaredException()).run(); } catch (Exception e) {};
        try { ((Runnable) () -> undeclaredException()).run(); } catch (Exception e) {};
        try { ((Callable) () -> { throw new Exception(); }).call(); } catch (Exception e) {};

        // we don't handle this one, so it should still be displayed
        Depr.deprecated();

        // we should to be able to catch undeclared checked exceptions
        try {
        } catch (IOException e) {}

        // make sure the catch branch is actually executed
        try {
            ioException();
            assert false;
        } catch (IOException e) {
            assert true;
        }
    }

    private static int one() { return 1; }
    private static void nop() {}
    private static void declaredException() throws Exception {
        throw new Exception(); 
    }
    private static void undeclaredException() {
        throw new Exception(); // no `throws Exception` necessary
    }
    private static void ioException() {
        throw new IOException();
    }
}

class Depr {
    @Deprecated(forRemoval=true)
    public static void deprecated() {}
}

