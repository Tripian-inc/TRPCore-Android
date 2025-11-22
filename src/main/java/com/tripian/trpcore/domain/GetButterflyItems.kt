package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.Butterfly
import com.tripian.trpcore.domain.model.ButterflyItem
import com.tripian.trpcore.repository.TripRepository
import com.tripian.trpcore.util.Category
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class GetButterflyItems @Inject constructor(val repository: TripRepository) : BaseUseCase<List<Butterfly>, GetButterflyItems.Params>() {

    val res = ArrayList<Butterfly>()

    private val attraction by lazy {
        Butterfly().apply {
//            title = strings.getString(R.string.attractions)
            items = ArrayList()
        }
    }

    private val restaurant by lazy {
        Butterfly().apply {
//            title = strings.getString(R.string.restaurants)
            items = ArrayList()
        }
    }

    private val cafe by lazy {
        Butterfly().apply {
//            title = strings.getString(R.string.cafes)
            items = ArrayList()
        }
    }

    private val nightlife by lazy {
        Butterfly().apply {
//            title = strings.getString(R.string.nightlife)
            items = ArrayList()
        }
    }

    class Params(val tripHash: String)

    override fun on(params: Params?) {
        addObservable {
            repository.fetchTrip(params!!.tripHash).map {

                var dayCount = 1
                var dayTitle = ""

                it.data?.plans?.forEach { plan ->
                    dayTitle = "Day ${dayCount++}"

                    plan.steps?.forEach { step ->
                        if (step.poi?.category != null && step.poi!!.category!!.isNotEmpty()) {
                            when (step.poi!!.category!![0].id) {
                                Category.ATTRACTIONS.id,
                                Category.RELIGIOUS_PLACE.id,
                                Category.MUSEUM.id,
                                Category.ART_GALLERY.id -> {
                                    if (!attraction.items.any { it.step?.id == step.id }) {
                                        val item = ButterflyItem()
                                        item.category = Category.ATTRACTIONS
                                        item.day = dayTitle
                                        item.step = step

                                        attraction.items.add(item)
                                    }
                                }
                                Category.RESTAURANT.id -> {
                                    if (!restaurant.items.any { it.step?.id == step.id }) {
                                        val item = ButterflyItem()
                                        item.category = Category.RESTAURANT
                                        item.day = dayTitle
                                        item.step = step

                                        restaurant.items.add(item)
                                    }
                                }
                                Category.CAFE.id,
                                Category.BAKERY.id -> {
                                    if (!cafe.items.any { it.step?.id == step.id }) {
                                        val item = ButterflyItem()
                                        item.category = Category.CAFE
                                        item.day = dayTitle
                                        item.step = step

                                        cafe.items.add(item)
                                    }
                                }
                                Category.NIGHTLIFE.id,
                                Category.BAR.id,
                                Category.BREWERY.id -> {
                                    if (!nightlife.items.any { it.step?.id == step.id }) {
                                        val item = ButterflyItem()
                                        item.category = Category.NIGHTLIFE
                                        item.day = dayTitle
                                        item.step = step

                                        nightlife.items.add(item)
                                    }
                                }
                            }
                        }
                    }
                }

                if (attraction.items.size > 0 && !attraction.isAdded) {
                    attraction.items.sortedWith(compareBy<ButterflyItem> { it.day }.thenBy { it.step!!.score })

                    res.add(attraction)

                    attraction.isAdded = true
                }

                if (restaurant.items.size > 0 && !restaurant.isAdded) {
                    restaurant.items.sortedWith(compareBy<ButterflyItem> { it.day }.thenBy { it.step!!.score })

                    res.add(restaurant)

                    restaurant.isAdded = true
                }

                if (cafe.items.size > 0 && !cafe.isAdded) {
                    cafe.items.sortedWith(compareBy<ButterflyItem> { it.day }.thenBy { it.step!!.score })

                    res.add(cafe)

                    cafe.isAdded = true
                }

                if (nightlife.items.size > 0 && !nightlife.isAdded) {
                    nightlife.items.sortedWith(compareBy<ButterflyItem> { it.day }.thenBy { it.step!!.score })

                    res.add(nightlife)

                    nightlife.isAdded = true
                }

                return@map res
            }
        }
    }
}