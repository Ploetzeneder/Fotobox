package com.fotobox.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fotobox.app.data.repository.FotoboxRepository
import com.fotobox.app.network.CloudSyncManager
import com.fotobox.app.network.FotobiechenApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectUiState(
    val apiUrl: String = "https://fotobienchen.de/api",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isConnected: Boolean = false,
    val customerName: String = "",
    val boxId: String = "",
    val error: String? = null,
)

@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val repository: FotoboxRepository,
    private val cloudSync: CloudSyncManager
) : ViewModel() {

    private val _state = MutableStateFlow(ConnectUiState())
    val state: StateFlow<ConnectUiState> = _state.asStateFlow()

    init {
        val s = repository.loadSettings()
        _state.value = ConnectUiState(
            apiUrl = s.cloudApiUrl,
            isConnected = s.cloudToken.isNotEmpty(),
            customerName = s.cloudCustomerName,
            boxId = s.cloudBoxId,
        )
    }

    fun setUrl(v: String) = _state.value.let { _state.value = it.copy(apiUrl = v) }
    fun setEmail(v: String) = _state.value.let { _state.value = it.copy(email = v) }
    fun setPassword(v: String) = _state.value.let { _state.value = it.copy(password = v) }

    fun connect() {
        val s = _state.value
        if (s.email.isBlank() || s.password.isBlank()) {
            _state.value = s.copy(error = "E-Mail und Passwort eingeben")
            return
        }
        viewModelScope.launch {
            _state.value = s.copy(isLoading = true, error = null)
            val api = FotobiechenApi(s.apiUrl)
            val result = api.login(s.email, s.password)
            if (result != null) {
                val settings = repository.loadSettings().copy(
                    cloudApiUrl = s.apiUrl,
                    cloudToken = result.token,
                    cloudBoxId = result.boxId,
                    cloudCustomerName = result.customerName,
                    autoUploadCloud = true,
                )
                repository.saveSettings(settings)
                cloudSync.configure(s.apiUrl, result.token, result.boxId)
                cloudSync.fetchAndApplySettings()
                _state.value = _state.value.copy(
                    isLoading = false,
                    isConnected = true,
                    customerName = result.customerName,
                    boxId = result.boxId,
                    error = null,
                )
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Verbindung fehlgeschlagen. E-Mail/Passwort prüfen."
                )
            }
        }
    }

    fun disconnect() {
        val settings = repository.loadSettings().copy(
            cloudToken = "",
            cloudBoxId = "",
            cloudCustomerName = "",
            autoUploadCloud = false,
        )
        repository.saveSettings(settings)
        cloudSync.configure("", "", "")
        _state.value = _state.value.copy(isConnected = false, customerName = "", boxId = "")
    }
}
