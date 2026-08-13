---
jahia-authentication: patch
---

Changed SSO logins so the account is resolved against the site configured on the authentication connector rather than the site named in the post-login URL.

A setup that expects to sign in with accounts belonging to another site needs a connector configured for that site. SSO connectors sign in ordinary accounts; the root account and `guest` are outside the set they resolve.

A session that authenticated through several connectors now has to get one and the same answer from them — the same account, on the same site — to be signed in. Two connectors naming different accounts, or one account on different sites, sign in no one instead of whichever the cache happened to return first.
