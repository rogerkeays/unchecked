
import java.io.UnsupportedEncodingException;
import java.util.function.*;
import java.util.concurrent.Callable;

public class Test {
    private static final byte[] STARS = new byte[] { 42, 42, 42 };

    public static void main(String [] args) {
    
        // procedural code with exception handling
        for (int i = 0; i < 1; i++) {
            try { throw new Exception(); } catch (Exception e) { if (1 == 1) break; }
            assert false;
        }
        try {
            assert new String(STARS, "UTF-8").equals("***");
        } catch (UnsupportedEncodingException e) {} // okay, never thrown

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

        // functional code ignoring exceptions
        ((Runnable) () -> { new String(new byte[] { 42, 42, 42 }, "UTF-8"); }).run();
        try { ((Runnable) () -> declaredException()).run(); } catch (Exception e) {};
        try { ((Runnable) () -> undeclaredException()).run(); } catch (Exception e) {};
        try { ((Callable) () -> { throw new Exception(); }).call(); } catch (Exception e) {};
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
