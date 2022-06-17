# unchecked

Evade the Java checked exception mafia.

`unchecked` provides wrapper functions to bypass checked exception handling in
your functions, and a utility method to rethrow checked exceptions as unchecked
exceptions from your procedural code.

## Lambda Examples

Before (exceptions must be handled inside the lambda):

    List.of("LICENSE", "README.md", "unchecked.java").stream()
        .map(file -> {
            try {
                return(file + ": " + Files.lines(Paths.get(file)).count());
            } catch (IOException e) {
                return(file + ": " + e.getMessage()); // java made me do it
            }
        })
        .toList();

After (exceptions can now be handled outside the lambda):

    List.of("LICENSE", "README.md", "unchecked.java").stream()
        .map(unchecked(file -> file + ": " + Files.lines(Paths.get(file)).count()))
        .toList();

You can use `uc` instead of `unchecked` if you prefer:

    List.of("LICENSE", "README.md", "unchecked.java").stream()
        .map(uc(file -> file + ": " + Files.lines(Paths.get(file)).count()))
        .toList();

Note, consumer functions must use `uncheckedconsumer` or `ucc`:

    List.of("LICENSE", "README.md", "unchecked.java")
        .forEach(uncheckedconsumer(file -> 
            System.out.println(file + ": " + Files.lines(Paths.get(file)).count())));

    List.of("LICENSE", "README.md", "unchecked.java")
        .forEach(ucc(file -> 
            System.out.println(file + ": " + Files.lines(Paths.get(file)).count())));

## Procedural Examples

When you can't handle a checked exception, a common practise is to rethrow it
as a RuntimeException:

    try {
       byte[] bytes = {'f','o','o'};
       String foo = new String(bytes, "WOOPS");
    } catch (UnsupportedEncodingException e) {
       throw new RuntimeException(e);
    }

    |  Exception java.lang.RuntimeException: java.io.UnsupportedEncodingException: WOOPS
    |        at (#9:5)
    |  Caused by: java.io.UnsupportedEncodingException: WOOPS
    |        at String.lookupCharset (String.java:819)
    |        at String.<init> (String.java:487)
    |        at String.<init> (String.java:1358)
    |        at (#9:3)

With `unchecked`, you can throw the checked exception without wrapping:

    try {
       byte[] bytes = {'f','o','o'};
       String foo = new String(bytes, "WOOPS");
    } catch (UnsupportedEncodingException e) {
       throw unchecked(e);
    }

    |  Exception java.io.UnsupportedEncodingException: WOOPS
    |        at String.lookupCharset (String.java:819)
    |        at String.<init> (String.java:487)
    |        at String.<init> (String.java:1358)
    |        at (#10:3)

This is possible because of Java's runtime type erasure. See the code for
implementation details.

## Installation

`unchecked` is a single java source file that you can drop into your project,
or install as a jar on your classpath.

Copy the java source to your project:

    mkdir src/jamaica && cd src/jamaica
    wget https://github.com/rogerkeays/unchecked/raw/main/unchecked.java

Or build as a jar:

    git clone https://github.com/rogerkeays/unchecked
    cd unchecked
    ./make

The `make` script automatically installs the jar to your maven repo if the `mvn`
command is found. To use in your maven projects, add the following dependency:

    <dependency>
      <groupId>jamaica</groupId>
      <artifactId>unchecked</artifactId>
      <version>1.0.0</version>
    </dependency>

For use with `jshell`, I recommend making a folder called `$HOME/.java/lib` and
copying or symlinking all your commonly used libraries there. Then add the
following `CLASSPATH` variable to your environment, for example in `.bashrc`:

    export CLASSPATH=$HOME/.java/lib/*

Finally, import to use in your code:

    import static jamaica.unchecked.*;

## Testing

Tests are run by the `make` script. No output means the tests ran successfully.
If you want to run the tests from your own build system, make sure assertions
are enabled with the `java -ea` switch.

## Related Resources

 - [Lombok @SneakyThrows][1]
 - For more solutions looking for a problem, visit [the authors homepage][2]

[1]: https://projectlombok.org/features/SneakyThrows
[2]: https://rogerkeays.com

