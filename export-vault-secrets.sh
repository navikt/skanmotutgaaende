#!/usr/bin/env sh

if test -f /var/run/secrets/nais.io/srvskanmotutgaaende/username;
then
    echo "Setting SERVICEUSER_USERNAME"
    export SKANMOTUTGAAENDE_SERVICEUSER_USERNAME=$(cat /var/run/secrets/nais.io/srvskanmotutgaaende/username)
fi

if test -f /var/run/secrets/nais.io/srvskanmotutgaaende/password;
then
    echo "Setting SERVICEUSER_PASSWORD"
    export SKANMOTUTGAAENDE_SERVICEUSER_PASSWORD=$(cat /var/run/secrets/nais.io/srvskanmotutgaaende/password)
fi