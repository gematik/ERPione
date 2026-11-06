
# How-to build and run Erpione

## Build executable jar for the Erpione Service (resource-server)

```shell
cd $(git rev-parse --show-toplevel)

mvn clean install -DskipTests
mvn package spring-boot:repackage -f erpione-server/pom.xml -DskipTests -o
```

## Run Keycloak as standalone


## Run Erpione locally with Docker-Compose

```shell
cd $(git rev-parse --show-toplevel)

export ENVIRONMENT=rudev
export ERP_FHIR_PROFILE=1.6.0
export ACTIVE_SPRING_PROFILES=${ENVIRONMENT},logging,noauth
export PUBLIC_PORT=8080

#docker volume create "erpione-data-${ENVIRONMENT}"

docker compose -f dc-erpione-local.yml up -d --build
```

## Copy Frontend-Updates to running container

```shell
cd $(git rev-parse --show-toplevel)

export ENVIRONMENT=rudev

docker cp ./erpione-frontend/dist/. erpione-ui-${ENVIRONMENT}:/usr/share/nginx/html/
```

## Create Documentation

```shell
#!/bin/sh

cd $(git rev-parse --show-toplevel)/adocs

doc_index="index"

# prepare target directory
mkdir ./target

docker create --name erpione-doc -e PLANTUML_URL="https://plantuml.gematik.solutions/plantuml/" eu.gcr.io/gematik-all-infra-prod/shared/gematik-asciidoc-converter:latest /tmp/doc/$doc_index.adoc

docker cp ./. erpione-doc:/tmp/doc
docker start --attach erpione-doc

docker cp erpione-doc:/tmp/doc/$doc_index.pdf ./target
docker cp erpione-doc:/tmp/doc/$doc_index.html ./target

docker rm erpione-doc

cp -r ./images ./target/images
```