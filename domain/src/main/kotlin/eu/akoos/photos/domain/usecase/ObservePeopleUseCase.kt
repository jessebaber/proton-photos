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

package eu.akoos.photos.domain.usecase

import eu.akoos.photos.domain.entity.GalleryItem
import eu.akoos.photos.domain.model.PersonSummary
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId


/**
 * Observes the account's clustered people and resolves each to a [PersonSummary]: cover face, display
 * name, face count and a normalised cover-face box. People arrive most-photographed first from
 * [PersonDao]; a person whose cover face no longer resolves, or whose cover photo has left the
 * library, is dropped, so the list matches the People rail the gallery has always shown. Reusable by
 * every people surface (the rail, an album picker, a person-detail screen) so the resolution and
 * ordering live in one place.
 *
 * The cover-face box is normalised against the cover photo's own dimensions, which the face and person
 * tables do not carry. The caller passes its already-loaded feed as [items] so the lookup reads
 * dimensions it already holds rather than re-decoding an image; a cover photo absent from that feed (a
 * cloud-only photo off the device) keeps a null box and its tile shows the whole cover. Recombined
 * with the feed, so a box appears once its cover photo loads and does not rebuild on a thumbnail
 * decrypt, since the feed changes only when photos are added or removed.
 *
 * Each list is built to the end before the next input is taken ([mapCompleting]): while a sync keeps the
 * feed and the person table changing faster than one build takes, a build cancelled by every new input
 * would never deliver a list at all. After each build it rests in proportion to what the build cost, so a
 * library that keeps changing (a sync, a grouping under way) never has the list rebuilt back to back.
 */
interface ObservePeopleUseCase {
    operator fun invoke(
        userId: UserId?,
        items: Flow<List<GalleryItem>>,
        namedOnly: Boolean = false,
    ): Flow<List<PersonSummary>>
}
