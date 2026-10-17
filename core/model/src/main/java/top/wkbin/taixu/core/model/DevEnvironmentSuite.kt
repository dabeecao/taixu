package top.wkbin.taixu.core.model

import kotlinx.serialization.Serializable

/**
 * 🧩 插件子组件定义 (Plugin Sub-Component)
 * 隶属于某个聚合插件大套件下的原子能力组件。
 */
@Serializable
data class PluginComponent(
    val id: String,
    val name: String,
    val description: String,
    val isRequired: Boolean = false, // 是否为必选核心组件（不可取消勾选）
    val aptPackages: List<String> = emptyList(),
    val postInstallSteps: List<String> = emptyList(),
    val checkCommand: String, // 状态探针命令，返回 0 表示已就绪
    /** 其他仍处于装配状态时，禁止卸载本组件。 */
    val dependsOn: List<String> = emptyList(),
    /** 卸载时整目录删除。路径必须落在脚本构建器的白名单内。 */
    val purgePaths: List<String> = emptyList(),
    /** 卸载时删除的单个文件（环境脚本、离线包里的组件文件）。 */
    val purgeFiles: List<String> = emptyList(),
    /** 卸载时删除的命令软链接。 */
    val purgeLinks: List<String> = emptyList(),
    val postUninstallSteps: List<String> = emptyList(),
)

/**
 * 📦 聚合插件大套件定义 (Plugin Bundle)
 * 将领域相关的能力（基础环境、扩展工具、逆向调试等）深度聚合成单一插件大类。
 */
@Serializable
data class PluginBundle(
    val id: String,
    val name: String,
    val summary: String,
    val description: String,
    val iconName: String = "Code",
    val category: String = "开发套件",
    val components: List<PluginComponent> = emptyList(),
    /**
     * 引导流程是否默认勾选本套件。
     *
     * 需要联网下载、且属于"按需能力"而非"开箱可用基座"的套件（如 MCP 能力仓库）设为 false：
     * 引导期静默安装会在新用户第一次进入 App 时引入 npm 下载与镜像可用性依赖。
     */
    val defaultSelected: Boolean = true,
)

object BuiltinPluginBundles {
    /** 基础核心包：始终隐式自动预装，保证 Linux 基础终端与工具可用 */
    val baseRequiredPackages: List<String> = listOf(
        "curl", "wget", "git", "python3", "ca-certificates", "util-linux", "jq", "tmux", "tar", "gzip", "xz-utils", "file",
    )

