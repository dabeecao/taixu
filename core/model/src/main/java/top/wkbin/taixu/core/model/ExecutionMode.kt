package top.wkbin.taixu.core.model

/**
 * 太墟支持的系统运行与特权模式。
 */
enum class ExecutionMode(
    val id: String,
    val shortLabel: String,
    val title: String,
    val summary: String,
    val requiredPrivilege: String,
    val capabilities: List<String>,
) {
    PROOT(
        id = "PROOT",
        shortLabel = "PRoot",
        title = "Sandbox người dùng PRoot (mặc định)",
        summary = "Không cần quyền đặc biệt nào, dùng ngay. Mô phỏng môi trường người dùng Linux qua PRoot.",
        requiredPrivilege = "Không (quyền ứng dụng thông thường)",
        capabilities = listOf(
            "100% không cần Root, chạy Debian / Ubuntu đầy đủ",
            "Hỗ trợ hệ sinh thái quản lý gói APT / APK",
            "Hỗ trợ terminal tương tác PTY và bộ công cụ Agent",
        ),
    ),
    SHIZUKU(
        id = "SHIZUKU",
        shortLabel = "Shizuku",
        title = "Chế độ nâng quyền Shizuku / ADB",
        summary = "Nhận quyền ADB qua dịch vụ Shizuku, gỡ bỏ giới hạn hệ thống Android và bật tự động hóa toàn máy.",
        requiredPrivilege = "Ủy quyền dịch vụ Shizuku (shell UID 2000)",
        capabilities = listOf(
            "Gỡ giới hạn 32 tiến trình ma trên Android 12+ (Phantom Process Killer)",
            "Miễn trừ danh sách trắng giữ ứng dụng chạy nền sâu",
            "Điều khiển tự động máy chủ Agent (input / chụp màn hình / logcat)",
            "Vượt qua giới hạn truy cập tệp /sdcard/Android/data",
        ),
    ),
    ROOT(
        id = "ROOT",
        shortLabel = "Root",
        title = "Chế độ hiệu năng gốc Root",
        summary = "Yêu cầu quyền SU, giải phóng 100% sức mạnh phần cứng, tránh triệt để hao phí do PRoot chặn lệnh gọi hệ thống.",
        requiredPrivilege = "Quyền Root (Magisk / KernelSU / APatch / UID 0)",
        capabilities = listOf(
            "Hiệu năng Linux gốc 100% (không tốn chi phí chặn syscall)",
            "Ngữ nghĩa ext4 gốc (giải quyết triệt để lỗi hardlink của perl/dpkg)",
            "Hỗ trợ chạy container và image Docker / Podman",
            "Truy cập trực tiếp nút GPU/NPU (/dev/kgsl-3d0…) để tăng tốc phần cứng",
            "Hỗ trợ lắng nghe cổng đặc quyền 80/443 và iptables",
        ),
    );

    companion object {
        /** 兼容历史持久化值：已下线的 ADB 等未知 id 会安全回落到 PROOT。 */
        fun fromId(id: String): ExecutionMode =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: PROOT
    }
}
