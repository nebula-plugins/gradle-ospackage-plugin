#!/bin/sh -e

ec() {
    echo "\$@" >&2
    "\$@"
}

case "\$1" in
    configure)
        <% commands.each {command -> %>
        <%= command %>
        <% } %>
        ;;
esac
