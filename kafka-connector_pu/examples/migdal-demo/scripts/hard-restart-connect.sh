export DEBEZIUM_VERSION=1.6
docker-compose stop connect && docker-compose rm -f connect && docker-compose up -d --force-recreate --no-deps connect
