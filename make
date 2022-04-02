#!/usr/bin/env bash

VERSION=0.9.0
#JAVA_HOME=/opt/jdk1.8.0_251 # for running maven

# compile
test -d target || mkdir -p target/unchecked
javac -d target unchecked.java
jar --create --file target/unchecked-$VERSION.jar -C target unchecked 

# run tests
java -cp target/unchecked-$VERSION.jar unchecked.unchecked

# install to maven repo if mvn command available
which mvn && \
mvn install:install-file -Dfile=target/unchecked-$VERSION.jar -DartifactId=unchecked -DgroupId=unchecked -Dversion=$VERSION -Dpackaging=jar

