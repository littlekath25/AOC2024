#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
# shellcheck source-path=SCRIPTDIR
source "${SCRIPT_DIR}/helpers.sh"

SCHEMA_FILE=./server/schema.graphqls
SCHEMA_BACKUP_FILE=./server/schema.graphqls.bak

restoreBackup () {
  # Remove the new file and move the backup back
  header "Restoring schema backup"
  mv $SCHEMA_BACKUP_FILE $SCHEMA_FILE
}

header "Validate there are no uncommitted changes to the ${SCHEMA_FILE} file"

# Copy existing schema to backup location
header "Creating backup of schema file"
cp $SCHEMA_FILE $SCHEMA_BACKUP_FILE

# Run schema generation, which re-generates ./server/schema.graphqls
header "Regenerating graphql schema"
./gradlew server:graphqlGenerateSDL

# Use git to detect if the contents of the schema changed
if [[ $(git status --porcelain $SCHEMA_FILE) ]]; then
  header "Changes detected"
  git diff $SCHEMA_FILE

  restoreBackup

  # Schema changed during regeneration
  error "Uncommitted graphql schema changes detected. Run ./gradlew server:graphqlGenerateSDL and commit the changes"
  exit 1
else
  restoreBackup
  success "No schema changes detected, you may proceed"
  exit 0
fi

