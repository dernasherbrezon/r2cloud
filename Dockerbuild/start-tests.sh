#!/bin/bash

set -e
set -o pipefail

/home/pi/r2cloud-jdk/bin/java -cp /home/pi/r2cloud/etc:/home/pi/r2cloud-tests/lib/* -Djava.compiler=NONE -Duser.timezone=UTC -Dr2cloud.baseurl=http://localhost:8097 -Djava.util.logging.config.file=/home/pi/r2cloud/etc/logging-prod.properties ru.r2cloud.it.IntegrationalTests