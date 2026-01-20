package com.tripian.trpcore.domain.model.itinerary

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Kullanıcının rezerve ettiği veya satın aldığı aktiviteler
 * Timeline'da bookedActivity veya reservedActivity segment olarak gösterilir.
 */
@Parcelize
data class SegmentActivityItem(
    val activityId: String? = null,         // Aktivite ID
    val bookingId: String? = null,          // Rezervasyon ID
    val title: String? = null,              // Başlık
    val imageUrl: String? = null,           // Görsel URL
    val description: String? = null,        // Açıklama
    val startDatetime: String? = null,      // "yyyy-MM-dd HH:mm"
    val endDatetime: String? = null,        // "yyyy-MM-dd HH:mm"
    val coordinate: ItineraryCoordinate,    // Konum
    val cancellation: String? = null,       // İptal politikası
    val adultCount: Int = 1,                // Yetişkin sayısı
    val childCount: Int = 0,                // Çocuk sayısı
    val bookingUrl: String? = null,         // Rezervasyon URL'i
    val duration: Double? = null,           // Süre (dakika)
    val price: SegmentActivityPrice? = null,// Fiyat
    val cityId: Int? = null                 // Şehir ID
) : Parcelable
