

# build executable jar and docker image
mvn clean install

## To compile, build, and push the image to a remote repo:
mvn clean deploy -Ddocker.user=<username> -Ddocker.password=<passwd> -Ddocker.url=<docker-registry-url>

## run local
mvn clean install spring-boot:run

## build the docker image and start the container
mvn clean install -DskipTests
docker run -p 8080:8080 -d --name java-server com.cloudcms/server:1.0.0

## stop the docker container
docker rm --force java-server