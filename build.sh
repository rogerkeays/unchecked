#!/bin/sh

# build configuration
GROUP=jamaica
NAME=unchecked
VERSION=0.2.0
TARGET=9

PACKAGE=jamaica.unchecked
CLASSNAME=Unchecked
JAR=$NAME.jar
TEST_CLASSPATH=$JAR
TEST_OPTS=-J--add-opens=java.base/java.lang=ALL-UNNAMED 

# location of jdk for building
[ ! "$JAVA_HOME" ] && JAVA_HOME="$(dirname $(dirname $(readlink -f $(which javac))))"

# directories containing jdks to test against, separated by spaces
JDKS="$JAVA_HOME"
#JDKS="$HOME/tools/jdk-*"

# compile and build jar
# note: -source 8 is required to import com.sun.tools.javac.*
echo "===== BUILDING ====="
echo $JAVA_HOME
[ -d target ] && rm -r target
mkdir -p target/META-INF/services
echo "$PACKAGE.$CLASSNAME" > target/META-INF/services/com.sun.source.util.Plugin
$JAVA_HOME/bin/javac -Xlint:unchecked -nowarn -source 8 -target $TARGET -d target $CLASSNAME.java
[ $? -eq 0 ] || exit 1
cd target; $JAVA_HOME/bin/jar --create --file ../$JAR *; cd ..

# test against all jdks
echo "\n===== TESTING ====="
echo "\n----- press enter to begin warning test cases"; read x
for JDK in $JDKS; do
    echo $JDK
    "$JDK"/bin/javac -cp $TEST_CLASSPATH -d target -Xplugin:$NAME $TEST_OPTS TestWarnings.java
    [ $? -eq 0 ] || exit 1
    "$JDK"/bin/java -cp target -enableassertions TestWarnings
    [ $? -eq 0 ] || exit 1
done
echo "\n----- press enter to begin error test cases"; read x
for JDK in $JDKS; do
    echo $JDK
    "$JDK"/bin/javac -cp $TEST_CLASSPATH -d target -Xplugin:$NAME $TEST_OPTS TestErrors.java
done

# install using maven
echo "===== INSTALLING WITH MAVEN ====="
echo "\n----- press enter to install using maven"; read x
mvn install:install-file -DgroupId=$GROUP -DartifactId=$NAME -Dversion=$VERSION -Dpackaging=jar -Dfile=$JAR

