# Library-only scope (v6)

The goal is **only** to list and read books already owned by the user. No store, shopping, purchasing, or subscription flows.

## Included today

- Android Kotlin/Jetpack Compose app with a simple My Library screen.
- Existing local file list and file picker for unprotected EPUB/PDF documents. EPUB text preview is basic; PDF rendering remains unimplemented.
- Removed Fidiplus subscription navigation and screen.
- Account information screen links to Fidibo's official website, which runs outside this app.

## Not yet available

- Existing purchased Fidibo books **do not automatically appear** in this app. There is no verified authorized sign-in/private-library API in the provided project, so implementing a fake login would be misleading.
- The official website button does not sync books or transfer a session to this app.
- To integrate: obtain Fidibo's supported account authorization and read-only owned-library API specifications, including entitlements and permitted reading methods.
- Avoid embedding credentials, importing cookies, or attempting to bypass copy protection.

Use GitHub Actions > Build installable debug APK to build.
