# Unchecked Java: Say Goodbye to Checked Exceptions Forever

*Unchecked* allows you to treat Java's checked exceptions as though they were
unchecked.

Before:

    List.of("LICENSE", "README.md", "Unchecked.java").stream()
        .map(file -> {
            try {
                return(file + ": " + Files.lines(Paths.get(file)).count());
            } catch (IOException e) {
                throw new RuntimeException(e); // java made me do it
            }
        })
        .toList();

After:

    List.of("LICENSE", "README.md", "Unchecked.java").stream()
        .map(file -> file + ": " + Files.lines(Paths.get(file)).count())
        .toList();

When you can't handle a checked exception, a common practise is to rethrow it
as a RuntimeException:

    public static void rm() {
        try {
            Files.delete(Paths.get("unchecked.kt"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

Which causes a chain of exceptions, nested inside one another:

    |  jshell> rm();
    |  Exception java.lang.RuntimeException: java.nio.file.NoSuchFileException: unchecked.kt
    |        at rm (#22:5)
    |        at (#23:1)
    |  Caused by: java.nio.file.NoSuchFileException: unchecked.kt
    |        at UnixException.translateToIOException (UnixException.java:92)
    |        at UnixException.rethrowAsIOException (UnixException.java:106)
    |        at UnixException.rethrowAsIOException (UnixException.java:111)
    |        at UnixFileSystemProvider.implDelete (UnixFileSystemProvider.java:248)
    |        at AbstractFileSystemProvider.delete (AbstractFileSystemProvider.java:105)
    |        at Files.delete (Files.java:1152)
    |        at rm (#22:3)
    |        ...

With *Unchecked*, you can let checked exceptions go back up the call stack. You are not obliged to declare exceptions in the method signature, and the exception stack does not become polluted:

    public static void rm() {
        Files.delete(Paths.get("unchecked.kt"));
    }

    |  jshell> rm();
    |  Exception java.nio.file.NoSuchFileException: unchecked.kt
    |        at UnixException.translateToIOException (UnixException.java:92)
    |        at UnixException.rethrowAsIOException (UnixException.java:106)
    |        at UnixException.rethrowAsIOException (UnixException.java:111)
    |        at UnixFileSystemProvider.implDelete (UnixFileSystemProvider.java:248)
    |        at AbstractFileSystemProvider.delete (AbstractFileSystemProvider.java:105)
    |        at Files.delete (Files.java:1152)
    |        at rm (#20:3)
    |        at (#21:1)

*Unchecked* is invoked as a compiler plugin and has no runtime dependencies.

## Quick Start

Download the jar, place it on your classpath, and run `javac` with `-Xplugin:unchecked` and `-J--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED`:

    wget https://github.com/rogerkeays/unchecked/raw/main/unchecked.jar
    javac -cp unchecked.jar -Xplugin:unchecked -J--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED Test.java

Run your code like you always have:

    java Test

## Install Using Maven

*Unchecked* is not yet available on Maven Central, however you can install it locally like this:

    wget https://github.com/rogerkeays/unchecked/raw/main/unchecked.jar
    mvn install:install-file -DgroupId=jamaica -DartifactId=unchecked -Dversion=0.2.0 -Dpackaging=jar -Dfile=unchecked.jar
    
Next, add the dependency to your `pom.xml`:

    <dependency>
      <groupId>jamaica</groupId>
      <artifactId>unchecked</artifactId>
      <version>0.2.0</version>
      <scope>compile</scope>
    </dependency>

And configure the compiler plugin:

    <build>
      <plugins>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-compiler-plugin</artifactId>
          <version>3.11.0</version>
          <configuration>
            <compilerArgs>
              <arg>-Xplugin:unchecked</arg>
              <arg>-J--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED</arg>
            </compilerArgs>
            <fork>true</fork>
            ...
          </configuration>
        </plugin>

Note, older versions of the compiler plugin use a different syntax. Refer to the [Maven Compiler Plugin docs](https://maven.apache.org/plugins/maven-compiler-plugin/compile-mojo.html) for more details. Make sure you add the `<fork>true</fork>` option too.

## Build It Yourself

*Unchecked* is built using a POSIX shell script:

    git clone https://github.com/rogerkeays/unchecked.git
    cd unchecked
    ./build.sh

If your operating system doesn't include `sh` it shouldn't be too hard to convert to whatever shell you are using. I mean, we're talking about one java file and a text file here.

## JDK Support

*Unchecked* is tested with the following JDKs:

  * jdk-09.0.4
  * jdk-10.0.2
  * jdk-11.0.8
  * jdk-12.0.2
  * jdk-13.0.2
  * jdk-14.0.2
  * jdk-15.0.2
  * jdk-16.0.2
  * jdk-17.0.2
  * jdk-18.0.2.1
  * jdk-19.0.2
  * jdk-20.0.1
  * jdk-21 (early access)
  * jdk-22 (early access)

## IDE Support

There is currently no IDE support for *Unchecked*. Contributions are welcome. Other projects such as Lombok and Manifold have the same feature, so you may be able to use their plugins.

## Known Issues

  * *Unchecked* may not be compatible with other `javac` plugins, though so far it seems to play nice with Lombok and [Fluent](https://github.com/rogerkeays/fluent), at least.

Please submit issues to the [github issue tracker](https://github.com/rogerkeays/unchecked/issues).

## Related Resources

  * [kotlin](https://kotlinlang.org): a JVM language which supports extension methods out of the box.
  * [Lombok](https://github.com/projectlombok/lombok): the grand-daddy of `javac` hacks, with various tools for handling checked exceptions.
  * [Manifold](https://manifold.systems): a `javac` plugin with many features, including disabling checked exceptions.
  * [Checked exception helper functions](https://github.com/rogerkeays/jamaica-core/blob/master/src/exceptions.java): for when you can't use a compiler plugin.
  * [Fluent](https://github.com/rogerkeays/fluent): a similar compiler plugin to support static extension methods in Java.
  * [More solutions looking for a problem](https://rogerkeays.com)

