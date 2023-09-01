#!/usr/bin/env bash

source $(dirname $0)/_main.sh

# force Envs to be set up
set -uex

readonly user="-Pavito.ossrh.user=${OSSRH_USER}"
readonly password="-Pavito.ossrh.password=${OSSRH_PASSWORD}"
readonly profileId="-Pavito.ossrh.stagingProfileId=${OSSRH_STAGING_ID}"
readonly CREDENTIALS="${user} ${password} ${profileId} "

readonly gradle_properties_path="gradle.properties"
if [[ -f ${gradle_properties_path} ]]; then
    # contains spaces and newlines so hard to pass as argument
    echo "avito.pgp.key=$PGP_KEY" >> ${gradle_properties_path}
else
    echo "${gradle_properties_path} file doesn't exist"
    exit 1;
fi
readonly pgp_key_id="-Pavito.pgp.keyid=${PGP_KEY_ID}"
readonly pgp_password="-Pavito.pgp.password=${PGP_PASSWORD}"
readonly PGP="${pgp_key_id} ${pgp_password} "

GRADLE_ARGS+=${CREDENTIALS}
GRADLE_ARGS+=${PGP}
GRADLE_ARGS+="--no-daemon "

runInBuilder "./gradlew :blur-layout:publishToSonatype closeAndReleaseSonatypeStagingRepository ${GRADLE_ARGS}"
