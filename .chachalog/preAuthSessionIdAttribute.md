---
jahia-authentication: minor
---

A module that reacts to Jahia's login event can now find the mapper results an SSO connector produced for that login. The SSO valve publishes the session id they are cached under as a request attribute, `org.jahia.modules.jahiaauth.preAuthenticationSessionId`, before it authenticates.

**This affects you if** you wrote a mapper whose results are read back from the login event rather than applied in `executeMapper`. Until now there was no way to read them: authentication replaces the session and publishes the login event from inside itself, so asking for the results of the current session found nothing, and the valve re-keys the cached entry onto the new session id only after that event has been delivered. Read the attribute and fall back to the current session id when it is absent.
