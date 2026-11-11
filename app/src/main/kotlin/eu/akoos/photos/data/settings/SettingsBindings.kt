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


package eu.akoos.photos.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import eu.akoos.photos.data.preferences.SettingsKeys
import eu.akoos.photos.domain.settings.Settings
import eu.akoos.photos.domain.settings.SettingsStore

/** Builds the production [SettingsStore] with a typed binding for every catalog [Setting]. */
object SettingsBindings {

    fun create(dataStore: DataStore<Preferences>): SettingsStore =
        DataStoreSettingsStore.Builder(dataStore).apply {
            bind(Settings.AI_FEATURES_ENABLED, SettingsKeys.AI_FEATURES_ENABLED)
            bind(Settings.ALBUMS_CUSTOM_ORDER, SettingsKeys.ALBUMS_CUSTOM_ORDER)
            bind(Settings.ALBUMS_DEFAULT_FILTER, SettingsKeys.ALBUMS_DEFAULT_FILTER)
            bind(Settings.ALBUMS_GRID_EXCLUDED, SettingsKeys.ALBUMS_GRID_EXCLUDED)
            bind(Settings.ALBUMS_LAST_FILTER, SettingsKeys.ALBUMS_LAST_FILTER)
            bind(Settings.ALBUMS_REMEMBER_LAST_FILTER, SettingsKeys.ALBUMS_REMEMBER_LAST_FILTER)
            bind(Settings.ALBUMS_SHOW_COLLECTION, SettingsKeys.ALBUMS_SHOW_COLLECTION)
            bind(Settings.ALBUMS_SORT_MODE, SettingsKeys.ALBUMS_SORT_MODE)
            bind(Settings.ALBUMS_TILE_HIDDEN, SettingsKeys.ALBUMS_TILE_HIDDEN)
            bind(Settings.ALBUMS_TILE_ORDER, SettingsKeys.ALBUMS_TILE_ORDER)
            bind(Settings.ALBUMS_VIEW_MODE, SettingsKeys.ALBUMS_VIEW_MODE)
            bind(Settings.ALBUM_ALL_BY_DEFAULT, SettingsKeys.ALBUM_ALL_BY_DEFAULT)
            bind(Settings.ALBUM_BUCKET_MAP, SettingsKeys.ALBUM_BUCKET_MAP)
            bind(Settings.ALBUM_GRID_COLUMNS, SettingsKeys.ALBUM_GRID_COLUMNS)
            bind(Settings.ALBUM_OPT_IN_FOLDER_NAMES, SettingsKeys.ALBUM_OPT_IN_FOLDER_NAMES)
            bind(Settings.ALBUM_OPT_IN_MIGRATED, SettingsKeys.ALBUM_OPT_IN_MIGRATED)
            bind(Settings.ALBUM_OPT_OUT_FOLDER_NAMES, SettingsKeys.ALBUM_OPT_OUT_FOLDER_NAMES)
            bind(Settings.ALBUM_PHOTO_GROUPING, SettingsKeys.ALBUM_PHOTO_GROUPING)
            bind(Settings.ALBUM_PHOTO_GROUPING_OVERRIDES, SettingsKeys.ALBUM_PHOTO_GROUPING_OVERRIDES)
            bind(Settings.ALBUM_PHOTO_SORT_MODE, SettingsKeys.ALBUM_PHOTO_SORT_MODE)
            bind(Settings.ALBUM_PHOTO_SORT_OVERRIDES, SettingsKeys.ALBUM_PHOTO_SORT_OVERRIDES)
            bind(Settings.AMOLED_BLACK, SettingsKeys.AMOLED_BLACK)
            bind(Settings.APP_LOCK_ENABLED, SettingsKeys.APP_LOCK_ENABLED)
            bind(Settings.APP_LOCK_TIMEOUT_MINUTES, SettingsKeys.APP_LOCK_TIMEOUT_MINUTES)
            bind(Settings.APP_LOCK_TIMEOUT_SECONDS, SettingsKeys.APP_LOCK_TIMEOUT_SECONDS)
            bind(Settings.AUTO_FREE_UP, SettingsKeys.AUTO_FREE_UP)
            bind(Settings.AUTO_SYNC, SettingsKeys.AUTO_SYNC)
            bind(Settings.BACKUP_EVERYTHING, SettingsKeys.BACKUP_EVERYTHING)
            bind(Settings.CATEGORY_RAIL_HIDDEN, SettingsKeys.CATEGORY_RAIL_HIDDEN)
            bind(Settings.CATEGORY_RAIL_ORDER, SettingsKeys.CATEGORY_RAIL_ORDER)
            bind(Settings.CLEAR_CACHE_ON_APP_CLOSE, SettingsKeys.CLEAR_CACHE_ON_APP_CLOSE)
            bind(Settings.CLOCK_FORMAT, SettingsKeys.CLOCK_FORMAT)
            bind(Settings.COMPRESS_HDR_PHOTOS, SettingsKeys.COMPRESS_HDR_PHOTOS)
            bind(Settings.COMPRESS_ON_UPLOAD, SettingsKeys.COMPRESS_ON_UPLOAD)
            bind(Settings.COMPRESS_UPLOAD_TIER, SettingsKeys.COMPRESS_UPLOAD_TIER)
            bind(Settings.COMPRESS_UPLOAD_TIER_VIDEO, SettingsKeys.COMPRESS_UPLOAD_TIER_VIDEO)
            bind(Settings.COMPRESS_VIDEO_CODEC, SettingsKeys.COMPRESS_VIDEO_CODEC)
            bind(Settings.COMPRESS_VIDEO_ON_UPLOAD, SettingsKeys.COMPRESS_VIDEO_ON_UPLOAD)
            bind(Settings.CONTINUE_WITHOUT_ACCOUNT, SettingsKeys.CONTINUE_WITHOUT_ACCOUNT)
            bind(Settings.DARK_MODE, SettingsKeys.DARK_MODE)
            bind(Settings.DELETE_LOCAL_AFTER_BACKUP, SettingsKeys.DELETE_LOCAL_AFTER_BACKUP)
            bind(Settings.DENSE_GRID_WARNING_DISMISSED, SettingsKeys.DENSE_GRID_WARNING_DISMISSED)
            bind(Settings.DEVICE_FOLDER_PHOTO_GROUPING, SettingsKeys.DEVICE_FOLDER_PHOTO_GROUPING)
            bind(Settings.DEVICE_FOLDER_PHOTO_GROUPING_OVERRIDES, SettingsKeys.DEVICE_FOLDER_PHOTO_GROUPING_OVERRIDES)
            bind(Settings.DEVICE_FOLDER_PHOTO_SORT_MODE, SettingsKeys.DEVICE_FOLDER_PHOTO_SORT_MODE)
            bind(Settings.DEVICE_FOLDER_PHOTO_SORT_OVERRIDES, SettingsKeys.DEVICE_FOLDER_PHOTO_SORT_OVERRIDES)
            bind(Settings.DOWNLOAD_DATE_OVERRIDES, SettingsKeys.DOWNLOAD_DATE_OVERRIDES)
            bind(Settings.DUPLICATE_IGNORED_GROUPS, SettingsKeys.DUPLICATE_IGNORED_GROUPS)
            bind(Settings.EXCLUDED_FOLDER_NAMES, SettingsKeys.EXCLUDED_FOLDER_NAMES)
            bind(Settings.FACE_AUTO_MERGE, SettingsKeys.FACE_AUTO_MERGE)
            bind(Settings.FACE_ENABLED, SettingsKeys.FACE_ENABLED)
            bind(Settings.FACE_INDEXING_PAUSED, SettingsKeys.FACE_INDEXING_PAUSED)
            bind(Settings.FACE_MODEL_DOWNLOAD_ALLOWED, SettingsKeys.FACE_MODEL_DOWNLOAD_ALLOWED)
            bind(Settings.FAVORITE_IDS, SettingsKeys.FAVORITE_IDS)
            bind(Settings.FOLDER_COVER_URI_MAP, SettingsKeys.FOLDER_COVER_URI_MAP)
            bind(Settings.FREE_UP_INTERVAL, SettingsKeys.FREE_UP_INTERVAL)
            bind(Settings.FULLRES_WIFI_ONLY, SettingsKeys.FULLRES_WIFI_ONLY)
            bind(Settings.GIF_AUTOPLAY_COVERS, SettingsKeys.GIF_AUTOPLAY_COVERS)
            bind(Settings.GIF_AUTOPLAY_GRID, SettingsKeys.GIF_AUTOPLAY_GRID)
            bind(Settings.GRID_DEFAULT_COLUMNS, SettingsKeys.GRID_DEFAULT_COLUMNS)
            bind(Settings.GRID_LAST_LEVEL, SettingsKeys.GRID_LAST_LEVEL)
            bind(Settings.GRID_REMEMBER_LAST, SettingsKeys.GRID_REMEMBER_LAST)
            bind(Settings.HIDDEN_ALBUM_IDS, SettingsKeys.HIDDEN_ALBUM_IDS)
            bind(Settings.HIDDEN_CLOUD_PHOTO_IDS, SettingsKeys.HIDDEN_CLOUD_PHOTO_IDS)
            bind(Settings.HIDDEN_FOLDER_NAMES, SettingsKeys.HIDDEN_FOLDER_NAMES)
            bind(Settings.HIDDEN_PHOTO_URIS, SettingsKeys.HIDDEN_PHOTO_URIS)
            bind(Settings.HIDDEN_REMOTE_HIDE_QUEUE, SettingsKeys.HIDDEN_REMOTE_HIDE_QUEUE)
            bind(Settings.HIDDEN_URI_CLOUD_ID_MAP, SettingsKeys.HIDDEN_URI_CLOUD_ID_MAP)
            bind(Settings.HIDDEN_URI_ORIGINAL_NAME_MAP, SettingsKeys.HIDDEN_URI_ORIGINAL_NAME_MAP)
            bind(Settings.HIDDEN_URI_SOURCE_FOLDER_MAP, SettingsKeys.HIDDEN_URI_SOURCE_FOLDER_MAP)
            bind(Settings.HIDE_PHOTOS_IN_ALBUMS, SettingsKeys.HIDE_PHOTOS_IN_ALBUMS)
            bind(Settings.KEEP_SCROLL_ON_TAB_SWITCH, SettingsKeys.KEEP_SCROLL_ON_TAB_SWITCH)
            bind(Settings.LABELS_NOTE_SEEN, SettingsKeys.LABELS_NOTE_SEEN)
            bind(Settings.LANDING_TAB, SettingsKeys.LANDING_TAB)
            bind(Settings.LANGUAGE, SettingsKeys.LANGUAGE)
            bind(Settings.LAST_SYNC_MS, SettingsKeys.LAST_SYNC_MS)
            bind(Settings.LIQUID_GLASS, SettingsKeys.LIQUID_GLASS)
            bind(Settings.MANAGE_MEDIA_PROMPTED, SettingsKeys.MANAGE_MEDIA_PROMPTED)
            bind(Settings.MANUAL_LOCAL_FOLDER_NAMES, SettingsKeys.MANUAL_LOCAL_FOLDER_NAMES)
            bind(Settings.MAP_STYLE_OSM, SettingsKeys.MAP_STYLE_OSM)
            bind(Settings.MAP_STYLE_PROMPTED, SettingsKeys.MAP_STYLE_PROMPTED)
            bind(Settings.MIRROR_COMPRESS_TO_LOCAL, SettingsKeys.MIRROR_COMPRESS_TO_LOCAL)
            bind(Settings.MIRROR_STRIP_TO_LOCAL, SettingsKeys.MIRROR_STRIP_TO_LOCAL)
            bind(Settings.ML_ONLY_WHILE_CHARGING, SettingsKeys.ML_ONLY_WHILE_CHARGING)
            bind(Settings.MOSAIC_GRID, SettingsKeys.MOSAIC_GRID)
            bind(Settings.NOTIFICATION_PERMISSION_ASKED, SettingsKeys.NOTIFICATION_PERMISSION_ASKED)
            bind(Settings.NOTIFY_ALBUM_DOWNLOAD, SettingsKeys.NOTIFY_ALBUM_DOWNLOAD)
            bind(Settings.NOTIFY_BACKUP_STATUS, SettingsKeys.NOTIFY_BACKUP_STATUS)
            bind(Settings.NOTIFY_DELETE_REMINDER, SettingsKeys.NOTIFY_DELETE_REMINDER)
            bind(Settings.OCR_ENABLED, SettingsKeys.OCR_ENABLED)
            bind(Settings.OCR_MODEL_DOWNLOAD_ALLOWED, SettingsKeys.OCR_MODEL_DOWNLOAD_ALLOWED)
            bind(Settings.OFFLINE_PIN_IDS, SettingsKeys.OFFLINE_PIN_IDS)
            bind(Settings.ONBOARDING_COMPLETE, SettingsKeys.ONBOARDING_COMPLETE)
            bind(Settings.ON_THIS_DAY_WINDOW_DAYS, SettingsKeys.ON_THIS_DAY_WINDOW_DAYS)
            bind(Settings.PENDING_DELETE_URIS, SettingsKeys.PENDING_DELETE_URIS)
            bind(Settings.PEOPLE_FOLLOW_ALBUMS, SettingsKeys.PEOPLE_FOLLOW_ALBUMS)
            bind(Settings.PEOPLE_GRID_COLUMNS, SettingsKeys.PEOPLE_GRID_COLUMNS)
            bind(Settings.PEOPLE_VIEW_MODE, SettingsKeys.PEOPLE_VIEW_MODE)
            bind(Settings.RENAME_TO_CAPTURE_DATE, SettingsKeys.RENAME_TO_CAPTURE_DATE)
            bind(Settings.REVERSE_TIMELINE_ORDER, SettingsKeys.REVERSE_TIMELINE_ORDER)
            bind(Settings.SCREENSHOT_OVERLAY_ENABLED, SettingsKeys.SCREENSHOT_OVERLAY_ENABLED)
            bind(Settings.SEAMLESS_GRID, SettingsKeys.SEAMLESS_GRID)
            bind(Settings.SEMANTIC_ENABLED, SettingsKeys.SEMANTIC_ENABLED)
            bind(Settings.SEMANTIC_MODEL_DOWNLOAD_ALLOWED, SettingsKeys.SEMANTIC_MODEL_DOWNLOAD_ALLOWED)
            bind(Settings.SHARED_BACKUP_ENABLED, SettingsKeys.SHARED_BACKUP_ENABLED)
            bind(Settings.SHOW_ON_THIS_DAY, SettingsKeys.SHOW_ON_THIS_DAY)
            bind(Settings.SHOW_SCROLL_DATE, SettingsKeys.SHOW_SCROLL_DATE)
            bind(Settings.SIGN_OUT_CLEANUP_PENDING, SettingsKeys.SIGN_OUT_CLEANUP_PENDING)
            bind(Settings.SIM_LARGE_LIBRARY_COUNT, SettingsKeys.SIM_LARGE_LIBRARY_COUNT)
            bind(Settings.STRIP_CAMERA_INFO, SettingsKeys.STRIP_CAMERA_INFO)
            bind(Settings.STRIP_GPS, SettingsKeys.STRIP_GPS)
            bind(Settings.STRIP_ON_SHARE, SettingsKeys.STRIP_ON_SHARE)
            bind(Settings.STRIP_ON_UPLOAD, SettingsKeys.STRIP_ON_UPLOAD)
            bind(Settings.STRIP_SHARE_AUTHORSHIP, SettingsKeys.STRIP_SHARE_AUTHORSHIP)
            bind(Settings.STRIP_SHARE_CAMERA_INFO, SettingsKeys.STRIP_SHARE_CAMERA_INFO)
            bind(Settings.STRIP_SHARE_GPS, SettingsKeys.STRIP_SHARE_GPS)
            bind(Settings.STRIP_SHARE_SOFTWARE_INFO, SettingsKeys.STRIP_SHARE_SOFTWARE_INFO)
            bind(Settings.STRIP_SHARE_TIMESTAMP, SettingsKeys.STRIP_SHARE_TIMESTAMP)
            bind(Settings.STRIP_SOFTWARE_INFO, SettingsKeys.STRIP_SOFTWARE_INFO)
            bind(Settings.STRIP_TIMESTAMP, SettingsKeys.STRIP_TIMESTAMP)
            bind(Settings.SYNC_FOLDER_NAMES, SettingsKeys.SYNC_FOLDER_NAMES)
            bind(Settings.SYNC_WIFI_ONLY, SettingsKeys.SYNC_WIFI_ONLY)
            bind(Settings.THEME_MODE, SettingsKeys.THEME_MODE)
            bind(Settings.THEME_PALETTE, SettingsKeys.THEME_PALETTE)
            bind(Settings.TIMELINE_EXCLUDED_ALBUM_IDS, SettingsKeys.TIMELINE_EXCLUDED_ALBUM_IDS)
            bind(Settings.TIMELINE_EXCLUDED_FOLDER_NAMES, SettingsKeys.TIMELINE_EXCLUDED_FOLDER_NAMES)
            bind(Settings.TINT_CLOUD_WITH_ACCENT, SettingsKeys.TINT_CLOUD_WITH_ACCENT)
            bind(Settings.UPDATE_BACKGROUND_CHECK, SettingsKeys.UPDATE_BACKGROUND_CHECK)
            bind(Settings.VAULT_PASSWORD_BLOB, SettingsKeys.VAULT_PASSWORD_BLOB)
            bind(Settings.VIEWER_LONG_PRESS_OPENS_MENU, SettingsKeys.VIEWER_LONG_PRESS_OPENS_MENU)
            bind(Settings.WHATS_NEW_SEEN_VERSION, SettingsKeys.WHATS_NEW_SEEN_VERSION)
        }.build(Settings.all)
}
