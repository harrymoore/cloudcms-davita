#!/bin/bash

docker network create keycloak_network

docker run -d --name postgres --pull missing -p 5432:5432 --net keycloak-network -e POSTGRES_DB=keycloak -e POSTGRES_USER=keycloak -e POSTGRES_PASSWORD=password postgres
# docker run --name mysql -d --net keycloak_network -e MYSQL_DATABASE=keycloak -e MYSQL_USER=keycloak -e MYSQL_PASSWORD=password -e MYSQL_ROOT_PASSWORD=root_password mysql


# docker build . -t keycloak --force-rm
# docker run -p 9090:8080 --network cloudcms_cloudcms --env-file ./keycloak.env keycloak
# docker run -p 9090:8080 --net keycloak-network -e DB_USER=keycloak -e DB_PASSWORD=password keycloak
# docker run --name keycloak --net keycloak-network jboss/keycloak -e DB_USER=keycloak -e DB_PASSWORD=password -e DB_ADDR=keycloak DB_VENDOR=postgres
# docker run --name keycloak -p 9090:8080 --net keycloak_network --env-file ./keycloak-mysql.env jboss/keycloak
docker run --name keycloak --pull missing -p 9090:8080 --net keycloak_network --env-file ./keycloak.env jboss/keycloak
