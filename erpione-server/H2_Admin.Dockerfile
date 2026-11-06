FROM eclipse-temurin:21-jre-alpine

ADD https://repo1.maven.org/maven2/com/h2database/h2/2.3.232/h2-2.3.232.jar /opt/h2.jar

ENTRYPOINT ["sleep", "infinity"]