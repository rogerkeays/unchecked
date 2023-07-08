
import java.io.*;
import java.util.function.*;
import java.util.concurrent.Callable;

public class TestValid {
    private static final byte[] STARS = new byte[] { 42, 42, 42 };

    public static void main(String [] args) {
    
        // procedural code with checked exception handling
        for (int i = 0; i < 1; i++) {
            try { throw new Exception(); } catch (Exception e) { if (1 == 1) break; }
            assert false;
        }

        try {
            assert new String(STARS, "UTF-8").equals("***");
        } catch (UnsupportedEncodingException e) {}

        // traditional resource handling
        OutputStream s = null;
        try {
            s = new ByteArrayOutputStream();
            s.write((byte) 0);
        } catch (IOException e) {
        } finally {
            try {
                s.close();
            } catch (IOException e) {}
        }

        // unchecked resource handling
        OutputStream s2 = new ByteArrayOutputStream();
        s2.write((byte) 0);
        s2.close(); // still use finally in real code, please

        // procedural code with unchecked exceptions
        try { int i = 0; } catch (NullPointerException e) {}

        // procedural code ignoring exceptions
        assert new String(STARS, "UTF-8").equals("***");

        // functional code without exceptions
        ((Consumer<Integer>) x -> {}).accept(1);
        ((Consumer<Integer>) x -> nop()).accept(1);
        ((Consumer<Integer>) x -> { return; }).accept(1);
        ((Consumer<Integer>) x -> { nop(); }).accept(1);
        assert ((Callable<Integer>) (() -> 1)).call().equals(1);
        assert ((Function<Integer, Integer>) (x -> x)).apply(1).equals(1);
        assert ((Function<Integer, Integer>) x -> { return one(); }).apply(5).equals(1);

        // functional code with exception handling
        ((Runnable) () -> { 
            for (int i = 0; i < 1; i++) {
                try { throw new Exception(); } catch (Exception e) { if (1 == 1) break; }
                assert false;
            }
        }).run();
        ((Runnable) () -> { 
            try {
                assert new String(STARS, "UTF-8").equals("***");
            } catch (UnsupportedEncodingException e) {} // okay, never thrown
        }).run();

        // functional code ignoring exceptions
        ((Runnable) () -> { new String(new byte[] { 42, 42, 42 }, "UTF-8"); }).run();
        try { ((Runnable) () -> declaredException()).run(); } catch (Exception e) {};
        try { ((Runnable) () -> undeclaredException()).run(); } catch (Exception e) {};
        try { ((Callable) () -> { throw new Exception(); }).call(); } catch (Exception e) {};

        // function as an anonymous class, no exceptions
    	assert new Function<String, String>() {
    		@Override public String apply(String name) {
    			return "hello " + name;
    		}
    	}.apply("world").equals("hello world");
    }

    private static int one() { return 1; }
    private static void nop() {}
    private static void declaredException() throws Exception {
        throw new Exception(); 
    }
    private static void undeclaredException() {
        throw new Exception(); // no `throws Exception` necessary
    }
}
