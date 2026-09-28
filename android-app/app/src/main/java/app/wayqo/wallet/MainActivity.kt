// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import java.math.BigDecimal
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.NumberFormat
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class Step {
    CONNECT, HOME, PROFILE, KYC, MARKET, SCAN, CONFIRM, QUOTE, PAYING, COMPLETE,
    SEND, SEND_SCAN, SEND_CONFIRM, SENDING, SEND_COMPLETE, FUNDING, RECEIVE, ACTIVITY, SAVED,
    BACKUP, RESTORE, PHRASE, WALLETS, MANUAL
}

// Residency options now come from RESIDENCY_COUNTRIES (Countries.kt): all real ISO
// countries except the blocked set, matching the backend blocklist in internal/policy.

// This is only an input-length bound. Provider and corridor amount limits are
// checked by the quote; no unverified fiat or wallet cap is imposed here.
const val PAYMENT_MAX_DIGITS = 12

val PAYMENT_PURPOSES = listOf(
    "food_dining" to "Food & dining",
    "groceries" to "Groceries",
    "shopping" to "Shopping",
    "goods_services" to "Goods & services",
    "transport" to "Transport",
    "travel" to "Travel",
    "accommodation" to "Accommodation",
    "bills_utilities" to "Bills & utilities",
    "entertainment" to "Entertainment",
    "health" to "Health",
    "education" to "Education",
    "gift" to "Gift",
    "personal_transfer" to "Personal transfer",
    "other" to "Other",
)

data class AppState(
    val step: Step = Step.CONNECT,
    val busy: Boolean = false,
    val wallet: String = "",
    val walletName: String = "Main wallet",
    val balanceUsd: String = "$0.00",
    val balanceLoaded: Boolean = false,
    val balanceHidden: Boolean = false,
    val paymentNote: String = "",
    val hardwareProtected: Boolean = true,
    val isEmbeddedWallet: Boolean = false,
    val backupPassphrase: String = "",
    val backupPassphraseConfirm: String = "",
    val backupBlob: String = "",
    val restoreBlobInput: String = "",
    val restorePassphrase: String = "",
    val restorePhraseInput: String = "",
    val recoveryPhrase: String = "",
    val recoveryCheck: String = "",
    val recoveryPositions: List<Int> = emptyList(),
    val recoveryWritten: Boolean = false,
    val recoveryVerified: Boolean = false,
    val localWallets: List<String> = emptyList(),
    val externalWallets: List<String> = emptyList(),
    val backupStatus: String = "Not backed up",
    val backupWalletAddress: String = "",
    val backupReturnStep: Step = Step.HOME,
    val backupHasMnemonic: Boolean = false,
    val manualCountry: String = "",
    val manualBank: String = "",
    val manualAccount: String = "",
    val sharedImage: String = "",
    val scanReturnStep: Step = Step.CONNECT,
    val receiveReturnStep: Step = Step.HOME,
    val fundingReturnStep: Step = Step.HOME,
    val qrRoute: String = "",
    val residencyCountry: String = "",
    val userId: String = "",
    val kycStatus: String = "",
    val kycUrl: String = "",
    val merchantPayReady: Boolean = false,
    val paymentMarkets: List<PaymentMarket> = emptyList(),
    val paymentCountry: String = "",
    val paymentNetwork: String = "",
    val sourceOfFundsConfirmed: Boolean = false,
    val paymentPurpose: String = "",
    val message: String = "Connect a Solana wallet to begin.",
    val qrPayload: String = "",
    val parsed: ParsedQr? = null,
    val amount: String = "",
    val quote: Quote? = null,
    val order: Order? = null,
    val peerRecipientInput: String = "",
    val peerRecipient: String = "",
    val peerAmount: String = "",
    val peerMint: String = "",
    val peerSignature: String = "",
    val activities: List<ActivityItem> = emptyList(),
    val savedPayees: List<SavedPayee> = emptyList(),
    val lastStatusChecked: String = "",
    val feedback: String = "",
    val feedbackId: Long = 0,
    val hasLocalWallet: Boolean = false,
    val environment: String = "sandbox",
    val paymentMode: String = "disabled",
    val walletAuthorized: Boolean = false,
    val peerTransfersEnabled: Boolean = false,
    val scannerSession: Long = 0,
    val scanFailures: Int = 0,
    val rejectedQrPayload: String = "",
    val scannerErrorReason: String = "",
)

