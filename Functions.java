package jamaica.unchecked;

import java.util.function.*;
import java.util.concurrent.Callable;

/**
 * Functional wrappers which rethrow all exceptions as unchecked exceptions,
 * and a utility method to rethrow checked exceptions as unchecked exceptions
 * from your procedural code.
 *
 * This is largely superseded by the compiler plugin, but is left here 
 * because it is still useful for cases when you can't use the plugin.
 *
 * ## Functional Examples:
 * 
 * Before:
 * 
 *     List.of("LICENSE", "README.md", "unchecked.java").stream()
 *         .map(file -> {
 *             try {
 *                 return(file + ": " + Files.lines(Paths.get(file)).count());
 *             } catch (IOException e) {
 *                 throw new RuntimeException(e); // java made me do it
 *             }
 *         })
 *         .toList();
 * 
 * After (exceptions will propagate up the call stack):
 * 
 *     List.of("LICENSE", "README.md", "unchecked.java").stream()
 *         .map(unchecked(file -> file + ": " + Files.lines(Paths.get(file)).count()))
 *         .toList();
 * 
 * You can use `uc` instead of `unchecked` if you prefer:
 * 
 *     List.of("LICENSE", "README.md", "unchecked.java").stream()
 *         .map(uc(file -> file + ": " + Files.lines(Paths.get(file)).count()))
 *         .toList();
 * 
 * Note, consumer functions must use `uncheckedconsumer` or `ucc`:
 * 
 *     List.of("LICENSE", "README.md", "unchecked.java")
 *         .forEach(uncheckedconsumer(file -> 
 *             System.out.println(file + ": " + Files.lines(Paths.get(file)).count())));
 * 
 *     List.of("LICENSE", "README.md", "unchecked.java")
 *         .forEach(ucc(file -> 
 *             System.out.println(file + ": " + Files.lines(Paths.get(file)).count())));
 * 
 * ## Procedural Examples
 * 
 * When you can't handle a checked exception, a common practise is to rethrow it
 * as a RuntimeException:
 * 
 *     public static void rm() {
 *         try {
 *             Files.delete(Paths.get("unchecked.kt"));
 *         } catch (IOException e) {
 *             throw new RuntimeException(e);
 *         }
 *     }
 * 
 *     |  Exception java.lang.RuntimeException: java.nio.file.NoSuchFileException: unchecked.kt
 *     |        at rm (#22:5)
 *     |        at (#23:1)
 *     |  Caused by: java.nio.file.NoSuchFileException: unchecked.kt
 *     |        at UnixException.translateToIOException (UnixException.java:92)
 *     |        at UnixException.rethrowAsIOException (UnixException.java:106)
 *     |        at UnixException.rethrowAsIOException (UnixException.java:111)
 *     |        at UnixFileSystemProvider.implDelete (UnixFileSystemProvider.java:248)
 *     |        at AbstractFileSystemProvider.delete (AbstractFileSystemProvider.java:105)
 *     |        at Files.delete (Files.java:1152)
 *     |        at rm (#22:3)
 *     |        ...
 * 
 * With `unchecked`, you can throw checked exceptions without wrapping them or
 * declaring them in the method signature:
 * 
 *     public static void rm() {
 *         try {
 *             Files.delete(Paths.get("unchecked.kt"));
 *         } catch (IOException e) {
 *             throw unchecked(e);
 *         }
 *     }
 * 
 *     |  Exception java.nio.file.NoSuchFileException: unchecked.kt
 *     |        at UnixException.translateToIOException (UnixException.java:92)
 *     |        at UnixException.rethrowAsIOException (UnixException.java:106)
 *     |        at UnixException.rethrowAsIOException (UnixException.java:111)
 *     |        at UnixFileSystemProvider.implDelete (UnixFileSystemProvider.java:248)
 *     |        at AbstractFileSystemProvider.delete (AbstractFileSystemProvider.java:105)
 *     |        at Files.delete (Files.java:1152)
 *     |        at rm (#20:3)
 *     |        at (#21:1)
 * 
 * This is possible because of Java's runtime type erasure. See the code for
 * implementation details.
 * 
 * ## Installation
 * 
 * `unchecked` is a single java source file that you can drop into your project.
 * 
 * Copy the java source to your project:
 * 
 *     mkdir -p src/jamaica/unchecked
 *     cd src/jamaica/unchecked
 *     wget https://github.com/rogerkeays/unchecked/raw/main/Functions.java
 * 
 * Finally, import to use in your code:
 * 
 *     import static jamaica.unchecked.Functions.*;
 * 
 * ## Testing
 * 
 * Tests are run by executing the `main` method of this class . No output means
 * the tests ran successfully.  If you want to run the tests from your own
 * build system, make sure assertions are enabled with the `java -ea` switch.
 * 
 * ## Related Resources
 * 
 *  - Lombok @SneakyThrows: https://projectlombok.org/features/SneakyThrows
 *  - Function type inference discussion on Stack Overflow: https://stackoverflow.com/questions/71276582/why-does-java-type-inference-fail-to-distinguish-between-function-and-consumer
 */
