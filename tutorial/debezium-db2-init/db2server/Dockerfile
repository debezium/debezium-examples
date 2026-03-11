FROM ibmcom/db2:11.5.4.0

LABEL maintainer="Debezium Community"

RUN mkdir -p /asncdctools/src

ADD asncdc_UDF.sql /asncdctools/src
ADD asncdcaddremove.sql /asncdctools/src
ADD asncdctables.sql /asncdctools/src
ADD dbsetup.sh /asncdctools/src
ADD startup-agent.sql /asncdctools/src
ADD startup-cdc-demo.sql /asncdctools/src
ADD inventory.sql /asncdctools/src
ADD asncdc.c /asncdctools/src

RUN mkdir /var/custom && \
    chmod -R  777 /asncdctools && \
    chmod -R  777 /var/custom

ADD cdcsetup.sh /var/custom
ADD custom-init /var/custom-init

RUN chmod -R 777 /var/custom-init

ADD openshift_entrypoint.sh /var/db2_setup/lib

RUN chmod 777 /var/custom/cdcsetup.sh && \
    chmod 777 /var/db2_setup/lib/openshift_entrypoint.sh
