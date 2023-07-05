#!/bin/sh

VERSION=2.0.0
TARGET=9

# location of jdk for building
[ ! "$JAVA_HOME" ] && JAVA_HOME="$(dirname $(dirname $(readlink -f $(which javac))))"

# directories containing jdks to test against, separated by spaces
JDKS="$JAVA_HOME"
#JDKS="$HOME/tools/jdk-*"

# javac arguments to inject the compiled plugin
WITH_PLUGIN="-Xplugin:unchecked -J--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED"

# compile and build jar
# note: -source 8 is required to import com.sun.tools.javac.*
echo "===== BUILDING ====="
echo $JAVA_HOME
[ -d target ] && rm -r target
mkdir -p target/META-INF/services
echo "com.sun.tools.javac.comp.Unchecked" > target/META-INF/services/com.sun.source.util.Plugin
$JAVA_HOME/bin/javac  -Xlint:deprecation -Xlint:unchecked -nowarn -source 8 -target $TARGET -d target Unchecked.java
[ $? -eq 0 ] || exit 1
cd target; $JAVA_HOME/bin/jar --create --file ../unchecked.jar *; cd ..

# test against all jdks
echo "\n===== TESTING ====="
for JDK in $JDKS; do
    echo $JDK
    "$JDK"/bin/javac -Xlint:deprecation -cp unchecked.jar -d target $WITH_PLUGIN Test.java
    [ $? -eq 0 ] || exit 1
    "$JDK"/bin/java -cp target -enableassertions Test
    [ $? -eq 0 ] || exit 1
done

# install using maven
echo "===== INSTALLING WITH MAVEN ====="
mvn install:install-file -DgroupId=jamaica -DartifactId=unchecked -Dversion=$VERSION -Dpackaging=jar -Dfile=unchecked.jar

