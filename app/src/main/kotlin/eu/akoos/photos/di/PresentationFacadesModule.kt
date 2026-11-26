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

package eu.akoos.photos.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import eu.akoos.photos.data.catalog.AlbumCatalogImpl
import eu.akoos.photos.data.catalog.ImportCatalogImpl
import eu.akoos.photos.data.catalog.LocationCatalogImpl
import eu.akoos.photos.data.catalog.PeopleCatalogImpl
import eu.akoos.photos.data.catalog.PhotoCatalogImpl
import eu.akoos.photos.data.catalog.SyncCatalogImpl
import eu.akoos.photos.data.hidden.VaultPasswordStore
import eu.akoos.photos.data.label.LabelBackupImpl
import eu.akoos.photos.data.label.PhotoLabelsImpl
import eu.akoos.photos.data.ml.FaceIndexingImpl
import eu.akoos.photos.data.ml.FaceModelsImpl
import eu.akoos.photos.data.ml.MlSyncImpl
import eu.akoos.photos.data.ml.OcrModelsImpl
import eu.akoos.photos.data.ml.SemanticIndexingImpl
import eu.akoos.photos.data.ml.SemanticModelsImpl
import eu.akoos.photos.data.offline.OfflineStorageManager
import eu.akoos.photos.data.repository.drive.ThumbnailUrlStore
import eu.akoos.photos.data.sharing.AlbumSharingImpl
import eu.akoos.photos.data.thumbnails.ThumbnailDecryptImpl
import eu.akoos.photos.data.transfer.TransfersImpl
import eu.akoos.photos.data.trash.CloudTrashImpl
import eu.akoos.photos.data.updater.StagedUpdatesImpl
import eu.akoos.photos.data.updater.UpdateDownloadImpl
import eu.akoos.photos.data.updater.UpdateInstallImpl
import eu.akoos.photos.data.vault.HiddenCloudImpl
import eu.akoos.photos.data.vault.HiddenFoldersImpl
import eu.akoos.photos.data.vault.HiddenStorageImpl
import eu.akoos.photos.data.vault.HiddenVaultEditImpl
import eu.akoos.photos.data.vault.HiddenVaultIndexImpl
import eu.akoos.photos.data.vault.HiddenVaultLeftoverImpl
import eu.akoos.photos.data.vault.HiddenVaultRestoreImpl
import eu.akoos.photos.data.work.BackgroundWorkImpl
import eu.akoos.photos.domain.catalog.AlbumCatalog
import eu.akoos.photos.domain.catalog.ImportCatalog
import eu.akoos.photos.domain.catalog.LocationCatalog
import eu.akoos.photos.domain.catalog.PeopleCatalog
import eu.akoos.photos.domain.catalog.PhotoCatalog
import eu.akoos.photos.domain.catalog.SyncCatalog
import eu.akoos.photos.domain.label.LabelBackup
import eu.akoos.photos.domain.label.PhotoLabels
import eu.akoos.photos.domain.ml.FaceIndexing
import eu.akoos.photos.domain.ml.FaceModels
import eu.akoos.photos.domain.ml.MlSync
import eu.akoos.photos.domain.ml.OcrModels
import eu.akoos.photos.domain.ml.SemanticIndexing
import eu.akoos.photos.domain.ml.SemanticModels
import eu.akoos.photos.domain.offline.OfflineStorage
import eu.akoos.photos.domain.sharing.AlbumSharing
import eu.akoos.photos.domain.thumbnails.ThumbnailDecrypt
import eu.akoos.photos.domain.thumbnails.ThumbnailUrls
import eu.akoos.photos.domain.transfer.Transfers
import eu.akoos.photos.domain.trash.CloudTrash
import eu.akoos.photos.domain.updater.StagedUpdates
import eu.akoos.photos.domain.updater.UpdateDownload
import eu.akoos.photos.domain.updater.UpdateInstall
import eu.akoos.photos.domain.vault.HiddenCloud
import eu.akoos.photos.domain.vault.HiddenFolders
import eu.akoos.photos.domain.vault.HiddenStorage
import eu.akoos.photos.domain.vault.HiddenVaultEdit
import eu.akoos.photos.domain.vault.HiddenVaultIndex
import eu.akoos.photos.domain.vault.HiddenVaultLeftover
import eu.akoos.photos.domain.vault.HiddenVaultRestore
import eu.akoos.photos.domain.vault.VaultPassword
import eu.akoos.photos.domain.work.BackgroundWork

import eu.akoos.photos.data.backup.SharedBackupImpl
import eu.akoos.photos.data.compression.VideoEncodersImpl
import eu.akoos.photos.data.debug.LargeLibrarySimImpl
import eu.akoos.photos.data.debug.SdkDebugRoutesImpl
import eu.akoos.photos.data.settings.AccountCleanupImpl
import eu.akoos.photos.data.vault.HiddenVaultMaintenanceImpl
import eu.akoos.photos.data.vault.VaultSignOutImpl
import eu.akoos.photos.domain.backup.SharedBackup
import eu.akoos.photos.domain.compression.VideoEncoders
import eu.akoos.photos.domain.debug.LargeLibrarySim
import eu.akoos.photos.domain.debug.SdkDebugRoutes
import eu.akoos.photos.domain.settings.AccountCleanup
import eu.akoos.photos.domain.vault.HiddenVaultMaintenance
import eu.akoos.photos.domain.vault.VaultSignOut
import javax.inject.Singleton

