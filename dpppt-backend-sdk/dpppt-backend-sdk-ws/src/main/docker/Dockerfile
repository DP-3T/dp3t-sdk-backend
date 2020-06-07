FROM adoptopenjdk:11-jre-hotspot

# Metadata
LABEL module.maintainer="DP3T" \
	  module.name="${project.artifactId}"

RUN useradd ws

COPY [ "${project.build.finalName}-exec.jar", "/home/ws/app.jar" ]
COPY [ "application.yaml", "/home/ws/application.yaml" ]

VOLUME [ "/tmp" ]

ENV JAVA_OPTS="$JAVA_OPTS -Xms256M -Xmx1G" \
    SERVER_PORT=8080

USER ws
EXPOSE ${SERVER_PORT}

ENTRYPOINT java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Dspring.config.location=/home/ws/application.yaml -jar /home/ws/app.jar
