# WAYQO Open for Android

WAYQO Open is the GPLv3 edition of WAYQO's Android wallet and QR payment client.
It uses ZXing-C++ for local QR decoding, with no Google ML Kit or Play Services
runtime. Keys stay on the device. Payment availability, recipient verification,
quotes, identity checks and settlement are controlled by the configured service
and its providers; recognizing a QR does not authorize a payment.

## Build

Install JDK 17 and Android SDK platform 36 / build-tools 35.0.0, then:

```sh
./android-app/gradlew -p android-app --no-daemon \
  testFdroidDebugUnitTest lintFdroidDebug assembleFdroidDebug
```

The public source defaults to an invalid API origin to avoid accidentally
presenting a sandbox as a live service. A self-hosted or reviewed production
origin can be supplied with `-PPAY_API_BASE_URL=https://YOUR-ORIGIN` and
`-PPAY_IDENTITY_URI=https://YOUR-ORIGIN`. Its API must implement the contract
used by `ApiClient.kt`; this repository does not include WAYQO's private backend
or grant access to its accounts or provider credentials.

Release builds use `assembleFdroidRelease`. Without explicit signing properties
they are unsigned review artifacts. Debug APKs are not public release packages.
The stable independent ID is `app.wayqo.wallet.fdroid`. Do not replace an
existing wallet installation by uninstalling it; updates require compatible
signing certificates, and switching IDs requires an explicit recovery import.

## Distribution

Signed official downloads and an F-Droid listing are in preparation. No listing,
production availability or signed public release is claimed by this source
publication. Direct downloads will be published on this repository's Releases
page after production configuration and signing checks pass.

## License

Company-authored code and app artwork are under **GPL-3.0-only**, in `LICENSE`.
Third-party notices and licenses are in `android-app/THIRD_PARTY_NOTICES.md`
and `android-app/app/src/main/assets/legal/`. The code is provided without
warranty, subject to applicable law. Brand rights are addressed separately in
`TRADEMARK_POLICY.md`; no permission to impersonate the official service is
granted.

Contributions are welcome under GPLv3. Inclusion in a separately licensed
Standard edition requires additional permission; submitting a GPL patch does
not automatically grant commercial relicensing rights.
