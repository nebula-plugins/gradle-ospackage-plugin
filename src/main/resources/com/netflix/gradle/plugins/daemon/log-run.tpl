#!/bin/sh
mkdir -p ${logDir}
chown ${logUser} ${logDir}
exec setuidgid ${logUser} ${logCommand}
