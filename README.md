# unchecked

Evade the Java checked exception mafia.

`unchecked` provides wrapper functions to bypass checked exception handling in
your functions, and a utility method to rethrow checked exceptions as unchecked
exceptions from your procedural code.

## Examples

Before (exceptions must be handled inside the lambda):

    List.of("LICENSE", "README.md", "unchecked.java").stream().map(file -> {
        try {
            return(file + ": " + Files.lines(Paths.get(file)).count());
        } catch (IOException e) {
            return(file + ": " + e.getMessage()); // java made me do it
        }
    }).toList();

After (exceptions can now be handled outside the lambda):

    List.of("LICENSE", "README.md", "unchecked.java").stream().map(unchecked(file -> 
        file + ": " + Files.lines(Paths.get(file)).count())).toList();

You can use `uc` instead of `unchecked` if you prefer:

    List.of("LICENSE", "README.md", "unchecked.java").stream().map(uc(file -> 
        file + ": " + Files.lines(Paths.get(file)).count())).toList();

Note, consumer functions must use `uncheckedconsumer` or `ucc`:

    List.of("LICENSE", "README.md", "unchecked.java").forEach(uncheckedconsumer(file -> 
        System.out.println(file + ": " + Files.lines(Paths.get(file)).count())));

    List.of("LICENSE", "README.md", "unchecked.java").forEach(ucc(file -> 
        System.out.println(file + ": " + Files.lines(Paths.get(file)).count())));

## Installation

`unchecked` is a single java source file that you can drop into your project,
or install as a jar on your classpath.

Copy the java source to your project:

    mkdir src/unchecked && cd src/unchecked
    wget https://github.com/rogerkeays/unchecked/raw/main/unchecked.java

Or build as a jar (installs to your maven repo if the `mvn` command is found):

    git clone https://github.com/rogerkeays/unchecked
    cd unchecked
    ./make

Add to your maven projects using:

    <dependency>
      <groupId>unchecked</groupId>
      <artifactId>unchecked</artifactId>
      <version>0.9</version>
    </dependency>

Import to use in your code:

    import static unchecked.unchecked.*;

## Testing

Tests are run by the `make` script. No output means the tests ran successfully.
If you want to run the tests from your own build system, make sure assertions
are enabled with the `java -ea` switch.

## Related Resources

 - [Lombok @SneakyThrows][1]
 - For more solutions looking for a problem, visit [the authors homepage][2]

[1]: https://projectlombok.org/features/SneakyThrows
[2]: https://rogerkeays.com

