#!/bin/sh -e

case "\$1" in
    install)
        <% commands.each {command -> %>
        <%= command %>
        <% } %>
        ;;
esac
