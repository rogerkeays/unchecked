#!/bin/sh

USAGE="
usage: $0 action [-all]

valid actions:
  clean
  compile
  jar
  test-valid     run tests on standard java code with no warnings or errors
  test-warnings  run tests expected to show warnings
  test-errors    run tests expected to show errors
  test           run all tests
  install        install locally with maven
  bundle         bundle for release to central

use -all to test against all JDKs in \$HOME/tools
"
# project configuration
GROUP="io.github.rogerkeays"
ARTIFACT="unchecked"
VERSION="0.4.3"
DESCRIPTION="Say goodbye to checked exceptions forever"
LICENSE="MIT License"
DEVELOPER="Roger Keays"
GITHUB_NAME="rogerkeays"
EMAIL="realguybrush@vivaldi.net"

# build configuration
TARGET=9
PACKAGE="jamaica.unchecked"
CLASSNAME="Unchecked"
JAR="target/$ARTIFACT.jar"
PLUGIN="-Xplugin:unchecked"
#PLUGIN=-Xplugin:"unchecked nowarn"
TEST_OPTS="-J--add-opens=java.base/java.lang=ALL-UNNAMED "
TEST_CLASSPATH="$JAR"

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
cd target; $JAVA_HOME/bin/jar -cf ../$JAR *; cd ..
[ $1 = "jar" ] && exit 0

for JDK in $JDKS; do
    echo ">>> test-valid ($JDK)"
    "$JDK"/bin/javac -cp $TEST_CLASSPATH -d target "$PLUGIN" $TEST_OPTS TestValid.java
    [ $? -eq 0 ] || exit 1
    "$JDK"/bin/java -cp target -enableassertions TestValid
    [ $? -eq 0 ] || exit 1
done
[ $1 = "test-valid" ] && exit 0

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
mvn install:install-file -DgroupId=$GROUP -DartifactId=$ARTIFACT -Dversion=$VERSION -Dpackaging=jar -Dfile=$JAR
[ $? -eq 0 ] || exit 1
[ $1 = "install" ] && exit 0

echo ">>> bundle"
mkdir target/bundle
echo "<?xml version='1.0' encoding='UTF-8'?>
<project xmlns='http://maven.apache.org/POM/4.0.0'
         xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'
         xsi:schemaLocation='http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd'>

  <modelVersion>4.0.0</modelVersion>
  <groupId>$GROUP</groupId>
  <artifactId>$ARTIFACT</artifactId>
  <packaging>jar</packaging>
  <version>$VERSION</version>

  <name>$CLASSNAME</name>
  <description>$DESCRIPTION</description>
  <url>https://github.com/$GITHUB_NAME/$ARTIFACT</url>

  <licenses>
    <license>
      <name>$LICENSE</name>
      <url>https://github.com/$GITHUB_NAME/$ARTIFACT/LICENSE</url>
      <distribution>repo</distribution>
    </license>
  </licenses>

  <developers>
    <developer>
      <name>$DEVELOPER</name>
      <email>$EMAIL</email>
      <organization>Individual</organization>
      <organizationUrl>https://github.com/$GITHUB_NAME</organizationUrl>
    </developer>
  </developers>

  <scm>
    <connection>scm:git:git://github.com/$GITHUB_NAME/$ARTIFACT.git</connection>
    <developerConnection>scm:git:ssh://github.com:$GITHUB_NAME/$ARTIFACT.git</developerConnection>
    <url>https://github.com/$GITHUB_NAME/$ARTIFACT/tree/master</url>
  </scm>
</project>
" >> target/bundle/$ARTIFACT-$VERSION.pom
cp $JAR target/bundle/$ARTIFACT-$VERSION.jar
$JAVA_HOME/bin/jar -cf target/bundle/$ARTIFACT-$VERSION-sources.jar *.java
$JAVA_HOME/bin/javadoc --source 8 -d target/javadoc $CLASSNAME.java
cd target/javadoc
$JAVA_HOME/bin/jar -cf ../bundle/$ARTIFACT-$VERSION-javadoc.jar *
cd ../bundle
for i in *; do gpg -ab $i; done
for i in *; do
  md5sum $i > $i.md5
  sha1sum $i > $i.sha1
  sha256sum $i > $i.sha256
  sha512sum $i > $i.sha512
done
$JAVA_HOME/bin/jar -cf ../$ARTIFACT-$VERSION.bundle.jar *

