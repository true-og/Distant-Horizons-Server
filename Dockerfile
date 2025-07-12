FROM maven:3.9.9-eclipse-temurin-22

# Default to first user on Debian based distros.
# Pass in your own UID and GID as environment variables to override.
ARG UID=1000
ARG GID=1000

# Create a user with the same UID and GID as the host user if they don't already exist.
RUN getent group $GID || groupadd -g $GID app
RUN getent passwd $UID || useradd -m -u $UID -g $GID -s /bin/bash -d /app app

# If an alternate user was created, let it sudo.
RUN ( getent passwd app > /dev/null && echo 'app ALL=(ALL:ALL) NOPASSWD: ALL' | tee -a /etc/sudoers > /dev/null ) || true

# Drop privileges.
USER $UID:$GID

WORKDIR /app

ENTRYPOINT [ "mvn", "-Dmaven.repo.local=/app/.m2" ]
