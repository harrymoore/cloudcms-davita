# build executable jar
mvn clean install -DskipTests

## run executable jar
java -jar ./target/content-server-1.0.0-spring-boot.jar

## run local
mvn clean package -DskipTests spring-boot:run

cache and log folders will be created automatically:
    cache: ./cache/*
    logs: ./logs/*