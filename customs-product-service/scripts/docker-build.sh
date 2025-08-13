#!/usr/bin/env bash
#
# This script builds the docker container. It requires you
# to have build the jar first using `gradle build`. If you
# would like to use the docker container on your machine, run
# this script with the `--load` argument to load it into your
# local registry.
#
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

set -o errexit
set -o pipefail
set -o nounset
set -e
#set -x


# This variable is in the helpers script to make it easy to reference in multiple scripts
repo_name=undefined
# shellcheck source-path=SCRIPTDIR
source "${SCRIPT_DIR}/helpers.sh"

if [ ! -f "$SCRIPT_DIR/../server/build/distributions/server.tar" ] ; then
  if [[ "${CI:=false}" != "true" ]]; then # we're not running in a GH action
    header "WARNING: This script does not run './gradlew distTar' for you.
      Make sure you've built your code!"
  else
    cd "$SCRIPT_DIR/.."
    ./gradlew distTar
    cd -
  fi
fi

version=${VERSION_TAG}

artifact_tag="${repo_name}:${version}"

header "Building Image with tags ${artifact_tag}"

dist_file="${SCRIPT_DIR}/../server/build/distributions/server.tar"
if [ ! -f "$dist_file" ] ; then
  echo "Distribution not found @ $dist_file. Did you run './gradlew distTar'? "
  exit 1
fi

# Because we use buildx, the images are not usable locally
# unless you specify the `--load` property. Because we use a
# specific arch we only do that when you're not on an ARM.
# Add the --load property to use the image outside of buildx
docker buildx build \
  --platform linux/amd64 \
  --tag "${artifact_tag}" \
  "$@" --progress plain \
  -f "${SCRIPT_DIR}/../server/docker/Dockerfile" server

success "Docker image built name=docker_version::${version}"
echo "tag=${artifact_tag}" >> "$GITHUB_OUTPUT"
echo "docker_version=${version}" >> "$GITHUB_OUTPUT"
