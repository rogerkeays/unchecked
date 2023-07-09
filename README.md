# Unchecked Java: Say Goodbye to Checked Exceptions Forever

*Unchecked* allows you to treat Java's checked exceptions as though they were
unchecked.

**Before:**

    List.of("LICENSE", "README.md", "Unchecked.java").stream()
        .map(file -> {
            try {
                return(file + ": " + Files.lines(Paths.get(file)).count());
            } catch (IOException e) {
                throw new RuntimeException(e); // java made me do it
            }
        })
        .toList();

**After:**

    List.of("LICENSE", "README.md", "Unchecked.java").stream()
        .map(file -> file + ": " + Files.lines(Paths.get(file)).count())
        .toList();

When you can't handle a checked exception, a common practise is to rethrow it as a RuntimeException. The problem with this, apart from the code pollution, is that the root cause of exceptions get hidden, and sometimes lost if developers forget to retain it. With *Unchecked*, wrapping checked exceptions is longer necessary. The exception will just be passed back up the call stack.

**Before:**

    public static void rm() {
        try {
            Files.delete(Paths.get("unchecked.kt"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

**After:**

    public static void rm() {
        Files.delete(Paths.get("unchecked.kt"));
    }

*Unchecked* works by converting checked exception errors to warnings.

**Before:**

    TestValid.java:42: error: unreported exception UnsupportedEncodingException; must be caught or declared to be thrown
            assert new String(STARS, "UTF-8").equals("***");
                   ^
    1 errors

**After:**

    TestValid.java:42: warning: unreported exception java.io.UnsupportedEncodingException not caught or decalared to be thrown
            assert new String(STARS, "UTF-8").equals("***");
               ^
    1 warnings

*Unchecked* does not make any changes to your bytecode. This is possible because the JVM does not know about checked exceptions. It's been the compiler holding you back all this time.

**Before:**

    $ ls -o target/classes/org/apache/commons/lang3/stream/
    -rw-r--r--  620 Jul  8 23:42  IntStreams.class
    -rw-r--r--  254 Jul  8 23:42 'LangCollectors$1.class'
    -rw-r--r-- 3262 Jul  8 23:42 'LangCollectors$SimpleCollector.class'
    -rw-r--r-- 5223 Jul  8 23:42  LangCollectors.class
    -rw-r--r--  137 Jul  8 23:42  package-info.class
    -rw-r--r-- 3841 Jul  8 23:42 'Streams$ArrayCollector.class'
    -rw-r--r-- 2036 Jul  8 23:42 'Streams$EnumerationSpliterator.class'
    -rw-r--r-- 5314 Jul  8 23:42 'Streams$FailableStream.class'
    -rw-r--r-- 6395 Jul  8 23:42  Streams.class

**After:**

    $ ls -o target/classes/org/apache/commons/lang3/stream/
    -rw-r--r--  620 Jul  8 23:49  IntStreams.class
    -rw-r--r--  254 Jul  8 23:49 'LangCollectors$1.class'
    -rw-r--r-- 3262 Jul  8 23:49 'LangCollectors$SimpleCollector.class'
    -rw-r--r-- 5223 Jul  8 23:49  LangCollectors.class
    -rw-r--r--  137 Jul  8 23:49  package-info.class
    -rw-r--r-- 3841 Jul  8 23:49 'Streams$ArrayCollector.class'
    -rw-r--r-- 2036 Jul  8 23:49 'Streams$EnumerationSpliterator.class'
    -rw-r--r-- 5314 Jul  8 23:49 'Streams$FailableStream.class'
    -rw-r--r-- 6395 Jul  8 23:49  Streams.class

*Unchecked* is invoked as a compiler plugin and has no runtime dependencies. Warnings can be suppressed with the `nowarn` option.

## Quick Start

Download the jar, place it on your classpath, and run `javac` with `-Xplugin:unchecked` and `-J--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED`:

    wget https://github.com/rogerkeays/unchecked/raw/main/unchecked.jar
    javac -cp unchecked.jar -Xplugin:unchecked -J--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED Test.java

Run your code like you always have:

    java Test

To add the `nowarn` parameter, use this syntax:

    -Xplugin:"unchecked nowarn"

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

To add the `nowarn` parameter, use this syntax:

    <arg>-Xplugin:"unchecked nowarn"</arg>

## Build It Yourself

*Unchecked* is built using a POSIX shell script:

    git clone https://github.com/rogerkeays/unchecked.git
    cd unchecked
    ./build.sh

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

To ensure backwards compatibility with existing code, *Unchecked* has been used to compile and test the following open source projects:

  * [Apache Commons Lang](https://github.com/apache/commons-lang)
  * [iText](https://github.com/itext/itext7)
  * [Tomcat](https://github.com/apache/tomcat)

## IDE Support

There is currently no IDE support for *Unchecked*. Contributions are welcome. Other projects such as Lombok and Manifold have the same feature, so you may be able to use their plugins.

## Known Issues

  * *Unchecked* may not be compatible with other `javac` plugins, although it works with Lombok and [Fluent](https://github.com/rogerkeays/fluent), at least.
  * If you are using *Unchecked* with [Fluent](https://github.com/rogerkeays/fluent), we recommend you specify the `-Xplugin:unchecked` option first, as this is how it is tested.

Please submit issues to the [github issue tracker](https://github.com/rogerkeays/unchecked/issues). Be sure to include the JDK version and build tools you are using. A snippet of the code causing the problem will help to reproduce the bug. Before submitting, please try a clean build of your project.

## Related Resources

  * [Kotlin](https://kotlinlang.org): a JVM language which supports extension methods out of the box.
  * [Lombok](https://github.com/projectlombok/lombok): the grand-daddy of `javac` hacks, with various tools for handling checked exceptions.
  * [Manifold](https://manifold.systems): a `javac` plugin with many features, including disabling checked exceptions.
  * [Checked exception helper functions](https://github.com/rogerkeays/jamaica-core/blob/master/src/exceptions.java): for when you can't use a compiler plugin.
  * [Fluent](https://github.com/rogerkeays/fluent): a similar compiler plugin to support static extension methods in Java.
  * [More solutions looking for a problem](https://rogerkeays.com).

## Disclaimer

  * *Unchecked* is not supported or endorsed by the OpenJDK team.
  * The reasonable man adapts himself to the world. The unreasonable one persists in trying to adapt the world to himself. Therefore all progress depends on the unreasonable man. --George Bernard Shaw

