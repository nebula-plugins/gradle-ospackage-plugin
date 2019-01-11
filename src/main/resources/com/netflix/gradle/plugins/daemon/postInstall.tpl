[ -x /bin/touch ] && touch=/bin/touch || touch=/usr/bin/touch
\$touch /service/${daemonName}/down

${autoStart? installCmd : ""}