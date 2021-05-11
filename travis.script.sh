#!/bin/bash

# exit script on any error
set -e

STROOM_DOCKER_REPO="gchq/stroom"
STROOM_PROXY_DOCKER_REPO="gchq/stroom-proxy"
GITHUB_REPO="gchq/stroom"
GITHUB_API_URL="https://api.github.com/repos/gchq/stroom/releases"
STROOM_DOCKER_CONTEXT_ROOT="stroom-app/docker/."
STROOM_PROXY_DOCKER_CONTEXT_ROOT="stroom-proxy/stroom-proxy-app/docker/."
VERSION_FIXED_TAG=""
SNAPSHOT_FLOATING_TAG=""
MAJOR_VER_FLOATING_TAG=""
MINOR_VER_FLOATING_TAG=""
# This is a whitelist of branches to produce docker builds for
BRANCH_WHITELIST_REGEX='(^dev$|^master$|^[0-9]+\.[0-9]+$)'
RELEASE_VERSION_REGEX='^v[0-9]+\.[0-9]+.*$'
CRON_TAG_SUFFIX="DAILY"
LATEST_SUFFIX="-LATEST"
# This is the branch containing the current release of stroom
CURRENT_STROOM_RELEASE_BRANCH="6.0"
doDockerBuild=false
RELEASE_ARTEFACTS_DIR="${TRAVIS_BUILD_DIR}/release_artefacts"

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

create_file_hash() {
  local -r file="$1"
  local -r hash_file="${file}.sha256"
  local dir
  dir="$(dirname "${file}")"
  local filename
  filename="$(basename "${file}")"

  echo -e "Creating a SHA-256 hash for file ${GREEN}${filename}${NC} in ${GREEN}${dir}${NC}"
  # Go to the dir where the file is so the hash file doesn't contain the full
  # path
  pushd "${dir}" > /dev/null
  sha256sum "${filename}" > "${hash_file}"
  popd > /dev/null
  echo -e "Created hash file ${GREEN}${hash_file}${NC}, containing:"
  echo -e "-------------------------------------------------------"
  cat "${hash_file}"
  echo -e "-------------------------------------------------------"
}

generate_ddl_dump() {
  mkdir -p "${RELEASE_ARTEFACTS_DIR}"

  # clear down stroom-all-dbs container and volumes
  docker ps -q -f=name='stroom-all-dbs' | xargs -r docker stop --time 0
  docker ps -a -q -f=name='stroom-all-dbs' | xargs -r docker rm
  docker volume ls -q -f=name='bounceit_stroom-all-dbs*' | xargs -r docker volume rm

  # Run the db migration against the empty db to give us a vanilla
  # schema to dump
  ./container_build/runInJavaDocker.sh MIGRATE

  docker exec \
    stroom-all-dbs \
    mysqldump \
      -d \
      -p"my-secret-pw" \
      stroom \
    > "${RELEASE_ARTEFACTS_DIR}/database-schema-${TRAVIS_TAG}.sql"
}

generate_entity_rel_diagram() {
  ./container_build/runInJavaDocker.sh ERD
}

