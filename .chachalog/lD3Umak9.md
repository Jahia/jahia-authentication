---
jahia-authentication: patch
---

Changed SSO logins so the account is always resolved against the site configured on the authentication connector.

Sign-in resolves the account on the connector's own site whatever site the post-login URL names, and signing in as the platform administrator through an SSO connector is not supported. A setup that expects to sign in with accounts belonging to another site needs a connector configured for that site.
