#!/usr/bin/env bash

VERSION=1.0.0
#JAVA_HOME=/opt/jdk1.8.0_251 # for running maven

# clean
test -d target && rm -r target

# compile
mkdir -p target
javac -d target unchecked.java
jar --create --file target/unchecked-$VERSION.jar -C target jamaica

# run tests
java -cp target/unchecked-$VERSION.jar jamaica.unchecked

# install to maven repo if mvn command available
command -v mvn && \
mvn install:install-file -Dfile=target/unchecked-$VERSION.jar -DgroupId=jamaica -DartifactId=unchecked -Dversion=$VERSION -Dpackaging=jar

