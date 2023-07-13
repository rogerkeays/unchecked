#!/bin/sh

USAGE="
usage: $0 action

valid actions:
  clean
  compile
  jar
  test-warnings [-all] run tests expected to show warnings
  test-errors [-all]   run tests expected to show errors
  test [-all]          run all tests
  install              install locally with maven
  bundle               bundle for release to central

use -all to test against all JDKs in \$HOME/tools
"
# build configuration
GROUP=jamaica
NAME=unchecked
VERSION=0.4.0
TARGET=9

PACKAGE=jamaica.unchecked
CLASSNAME=Unchecked
JAR=$NAME.jar
PLUGIN=-Xplugin:unchecked
#PLUGIN=-Xplugin:"unchecked nowarn"
TEST_OPTS=-J--add-opens=java.base/java.lang=ALL-UNNAMED 
TEST_CLASSPATH=$JAR

# location of jdk for building
[ ! "$JAVA_HOME" ] && JAVA_HOME="$(dirname $(dirname $(readlink -f $(which javac))))"

# directories containing jdks to test against, separated by spaces
JDKS="$JAVA_HOME"
[ "$2" = "-all" ] && JDKS="$HOME/tools/jdk-*"

# build code starts here
[ ! $1 ] && echo "$USAGE" && exit 1;

echo ">>> clean"
[ -d target ] && rm -r target
[ $1 = "clean" ] && exit 0

# note: -source 8 is required to import com.sun.tools.javac.*
echo ">>> compile ($JAVA_HOME)"
$JAVA_HOME/bin/javac -Xlint:unchecked -nowarn -source 8 -target $TARGET -d target $CLASSNAME.java
[ $? -eq 0 ] || exit 1
[ $1 = "compile" ] && exit 0

echo ">>> jar"
mkdir -p target/META-INF/services
echo "$PACKAGE.$CLASSNAME" > target/META-INF/services/com.sun.source.util.Plugin
cd target; $JAVA_HOME/bin/jar --create --file ../$JAR *; cd ..
[ $1 = "jar" ] && exit 0

for JDK in $JDKS; do
    echo ">>> test-warnings ($JDK)"
    "$JDK"/bin/javac -cp $TEST_CLASSPATH -d target "$PLUGIN" $TEST_OPTS TestWarnings.java
    [ $? -eq 0 ] || exit 1
    "$JDK"/bin/java -cp target -enableassertions TestWarnings
    [ $? -eq 0 ] || exit 1
done
[ $1 = "test-warnings" ] && exit 0

for JDK in $JDKS; do
    echo ">>> test-errors ($JDK)"
    "$JDK"/bin/javac -cp $TEST_CLASSPATH -d target "$PLUGIN" $TEST_OPTS TestAttrErrors.java
    "$JDK"/bin/javac -cp $TEST_CLASSPATH -d target "$PLUGIN" $TEST_OPTS TestFlowErrors.java
done
[ $1 = "test-errors" ] && exit 0
[ $1 = "test" ] && exit 0

echo ">>> install"
mvn install:install-file -DgroupId=$GROUP -DartifactId=$NAME -Dversion=$VERSION -Dpackaging=jar -Dfile=$JAR
[ $? -eq 0 ] || exit 1
[ $1 = "test-errors" ] && exit 0