    /** 核心聚合大插件清单 */
    val bundles: List<PluginBundle> = listOf(
        PluginBundle(
            id = "android-suite",
            name = "Android & 移动全栈开发套件",
            summary = "Gradle 8.14, ARM64 AAPT2/NDK và Flutter tùy chọn, kiểm toán dịch ngược",
            description = "Tích hợp OpenJDK 17, Gradle 8.14.2, ARM64 AAPT2 (digest cố định) và lzhiyong/termux-ndk, kèm toolchain tùy chọn Flutter, C/C++, JADX/APKTool. Cấm tự động tải công cụ host x86_64 chính thức trong lúc build.",
            iconName = "Android",
            category = "移动开发",
            components = listOf(
                PluginComponent(
                    id = "android-core",
                    name = "Android 核心基础环境",
                    description = "OpenJDK 17, Android 34, Build Tools 35, Gradle 8.14.2, ARM64 AAPT2 bất biến, lzhiyong NDK r29, ADB và Maven mirror trong nước — tất cả sẵn sàng ngay khi cài đặt.",
                    isRequired = true,
                    // aapt/zipalign/apksigner come from the downloaded Google
                    // Build-Tools archive. Installing Ubuntu's similarly named
                    // packages pulls GUI/D-Bus/OpenJDK 21 dependencies that
                    // are unnecessary and fragile inside PRoot.
                    aptPackages = listOf("openjdk-17-jdk-headless", "ca-certificates-java", "adb", "curl", "ca-certificates", "util-linux"),
                    postInstallSteps = listOf(
                        "/bin/sh /opt/taixu/scripts/setup_android_core.sh",
                    ),
                    checkCommand = ". /etc/profile.d/taixu-android.sh 2>/dev/null || true; JAVA_BIN=\"\${JAVA_HOME:-/opt/taixu/toolchains/android/jdk}/bin/java\"; (test -x \"\$JAVA_BIN\" || test -x /opt/taixu/bin/java || command -v java >/dev/null 2>&1) && test -f /opt/android-sdk/platforms/android-34/android.jar && test -f /opt/android-sdk/build-tools/35.0.0/lib/d8.jar && (test -f /opt/gradle-8.14.2/lib/gradle-launcher-8.14.2.jar || test -x /opt/taixu/bin/gradle || command -v gradle >/dev/null 2>&1) && (test -x \"\${TAIXU_AAPT2_PATH:-/opt/android-sdk/build-tools/35.0.0/aapt2}\" || test -x /opt/android-sdk/build-tools/35.0.0/aapt2 || test -x /opt/taixu/bin/aapt2) && (test -f \"\${TAIXU_NDK_PATH:-/opt/taixu/toolchains/android/ndk}/source.properties\" || test -f /opt/taixu/toolchains/android/ndk/source.properties)",
                    purgePaths = listOf(
                        "/opt/android-sdk",
                        "/opt/gradle-8.14.2",
                        "/opt/taixu/android-sdk-tools",
                        "/opt/taixu/toolchains/android/sdk-tools",
                        "/opt/taixu/toolchains/android/jdk",
                        "/opt/taixu/toolchains/android/ndk",
                    ),
                    purgeFiles = listOf(
                        "/etc/profile.d/taixu-android.sh",
                        "/root/.gradle/init.gradle",
                        "/root/.gradle/init.d/taixu-android-ndk.gradle",
                    ),
                    purgeLinks = listOf(
                        "/opt/taixu/bin/gradle",
                        "/opt/taixu/bin/aapt2",
                        "/opt/taixu/bin/java",
                        "/opt/taixu/bin/javac",
                        "/opt/taixu/bin/keytool",
                        "/opt/taixu/bin/jarsigner",
                        "/usr/local/bin/gradle",
                        "/usr/bin/gradle",
                        "/usr/local/bin/aapt2",
                        "/usr/bin/aapt2",
                    ),
                ),
                PluginComponent(
                    id = "flutter",
                    name = "Flutter 跨平台开发环境",
                    description = "Flutter ARM64 SDK, Dart runtime và phụ thuộc build APK Android (cần môi trường lõi Android)",
                    isRequired = false,
                    dependsOn = listOf("android-core"),
                    // Archives are extracted by the setup script (Python/BusyBox);
                    // Ubuntu's unzip package is unreliable in PRoot during dpkg
                    // ownership updates (zipinfo.dpkg-new).
                    aptPackages = listOf("git", "curl", "ca-certificates", "xz-utils"),
                    postInstallSteps = listOf(
                        "/bin/sh /opt/taixu/scripts/setup_flutter.sh",
                    ),
                    checkCommand = ". /etc/profile.d/taixu-android.sh 2>/dev/null || true; (test -x /opt/flutter/bin/flutter || test -x /opt/taixu/bin/flutter || command -v flutter >/dev/null 2>&1) && test -f /opt/android-sdk/platforms/android-34/android.jar && test -f /opt/android-sdk/build-tools/35.0.0/lib/d8.jar",
                    purgePaths = listOf("/opt/flutter"),
                    purgeLinks = listOf(
                        "/opt/taixu/bin/flutter",
                        "/opt/taixu/bin/dart",
                        "/usr/local/bin/flutter",
                        "/usr/bin/flutter",
                        "/usr/local/bin/dart",
                        "/usr/bin/dart",
                        "/opt/taixu/tools/android-suite-offline/bin/flutter",
                        "/opt/taixu/tools/android-suite-offline/bin/dart",
                    ),
                ),
                PluginComponent(
                    id = "android-ndk",
                    name = "C/C++ & NDK 原生构建链",
                    description = "Toolchain lzhiyong/termux-ndk r29 Linux AArch64 cùng các công cụ build native CMake, Ninja, GCC/G++, Clang",
                    isRequired = false,
                    aptPackages = listOf("cmake", "ninja-build", "gcc", "g++", "clang", "make", "pkg-config", "util-linux"),
                    postInstallSteps = listOf(
                        "/bin/sh /opt/taixu/scripts/setup_termux_ndk.sh",
                    ),
                    checkCommand = ". /etc/profile.d/taixu-android.sh 2>/dev/null || . /opt/taixu/toolchains/android/ndk/taixu-ndk.env 2>/dev/null || true; (command -v cmake >/dev/null 2>&1 || test -x /opt/taixu/bin/cmake || test -x /opt/taixu/tools/android-suite-offline/cmake/bin/cmake || test -x /usr/bin/cmake) && (test -f \"\${TAIXU_NDK_PATH:-/opt/taixu/toolchains/android/ndk}/source.properties\" || test -f /opt/taixu/toolchains/android/ndk/source.properties)",
                    purgePaths = listOf(
                        "/opt/taixu/toolchains/android/ndk",
                        "/opt/taixu/tools/android-suite-offline/cmake",
                    ),
                    purgeLinks = listOf(
                        "/opt/taixu/bin/cmake",
                        "/opt/taixu/bin/ninja",
                        "/usr/local/bin/cmake",
                        "/usr/local/bin/ninja",
                    ),
                ),
                PluginComponent(
                    id = "android-re",
                    name = "Android 逆向分析与代码审计",
                    description = "APKTool biên dịch ngược tài nguyên, JADX-CLI dịch ngược Java source và dịch vụ MCP dịch ngược APK tích hợp (cần python3)",
                    isRequired = false,
                    aptPackages = listOf("openjdk-17-jdk-headless", "curl", "apktool", "python3"),
                    postInstallSteps = listOf(
                        "/bin/sh /opt/taixu/scripts/setup_jadx.sh",
                    ),
                    checkCommand = ". /etc/profile.d/taixu-android.sh 2>/dev/null || true; (command -v apktool >/dev/null 2>&1 || test -x /opt/taixu/bin/apktool || test -f /opt/taixu/tools/android-suite-offline/lib/apktool.jar || command -v jadx >/dev/null 2>&1 || test -x /opt/taixu/bin/jadx || test -x /opt/jadx/bin/jadx || test -x /opt/taixu/tools/android-suite-offline/jadx/bin/jadx)",
                    purgePaths = listOf(
                        "/opt/jadx",
                        "/opt/taixu/tools/android-suite-offline/jadx",
                    ),
                    purgeFiles = listOf("/opt/taixu/tools/android-suite-offline/lib/apktool.jar"),
                    purgeLinks = listOf(
                        "/opt/taixu/bin/jadx",
                        "/opt/taixu/bin/apktool",
                        "/usr/local/bin/jadx",
                        "/usr/bin/jadx",
                        "/usr/local/bin/apktool",
                        "/usr/bin/apktool",
                        "/opt/taixu/tools/android-suite-offline/bin/jadx",
                        "/opt/taixu/tools/android-suite-offline/bin/apktool",
                    ),
                ),
                PluginComponent(
                    id = "rust-dev",
                    name = "Rust 系统与 Android JNI 交叉编译链",
                    description = "Toolchain Rust 1.85+ ARM64 độc lập, quản lý package Cargo, thư viện chuẩn cross-compile kiến trúc native Android ARM64 và binding linker NDK Clang",
                    isRequired = false,
                    aptPackages = listOf("curl", "ca-certificates", "build-essential"),
                    postInstallSteps = listOf(
                        "/bin/sh /opt/taixu/scripts/setup_rust.sh",
                    ),
                    checkCommand = ". /etc/profile.d/taixu-android.sh 2>/dev/null || true; (command -v rustc >/dev/null 2>&1 || test -x /opt/taixu/bin/rustc || test -x /opt/taixu/toolchains/rust/bin/rustc) && (command -v cargo >/dev/null 2>&1 || test -x /opt/taixu/bin/cargo || test -x /opt/taixu/toolchains/rust/bin/cargo)",
                    purgePaths = listOf("/opt/taixu/toolchains/rust"),
                    purgeFiles = listOf("/etc/profile.d/taixu-rust.sh"),
                    purgeLinks = listOf(
                        "/opt/taixu/bin/rustc",
                        "/opt/taixu/bin/cargo",
                        "/opt/taixu/bin/rustdoc",
                        "/usr/local/bin/rustc",
                        "/usr/local/bin/cargo",
                        "/usr/local/bin/rustdoc",
                        "/usr/bin/rustc",
                        "/usr/bin/cargo",
                        "/usr/bin/rustdoc",
                    ),
                ),
            ),
        ),
        PluginBundle(
            id = "code-search-suite",
            name = "代码检索与终端效率套件",
            summary = "Bộ tứ tìm kiếm code rg, fd, fzf, bat",
            description = "Tích hợp tìm kiếm toàn văn ripgrep, tìm tệp fd, lọc mờ fzf và xem trước highlight cú pháp bat, thống nhất tên lệnh trên Debian/Ubuntu.",
            iconName = "Search",
            category = "开发效率",
            components = listOf(
                PluginComponent(
                    id = "code-search-toolkit",
                    name = "代码检索四件套 (rg / fd / fzf / bat)",
                    description = "Tìm kiếm toàn văn tốc độ cao, khám phá tệp, lọc mờ tương tác và xem trước source highlight cú pháp",
                    isRequired = true,
                    aptPackages = listOf("ripgrep", "fd-find", "fzf", "bat"),
                    checkCommand = "(command -v rg >/dev/null 2>&1 || test -x /opt/taixu/bin/rg) && (command -v fd >/dev/null 2>&1 || command -v fdfind >/dev/null 2>&1 || test -x /usr/local/bin/fd) && (command -v fzf >/dev/null 2>&1 || test -x /usr/bin/fzf) && (command -v bat >/dev/null 2>&1 || command -v batcat >/dev/null 2>&1 || test -x /usr/local/bin/bat)",
                    purgeFiles = listOf(
                        "/opt/taixu/bin/rg",
                        "/opt/taixu/bin/fd",
                        "/opt/taixu/bin/fzf",
                        "/opt/taixu/bin/bat",
                        "/usr/local/bin/fd",
                        "/usr/local/bin/bat",
                        "/opt/taixu/tools/android-suite-offline/bin/rg",
                    ),
                ),
            ),
        ),
        PluginBundle(
            id = "python-suite",
            name = "Python & AI 开发者套件",
            summary = "Python 3 runtime, pip, môi trường ảo venv và phụ thuộc biên dịch tính toán khoa học AI",
            description = "Gồm đầy đủ môi trường chạy Python 3, quản lý package pip, môi trường cách ly venv và thư viện nền build-essential để biên dịch wheel C extension Python.",
            iconName = "Code",
            category = "AI 与脚本",
            components = listOf(
                PluginComponent(
                    id = "python-core",
                    name = "Python 3 核心运行基座",
                    description = "Trình thông dịch Python 3, trình quản lý package pip và công cụ môi trường ảo venv",
                    isRequired = true,
                    aptPackages = listOf("python3", "python3-pip", "python3-venv"),
                    checkCommand = "command -v python3 && command -v pip3",
                ),
                PluginComponent(
                    id = "python-ai-dev",
                    name = "AI 科学计算与 C 扩展编译库",
                    description = "python3-dev, build-essential, pkg-config và header hệ thống cấp thấp",
                    isRequired = false,
                    aptPackages = listOf("python3-dev", "build-essential", "pkg-config", "libffi-dev"),
                    checkCommand = "dpkg -s python3-dev 2>/dev/null || test -f /usr/include/python3*/Python.h",
                ),
            ),
        ),
        PluginBundle(
            id = "nodejs-suite",
            name = "Node.js & Web 全栈套件",
            summary = "Node.js runtime, npm, pnpm và hệ sinh thái fullstack frontend hiện đại",
            description = "Tích hợp Node.js runtime LTS hiện đại, trình quản lý package npm, hỗ trợ pnpm và các công cụ quản lý package hiện đại cho phát triển fullstack JavaScript/TypeScript.",
            iconName = "Globe",
            category = "全栈开发",
            components = listOf(
                PluginComponent(
                    id = "nodejs-core",
                    name = "Node.js 核心运行时",
                    description = "Node.js runtime và trình quản lý package npm",
                    isRequired = true,
                    aptPackages = listOf("nodejs", "npm"),
                    checkCommand = "command -v node && command -v npm",
                ),
                PluginComponent(
                    id = "nodejs-pkg",
                    name = "现代包管理器与编译加速 (pnpm / yarn)",
                    description = "Trình quản lý cache package local hiệu năng cao pnpm và yarn",
                    isRequired = false,
                    dependsOn = listOf("nodejs-core"),
                    postInstallSteps = listOf(
                        "/bin/sh /opt/taixu/scripts/setup_pnpm.sh",
                    ),
                    checkCommand = "command -v pnpm || command -v yarn",
                    purgeLinks = listOf(
                        "/usr/local/bin/pnpm",
                        "/usr/local/bin/yarn",
                        "/usr/bin/pnpm",
                        "/usr/bin/yarn",
                        "/opt/taixu/bin/pnpm",
                        "/opt/taixu/bin/yarn",
                    ),
                    postUninstallSteps = listOf("npm uninstall -g pnpm yarn >/dev/null 2>&1 || true"),
                ),
            ),
        ),
        PluginBundle(
            id = McpCatalog.BUNDLE_ID,
            name = "MCP 能力仓库",
            summary = "Dịch vụ MCP upstream cài theo nhu cầu (kênh Node, phiên bản cố định + checksum)",
            description = "Chuyển các năng lực MCP có thể tách khỏi APK sang cài theo nhu cầu: mỗi năng lực cố định phiên bản upstream," +
                "装配期与 npm 镜像比对 sha512 摘要，落位后写标记文件作为唯一状态探针。" +
                "安装即代表能力就绪；未安装的能力不会进入 Agent 的工具发现，也不会在启动期造成超时等待。",
            iconName = "Extension",
            category = "MCP 能力",
            // 引导流程不默认勾选：MCP 属于按需能力，首次进入 App 不应强制走一次 npm 下载。
            defaultSelected = false,
            components = McpCatalog.installable.map { McpCatalog.component(it) },
        ),
    )

    /**
     * 批量聚合安装脚本。[reinstall] 为 true 时先删掉该组件自己的程序文件，
     * 再执行安装，避免脚本看见残留文件后直接跳过。
     */
    fun buildBatchInstallScript(selectedComponentIds: Set<String>, reinstall: Boolean = false): List<String> =
        PluginBundleScripts.installScript(selectedComponentIds, reinstall)

    /**
     * 卸载选中组件。[retainedComponentIds] 中的组件仍要可用，共享目录和软件包会留下。
     */
    fun buildBatchUninstallScript(selectedComponentIds: Set<String>, retainedComponentIds: Set<String>): List<String> =
        PluginBundleScripts.uninstallScript(selectedComponentIds, retainedComponentIds)

    /** PRoot 下 apt/dpkg 准备步骤（force-unsafe-io、dpkg 状态修复）。供沙箱工具链补齐复用。 */
    fun bundlePreparationSteps(): List<String> = PluginBundleScripts.preparationSteps()

    /** 统一的 apt 选项（重试/超时/IPv4）。供沙箱工具链补齐复用，保证与套件安装行为一致。 */
    fun bundleAptOptions(): String = PluginBundleScripts.aptOptions()

    /** 仍装配着、且声明依赖 [componentId] 的组件。这些组件不在 [alsoRemoving] 里时，不能先卸依赖基座。 */
    fun blockingDependents(
        componentId: String,
        installedIds: Set<String>,
        alsoRemoving: Set<String> = emptySet(),
    ): List<PluginComponent> = bundles.flatMap { it.components }.filter { component ->
        componentId in component.dependsOn &&
            component.id in installedIds &&
            component.id !in alsoRemoving &&
            component.id != componentId
    }
}
