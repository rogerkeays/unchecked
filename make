#!/usr/bin/env bash

VERSION=0.9.0
#JAVA_HOME=/opt/jdk1.8.0_251 # for running maven

# compile
test -d target || mkdir -p target
javac -d target unchecked.java
jar --create --file target/unchecked-$VERSION.jar -C target jamaica

# run tests
java -cp target/unchecked-$VERSION.jar jamaica.unchecked

# install to maven repo if mvn command available
command mvn && \
mvn install:install-file -Dfile=target/unchecked-$VERSION.jar -DgroupId=jamaica -DartifactId=unchecked -Dversion=$VERSION -Dpackaging=jar

