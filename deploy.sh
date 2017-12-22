#!/bin/bash

set -e

DATE=`date +%s`

openssl aes-256-cbc -K $encrypted_0efec95fe1a5_key -iv $encrypted_0efec95fe1a5_iv -in codesigning.asc.enc -out codesigning.asc -d
gpg --fast-import codesigning.asc

deb-s3 upload -a armhf -c r2cloud --access-key-id=${AWS_ACCESS_KEY} --secret-access-key=${AWS_SECRET_ACCESS_KEY}  -m main --sign=27679FBF --gpg-options="--passphrase ${GPG_PASSPHRASE}" --bucket r2cloud ./target/*.deb
