# GitHub repository setup

After publishing the repository, configure these settings in GitHub:

1. Protect the default branch: require pull requests, one maintainer approval, and successful `build` and `hygiene` checks; dismiss stale approvals; block force-pushes and branch deletion.
2. Enable Dependabot security updates and grouped monthly version updates.
3. Enable secret scanning and push protection when available for the repository.
4. Add the maintainers to `.github/CODEOWNERS` once their GitHub usernames are known. Require CODEOWNERS review for `app/src/main/java/**`, `app/src/test/**`, `SECURITY.md`, and `PRIVACY.md`.
5. Create labels for `provider`, `bug`, `security`, and `documentation`, then add a first milestone for the next release.
6. Publish tagged releases and keep `CHANGELOG.md` updated.

## Release signing

Create one long-lived Android app-signing key and keep an offline backup. Never commit the keystore or its passwords. Add these GitHub Actions repository secrets:

- `ANDROID_SIGNING_KEYSTORE_BASE64`: the complete keystore file encoded as Base64;
- `ANDROID_SIGNING_STORE_PASSWORD`;
- `ANDROID_SIGNING_KEY_ALIAS`;
- `ANDROID_SIGNING_KEY_PASSWORD`.

The release workflow runs when a tag matching `v*` is pushed. The tag without its leading `v` must equal `versionName` in `app/build.gradle.kts`. Before releasing, update `versionCode`, `versionName`, and `CHANGELOG.md`, commit them, then create and push the tag:

```sh
git tag v0.1.0
git push origin v0.1.0
```

The workflow tests the project, builds and verifies the signed APK, creates `SHA256SUMS`, and publishes both files in a GitHub Release. Android updates must always use the same signing key.

The repository's contribution scope remains limited to payment-related notification handling. Pull requests outside that scope should be closed rather than merged.