class MainActivity : FragmentActivity() {
    private lateinit var store: SecureStore
    private lateinit var api: ApiClient
    private lateinit var sender: ActivityResultSender
    private lateinit var walletAdapter: MobileWalletAdapter
    private val solanaRpc by lazy { SolanaRpc(BuildConfig.SOLANA_RPC_URL) }
    private lateinit var savedPayees: SavedPayeeStore
    private lateinit var backupExportLauncher: ActivityResultLauncher<String>
    private lateinit var backupImportLauncher: ActivityResultLauncher<Array<String>>
    private var state by mutableStateOf(AppState())
    private var awaitingKycReturn = false
    private var kycReturnStep: Step? = null
    private var pendingKycQrPayload: String? = null
    private val mnemonic by lazy {
        WalletMnemonic(assets.open("bip39_english.txt").bufferedReader().use { it.readLines() })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        backupExportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri == null) {
                state = state.copy(message = "Backup export canceled. Your wallet is still on this device.")
            } else {
                exportBackupTo(uri)
            }
        }
        backupImportLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) importBackupFrom(uri)
        }
        lifecycleScope.launch(Dispatchers.IO) {
            getSystemService(ShortcutManager::class.java)?.dynamicShortcuts = listOf(
                ShortcutInfo.Builder(this@MainActivity, "scan_to_pay")
                    .setShortLabel("Scan to pay")
                    .setLongLabel("Scan to pay with WAYQO")
                    .setIcon(Icon.createWithResource(this@MainActivity, R.drawable.wayqo_icon))
                    .setIntent(Intent(this@MainActivity, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        data = Uri.parse("wayqo://scan")
                    })
                    .build()
            )
        }
        store = SecureStore(this)
        api = ApiClient(BuildConfig.API_BASE_URL, store)
        sender = ActivityResultSender(this)
        walletAdapter = MobileWalletAdapter(
            ConnectionIdentity(
                identityUri = Uri.parse(BuildConfig.IDENTITY_URI),
                iconUri = Uri.parse("favicon.ico"),
                identityName = "WAYQO"
            )
        )
        walletAdapter.authToken = store.get("walletAuthToken")
        savedPayees = SavedPayeeStore(store)
        // Migrate installs from early builds which persisted the API session and wallet
        // seed but not the wallet address needed to restore UI state after process death.
        EmbeddedWallet.migrate(this)
        val storedWallet = store.get("walletAddress")
            ?.takeIf { it.isNotBlank() }
            ?: EmbeddedWallet.publicAddress(applicationContext).orEmpty()
        val storedResidency = store.get("residencyCountry").orEmpty()
        val savedScan = store.get("pendingScanPayload")
        val savedRoute = savedScan?.let {
            if (classifyScannedQr(it) == ScannedQrRoute.SOLANA) "Solana wallet transfer"
            else "Local bank/merchant QR payment"
        }.orEmpty()
        state = state.copy(
            hasLocalWallet = EmbeddedWallet.exists(applicationContext),
            localWallets = EmbeddedWallet.addresses(applicationContext),
            residencyCountry = storedResidency,
            externalWallets = store.get("externalWallets")?.split(',')?.filter { it.isNotBlank() }.orEmpty(),
            balanceHidden = store.get("balanceHidden") == "true",
            qrRoute = savedRoute,
            message = if (savedRoute.isNotBlank()) "$savedRoute saved. Choose a wallet to continue."
                else state.message,
        )
        if (storedWallet.isNotBlank() && store.get("apiToken") != null) {
            store.put("walletAddress", storedWallet)
            state = state.copy(
                wallet = storedWallet,
                residencyCountry = storedResidency,
                busy = true,
                message = "Restoring your secure session…",
            )
        }
        setContent {
            UnboundTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    App(
                        state = state,
                        onConnect = ::connectAndOnboard,
                        onCreateWallet = ::startNewWallet,
                        onUnlockWallet = ::unlockLocalWallet,
                        onRecoveryCheck = { state = state.copy(recoveryCheck = it, recoveryVerified = false) },
                        onRecoveryWritten = { state = state.copy(recoveryWritten = it, recoveryVerified = if (it) state.recoveryVerified else false) },
                        onVerifyRecovery = ::verifyRecovery,
                        onGenerateRecoveryPassword = {
                            val password = RecoveryFilePassword.generate()
                            state = state.copy(
                                backupPassphrase = password, backupPassphraseConfirm = password,
                                backupBlob = "",
                                message = "Strong password generated. Save it in your password manager before exporting the recovery file.",
                            )
                        },
                        onFinishPaperBackup = ::finishPaperBackup,
                        onShowRecoveryWords = ::openRecoveryWords,
                        onWallets = { state = state.copy(step = Step.WALLETS, localWallets = EmbeddedWallet.addresses(this), message = "Choose a wallet to use.") },
                        onSwitchWallet = ::switchLocalWallet,
                        onResidency = { value ->
                            store.put("residencyCountry", value)
                            state = state.copy(
                                residencyCountry = value,
                                message = "Country selected. Create a secure wallet or connect one you already use.",
                            )
                        },
                        onKyc = ::openKyc,
                        onRegister = ::registerProviderAccount,
                        onNotify = ::notifyUser,
                        onRename = ::renameWallet,
                        onToggleBalance = ::toggleBalancePrivacy,
                        onOpenBackup = ::openBackup,
                        onBackupLocalWallet = { openBackupFor(it, Step.WALLETS) },
                        onCreateBackup = ::createBackup,
                        onShareBackup = ::shareBackup,
                        onImportBackupFile = { backupImportLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                        onBackupPass = {
                            state = state.copy(
                                backupPassphrase = it,
                                backupBlob = if (state.step == Step.BACKUP) "" else state.backupBlob,
                            )
                        },
                        onBackupPassConfirm = {
                            state = state.copy(
                                backupPassphraseConfirm = it,
                                backupBlob = if (state.step == Step.BACKUP) "" else state.backupBlob,
                            )
                        },
                        onOpenRestore = ::openRestore,
                        onRestore = ::restoreFromBackup,
                        onRestoreMnemonic = ::restoreFromMnemonic,
                        onRestoreBlob = { state = state.copy(restoreBlobInput = it) },
                        onRestorePass = { state = state.copy(restorePassphrase = it) },
                        onRestorePhrase = { state = state.copy(restorePhraseInput = it) },
                        onBackToConnect = ::navigateBack,
                        onRefresh = ::refreshOnboarding,
                        onRefreshBalance = {
                            launchTask("Checking USDC balance…") {
                                refreshBalance()
                                state = state.copy(
                                    busy = false,
                                    message = if (state.balanceLoaded) "USDC balance updated." else "Balance is unavailable. Check your wallet or try again.",
                                )
                            }
                        },
                        onPayRail = ::beginMerchantPay,
                        onPayMarket = { value -> state = state.copy(paymentCountry = value, paymentNetwork = "", message = "${countryName(value)} selected. Continue when ready to scan.") },
                        onPaymentNetwork = { value -> state = state.copy(paymentNetwork = value, message = "$value selected.") },
                        onContinuePay = ::continueMerchantPay,
                        onQr = { handleQr(it) },
                        onSharedImageConsumed = { state = state.copy(sharedImage = "") },
                        onManual = ::openManualDraft,
                        onManualCountry = { state = state.copy(manualCountry = it) ; saveManualDraft() },
                        onManualBank = { state = state.copy(manualBank = it.take(80)) ; saveManualDraft() },
                        onManualAccount = { state = state.copy(manualAccount = it.take(100)) ; saveManualDraft() },
                        onAmount = { state = state.copy(amount = it.filter(Char::isDigit).take(PAYMENT_MAX_DIGITS)) },
                        // Append/delete read the live state holder (not a captured snapshot),
                        // so rapid keypad taps and press-and-hold delete stay correct.
                        onAmountAppend = { d -> state = state.copy(amount = (state.amount + d).filter(Char::isDigit).take(PAYMENT_MAX_DIGITS)) },
                        onAmountDelete = { state = state.copy(amount = state.amount.dropLast(1)) },
                        onPaymentPurpose = { value -> state = state.copy(paymentPurpose = value) },
                        onPaymentNote = { value -> state = state.copy(paymentNote = value.take(40)) },
                        onToggleBalanceHidden = { state = state.copy(balanceHidden = !state.balanceHidden) },
                        // One-slide flow: locking the quote and completing the payment
                        // (behind biometrics) happen in a single action.
                        onQuote = ::payFromInput,
                        onPay = ::pay,
                        onSendRail = ::beginPeerSend,
                        onPeerRecipient = { state = state.copy(peerRecipientInput = it.take(512)) },
                        onPeerAmount = { value ->
                            if (value.matches(Regex("""\d*(?:\.\d{0,6})?"""))) state = state.copy(peerAmount = value)
                        },
                        onPeerScan = { state = state.copy(step = Step.SEND_SCAN, scannerSession = state.scannerSession + 1,
                            scanFailures = 0, rejectedQrPayload = "", scannerErrorReason = "", message = "Scan a Solana wallet QR") },
                        onPeerQr = ::handlePeerQr,
                        onPeerReview = ::reviewPeerTransfer,
                        onPeerSend = ::sendPeerTransfer,
                        onFunding = {
                            val fromMerchantPayment = state.step == Step.QUOTE || state.step == Step.CONFIRM
                            val dynamicQr = fromMerchantPayment && state.parsed?.dynamic == true
                            state = state.copy(
                                step = Step.FUNDING,
                                fundingReturnStep = if (dynamicQr) Step.SCAN else if (fromMerchantPayment) Step.CONFIRM else Step.HOME,
                                quote = if (fromMerchantPayment) null else state.quote,
                                qrPayload = if (dynamicQr) "" else state.qrPayload,
                                parsed = if (dynamicQr) null else state.parsed,
                                message = when {
                                    dynamicQr -> "Add USDC, then scan a fresh dynamic QR."
                                    fromMerchantPayment -> "Add USDC to your wallet, then request a fresh quote."
                                    else -> "Choose how to fund your connected wallet."
                                },
                            )
                        },
                        onReceive = {
                            state = state.copy(
                                step = Step.RECEIVE,
                                receiveReturnStep = if (state.step == Step.FUNDING) Step.FUNDING else Step.HOME,
                                message = "",
                            )
                        },
                        onActivity = ::loadActivity,
                        onRepeat = ::repeatActivity,
                        onSaved = ::openSavedPayees,
                        onProfile = { state = state.copy(step = Step.PROFILE, message = "Account, verification, and security") },
                        onSignOut = ::signOut,
                        onDeleteAccount = ::deleteAccount,
                        onExportData = ::exportData,
                        onUseSaved = ::useSavedPayee,
                        onDeleteSaved = ::deleteSavedPayee,
                        onRenamePayee = ::renameSavedPayee,
                        onSaveMerchant = ::saveCurrentMerchant,
                        onSaveWallet = ::saveCurrentWallet,
                        onHome = ::showHome,
                        onBack = ::navigateBack
                    )
                }
            }
        }
        if (state.wallet.isNotBlank()) restoreSession(intent) else handleLaunchIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.data?.scheme in setOf("apacqrpay", Uri.parse(BuildConfig.KYC_CALLBACK_URL).scheme)) {
            awaitingKycReturn = false
            refreshOnboarding("Checking the verification result…")
        } else {
            handleLaunchIntent(intent)
        }
    }

    private fun handleLaunchIntent(intent: Intent?) {
        when {
            intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true -> {
                val stream = if (android.os.Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                }
                val uri = stream ?: intent.clipData?.getItemAt(0)?.uri ?: intent.data
                if (uri != null) state = state.copy(
                    step = Step.SCAN, sharedImage = uri.toString(), scannerSession = state.scannerSession + 1,
                    scanFailures = 0, rejectedQrPayload = "", scannerErrorReason = "",
                    paymentNote = "",
                    scanReturnStep = if (state.wallet.isBlank()) Step.CONNECT else Step.HOME,
                    message = "Reading shared QR image…",
                )
            }
            intent?.data?.toString() == "wayqo://scan" -> beginMerchantPay()
        }
    }

    private fun startNewWallet() = launchTask("Creating wallet…") {
        val residency = state.residencyCountry.takeIf { it.length == 2 }
            ?: error("Select your country of residence before creating a wallet")
        val phrase = mnemonic.generate()
        val seed = mnemonic.solanaSeed(phrase)
        val connected = try {
            activateLocalSeed(
                seed, residency, "Recovery not exported · back up this wallet",
                "Secure your new wallet on this device", phrase, automatic = true,
            )
        } finally { seed.fill(0) }
        // Payments are gated on device auth (biometrics or a PIN/pattern/password). If
        // none is enrolled, prompt the user to add one now so their first payment works.
        val secureReady = BiometricManager.from(this).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
        state = state.copy(
            hasLocalWallet = true, localWallets = EmbeddedWallet.addresses(this), busy = false,
            step = if (connected) Step.HOME else Step.WALLETS,
            message = when {
                connected && !secureReady -> "Wallet created. Set up fingerprint, face unlock, or a device PIN in your phone settings — it's required to authorize payments."
                connected -> "Wallet created. You can scan payments and check your USD balance. Back up the wallet when you're ready."
                else -> "Wallet created on this device. Select it to retry the payment service connection."
            },
        )
        notifyUser(if (connected) "Wallet created" else "Wallet created locally")
    }

    private fun verifyRecovery() {
        val phrase = mnemonic.wordsFromInput(state.recoveryPhrase)
        val positions = state.recoveryPositions
        if (!state.recoveryWritten || phrase.size != 24 || positions.size != 3) {
            state = state.copy(message = "Write down the 24 words before checking them.")
            return
        }
        val expected = positions.map { phrase[it - 1] }
        val entered = mnemonic.wordsFromInput(state.recoveryCheck)
        state = state.copy(
            recoveryVerified = entered == expected,
            message = if (entered == expected) "Recovery words checked. You can create the wallet."
                else "Words ${positions.joinToString(", ")} do not match. Check your written copy.",
        )
    }

    private fun unlockLocalWallet() = launchTask("Unlocking wallet…") {
        val address = EmbeddedWallet.activeAddress(this) ?: error("No local wallet on this device")
        switchLocalWalletInternal(address)
    }

    private fun switchLocalWallet(address: String) = launchTask("Switching wallet…") {
        switchLocalWalletInternal(address)
    }

    private suspend fun switchLocalWalletInternal(address: String, alreadyAuthenticated: Boolean = false) {
        val residency = state.residencyCountry.takeIf { it.length == 2 }
            ?: error("Select your country of residence")
        val old = EmbeddedWallet.activeAddress(this)
        val oldToken = store.get("apiToken")
        val oldWallet = store.get("walletAddress")
        val oldResidency = store.get("residencyCountry")
        val oldState = state
        if (!alreadyAuthenticated) authenticateForSigning("Unlock the selected wallet")
        try {
            EmbeddedWallet.select(this, address)
            val publicKey = EmbeddedWallet.publicKeyBytes(this)
            require(Base58.encode(publicKey) == address) { "Stored wallet address does not match its key" }
            val encodedKey = Base64.encodeToString(publicKey, Base64.NO_WRAP)
            val challenge = api.challenge(address, encodedKey)
            val signature = EmbeddedWallet.sign(this, challenge.message.toByteArray())
            api.verify(challenge, address, encodedKey, Base64.encodeToString(signature, Base64.NO_WRAP))
            store.put("walletAddress", address)
            store.put("residencyCountry", residency)
            state = state.copy(wallet = address, balanceUsd = "$0.00", balanceLoaded = false, localWallets = EmbeddedWallet.addresses(this))
            loadCapabilities()
            applyOnboarding(api.onboard(residency))
            refreshPaymentMarkets()
            resumePendingScan()
        } catch (error: Exception) {
            if (old != null && old != address) EmbeddedWallet.select(this, old)
            store.put("apiToken", oldToken)
            store.put("walletAddress", oldWallet)
            store.put("residencyCountry", oldResidency)
            state = oldState.copy(localWallets = EmbeddedWallet.addresses(this))
            throw error
        }
    }

    private fun navigateBack() {
        state = when (state.step) {
            Step.SCAN -> state.copy(
                step = state.scanReturnStep, sharedImage = "",
                message = if (state.scanReturnStep == Step.CONNECT) "Scan canceled. You can scan again or set up a wallet."
                    else "Choose how you want to pay or receive.",
            )
            Step.SEND_SCAN, Step.SEND_CONFIRM -> state.copy(step = Step.SEND)
            // Back from the payment screen returns Home (where the nav lives) instead of
            // reopening the live camera, which would re-read the same QR and trap the user.
            Step.CONFIRM -> state.copy(step = if (state.wallet.isBlank()) Step.CONNECT else Step.HOME)
            Step.QUOTE -> state.copy(step = Step.CONFIRM)
            Step.KYC -> state.copy(step = if (state.parsed != null) Step.CONFIRM else if (state.wallet.isBlank()) Step.CONNECT else Step.HOME)
            Step.PHRASE -> state.copy(step = Step.BACKUP, recoveryPhrase = "", recoveryCheck = "", recoveryPositions = emptyList(), recoveryWritten = false, recoveryVerified = false)
            Step.RESTORE -> state.copy(step = if (state.wallet.isBlank()) Step.CONNECT else Step.WALLETS, restorePhraseInput = "", restorePassphrase = "", restoreBlobInput = "")
            Step.MANUAL -> state.copy(step = Step.SCAN, sharedImage = "", scannerErrorReason = "", scannerSession = state.scannerSession + 1)
            Step.MARKET -> state.copy(step = Step.SCAN, sharedImage = "", scannerErrorReason = "", scannerSession = state.scannerSession + 1)
            Step.BACKUP -> state.copy(
                step = state.backupReturnStep, backupWalletAddress = "", backupHasMnemonic = false,
                backupStatus = store.get("backupStatus_${state.wallet}") ?: "Not backed up",
                backupPassphrase = "", backupPassphraseConfirm = "", backupBlob = "",
            )
            Step.WALLETS -> state.copy(step = if (state.wallet.isBlank()) Step.CONNECT else Step.HOME)
            Step.RECEIVE -> state.copy(step = state.receiveReturnStep)
            Step.FUNDING -> state.copy(
                step = state.fundingReturnStep,
                scannerSession = if (state.fundingReturnStep == Step.SCAN) state.scannerSession + 1 else state.scannerSession,
            )
            Step.SEND, Step.ACTIVITY, Step.SAVED, Step.PROFILE -> state.copy(step = Step.HOME)
            Step.PAYING -> state.copy(step = Step.QUOTE)
            Step.SENDING -> state.copy(step = Step.SEND_CONFIRM)
            else -> state.copy(step = if (state.wallet.isBlank()) Step.CONNECT else Step.HOME)
        }
    }

    private fun openManualDraft() {
        state = state.copy(
            step = Step.MANUAL,
            manualCountry = store.get("manualCountry").orEmpty(),
            manualBank = store.get("manualBank").orEmpty(),
            manualAccount = store.get("manualAccount").orEmpty(),
            message = "Unverified draft. Enter country, bank identifier, and account details for your own record.",
        )
    }

    private fun saveManualDraft() {
        store.put("manualCountry", state.manualCountry)
        store.put("manualBank", state.manualBank)
        store.put("manualAccount", state.manualAccount)
    }

    private fun resumePendingScan() {
        val payload = store.get("pendingScanPayload")?.takeIf { it.isNotBlank() } ?: return
        store.put("pendingScanPayload", null)
        lifecycleScope.launch {
            delay(100)
            handleQr(payload)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::api.isInitialized && awaitingKycReturn && state.wallet.isNotBlank()) {
            awaitingKycReturn = false
            refreshOnboarding("Checking the verification result…")
        }
    }

    private fun allowedMints(): Set<String> = BuildConfig.SOLANA_ALLOWED_MINTS
        .split(',').map(String::trim).filter(String::isNotBlank).toSet()

    private fun connectAndOnboard() = launchTask("Connecting wallet…") {
        val residency = state.residencyCountry.takeIf { it.length == 2 }
            ?: error("Select your country of residence")
        when (val connected = walletAdapter.connect(sender)) {
            is TransactionResult.Success -> {
                val publicKey = connected.authResult.accounts.firstOrNull()?.publicKey
                    ?: error("Wallet returned no Solana account")
                val wallet = Base58.encode(publicKey)
                store.put("walletAuthToken", walletAdapter.authToken)
                val encodedKey = Base64.encodeToString(publicKey, Base64.NO_WRAP)
                val challenge = api.challenge(wallet, encodedKey)
                val signed = walletAdapter.transact(sender) { authResult ->
                    signMessagesDetached(arrayOf(challenge.message.toByteArray()), arrayOf(authResult.accounts.first().publicKey))
                }
                val signature = (signed as? TransactionResult.Success)
                    ?.payload?.messages?.firstOrNull()?.signatures?.firstOrNull()
                    ?: error("Wallet did not sign the login challenge")
                api.verify(challenge, wallet, encodedKey, Base64.encodeToString(signature, Base64.NO_WRAP))
                store.put("walletAddress", wallet)
                store.put("residencyCountry", residency)
                state = state.copy(wallet = wallet, balanceLoaded = false)
                loadCapabilities()
                applyOnboarding(api.onboard(residency))
                refreshPaymentMarkets()
                val known = (store.get("externalWallets")?.split(',').orEmpty() + wallet).distinct()
                store.put("externalWallets", known.joinToString(","))
                state = state.copy(externalWallets = known)
                notifyUser("Wallet connected")
                resumePendingScan()
            }
            is TransactionResult.NoWalletFound -> error("Install an MWA-compatible Solana wallet, then try again")
            is TransactionResult.Failure -> throw connected.e
        }
    }

    private fun refreshOnboarding(label: String = "Checking KYC and market access…") = launchTask(label) {
        loadCapabilities()
        val previousStep = state.step
        val previousStatus = state.kycStatus
        val onboarding = api.onboard()
        applyOnboarding(onboarding, if (previousStep == Step.KYC) Step.KYC else previousStep)
        refreshPaymentMarkets(
            if (onboarding.kycStatus.equals(previousStatus, ignoreCase = true)) {
                "Status checked. No change from the provider."
            } else {
                "Verification status updated to ${onboarding.kycStatus.ifBlank { "Not submitted" }}."
            }
        )
        val resume = kycReturnStep
        val resumedPendingPayment = resumePendingKycPaymentIfPresent()
        val corridorApproved = state.paymentMarkets.any { it.country == state.paymentCountry && it.approved }
        if (!resumedPendingPayment && resume == Step.CONFIRM && state.parsed != null && corridorApproved) {
            kycReturnStep = null
            state = state.copy(
                step = Step.CONFIRM,
                message = "Identity verified. Your recipient and payment details are still ready for review.",
            )
        }
        val checkedAt = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a"))
        state = state.copy(lastStatusChecked = checkedAt)
        notifyUser("KYC status checked at $checkedAt")
    }

    private fun restoreSession(launchIntent: Intent?) {
        lifecycleScope.launch {
            runCatching {
                loadCapabilities()
                val onboarding = try {
                    api.onboard()
                } catch (error: ApiException) {
                    if (error.code !in setOf("not_onboarded", "residency_required")) throw error
                    null
                }
                if (onboarding == null) {
                    showUnregisteredHome()
                } else {
                    applyOnboarding(onboarding, nextStep = if (state.step == Step.SCAN) Step.SCAN else Step.HOME)
                    refreshPaymentMarkets()
                    resumePendingKycPaymentIfPresent()
                }
                if (state.step != Step.SCAN) resumePendingScan()
            }.onFailure { error ->
                state = state.copy(
                    step = if (state.step == Step.SCAN) Step.SCAN else Step.CONNECT,
                    wallet = "",
                    busy = false,
                    message = "Your saved session expired. Reconnect the wallet to continue. ${error.message.orEmpty()}",
                )
                resumePendingScan()
            }
            handleLaunchIntent(launchIntent)
        }
    }

    private fun savePendingKycPayment(
        payload: String,
        country: String,
        network: String,
        amount: String = "",
        purpose: String = "",
        sourceConfirmed: Boolean = false,
    ) {
        pendingKycQrPayload = payload
        store.put("pendingKycQrPayload", payload)
        store.put("pendingKycQrCountry", country)
        store.put("pendingKycQrNetwork", network)
        store.put("pendingKycQrAmount", amount)
        store.put("pendingKycQrPurpose", purpose)
        store.put("pendingKycQrSource", sourceConfirmed.toString())
    }

    private fun clearPendingKycPayment() {
        pendingKycQrPayload = null
        listOf(
            "pendingKycQrPayload", "pendingKycQrCountry", "pendingKycQrNetwork",
            "pendingKycQrAmount", "pendingKycQrPurpose", "pendingKycQrSource",
        ).forEach { store.put(it, null) }
    }

    // A browser-hosted KYC flow may background or even kill the app. Keep only the
    // scanned payment inputs in encrypted device storage, then re-parse with Gaian after
    // approval so beneficiary verification remains authoritative and no rescan is needed.
    private suspend fun resumePendingKycPaymentIfPresent(): Boolean {
        val payload = pendingKycQrPayload ?: store.get("pendingKycQrPayload") ?: return false
        val country = store.get("pendingKycQrCountry").orEmpty().ifBlank { state.paymentCountry }
        val network = store.get("pendingKycQrNetwork").orEmpty()
        pendingKycQrPayload = payload
        val approved = state.paymentMarkets.any { it.country == country && it.approved }
        if (state.userId.isBlank() || (!approved && !canParseQrBeforeKyc(country))) {
            state = state.copy(
                step = Step.KYC,
                busy = false,
                paymentCountry = country,
                paymentNetwork = network,
                qrPayload = payload,
                message = if (state.userId.isBlank()) "QR saved. Register a payment profile to check the recipient; no rescan is needed."
                    else "Payment saved. Complete identity verification to verify the beneficiary; no rescan is needed.",
            )
            return true
        }
        val parsed = try {
            api.parseQr(payload, country, network)
        } catch (error: ApiException) {
            if (error.code != "recipient_unverified") throw error
            clearPendingKycPayment()
            kycReturnStep = null
            rejectScannedQr(payload, "Identity verification is complete, but this QR's recipient name could not be verified. Scan another QR.")
            return true
        }
        val preservedAmount = store.get("pendingKycQrAmount").orEmpty()
        val preservedPurpose = store.get("pendingKycQrPurpose").orEmpty()
        val preservedSource = store.get("pendingKycQrSource") == "true"
        clearPendingKycPayment()
        kycReturnStep = null
        state = state.copy(
            step = Step.CONFIRM,
            busy = false,
            paymentCountry = country,
            paymentNetwork = network,
            qrPayload = payload,
            parsed = parsed,
            amount = preservedAmount.ifBlank { parsed.amount.substringBefore('.').toLongOrNull()?.takeIf { it > 0 }?.toString().orEmpty() },
            paymentPurpose = preservedPurpose,
            sourceOfFundsConfirmed = preservedSource,
            message = if (approved) "Identity verified and recipient confirmed. Review the preserved payment details before requesting a quote."
                else "Recipient verified. Review the payment details; identity approval is needed before a quote.",
        )
        return true
    }

    private fun applyOnboarding(onboarding: Onboarding, nextStep: Step = Step.HOME) {
        val embedded = EmbeddedWallet.exists(applicationContext) &&
            EmbeddedWallet.publicAddress(applicationContext) == state.wallet
        state = state.copy(
            userId = onboarding.userId,
            kycStatus = onboarding.kycStatus,
            isEmbeddedWallet = embedded,
            hardwareProtected = !embedded || EmbeddedWallet.isHardwareProtected(applicationContext),
            walletName = store.get("walletName_${state.wallet}")?.takeIf { it.isNotBlank() } ?: store.get("walletName")?.takeIf { it.isNotBlank() } ?: "Main wallet",
            backupStatus = store.get("backupStatus_${state.wallet}") ?: "Legacy wallet · create a verified encrypted backup",
            localWallets = EmbeddedWallet.addresses(applicationContext),
            externalWallets = store.get("externalWallets")?.split(',')?.filter { it.isNotBlank() }.orEmpty(),
            step = nextStep,
            busy = false,
            message = "Choose how you want to pay or receive."
        )
    }

    private fun renameWallet(name: String) {
        val clean = name.trim().ifBlank { "Main wallet" }
        store.put("walletName_${state.wallet}", clean)
        state = state.copy(walletName = clean)
        notifyUser("Wallet name updated")
    }

    private fun toggleBalancePrivacy() {
        val hidden = !state.balanceHidden
        store.put("balanceHidden", hidden.toString())
        state = state.copy(balanceHidden = hidden)
        notifyUser(if (hidden) "Balance hidden" else "Balance visible")
    }

    private suspend fun loadCapabilities() {
        val capabilities = api.capabilities()
        state = state.copy(
            environment = capabilities.environment.lowercase(),
            paymentMode = capabilities.paymentMode.lowercase(),
            walletAuthorized = capabilities.walletAuthorized,
            peerTransfersEnabled = capabilities.peerTransfers,
        )
    }

    private fun registerProviderAccount() = launchTask("Registering payment profile…") {
        val residency = state.residencyCountry.takeIf { it.length == 2 }
            ?: error("Select your country of residence before registering")
        require(state.wallet.isNotBlank()) { "Connect your wallet before registering" }
        applyOnboarding(api.onboard(residency))
        refreshPaymentMarkets("Payment profile registered. Check identity status for each supported market.")
        resumePendingKycPaymentIfPresent()
        notifyUser("Payment profile registered")
    }

    private suspend fun showUnregisteredHome() {
        val embedded = EmbeddedWallet.exists(applicationContext) &&
            EmbeddedWallet.publicAddress(applicationContext) == state.wallet
        state = state.copy(
            step = if (state.step == Step.SCAN) Step.SCAN else Step.HOME,
            busy = false,
            hasLocalWallet = EmbeddedWallet.exists(applicationContext),
            isEmbeddedWallet = embedded,
            hardwareProtected = !embedded || EmbeddedWallet.isHardwareProtected(applicationContext),
            walletName = store.get("walletName_${state.wallet}")?.takeIf { it.isNotBlank() } ?: store.get("walletName")?.takeIf { it.isNotBlank() } ?: "Main wallet",
            backupStatus = store.get("backupStatus_${state.wallet}") ?: "Legacy wallet · create a verified encrypted backup",
            message = "Wallet ready. Scan a QR or register for provider verification; sending stays gated.",
        )
        refreshPaymentMarkets("Wallet ready. Register for provider verification to request quotes; scanning is available now.")
    }

    private suspend fun refreshPaymentMarkets(messageOverride: String? = null) {
        val markets = api.markets()
        val recent = runCatching { api.activity(limit = 3).items }.getOrElse { state.activities }
        val selected = state.paymentCountry.takeIf { country -> markets.any { it.country == country } }
            ?: markets.firstOrNull { it.approved }?.country
            ?: markets.firstOrNull()?.country.orEmpty()
        state = state.copy(
            paymentMarkets = markets,
            paymentCountry = selected,
            merchantPayReady = markets.any { it.approved },
            activities = recent,
            savedPayees = savedPayees.list(),
            busy = false,
            message = messageOverride ?: if (markets.any { it.approved }) "Choose how you want to pay or receive." else "Scan and review are available. Quotes require corridor approval; sending requires payment access."
        )
        refreshBalance()
    }

    private fun notifyUser(message: String) {
        state = state.copy(feedback = message, feedbackId = state.feedbackId + 1)
    }

    private fun signOut() {
        walletAdapter.authToken = null
        clearPendingKycPayment()
        store.put("pendingScanPayload", null)
        kycReturnStep = null
        listOf("apiToken", "walletAuthToken", "walletAddress", "residencyCountry").forEach {
            store.put(it, null)
        }
        awaitingKycReturn = false
        state = AppState(
            hasLocalWallet = EmbeddedWallet.exists(applicationContext),
            balanceHidden = store.get("balanceHidden") == "true",
            message = if (EmbeddedWallet.exists(applicationContext)) {
                "Signed out. Your encrypted wallet remains safely on this device."
            } else {
                "Signed out. Connect a wallet to begin."
            },
            feedback = "Signed out",
            feedbackId = state.feedbackId + 1,
        )
    }

    // Deletes the account's server-side data on request, then clears the local session.
    // The on-device self-custody wallet and on-chain funds are the user's to remove.
    private fun deleteAccount() = launchTask("Deleting your account…") {
        runCatching { api.deleteAccount() }
        walletAdapter.authToken = null
        clearPendingKycPayment()
        store.put("pendingScanPayload", null)
        kycReturnStep = null
        listOf("apiToken", "walletAuthToken", "walletAddress", "residencyCountry").forEach {
            store.put(it, null)
        }
        awaitingKycReturn = false
        state = AppState(
            hasLocalWallet = EmbeddedWallet.exists(applicationContext),
            balanceHidden = store.get("balanceHidden") == "true",
            message = "Your account data was deleted. Your self-custody wallet and funds remain on this device — remove the wallet or uninstall the app to erase them.",
            feedback = "Account data deleted",
            feedbackId = state.feedbackId + 1,
        )
    }

    // Produces a signed PDF copy of all data we hold for the wallet (a data-subject
    // access request) and opens the system share sheet so the user can keep it.
    private fun exportData() = launchTask("Preparing your data…") {
        val envelope = api.exportAccount()
        val uri = withContext(Dispatchers.IO) { SarPdf.write(applicationContext, envelope) }
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Wayqo — my data export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(
            Intent.createChooser(share, "Save or share your data export")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        )
        state = state.copy(busy = false, feedback = "Signed data export ready", feedbackId = state.feedbackId + 1)
    }

    // Reads the wallet's on-chain USDC and shows it as a USD figure (1 USDC ≈ $1).
    private suspend fun refreshBalance() {
        val wallet = state.wallet.takeIf { it.isNotBlank() } ?: return
        // A quote names USDC but not a mint. Only compare its debit with a balance
        // when this build has exactly one permitted mint.
        val mint = allowedMints().singleOrNull()
        if (mint == null) {
            state = state.copy(balanceLoaded = false)
            return
        }
        val usd = runCatching { solanaRpc.tokenBalance(wallet, mint) }.getOrNull()
        if (state.wallet != wallet) return
        state = if (usd == null) state.copy(balanceLoaded = false)
            else state.copy(balanceUsd = "$" + usd.toPlainString(), balanceLoaded = true)
    }

    private fun showHome() {
        clearPendingKycPayment()
        kycReturnStep = null
        state = state.copy(
            step = Step.HOME,
            busy = false,
            qrPayload = "",
            parsed = null,
            amount = "",
            quote = null,
            order = null,
            sourceOfFundsConfirmed = false,
            paymentPurpose = "",
            paymentNote = "",
            peerRecipientInput = "",
            peerRecipient = "",
            peerAmount = "",
            peerMint = "",
            peerSignature = "",
            recoveryPhrase = "",
            recoveryCheck = "",
            restorePhraseInput = "",
            restorePassphrase = "",
            restoreBlobInput = "",
            backupPassphrase = "",
            backupPassphraseConfirm = "",
            backupBlob = "",
            savedPayees = savedPayees.list(),
            message = "Choose how you want to pay or receive."
        )
        lifecycleScope.launch {
            runCatching { api.activity(limit = 3).items }.onSuccess { recent ->
                state = state.copy(activities = recent)
            }
        }
    }

    private fun beginMerchantPay() {
        val origin = if (state.step == Step.SCAN) state.scanReturnStep else state.step
        store.put("pendingScanPayload", null)
        clearPendingKycPayment()
        kycReturnStep = null
        state = state.copy(
            step = Step.SCAN,
            scanReturnStep = origin,
            scannerSession = state.scannerSession + 1,
            scanFailures = 0,
            rejectedQrPayload = "",
            scannerErrorReason = "",
            paymentNetwork = "",
            qrPayload = "",
            parsed = null,
            amount = "",
            quote = null,
            order = null,
            sourceOfFundsConfirmed = false,
            paymentPurpose = "",
            paymentNote = "",
            message = "Scan a Solana wallet or local bank/merchant QR. WAYQO will identify the route.",
        )
    }

    private fun continueMerchantPay() {
        val market = state.paymentMarkets.firstOrNull { it.country == state.paymentCountry }
        if (market != null && state.qrPayload.isNotBlank() &&
            (market.country != "PE" || state.paymentNetwork in setOf("YAPE", "PLIN"))) {
            handleQr(state.qrPayload, explicitMarket = market.country)
            return
        }
        state = when {
            market == null -> state.copy(message = "Select an available payout market.")
            market.country == "PE" && state.paymentNetwork !in setOf("YAPE", "PLIN") -> state.copy(message = "Choose Yape or Plin for Peru.")
            else -> state.copy(
                step = Step.SCAN,
                scannerSession = state.scannerSession + 1,
                qrPayload = "",
                parsed = null,
                amount = "",
                quote = null,
                order = null,
                sourceOfFundsConfirmed = false,
                paymentNote = "",
                paymentPurpose = "",
                message = if (market.approved) {
                    "Scan a supported merchant or business QR in ${countryName(market.country)}."
                } else {
                    "Scan first. The provider will verify the recipient and request identity verification only before the quote."
                },
            )
        }
    }

    private fun openKyc() = launchTask("Creating secure KYC link…") {
        if (state.userId.isBlank()) error("Register your payment profile before starting identity verification")
        if (state.step == Step.CONFIRM && state.parsed != null) {
            kycReturnStep = Step.CONFIRM
            savePendingKycPayment(
                payload = state.qrPayload,
                country = state.paymentCountry,
                network = state.paymentNetwork,
                amount = state.amount,
                purpose = state.paymentPurpose,
                sourceConfirmed = state.sourceOfFundsConfirmed,
            )
        }
        val url = api.kycLink()
        val target = Uri.parse(url)
        require(target.scheme == "https" && !target.host.isNullOrBlank()) {
            "The identity provider returned an invalid verification link"
        }
        state = state.copy(
            busy = false,
            step = Step.KYC,
            kycUrl = url,
            message = "Verification opened in your browser. Return here when finished; status will refresh automatically.",
        )
        awaitingKycReturn = true
        try {
            startActivity(Intent(Intent.ACTION_VIEW, target))
        } catch (error: Exception) {
            awaitingKycReturn = false
            throw error
        }
    }

    private fun openBackup() = openBackupFor(state.wallet, Step.HOME)

    private fun openBackupFor(address: String, returnStep: Step) {
        if (address !in EmbeddedWallet.addresses(this)) {
            state = state.copy(message = "Select a local wallet to back up")
            return
        }
        state = state.copy(
            step = Step.BACKUP, backupWalletAddress = address, backupReturnStep = returnStep,
            backupHasMnemonic = EmbeddedWallet.hasMnemonic(this, address),
            backupStatus = store.get("backupStatus_$address") ?: "Legacy wallet · create a verified encrypted backup",
            backupPassphrase = "",
            backupPassphraseConfirm = "",
            backupBlob = "",
            message = "Your wallet is ready to use. Export recovery only when you want a copy for another device.",
        )
    }

    private fun openRecoveryWords() = launchTask("Unlocking recovery words…") {
        val address = state.backupWalletAddress
        require(address.isNotBlank() && EmbeddedWallet.hasMnemonic(this, address)) {
            "This wallet has no BIP39 recovery words; export its encrypted Solana key instead"
        }
        authenticateForSigning("Show recovery words for this wallet")
        val previous = EmbeddedWallet.activeAddress(this)
        val phrase = try {
            EmbeddedWallet.select(this, address)
            EmbeddedWallet.exportMnemonic(this, address)
        } finally { if (previous != null && previous != address) EmbeddedWallet.select(this, previous) }
        require(mnemonic.isValid(phrase) && mnemonic.address(phrase) == address) {
            "Stored recovery words do not match this wallet"
        }
        state = state.copy(
            step = Step.PHRASE, busy = false, recoveryPhrase = phrase,
            recoveryPositions = chooseRecoveryWordPositions(), recoveryCheck = "",
            recoveryWritten = false, recoveryVerified = false,
            message = "Write these words down only if you want a paper backup. Anyone with them controls this wallet.",
        )
    }

    private fun finishPaperBackup() {
        if (!state.recoveryWritten || !state.recoveryVerified || state.backupWalletAddress.isBlank()) return
        val status = "BIP39 recovery words written and checked"
        store.put("backupStatus_${state.backupWalletAddress}", status)
        state = state.copy(
            step = Step.BACKUP, recoveryPhrase = "", recoveryCheck = "", recoveryPositions = emptyList(),
            recoveryWritten = false, recoveryVerified = false, backupStatus = status,
            message = "Recovery words checked. Keep the written copy private.",
        )
    }

    private fun createBackup() = launchTask("Creating encrypted backup…") {
        val passphrase = state.backupPassphrase
        val address = state.backupWalletAddress
        require(address in EmbeddedWallet.addresses(this)) {
            "Select the local wallet you want to back up"
        }
        require(passphrase.length >= 12 && passphrase.any(Char::isLetter)) { "Use a passphrase of at least 12 characters with letters. A short PIN is unsafe for a portable file." }
        require(passphrase == state.backupPassphraseConfirm) { "Passphrases don't match" }
        authenticateForSigning("Unlock your wallet to back it up")
        val previous = EmbeddedWallet.activeAddress(this)
        val blob = try {
          EmbeddedWallet.select(this, address)
          if (EmbeddedWallet.hasMnemonic(this, address)) {
            val phrase = EmbeddedWallet.exportMnemonic(this, address)
            require(mnemonic.isValid(phrase) && mnemonic.address(phrase) == address) {
                "Stored recovery words do not match this wallet"
            }
            WalletBackup.createPhrase(phrase, passphrase.toCharArray()).also { backup ->
                val restored = WalletBackup.restoreMaterial(backup, passphrase.toCharArray())
                require(restored is WalletRecoveryMaterial.Phrase && restored.words == phrase) {
                    "Backup verification failed — do not rely on this backup"
                }
            }
          } else {
            val seed = EmbeddedWallet.exportSeed(applicationContext)
            try {
                WalletBackup.create(seed, passphrase.toCharArray()).also { backup ->
                    val restored = WalletBackup.restore(backup, passphrase.toCharArray())
                    try { require(restored.contentEquals(seed)) { "Backup verification failed — do not rely on this backup" } }
                    finally { restored.fill(0) }
                }
            } finally { seed.fill(0) }
          }
        } finally { if (previous != null && previous != address) EmbeddedWallet.select(this, previous) }
        state = state.copy(
            busy = false,
            backupBlob = blob,
            backupStatus = "Encrypted recovery ready · export file",
            message = "Backup encrypted and test-restored. Export it to a file you control to finish the backup.",
        )
    }

    private fun shareBackup() {
        if (state.backupBlob.isNotBlank()) {
            backupExportLauncher.launch("WAYQO-wallet-${state.backupWalletAddress.take(8)}.json")
        }
    }

    private fun exportBackupTo(uri: Uri) = launchTask("Saving encrypted backup file…") {
        val blob = state.backupBlob.takeIf { it.isNotBlank() } ?: error("Create an encrypted backup first")
        val wallet = state.backupWalletAddress
        val bytes = blob.toByteArray(Charsets.UTF_8)
        withContext(Dispatchers.IO) {
            contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes); it.flush() }
                ?: error("Could not write the selected file")
            require(readBackupFile(uri).contentEquals(bytes)) { "Backup file verification failed. Choose another location." }
        }
        require(state.backupWalletAddress == wallet && wallet.isNotBlank()) { "Selected wallet changed during export" }
        val status = if (EmbeddedWallet.hasMnemonic(this, wallet)) "Encrypted BIP39 file exported and verified"
            else "Encrypted legacy backup exported and verified"
        store.put("backupStatus_$wallet", status)
        state = state.copy(busy = false, backupStatus = status, message = "Backup file saved and checked. Keep its passphrase separately; either is required to restore.")
    }

    private fun importBackupFrom(uri: Uri) = launchTask("Reading encrypted backup file…") {
        val blob = withContext(Dispatchers.IO) { readBackupFile(uri).toString(Charsets.UTF_8) }
        require(blob.isNotBlank()) { "Backup file is empty" }
        state = state.copy(step = Step.RESTORE, restoreBlobInput = blob, busy = false, message = "Encrypted backup loaded. Enter its passphrase to restore.")
    }

    private fun readBackupFile(uri: Uri): ByteArray = contentResolver.openInputStream(uri)?.use { input ->
        readLimited(input, 16_384)
    } ?: error("Could not read the selected backup file")

    private fun readLimited(input: InputStream, maximum: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(4_096)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(output.size() + count <= maximum) { "Backup file is too large" }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun openRestore() {
        state = state.copy(
            step = Step.RESTORE,
            restoreBlobInput = "",
            restorePassphrase = "",
            restorePhraseInput = "",
            message = "Paste your encrypted backup and enter its passphrase.",
        )
    }

    private fun restoreFromBackup() = launchTask("Restoring wallet…") {
        val residency = state.residencyCountry.takeIf { it.length == 2 }
            ?: error("Select your country of residence before restoring")
        val material = WalletBackup.restoreMaterial(state.restoreBlobInput.trim(), state.restorePassphrase.toCharArray())
        val seed = when (material) {
            is WalletRecoveryMaterial.Seed -> material.bytes
            is WalletRecoveryMaterial.Phrase -> {
                require(mnemonic.isValid(material.words)) { "Encrypted file has an invalid recovery phrase" }
                mnemonic.solanaSeed(material.words)
            }
        }
        val backupStatus = if (material is WalletRecoveryMaterial.Phrase) "Encrypted BIP39 file restored" else "Encrypted legacy backup restored"
        try { finishRestoration(seed, residency, backupStatus, (material as? WalletRecoveryMaterial.Phrase)?.words) } finally { seed.fill(0) }
    }

    private fun restoreFromMnemonic() = launchTask("Restoring recovery phrase…") {
        val residency = state.residencyCountry.takeIf { it.length == 2 }
            ?: error("Select your country of residence before restoring")
        val phrase = mnemonic.wordsFromInput(state.restorePhraseInput).joinToString(" ")
        require(mnemonic.isValid(phrase)) { "Invalid 24-word recovery phrase or checksum" }
        val seed = mnemonic.solanaSeed(phrase)
        try { finishRestoration(seed, residency, "BIP39 recovery phrase restored", phrase) } finally { seed.fill(0) }
    }

    private suspend fun finishRestoration(seed: ByteArray, residency: String, backupStatus: String, phrase: String? = null) {
        val connected = activateLocalSeed(seed, residency, backupStatus, phrase = phrase)
        state = state.copy(
            step = if (connected) state.step else Step.WALLETS,
            busy = false,
            message = if (connected) "Wallet restored."
                else "Wallet restored securely on this device. Select it again to retry the payment service connection. No funds moved.",
        )
        notifyUser(if (connected) "Wallet restored" else "Wallet restored locally")
    }

    /** Local keys remain usable even if provider login is temporarily unavailable. */
    private suspend fun activateLocalSeed(
        seed: ByteArray, residency: String, backupStatus: String,
        authMessage: String = "Secure the restored wallet on this device",
        phrase: String? = null, automatic: Boolean = false,
    ): Boolean {
        if (!automatic) authenticateForSigning(authMessage)
        val previous = EmbeddedWallet.activeAddress(this)
        fun import() = if (phrase != null) EmbeddedWallet.importMnemonic(this, seed, phrase)
            else EmbeddedWallet.importSeed(this, seed)
        suspend fun <T> withDeviceAuth(block: () -> T): T = try { block() } catch (error: Exception) {
            if (!automatic || !generateSequence(error as Throwable?) { it.cause }.any {
                it is android.security.keystore.UserNotAuthenticatedException
            }) throw error
            authenticateForSigning(authMessage)
            block()
        }
        val wallet = withDeviceAuth { import() }
        val publicKey = withDeviceAuth { EmbeddedWallet.publicKeyBytes(this) }
        require(Base58.encode(publicKey) == wallet) { "Restored wallet address does not match its key" }
        if (previous != null && previous != wallet) EmbeddedWallet.select(this, previous)
        store.put("backupStatus_$wallet", backupStatus)
        state = state.copy(
            hasLocalWallet = true,
            localWallets = EmbeddedWallet.addresses(this),
            restorePhraseInput = "", restoreBlobInput = "", restorePassphrase = "",
        )
        store.put("residencyCountry", residency)
        return runCatching {
            try { switchLocalWalletInternal(wallet, alreadyAuthenticated = true) }
            catch (error: Exception) {
                if (!automatic || !generateSequence(error as Throwable?) { it.cause }.any {
                    it is android.security.keystore.UserNotAuthenticatedException
                }) throw error
                authenticateForSigning(authMessage)
                switchLocalWalletInternal(wallet, alreadyAuthenticated = true)
            }
        }.isSuccess
    }

    // handleQr keeps the camera jurisdiction-agnostic: it detects the market from the
    // scanned code (EMVCo tag 58 or an Alipay/WeChat/UnionPay URL) rather than requiring
    // a pre-selection. Recipient parsing happens before the KYC gate where Gaian permits
    // it (VN/PH). Other corridors preserve the payload and auto-parse after KYC because
    // their provider contract forbids revealing beneficiary data to an unverified user.
    // MARKET remains a fallback for unknown/disabled codes and Peru's network choice.
    private fun rejectScannedQr(payload: String, reason: String, scannerStep: Step = Step.SCAN) {
        state = state.copy(step = scannerStep, busy = false, scannerSession = state.scannerSession + 1,
            scanFailures = state.scanFailures + 1, rejectedQrPayload = payload,
            scannerErrorReason = reason, qrPayload = "", parsed = null, quote = null, message = reason)
    }

    private fun handleQr(payload: String, explicitMarket: String = "") {
        if (state.busy) {
            store.put("pendingScanPayload", payload)
            state = state.copy(scannerSession = state.scannerSession + 1, scannerErrorReason = "", message = "QR saved while the account finishes loading.")
            return
        }
        launchTask("Detecting market…") {
        val route = classifyScannedQr(payload)
        if (route == ScannedQrRoute.UNKNOWN) {
            rejectScannedQr(payload, unsupportedQrReason(payload))
            return@launchTask
        }
        if (state.wallet.isBlank()) {
            val routeName = if (route == ScannedQrRoute.SOLANA)
                "Solana wallet transfer" else "Local bank/merchant QR payment"
            store.put("pendingScanPayload", payload)
            state = state.copy(
                step = Step.CONNECT, busy = false, qrRoute = routeName, qrPayload = payload,
                message = "$routeName scanned. Create, connect, or restore a wallet to continue. Your QR is saved.",
            )
            window.decorView.scanFeedback(accepted = true)
            return@launchTask
        }
        // Unified scanner: a Solana wallet address or Solana Pay URI routes straight to
        // the Send flow; anything else is treated as a local merchant QR (EMVCo below).
        val solana = runCatching { PeerTransferInput.parse(payload) }.getOrNull()
        if (solana != null) {
            val mint = solana.mint ?: allowedMints().firstOrNull()
            if (mint == null || mint !in allowedMints()) {
                rejectScannedQr(payload, "This Solana token isn’t supported. Ask for a Solana USDC recipient QR.")
            } else {
                state = state.copy(
                    step = Step.SEND,
                    peerRecipientInput = solana.address,
                    peerRecipient = solana.address,
                    peerMint = mint,
                    qrRoute = "Solana wallet transfer",
                    busy = false,
                    message = "Solana recipient detected — enter the amount to send.",
                )
                window.decorView.scanFeedback(accepted = true)
            }
            return@launchTask
        }
        val detection = api.detectQr(payload)
        val detected = if (!detection.detected && route == ScannedQrRoute.PROVIDER_PAYMENT &&
            state.paymentMarkets.any { it.country == explicitMarket }) {
            detection.copy(detected = true, country = explicitMarket, enabled = true, networkRequired = explicitMarket == "PE")
        } else detection
        state = state.copy(qrRoute = "Local bank/merchant QR payment", qrPayload = payload, scannerErrorReason = "")
        val market = state.paymentMarkets.firstOrNull { it.country == detected.country }
        when {
            !detected.detected || detected.country.length != 2 -> {
                if (route == ScannedQrRoute.PROVIDER_PAYMENT) {
                    state = state.copy(step = Step.MARKET, busy = false, paymentCountry = "",
                        message = "This payment format needs provider verification. Choose its country to verify the scanned QR.")
                } else {
                    rejectScannedQr(payload, "The local payment data failed validation. Its country, format or checksum could not be verified. Ask for another payment QR.")
                }
            }
            market == null || !detected.enabled ->
                rejectScannedQr(payload, "Payments to ${countryName(detected.country)} aren’t available through the current provider. Ask for a supported payment QR.")
            detected.networkRequired && state.paymentNetwork !in setOf("YAPE", "PLIN") ->
                state = state.copy(step = Step.MARKET, busy = false, paymentCountry = detected.country, message = "Choose Yape or Plin for ${countryName(detected.country)} to verify the scanned QR.")
            state.userId.isBlank() -> {
                savePendingKycPayment(payload, detected.country, state.paymentNetwork)
                state = state.copy(
                    step = Step.KYC,
                    busy = false,
                    paymentCountry = detected.country,
                    qrPayload = payload,
                    parsed = null,
                    message = "QR detected for ${countryName(detected.country)}. Register a payment profile to verify its recipient; no rescan is needed.",
                )
            }
            !market.approved && !canParseQrBeforeKyc(detected.country) -> {
                savePendingKycPayment(payload, detected.country, state.paymentNetwork)
                state = state.copy(
                    step = Step.KYC,
                    busy = false,
                    paymentCountry = detected.country,
                    qrPayload = payload,
                    parsed = null,
                    message = "QR saved. Gaian requires identity verification in ${countryName(detected.country)} before it will reveal and verify the beneficiary. You will not need to scan again.",
                )
            }
            else -> {
                val parsed = try {
                    api.parseQr(payload, detected.country, state.paymentNetwork)
                } catch (error: ApiException) {
                    if (error.code != "recipient_unverified") throw error
                    rejectScannedQr(payload, "${railName(detected.country, market.scheme)} was recognized, but the recipient name could not be verified. Ask for another QR or choose a different image.")
                    return@launchTask
                }
                val needsKyc = !market.approved
                state = state.copy(
                    step = Step.CONFIRM,
                    busy = false,
                    paymentCountry = detected.country,
                    qrPayload = payload,
                    parsed = parsed,
                    amount = parsed.amount.substringBefore('.').toLongOrNull()?.takeIf { it > 0 }?.toString().orEmpty(),
                    message = if (needsKyc) {
                        "Recipient verified. Review every payment detail; identity verification is requested only when you continue."
                    } else {
                        "Review the verified recipient and amount, then tap Pay to see the exact wallet debit."
                    }
                )
            }
        }
        if (state.step != Step.SCAN && state.scannerErrorReason.isBlank()) {
            window.decorView.scanFeedback(accepted = true)
        }
    }
    }

    private fun createQuote() = launchTask("Locking exchange rate and fees…") {
        val amount = state.amount.toLongOrNull()?.takeIf { it > 0 } ?: error("Enter a valid amount")
        // The Pay action uses the app terms declaration and the controlled "other"
        // code when no optional purpose chip was selected.
        val purpose = state.paymentPurpose.ifBlank { "other" }
        state = state.copy(sourceOfFundsConfirmed = true, paymentPurpose = purpose)
        try {
            val quote = api.quote(state.qrPayload, amount, state.userId, state.paymentCountry, state.paymentNetwork, purpose)
            refreshBalance()
            state = state.copy(step = Step.QUOTE, quote = quote, busy = false, message = "Review the exact wallet debit before signing.")
        } catch (error: ApiException) {
            if (!error.requiresKyc) throw error
            kycReturnStep = Step.CONFIRM
            savePendingKycPayment(
                payload = state.qrPayload,
                country = state.paymentCountry,
                network = state.paymentNetwork,
                amount = state.amount,
                purpose = state.paymentPurpose,
                sourceConfirmed = state.sourceOfFundsConfirmed,
            )
            state = state.copy(
                step = Step.KYC,
                busy = false,
                quote = null,
                message = "Gaian requires completed identity and nationality verification before this payment. Complete KYC, refresh your status, then request a new quote.",
            )
        }
    }

    private fun pay() = launchTask("Creating payment order…") { runPayment() }

    // The amount screen can show only an indicative conversion. Show the exact
    // provider quote and wallet debit before an order can be created or signed.
    private fun payFromInput() = launchTask("Locking rate and preparing payment…") {
        require(recipientNameResolved(state.parsed?.merchant)) { "Recipient name is unverified. Scan another QR before paying." }
        val amount = state.amount.toLongOrNull()?.takeIf { it > 0 } ?: error("Enter a valid amount")
        val purpose = state.paymentPurpose.ifBlank { "other" }
        state = state.copy(sourceOfFundsConfirmed = true, paymentPurpose = purpose)
        val quote = try {
            api.quote(state.qrPayload, amount, state.userId, state.paymentCountry, state.paymentNetwork, purpose)
        } catch (error: ApiException) {
            if (!error.requiresKyc) throw error
            kycReturnStep = Step.CONFIRM
            savePendingKycPayment(
                payload = state.qrPayload,
                country = state.paymentCountry,
                network = state.paymentNetwork,
                amount = state.amount,
                purpose = state.paymentPurpose,
                sourceConfirmed = state.sourceOfFundsConfirmed,
            )
            state = state.copy(
                step = Step.KYC,
                busy = false,
                quote = null,
                message = "Gaian requires completed identity and nationality verification before this payment. Complete KYC, refresh your status, then try again.",
            )
            return@launchTask
        }
        refreshBalance()
        state = state.copy(step = Step.QUOTE, quote = quote, busy = false, message = "Review the exact wallet debit before final approval.")
    }

    private suspend fun runPayment() {
        require(recipientNameResolved(state.parsed?.merchant)) { "Recipient name is unverified. Scan another QR before paying." }
        require(state.walletAuthorized) { "Sending is not enabled for this wallet. Your quote has not moved funds." }
        val quote = state.quote ?: error("Quote missing")
        refreshBalance()
        val funding = paymentFundingStatus(state.balanceUsd, state.balanceLoaded, quote.settlementAmount, quote.settlementCurrency)
        if (funding.availableUsdc == null || funding.requiredUsdc == null) {
            state = state.copy(step = Step.CONFIRM, busy = false, message = "USDC balance or quote amount is unavailable. Check balance and try again.")
            return
        }
        if (funding.insufficient) {
            state = state.copy(step = Step.CONFIRM, busy = false, message = "Insufficient USDC balance. Add funds and try again.")
            return
        }
        state = state.copy(step = Step.PAYING)
        var order = try {
            api.placeOrder(state.quote?.id ?: error("Quote missing"))
        } catch (error: ApiException) {
            if (!error.requiresKyc) throw error
            kycReturnStep = Step.CONFIRM
            savePendingKycPayment(
                payload = state.qrPayload,
                country = state.paymentCountry,
                network = state.paymentNetwork,
                amount = state.amount,
                purpose = state.paymentPurpose,
                sourceConfirmed = state.sourceOfFundsConfirmed,
            )
            state = state.copy(
                step = Step.KYC,
                busy = false,
                quote = null,
                message = "Identity verification is required before creating this payment order. No funds moved. After approval, request a fresh quote.",
            )
            return
        }
        val transactionHash = if (order.encodedTransaction.startsWith("mock:")) {
            "mock-${System.currentTimeMillis()}"
        } else {
            if (order.chainId != 101) error("Provider returned an order for the wrong network")
            val transaction = decodeTransaction(order.encodedTransaction)
            SolanaTransactionValidator.validate(
                transaction = transaction,
                wallet = state.wallet,
                depositAddress = order.depositAddress,
                settlementAmount = state.quote?.settlementAmount ?: error("Quote missing"),
                allowedMints = allowedMints()
            )
            signAndSend(transaction)
        }
        order = api.verifyOrder(order.id, transactionHash)
        state = state.copy(order = order, message = "Deposit submitted. Waiting for recipient settlement…")
        var polls = 0
        while (order.status !in setOf(10, 20, 21, 22) && polls < 60) {
            delay(2_000)
            order = api.orderStatus(order.id)
            state = state.copy(order = order, message = "Settlement: ${order.label}")
            polls++
        }
        if (order.status != 10) error("Payment ended with status: ${order.label}")
        state = state.copy(step = Step.COMPLETE, order = order, busy = false, message = "Local-currency payment completed.")
    }

    private fun beginPeerSend() {
        val mint = allowedMints().firstOrNull()
        state = if (mint == null) {
            state.copy(message = "This build has no allowlisted Solana mint; peer sending is disabled.")
        } else {
            state.copy(step = Step.SEND, peerMint = mint, message = "Enter or scan the recipient wallet, then enter the USDC amount.")
        }
    }

    private fun handlePeerQr(payload: String) {
        runCatching {
            val parsed = PeerTransferInput.parse(payload)
            val mint = parsed.mint ?: state.peerMint.ifBlank { allowedMints().firstOrNull() ?: error("No token mint is configured") }
            require(mint in allowedMints()) { "The scanned token mint is not supported" }
            state = state.copy(
                step = Step.SEND,
                peerRecipientInput = parsed.address,
                peerRecipient = parsed.address,
                peerMint = mint,
                message = "Recipient loaded. Enter the amount and review carefully."
            )
            window.decorView.scanFeedback(accepted = true)
        }.onFailure {
            rejectScannedQr(payload, if (classifyScannedQr(payload) == ScannedQrRoute.SOLANA)
                    "This Solana token isn’t supported. Ask for a Solana USDC recipient QR."
                else "This scanner accepts Solana wallet recipients only. " + unsupportedQrReason(payload), Step.SEND_SCAN)
        }
    }

    private fun reviewPeerTransfer() {
        runCatching {
            val parsed = PeerTransferInput.parse(state.peerRecipientInput)
            val mint = parsed.mint ?: state.peerMint
            require(mint in allowedMints()) { "The selected token mint is not supported" }
            require(parsed.address != state.wallet) { "You cannot send to the connected wallet" }
            val amount = state.peerAmount.trim()
            require(amount.matches(Regex("""\d+(?:\.\d{1,6})?""")) && BigDecimal(amount) > BigDecimal.ZERO) {
                "Enter a positive amount with at most 6 decimal places"
            }
            state = state.copy(
                step = Step.SEND_CONFIRM,
                peerRecipientInput = parsed.address,
                peerRecipient = parsed.address,
                peerAmount = amount,
                peerMint = mint,
                message = "Verify the full recipient address. On-chain transfers cannot be reversed."
            )
        }.onFailure { state = state.copy(message = it.message ?: "Transfer details are invalid") }
    }

    private fun sendPeerTransfer() = launchTask("Building and validating unsigned transfer…") {
        require(state.walletAuthorized && state.peerTransfersEnabled) { "Sending is not enabled for this wallet. No transfer was prepared." }
        state = state.copy(step = Step.SENDING)
        val prepared = api.prepareTransfer(state.peerRecipient, state.peerAmount, state.peerMint)
        require(prepared.recipient == state.peerRecipient) { "Server response changed the recipient" }
        require(prepared.amount == state.peerAmount) { "Server response changed the amount" }
        require(prepared.mint == state.peerMint) { "Server response changed the token mint" }
        require(prepared.decimals == 6 && prepared.lastValidBlockHeight > 0) { "Server returned invalid transfer metadata" }
        val transaction = decodeTransaction(prepared.encodedTransaction)
        val summary = SolanaTransactionValidator.validatePeerTransfer(
            transaction = transaction,
            wallet = state.wallet,
            recipient = state.peerRecipient,
            amount = state.peerAmount,
            mint = state.peerMint,
            allowedMints = allowedMints()
        )
        require(summary.baseUnits.toString() == prepared.baseUnits) { "Server metadata does not match the transaction" }
        val signature = signAndSend(transaction)
        val activitySyncFailed = runCatching { api.markTransferSubmitted(prepared.transferId, signature) }.isFailure
        state = state.copy(
            step = Step.SEND_COMPLETE,
            peerSignature = signature,
            busy = false,
            message = if (activitySyncFailed) {
                "Transfer broadcast, but activity sync failed. Keep the signature and verify it in your wallet."
            } else {
                "Transfer broadcast and recorded. On-chain confirmation is not yet tracked in-app."
            }
        )
    }

    private fun loadActivity() = launchTask("Loading payment activity…") {
        val page = api.activity()
        state = state.copy(
            step = Step.ACTIVITY,
            activities = page.items,
            busy = false,
            message = if (page.items.isEmpty()) "No payments recorded yet." else "Payment activity and receipts"
        )
    }

    private fun repeatActivity(item: ActivityItem) {
        if (item.kind == "onchain_transfer" && item.repeatable) {
            state = state.copy(
                step = Step.SEND,
                peerRecipientInput = item.recipient,
                peerRecipient = item.recipient,
                peerAmount = item.settlementAmount,
                peerMint = item.settlementCurrency,
                message = "Review this saved recipient. A fresh transaction will be built and signed."
            )
        } else {
            state = state.copy(
                step = Step.SCAN,
                scannerErrorReason = "",
                message = "Merchant payments require a fresh scan until an encrypted payee template has been saved."
            )
        }
    }

    private fun openSavedPayees() {
        val payees = savedPayees.list()
        state = state.copy(
            step = Step.SAVED,
            savedPayees = payees,
            message = if (payees.isEmpty()) "No saved payees yet." else "Saved payment details are encrypted on this device."
        )
    }

    private fun saveCurrentMerchant() {
        val dynamic = state.parsed?.dynamic == true
        runCatching { savedPayees.saveMerchant(state.qrPayload, state.parsed ?: error("Payment details missing"), state.paymentCountry, state.paymentNetwork, state.paymentNote) }
            .onSuccess {
                state = state.copy(message = if (dynamic) "Recipient reference saved. Scan a fresh QR before each payment." else "Static merchant payment details saved on this device.")
                notifyUser(if (dynamic) "Reference saved — fresh QR required" else "Payee saved")
            }
            .onFailure { state = state.copy(message = it.message ?: "Could not save this payee") }
    }

    private fun saveCurrentWallet() {
        runCatching { savedPayees.saveWallet(state.peerRecipient, state.peerMint) }
            .onSuccess { state = state.copy(message = "Wallet recipient saved on this device."); notifyUser("Recipient saved") }
            .onFailure { state = state.copy(message = it.message ?: "Could not save this recipient") }
    }

    private fun useSavedPayee(payee: SavedPayee) {
        if (payee.kind == "wallet") {
            state = state.copy(
                step = Step.SEND,
                peerRecipientInput = payee.destination,
                peerRecipient = payee.destination,
                peerMint = payee.mint,
                message = "Saved recipient loaded. Enter an amount and review it before signing."
            )
            return
        }
        if (payee.kind == "merchant_reference") {
            state = state.copy(step = Step.SCAN, scannerSession = state.scannerSession + 1,
                scannerErrorReason = "",
                message = "${payee.label}: scan a fresh QR. This reference cannot start a payment.")
            return
        }
        launchTask("Revalidating saved merchant details…") {
            require(payee.country.isNotBlank()) { "This saved payee predates market verification; scan it again before paying" }
            val parsed = api.parseQr(payee.payload, payee.country, payee.network)
            require(!parsed.dynamic) { "Saved dynamic QR payments cannot be replayed" }
            state = state.copy(
                step = Step.CONFIRM,
                busy = false,
                qrPayload = payee.payload,
                paymentCountry = payee.country,
                paymentNetwork = payee.network,
                parsed = parsed,
                amount = "",
                paymentNote = payee.note,
                message = "Saved payee revalidated. Enter an amount; a fresh quote and transaction are required."
            )
        }
    }

    private fun deleteSavedPayee(payee: SavedPayee) {
        savedPayees.delete(payee.id)
        openSavedPayees()
        notifyUser("Saved payee removed")
    }

    private fun renameSavedPayee(payee: SavedPayee, note: String) {
        savedPayees.rename(payee.id, note)
        openSavedPayees()
        notifyUser(if (note.isBlank()) "Note removed" else "Note saved")
    }

    // Gates an embedded-wallet signing operation behind a biometric / device-credential
    // prompt. If the device has no biometric or PIN enrolled we don't block (the wallet
    // is already device-bound); binding the Keystore wrap key to user-auth for
    // hardware-enforced gating is a follow-up.
    private suspend fun authenticateForSigning(reason: String) {
        // STRONG biometric or device credential — required to authorize the hardware
        // auth-bound wallet key (a weak biometric would not unlock it).
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        // Fail closed: never sign a payment without user presence. Device biometrics are
        // used when enrolled; otherwise the device credential (PIN/pattern/password) is
        // accepted. If neither exists we stop and tell the user to set one up.
        if (BiometricManager.from(this).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            error("Set up fingerprint, face unlock, or a device PIN/pattern in your phone settings to authorize payments.")
        }
        val approved = suspendCancellableCoroutine { continuation ->
            val prompt = BiometricPrompt(
                this,
                ContextCompat.getMainExecutor(this),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (continuation.isActive) continuation.resume(true)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (continuation.isActive) continuation.resume(false)
                    }
                },
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Confirm it's you")
                .setSubtitle(reason)
                .setAllowedAuthenticators(authenticators)
                .build()
            prompt.authenticate(info)
        }
        require(approved) { "Verification cancelled" }
    }

    // True when the connected wallet is this device's embedded wallet (vs an external
    // MWA wallet), so signing + submission happen locally instead of via a wallet app.
    private fun usingEmbeddedWallet(): Boolean =
        EmbeddedWallet.exists(applicationContext) &&
            EmbeddedWallet.publicAddress(applicationContext) == state.wallet

    private suspend fun signAndSend(transaction: ByteArray): String {
        if (usingEmbeddedWallet()) {
            // Sign the provider-built (and already-validated) transaction with the
            // on-device key after a biometric check, then broadcast via Solana RPC.
            val debit = state.quote?.let { "Approve exactly ${it.settlementAmount} ${it.settlementCurrency}" }
                ?: "Approve this payment"
            authenticateForSigning(debit)
            val signed = SolanaTx.signLegacy(transaction) { message ->
                EmbeddedWallet.sign(applicationContext, message)
            }
            return solanaRpc.sendTransaction(signed)
        }
        return when (val signed = walletAdapter.transact(sender) { signAndSendTransactions(arrayOf(transaction)) }) {
            is TransactionResult.Success -> {
                val bytes = signed.payload.signatures.firstOrNull() ?: error("Wallet returned no transaction signature")
                Base58.encode(bytes)
            }
            is TransactionResult.NoWalletFound -> error("Wallet app is unavailable")
            is TransactionResult.Failure -> throw signed.e
        }
    }

    private fun decodeTransaction(encoded: String): ByteArray {
        if (encoded.startsWith("0x")) {
            return encoded.drop(2).chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        }
        return runCatching { Base64.decode(encoded, Base64.DEFAULT) }
            .recoverCatching { Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP) }
            .getOrElse { error("Server returned an unsupported transaction encoding") }
    }

    private fun launchTask(label: String, block: suspend () -> Unit) {
        if (state.busy) return
        state = state.copy(busy = true, message = label)
        lifecycleScope.launch {
            runCatching { block() }.onFailure {
                val message = it.message ?: "Something went wrong"
                val scannerFailed = state.step == Step.SCAN || state.step == Step.SEND_SCAN
                state = state.copy(
                    busy = false,
                    message = message,
                    feedback = message,
                    feedbackId = state.feedbackId + 1,
                    scannerSession = if (scannerFailed) state.scannerSession + 1 else state.scannerSession,
                    scanFailures = if (scannerFailed) state.scanFailures + 1 else state.scanFailures,
                    scannerErrorReason = if (scannerFailed) message else "",
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun App(
    state: AppState,
    onConnect: () -> Unit,
    onCreateWallet: () -> Unit,
    onUnlockWallet: () -> Unit,
    onRecoveryCheck: (String) -> Unit,
    onRecoveryWritten: (Boolean) -> Unit,
    onVerifyRecovery: () -> Unit,
    onGenerateRecoveryPassword: () -> Unit,
    onFinishPaperBackup: () -> Unit,
    onShowRecoveryWords: () -> Unit,
    onWallets: () -> Unit,
    onSwitchWallet: (String) -> Unit,
    onResidency: (String) -> Unit,
    onKyc: () -> Unit,
    onRegister: () -> Unit,
    onNotify: (String) -> Unit,
    onRename: (String) -> Unit,
    onToggleBalance: () -> Unit,
    onOpenBackup: () -> Unit,
    onBackupLocalWallet: (String) -> Unit,
    onCreateBackup: () -> Unit,
    onShareBackup: () -> Unit,
    onImportBackupFile: () -> Unit,
    onBackupPass: (String) -> Unit,
    onBackupPassConfirm: (String) -> Unit,
    onOpenRestore: () -> Unit,
    onRestore: () -> Unit,
    onRestoreMnemonic: () -> Unit,
    onRestoreBlob: (String) -> Unit,
    onRestorePass: (String) -> Unit,
    onRestorePhrase: (String) -> Unit,
    onBackToConnect: () -> Unit,
    onRefresh: () -> Unit,
    onRefreshBalance: () -> Unit,
    onPayRail: () -> Unit,
    onPayMarket: (String) -> Unit,
    onPaymentNetwork: (String) -> Unit,
    onContinuePay: () -> Unit,
    onQr: (String) -> Unit,
    onSharedImageConsumed: () -> Unit,
    onManual: () -> Unit,
    onManualCountry: (String) -> Unit,
    onManualBank: (String) -> Unit,
    onManualAccount: (String) -> Unit,
    onAmount: (String) -> Unit,
    onAmountAppend: (String) -> Unit,
    onAmountDelete: () -> Unit,
    onPaymentPurpose: (String) -> Unit,
    onPaymentNote: (String) -> Unit,
    onToggleBalanceHidden: () -> Unit,
    onQuote: () -> Unit,
    onPay: () -> Unit,
    onSendRail: () -> Unit,
    onPeerRecipient: (String) -> Unit,
    onPeerAmount: (String) -> Unit,
    onPeerScan: () -> Unit,
    onPeerQr: (String) -> Unit,
    onPeerReview: () -> Unit,
    onPeerSend: () -> Unit,
    onFunding: () -> Unit,
    onReceive: () -> Unit,
    onActivity: () -> Unit,
    onRepeat: (ActivityItem) -> Unit,
    onSaved: () -> Unit,
    onProfile: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onExportData: () -> Unit,
    onUseSaved: (SavedPayee) -> Unit,
    onDeleteSaved: (SavedPayee) -> Unit,
    onRenamePayee: (SavedPayee, String) -> Unit,
    onSaveMerchant: () -> Unit,
    onSaveWallet: () -> Unit,
    onHome: () -> Unit,
    onBack: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmSignOut by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showRecoveryPassword by remember(state.step) { mutableStateOf(false) }
    LaunchedEffect(state.feedbackId) {
        if (state.feedbackId > 0 && state.feedback.isNotBlank()) snackbarHostState.showSnackbar(state.feedback)
    }
    BackHandler(enabled = state.step !in setOf(Step.CONNECT, Step.HOME)) { onBack() }
    val scrollState = rememberScrollState()
    LaunchedEffect(state.step) { scrollState.scrollTo(0) }
    // Screenshots stay blocked (FLAG_SECURE) only on screens that reveal the recovery
    // phrase or backup; elsewhere (home, activity, receive QR…) they are allowed.
    val secureActivity = LocalActivity.current
    LaunchedEffect(state.step) {
        secureActivity?.window?.let { w ->
            val secret = state.step == Step.BACKUP || state.step == Step.PHRASE || state.step == Step.RESTORE
            if (secret) {
                w.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                w.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
    val topLevel = state.step in setOf(Step.HOME, Step.ACTIVITY, Step.PROFILE)
    // The payment amount-entry and review steps use a dark "focus" treatment.
    val darkPay = state.step == Step.CONFIRM || state.step == Step.QUOTE
    Scaffold(
        containerColor = if (darkPay) PayBg else MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (state.step !in setOf(Step.CONNECT, Step.SCAN, Step.SEND_SCAN)) {
                TopAppBar(
                    title = { Text(screenTitle(state.step), style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        if (!topLevel) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = if (darkPay) Color.White else BrandInk,
                                )
                            }
                        }
                    },
                    actions = {
                        if (state.step == Step.PROFILE) {
                            TextButton(onClick = { confirmSignOut = true }) { Text("Sign out") }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (darkPay) PayBg else MaterialTheme.colorScheme.background,
                        titleContentColor = if (darkPay) Color.White else BrandInk,
                    ),
                )
            }
        },
        bottomBar = {
            if (state.wallet.isNotBlank() && topLevel) {
                NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                    NavigationBarItem(state.step == Step.HOME, onHome, { Icon(Icons.Filled.Home, null) }, label = { Text("Home") })
                    NavigationBarItem(false, onPayRail, { Icon(Icons.Filled.QrCodeScanner, null) }, label = { Text("Pay") })
                    NavigationBarItem(state.step == Step.ACTIVITY, onActivity, { Icon(Icons.Filled.History, null) }, label = { Text("Activity") })
                    NavigationBarItem(state.step == Step.PROFILE, onProfile, { Icon(Icons.Filled.Person, null) }, label = { Text("Profile") })
                }
            }
        },
    ) { scaffoldPadding ->
    if (state.step == Step.SCAN) {
        FullScreenScanner(
            sessionId = state.scannerSession,
            priorFailures = state.scanFailures,
            onQr = onQr,
            onSharedImageConsumed = onSharedImageConsumed,
            prompt = "Scan a Solana wallet or local bank/merchant QR",
            permissionLabel = "Allow camera to scan",
            onCancel = onBack,
            onManual = onManual,
            sharedImage = state.sharedImage,
            onSavedPayees = onSaved,
            hasSavedPayees = state.savedPayees.any { it.kind != "wallet" },
            skipPayload = state.rejectedQrPayload,
            notice = state.message,
            rejectionReason = state.scannerErrorReason,
            modifier = Modifier.fillMaxSize().padding(scaffoldPadding),
        )
    } else if (state.step == Step.SEND_SCAN) {
        FullScreenScanner(
            sessionId = state.scannerSession,
            priorFailures = state.scanFailures,
            onQr = onPeerQr,
            onSharedImageConsumed = {},
            prompt = "Point your camera at the recipient's Solana wallet QR",
            permissionLabel = "Allow camera to scan",
            onCancel = onSendRail,
            onManual = null,
            sharedImage = "",
            onSavedPayees = onSaved,
            hasSavedPayees = state.savedPayees.any { it.kind == "wallet" },
            skipPayload = state.rejectedQrPayload,
            notice = state.message,
            rejectionReason = state.scannerErrorReason,
            modifier = Modifier.fillMaxSize().padding(scaffoldPadding),
        )
    } else if (darkPay) {
        // Full-height dark focus screen; not inside the scrolling column so the hero
        // amount can center and the slide-to-pay control pins to the bottom.
        Box(
            Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .background(PayBg)
                .imePadding(),
        ) {
            when (state.step) {
                Step.CONFIRM -> PaymentInputScreen(
                    state = state,
                    onAmount = onAmount,
                    onAmountAppend = onAmountAppend,
                    onAmountDelete = onAmountDelete,
                    onPaymentPurpose = onPaymentPurpose,
                    onPaymentNote = onPaymentNote,
                    onToggleBalanceHidden = onToggleBalanceHidden,
                    onQuote = onQuote,
                    onRegister = onRegister,
                    onSaveMerchant = onSaveMerchant,
                    onBack = onBack,
                )
                else -> PaymentQuoteScreen(
                    state = state,
                    onPay = onPay,
                    onFunding = onFunding,
                    onRefreshBalance = onRefreshBalance,
                    onBack = onBack,
                    onPaymentPurpose = onPaymentPurpose,
                )
            }
        }
    } else {
    Box(Modifier.fillMaxSize().padding(scaffoldPadding)) {
      Column(
        Modifier
            .align(Alignment.TopCenter)
            .fillMaxHeight()
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
      ) {
        if (state.step == Step.CONNECT) {
            Image(
                painter = painterResource(R.drawable.wayqo_logo),
                contentDescription = "WAYQO",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().heightIn(max = 87.dp).padding(horizontal = 24.dp, vertical = 4.dp)
            )
            Text("Your wallet.\nLocal payments.", style = MaterialTheme.typography.headlineLarge, color = BrandInk)
        }
        if (state.message.isNotBlank()) StatusMessage(state.message)
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())

        when (state.step) {
            Step.CONNECT -> {
                Button(onClick = onPayRail, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Scan to pay")
                }
                Text("Scan a Solana wallet QR or a local bank/merchant QR. You can use the camera or choose an image before setting up a wallet.", style = MaterialTheme.typography.bodyMedium)
                if (state.qrRoute.isNotBlank()) StatusCard("Scanned route", state.qrRoute + " · saved while you set up")
                Text("Set up your wallet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Choose your legal country of residence. This determines identity checks; it does not limit where you can scan and pay.", style = MaterialTheme.typography.bodyMedium, color = BrandGreyMuted)
                CountryPickerField(selectedCode = state.residencyCountry, onSelect = onResidency)
                if (state.hasLocalWallet) {
                    Button(onClick = onUnlockWallet, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Unlock wallet on this device") }
                    OutlinedButton(onClick = onWallets, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Choose local wallet") }
                }
                OutlinedButton(onClick = onCreateWallet, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Create wallet") }
                Text("A secure wallet is created automatically. You can export its recovery later from Wallets or Back up wallet.", style = MaterialTheme.typography.bodySmall, color = BrandGreyMuted)
                OutlinedButton(onClick = onConnect, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Connect wallet") }
                TextButton(onClick = onOpenRestore, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Restore wallet") }
                Text("Keys stay on this device. WAYQO cannot sign or move funds without you.", style = MaterialTheme.typography.labelSmall, color = BrandGreyMuted)
                LegalNoticeButton()
            }
            Step.PHRASE -> {
                StatusCard("Recovery words", "Write all 24 words on paper, in order. Anyone with these words controls this wallet. WAYQO cannot recover them for you.")
                Text(state.recoveryPhrase.split(' ').mapIndexed { index, word -> "${index + 1}. $word" }.chunked(4).joinToString("\n") { it.joinToString("    ") }, style = MaterialTheme.typography.bodyLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = state.recoveryWritten, onCheckedChange = onRecoveryWritten)
                    Text("I wrote down all 24 words in order")
                }
                OutlinedTextField(
                    value = state.recoveryCheck, onValueChange = onRecoveryCheck,
                    label = { Text("Enter words ${state.recoveryPositions.joinToString(", ")} in order") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Use spaces or new lines between words. Commas are accepted too.", style = MaterialTheme.typography.bodySmall, color = BrandGreyMuted)
                OutlinedButton(onClick = onVerifyRecovery, enabled = state.recoveryWritten, modifier = Modifier.fillMaxWidth()) { Text("Check recovery words") }
                Button(onClick = onFinishPaperBackup, enabled = state.recoveryWritten && state.recoveryVerified && !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Finish paper backup") }
                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to backup options") }
            }
            Step.WALLETS -> {
                Text("Local wallets", style = MaterialTheme.typography.titleMedium)
                if (state.residencyCountry.isBlank()) {
                    Text("Select your country of residence before unlocking a wallet.", color = BrandGreyMuted)
                    CountryPickerField(selectedCode = state.residencyCountry, onSelect = onResidency)
                }
                state.localWallets.forEach { address ->
                    OutlinedButton(onClick = { onSwitchWallet(address) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text("${address.take(8)}…${address.takeLast(8)}${if (address == state.wallet) " · current" else ""}")
                    }
                    TextButton(onClick = { onBackupLocalWallet(address) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text("Back up ${address.take(8)}…${address.takeLast(8)}")
                    }
                }
                if (state.externalWallets.isNotEmpty()) {
                    Text("Previously connected external wallets", style = MaterialTheme.typography.titleMedium)
                    state.externalWallets.forEach { address ->
                        Text("${address.take(8)}…${address.takeLast(8)} · reconnect in your wallet app", color = BrandGreyMuted)
                    }
                }
                OutlinedButton(onClick = onCreateWallet, modifier = Modifier.fillMaxWidth()) { Text("Create another wallet") }
                OutlinedButton(onClick = onConnect, modifier = Modifier.fillMaxWidth()) { Text("Connect external wallet") }
                TextButton(onClick = onOpenRestore, modifier = Modifier.fillMaxWidth()) { Text("Restore wallet") }
            }
            Step.MANUAL -> {
                StatusCard("Unverified manual draft", "This is a private record only. No payment can be made from these details until a provider verifies the recipient.")
                CountryPickerField(selectedCode = state.manualCountry, onSelect = onManualCountry, label = "Search recipient bank country")
                OutlinedTextField(value = state.manualBank, onValueChange = onManualBank, label = { Text(manualBankLabel(state.manualCountry)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = state.manualAccount, onValueChange = onManualAccount, label = { Text("Account number or identifier") }, modifier = Modifier.fillMaxWidth())
                if (manualDraftComplete(state.manualCountry, state.manualBank, state.manualAccount)) {
                    Text("Draft saved encrypted on this device · unverified", color = BrandGreenInk)
                } else Text("Country, bank identifier, and account details are required to complete this draft.", color = BrandGreyMuted)
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to scan") }
            }
            Step.HOME -> {
                Image(
                    painter = painterResource(R.drawable.wayqo_logo),
                    contentDescription = "WAYQO",
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxWidth(0.58f).heightIn(max = 44.dp).padding(top = 4.dp),
                )
                WalletSummaryCard(
                    walletName = state.walletName,
                    wallet = state.wallet,
                    balanceUsd = state.balanceUsd,
                    balanceLoaded = state.balanceLoaded,
                    balanceHidden = state.balanceHidden,
                    kycStatus = state.kycStatus,
                    approvedCorridors = state.paymentMarkets.filter { it.approved },
                    paymentsEnabled = state.userId.isNotBlank(),
                    hardwareProtected = state.hardwareProtected,
                    onKyc = onKyc,
                    onRename = onRename,
                    onToggleBalance = onToggleBalance,
                )
                OutlinedButton(onClick = onWallets, modifier = Modifier.fillMaxWidth()) { Text("Wallets · switch or add") }
                if (state.environment == "sandbox") {
                    StatusCard("Provider sandbox", "Solana transactions can move real mainnet USDC. Use only a controlled test wallet and review every signature.")
                }
                if (!state.walletAuthorized) {
                    StatusCard(
                        "Preview available",
                        "Scan or upload a merchant QR, review available payment details, and inspect a recipient transfer. Creating an order or sending USDC is still disabled for this wallet.",
                    )
                }
                if (state.userId.isBlank()) {
                    Button(onClick = onRegister, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text("Register payment profile")
                    }
                }
                Button(onClick = onPayRail, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Scan to pay", fontWeight = FontWeight.SemiBold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionButton("Send", Icons.AutoMirrored.Filled.Send, onSendRail, !state.busy)
                    ActionButton("Add funds", Icons.Filled.Add, onFunding, !state.busy)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionButton("Activity", Icons.Filled.History, onActivity, !state.busy)
                    ActionButton("Saved", Icons.Filled.Bookmarks, onSaved, !state.busy)
                }
                if (state.isEmbeddedWallet) {
                    OutlinedButton(onClick = onOpenBackup, enabled = !state.busy, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandInk)) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Back up wallet", fontWeight = FontWeight.Bold)
                    }
                }
                if (state.userId.isNotBlank() && !state.merchantPayReady) {
                    KycAccessCard(state.kycStatus, state.lastStatusChecked, state.merchantPayReady, onKyc, onRefresh)
                }
                Text("RECENT ACTIVITY", style = MaterialTheme.typography.labelSmall, color = BrandGreyMuted)
                if (state.activities.isEmpty()) {
                    Text("No recorded payments yet. Your first receipt will appear here.", style = MaterialTheme.typography.bodySmall)
                } else {
                    state.activities.take(3).forEach { item -> ActivityCard(item, onRepeat) }
                    OutlinedButton(onClick = onActivity, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("View all activity") }
                }
            }
            Step.PROFILE -> {
                ProfileScreen(
                    state = state,
                    onRename = onRename,
                    onKyc = onKyc,
                    onRegister = onRegister,
                    onRefresh = onRefresh,
                    onBackup = onOpenBackup,
                    onSignOut = { confirmSignOut = true },
                    onDeleteAccount = { confirmDelete = true },
                    onExportData = onExportData,
                    onNotify = onNotify,
                )
            }
            Step.KYC -> {
                state.parsed?.let { parsed ->
                    StatusCard(
                        "Payment details preserved",
                        "${parsed.merchant.ifBlank { "Verified recipient" }} · ${state.amount.ifBlank { parsed.amount }} ${parsed.currency} · ${countryName(state.paymentCountry)}. You will return to review after approval.",
                    )
                } ?: run {
                    if (state.qrPayload.isNotBlank() && state.paymentCountry.isNotBlank()) {
                        StatusCard(
                            "Scanned payment preserved",
                            "${countryName(state.paymentCountry)} · ${railName(state.paymentCountry, state.paymentMarkets.firstOrNull { it.country == state.paymentCountry }?.scheme.orEmpty())}. Gaian requires a verified identity before beneficiary details can be revealed in this corridor; the QR will be parsed automatically after approval.",
                        )
                    }
                }
                if (state.userId.isBlank()) {
                    Button(onClick = onRegister, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text("Register payment profile")
                    }
                } else {
                    KycAccessCard(state.kycStatus, state.lastStatusChecked, state.merchantPayReady, onKyc, onRefresh)
                }
                OutlinedButton(onClick = onBack, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            }
            Step.BACKUP -> {
                if (!state.backupHasMnemonic) {
                    StatusCard("Legacy wallet recovery", "This wallet has an arbitrary seed. A new BIP39 phrase would create a different address, so keep a verified encrypted backup for this wallet.")
                }
                if (state.backupHasMnemonic) StatusCard("Full wallet recovery", "This export contains the original BIP39 words and can restore the same Solana wallet and future chain accounts.")
                Text("Encrypt your wallet with a strong passphrase. A short PIN can be guessed from a stolen file. You'll need both the exported file and passphrase on a new device.", style = MaterialTheme.typography.bodySmall, color = BrandGreyMuted)
                TextButton(onClick = { onGenerateRecoveryPassword(); showRecoveryPassword = true }, enabled = !state.busy) { Text("Generate strong password") }
                OutlinedTextField(
                    value = state.backupPassphrase, onValueChange = onBackupPass, label = { Text("Backup password") }, singleLine = true,
                    visualTransformation = if (showRecoveryPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { TextButton(onClick = { showRecoveryPassword = !showRecoveryPassword }) { Text(if (showRecoveryPassword) "Hide" else "Show") } },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(value = state.backupPassphraseConfirm, onValueChange = onBackupPassConfirm, label = { Text("Confirm passphrase") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                PasswordConfirmationStatus(state.backupPassphrase, state.backupPassphraseConfirm)
                if (state.backupPassphrase.isNotBlank()) Text("Save this password in your password manager. It is required with the exported file to restore this wallet.", style = MaterialTheme.typography.bodySmall, color = BrandGreyMuted)
                Button(onClick = onCreateBackup, enabled = !state.busy && state.backupPassphrase.length >= 12 && state.backupPassphrase.any(Char::isLetter) && state.backupPassphrase == state.backupPassphraseConfirm, modifier = Modifier.fillMaxWidth()) { Text("Create encrypted backup") }
                if (state.backupBlob.isNotBlank()) {
                    StatusCard("Encrypted backup ready", "The backup was test-restored. Export it to a file and keep the passphrase separately.")
                    Button(onClick = onShareBackup, modifier = Modifier.fillMaxWidth()) { Text("Export encrypted file") }
                }
                if (state.backupHasMnemonic) {
                    TextButton(onClick = onShowRecoveryWords, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Show 24 recovery words instead") }
                }
                OutlinedButton(onClick = onBack, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            }
            Step.RESTORE -> {
                if (state.residencyCountry.isBlank()) CountryPickerField(selectedCode = state.residencyCountry, onSelect = onResidency)
                Text("Restore from a 24-word recovery phrase", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = state.restorePhraseInput, onValueChange = onRestorePhrase, label = { Text("24 words in order · spaces or new lines") }, minLines = 3, maxLines = 5, modifier = Modifier.fillMaxWidth())
                Button(onClick = onRestoreMnemonic, enabled = !state.busy && state.restorePhraseInput.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Restore from phrase") }
                HorizontalDivider()
                Text("Restore from an encrypted WAYQO file", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = onImportBackupFile, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Choose backup file") }
                OutlinedTextField(value = state.restoreBlobInput, onValueChange = onRestoreBlob, label = { Text("Paste your encrypted backup") }, minLines = 3, maxLines = 6, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = state.restorePassphrase, onValueChange = onRestorePass, label = { Text("Backup passphrase") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                Button(onClick = onRestore, enabled = !state.busy && state.restoreBlobInput.isNotBlank() && state.restorePassphrase.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Restore wallet") }
                OutlinedButton(onClick = onBackToConnect, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
            Step.MARKET -> {
                Text("Recipient payout market", style = MaterialTheme.typography.labelMedium, color = BrandGreyMuted)
                OutlinedButton(onClick = onBack, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Retry scan or choose another image") }
                if (state.scanFailures >= 3) {
                    OutlinedButton(onClick = onManual, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Keep an unverified bank-details draft") }
                }
                if (state.paymentMarkets.isEmpty()) {
                    Text("No provider corridors are currently quotable. Try refreshing your status.")
                } else {
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.paymentMarkets.forEach { market ->
                            FilterChip(
                                selected = state.paymentCountry == market.country,
                                onClick = { onPayMarket(market.country) },
                                label = { Text("${countryName(market.country)} · ${market.fiatCurrency}") }
                            )
                        }
                    }
                    val selected = state.paymentMarkets.firstOrNull { it.country == state.paymentCountry }
                    DetailCard("Market access", selected?.status ?: "UNAVAILABLE")
                    DetailCard("Payment rail", railName(state.paymentCountry, selected?.scheme.orEmpty()))
                    Text("Only corridors returned by the active provider appear here. Availability is not a regulatory approval.", style = MaterialTheme.typography.bodySmall)
                    if (state.paymentCountry == "CN") {
                        StatusCard("Mainland China pilot", "Business Weixin QR only. Personal receive codes and Alipay remain disabled until the provider explicitly contracts and tests them.")
                    }
                    if (state.paymentCountry == "PE") {
                        Text("Peru payment network", style = MaterialTheme.typography.labelMedium, color = BrandGreyMuted)
                        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("YAPE", "PLIN").forEach { network ->
                                FilterChip(selected = state.paymentNetwork == network, onClick = { onPaymentNetwork(network) }, label = { Text(network) })
                            }
                        }
                    }
                    Button(onClick = onContinuePay, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text(if (state.qrPayload.isNotBlank()) "Verify scanned QR" else "Continue to scan") }
                }
                OutlinedButton(onClick = onBack, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
            Step.SCAN -> {
                // Rendered by FullScreenScanner above.
            }
            Step.CONFIRM -> PaymentInputScreen(
                state = state,
                onAmount = onAmount,
                onAmountAppend = onAmountAppend,
                onAmountDelete = onAmountDelete,
                onPaymentPurpose = onPaymentPurpose,
                onPaymentNote = onPaymentNote,
                onToggleBalanceHidden = onToggleBalanceHidden,
                onQuote = onQuote,
                onRegister = onRegister,
                onSaveMerchant = onSaveMerchant,
                onBack = onBack,
            )
            Step.QUOTE -> PaymentQuoteScreen(
                state = state,
                onPay = onPay,
                onFunding = onFunding,
                onRefreshBalance = onRefreshBalance,
                onBack = onBack,
                onPaymentPurpose = onPaymentPurpose,
            )
            Step.PAYING -> {
                DetailCard("Order", state.order?.id ?: "Creating…")
                DetailCard("Status", state.order?.label ?: "Waiting for wallet")
                Text("Do not close the app until a terminal recipient-settlement status appears.")
            }
            Step.COMPLETE -> {
                DetailCard("Payment complete", state.order?.id.orEmpty())
                DetailCard("Transaction", state.order?.transactionHash.orEmpty())
                Button(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            }
            Step.SEND -> {
                if (state.qrRoute == "Solana wallet transfer") Text("Solana wallet transfer", style = MaterialTheme.typography.titleMedium, color = BrandPurple)
                OutlinedTextField(
                    value = state.peerRecipientInput,
                    onValueChange = onPeerRecipient,
                    label = { Text("Recipient wallet or Solana Pay URI") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = onPeerScan, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Scan recipient QR") }
                OutlinedTextField(
                    value = state.peerAmount,
                    onValueChange = onPeerAmount,
                    label = { Text("Amount (USDC)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                DetailCard("Token mint", state.peerMint)
                Button(onClick = onPeerReview, enabled = !state.busy && state.peerRecipientInput.isNotBlank() && state.peerAmount.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Review transfer") }
                OutlinedButton(onClick = onBack, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
            Step.SEND_SCAN -> {
                // Rendered by FullScreenScanner above.
            }
            Step.SEND_CONFIRM -> {
                DetailCard("Recipient wallet", state.peerRecipient)
                if (state.peerRecipientInput.isNotBlank() && state.peerRecipientInput.trim() != state.peerRecipient) {
                    DetailCard("Scanned (raw)", state.peerRecipientInput)
                    Text(
                        "The recipient above is parsed from the scanned code. If it doesn't match what you expect, re-scan — do not send.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandGreyMuted,
                    )
                }
                DetailCard("You send", "${state.peerAmount} USDC on Solana")
                DetailCard("Token mint", state.peerMint)
                Text("Your wallet pays the network fee and may also fund creation of the recipient's token account. This transfer is irreversible.")
                if (!state.walletAuthorized || !state.peerTransfersEnabled) StatusCard("Preview only", "No transfer has been prepared. Sending is unavailable for this wallet or release.")
                Button(onClick = onPeerSend, enabled = !state.busy && state.walletAuthorized && state.peerTransfersEnabled, modifier = Modifier.fillMaxWidth()) { Text("Validate in app and sign") }
                OutlinedButton(onClick = onSaveWallet, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Save recipient on this device") }
                OutlinedButton(onClick = onSendRail, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Edit") }
            }
            Step.SENDING -> {
                DetailCard("Recipient", state.peerRecipient)
                DetailCard("Amount", "${state.peerAmount} USDC")
                Text("The unsigned transaction is checked locally before your wallet is asked to sign.")
                if (!state.busy) {
                    Button(onClick = onPeerSend, modifier = Modifier.fillMaxWidth()) { Text("Retry with a fresh transaction") }
                    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
                }
            }
            Step.SEND_COMPLETE -> {
                DetailCard("Broadcast signature", state.peerSignature)
                Text("Broadcast is not the same as final confirmation. Check the signature in your wallet or an explorer before relying on settlement.")
                Button(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            }
            Step.FUNDING -> {
                val kycReady = state.merchantPayReady
                FundingMethodCard(
                    title = "Crypto transfer",
                    detail = "Receive USDC directly into your self-custody wallet. No verification needed.",
                    status = "Available",
                    icon = Icons.Filled.AccountBalanceWallet,
                    action = "Show receive code",
                    free = true,
                    onClick = onReceive,
                )
                FundingMethodCard(
                    title = "Debit / credit card",
                    detail = "Top up instantly with a card.",
                    status = if (kycReady) "Available soon" else "KYC required",
                    icon = Icons.Filled.CreditCard,
                    locked = !kycReady,
                    onClick = onKyc.takeIf { !kycReady },
                )
                FundingMethodCard(
                    title = "Bank transfer",
                    detail = "Fund from your bank account.",
                    status = if (kycReady) "Available soon" else "KYC required",
                    icon = Icons.Filled.AccountBalance,
                    locked = !kycReady,
                    onClick = onKyc.takeIf { !kycReady },
                )
                Text("A crypto transfer can still incur exchange withdrawal and network fees. WAYQO does not hold a stored balance.", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = onBack, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            }
            Step.RECEIVE -> ReceiveCard(state.wallet, BuildConfig.SOLANA_ALLOWED_MINTS) { onNotify("Wallet address copied") }
            Step.ACTIVITY -> {
                if (state.activities.isEmpty()) Text("No recorded payments yet.")
                state.activities.forEach { item -> ActivityCard(item, onRepeat) }
                OutlinedButton(onClick = onBack, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            }
            Step.SAVED -> {
                if (state.savedPayees.isEmpty()) Text("No saved payees yet. Verified recipients can be saved after scanning.")
                state.savedPayees.forEach { payee -> SavedPayeeCard(payee, onUseSaved, onDeleteSaved, onRenamePayee) }
                OutlinedButton(onClick = onBack, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            }
        }
      }
    }
    }
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
            title = { Text("Sign out of WAYQO?") },
            text = {
                Text("This clears the app session. Your encrypted self-custody wallet stays on this device and can be unlocked again. It does not delete funds or your Gaian verification record.")
            },
            confirmButton = {
                Button(onClick = { confirmSignOut = false; onSignOut() }) { Text("Sign out") }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") }
            },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            title = { Text("Delete your account data?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(color = Color(0xFFFDECEA), shape = RoundedCornerShape(10.dp)) {
                        Text(
                            "⚠ Transfer out any remaining funds first. Deleting your account does not move your money — send your USDC to another wallet or off-ramp it before continuing, or you may lose access to it.",
                            color = Color(0xFF8C1D18),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                    Text("This permanently deletes the account data Wayqo holds for you (profile, residency, verification status, and activity). Your self-custody wallet and funds stay on this device — remove the wallet or uninstall the app to erase them. On-chain transactions are public and cannot be deleted. This cannot be undone.")
                }
            },
            confirmButton = {
                Button(
                    onClick = { confirmDelete = false; onDeleteAccount() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8C1D18)),
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun CountryPickerField(selectedCode: String, onSelect: (String) -> Unit, label: String = "Search country of residence") {
    // Build the ~240-country list off the main thread so first composition never
    // blocks on ISU/ICU work; until it's ready, show a light placeholder.
    val countries by produceState(initialValue = emptyList<Country>()) {
        value = withContext(Dispatchers.Default) { RESIDENCY_COUNTRIES }
    }
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(selectedCode.isBlank()) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(selectedCode) {
        if (selectedCode.isBlank()) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    val selected = countries.firstOrNull { it.code.equals(selectedCode, ignoreCase = true) }
    val results = remember(query, countries) {
        val q = query.trim()
        if (q.isEmpty()) countries
        else countries.filter {
            it.english.contains(q, ignoreCase = true) ||
                it.native.contains(q, ignoreCase = true) ||
                it.code.equals(q, ignoreCase = true)
        }
    }

    Text(label, style = MaterialTheme.typography.labelMedium, color = BrandGreyMuted)
    OutlinedTextField(
        value = if (expanded) query else selected?.let { "${it.native}  ${it.callingCode}" }.orEmpty(),
        onValueChange = {
            query = it
            expanded = true
        },
        label = { Text(label) },
        placeholder = { Text("Country name or ISO code") },
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (selected != null && !expanded) {
                TextButton(onClick = {
                    query = ""
                    expanded = true
                }) { Text("Change") }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.isFocused) expanded = true }
    )
    if (expanded) {
        Spacer(Modifier.height(4.dp))
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (countries.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Loading countries…", color = BrandGreyMuted)
                }
            } else if (results.isEmpty()) {
                Text("No matching country", Modifier.padding(16.dp), color = BrandGreyMuted)
            } else {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 230.dp)) {
                    items(results) { c ->
                        val isSelected = c.code.equals(selectedCode, ignoreCase = true)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(c.code)
                                    query = ""
                                    expanded = false
                                    focusManager.clearFocus()
                                }
                                .background(if (isSelected) BrandLavender else Color.Transparent)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(c.flag, style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.width(12.dp))
                            Text(c.native, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
                            Text(c.callingCode, color = BrandGreyMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordConfirmationStatus(password: String, confirmation: String) {
    if (confirmation.isBlank()) return
    val matches = password == confirmation
    Text(
        if (matches) "Passwords match" else "Passwords do not match",
        style = MaterialTheme.typography.bodySmall,
        color = if (matches) BrandGreenInk else MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun StatusMessage(message: String) {
    val error = listOf("wrong", "invalid", "cannot", "missing", "failed", "disabled", "expired").any {
        message.contains(it, ignoreCase = true)
    }
    Surface(
        color = if (error) Color(0xFFFFEDEA) else BrandLavender,
        contentColor = if (error) Color(0xFF8C1D18) else BrandInk,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(message, modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ProfileScreen(
    state: AppState,
    onRename: (String) -> Unit,
    onKyc: () -> Unit,
    onRegister: () -> Unit,
    onRefresh: () -> Unit,
    onBackup: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onExportData: () -> Unit,
    onNotify: (String) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    var editingName by remember { mutableStateOf(false) }
    var draftName by remember(state.walletName) { mutableStateOf(state.walletName) }
    UnboundCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = BrandLavender, shape = RoundedCornerShape(50), modifier = Modifier.size(52.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = BrandInk)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(state.walletName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Self-custody account", color = BrandGreyMuted, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { editingName = !editingName }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit wallet name")
                }
            }
            AnimatedVisibility(editingName) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = draftName,
                        onValueChange = { draftName = it.take(40) },
                        label = { Text("Display name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onRename(draftName); editingName = false }) { Text("Save") }
                }
            }
        }
    }
    DetailCard("Wallet address", state.wallet)
    if (state.isEmbeddedWallet) DetailCard("Backup for this wallet", state.backupStatus)
    OutlinedButton(
        onClick = { clipboard.setText(AnnotatedString(state.wallet)); onNotify("Wallet address copied") },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Copy wallet address") }
    DetailCard("Country of residence", countryName(state.residencyCountry))
    DetailCard("Environment", "${state.environment.replaceFirstChar(Char::uppercase)} · ${state.paymentMode}")
    LegalNoticeButton()
    if (state.userId.isNotBlank()) {
        DetailCard("Provider account ID", state.userId)
        DetailCard("Identity status", state.kycStatus.ifBlank { "Not submitted" })
        KycAccessCard(state.kycStatus, state.lastStatusChecked, state.merchantPayReady, onKyc, onRefresh)
    } else {
        StatusCard("Payment profile", "Register to check identity and market access. You can scan and review a QR before sending is enabled.")
        Button(onClick = onRegister, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Register payment profile") }
    }
    if (state.isEmbeddedWallet) {
        OutlinedButton(onClick = onBackup, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Back up this wallet")
        }
    }
    if (!state.hardwareProtected) {
        Text("⚠ Add a device PIN or fingerprint to hardware-protect this wallet.",
            style = MaterialTheme.typography.labelSmall, color = Color(0xFFB3261E))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = { uriHandler.openUri("https://www.wayqo.app/privacy") }, modifier = Modifier.weight(1f)) {
            Text("Privacy")
        }
        OutlinedButton(onClick = { uriHandler.openUri("https://www.wayqo.app/terms/") }, modifier = Modifier.weight(1f)) {
            Text("Terms")
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onExportData, enabled = !state.busy, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("My data")
        }
        OutlinedButton(onClick = onSignOut, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8C1D18))) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Sign out")
        }
    }
    Text(
        "\"My data\" is a signed PDF (subject access request) whose authenticity can be verified independently. Sign out clears the session only; your wallet and funds stay on this device.",
        style = MaterialTheme.typography.labelSmall,
        color = BrandGreyMuted,
    )
    OutlinedButton(
        onClick = onDeleteAccount,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8C1D18)),
    ) {
        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Delete my account data")
    }
    Text(
        "Deletes the account data we hold. Your wallet and funds stay on this device; on-chain records are public and permanent; some records are kept where the law requires. WAYQO ${BuildConfig.VERSION_NAME}",
        style = MaterialTheme.typography.labelSmall,
        color = BrandGreyMuted,
    )
}

private fun screenTitle(step: Step): String = when (step) {
    Step.CONNECT -> "Money that moves with you"
    Step.HOME -> "Home"
    Step.PROFILE -> "Profile"
    Step.KYC -> "Verify your account"
    Step.BACKUP -> "Back up your wallet"
    Step.RESTORE -> "Restore your wallet"
    Step.PHRASE -> "Recovery words"
    Step.WALLETS -> "Wallets"
    Step.MANUAL -> "Unverified bank draft"
    Step.MARKET -> "Choose payout country"
    Step.SCAN -> "Scan payment QR"
    Step.CONFIRM -> "Payment"
    Step.QUOTE -> "Payment"
    Step.PAYING -> "Payment in progress"
    Step.COMPLETE -> "Payment receipt"
    Step.SEND, Step.SEND_SCAN -> "Send USDC"
    Step.SEND_CONFIRM -> "Review transfer"
    Step.SENDING -> "Transfer in progress"
    Step.SEND_COMPLETE -> "Transfer receipt"
    Step.FUNDING -> "Add funds"
    Step.RECEIVE -> "Receive USDC"
    Step.ACTIVITY -> "Activity"
    Step.SAVED -> "Saved payees"
}

private fun countryName(code: String): String = mapOf(
    "VN" to "Vietnam", "PH" to "Philippines", "TH" to "Thailand",
    "ID" to "Indonesia", "MY" to "Malaysia", "KH" to "Cambodia",
    "SG" to "Singapore", "BR" to "Brazil", "AR" to "Argentina",
    "PE" to "Peru", "BO" to "Bolivia", "CO" to "Colombia",
    "CN" to "Mainland China",
)[code.uppercase()] ?: countryByCode(code)?.english ?: code.uppercase()

// Gaian V2 explicitly permits QR parsing without a user only in Vietnam and the
// Philippines. Treat every other discovered corridor conservatively until its live
// contract says otherwise; quotes/orders still require identity in every market.
internal fun canParseQrBeforeKyc(country: String): Boolean =
    country.uppercase() in setOf("VN", "PH")

private fun railName(country: String, scheme: String): String = when (country.uppercase()) {
    "CN" -> "WeChat / Weixin business QR"
    "PE" -> "Yape or Plin"
    "BR" -> "Pix"
    "VN" -> "VietQR"
    "PH" -> "QR Ph"
    "TH" -> "PromptPay"
    "KH" -> "KHQR"
    "MY" -> "DuitNow"
    "ID" -> "QRIS"
    else -> scheme.ifBlank { "Provider-verified local QR" }
}

@Composable
private fun RowScope.ActionButton(label: String, icon: ImageVector, onClick: () -> Unit, enabled: Boolean) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandInk),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WalletSummaryCard(
    walletName: String,
    wallet: String,
    balanceUsd: String,
    balanceLoaded: Boolean,
    balanceHidden: Boolean,
    kycStatus: String,
    approvedCorridors: List<PaymentMarket>,
    paymentsEnabled: Boolean,
    hardwareProtected: Boolean,
    onKyc: () -> Unit,
    onRename: (String) -> Unit,
    onToggleBalance: () -> Unit,
) {
    var renaming by remember { mutableStateOf(false) }
    UnboundCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!hardwareProtected) {
                Text(
                    "⚠ Set a screen lock (fingerprint or PIN) to hardware-protect this wallet.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFB3261E),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (renaming) {
                    var draft by remember { mutableStateOf(walletName) }
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Save",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            onRename(draft.trim().ifBlank { walletName }); renaming = false
                        },
                    )
                } else {
                    Text(walletName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = { renaming = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Rename wallet", modifier = Modifier.size(18.dp))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (balanceHidden) {
                    Box(
                        Modifier.weight(1f).height(48.dp).clearAndSetSemantics {
                            contentDescription = "Balance hidden"
                        },
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        HiddenBalanceMosaic()
                    }
                } else {
                    val amount = balanceUsd.removePrefix("$").toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val formatted = formatUsdcBalance(amount)
                    val visible = if (!balanceLoaded) AnnotatedString("—") else buildAnnotatedString {
                        append(formatted.main)
                        if (formatted.raisedDigits.isNotBlank()) {
                            withStyle(SpanStyle(fontSize = 25.sp, baselineShift = BaselineShift(0.32f))) {
                                append(formatted.raisedDigits)
                            }
                        }
                    }
                    Text(
                        visible,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.weight(1f).clearAndSetSemantics {
                            contentDescription = if (!balanceLoaded) "USDC balance unavailable" else formatted.fullValue
                        },
                    )
                }
                IconButton(onClick = onToggleBalance) {
                    Icon(
                        if (balanceHidden) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (balanceHidden) "Show balance" else "Hide balance",
                        tint = BrandGreyMuted,
                    )
                }
            }
            Text(if (balanceLoaded) "Balance in USD · held as USDC" else "USDC balance unavailable · check wallet connection", style = MaterialTheme.typography.labelSmall, color = BrandGreyMuted)
            Text("${wallet.take(8)}…${wallet.takeLast(8)}", style = MaterialTheme.typography.bodyMedium, color = BrandGreyMuted)
            Text("Self-custody · Solana · WAYQO cannot move funds without your wallet signature", style = MaterialTheme.typography.labelSmall, color = BrandGreyMuted)
            Spacer(Modifier.height(2.dp))
            if (approvedCorridors.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(16.dp))
                    Text("Active", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = BrandGreenInk)
                }
                Spacer(Modifier.height(6.dp))
                val ctx = LocalContext.current
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    approvedCorridors.forEach { market ->
                        val brand = paymentRailBrand(market.country, market.scheme)
                        val logoId = remember(brand.asset) {
                            if (brand.asset.isBlank()) 0
                            else ctx.resources.getIdentifier(brand.asset, "drawable", ctx.packageName)
                        }
                        if (logoId != 0) {
                            // Official rail logo on a white chip so transparent/dark marks show.
                            Surface(color = Color.White, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, BrandBorder)) {
                                Image(
                                    painter = painterResource(logoId),
                                    contentDescription = brand.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.height(28.dp).widthIn(min = 36.dp, max = 110.dp).padding(horizontal = 8.dp, vertical = 5.dp),
                                )
                            }
                        } else {
                            Surface(color = brand.color, shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    brand.mark,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
            } else if (paymentsEnabled) {
                val normalized = kycStatus.trim().uppercase()
                val pending = normalized in setOf("PENDING", "SUBMITTED", "IN_REVIEW", "PROCESSING")
                Surface(
                    color = BrandLavender,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onKyc() },
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (pending) "Identity verification is under review" else "Verify your identity to unlock local QR payments",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = BrandInk,
                            )
                            Text(
                                if (pending) "Provider status: $kycStatus" else "Scan and Receive work without KYC; sending follows access controls.",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandGreyMuted,
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = BrandInk)
                    }
                }
            }
        }
    }
}

@Composable
private fun KycAccessCard(kycStatus: String, lastChecked: String, marketReady: Boolean, onOpen: () -> Unit, onRefresh: () -> Unit) {
    val normalized = kycStatus.trim().uppercase()
    val approved = normalized in setOf("APPROVED", "VERIFIED", "ACTIVE", "COMPLETED")
    val pending = normalized in setOf("PENDING", "SUBMITTED", "IN_REVIEW", "PROCESSING")
    val rejected = normalized in setOf("REJECTED", "DECLINED", "FAILED", "EXPIRED")
    val title = when {
        approved && marketReady -> "Identity verified"
        approved -> "Identity verified; corridor access is pending"
        pending -> "Verification submitted"
        rejected -> "Verification needs attention"
        else -> "Identity verification required"
    }
    val detail = when {
        approved && marketReady -> "Your provider verification is approved and local QR payment access is active."
        approved -> "Gaian has verified your identity, but no local QR corridor is active yet. Refresh to check market approval."
        pending -> "Gaian is reviewing your information. You can leave this screen; local QR payments unlock after approval."
        rejected -> "The provider did not approve the submission. Open verification to review its instructions or submit corrected information."
        else -> "Complete the provider-hosted check in your browser. Scanning and Receive remain available without KYC."
    }
    UnboundCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall)
            Text(
                "Provider status: ${kycStatus.ifBlank { "Not submitted" }}",
                style = MaterialTheme.typography.labelMedium,
                color = BrandGreyMuted,
            )
            if (lastChecked.isNotBlank()) {
                Text("Last checked $lastChecked", style = MaterialTheme.typography.labelSmall, color = BrandGreyMuted)
            }
            if (!approved) {
                Button(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
                    Text(if (pending) "Continue verification" else "Open secure verification")
                }
            }
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Text("Check status now")
            }
        }
    }
}

@Composable
private fun StatusCard(title: String, detail: String) {
    UnboundCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FundingMethodCard(
    title: String,
    detail: String,
    status: String,
    icon: ImageVector,
    action: String = "",
    locked: Boolean = false,
    free: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val cardModifier = if (locked && onClick != null) {
        Modifier.fillMaxWidth().clickable { onClick() }
    } else {
        Modifier.fillMaxWidth()
    }
    UnboundCard(cardModifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = if (locked) BrandGreyMuted else BrandInk, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (locked) BrandGreyMuted else BrandInk,
                    )
                    Text(
                        status,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (locked) BrandGreyMuted else MaterialTheme.colorScheme.primary,
                    )
                }
                if (free) {
                    Surface(color = BrandGreenSoft, shape = RoundedCornerShape(50)) {
                        Text(
                            "Free",
                            color = BrandGreenInk,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        )
                    }
                }
            }
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = if (locked) BrandGreyMuted else BrandInk)
            if (locked) {
                Text("🔒 Complete identity verification to enable — tap to verify.", style = MaterialTheme.typography.labelSmall, color = BrandGreyMuted)
            } else if (onClick != null && action.isNotBlank()) {
                Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(action) }
            }
        }
    }
}

@Composable
private fun ReceiveCard(wallet: String, allowedMints: String, onCopied: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val mint = allowedMints.split(',').map(String::trim).firstOrNull(String::isNotBlank)
    // Encode a Solana Pay request scoped to USDC so compatible senders auto-select the
    // token (not SOL). Falls back to a bare address when no mint is configured.
    val qrContent = if (mint != null) "solana:$wallet?spl-token=$mint" else "solana:$wallet"
    val bitmap = remember(qrContent) { qrBitmap(qrContent) }
    val mintLabel = mint ?: "No mint configured in this build"
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val qrSize = if (maxHeight < 620.dp) 184.dp else 220.dp
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Image(
                bitmap.asImageBitmap(),
                contentDescription = "Solana receive QR",
                modifier = Modifier.size(qrSize),
            )
            UnboundCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Your Solana wallet", style = MaterialTheme.typography.labelMedium, color = BrandGreyMuted)
                    Text(wallet, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = BrandLavender, shape = RoundedCornerShape(20.dp)) {
                            Text("Solana", Modifier.padding(horizontal = 12.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium)
                        }
                        Surface(color = BrandLavender, shape = RoundedCornerShape(20.dp)) {
                            Text("USDC", Modifier.padding(horizontal = 12.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Text("Mint: $mintLabel", style = MaterialTheme.typography.labelSmall, color = BrandGreyMuted)
                }
            }
            Text(
                "Only send USDC on Solana to this address. This is a wallet address, not a payment request; exchange or network withdrawal fees may apply.",
                style = MaterialTheme.typography.bodySmall,
                color = BrandGreyMuted,
            )
            Button(
                onClick = { clipboard.setText(AnnotatedString(wallet)); onCopied() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Copy wallet address") }
            OutlinedButton(
                onClick = { shareQr(context, qrContent, "My Wayqo Solana address", "wayqo-receive-qr.png") },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save / share QR") }
        }
    }
}

@Composable
private fun ActivityCard(item: ActivityItem, onRepeat: (ActivityItem) -> Unit) {
    UnboundCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (item.kind == "onchain_transfer") "USDC transfer" else "Local QR payment", fontWeight = FontWeight.SemiBold)
            Text(item.status.ifBlank { "unknown" }, color = BrandGreyMuted)
            if (item.recipient.isNotBlank()) Text("To: ${item.recipient}")
            if (item.fiatAmount > 0) Text("Recipient: ${item.fiatAmount} ${item.fiatCurrency}")
            if (item.settlementAmount.isNotBlank()) Text("Wallet: ${item.settlementAmount} ${displayAsset(item.settlementCurrency)}")
            if (item.exchangeRate.isNotBlank()) Text("Rate: ${item.exchangeRate}")
            if (item.protocolFeeUsd.isNotBlank() || item.minimumFeeUsd.isNotBlank()) {
                Text("Provider fee: ${item.protocolFeeUsd.ifBlank { "—" }} USD · minimum ${item.minimumFeeUsd.ifBlank { "—" }} USD")
            }
            if (item.transactionHash.isNotBlank()) Text("Transaction: ${item.transactionHash}")
            Text(item.createdAt, style = MaterialTheme.typography.labelSmall, color = BrandGreyMuted)
            if (item.repeatable || item.kind == "merchant_payment") {
                OutlinedButton(onClick = { onRepeat(item) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (item.kind == "onchain_transfer") "Send again" else "Scan merchant again")
                }
            }
        }
    }
}

@Composable
private fun SavedPayeeCard(
    payee: SavedPayee,
    onUse: (SavedPayee) -> Unit,
    onDelete: (SavedPayee) -> Unit,
    onRename: (SavedPayee, String) -> Unit,
) {
    var editing by remember(payee.id) { mutableStateOf(false) }
    var noteText by remember(payee.id) { mutableStateOf(payee.note) }
    UnboundCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(payee.label.ifBlank { "Saved payee" }, fontWeight = FontWeight.SemiBold)
                if (payee.note.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Surface(color = BrandLavender, shape = RoundedCornerShape(50)) {
                        Text(payee.note, color = BrandPurple, style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { noteText = payee.note; editing = true }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Add or edit note", tint = BrandPurple)
                }
            }
            Text(when (payee.kind) {
                "wallet" -> "Solana wallet"
                "merchant_reference" -> "Recipient reference · fresh QR required"
                else -> "Static merchant QR"
            }, color = BrandGreyMuted)
            Text(payee.destination)
            Button(onClick = { onUse(payee) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (payee.kind == "merchant_reference") "Scan fresh QR" else "Use with fresh payment")
            }
            OutlinedButton(onClick = { onDelete(payee) }, modifier = Modifier.fillMaxWidth()) { Text("Delete") }
        }
    }
    if (editing) {
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Add a note") },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it.take(40) },
                    label = { Text("e.g. RENT, LANDLORD, GYM") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = { TextButton(onClick = { onRename(payee, noteText.trim()); editing = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel") } },
        )
    }
}

private fun displayAsset(value: String): String = if (value == solanaUsdcMint()) "USDC on Solana" else value

private fun solanaUsdcMint(): String = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"

@Composable
private fun FullScreenScanner(
    sessionId: Long,
    priorFailures: Int,
    onQr: (String) -> Unit,
    onSharedImageConsumed: () -> Unit,
    prompt: String,
    permissionLabel: String,
    onCancel: () -> Unit,
    onManual: (() -> Unit)?,
    sharedImage: String,
    onSavedPayees: (() -> Unit)? = null,
    hasSavedPayees: Boolean = false,
    skipPayload: String = "",
    notice: String = "",
    rejectionReason: String = "",
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var allowed by remember(sessionId) {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var importMessage by remember(sessionId) { mutableStateOf("") }
    var lockedPayload by remember(sessionId) { mutableStateOf<String?>(null) }
    var importing by remember(sessionId) { mutableStateOf(false) }
    var failures by remember(sessionId) { mutableStateOf(priorFailures) }
    var showHelp by remember(sessionId) { mutableStateOf(false) }
    var retryRejected by remember(sessionId) { mutableStateOf(false) }
    var scanError by remember(sessionId) { mutableStateOf(rejectionReason) }
    val importAttempts = remember(sessionId) { AsyncAttemptGate() }
    val scope = rememberCoroutineScope()
    val feedbackView = LocalView.current
    LaunchedEffect(sessionId, scanError) {
        if (scanError.isNotBlank()) feedbackView.scanFeedback(accepted = false)
    }
    val lockOn: (String, Long?) -> Unit = { payload, attempt ->
        if (scanError.isBlank() && (attempt == null || importAttempts.isCurrent(attempt)) && lockedPayload == null) {
            lockedPayload = payload
            importMessage = "QR read"
            scope.launch {
                delay(120)
                // An image decode can finish after the user has already selected a
                // different image. Submit only the payload that still owns the screen.
                if (lockedPayload == payload && (attempt == null || importAttempts.isCurrent(attempt))) {
                    onQr(payload)
                    // A rejected or deferred QR can leave the scanner visible. Release
                    // its one-shot latch so a different code can still be read.
                    delay(1_500)
                    if (lockedPayload == payload) {
                        lockedPayload = null
                    }
                }
            }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = it }
    val imageScanner = remember { newQrImageDecoder() }
    DisposableEffect(Unit) { onDispose { imageScanner.close() } }
    LaunchedEffect(sessionId, sharedImage) {
        if (sharedImage.isNotBlank()) {
            importing = true
            runCatching { decodeImportedQr(context, Uri.parse(sharedImage), imageScanner) }
                .onSuccess { values ->
                    importing = false
                    if (values.size == 1) lockOn(values.single(), null)
                    else { failures++; scanError = if (values.isEmpty()) "No readable QR code was found in this shared image." else "This shared image contains several QR codes. Crop it to one payment QR." }
                }
                .onFailure { importing = false; failures++; scanError = "The shared image could not be read. Choose a clearer screenshot." }
            onSharedImageConsumed()
        }
    }
    LaunchedEffect(sessionId, allowed, lockedPayload, importing, failures, scanError, showHelp) {
        if (allowed && lockedPayload == null && !importing && !showHelp && failures < 3 && scanError.isBlank()) {
            delay(15_000)
            if (lockedPayload == null && !importing) {
                failures++
                scanError = "No QR could be read yet. Check lighting and distance, or choose a clearer image."
            }
        }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) {
            importing = false
            return@rememberLauncherForActivityResult
        }
        importing = true
        lockedPayload = null
        importMessage = "Reading image…"
        scope.launch {
            try {
                val payloads = decodeImportedQr(context, uri, imageScanner)
                when {
                    payloads.size == 1 -> lockOn(payloads.single(), null)
                    payloads.isEmpty() -> { failures++; scanError = "No readable QR code was found in that image. Choose a clearer screenshot." }
                    else -> { failures++; scanError = "This image contains several QR codes. Crop it to one payment QR." }
                }
            } catch (_: Exception) {
                failures++
                scanError = "That image could not be read. Choose a clearer screenshot."
            } finally {
                // Always clear the busy flag so the button can never get stuck disabled.
                importing = false
            }
        }
    }

    Box(modifier.background(Color.Black)) {
        if (allowed) {
            // Keep the live CameraX preview mounted across errors. Only delivery
            // pauses; the latch rearms when scanning resumes, without lens rebinding.
            QrScanner(skip = if (retryRejected) "" else skipPayload,
                enabled = !importing && !showHelp && scanError.isBlank() && lockedPayload == null) { payload -> lockOn(payload, null) }
            UnboundScanTarget(
                locked = lockedPayload != null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-56).dp)
                    .fillMaxWidth(0.72f)
                    .aspectRatio(1f),
            )
            AnimatedVisibility(
                visible = lockedPayload != null,
                modifier = Modifier.align(Alignment.Center).offset(y = (-56).dp),
            ) {
                Surface(color = BrandGreen, shape = RoundedCornerShape(24.dp)) {
                    Text(
                        "QR read",
                        color = BrandInk,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    )
                }
            }
        } else {
            Column(
                Modifier.align(Alignment.Center).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp))
                Text("Camera access is needed only while scanning a QR code.", color = Color.White)
                Button(onClick = { permission.launch(Manifest.permission.CAMERA) }) { Text(permissionLabel) }
            }
        }
        Image(
            painter = painterResource(R.drawable.wayqo_logo_scanner),
            contentDescription = "WAYQO",
            contentScale = ContentScale.Fit,
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(0.90f).height(84.dp),
        )
        IconButton(onClick = onCancel, modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 86.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Cancel scanning", tint = Color.White)
        }
        Text(
            "Position the QR code in the frame. Check lighting, hold steady, and adjust distance.",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 140.dp),
        )
        Surface(
            color = Color.Black.copy(alpha = 0.72f),
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(prompt, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.92f))
                if (notice.isNotBlank() && scanError.isBlank()) Text(notice, style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.92f), maxLines = 3)
                if (importMessage.isNotBlank()) Text(importMessage, style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { scanError = ""; importing = true; imagePicker.launch("image/*") },
                        enabled = !importing && lockedPayload == null,
                        border = BorderStroke(1.dp, Color.White),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (importing) "Reading…" else "Choose image")
                    }
                    if (onSavedPayees != null && hasSavedPayees) {
                        OutlinedButton(
                            onClick = onSavedPayees,
                            enabled = !importing,
                            border = BorderStroke(1.dp, Color.White),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.Bookmarks, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Saved payees")
                        }
                    }
                }
                TextButton(onClick = { showHelp = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("I need help scanning", color = Color(0xFFD5C6FF))
                }
                if (failures >= 3) {
                    Text("Three scans failed. You can retry, choose another image, or keep an unverified bank-details draft.", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    val helpPurple = Color(0xFFD5C6FF)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { failures = 0; scanError = ""; lockedPayload = null; retryRejected = true; importMessage = "Try holding the camera steady." },
                            border = BorderStroke(1.dp, helpPurple),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = helpPurple),
                        ) { Text("Retry camera") }
                        if (onManual != null) OutlinedButton(
                            onClick = onManual,
                            border = BorderStroke(1.dp, helpPurple),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = helpPurple),
                        ) { Text("Manual draft") }
                    }
                }
            }
        }
        if (showHelp) {
            AlertDialog(
                onDismissRequest = { showHelp = false },
                title = { Text("Help with scanning") },
                text = { Text("Hold the QR inside the brackets, keep the phone steady, and move closer or farther until it is sharp. You can choose a screenshot from your gallery. After three unsuccessful tries, an unverified bank-details draft is available.") },
                confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Try again") } },
            )
        }
        if (scanError.isNotBlank()) {
            ScannerErrorOverlay(
                reason = scanError,
                onResume = { scanError = ""; importMessage = "Point at a different payment QR."; lockedPayload = null },
                modifier = Modifier.align(Alignment.Center).offset(y = (-56).dp)
                    .fillMaxWidth(0.72f).aspectRatio(1f).padding(12.dp),
            )
        }
    }
}

private suspend fun decodeImportedQr(context: Context, uri: Uri, scanner: QrImageDecoder): List<String> =
    scanner.decode(context, uri)

internal fun decodeUploadBitmap(context: Context, uri: Uri): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / sample > 2_048 || bounds.outHeight / sample > 2_048) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}

@Composable
private fun UnboundScanTarget(locked: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "Unbound scanner")
    val progress by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "Scan line",
    )
    val lockProgress by animateFloatAsState(
        targetValue = if (locked) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "QR lock-on",
    )
    Canvas(modifier) {
        val stroke = 5.dp.toPx()
        val corner = size.minDimension * 0.18f
        val radius = 18.dp.toPx()
        val green = BrandGreen
        val white = Color.White.copy(alpha = 0.96f)
        if (lockProgress > 0f) {
            drawRoundRect(
                color = green.copy(alpha = 0.18f * lockProgress),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
            )
        }
        val topLeft = if (locked) green else white
        val topRight = if (locked) green else white
        val bottomLeft = if (locked) green else white
        val bottomRight = if (locked) green else white

        // Four capture brackets frame the QR without claiming a successful payment.
        drawLine(topLeft, androidx.compose.ui.geometry.Offset(radius, 0f), androidx.compose.ui.geometry.Offset(corner, 0f), stroke, StrokeCap.Round)
        drawLine(topLeft, androidx.compose.ui.geometry.Offset(0f, radius), androidx.compose.ui.geometry.Offset(0f, corner), stroke, StrokeCap.Round)
        drawLine(topRight, androidx.compose.ui.geometry.Offset(size.width - corner, 0f), androidx.compose.ui.geometry.Offset(size.width - radius, 0f), stroke, StrokeCap.Round)
        drawLine(topRight, androidx.compose.ui.geometry.Offset(size.width, radius), androidx.compose.ui.geometry.Offset(size.width, corner), stroke, StrokeCap.Round)
        drawLine(bottomLeft, androidx.compose.ui.geometry.Offset(0f, size.height - corner), androidx.compose.ui.geometry.Offset(0f, size.height - radius), stroke, StrokeCap.Round)
        drawLine(bottomLeft, androidx.compose.ui.geometry.Offset(radius, size.height), androidx.compose.ui.geometry.Offset(corner, size.height), stroke, StrokeCap.Round)
        drawLine(bottomRight, androidx.compose.ui.geometry.Offset(size.width - corner, size.height), androidx.compose.ui.geometry.Offset(size.width - radius, size.height), stroke, StrokeCap.Round)
        drawLine(bottomRight, androidx.compose.ui.geometry.Offset(size.width, size.height - corner), androidx.compose.ui.geometry.Offset(size.width, size.height - radius), stroke, StrokeCap.Round)

        val y = size.height * progress
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(
                    Color.Transparent,
                    BrandPurple.copy(alpha = 1f - lockProgress),
                    Color.White.copy(alpha = 1f - lockProgress),
                    BrandPurple.copy(alpha = 1f - lockProgress),
                    Color.Transparent,
                )
            ),
            start = androidx.compose.ui.geometry.Offset(12.dp.toPx(), y),
            end = androidx.compose.ui.geometry.Offset(size.width - 12.dp.toPx(), y),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )
        if (lockProgress > 0f) {
            val checkStroke = 8.dp.toPx()
            drawLine(
                green.copy(alpha = lockProgress),
                androidx.compose.ui.geometry.Offset(size.width * 0.38f, size.height * 0.52f),
                androidx.compose.ui.geometry.Offset(size.width * 0.47f, size.height * 0.61f),
                checkStroke,
                StrokeCap.Round,
            )
            drawLine(
                green.copy(alpha = lockProgress),
                androidx.compose.ui.geometry.Offset(size.width * 0.47f, size.height * 0.61f),
                androidx.compose.ui.geometry.Offset(size.width * 0.65f, size.height * 0.40f),
                checkStroke,
                StrokeCap.Round,
            )
        }
    }
}

@Composable
internal fun RecipientConfirmationCard(
    parsed: ParsedQr,
    country: String,
    network: String,
    fallbackScheme: String,
) {
    val brand = paymentRailBrand(country, parsed.scheme.ifBlank { fallbackScheme }, network)
    val institution = financialInstitution(country, parsed.bankBin)
    val bankLogo by produceState<Bitmap?>(initialValue = null, key1 = institution?.logoUrl) {
        value = institution?.logoUrl?.let { loadTrustedInstitutionLogo(it) }
    }
    val institutionLabel = when {
        institution != null -> institution.shortName
        parsed.bankBin.isNotBlank() -> "Bank / institution"
        else -> "Payment institution"
    }
    val institutionDetail = institution?.name
        ?: parsed.bankBin.takeIf { it.isNotBlank() }?.let { "Participant ID $it" }
        ?: "Not supplied by this payment rail"

    UnboundCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = brand.color,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.height(42.dp).widthIn(min = 62.dp),
                ) {
                    Box(Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                        Text(brand.mark, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(brand.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(countryName(country), style = MaterialTheme.typography.labelSmall, color = BrandGreyMuted)
                }
                Surface(color = BrandGreen.copy(alpha = 0.22f), shape = RoundedCornerShape(50)) {
                    Text(
                        "✓ Verified",
                        color = Color(0xFF176B2A),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            HorizontalDivider(color = BrandBorder)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("RECIPIENT", style = MaterialTheme.typography.labelSmall, color = BrandGreyMuted)
                Text(
                    parsed.merchant.ifBlank { "Merchant account" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = BrandLavender,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(46.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (bankLogo != null) {
                            Image(
                                bitmap = bankLogo!!.asImageBitmap(),
                                contentDescription = institution?.shortName,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.padding(7.dp),
                            )
                        } else {
                            Text(
                                institution?.shortName?.take(2)?.uppercase()
                                    ?: parsed.bankBin.takeLast(2).ifBlank { brand.mark.take(2).uppercase() },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = BrandInk,
                            )
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(institutionLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(institutionDetail, style = MaterialTheme.typography.labelSmall, color = BrandGreyMuted, maxLines = 2)
                }
            }

            ReceiptDetailRow("Account", parsed.account.ifBlank { "Not supplied" })
            ReceiptDetailRow("QR type", if (parsed.dynamic) "Dynamic · single payment" else "Static · reusable")
        }
    }
}

@Composable
private fun ReceiptDetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = BrandGreyMuted, modifier = Modifier.width(84.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
    }
}

internal suspend fun loadTrustedInstitutionLogo(url: String): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val parsed = URL(url)
        require(parsed.protocol == "https")
        require(parsed.host in setOf("cdn.vietqr.io", "api.vietqr.io", "vietqr.net"))
        val connection = parsed.openConnection() as HttpURLConnection
        connection.connectTimeout = 2_500
        connection.readTimeout = 2_500
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Accept", "image/png,image/*")
        try {
            require(connection.responseCode == HttpURLConnection.HTTP_OK)
            connection.inputStream.use(BitmapFactory::decodeStream)
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
}

@Composable
private fun DetailCard(label: String, value: String) {
    UnboundCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = BrandGreyMuted)
            Spacer(Modifier.height(4.dp))
            Text(value.ifBlank { "—" }, fontWeight = FontWeight.SemiBold)
        }
    }
}
