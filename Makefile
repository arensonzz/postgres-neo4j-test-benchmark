# Makefile for postgres-neo4j-test-benchmark
#
# @author arensonzz
# project_name:=postgres-neo4j-test-benchmark

# Source default environment variables from .env file
include .env

.DELETE_ON_ERROR:

# Default target
all: .env build run

build:
	# build Java application using Maven into Docker image
	docker build -t ${project_name} .

run:
	# start Docker compose services
	docker compose up -d

# Create .env file from .env.example if it does not exist
.env:
	cp .env.example .env

clean:
	@echo Clean target run
	docker compose down --volumes


.PHONY: all build run clean
