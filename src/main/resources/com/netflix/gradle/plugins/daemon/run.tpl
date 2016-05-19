#!/bin/sh
exec 2>&1
ulimit -n 32768
ulimit -u 10240
exec setuidgid ${user} ${command}