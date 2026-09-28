# Android third-party notices

The Open runtime dependency inventory is bundled in
`app/src/main/assets/legal/DEPENDENCIES.txt`. Exact coordinates and licenses
are checked from the resolved artifacts and cached Maven POMs; upstream
license files and notices are retained in `legal/THIRD-PARTY.txt`.

- AndroidX, Kotlin/coroutines, ZXing Java, ZXing-C++ 2.3.0, Solana Mobile Wallet
  Adapter 2.0.7 and libphonenumber use Apache-2.0. Their original notices remain.
- AndroidX Camera also includes libyuv under its BSD license; Salkt 0.1.0
  uses MIT. Their complete notices are bundled.
- Bouncy Castle bcprov-jdk18on 1.78.1 uses the Bouncy Castle MIT-style license.
- The unchanged English BIP39 list and test vectors are from Trezor
  python-mnemonic v0.21, MIT: Copyright (c) 2013 Pavol Rusnak.
- Test/build tooling retains its upstream license, including JUnit 4 (EPL-1.0)
  and the Gradle wrapper (Apache-2.0); those do not become GPL by this notice.
- The private Standard source set uses Google ML Kit under Google's SDK terms.
  It and its dependencies are excluded from the Open public source/runtime.

References:

- https://github.com/trezor/python-mnemonic/tree/v0.21
- https://github.com/zxing-cpp/zxing-cpp/tree/v2.3.0
- https://github.com/zxing/zxing
- https://github.com/solana-mobile/mobile-wallet-adapter/tree/v2.0.7
- https://github.com/bcgit/bc-java/tree/r1rv78
- https://github.com/google/libphonenumber
- https://android.googlesource.com/platform/frameworks/support/
- https://github.com/JetBrains/kotlin

Bank and payment-rail logos are remote references and are not bundled as app
artwork. They remain their owners' marks. Company-authored WAYQO app artwork
is GPL-3.0-only; trademark rights are addressed separately.
