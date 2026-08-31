# Security

Report suspected vulnerabilities privately to the repository maintainers before opening a public issue. Do not include webhook URLs, Bearer tokens, notification text, card numbers, or other personal data in reports.

Do not report vulnerabilities in public issues. If no private security contact is configured on the hosting service, open a minimal issue asking for a private contact without including technical details.

The app accepts only HTTPS webhook URLs. Optional Bearer tokens are encrypted with Android Keystore. No credentials or notification content are sent anywhere except the webhook configured by the device owner.
