package top.wkbin.taixu.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import top.wkbin.taixu.core.datastore.RemoteServerRepository
import top.wkbin.taixu.core.model.RemoteServer
import top.wkbin.taixu.core.model.SshCredentials
import top.wkbin.taixu.runtime.remote.HostKeyConfirmationRequired
import top.wkbin.taixu.runtime.remote.SshConnectionTester

data class RemoteConnectionState(val busy: Boolean = false, val fingerprint: String? = null, val message: String? = null, val saved: Boolean = false, val tested: Boolean = false)

class RemoteServerViewModel(private val repository: RemoteServerRepository, private val connector: SshConnectionTester) : ViewModel() {
    val servers = repository.servers
    val activeId = repository.activeId
    val configurationError = repository.configurationError
    suspend fun credentials(id: String): SshCredentials = repository.credentials(id)
    private val state = MutableStateFlow(RemoteConnectionState())
    val connectionState: StateFlow<RemoteConnectionState> = state
    private class Pending(val server: RemoteServer, val credentials: SshCredentials, val save: Boolean)
    private var pending: Pending? = null
    private var verified: Pending? = null

    fun connect(server: RemoteServer, credentials: SshCredentials) {
        if (state.value.busy) return
        pending = null
        runConnection(server, credentials, save = true)
    }
    fun test(server: RemoteServer, credentials: SshCredentials) {
        if (state.value.busy) return
        pending = null
        runConnection(server, credentials, save = false)
    }
    fun confirmFingerprint() {
        val request = pending ?: return
        val fingerprint = state.value.fingerprint ?: return
        pending = null
        runConnection(request.server.copy(fingerprint = fingerprint), request.credentials, request.save)
    }
    private fun runConnection(server: RemoteServer, credentials: SshCredentials, save: Boolean) {
        val trusted = verified?.takeIf {
            it.server.copy(fingerprint = "") == server.copy(fingerprint = "") &&
                it.credentials.secret == credentials.secret && it.credentials.passphrase == credentials.passphrase
        }?.server ?: server
        state.value = RemoteConnectionState(busy = true)
        viewModelScope.launch {
            try {
                trusted.validate()
                require(credentials.secret.isNotBlank()) { "Hãy nhập mật khẩu hoặc khóa riêng" }
                connector.test(trusted, credentials)
                verified = Pending(trusted, credentials, save)
                if (save) repository.save(trusted, credentials)
                state.value = RemoteConnectionState(saved = save, tested = true, message = if (save) null else "Kiểm tra kết nối thành công, chưa lưu")
            } catch (e: CancellationException) { throw e }
            catch (e: HostKeyConfirmationRequired) {
                pending = Pending(server, credentials, save)
                state.value = RemoteConnectionState(fingerprint = e.fingerprint)
            } catch (e: IllegalArgumentException) {
                state.value = RemoteConnectionState(message = e.message ?: "Cấu hình kết nối không hợp lệ")
            } catch (e: Exception) {
                state.value = RemoteConnectionState(message = "Kết nối thất bại: hãy kiểm tra mạng, thông tin xác thực, fingerprint máy chủ và thư mục từ xa.")
            }
        }
    }
    fun select(id: String?) = mutate { repository.select(id) }
    fun remove(id: String) = mutate { repository.remove(id) }
    private fun mutate(block: suspend () -> Unit) {
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { state.value = RemoteConnectionState(message = "Lưu cấu hình máy chủ thất bại") }
        }
    }
    fun clear() { if (!state.value.busy) { pending = null; verified = null; state.value = RemoteConnectionState() } }
}
