#!/bin/bash

set -e
set -o pipefail

apt-get update 
dpkg -i *.deb

echo "test success"