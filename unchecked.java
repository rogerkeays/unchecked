/**
 * MIT License
 * 
 * Copyright (c) 2022 Roger Keays
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package jamaica;

import java.util.function.*;
import java.util.concurrent.Callable;

public class unchecked {

    // replacements for the standard java functional interfaces which allow for exceptions 
    public interface ThrowingRunnable<E extends Throwable> { void apply() throws E; }
    public interface ThrowingCallable<R,E extends Throwable> { R apply() throws E; }
    public interface ThrowingConsumer <T,E extends Throwable> { void apply(T t) throws E; }
    public interface ThrowingFunction<T,R,E extends Throwable> { R apply(T t) throws E; }

    // functional wrappers which rethrow all exceptions as unchecked exceptions
    // consumers use a different name because java specifies they must also accept functions [1]
    public static <E extends Throwable> Runnable unchecked(ThrowingRunnable<E> f) {
        return () -> { try { f.apply(); } catch (Throwable e) { throw unchecked(e); } }; }
    public static <R,E extends Throwable> Callable<R> unchecked(ThrowingCallable<R,E> f) {
        return () -> { try { return f.apply(); } catch (Throwable e) { throw unchecked(e); } }; }
    public static <T,E extends Throwable> Consumer<T> uncheckedconsumer(ThrowingConsumer<T,E> f) {
        return t -> { try { f.apply(t); } catch (Throwable e) { throw unchecked(e); } }; }
    public static <T,R,E extends Throwable> Function<T,R> unchecked(ThrowingFunction<T,R,E> f) {
        return t -> { try { return f.apply(t); } catch (Throwable e) { throw unchecked(e); } }; }

    // shorthand methods
    public static <E extends Throwable> Runnable uc(ThrowingRunnable<E> f) { return unchecked(f); }
    public static <R,E extends Throwable> Callable<R> uc(ThrowingCallable<R,E> f) { return unchecked(f); }
    public static <T,E extends Throwable> Consumer<T> ucc(ThrowingConsumer<T,E> f) { return uncheckedconsumer(f); }
    public static <T,R,E extends Throwable> Function<T,R> uc(ThrowingFunction<T,R,E> f) { return unchecked(f); }

    // function wrapper tests
    private static void $exception() throws Exception { throw new Exception(); }
    private static void $void() {}
    private static int $int() { return 1; }
    private static void $test_unchecked() {
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

    // converts any exception to an unchecked exception
    public static RuntimeException unchecked(Throwable e) { return new RuntimeException(e); }

    // run tests when started from the command line
    public static void main(String [] args) {
        $test_unchecked();
    }
}

// [1]: https://stackoverflow.com/questions/71276582/why-does-java-type-inference-fail-to-distinguish-between-function-and-consumer