/**
 * Binds presentation-facing domain facades to their data implementations.
 * Wave 2 switches presentation injectables onto these interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PresentationFacadesModule {

    @Binds @Singleton abstract fun bindHiddenStorage(impl: HiddenStorageImpl): HiddenStorage
    @Binds @Singleton abstract fun bindVaultPassword(impl: VaultPasswordStore): VaultPassword
    @Binds @Singleton abstract fun bindHiddenCloud(impl: HiddenCloudImpl): HiddenCloud
    @Binds @Singleton abstract fun bindHiddenFolders(impl: HiddenFoldersImpl): HiddenFolders
    @Binds @Singleton abstract fun bindHiddenVaultIndex(impl: HiddenVaultIndexImpl): HiddenVaultIndex
    @Binds @Singleton abstract fun bindHiddenVaultEdit(impl: HiddenVaultEditImpl): HiddenVaultEdit
    @Binds @Singleton abstract fun bindHiddenVaultRestore(impl: HiddenVaultRestoreImpl): HiddenVaultRestore
    @Binds @Singleton abstract fun bindHiddenVaultLeftover(impl: HiddenVaultLeftoverImpl): HiddenVaultLeftover

    @Binds @Singleton abstract fun bindOfflineStorage(impl: OfflineStorageManager): OfflineStorage
    @Binds @Singleton abstract fun bindCloudTrash(impl: CloudTrashImpl): CloudTrash
    @Binds @Singleton abstract fun bindTransfers(impl: TransfersImpl): Transfers
    @Binds @Singleton abstract fun bindAlbumSharing(impl: AlbumSharingImpl): AlbumSharing

    @Binds @Singleton abstract fun bindStagedUpdates(impl: StagedUpdatesImpl): StagedUpdates
    @Binds @Singleton abstract fun bindUpdateDownload(impl: UpdateDownloadImpl): UpdateDownload
    @Binds @Singleton abstract fun bindUpdateInstall(impl: UpdateInstallImpl): UpdateInstall

    @Binds @Singleton abstract fun bindThumbnailUrls(impl: ThumbnailUrlStore): ThumbnailUrls
    @Binds @Singleton abstract fun bindThumbnailDecrypt(impl: ThumbnailDecryptImpl): ThumbnailDecrypt

    @Binds @Singleton abstract fun bindFaceIndexing(impl: FaceIndexingImpl): FaceIndexing
    @Binds @Singleton abstract fun bindSemanticIndexing(impl: SemanticIndexingImpl): SemanticIndexing
    @Binds @Singleton abstract fun bindFaceModels(impl: FaceModelsImpl): FaceModels
    @Binds @Singleton abstract fun bindOcrModels(impl: OcrModelsImpl): OcrModels
    @Binds @Singleton abstract fun bindSemanticModels(impl: SemanticModelsImpl): SemanticModels
    @Binds @Singleton abstract fun bindMlSync(impl: MlSyncImpl): MlSync

    @Binds @Singleton abstract fun bindPhotoLabels(impl: PhotoLabelsImpl): PhotoLabels
    @Binds @Singleton abstract fun bindLabelBackup(impl: LabelBackupImpl): LabelBackup

    @Binds @Singleton abstract fun bindPeopleCatalog(impl: PeopleCatalogImpl): PeopleCatalog
    @Binds @Singleton abstract fun bindPhotoCatalog(impl: PhotoCatalogImpl): PhotoCatalog
    @Binds @Singleton abstract fun bindAlbumCatalog(impl: AlbumCatalogImpl): AlbumCatalog
    @Binds @Singleton abstract fun bindLocationCatalog(impl: LocationCatalogImpl): LocationCatalog
    @Binds @Singleton abstract fun bindImportCatalog(impl: ImportCatalogImpl): ImportCatalog
    @Binds @Singleton abstract fun bindSyncCatalog(impl: SyncCatalogImpl): SyncCatalog

    @Binds @Singleton abstract fun bindBackgroundWork(impl: BackgroundWorkImpl): BackgroundWork

    @Binds @Singleton abstract fun bindSharedBackup(impl: SharedBackupImpl): SharedBackup
    @Binds @Singleton abstract fun bindVideoEncoders(impl: VideoEncodersImpl): VideoEncoders
    @Binds @Singleton abstract fun bindLargeLibrarySim(impl: LargeLibrarySimImpl): LargeLibrarySim
    @Binds @Singleton abstract fun bindSdkDebugRoutes(impl: SdkDebugRoutesImpl): SdkDebugRoutes
    @Binds @Singleton abstract fun bindAccountCleanup(impl: AccountCleanupImpl): AccountCleanup
    @Binds @Singleton abstract fun bindVaultSignOut(impl: VaultSignOutImpl): VaultSignOut
    @Binds @Singleton abstract fun bindHiddenVaultMaintenance(impl: HiddenVaultMaintenanceImpl): HiddenVaultMaintenance
}


