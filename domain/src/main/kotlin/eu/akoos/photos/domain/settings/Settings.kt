/*
 * Photos for Proton
 * Copyright (C) 2026 Akoos <https://akoos.eu>
 *
 * Source:  https://github.com/gitakoos/proton-photos
 * Website: https://www.photosforproton.eu
 *
 * This file is part of Photos for Proton.
 *
 * Photos for Proton is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */


package eu.akoos.photos.domain.settings

/**
 * Typed settings the presentation, navigation, widget and util layers read.
 *
 * Data-side code keeps using SettingsKeys. Each [Setting.name] matches the DataStore
 * preference key string so a binding can check them equal.
 *
 * Conflicting call-site defaults (and keys where absence is itself meaningful) are nullable
 * with a `null` default so call sites can keep their own fallback.
 */
object Settings {

    val AI_FEATURES_ENABLED: Setting<Boolean> = Setting("ai_features_enabled", false)
    val ALBUMS_CUSTOM_ORDER: Setting<String?> = Setting("albums_custom_order", null)
    val ALBUMS_DEFAULT_FILTER: Setting<Int> = Setting("albums_default_filter", 0)
    val ALBUMS_GRID_EXCLUDED: Setting<Set<String>> = Setting("albums_grid_excluded", emptySet())
    val ALBUMS_LAST_FILTER: Setting<Int> = Setting("albums_last_filter", 0)
    val ALBUMS_REMEMBER_LAST_FILTER: Setting<Boolean> = Setting("albums_remember_last_filter", false)
    val ALBUMS_SHOW_COLLECTION: Setting<Boolean> = Setting("albums_show_collection", true)
    val ALBUMS_SORT_MODE: Setting<Int> = Setting("albums_sort_mode", 2)
    val ALBUMS_TILE_HIDDEN: Setting<Set<String>> = Setting("albums_tile_hidden", emptySet())
    val ALBUMS_TILE_ORDER: Setting<String?> = Setting("albums_tile_order", null)
    val ALBUMS_VIEW_MODE: Setting<Int> = Setting("albums_view_mode", 0)
    val ALBUM_ALL_BY_DEFAULT: Setting<Boolean> = Setting("album_all_by_default", false)
    val ALBUM_BUCKET_MAP: Setting<Set<String>> = Setting("album_bucket_map", emptySet())
    val ALBUM_GRID_COLUMNS: Setting<Int> = Setting("album_grid_columns", 2)
    val ALBUM_OPT_IN_FOLDER_NAMES: Setting<Set<String>> = Setting("album_opt_in_folder_names", emptySet())
    val ALBUM_OPT_IN_MIGRATED: Setting<Boolean> = Setting("album_opt_in_migrated", false)
    val ALBUM_OPT_OUT_FOLDER_NAMES: Setting<Set<String>> = Setting("album_opt_out_folder_names", emptySet())
    val ALBUM_PHOTO_GROUPING: Setting<Int> = Setting("album_photo_grouping", 0)
    val ALBUM_PHOTO_GROUPING_OVERRIDES: Setting<Set<String>> = Setting("album_photo_grouping_overrides", emptySet())
    val ALBUM_PHOTO_SORT_MODE: Setting<Int> = Setting("album_photo_sort_mode", 0)
    val ALBUM_PHOTO_SORT_OVERRIDES: Setting<Set<String>> = Setting("album_photo_sort_overrides", emptySet())
    val AMOLED_BLACK: Setting<Boolean> = Setting("amoled_black", false)
    val APP_LOCK_ENABLED: Setting<Boolean> = Setting("app_lock_enabled", false)
    val APP_LOCK_TIMEOUT_MINUTES: Setting<Int?> = Setting("app_lock_timeout_minutes", null)
    val APP_LOCK_TIMEOUT_SECONDS: Setting<Int> = Setting("app_lock_timeout_seconds", 0)
    val AUTO_FREE_UP: Setting<Boolean> = Setting("auto_free_up", false)
    val AUTO_SYNC: Setting<Boolean> = Setting("auto_sync", true)
    val BACKUP_EVERYTHING: Setting<Boolean> = Setting("backup_everything", false)
    val CATEGORY_RAIL_HIDDEN: Setting<Set<String>> = Setting("category_rail_hidden", emptySet())
    val CATEGORY_RAIL_ORDER: Setting<String?> = Setting("category_rail_order", null)
    val CLEAR_CACHE_ON_APP_CLOSE: Setting<Boolean> = Setting("clear_cache_on_app_close", false)
    val CLOCK_FORMAT: Setting<String> = Setting("clock_format", "auto")
    val COMPRESS_HDR_PHOTOS: Setting<Boolean> = Setting("compress_hdr_photos", false)
    val COMPRESS_ON_UPLOAD: Setting<Boolean> = Setting("compress_on_upload", false)
    val COMPRESS_UPLOAD_TIER: Setting<Int?> = Setting("compress_upload_tier", null)
    val COMPRESS_UPLOAD_TIER_VIDEO: Setting<Int?> = Setting("compress_upload_tier_video", null)
    val COMPRESS_VIDEO_CODEC: Setting<String?> = Setting("compress_video_codec", null)
    val COMPRESS_VIDEO_ON_UPLOAD: Setting<Boolean> = Setting("compress_video_on_upload", false)
    val CONTINUE_WITHOUT_ACCOUNT: Setting<Boolean> = Setting("continue_without_account", false)
    val DARK_MODE: Setting<Boolean?> = Setting("dark_mode", null)
    val DELETE_LOCAL_AFTER_BACKUP: Setting<Boolean> = Setting("delete_local_after_backup", false)
    val DENSE_GRID_WARNING_DISMISSED: Setting<Boolean> = Setting("dense_grid_warning_dismissed", false)
    val DEVICE_FOLDER_PHOTO_GROUPING: Setting<Int> = Setting("device_folder_photo_grouping", 0)
    val DEVICE_FOLDER_PHOTO_GROUPING_OVERRIDES: Setting<Set<String>> = Setting("device_folder_photo_grouping_overrides", emptySet())
    val DEVICE_FOLDER_PHOTO_SORT_MODE: Setting<Int> = Setting("device_folder_photo_sort_mode", 0)
    val DEVICE_FOLDER_PHOTO_SORT_OVERRIDES: Setting<Set<String>> = Setting("device_folder_photo_sort_overrides", emptySet())
    val DOWNLOAD_DATE_OVERRIDES: Setting<Set<String>> = Setting("download_date_overrides", emptySet())
    val DUPLICATE_IGNORED_GROUPS: Setting<Set<String>> = Setting("duplicate_ignored_groups", emptySet())
    val EXCLUDED_FOLDER_NAMES: Setting<Set<String>> = Setting("excluded_folder_names", emptySet())
    val FACE_AUTO_MERGE: Setting<Boolean> = Setting("face_auto_merge", false)
    val FACE_ENABLED: Setting<Boolean> = Setting("face_enabled", false)
    val FACE_INDEXING_PAUSED: Setting<Boolean> = Setting("face_indexing_paused", false)
    val FACE_MODEL_DOWNLOAD_ALLOWED: Setting<Boolean> = Setting("face_model_download_allowed", false)
    val FAVORITE_IDS: Setting<Set<String>> = Setting("favorite_ids", emptySet())
    val FOLDER_COVER_URI_MAP: Setting<Set<String>> = Setting("folder_cover_uri_map", emptySet())
    val FREE_UP_INTERVAL: Setting<String> = Setting("free_up_interval", "OneMonth")
    val FULLRES_WIFI_ONLY: Setting<Boolean> = Setting("fullres_wifi_only", true)
    val GIF_AUTOPLAY_COVERS: Setting<Boolean> = Setting("gif_autoplay_covers", false)
    val GIF_AUTOPLAY_GRID: Setting<Boolean> = Setting("gif_autoplay_grid", false)
    val GRID_DEFAULT_COLUMNS: Setting<Int> = Setting("grid_default_columns", 3)
    val GRID_LAST_LEVEL: Setting<Int> = Setting("grid_last_level", 4)
    val GRID_REMEMBER_LAST: Setting<Boolean> = Setting("grid_remember_last", false)
    val HIDDEN_ALBUM_IDS: Setting<Set<String>> = Setting("hidden_album_ids", emptySet())
    val HIDDEN_CLOUD_PHOTO_IDS: Setting<Set<String>> = Setting("hidden_cloud_photo_ids", emptySet())
    val HIDDEN_FOLDER_NAMES: Setting<Set<String>> = Setting("hidden_folder_names", emptySet())
    val HIDDEN_PHOTO_URIS: Setting<Set<String>> = Setting("hidden_photo_uris", emptySet())
    val HIDDEN_REMOTE_HIDE_QUEUE: Setting<Set<String>> = Setting("hidden_remote_hide_queue", emptySet())
    val HIDDEN_URI_CLOUD_ID_MAP: Setting<Set<String>> = Setting("hidden_uri_cloud_id_map", emptySet())
    val HIDDEN_URI_ORIGINAL_NAME_MAP: Setting<Set<String>> = Setting("hidden_uri_original_name_map", emptySet())
    val HIDDEN_URI_SOURCE_FOLDER_MAP: Setting<Set<String>> = Setting("hidden_uri_source_folder_map", emptySet())
    val HIDE_PHOTOS_IN_ALBUMS: Setting<Boolean> = Setting("hide_photos_in_albums", false)
    val KEEP_SCROLL_ON_TAB_SWITCH: Setting<Boolean> = Setting("keep_scroll_on_tab_switch", false)
    val LABELS_NOTE_SEEN: Setting<Boolean> = Setting("labels_note_seen", false)
    val LANDING_TAB: Setting<Int> = Setting("landing_tab", 0)
    val LANGUAGE: Setting<String> = Setting("language", "system")
    val LAST_SYNC_MS: Setting<Long> = Setting("last_sync_ms", 0L)
    val LIQUID_GLASS: Setting<Boolean> = Setting("liquid_glass", false)
    val MANAGE_MEDIA_PROMPTED: Setting<Boolean> = Setting("manage_media_prompted", false)
    val MANUAL_LOCAL_FOLDER_NAMES: Setting<Set<String>> = Setting("manual_local_folder_names", emptySet())
    val MAP_STYLE_OSM: Setting<Boolean> = Setting("map_style_osm", false)
    val MAP_STYLE_PROMPTED: Setting<Boolean> = Setting("map_style_prompted", false)
    val MIRROR_COMPRESS_TO_LOCAL: Setting<Boolean> = Setting("mirror_compress_to_local", false)
    val MIRROR_STRIP_TO_LOCAL: Setting<Boolean> = Setting("mirror_strip_to_local", false)
    val ML_ONLY_WHILE_CHARGING: Setting<Boolean> = Setting("ml_only_while_charging", false)
    val MOSAIC_GRID: Setting<Boolean> = Setting("mosaic_grid", false)
    val NOTIFICATION_PERMISSION_ASKED: Setting<Boolean> = Setting("notification_permission_asked", false)
    val NOTIFY_ALBUM_DOWNLOAD: Setting<Boolean> = Setting("notify_album_download", true)
    val NOTIFY_BACKUP_STATUS: Setting<Boolean> = Setting("notify_backup_status", false)
    val NOTIFY_DELETE_REMINDER: Setting<Boolean> = Setting("notify_delete_reminder", true)
    val OCR_ENABLED: Setting<Boolean?> = Setting("ocr_enabled", null)
    val OCR_MODEL_DOWNLOAD_ALLOWED: Setting<Boolean> = Setting("ocr_model_download_allowed", false)
    val OFFLINE_PIN_IDS: Setting<Set<String>> = Setting("offline_pin_ids", emptySet())
    val ONBOARDING_COMPLETE: Setting<Boolean> = Setting("onboarding_complete", false)
    val ON_THIS_DAY_WINDOW_DAYS: Setting<Int> = Setting("on_this_day_window_days", 0)
    val PENDING_DELETE_URIS: Setting<Set<String>> = Setting("pending_delete_uris", emptySet())
    val PEOPLE_FOLLOW_ALBUMS: Setting<Boolean> = Setting("people_follow_albums", true)
    val PEOPLE_GRID_COLUMNS: Setting<Int> = Setting("people_grid_columns", 2)
    val PEOPLE_VIEW_MODE: Setting<Int> = Setting("people_view_mode", 0)
    val RENAME_TO_CAPTURE_DATE: Setting<Boolean> = Setting("rename_to_capture_date", false)
    val REVERSE_TIMELINE_ORDER: Setting<Boolean> = Setting("reverse_timeline_order", false)
    val SCREENSHOT_OVERLAY_ENABLED: Setting<Boolean> = Setting("screenshot_overlay_enabled", false)
    val SEAMLESS_GRID: Setting<Boolean> = Setting("seamless_grid", false)
    val SEMANTIC_ENABLED: Setting<Boolean> = Setting("semantic_enabled", false)
    val SEMANTIC_MODEL_DOWNLOAD_ALLOWED: Setting<Boolean> = Setting("semantic_model_download_allowed", false)
    val SHARED_BACKUP_ENABLED: Setting<Boolean> = Setting("shared_backup_enabled", false)
    val SHOW_ON_THIS_DAY: Setting<Boolean> = Setting("show_on_this_day", true)
    val SHOW_SCROLL_DATE: Setting<Boolean> = Setting("show_scroll_date", false)
    val SIGN_OUT_CLEANUP_PENDING: Setting<Set<String>> = Setting("sign_out_cleanup_pending", emptySet())
    val SIM_LARGE_LIBRARY_COUNT: Setting<Int> = Setting("sim_large_library_count", 0)
    val STRIP_CAMERA_INFO: Setting<Boolean> = Setting("strip_camera_info", false)
    val STRIP_GPS: Setting<Boolean> = Setting("strip_gps", false)
    val STRIP_ON_SHARE: Setting<Boolean> = Setting("strip_on_share", false)
    val STRIP_ON_UPLOAD: Setting<Boolean> = Setting("strip_on_upload", false)
    val STRIP_SHARE_AUTHORSHIP: Setting<Boolean> = Setting("strip_share_authorship", false)
    val STRIP_SHARE_CAMERA_INFO: Setting<Boolean> = Setting("strip_share_camera_info", false)
    val STRIP_SHARE_GPS: Setting<Boolean> = Setting("strip_share_gps", true)
    val STRIP_SHARE_SOFTWARE_INFO: Setting<Boolean> = Setting("strip_share_software_info", false)
    val STRIP_SHARE_TIMESTAMP: Setting<Boolean> = Setting("strip_share_timestamp", false)
    val STRIP_SOFTWARE_INFO: Setting<Boolean> = Setting("strip_software_info", false)
    val STRIP_TIMESTAMP: Setting<Boolean> = Setting("strip_timestamp", false)
    val SYNC_FOLDER_NAMES: Setting<Set<String>?> = Setting("sync_folder_names", null)
    val SYNC_WIFI_ONLY: Setting<Boolean> = Setting("sync_wifi_only", true)
    val THEME_MODE: Setting<String?> = Setting("theme_mode", null)
    val THEME_PALETTE: Setting<String> = Setting("theme_palette", "default")
    val TIMELINE_EXCLUDED_ALBUM_IDS: Setting<Set<String>> = Setting("timeline_excluded_album_ids", emptySet())
    val TIMELINE_EXCLUDED_FOLDER_NAMES: Setting<Set<String>> = Setting("timeline_excluded_folder_names", emptySet())
    val TINT_CLOUD_WITH_ACCENT: Setting<Boolean> = Setting("tint_cloud_with_accent", false)
    val UPDATE_BACKGROUND_CHECK: Setting<Boolean> = Setting("update_background_check", true)
    val VAULT_PASSWORD_BLOB: Setting<String?> = Setting("vault_password_blob", null)
    val VIEWER_LONG_PRESS_OPENS_MENU: Setting<Boolean> = Setting("viewer_long_press_opens_menu", false)
    val WHATS_NEW_SEEN_VERSION: Setting<Int> = Setting("whats_new_seen_version", 0)

