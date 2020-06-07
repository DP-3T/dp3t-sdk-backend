FROM adoptopenjdk:11-jre-hotspot

# Metadata
LABEL module.maintainer="DP3T" \
	  module.name="${project.artifactId}"

COPY  [ "${project.build.finalName}-exec.jar", "/app.jar" ]

RUN useradd ws \
 && sh -c 'touch /home/ws/app.jar'

VOLUME [ "/tmp" ]

ENV JAVA_OPTS="$JAVA_OPTS -Xms256M -Xmx1G" \
    SERVER_PORT=8080

USER ws
EXPOSE ${SERVER_PORT}

ENTRYPOINT java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar
