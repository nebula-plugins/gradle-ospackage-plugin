#!/bin/sh
mkdir -p ${logDir}
chown ${logUser}:nobody ${logDir}
exec setuidgid ${logUser} ${logCommand}