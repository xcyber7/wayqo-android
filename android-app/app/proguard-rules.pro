-keep class com.solana.mobilewalletadapter.** { *; }

# BouncyCastle lightweight crypto (embedded wallet ed25519) — keep the APIs we call.
-keep class org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters { *; }
-keep class org.bouncycastle.crypto.params.Ed25519PublicKeyParameters { *; }
-keep class org.bouncycastle.crypto.signers.Ed25519Signer { *; }
-dontwarn org.bouncycastle.**

# org.json is provided by the Android platform.
-dontwarn org.json.**
