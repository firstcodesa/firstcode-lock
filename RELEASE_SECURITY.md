# Release and trust requirements

- Build release variant with debugging disabled. Sign locally with the permanent private release key; never commit it or its password.
- Keep the signing backup private. A single migration uninstall from the old ephemeral debug signer is necessary; future releases must reuse the permanent key.
- Target user permission: USE_BIOMETRIC only. Accessibility service and Device Admin receiver are guarded by their Android BIND permissions.
- Accessibility is explicitly not advertised as an accessibility aid: isAccessibilityTool=false, no screen-content retrieval, no gestures. Explain its foreground-app use before opening system permission settings.
- Device Admin is optional, has no destructive policies, and remains owner-deactivatable. Never block Settings, installer, home, or emergency dialer.
- No network permission, ad SDKs, telemetry, remote code, hidden icon, or automatic permission granting. Backups and cleartext traffic disabled. Secure windows protect PIN screens from ordinary screenshots.
- Release signing and these checks do not establish Google approval or guarantee absence of Play Protect warnings. Internet-sideloaded apps using accessibility may still be restricted. Do not ask users to disable Play Protect. Review official guidance, then submit an appeal if classification is erroneous.
- Public distribution through Play requires developer account verification, accurate store declarations and privacy disclosures, and Google review; it has not been performed by this workflow.

Official references:
https://developers.google.com/android/play-protect/warning-dev-guidance
https://developer.android.com/studio/publish/app-signing
https://developer.android.com/work/device-admin

## Device acceptance still needed
Physical biometric success and enrollment changes, Samsung permission screens, Device Admin enable/deactivate/uninstall, service restart and battery optimization, and locking third-party apps must be checked on the intended phone. The CI emulator is evidence for its covered tests only, not a certification or security audit by Google.
