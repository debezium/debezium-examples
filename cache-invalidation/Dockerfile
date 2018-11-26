FROM jboss/wildfly:14.0.1.Final

ADD resources/wildfly/customization /opt/jboss/wildfly/customization/
RUN mkdir /tmp/pg-driver && cd /tmp/pg-driver && curl -sO https://jdbc.postgresql.org/download/postgresql-42.2.5.jar

# Based on:
# https://goldmann.pl/blog/2014/07/23/customizing-the-configuration-of-the-wildfly-docker-image/
# https://tomylab.wordpress.com/2016/07/24/how-to-add-a-datasource-to-wildfly/
RUN /opt/jboss/wildfly/customization/execute.sh

RUN /opt/jboss/wildfly/bin/add-user.sh admin secret

ADD target/cache-invalidation.war /opt/jboss/wildfly/standalone/deployments/

# Fix for Error: Could not rename /opt/jboss/wildfly/standalone/configuration/standalone_xml_history/current
RUN rm -rf /opt/jboss/wildfly/standalone/configuration/standalone_xml_history

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0", "--debug"]
