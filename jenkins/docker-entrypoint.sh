#!/bin/bash
set -e

# Fix Docker socket permissions (run as root)
if [ "$(id -u)" = "0" ] && [ -S /var/run/docker.sock ]; then
    DOCKER_GID=$(stat -c '%g' /var/run/docker.sock 2>/dev/null || stat -f '%g' /var/run/docker.sock 2>/dev/null || echo "")
    if [ -n "$DOCKER_GID" ] && [ "$DOCKER_GID" != "0" ]; then
        # Ensure docker group exists with correct GID
        groupmod -g "$DOCKER_GID" docker 2>/dev/null || groupadd -g "$DOCKER_GID" docker 2>/dev/null || true
        # Add jenkins user to docker group
        usermod -aG docker jenkins 2>/dev/null || true
    fi
    # Fallback: make socket readable/writable by group
    chmod 666 /var/run/docker.sock 2>/dev/null || true
    # Switch to jenkins user
    exec gosu jenkins /usr/local/bin/jenkins.sh "$@"
else
    # Already jenkins user, just run Jenkins
    exec /usr/local/bin/jenkins.sh "$@"
fi

