
import java.io.*;
import java.util.function.*;
import java.util.concurrent.Callable;

// this code is standard java and should produce no warnings or errors
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

        // procedural code with unchecked exceptions
        try { int i = 0; } catch (NullPointerException e) {}

        // functional code without exceptions
        ((Consumer<Integer>) x -> {}).accept(1);
        ((Consumer<Integer>) x -> nop()).accept(1);
        ((Consumer<Integer>) x -> { return; }).accept(1);
        ((Consumer<Integer>) x -> { nop(); }).accept(1);
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

        // function as an anonymous class, no exceptions
    	assert new Function<String, String>() {
    		@Override public String apply(String name) {
    			return "hello " + name;
    		}
    	}.apply("world").equals("hello world");
    }

    private static int one() { return 1; }
    private static void nop() {}
    private static void declaredRuntimeException() throws RuntimeException {
        throw new RuntimeException();
    }
    private static void undeclaredRuntimeException() {
        throw new RuntimeException();
    }
}
