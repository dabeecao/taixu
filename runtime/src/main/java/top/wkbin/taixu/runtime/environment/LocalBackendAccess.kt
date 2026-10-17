package top.wkbin.taixu.runtime.environment

import top.wkbin.taixu.core.datastore.RemoteServerRepository

/** Guard the service entry as well as its UI so alternate install/uninstall routes cannot bypass it. */
class LocalBackendAccess(private val servers: RemoteServerRepository) {
    fun requireLocal() {
        check(servers.activeId.value == null) { "Hệ thống chính hiện tại không hỗ trợ thao tác plugin trên máy; hãy chuyển rõ về hệ thống local trong mục quản lý hệ thống Linux trước" }
    }
}
