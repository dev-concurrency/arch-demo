set shell := ["sh", "-c"]
set allow-duplicate-recipes := true
set positional-arguments := true
set dotenv-load := true
set export := true

# default: compile

postgres_data_dir := justfile_directory() + "/support/.data/.postgres"
cassandra_data_dir := justfile_directory() + "/support/.data/.cassandra"
kafka_data_dir := justfile_directory() + "/support/.data/.kafka"
prometheus_data_dir := justfile_directory() + "/support/.data/.prometheus"

run-migrations:
    liquibase update --defaults-file=support/storage/postgres/liquibase.properties
    liquibase update --defaults-file=support/storage/postgres/liquibase-test.properties
    liquibase update --defaults-file=support/storage/cassandra/liquibase.properties

truncate-all:
    liquibase execute-sql \
              --sql-file=support/storage/cassandra/ddls/truncate-all.cql \
              --defaults-file=support/storage/cassandra/liquibase.properties
    liquibase execute-sql \
              --sql-file=support/storage/postgres/ddls/truncate-all.sql \
              --defaults-file=support/storage/postgres/liquibase.properties

drop-all:
    liquibase drop-all \
              --defaults-file=support/storage/postgres/liquibase.properties

[private]
[macos]
docker-compose-up:
    docker compose \
      -f support/docker-compose-storage-macos.yml \
      -f support/docker-compose-messaging.yml \
      -f support/docker-compose-observability.yml \
      up -d

[private]
[linux]
docker-compose-up:
    docker compose \
      -f support/docker-compose-storage-linux.yml \
      -f support/docker-compose-messaging.yml \
      -f support/docker-compose-observability.yml \
      up -d

infrastructure-up:
    #!/usr/bin/env bash
    set -euxo pipefail

    if [[ ! -d "{{ kafka_data_dir }}" ]]; then
      mkdir -p "{{ kafka_data_dir }}"
      mkdir -p "{{ prometheus_data_dir }}"
      sudo chmod -R 777 support/.data
    fi

    if [[ ! -d "{{ cassandra_data_dir }}" ]]; then
      firstTime=true
    else
      firstTime=false
    fi

    just docker-compose-up

    if [ "$firstTime" == "true" ]; then
      docker logs support-cassandra_temp-1 -f
      just run-migrations
    fi

[private]
[macos]
docker-compose-down:
    docker compose \
      -f support/docker-compose-storage-macos.yml \
      -f support/docker-compose-messaging.yml \
      -f support/docker-compose-observability.yml \
      down

[private]
[linux]
docker-compose-down:
    docker compose \
      -f support/docker-compose-storage-linux.yml \
      -f support/docker-compose-messaging.yml \
      -f support/docker-compose-observability.yml \
      down

infrastructure-down:
    just docker-compose-down

[confirm]
clean-infrastructure-data: infrastructure-down
    #!/usr/bin/env bash

    rm -Rf support/observability/data/grafana/grafana.db
    sudo rm -Rf support/observability/data/grafana/alerting
    sudo rm -Rf support/.data
    mkdir -p "{{ prometheus_data_dir }}"
    mkdir -p "{{ kafka_data_dir }}"
    sudo chmod -R 777 support/.data
    sudo chmod -R 777 support/observability/data/grafana
    rm -Rf logs
    echo "All infrastructure data cleaned"

[confirm]
clean-kafka-data: infrastructure-down
    sudo rm -Rf "{{ kafka_data_dir }}"
    mkdir -p "{{ kafka_data_dir }}"
    sudo chmod -R 777 "{{ kafka_data_dir }}"

lstart:
    #!/usr/bin/env bash
    set -euxo pipefail

    loki -config.file=support/observability/loki/loki-local-config.yaml &
    echo $! > .loki.pid

    promtail -config.file=support/observability/loki/promtail-local-config.yaml &
    echo $! > .promtail.pid

lstop:
    #!/usr/bin/env bash
    set -euxo pipefail

    kill -9 $(cat .loki.pid)
    kill -9 $(cat .promtail.pid)
    rm .loki.pid
    rm .promtail.pid
