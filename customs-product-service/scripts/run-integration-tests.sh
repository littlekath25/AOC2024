#!/usr/bin/env bash
# This script rebuilds the server container from scratch and runs the integration tests against it.

# ANSI colors
CYAN="\033[36m"
BOLD="\033[1m"
DEFAULT="\033[39m"
RESET="\033[0m"

pp() {
    local message="$1"
    local color="${2:-$DEFAULT}"
    local style="${3:-}"

    echo -e "${color}${style}$message${RESET}"
}

header() {
    pp "------------------------------------"
    pp "$1" "$CYAN" "$BOLD"
    pp "------------------------------------"
}

# Exit when a command fails.
set -eu -o pipefail

teardown() {
  header "Tearing down docker containers"
  docker compose --project-directory ./server --profile integration-test down -v
}

build() {
  header "Building server"
  docker compose --project-directory ./server --profile integration-test up -d postgres # the build depends on the database
  ./scripts/generate-db-schema.sh
  ./gradlew distTar # the Dockerfile only imports the tar file so it must be built first
  ./gradlew integration-test:build # To ensure the extensions for wiremock are available
}

up() {
  header "Starting server and dependencies"
  # force recreate so the server container gets recreated
  docker compose --project-directory ./server --profile integration-test up -d --wait --build --force-recreate
}

run_tests() {
  header "Running integration tests"
  ./gradlew integration-test:test -PrunIntegrationTest=true
}

# Default value for rebuild is true
REBUILD=true

# Check for the --rebuild argument (if it's set to false, change REBUILD to false)
while [[ $# -gt 0 ]]; do
  case $1 in
    --rebuild=*)
      REBUILD="${1#*=}"
      shift
      ;;
    *)
      echo "Unknown option: $1"
      exit 1
      ;;
  esac
done

# Only run teardown, build, and up if REBUILD is true
if [ "$REBUILD" = true ]; then
  teardown
  build
  up
fi

run_tests