# Put all release artefacts in a dir to make it easier to upload them to
# Github releases. Some of them are needed by the stack builds in
# stroom-resources
gather_release_artefacts() {
  mkdir -p "${RELEASE_ARTEFACTS_DIR}"

  local -r release_config_dir="${TRAVIS_BUILD_DIR}/stroom-app/build/release/config"
  local -r proxy_release_config_dir="${TRAVIS_BUILD_DIR}/stroom-proxy/stroom-proxy-app/build/release/config"

  local -r docker_build_dir="${TRAVIS_BUILD_DIR}/stroom-app/docker/build"
  local -r proxy_docker_build_dir="${TRAVIS_BUILD_DIR}/stroom-proxy/stroom-proxy-app/docker/build"

  echo "Copying release artefacts to ${RELEASE_ARTEFACTS_DIR}"

  # The zip dist config is inside the zip dist. We need the docker dist
  # config so stroom-resources can use it.

  # Stroom
  cp "${docker_build_dir}/config.yml" \
    "${RELEASE_ARTEFACTS_DIR}/stroom-app-config-${TRAVIS_TAG}.yml"

  cp "${release_config_dir}/config-defaults.yml" \
    "${RELEASE_ARTEFACTS_DIR}/stroom-app-config-defaults-${TRAVIS_TAG}.yml"

  cp "${release_config_dir}/config-schema.yml" \
    "${RELEASE_ARTEFACTS_DIR}/stroom-app-config-schema-${TRAVIS_TAG}.yml"

  cp "stroom-app/build/distributions/stroom-app-${TRAVIS_TAG}.zip" \
    "${RELEASE_ARTEFACTS_DIR}"

  cp "stroom-app/src/main/resources/ui/noauth/swagger/stroom.json" \
    "${RELEASE_ARTEFACTS_DIR}"

  cp "stroom-app/src/main/resources/ui/noauth/swagger/stroom.yaml" \
    "${RELEASE_ARTEFACTS_DIR}"

  # Stroom-Proxy
  cp "${proxy_docker_build_dir}/config.yml" \
    "${RELEASE_ARTEFACTS_DIR}/stroom-proxy-app-config-${TRAVIS_TAG}.yml"

  cp "${proxy_release_config_dir}/config-defaults.yml" \
    "${RELEASE_ARTEFACTS_DIR}/stroom-proxy-app-config-defaults-${TRAVIS_TAG}.yml"

  cp "${proxy_release_config_dir}/config-schema.yml" \
    "${RELEASE_ARTEFACTS_DIR}/stroom-proxy-app-config-schema-${TRAVIS_TAG}.yml"

  cp "stroom-proxy/stroom-proxy-app/build/distributions/stroom-proxy-app-${TRAVIS_TAG}.zip" \
    "${RELEASE_ARTEFACTS_DIR}"

  # Stroom (Headless)
  cp "stroom-headless/build/distributions/stroom-headless-${TRAVIS_TAG}.zip" \
    "${RELEASE_ARTEFACTS_DIR}"

  cp "./container_build/build/entity_relationships.svg" \
    "${RELEASE_ARTEFACTS_DIR}"

  # Now generate hashes for all the zips
  for file in "${RELEASE_ARTEFACTS_DIR}"/*.zip; do
    create_file_hash "${file}"
  done
}

createGitTag() {
  local -r tagName=$1
  
  git config --global user.email "builds@travis-ci.com"
  git config --global user.name "Travis CI"

  echo -e "Tagging commit [${GREEN}${TRAVIS_COMMIT}${NC}] with" \
    "tag [${GREEN}${tagName}${NC}]"
  git tag \
    -a \
    "${tagName}" \
    "${TRAVIS_COMMIT}" \
    -m "Automated Travis build $TRAVIS_BUILD_NUMBER" >/dev/null 2>&1
  # TAGPERM is a travis encrypted github token, see 'env' section in .travis.yml
  git push \
    -q \
    "https://${TAGPERM}@github.com/${GITHUB_REPO}" \
    "${tagName}" >/dev/null 2>&1
}

isCronBuildRequired() {
  # GH_USER_AND_TOKEN is set in env section of .travis.yml
  local authArgs=()
  if [ -n "${GH_USER_AND_TOKEN}" ]; then
    echo "Using authentication with curl"
    authArgs+=("--user" "${GH_USER_AND_TOKEN}")
  fi
  # query the github api for the latest cron release tag name
  # redirect stderr to dev/null to protect api token
  local latestTagName
  latestTagName=$(curl -s "${authArgs[@]}" ${GITHUB_API_URL} | \
    jq -r "[.[] | select(.tag_name | test(\"${TRAVIS_BRANCH}.*${CRON_TAG_SUFFIX}\"))][0].tag_name" \
    2>/dev/null)
  echo -e "Latest release ${CRON_TAG_SUFFIX}" \
    "tag: [${GREEN}${latestTagName}${NC}]"

  if [ "${latestTagName}x" != "x" ]; then 
    # Get the commit sha that this tag applies to (not the commit of the tag itself)
    local shaForTag
    shaForTag=$(git rev-list -n 1 "${latestTagName}")
    echo -e "SHA hash for tag ${latestTagName}: [${GREEN}${shaForTag}${NC}]"
    if [ "${shaForTag}x" = "x" ]; then
      echo -e "${RED}Unable to get sha for tag ${BLUE}${latestTagName}${NC}"
      exit 1
    else
      if [ "${shaForTag}x" = "${TRAVIS_COMMIT}x" ]; then
        echo -e "${RED}The commit of the build matches the latest" \
          "${CRON_TAG_SUFFIX} release.${NC}"
        echo -e "${RED}Git will not be tagged and no release will be" \
          "made.${NC}"
        # The latest release has the same commit sha as the commit travis is building
        # so don't bother creating a new tag as we don't want a new release
        false
      fi
    fi
  else
    # no release found so return true so a build happens
    true
  fi
  return
}

# args: dockerRepo contextRoot tag1VersionPart tag2VersionPart ... tagNVersionPart
releaseToDockerHub() {
  # echo "releaseToDockerHub called with args [$@]"

  if [ $# -lt 3 ]; then
    echo "Incorrect args, expecting at least 3"
    exit 1
  fi
  dockerRepo="$1"
  contextRoot="$2"
  # shift the the args so we can loop round the open ended list of tags,
  # $1 is now the first tag
  shift 2

  local allTagArgs=()

  for tagVersionPart in "$@"; do
    if [ "x${tagVersionPart}" != "x" ]; then
      # echo -e "Adding docker tag [${GREEN}${tagVersionPart}${NC}]"
      allTagArgs+=("--tag=${dockerRepo}:${tagVersionPart}")
    fi
  done

  echo -e "Building a docker image with tags: ${GREEN}${allTagArgs[*]}${NC}"
  echo -e "dockerRepo:  [${GREEN}${dockerRepo}${NC}]"
  echo -e "contextRoot: [${GREEN}${contextRoot}${NC}]"
  # If we have a TRAVIS_TAG (git tag) then use that, else use the floating tag
  docker build \
    "${allTagArgs[@]}" \
    --build-arg GIT_COMMIT="${TRAVIS_COMMIT}" \
    --build-arg GIT_TAG="${TRAVIS_TAG:-${SNAPSHOT_FLOATING_TAG}}" \
    "${contextRoot}"

  echo -e "Pushing the docker image to ${GREEN}${dockerRepo}${NC} with" \
    "tags: ${GREEN}${allTagArgs[*]}${NC}"
  docker push "${dockerRepo}" >/dev/null 2>&1

  echo -e "Completed Docker release"
}

# establish what version of stroom we are building
if [ -n "$TRAVIS_TAG" ]; then
  # Tagged commit so use that as our stroom version, e.g. v6.0.0
  STROOM_VERSION="${TRAVIS_TAG}"
else
  # No tag so use the branch name as the version, e.g. dev
  STROOM_VERSION="${TRAVIS_BRANCH}"
fi

# Dump all the travis env vars to the console for debugging
echo -e "HOME:                          [${GREEN}${HOME}${NC}]"
echo -e "TRAVIS_BUILD_DIR:              [${GREEN}${TRAVIS_BUILD_DIR}${NC}]"
echo -e "TRAVIS_BUILD_NUMBER:           [${GREEN}${TRAVIS_BUILD_NUMBER}${NC}]"
echo -e "TRAVIS_COMMIT:                 [${GREEN}${TRAVIS_COMMIT}${NC}]"
echo -e "TRAVIS_BRANCH:                 [${GREEN}${TRAVIS_BRANCH}${NC}]"
echo -e "TRAVIS_TAG:                    [${GREEN}${TRAVIS_TAG}${NC}]"
echo -e "TRAVIS_PULL_REQUEST:           [${GREEN}${TRAVIS_PULL_REQUEST}${NC}]"
echo -e "TRAVIS_EVENT_TYPE:             [${GREEN}${TRAVIS_EVENT_TYPE}${NC}]"
echo -e "STROOM_VERSION:                [${GREEN}${STROOM_VERSION}${NC}]"
echo -e "CURRENT_STROOM_RELEASE_BRANCH: [${GREEN}${CURRENT_STROOM_RELEASE_BRANCH}${NC}]"

if [ "$TRAVIS_EVENT_TYPE" = "cron" ]; then
  echo "This is a cron build so just tag the commit if we need to and exit"

  if isCronBuildRequired; then
    echo "The release build will happen when travis picks up the tagged commit"
    # This is a cron triggered build so tag as -DAILY and push a tag to git
    DATE_ONLY="$(date +%Y%m%d)"
    gitTag="${STROOM_VERSION}-${DATE_ONLY}-${CRON_TAG_SUFFIX}"

    createGitTag "${gitTag}"
  fi
else
  # Normal commit/PR/tag build
  extraBuildArgs=()

  if [ -n "$TRAVIS_TAG" ]; then
    doDockerBuild=true

    # This is a tagged commit, so create a docker image with that tag
    VERSION_FIXED_TAG="${TRAVIS_TAG}"

    # Extract the major version part for a floating tag
    majorVer=$(echo "${TRAVIS_TAG}" | grep -oP "^v[0-9]+")
    if [ -n "${majorVer}" ]; then
      MAJOR_VER_FLOATING_TAG="${majorVer}${LATEST_SUFFIX}"
    fi

    # Extract the minor version part for a floating tag
    minorVer=$(echo "${TRAVIS_TAG}" | grep -oP "^v[0-9]+\.[0-9]+")
    if [ -n "${minorVer}" ]; then
      MINOR_VER_FLOATING_TAG="${minorVer}${LATEST_SUFFIX}"
    fi

    if [[ "$TRAVIS_BRANCH" =~ ${RELEASE_VERSION_REGEX} ]]; then
      echo "This is a release version so add gradle arg for publishing" \
        "libs to Maven Central"
      # TODO need to add in the sonatype build args when we have decided 
      # what we are publishing from stroom
      #extraBuildArgs+=("bintrayUpload")
    fi
  elif [[ "$TRAVIS_BRANCH" =~ $BRANCH_WHITELIST_REGEX ]]; then
    # This is a branch we want to create a floating snapshot docker image for
    SNAPSHOT_FLOATING_TAG="${STROOM_VERSION}-SNAPSHOT"
    doDockerBuild=true
  fi

  echo -e "VERSION FIXED DOCKER TAG:      [${GREEN}${VERSION_FIXED_TAG}${NC}]"
  echo -e "SNAPSHOT FLOATING DOCKER TAG:  [${GREEN}${SNAPSHOT_FLOATING_TAG}${NC}]"
  echo -e "MAJOR VER FLOATING DOCKER TAG: [${GREEN}${MAJOR_VER_FLOATING_TAG}${NC}]"
  echo -e "MINOR VER FLOATING DOCKER TAG: [${GREEN}${MINOR_VER_FLOATING_TAG}${NC}]"
  echo -e "doDockerBuild:                 [${GREEN}${doDockerBuild}${NC}]"
  echo -e "extraBuildArgs:                [${GREEN}${extraBuildArgs[*]}${NC}]"

  # Ensure we have a local.yml file as the integration tests will need it
  ./local.yml.sh

  ./container_build/runInJavaDocker.sh GRADLE_BUILD

  # Do the gradle build
  # Use custom gwt compile jvm settings to avoid blowing the ram limit in
  # travis. At time of writing a sudo VM in travis has 7.5gb ram.
  # Each work will chew up the maxHeap value and we have to allow for
  # our docker services as well.
  # Don't clean as this is a fresh clone and clean will wipe the cached
  # content pack zips
  #echo -e "${GREEN}Do the basic java build${NC}"
  #./gradlew \
    #-Dorg.gradle.parallel=false \
    #--scan \
    #--stacktrace \
    #-PdumpFailedTestXml=true \
    #-Pversion="${TRAVIS_TAG}" \
    #build \
    #-x shadowJar \
    #-x resolve \
    #-x copyFilesForStroomDockerBuild \
    #-x copyFilesForProxyDockerBuild \
    #-x buildDistribution \
    #-Dorg.gradle.parallel=false

  #echo -e "${GREEN}Do the yarn build${NC}"
  #./gradlew \
    #--scan \
    #--stacktrace \
    #stroom-ui:copyYarnBuild

  ## Compile the application GWT UI
  #echo -e "${GREEN}Do the GWT app compile${NC}"
  #./gradlew \
    #--scan \
    #--stacktrace \
    #-PgwtCompilerWorkers=2 \
    #-PgwtCompilerMinHeap=50M \
    #-PgwtCompilerMaxHeap=2G \
    #stroom-app-gwt:gwtCompile

  ## Compile the dashboard GWT UI
  #echo -e "${GREEN}Do the GWT dashboard compile${NC}"
  #./gradlew \
    #--scan \
    #--stacktrace \
    #-PgwtCompilerWorkers=2 \
    #-PgwtCompilerMinHeap=50M \
    #-PgwtCompilerMaxHeap=2G \
    #stroom-dashboard-gwt:gwtCompile

  ## Make the distribution.
  #echo -e "${GREEN}Build the distribution${NC}"
  #./gradlew \
    #--scan \
    #--stacktrace \
    #-PdumpFailedTestXml=true \
    #-Pversion="${TRAVIS_TAG}" \
    #shadowJar \
    #buildDistribution \
    #copyFilesForStroomDockerBuild \
    #copyFilesForProxyDockerBuild \
    #-x test \
    #-x stroom-ui:copyYarnBuild \
    #-x stroom-app-gwt:gwtCompile \
    #-x stroom-dashboard-gwt:gwtCompile \
    #"${extraBuildArgs[@]}"

# Disable parallel build execution in travis. Note this is seprate to prallel test execution.
# -Dorg.gradle.parallel=false \

# IF WE WANT TO SKIP SOME PARTS OF THE BUILD INCLUDE THESE LINES
#      -x gwtCompile \
#      -x copyYarnBuild \
#      -x test \

# IF YOU WANT TO RUN A SPECIFIC TEST ADD LINES LIKE THIS
#       --info \
#      :stroom-app:test --tests "stroom.pipeline.task.TestFullTranslationTask" \


  # Don't do a docker build for pull requests
  if [ "$doDockerBuild" = true ] && [ "$TRAVIS_PULL_REQUEST" = "false" ] ; then
    # TODO - the major and minor floating tags assume that the release
    # builds are all done in strict sequence If say the build for v6.0.1 is
    # re-run after the build for v6.0.2 has run then v6.0-LATEST will point
    # to v6.0.1 which is incorrect, hopefully this course of events is
    # unlikely to happen
    allDockerTags=( \
      "${VERSION_FIXED_TAG}" \
      "${SNAPSHOT_FLOATING_TAG}" \
      "${MAJOR_VER_FLOATING_TAG}" \
      "${MINOR_VER_FLOATING_TAG}" \
    )

    echo -e "Logging in to Docker"
    # The username and password are configured in the travis gui
    echo "$DOCKER_PASSWORD" \
      | docker login -u "$DOCKER_USERNAME" --password-stdin >/dev/null 2>&1

    # build and release stroom image to dockerhub
    releaseToDockerHub \
      "${STROOM_DOCKER_REPO}" \
      "${STROOM_DOCKER_CONTEXT_ROOT}" \
      "${allDockerTags[@]}"

    # build and release stroom-proxy image to dockerhub
    releaseToDockerHub \
      "${STROOM_PROXY_DOCKER_REPO}" \
      "${STROOM_PROXY_DOCKER_CONTEXT_ROOT}" \
      "${allDockerTags[@]}"

    echo -e "Logging out of Docker"
    docker logout >/dev/null 2>&1
  fi

  # Deploy the generated swagger specs and swagger UI (obtained from github)
  # to gh-pages
  if [ "$TRAVIS_BRANCH" = "${CURRENT_STROOM_RELEASE_BRANCH}" ]; then
    echo "Deploying swagger-ui to gh-pages"
    ghPagesDir=$TRAVIS_BUILD_DIR/gh-pages
    swaggerUiCloneDir=$TRAVIS_BUILD_DIR/swagger-ui
    mkdir -p "${ghPagesDir}"
    # copy our generated swagger specs to gh-pages
    cp \
      "${TRAVIS_BUILD_DIR}"/stroom-app/src/main/resources/ui/swagger/swagger.* \
      "${ghPagesDir}/"
    # clone swagger-ui repo so we can get the ui html/js/etc
    git clone \
      --depth 1 \
      https://github.com/swagger-api/swagger-ui.git \
      "${swaggerUiCloneDir}"
    # copy the bits of swagger-ui that we need
    cp -r "${swaggerUiCloneDir}"/dist/* "${ghPagesDir}"/
    # repalce the default swagger spec url in swagger UI
    sed \
      -i \
      's#url: ".*"#url: "https://gchq.github.io/stroom/swagger.json"#g' \
      "${ghPagesDir}/index.html"
  fi

  # If it is a tagged build copy all the files needed for the github release
  # artefacts
  if [ -n "$TRAVIS_TAG" ]; then
    generate_ddl_dump

    generate_entity_rel_diagram

    gather_release_artefacts
  fi
fi

exit 0

# vim:sw=2:ts=2:et:

