#!/bin/sh
<% if (isRedhat) { %>
# chkconfig: ${runLevels.join("")} ${startSequence} ${stopSequence}
# description: Control Script for ${daemonName}

. /etc/rc.d/init.d/functions
<%
} else {
%>
### BEGIN INIT INFO
# Provides:          ${daemonName}
# Default-Start:     ${runLevels.join(" ")}
# Default-Stop:      0 1 6
# Required-Start:
# Required-Stop:
# Description: Control Script for ${daemonName}
### END INIT INFO
<% } %>

service=${daemonName}
<% if (isRedhat) { %>
subsys_lock="/var/lock/subsys/\$service"
<% } %>
daemontools_service=/service/${daemonName}

<% if (isRedhat) { %>
if [ -e /etc/sysconfig/${daemonName} ]; then
  . /etc/sysconfig/${daemonName}
fi
<%
} else {
%>
if [ -e /etc/default/${daemonName} ]; then
  . /etc/default/${daemonName}
fi
<% } %>

stop() {
    touch \$daemontools_service/down
    svc -d \$daemontools_service
}

start() {
    rm -f \$daemontools_service/down
    svc -u \$daemontools_service
}

restart() {
    svc -t \$daemontools_service
}

case \$1 in

start)
    echo -n "\${service}: "
    start
    if [ \$? -eq 0 ]
    then
<% if (isRedhat) { %>
        success
        touch \$subsys_lock
    else
        failure
<%
} else {
%>
        echo "OK"
    else
        echo "ERROR"
<% } %>
    fi
    ;;
 stop)
    echo -n "\$service:  "
    stop
    if [ \$? -eq 0 ]
    then
<% if (isRedhat) { %>
        success
        rm -f \$subsys_lock
    else
        failure
<%
} else {
%>
        echo "OK"
    else
        echo "ERROR"
<% } %>
    fi
    ;;
restart)
    echo -n "\$service: "
    restart
    if [ \$? -eq 0 ]
    then
<% if (isRedhat) { %>
        success
        touch \$subsys_lock
    else
        failure
<%
} else {
%>
        echo "OK"
    else
        echo "ERROR"
<% } %>
    fi
    ;;
  *)
    echo "Usage: service \$service {start|stop|restart}"
    exit 1
esac