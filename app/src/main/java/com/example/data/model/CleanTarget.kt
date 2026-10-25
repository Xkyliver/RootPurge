package com.example.data.model

enum class CleanTarget(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val defaultEnabled: Boolean = true
) {
    APP_CACHE(
        id = "app_cache",
        title = "App Caches",
        description = "/data/data/*/cache (Images, HTTP, thumbnails)",
        iconName = "FolderZip",
        defaultEnabled = true
    ),
    CODE_CACHE(
        id = "code_cache",
        title = "Code Caches",
        description = "/data/data/*/code_cache (JIT/oat compiled dex)",
        iconName = "Code",
        defaultEnabled = true
    ),
    EXTERNAL_CACHE(
        id = "external_cache",
        title = "External Media Caches",
        description = "Android/data/*/cache (Shared media caches)",
        iconName = "SdStorage",
        defaultEnabled = true
    ),
    SYSTEM_TEMP(
        id = "system_temp",
        title = "System Temp",
        description = "/cache and /data/local/tmp residual caches",
        iconName = "Memory",
        defaultEnabled = true
    ),
    DROP_RAM(
        id = "drop_ram",
        title = "Kernel RAM Buffer",
        description = "Drop pagecache, dentries & inodes (sync; drop_caches)",
        iconName = "Speed",
        defaultEnabled = true
    )
}
