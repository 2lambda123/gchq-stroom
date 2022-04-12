#!/usr/bin/env bash
# Runs just the java compilation
# Usage: ./build.sh
#        MAX_WORKERS=6 SKIP_TESTS=true ./build.sh

set -euo pipefail

# Shell Colour constants for use in 'echo -e'
# e.g.  echo -e "My message ${GREEN}with just this text in green${NC}"
# shellcheck disable=SC2034
{
  RED='\033[1;31m'
  GREEN='\033[1;32m'
  YELLOW='\033[1;33m'
  BLUE='\033[1;34m'
  NC='\033[0m' # No Colour
}

# -Dorg.gradle.caching=true
GRADLE_ARGS=(
  "-Dorg.gradle.daemon=false" # daemon no good in a container
  "-Dorg.gradle.parallel=true"
  "-Dorg.gradle.workers.max=${MAX_WORKERS:-6}"
  "-Dorg.gradle.configureondemand=true"
  "--console=plain"
)

determine_host_address() {
  if [ "$(uname)" == "Darwin" ]; then
    # Code required to find IP address is different in MacOS
    ip=$(ifconfig | grep "inet " | grep -Fv 127.0.0.1 | awk 'NR==1{print $2}')
  else
    local ip_binary
    # If ip is not on the path (as seems to be the case with ansible) then
    # try using /sbin instead.
    if command -v ip > /dev/null; then
      ip_binary="ip"
    elif command -v /sbin/ip > /dev/null; then
      ip_binary="/sbin/ip"
    else
      echo
      echo -e "${RED}ERROR${NC} Unable to locate ${BLUE}ip${NC} command." >&2
      exit 1
    fi
    ip=$( \
      "${ip_binary}" route get 1 \
      | awk 'match($0,"src [0-9\\.]+") {print substr($0,RSTART+4,RLENGTH-4)}')
  fi

  if [[ ! "${ip}" =~ ^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$ ]]; then
    echo
    echo -e "${RED}ERROR${NC} Unable to determine IP address. [${GREEN}${ip}${NC}] is not valid.${NC}" >&2
    exit 1
  fi

  echo "$ip"
}

main() {

  # We want to run from the repo root
  local local_repo_root
  local_repo_root="$(git rev-parse --show-toplevel)"
  pushd "${local_repo_root}" > /dev/null

  # When we are in a container localhost is no good for connecting to the db
  # so use the ip
  host_ip="${DOCKER_HOST_IP:-$(determine_host_address)}"
  export STROOM_JDBC_DRIVER_HOST="${host_ip}"
  echo -e "${GREEN}Setting STROOM_JDBC_DRIVER_HOST to ${BLUE}${host_ip}${NC}"

#  echo "::group::Java compile"
#  echo -e "${GREEN}Do the Jave compile${NC}"
#  ./gradlew \
#    "${GRADLE_ARGS[@]}" \
#    compileJava \
#    compileTestJava \
#    -x test \
#    -x gwtCompile
#  echo "::endgroup::"

  echo "::group::Java compile"
  echo -e "${GREEN}Do the Jave compile${NC}"
  ./gradlew \
    "${GRADLE_ARGS[@]}" \
    stroom-app:test \
    --tests TestFullTranslationTaskAndStepping
  echo "::endgroup::"

  echo -e "${GREEN}Done${NC}"
}

# Get a time for the whole build
time main "$@"
