#!/usr/bin/env bash
#
# This script sets up your kubernetes config to connect to

ENVIRONMENT=stage
SERVICE='customs-product-service'
CLUSTER=fke-nonprod-use1-01
BREAKGLASS_ROLE='-breakglass'

usage="
Usage: $(basename "$0") [-h] [-e environment] [-b reason]

Examples:
  Login to Non Prod FKE and set the default namespace to ${ENVIRONMENT}-${SERVICE}
  $ $(basename "$0")

  Login to Production FKE and set the default namespace to prod-${SERVICE}
  $ $(basename "$0") -e prod

  Login to Production FKE and set the default namespace to prod-${SERVICE} with elevated permissions
  $ $(basename "$0") -e prod -b \"Insert reason here\"

  For the help message:
  $ $(basename "$0") -h

Options:
  -e  which environment to log in to. Possible values: stage (default), staging, prod, production
  -b  request break the glass permission, specify the reason for elevated permissions
  -h  for this message
"

assertTeleportConnectivity () {
  if ! curl -q https://teleport-proxy.flexport.io --connect-timeout 1 > /dev/null 2>&1 ; then
      echo "Cannot reach teleport (@ https://teleport-proxy.flexport.io). Is your VPN connected?"
      exit 1
  fi
}

loggedInToCluster () {
  local cluster="$1"
  if tsh kube ls | sed 's/ //g' | grep -q "${cluster}"; then
    return 1
  else
    return 0
  fi
}

while getopts ":e:b:h" arg; do
  case $arg in
    e) ENVIRONMENT="$OPTARG"; ;;
    b) BREAKGLASS_REASON="$OPTARG"; ;;
    h) echo "$usage"; exit 0; ;;
    *) echo "$usage"; exit 0; ;;
  esac
done


case "$ENVIRONMENT" in
  "stage"|"staging") CLUSTER=fke-nonprod-use1-01 ;;
  "prod"|"production") CLUSTER=fke-prod-use1-01 ;;
  *) echo "Unknown environment: ${ENVIRONMENT}"; exit 1 ;;
esac

NAMESPACE="${ENVIRONMENT}-${SERVICE}"
echo "Connecting to $NAMESPACE at $CLUSTER"

if [[ -n "$BREAKGLASS_REASON" ]]; then
  assertTeleportConnectivity

  # When requesting break the glass we always login
  echo "Logging in to Teleport with break glass '${BREAKGLASS_REASON}'"
  tsh login --proxy teleport-proxy.flexport.io --request-roles="$BREAKGLASS_ROLE" --request-reason="$BREAKGLASS_REASON"
elif ! tsh status 2>&1 | grep -q 'valid for' ; then
  assertTeleportConnectivity

  # Only log in when no active session is found
  echo "Logging in to Teleport"
  tsh login --proxy teleport-proxy.flexport.io
fi

# Log in to the cluster if we haven't already
if ! tsh kube ls 2>&1 | sed 's/ //g' | grep -q "${CLUSTER}\*" ; then
  tsh kube login "$CLUSTER"
fi

kubectl config set-context --current --namespace="$NAMESPACE" > /dev/null
echo "You're connected to $NAMESPACE. Use 'tsh logout' to deactivate this session."
