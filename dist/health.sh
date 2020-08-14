#!/usr/bin/env bash
#
# Checks the health of each app using the supplied admin url

# We shouldn't use a lib function (e.g. in shell_utils.sh) because it will
# give the directory relative to the lib script, not this script.
readonly DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo_usage() {
  echo -e "${GREEN}This script checks the health of Stroom${NC}"
  echo -e "Usage: ${BLUE}$0${GREEN} [-h] [-m]${NC}" >&2
  echo -e " -h:   ${GREEN}Print Help (this message) and exit${NC}"
  echo -e " -m:   ${GREEN}Monochrome. Don't use colours in terminal output.${NC}"
}

invalid_arguments() {
  echo -e "${RED}ERROR${NC} - Invalid arguments" >&2
  echo_usage
  exit 1
}

echo_healthy() {
  info "  Status:   ${GREEN}HEALTHY${NC}"
}

echo_unhealthy() {
  error "  Status:   ${RED}UNHEALTHY${NC}"
  error "  Details:"
  echo
}

check_health() {
  if command -v jq 1>/dev/null; then 
    # jq is available so do a more complex health check
    local is_jq_installed=true
  else
    # jq is not available so do a simple health check
    warn "Doing simple health check as ${BLUE}jq${NC} is not installed."
    warn "See ${BLUE}https://stedolan.github.io/jq/${NC} for details on how to install it."
    local is_jq_installed=false
  fi 

  if [ $# -ne 4 ]; then
    error "Invalid arguments. Usage: ${BLUE}health.sh HOST PORT PATH${NC}, e.g. health.sh localhost 8080 stroomAdmin"
    echo "$@"
    exit 1
  fi

  local -r health_check_service="$1"
  local -r health_check_host="$2"
  local -r health_check_port="$3"
  local -r health_check_path="$4"

  local -r health_check_url="http://${health_check_host}:${health_check_port}/${health_check_path}/healthcheck"
  local -r health_check_pretty_url="${health_check_url}?pretty=true"

  echo
  info "Checking the health of ${GREEN}${health_check_service}${NC} using ${BLUE}${health_check_pretty_url}${NC}"

  local -r http_status_code=$(curl --silent --output /dev/null --write-out "%{http_code}" "${health_check_url}")
  #echo "http_status_code: $http_status_code"

  # First hit the url to see if it is there
  if [ "x501" = "x${http_status_code}" ]; then
    # Server is up but no healthchecks are implmented, so assume healthy
    echo_healthy
  elif [ "x200" = "x${http_status_code}" ] || [ "x500" = "x${http_status_code}" ]; then
    # 500 code indicates at least one health check is unhealthy but jq will fish that out

    if [ "${is_jq_installed}" = true ]; then
      # Count all the unhealthy checks
      local -r unhealthy_count=$( \
        curl -s "${health_check_url}" | 
        jq '[to_entries[] | {key: .key, value: .value.healthy}] | map(select(.value == false)) | length')

      #echo "unhealthy_count: $unhealthy_count"
      if [ "${unhealthy_count}" -eq 0 ]; then
        echo_healthy
      else
        echo_unhealthy

        # Dump details of the failing health checks
        curl -s "${health_check_url}" | 
          jq 'to_entries | map(select(.value.healthy == false)) | from_entries'

        echo
        info "  See ${BLUE}${health_check_url}?pretty=true${NC} for the full report"

        total_unhealthy_count=$((total_unhealthy_count + unhealthy_count))
      fi
    else
      # non-jq approach
      if [ "x200" = "x${http_status_code}" ]; then
        echo_healthy
      elif [ "x500" = "x${http_status_code}" ]; then
        echo_unhealthy
        warn "See ${BLUE}${health_check_pretty_url}${NC} for details"
        # Don't know how many are unhealthy but it is at least one
        total_unhealthy_count=$((total_unhealthy_count + 1))
      fi
    fi
  else
    echo_unhealthy
    local err_msg
    # Run a clightly different curl command to extract the curl error message
    # from stderr
    # OR it with true to stop it halting the script as we have '-e' set
    err_msg="$(curl -s --show-error "${health_check_url}" 2>&1 || true)"
    echo -e "${RED}${err_msg}${NC}"
    total_unhealthy_count=$((total_unhealthy_count + 1))
  fi
}

main() {
  # shellcheck disable=SC1090
  source "$DIR"/bin/utils.sh
  # shellcheck disable=SC1091
  source config/scripts.env

  while getopts ":mh" arg; do
    # shellcheck disable=SC2034
    case $arg in
      h ) 
        echo_usage
        exit 0
        ;;
      m )  
        MONOCHROME=true 
        ;;
      * ) 
        invalid_arguments
        ;;  # getopts already reported the illegal option
    esac
  done
  shift $((OPTIND-1)) # remove parsed options and args from $@ list

  setup_colours

  #check_is_configured
  check_start_is_not_erroring

  local total_unhealthy_count=0

  check_health "stroom" "localhost" "${STROOM_ADMIN_PORT}" "admin"

  return ${total_unhealthy_count}
}

main "$@"

# Return the unhealthy count so this script can be used in an automated way
exit $?
# vim:sw=2:ts=2:et:
