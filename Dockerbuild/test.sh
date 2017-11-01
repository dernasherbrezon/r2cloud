#!/bin/bash

set -e
set -o pipefail

systemctl start r2cloud
systemctl start nginx 

echo "test success"
exit 0
