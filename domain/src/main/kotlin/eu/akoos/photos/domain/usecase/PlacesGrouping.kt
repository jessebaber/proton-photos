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

/** One located photo handed to the place grouping: its stable id, where it sits, and the item to show. */
data class LocatedItem(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val item: GalleryItem,
)

/** A reverse-geocode split into its parts, so the grouping can key a city by name plus country. */
data class PlaceLabelParts(val city: String, val countryCode: String, val countryName: String)

/** A city the account has photos in: name, country, a representative point, the photo count, and a cover
 *  (the newest photo taken there). */
data class PlaceCity(
    val city: String,
    val countryCode: String,
    val countryName: String,
    val latitude: Double,
    val longitude: Double,
    val count: Int,
    val cover: GalleryItem?,
)

/** A country the account has photos in: its ISO code and name, the total photo count, the number of
 *  distinct cities, and a cover. */
data class PlaceCountry(
    val countryCode: String,
    val countryName: String,
    val count: Int,
    val cityCount: Int,
    val cover: GalleryItem?,
)

/** The account's photos grouped both ways, for the countries level and the cities level. */
data class PlacesData(val countries: List<PlaceCountry>, val cities: List<PlaceCity>)

/**
 * Groups [items] into cities and then countries. Each item is placed by [labelOf] (its nearest city and
 * country); one [labelOf] cannot place is dropped. A city keys on its name plus its country code, so two
 * same-named towns in different countries stay apart; its point is the mean of its members and its cover
 * the newest member. A country folds its cities, taking the busiest city's cover. Both lists are sorted
 * by photo count descending so the busiest places lead. Pure and deterministic: ties break by name/code.
 */
fun groupPlaces(items: List<LocatedItem>, labelOf: (LocatedItem) -> PlaceLabelParts?): PlacesData {
    val byCity = LinkedHashMap<String, MutableList<Pair<LocatedItem, PlaceLabelParts>>>()
    for (item in items) {
        val parts = labelOf(item) ?: continue
        byCity.getOrPut(parts.city + "" + parts.countryCode) { ArrayList() }.add(item to parts)
    }
    val cities = byCity.values.map { members ->
        val parts = members.first().second
        PlaceCity(
            city = parts.city,
            countryCode = parts.countryCode,
            countryName = parts.countryName,
            latitude = members.sumOf { it.first.latitude } / members.size,
            longitude = members.sumOf { it.first.longitude } / members.size,
            count = members.size,
            cover = members.maxByOrNull { it.first.item.captureTimeMs }?.first?.item,
        )
    }.sortedWith(compareByDescending<PlaceCity> { it.count }.thenBy { it.city })

    val byCountry = LinkedHashMap<String, MutableList<PlaceCity>>()
    for (city in cities) byCountry.getOrPut(city.countryCode) { ArrayList() }.add(city)
    val countries = byCountry.map { (code, cs) ->
        PlaceCountry(
            countryCode = code,
            countryName = cs.first().countryName,
            count = cs.sumOf { it.count },
            cityCount = cs.size,
            // Cities here are in the count-sorted order they were added, so the first with a cover is
            // the busiest city's, a fair representative for the country.
            cover = cs.firstOrNull { it.cover != null }?.cover,
        )
    }.sortedWith(compareByDescending<PlaceCountry> { it.count }.thenBy { it.countryName })

    return PlacesData(countries, cities)
}
