#!/usr/bin/env sh

if test -f /var/run/secrets/nais.io/srvskanmotutgaaende/username;
then
    echo "Setting SERVICEUSER_USERNAME"
    export SKANMOTUTGAAENDE_SERVICEUSER_USERNAME=$(cat /var/run/secrets/nais.io/skanmotutgaaende/username)
fi

if test -f /var/run/secrets/nais.io/srvskanmotutgaaende/password;
then
    echo "Setting SERVICEUSER_PASSWORD"
    export SKANMOTUTGAAENDE_SERVICEUSER_***passord=gammelt_passord***)
fi