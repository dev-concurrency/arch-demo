set shell := ["sh", "-c"]
set allow-duplicate-recipes := true
set positional-arguments := true
set dotenv-load := true
set export := true

# default: compile

alias dc-u := docker-compose-up
alias dc-d := docker-compose-down
alias clean-dd := clean-docker-data

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

docker-compose-up:
    #!/usr/bin/env bash
    set -euxo pipefail

    if [[ ! -d "{{ cassandra_data_dir }}" ]]; then
      firstTime=true
    else
      firstTime=false
    fi

    if [[ {{ os() }} == "macos" ]]; then
      docker compose \
        -f support/docker-compose-storage-macos.yml \
        -f support/docker-compose-messaging.yml \
        -f support/docker-compose-observability.yml \
        up -d
    else
      docker compose \
        -f support/docker-compose-storage-linux.yml \
        -f support/docker-compose-messaging.yml \
        -f support/docker-compose-observability.yml \
        up -d
    fi

    if [ "$firstTime" == "true" ]; then
      docker logs support-cassandra_temp-1 -f
      just run-migrations
    fi

docker-compose-down:
    #!/usr/bin/env bash
    set -euxo pipefail

    if [[ {{ os() }} == "macos" ]]; then
      docker compose \
        -f support/docker-compose-storage-macos.yml \
        -f support/docker-compose-messaging.yml \
        -f support/docker-compose-observability.yml \
        down
    else
      docker compose \
        -f support/docker-compose-storage-linux.yml \
        -f support/docker-compose-messaging.yml \
        -f support/docker-compose-observability.yml \
        down
    fi

clean-docker-data: docker-compose-down
    rm -Rf support/observability/data/grafana/grafana.db
    sudo rm -Rf support/observability/data/grafana/alerting
    sudo rm -Rf support/.data
    mkdir -p support/.data/.prometheus
    mkdir -p support/.data/.kafka
    chmod -R 777 support/.data
    chmod -R 777 support/observability/data/grafana

clean-kafka-data: docker-compose-down
    sudo rm -Rf support/.data/.kafka
    mkdir -p support/.data/.kafka
    chmod -R 777 support/.data/.kafka

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