public class Functions {

    // testable specification
    public static void main(String [] args) {
        assert unchecked(() -> {}) instanceof Runnable;
        assert unchecked(() -> $exception()) instanceof Runnable;
        assert unchecked(() -> { throw new Exception(); }) instanceof Callable; // dumb: should be Runnable
        assert unchecked(() -> 1) instanceof Callable;
        assert unchecked(x -> x) instanceof Function;
        assert unchecked(x -> { return $int(); }) instanceof Function;
        assert unchecked((ThrowingFunction<Object,Integer,Exception>) (x -> $int())) instanceof Function;
        assert uncheckedconsumer(x -> {}) instanceof Consumer;
        assert uncheckedconsumer(x -> { return; }) instanceof Consumer;
        assert uncheckedconsumer(x -> { $void(); }) instanceof Consumer;
        assert uncheckedconsumer(x -> $void()) instanceof Consumer;
        assert uncheckedconsumer((ThrowingConsumer<Object,Exception>) (x -> $void())) instanceof Consumer;
        assert uncheckedconsumer((Object x) -> $void()) instanceof Consumer;
    }
    private static void $void() {}
    private static int $int() { return 1; }
    private static void $exception() throws Exception { throw new Exception(); }
    private static void $test_throws() {
        throw unchecked(new Exception()); // no `throws Exception` necessary
    }

    // replacements for the standard java functional interfaces which allow for exceptions 
    public interface ThrowingRunnable<E extends Exception> { void apply() throws E; }
    public interface ThrowingCallable<R,E extends Exception> { R apply() throws E; }
    public interface ThrowingConsumer <T,E extends Exception> { void apply(T t) throws E; }
    public interface ThrowingFunction<T,R,E extends Exception> { R apply(T t) throws E; }

    // functional wrappers which rethrow all exceptions as unchecked exceptions
    // consumers use a different name because java specifies they must also accept functions
    public static <E extends Exception> Runnable unchecked(ThrowingRunnable<E> f) {
        return () -> { try { f.apply(); } catch (Exception e) { throw unchecked(e); } }; }
    public static <R,E extends Exception> Callable<R> unchecked(ThrowingCallable<R,E> f) {
        return () -> { try { return f.apply(); } catch (Exception e) { throw unchecked(e); } }; }
    public static <T,E extends Exception> Consumer<T> uncheckedconsumer(ThrowingConsumer<T,E> f) {
        return t -> { try { f.apply(t); } catch (Exception e) { throw unchecked(e); } }; }
    public static <T,R,E extends Exception> Function<T,R> unchecked(ThrowingFunction<T,R,E> f) {
        return t -> { try { return f.apply(t); } catch (Exception e) { throw unchecked(e); } }; }

    // shorthand methods
    public static <E extends Exception> Runnable uc(ThrowingRunnable<E> f) { return unchecked(f); }
    public static <R,E extends Exception> Callable<R> uc(ThrowingCallable<R,E> f) { return unchecked(f); }
    public static <T,E extends Exception> Consumer<T> ucc(ThrowingConsumer<T,E> f) { return uncheckedconsumer(f); }
    public static <T,R,E extends Exception> Function<T,R> uc(ThrowingFunction<T,R,E> f) { return unchecked(f); }

    // throw a checked exception as an unchecked exception by exploiting type erasure
    public static RuntimeException unchecked(Exception e) {
        Functions.<RuntimeException>throw_checked(e); 
        return null; 
    }
    private static <E extends Exception> void throw_checked(Exception e) throws E { throw (E) e; }
}

