#!/bin/bash
DIR="$(dirname "${BASH_SOURCE[0]}")"
DIR="$(realpath "${DIR}/..")"
docker run \
    --rm=true -t -i \
    --platform linux/amd64 \
    -v "$DIR:/tmp/lint" \
    -e RUN_LOCAL=true \
    -e USE_FIND_ALGORITHM=true \
    -e FILTER_REGEX_EXCLUDE='.*(helm/|build/|bin/|\.gradle/|gradlew).*' \
    -e VALIDATE_ALL_CODEBASE=true \
    -e VALIDATE_BASH=true \
    -e VALIDATE_DOCKERFILE_HADOLINT=true \
    -e VALIDATE_GITHUB_ACTIONS=true \
    -e VALIDATE_YAML=true \
    github/super-linter:slim-v4