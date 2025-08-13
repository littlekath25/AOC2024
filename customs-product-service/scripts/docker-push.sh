#!/usr/bin/env bash
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

set -o errexit
set -o pipefail

# This variable is in the helpers script to make it easy to reference in multiple scripts
repo_name=undefined
# shellcheck source-path=SCRIPTDIR
source "${SCRIPT_DIR}/helpers.sh"

version=${VERSION_TAG}
git_ref=$(get_branch_or_tag)

environment="${2:-stage}"

artifactory_hostname="artifactory.flexport.io"
artifact_repository_name="${repo_name}-docker-${environment}-local"
artifact_tag="${repo_name}:${version}"
artifact_remote_tag="${artifactory_hostname}/${artifact_repository_name}/${artifact_tag}"
artifact_remote_git_ref_tag="${artifactory_hostname}/${artifact_repository_name}/${repo_name}:${git_ref}"

dist_file="${SCRIPT_DIR}/../server/build/libs/server.jar"
if [[ ! -f "$dist_file" ]] ; then
  echo "Distribution not found @ $dist_file. Did you run './gradlew bootJar'? "
  exit 1
fi

tag_args="--tag \"${artifact_remote_tag}\""

# https://github.com/flexport/development-infrastructure/pull/153 Allows for image tag override, adding
# artifact_remote_git_ref_tag only for staging as only staging repo allows tag overrides (e.g. main, latest)
if [ "$environment" == "stage" ]; then
  tag_args="${tag_args} --tag \"${artifact_remote_git_ref_tag}\""
fi

eval "docker buildx build \
  --push \
  --platform linux/amd64 \
  --progress plain \
  ${tag_args} \
  -f server/docker/Dockerfile server"

success "docker-push completed successfully."