    /** Every catalog entry, for binding validation and tests. */
    val all: List<Setting<*>> = listOf(
        AI_FEATURES_ENABLED,
        ALBUMS_CUSTOM_ORDER,
        ALBUMS_DEFAULT_FILTER,
        ALBUMS_GRID_EXCLUDED,
        ALBUMS_LAST_FILTER,
        ALBUMS_REMEMBER_LAST_FILTER,
        ALBUMS_SHOW_COLLECTION,
        ALBUMS_SORT_MODE,
        ALBUMS_TILE_HIDDEN,
        ALBUMS_TILE_ORDER,
        ALBUMS_VIEW_MODE,
        ALBUM_ALL_BY_DEFAULT,
        ALBUM_BUCKET_MAP,
        ALBUM_GRID_COLUMNS,
        ALBUM_OPT_IN_FOLDER_NAMES,
        ALBUM_OPT_IN_MIGRATED,
        ALBUM_OPT_OUT_FOLDER_NAMES,
        ALBUM_PHOTO_GROUPING,
        ALBUM_PHOTO_GROUPING_OVERRIDES,
        ALBUM_PHOTO_SORT_MODE,
        ALBUM_PHOTO_SORT_OVERRIDES,
        AMOLED_BLACK,
        APP_LOCK_ENABLED,
        APP_LOCK_TIMEOUT_MINUTES,
        APP_LOCK_TIMEOUT_SECONDS,
        AUTO_FREE_UP,
        AUTO_SYNC,
        BACKUP_EVERYTHING,
        CATEGORY_RAIL_HIDDEN,
        CATEGORY_RAIL_ORDER,
        CLEAR_CACHE_ON_APP_CLOSE,
        CLOCK_FORMAT,
        COMPRESS_HDR_PHOTOS,
        COMPRESS_ON_UPLOAD,
        COMPRESS_UPLOAD_TIER,
        COMPRESS_UPLOAD_TIER_VIDEO,
        COMPRESS_VIDEO_CODEC,
        COMPRESS_VIDEO_ON_UPLOAD,
        CONTINUE_WITHOUT_ACCOUNT,
        DARK_MODE,
        DELETE_LOCAL_AFTER_BACKUP,
        DENSE_GRID_WARNING_DISMISSED,
        DEVICE_FOLDER_PHOTO_GROUPING,
        DEVICE_FOLDER_PHOTO_GROUPING_OVERRIDES,
        DEVICE_FOLDER_PHOTO_SORT_MODE,
        DEVICE_FOLDER_PHOTO_SORT_OVERRIDES,
        DOWNLOAD_DATE_OVERRIDES,
        DUPLICATE_IGNORED_GROUPS,
        EXCLUDED_FOLDER_NAMES,
        FACE_AUTO_MERGE,
        FACE_ENABLED,
        FACE_INDEXING_PAUSED,
        FACE_MODEL_DOWNLOAD_ALLOWED,
        FAVORITE_IDS,
        FOLDER_COVER_URI_MAP,
        FREE_UP_INTERVAL,
        FULLRES_WIFI_ONLY,
        GIF_AUTOPLAY_COVERS,
        GIF_AUTOPLAY_GRID,
        GRID_DEFAULT_COLUMNS,
        GRID_LAST_LEVEL,
        GRID_REMEMBER_LAST,
        HIDDEN_ALBUM_IDS,
        HIDDEN_CLOUD_PHOTO_IDS,
        HIDDEN_FOLDER_NAMES,
        HIDDEN_PHOTO_URIS,
        HIDDEN_REMOTE_HIDE_QUEUE,
        HIDDEN_URI_CLOUD_ID_MAP,
        HIDDEN_URI_ORIGINAL_NAME_MAP,
        HIDDEN_URI_SOURCE_FOLDER_MAP,
        HIDE_PHOTOS_IN_ALBUMS,
        KEEP_SCROLL_ON_TAB_SWITCH,
        LABELS_NOTE_SEEN,
        LANDING_TAB,
        LANGUAGE,
        LAST_SYNC_MS,
        LIQUID_GLASS,
        MANAGE_MEDIA_PROMPTED,
        MANUAL_LOCAL_FOLDER_NAMES,
        MAP_STYLE_OSM,
        MAP_STYLE_PROMPTED,
        MIRROR_COMPRESS_TO_LOCAL,
        MIRROR_STRIP_TO_LOCAL,
        ML_ONLY_WHILE_CHARGING,
        MOSAIC_GRID,
        NOTIFICATION_PERMISSION_ASKED,
        NOTIFY_ALBUM_DOWNLOAD,
        NOTIFY_BACKUP_STATUS,
        NOTIFY_DELETE_REMINDER,
        OCR_ENABLED,
        OCR_MODEL_DOWNLOAD_ALLOWED,
        OFFLINE_PIN_IDS,
        ONBOARDING_COMPLETE,
        ON_THIS_DAY_WINDOW_DAYS,
        PENDING_DELETE_URIS,
        PEOPLE_FOLLOW_ALBUMS,
        PEOPLE_GRID_COLUMNS,
        PEOPLE_VIEW_MODE,
        RENAME_TO_CAPTURE_DATE,
        REVERSE_TIMELINE_ORDER,
        SCREENSHOT_OVERLAY_ENABLED,
        SEAMLESS_GRID,
        SEMANTIC_ENABLED,
        SEMANTIC_MODEL_DOWNLOAD_ALLOWED,
        SHARED_BACKUP_ENABLED,
        SHOW_ON_THIS_DAY,
        SHOW_SCROLL_DATE,
        SIGN_OUT_CLEANUP_PENDING,
        SIM_LARGE_LIBRARY_COUNT,
        STRIP_CAMERA_INFO,
        STRIP_GPS,
        STRIP_ON_SHARE,
        STRIP_ON_UPLOAD,
        STRIP_SHARE_AUTHORSHIP,
        STRIP_SHARE_CAMERA_INFO,
        STRIP_SHARE_GPS,
        STRIP_SHARE_SOFTWARE_INFO,
        STRIP_SHARE_TIMESTAMP,
        STRIP_SOFTWARE_INFO,
        STRIP_TIMESTAMP,
        SYNC_FOLDER_NAMES,
        SYNC_WIFI_ONLY,
        THEME_MODE,
        THEME_PALETTE,
        TIMELINE_EXCLUDED_ALBUM_IDS,
        TIMELINE_EXCLUDED_FOLDER_NAMES,
        TINT_CLOUD_WITH_ACCENT,
        UPDATE_BACKGROUND_CHECK,
        VAULT_PASSWORD_BLOB,
        VIEWER_LONG_PRESS_OPENS_MENU,
        WHATS_NEW_SEEN_VERSION,
    )
}
