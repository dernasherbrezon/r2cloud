#!/bin/bash

set -e

DATE=$(date +%s)

openssl aes-256-cbc -K $encrypted_0efec95fe1a5_key -iv $encrypted_0efec95fe1a5_iv -in codesigning.asc.enc -out codesigning.asc -d
gpg --fast-import codesigning.asc

deb-s3 upload -c r2cloud --access-key-id=${AWS_ACCESS_KEY} --secret-access-key=${AWS_SECRET_ACCESS_KEY}  -m main --sign=A5A70917 --gpg-options="--passphrase ${GPG_PASSPHRASE} --digest-algo SHA256" --bucket r2cloud ./target/*.deb
deb-s3 upload -c stretch --access-key-id=${AWS_ACCESS_KEY} --secret-access-key=${AWS_SECRET_ACCESS_KEY}  -m main --sign=A5A70917 --gpg-options="--passphrase ${GPG_PASSPHRASE} --digest-algo SHA256" --bucket r2cloud ./target/*.deb
deb-s3 upload -c buster --access-key-id=${AWS_ACCESS_KEY} --secret-access-key=${AWS_SECRET_ACCESS_KEY}  -m main --sign=A5A70917 --gpg-options="--passphrase ${GPG_PASSPHRASE} --digest-algo SHA256" --bucket r2cloud ./target/*.deb
deb-s3 upload -c bullseye --access-key-id=${AWS_ACCESS_KEY} --secret-access-key=${AWS_SECRET_ACCESS_KEY}  -m main --sign=A5A70917 --gpg-options="--passphrase ${GPG_PASSPHRASE} --digest-algo SHA256" --bucket r2cloud ./target/*.deb
deb-s3 upload -c bionic --access-key-id=${AWS_ACCESS_KEY} --secret-access-key=${AWS_SECRET_ACCESS_KEY}  -m main --sign=A5A70917 --gpg-options="--passphrase ${GPG_PASSPHRASE} --digest-algo SHA256" --bucket r2cloud ./target/*.deb
deb-s3 upload -c focal --access-key-id=${AWS_ACCESS_KEY} --secret-access-key=${AWS_SECRET_ACCESS_KEY}  -m main --sign=A5A70917 --gpg-options="--passphrase ${GPG_PASSPHRASE} --digest-algo SHA256" --bucket r2cloud ./target/*.deb
