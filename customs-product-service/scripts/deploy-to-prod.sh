#!/usr/bin/env bash
set -eu -o pipefail

git fetch --tags
gh workflow run "Deploy the application to production" -F version="$(git tag --sort=committerdate | tail -1)"
gh run list --workflow=deploy-to-production.yaml
