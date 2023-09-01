#!/usr/bin/env bash

set -xeu

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"

IMAGE_ANDROID_BUILDER=${DOCKER_REGISTRY}/android/builder-hermetic:78633b8380a9

if [[ "$CI" == "true" ]]; then
    GRADLE_HOME_DIR=/opt/buildAgent/.gradle
elif [[ -n "$GRADLE_USER_HOME" ]]; then
    GRADLE_HOME_DIR=$GRADLE_USER_HOME
else
    GRADLE_HOME_DIR=$HOME/.gradle
fi

# only need dependencies: https://docs.gradle.org/current/userguide/dependency_resolution.html#sub:ephemeral-ci-cache
GRADLE_CACHE_DIR=$GRADLE_HOME_DIR/caches/modules-2
GRADLE_WRAPPER_DIR=$GRADLE_HOME_DIR/wrapper

# Warning. Hack!
# Мы можем удалять эти локи, т.к. гарантированно никакие другие процессы не используют этот шаренный кеш на начало новой сборки
# см. clearDockerContainers
# То что лок файлы остаются от предыдущих сборок, означает что мы где-то неправильно останавливаем процесс
# '|| true' необходим для свеже-поднятых агентов, где еще не создана папка с кешами
function clearGradleLockFiles() {
    echo "Removing Gradle lock files"
    find "${GRADLE_HOME_DIR}" \( -name "*.lock" -o -name "*.lck" \) -delete || true
}

# По разным причинам работа контейнера при прошлой сборке может не завершиться
# Здесь мы перестраховываемся и останавливаем все работающие контейнеры
# Перед сборкой не должно быть других контейнеров в любом случае
function clearDockerContainers() {
    local containers=$(docker container ls -aq)
    if [[ ! -z "$containers" ]]; then
        echo "Stopping and removing containers: $containers"
        docker container rm --force ${containers}
    fi
}

clearDockerContainers
clearGradleLockFiles

function runInBuilder() {
    local USER_ID=$(id -u)

    COMMANDS=$@

    if [[ -z ${CONTAINER_MAX_CPUS+x} ]]; then
        # Default limit reflects CI build agent's limits
        # Limiting org.gradle.workers.max is not enough.
        # Other spawned processes don't respect it and use all CPUs of build agent
        # (Kotlin daemon, r8 tracereferences and so on)
        CONTAINER_MAX_CPUS=15
    fi

    docker run --rm \
        --cpus="$CONTAINER_MAX_CPUS" \
        --volume "$(pwd)":/app \
        --volume /var/run/docker.sock:/var/run/docker.sock \
        --volume "${GRADLE_CACHE_DIR}":/gradle/caches/modules-2 \
        --volume "${GRADLE_WRAPPER_DIR}":/gradle/wrapper \
        --volume "$SCRIPT_DIR/gradle.properties":/gradle/gradle.properties \
        --workdir /app \
        --env TZ="Europe/Moscow" \
        --env LOCAL_USER_ID="$USER_ID" \
        --env GRADLE_USER_HOME=/gradle \
        "${IMAGE_ANDROID_BUILDER}" \
        bash -c "${COMMANDS}"
}
