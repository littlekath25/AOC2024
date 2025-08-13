#!/bin/bash

git_tag="${1}"

git_commit_sha="$(git rev-list -n 1 tags/"$git_tag")"
echo "GIT_COMMIT_SHA=$git_commit_sha"
