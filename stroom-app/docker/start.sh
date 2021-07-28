#!/usr/bin/env sh

ROOT_DIR=/stroom
BIND_MOUNT_CONFIG_FILE="${ROOT_DIR}/config/config.yml"
FALLBACK_CONFIG_FILE="${ROOT_DIR}/config_fallback/config.yml"

main() {
  local dropwizard_command="${1:-server}"; shift
  local dropwizard_command_args="$@"

  # To allow us to run the container outside of a stack it needs a config file
  # to work with. We bake one into the image so that if the config volume
  # is not bind mounted we can fallback on the default one.
  if [ -f "${BIND_MOUNT_CONFIG_FILE}" ]; then
    config_file="${BIND_MOUNT_CONFIG_FILE}"
  else
    config_file="${FALLBACK_CONFIG_FILE}"
    echo "WARN   Using fallback config file as ${BIND_MOUNT_CONFIG_FILE} does" \
      "not exist. You may not have correctly configured the /stroom/config" \
      "volume or you are running in development."
  fi

  local java_opts="${JAVA_OPTS:- -Xms50m -Xmx2g}"
  echo "Starting stroom"
  echo "Command:      [${dropwizard_command}]"
  echo "Command args: [${dropwizard_command_args}]"
  echo "Config file:  [${config_file}]"
  echo "JAVA_OPTS:    [${JAVA_OPTS}]"

  #shellcheck disable=2086
  java \
    ${JAVA_OPTS} \
    -jar stroom-app-all.jar \
    "${dropwizard_command}" \
    "$@" \
    "${config_file}"
}

main "$@"

#vim:set et sw=2 ts=2:
