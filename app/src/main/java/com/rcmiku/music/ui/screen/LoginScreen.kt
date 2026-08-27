package com.rcmiku.music.ui.screen

import android.annotation.SuppressLint
import android.os.Build
import android.webkit.CookieManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.rememberAsyncImagePainter
import com.rcmiku.music.R
import com.rcmiku.music.constants.ncmCookieKey
import com.rcmiku.music.utils.getDeviceID
import com.rcmiku.music.viewModel.LoginUiState
import com.rcmiku.music.viewModel.LoginViewModel
import com.rcmiku.music.utils.rememberPreference
import com.rcmiku.ncmapi.utils.CookieKeys
import com.rcmiku.ncmapi.utils.CookieProvider
import com.rcmiku.ncmapi.utils.json
import com.rcmiku.ncmapi.utils.parseCookieString

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    val captcha by viewModel.captcha.collectAsState()
    val qrImageUrl by viewModel.qrImageUrl.collectAsState()
    val countdown by viewModel.countdown.collectAsState()
    val selectedCountryCode by viewModel.selectedCountryCode.collectAsState()

    var ncmCookie by rememberPreference(ncmCookieKey, "")
    var expanded by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is LoginUiState.Success -> {
                if (state.cookie.isNotEmpty()) {
                    ncmCookie = state.cookie
                }
                viewModel.resetState()
                navController.navigateUp()
            }
            is LoginUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearError()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.login)) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.resetState()
                            navController.navigateUp()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
        Box(
            Modifier
                .padding(top = padding.calculateTopPadding())
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = stringResource(R.string.login),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.login_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        OutlinedTextField(
                            value = "${selectedCountryCode.name} ${selectedCountryCode.dialCode}",
                            onValueChange = {},
                            label = { Text(stringResource(R.string.country_code)) },
                            readOnly = true,
                            enabled = true,
                            modifier = Modifier.width(140.dp),
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.clickable { expanded = !expanded }
                                )
                            }
                        )

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            LoginViewModel.countryCodes.forEach { countryCode ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${countryCode.name} ${countryCode.dialCode}")
                                    },
                                    onClick = {
                                        viewModel.onCountryCodeChange(countryCode)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { viewModel.onPhoneNumberChange(it) },
                        label = { Text(stringResource(R.string.phone_number)) },
                        placeholder = { Text(stringResource(R.string.phone_number_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        enabled = uiState !is LoginUiState.Loading
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = captcha,
                        onValueChange = { viewModel.onCaptchaChange(it) },
                        label = { Text(stringResource(R.string.captcha)) },
                        placeholder = { Text(stringResource(R.string.captcha_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        enabled = uiState !is LoginUiState.Loading
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = { viewModel.sendCaptcha() },
                        enabled = uiState !is LoginUiState.Loading && countdown == 0,
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text(
                            text = if (countdown > 0) "${countdown}s" else stringResource(R.string.get_captcha)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.loginWithCaptcha() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState !is LoginUiState.Loading && captcha.isNotEmpty()
                ) {
                    if (uiState is LoginUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(R.string.login))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .padding(horizontal = 8.dp)
                    )

                    Text(
                        text = stringResource(R.string.or),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .padding(horizontal = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = { viewModel.startQrLogin() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState !is LoginUiState.Loading
                ) {
                    Text(stringResource(R.string.qr_code_login))
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (uiState is LoginUiState.QrLogin && qrImageUrl.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.scan_qr_code),
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Image(
                                painter = rememberAsyncImagePainter(qrImageUrl),
                                contentDescription = stringResource(R.string.qr_code),
                                modifier = Modifier.size(200.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = stringResource(R.string.scan_qr_code_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

private fun CookieManager.collectLoginCookies(currentUrl: String): MutableMap<String, String> {
    flush()
    val cookieUrls = linkedSetOf(
        "https://music.163.com",
        "https://y.music.163.com",
        currentUrl
    )
    return buildMap {
        cookieUrls.forEach { url ->
            getCookie(url)?.let { putAll(parseCookieString(it)) }
        }
    }.toMutableMap()
}

private fun MutableMap<String, String>.addDeviceInfo() {
    this[CookieKeys.DEVICE_ID] = getDeviceID()
    this[CookieKeys.OS_VER] = Build.VERSION.RELEASE
    this[CookieKeys.MOBILE_NAME] = Build.MODEL
}
