---
jahia-authentication: major
---

Changed SSO logins so each account records the identity its provider asserted, and a login is matched on that record.

An account created by an SSO login records the provider that signed it in. It records the identity that provider asserted as well. A later login for the same identity reaches the same account, whatever the account is named, so a deployment chooses freely how accounts are named.

Accounts created before this version carry no such record. Two conditions then meet: a login's identity matches no record, and an account of that name already exists. No account is created and no one is signed in, and the module reports that the account has to be linked. Record the identity of each account an SSO login created before you upgrade.
