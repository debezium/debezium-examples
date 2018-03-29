FROM jboss/wildfly:12.0.0.Final

ADD resources/wildfly/customization /opt/jboss/wildfly/customization/

# Based on:
# https://goldmann.pl/blog/2014/07/23/customizing-the-configuration-of-the-wildfly-docker-image/
# https://tomylab.wordpress.com/2016/07/24/how-to-add-a-datasource-to-wildfly/
RUN /opt/jboss/wildfly/customization/execute.sh

ADD target/hikr-1.0-SNAPSHOT.war /opt/jboss/wildfly/standalone/deployments/

# Fix for Error: Could not rename /opt/jboss/wildfly/standalone/configuration/standalone_xml_history/current
RUN rm -rf /opt/jboss/wildfly/standalone/configuration/standalone_xml_history
