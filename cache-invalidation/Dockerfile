FROM jboss/wildfly:20.0.1.Final

ADD resources/wildfly/customization /opt/jboss/wildfly/customization/
RUN mkdir /tmp/pg-driver && cd /tmp/pg-driver && curl -sO https://jdbc.postgresql.org/download/postgresql-42.2.9.jar

RUN mkdir /tmp/infinispan-modules && \
    cd /tmp/infinispan-modules && \
    curl -sO https://downloads.jboss.org/infinispan/10.1.8.Final/infinispan-wildfly-modules-10.1.8.Final.zip && \
    unzip infinispan-wildfly-modules-10.1.8.Final.zip && \
    cp -r infinispan-wildfly-modules-10.1.8.Final/modules/* /opt/jboss/wildfly/modules && \
    rm infinispan-wildfly-modules-10.1.8.Final.zip && \
    rm -rf infinispan-wildfly-modules-10.1.8.Final

# Based on:
# https://goldmann.pl/blog/2014/07/23/customizing-the-configuration-of-the-wildfly-docker-image/
# https://tomylab.wordpress.com/2016/07/24/how-to-add-a-datasource-to-wildfly/
RUN /opt/jboss/wildfly/customization/execute.sh

RUN /opt/jboss/wildfly/bin/add-user.sh admin secret

ADD target/cache-invalidation.war /opt/jboss/wildfly/standalone/deployments/

# Fix for Error: Could not rename /opt/jboss/wildfly/standalone/configuration/standalone_xml_history/current
RUN rm -rf /opt/jboss/wildfly/standalone/configuration/standalone_xml_history

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0", "--debug"]
