FROM registry.fedoraproject.org/fedora-minimal
COPY target/*-runner /deployments/application
RUN chmod 775 /deployments
CMD deplyments/application
