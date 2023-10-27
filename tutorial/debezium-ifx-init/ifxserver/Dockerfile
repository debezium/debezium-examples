FROM icr.io/informix/informix-developer-database:14.10.FC9W1DE

LABEL maintainer="Debezium Community"

ADD --chown=informix:informix --chmod=775 informix_post_init.sh /opt/ibm/config
ADD --chown=informix:informix --chmod=775 inventory.sql /opt/ibm/informix/etc

ENV DBDATE Y4MD-
ENV TYPE oltp
ENV SIZE medium
ENV LICENSE accept
ENV RUN_FILE_POST_INIT informix_post_init.sh
