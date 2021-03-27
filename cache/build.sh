#!/usr/bin/env bash

mvn -DskipTests=true clean spring-boot:build-image
docker run docker.io/library/cache:0.0.1-SNAPSHOT

