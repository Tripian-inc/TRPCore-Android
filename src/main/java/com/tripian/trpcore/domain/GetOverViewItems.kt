package com.tripian.trpcore.domain

import android.text.TextUtils
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.Butterfly
import com.tripian.trpcore.domain.model.ButterflyItem
import com.tripian.trpcore.domain.model.OverviewItem
import com.tripian.trpcore.repository.TripRepository
import com.tripian.trpcore.util.Category
import com.tripian.trpcore.util.extensions.formatDateWithShortName
import com.tripian.one.api.trip.model.isGenerated
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class GetOverViewItems @Inject constructor(val repository: TripRepository) : BaseUseCase<OverviewItem, GetOverViewItems.Params>() {

    val res by lazy {
        OverviewItem().apply {
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
                    if (plan.isGenerated()) {
                        dayTitle = "Day ${dayCount++}"

                        if (!res.items.any { TextUtils.equals(it.title, dayTitle) }) {
                            val rootItem = Butterfly()

                            rootItem.title = dayTitle
                            rootItem.date = plan.date?.formatDateWithShortName()
                            rootItem.items = ArrayList()

                            plan.steps?.forEach { step ->
                                if (step.poi?.category != null && step.poi!!.category!!.isNotEmpty()) {
                                    rootItem.items.add(ButterflyItem().apply {
                                        this.day = dayTitle
                                        this.step = step
                                        this.category = Category.values().find { it.id == step.poi!!.category!![0].id }
                                    })
                                }
                            }

                            rootItem.items.sortedWith(compareBy<ButterflyItem> { it.day }.thenBy { it.step!!.score })

                            res.items.add(rootItem)
                        }
                    }
                }

                res.generated = it.data.isGenerated()

                return@map res
            }
        }
    }
}