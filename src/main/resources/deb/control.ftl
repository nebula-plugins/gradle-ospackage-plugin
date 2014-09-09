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
Depends: ${depends}<% } %>
Description: ${summary}
 ${description}
