---
jahia-authentication: patch
---

Changed SSO logins so the account is resolved against the site configured on the authentication connector rather than the site named in the post-login URL.

A setup that expects to sign in with accounts belonging to another site needs a connector configured for that site. SSO connectors sign in ordinary accounts; the root account and `guest` are outside the set they resolve.
