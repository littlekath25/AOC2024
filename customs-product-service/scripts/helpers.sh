#!/bin/bash

RED="\033[31m"
YELLOW="\033[33m"
CYAN="\033[36m"
DEFAULT="\033[39m"
RESET="\033[0m"
BOLD="\033[1m"
DEFAULT_STYLE=""

repo_name='customs-product-service'
export repo_name


# Arguments: ($1: message, $2: color, $3: style)
pp() {
  local message="$1"
  local color="${2:-${DEFAULT}}"
  local style="${3:-${DEFAULT_STYLE}}"

  echo -e "${color}${style}${message}${RESET}"
}

success() {
  pp ""
  pp "✅ success: $1" "${YELLOW}" "${BOLD}"
  pp ""
}

error() {
  pp ""
  pp "❌ Error: $1" "${RED}" "${BOLD}"
  pp ""
}

fatal() {
  error "$@"
  exit 1
}

header() {
  pp ""
  pp "💡 $1" "${CYAN}" "${BOLD}"
  pp ""
}

check_rc() {
  local status="${1}"
  local message="${2}"
  if (( status != 0 )); then
    fatal "Process ${message} failed with ${status}"
  else
    success "Process ${message} succeeded with ${status}"
  fi
}

get_branch_or_tag () {
  local branch_name
  local github_ref

  branch_name="$(git symbolic-ref HEAD 2>/dev/null)" || branch_name="(unnamed branch)" # detached HEAD
  github_ref="${GITHUB_REF:-${branch_name}}"
  github_ref="${github_ref##refs/heads/}"

  echo "${github_ref}"
}

get_version_tag() {
  local current_date_time
  local version

  current_date_time=$(date +'%Y.%m.%d.%H%M')
  local GITHUB_SHA="${GITHUB_SHA:-$(git rev-parse --short HEAD)}"
  version="${current_date_time}-${GITHUB_SHA:0:7}"

  echo "${version}"
}
