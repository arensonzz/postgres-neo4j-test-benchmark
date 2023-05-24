# Makefile for postgres-neo4j-test-benchmark
#
# @author arensonzz

# Source default environment variables from .env file
include .env

.DELETE_ON_ERROR:

# Default target
all: .env build run

#
# General Targets
#
up:
	# Start PostgreSQL and Neo4j nodes.
	docker compose up -d
	# Create volume for Maven local repository.
	docker volume create --name ${PROJECT_NAME}-maven-repo

mvn: _mvn-package _mvn-clean

run:
	# Start Java benchmark program.
	@docker run -it --network="${PROJECT_NAME}_default" --env-file=".env" -v "$(shell pwd)/jars":/usr/src/mymaven -w /usr/src/mymaven eclipse-temurin:8-jre-alpine java -jar ${PROJECT_NAME}-1.0-SNAPSHOT.jar

clean: _mvn-clean
	# Clear the "jars" directory.
	@docker run -it -v "$(shell pwd)/jars":/usr/src/tmp/jars alpine find /usr/src/tmp/jars -mindepth 1 -delete || exit 0
	@touch jars/.gitkeep

	# Stop PostgreSQL and Neo4j nodes.
	docker compose down --volumes

#
# Private Targets
#

# Create .env file from .env.example if it does not exist
.env:
	cp .env.example .env

_mvn-package:
	# Compile the project using "mvn" and package it in a JAR file.
	@docker run -it -v "$(shell pwd)":/usr/src/mymaven -v ${PROJECT_NAME}-maven-repo:/root/.m2 -w /usr/src/mymaven maven:3.3-jdk-8 mvn clean package
	cp -rf target/${PROJECT_NAME}-1.0-SNAPSHOT.jar jars

_mvn-clean:
	# Clear the "target" directory.
	@docker run -it -v "$(shell pwd)":/usr/src/tmp alpine find /usr/src/tmp/target /usr/src/tmp/dependency-reduced-pom.xml -delete || exit 0

.PHONY: all up mvn run clean _mvn-package _mvn-clean
