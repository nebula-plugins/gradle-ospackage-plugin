#!/bin/sh -e

ec() {
    echo "\$@" >&2
    "\$@"
}

case "\$1" in
    configure)
        <% dirs.each{ dir -> %>
        <% if (dir['owner']) { %>
            ec install -o <%= dir['owner'] %> -d <%= dir['name'] %>
        <% } else { %>
            ec install -d <%= dir['name'] %>
        <% } %>
        <% } %>

        <% commands.each {command -> %>
        <%= command %>
        <% } %>
        ;;
esac
