package com.rcmiku.music.viewModel

import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcmiku.music.utils.getDeviceID
import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.utils.CookieKeys
import com.rcmiku.ncmapi.utils.json
import com.rcmiku.ncmapi.utils.parseCookieString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CountryCode(
    val code: String,
    val name: String,
    val dialCode: String
)

sealed class LoginUiState {
    data object Idle : LoginUiState()
    data object Loading : LoginUiState()
    data object PhoneLogin : LoginUiState()
    data object QrLogin : LoginUiState()
    data class Success(val cookie: String) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

@HiltViewModel
class LoginViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _captcha = MutableStateFlow("")
    val captcha: StateFlow<String> = _captcha.asStateFlow()

    private val _qrImageUrl = MutableStateFlow("")
    val qrImageUrl: StateFlow<String> = _qrImageUrl.asStateFlow()

    private val _qrKey = MutableStateFlow("")
    private val _countdown = MutableStateFlow(0)
    val countdown: StateFlow<Int> = _countdown.asStateFlow()

    private val _selectedCountryCode = MutableStateFlow(countryCodes.first())
    val selectedCountryCode: StateFlow<CountryCode> = _selectedCountryCode.asStateFlow()

    private var qrCheckJob: Job? = null
    private var countdownJob: Job? = null

    fun onPhoneNumberChange(phone: String) {
        _phoneNumber.value = phone
    }

    fun onCaptchaChange(code: String) {
        _captcha.value = code
    }

    fun onCountryCodeChange(countryCode: CountryCode) {
        _selectedCountryCode.value = countryCode
    }

    fun sendCaptcha() {
        val phone = _phoneNumber.value
        if (phone.isEmpty()) {
            _uiState.value = LoginUiState.Error("请输入手机号")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = AccountApi.sentCaptcha(phone, _selectedCountryCode.value.dialCode.replace("+", ""))
            if (result.isSuccess) {
                _uiState.value = LoginUiState.PhoneLogin
                startCountdown()
            } else {
                _uiState.value = LoginUiState.Error(result.exceptionOrNull()?.message ?: "发送验证码失败")
            }
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            _countdown.value = 60
            while (_countdown.value > 0) {
                delay(1000)
                _countdown.value -= 1
            }
        }
    }

    fun loginWithCaptcha() {
        val phone = _phoneNumber.value
        val captcha = _captcha.value

        if (phone.isEmpty()) {
            _uiState.value = LoginUiState.Error("请输入手机号")
            return
        }
        if (captcha.isEmpty()) {
            _uiState.value = LoginUiState.Error("请输入验证码")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = AccountApi.loginCellphoneWithCookie(phone, captcha, _selectedCountryCode.value.dialCode.replace("+", ""))
            if (result.isSuccess) {
                val rawCookie = result.getOrNull()?.cookie ?: ""
                if (rawCookie.isNotEmpty()) {
                    val cookieMap = parseCookieString(rawCookie).toMutableMap()
                    cookieMap[CookieKeys.DEVICE_ID] = getDeviceID()
                    cookieMap[CookieKeys.OS_VER] = Build.VERSION.RELEASE
                    cookieMap[CookieKeys.MOBILE_NAME] = Build.MODEL
                    val cookieJson = json.encodeToString(cookieMap)
                    _uiState.value = LoginUiState.Success(cookieJson)
                } else {
                    _uiState.value = LoginUiState.Error("登录失败，未获取到cookie")
                }
            } else {
                _uiState.value = LoginUiState.Error(result.exceptionOrNull()?.message ?: "登录失败")
            }
        }
    }

    fun startQrLogin() {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val keyResult = AccountApi.qrKey()
            if (keyResult.isSuccess) {
                val key = keyResult.getOrNull()?.data?.unikey ?: ""
                if (key.isNotEmpty()) {
                    _qrKey.value = key
                    val createResult = AccountApi.qrCreate(key)
                    if (createResult.isSuccess) {
                        _qrImageUrl.value = createResult.getOrNull()?.data?.qrimg ?: ""
                        _uiState.value = LoginUiState.QrLogin
                        startQrCheck(key)
                    } else {
                        _uiState.value = LoginUiState.Error("生成二维码失败")
                    }
                } else {
                    _uiState.value = LoginUiState.Error("获取二维码key失败")
                }
            } else {
                _uiState.value = LoginUiState.Error(keyResult.exceptionOrNull()?.message ?: "获取二维码key失败")
            }
        }
    }

    private fun startQrCheck(key: String) {
        qrCheckJob?.cancel()
        qrCheckJob = viewModelScope.launch {
            while (true) {
                delay(3000)
                val result = AccountApi.qrCheck(key)
                if (result.isSuccess) {
                    val response = result.getOrNull()
                    when (response?.code) {
                        803 -> {
                            val rawCookie = response.cookie ?: ""
                            if (rawCookie.isNotEmpty()) {
                                val cookieMap = parseCookieString(rawCookie).toMutableMap()
                                cookieMap[CookieKeys.DEVICE_ID] = getDeviceID()
                                cookieMap[CookieKeys.OS_VER] = Build.VERSION.RELEASE
                                cookieMap[CookieKeys.MOBILE_NAME] = Build.MODEL
                                val cookieJson = json.encodeToString(cookieMap)
                                _uiState.value = LoginUiState.Success(cookieJson)
                            }
                            break
                        }
                        800 -> {
                            _uiState.value = LoginUiState.Error("二维码已过期，请重新获取")
                            break
                        }
                        801 -> {
                            // Waiting for scan
                        }
                        802 -> {
                            // Scanned, waiting for confirm
                        }
                    }
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
        _phoneNumber.value = ""
        _captcha.value = ""
        _qrImageUrl.value = ""
        qrCheckJob?.cancel()
        countdownJob?.cancel()
    }

    fun clearError() {
        _uiState.value = LoginUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        qrCheckJob?.cancel()
        countdownJob?.cancel()
    }

    companion object {
        val countryCodes = listOf(
            CountryCode("CN", "中国", "+86"),
            CountryCode("HK", "中国香港", "+852"),
            CountryCode("MO", "中国澳门", "+853"),
            CountryCode("TW", "中国台湾", "+886"),
            CountryCode("US", "美国", "+1"),
            CountryCode("JP", "日本", "+81"),
            CountryCode("KR", "韩国", "+82"),
            CountryCode("GB", "英国", "+44"),
            CountryCode("DE", "德国", "+49"),
            CountryCode("FR", "法国", "+33"),
            CountryCode("AU", "澳大利亚", "+61"),
            CountryCode("CA", "加拿大", "+1"),
            CountryCode("SG", "新加坡", "+65"),
            CountryCode("MY", "马来西亚", "+60"),
            CountryCode("TH", "泰国", "+66"),
            CountryCode("VN", "越南", "+84"),
            CountryCode("PH", "菲律宾", "+63"),
            CountryCode("ID", "印度尼西亚", "+62"),
            CountryCode("IN", "印度", "+91"),
            CountryCode("RU", "俄罗斯", "+7"),
            CountryCode("BR", "巴西", "+55"),
            CountryCode("MX", "墨西哥", "+52"),
            CountryCode("IT", "意大利", "+39"),
            CountryCode("ES", "西班牙", "+34"),
            CountryCode("NL", "荷兰", "+31"),
            CountryCode("SE", "瑞典", "+46"),
            CountryCode("NO", "挪威", "+47"),
            CountryCode("DK", "丹麦", "+45"),
            CountryCode("FI", "芬兰", "+358"),
            CountryCode("NZ", "新西兰", "+64")
        )
    }
}
