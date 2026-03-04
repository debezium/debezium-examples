FROM python:3.11-slim

ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update \
    && apt-get install -y --no-install-recommends openjdk-21-jre-headless tini \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ENV PATH="${JAVA_HOME}/bin:${PATH}"

WORKDIR /workspace
COPY requirements.txt /tmp/requirements.txt
RUN pip install --no-cache-dir -r /tmp/requirements.txt

COPY docker/jupyter-start.sh /usr/local/bin/jupyter-start.sh
RUN chmod +x /usr/local/bin/jupyter-start.sh

EXPOSE 8888
ENTRYPOINT ["tini", "--"]
CMD ["/usr/local/bin/jupyter-start.sh"]
