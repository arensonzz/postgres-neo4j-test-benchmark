# Parts taken from: https://stackoverflow.com/a/27768965
#
# Build Stage
#
FROM maven:3.9.0-eclipse-temurin-17-alpine AS build

RUN mkdir -p /home/app/src
COPY pom.xml /home/app

RUN mvn -f /home/app/pom.xml dependency:resolve
COPY ./src /home/app/src
RUN mvn -f /home/app/pom.xml clean package

#
# Package Stage
#
FROM eclipse-temurin:17-jre-alpine
COPY --from=build /home/app/target/postgres-neo4j-test-benchmark-1.0-SNAPSHOT.jar /usr/local/lib/demo.jar
# Copy data files into JAR directory
ENTRYPOINT ["java","-jar","/usr/local/lib/demo.jar"]
