FROM registry.fedoraproject.org/fedora-minimal
COPY target/*-runner.jar /deployments/application
RUN chmod 775 /deployments/application
CMD deplyments/application
