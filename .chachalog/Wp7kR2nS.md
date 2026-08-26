---
# Allowed version bumps: patch, minor, major
jahia-authentication: patch
---

Fixed SSO sign-in for connectors that record their result directly, such as the Keycloak connector, which stopped signing users in and logged that the cached identity carried no site key.
