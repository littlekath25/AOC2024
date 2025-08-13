#!/bin/bash
#
artifactory_username="${ARTIFACTORY_USERNAME:?ARTIFACTORY_USERNAME needs to be set}"
artifactory_password="${ARTIFACTORY_PASSWORD:?ARTIFACTORY_PASSWORD needs to be set}"
artifactory_credentials="${artifactory_username}:${artifactory_password}"

service_artifact_id='customsproductservice'

artifactory_host="artifactory.flexport.io"
artifactory_url="https://${artifactory_host}/artifactory"
lib_name="${LIB_NAME:?LIB_NAME needs to be set}"
version="${VERSION:?VERSION needs to be set}"

path="com/flexport/customs/${service_artifact_id}/${lib_name}/${version}"
url="${artifactory_url}/${ARTIFACT_REPO}/${path}/"

status_code=$(curl -u "${artifactory_credentials}" --write-out '%{http_code}' --silent --output /dev/null "${url}")


if [[ "${status_code}" == 200 ]]
then
  echo "Artifact already exists"
  echo "exists=true" >> "$GITHUB_OUTPUT"
elif [ "${status_code}" == 404 ]; then
  echo "Artifact does not exist yet"
  echo "exists=false" >> "$GITHUB_OUTPUT"
else
  echo "Found unexpected status_code ${status_code}"

  curl -v -u "${artifactory_credentials}" --write-out '%{http_code}' "${url}"

  exit 1
fi
