---
jahia-authentication: patch
---

The two connector settings read actions accept POST only, and the settings UI calls them that way. This brings them under the same cross-site request policy the write actions are already under. A caller outside the UI that reached them by GET now gets 405.
