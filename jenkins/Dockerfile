FROM jenkins/jenkins:lts
USER root
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN apt-get update && \
    apt-get install -y docker.io curl gosu && \
    curl -L "https://github.com/docker/compose/releases/download/v2.24.5/docker-compose-linux-x86_64" -o /usr/local/bin/docker-compose && \
    chmod +x /usr/local/bin/docker-compose && \
    chmod +x /usr/local/bin/docker-entrypoint.sh && \
    groupadd -f docker && \
    usermod -aG docker jenkins && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*
ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
USER root
