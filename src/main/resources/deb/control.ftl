Source: ${name}
Section: ${section}
Priority: optional
Maintainer: ${author}
Uploaders: ${author}
Version: ${version}-${release}
Standards-Version: 3.8.3
Package: ${name}
Provides: ${provides}
Homepage: ${url}
Architecture: ${arch}<% if (multiArch) { %>
Multi-Arch: ${multiArch}<% } %>
Distribution: unstable<% if (depends) { %>
Depends: ${depends}<% } %><% if (conflicts) { %>
Conflicts: ${conflicts}<% } %><% if (replaces) { %>
Replaces: ${replaces}<% } %><% if (recommends) { %>
Recommends: ${recommends}<% } %><% if (suggests) { %>
Suggests: ${suggests}<% } %><% if (enhances) { %>
Enhances: ${enhances}<% } %><% if (preDepends) { %>
Pre-Depends: ${preDepends}<% } %><% if (breaks) { %>
Breaks: ${breaks}<% } %>
Description: ${summary}
 ${description}
