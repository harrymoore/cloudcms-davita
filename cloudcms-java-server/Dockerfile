FROM openjdk:15-jdk-slim

EXPOSE 80

WORKDIR /app

# ARG JAR_FILE=target/*.jar
# COPY ${JAR_FILE} app.jar
# COPY ./target/*docker-jar.jar /app.jar
# COPY ./target/*executable.jar app.jar
COPY ./target/server-1.0.0.jar app.jar


# ARG CONFIG_DIRECTORY=/
COPY docker-application.properties docker-application.properties
COPY docker-gitana.properties gitana.properties

# Generate a client key pair:
# RUN keytool -genkey -alias aClient -keyalg RSA -keystore clientKeyStore.key -storepass changeitX
# Generate a client certficate:
# RUN keytool -export -alias aClient -keystore clientKeyStore.key -rfc -file aClient.cert

# ENV JAVA_OPTS="-Xmn2g -Xmx3g -server -Dspring.config.location=file:application.properties -Djavax.net.ssl.keyStore=clientKeyStore.key -Djavax.net.ssl.keyStorePassword=changeitX"
ENV JAVA_OPTS="-Xmn2g -Xmx3g -server -Dspring.config.additional-location=file:docker-application.properties -Djavax.net.ssl.keyStore=clientKeyStore.key -Djavax.net.ssl.keyStorePassword=changeitX"

ENTRYPOINT ["java", "-jar", "app.jar"]