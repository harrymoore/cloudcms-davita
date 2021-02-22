

# build executable jar
mvn clean install

## run local
mvn clean package -DskipTests spring-boot:run

cache and log folders will be created automatically:
    cache: ./cache/*
    logs: ./logs/*