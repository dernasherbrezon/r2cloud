#!/bin/bash

set -e
set -o pipefail

java -cp /home/pi/r2cloud/etc:/home/pi/r2cloud/lib/* -Duser.timezone=UTC -Djava.util.logging.config.file=/home/pi/r2cloud/etc/logging-prod.properties ru.r2cloud.IntegrationalTests