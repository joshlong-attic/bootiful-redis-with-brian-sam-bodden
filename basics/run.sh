#!/usr/bin/env bash


docker run -e "SPRING_REDIS_HOST=host.docker.internal" docker.io/library/basics:0.0.1-SNAPSHOT
