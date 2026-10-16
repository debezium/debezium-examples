# Copyright Debezium Authors.
#
# Licensed under the Apache License version 2.0, available at
# http://www.apache.org/licenses/LICENSE-2.0

ARG PLATFORM_VERSION=nightly
FROM quay.io/debezium/platform-conductor:${PLATFORM_VERSION}

# This local-only example mounts a user-owned, read-only SSH directory. Running
# the container as root lets Ansible read the private key without weakening its
# file permissions on the Linux host.
USER root

# The host runtime runs the bundled Ansible playbooks. The callbacks, Docker
# image-pull task, and optional Agent Maven-download task use these collections.
RUN microdnf install -y openssh-clients python39 \
    && python3.9 -m ensurepip --upgrade \
    && python3.9 -m pip install --no-cache-dir --upgrade pip \
    && python3.9 -m pip install --no-cache-dir ansible-core \
    && ansible-galaxy collection install ansible.posix community.docker community.general \
    && microdnf clean all
