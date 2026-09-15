package com.tripian.trpcore.ui.timeline.compose.poidetail

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.onConsumedWindowInsetsChanged
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.core.os.BundleCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.tripian.one.api.pois.model.Poi
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.AcPoiDetailBinding
import com.tripian.trpcore.ui.timeline.compose.core.BindingHost
import com.tripian.trpcore.ui.timeline.compose.core.TimelineComposeScreen
import com.tripian.trpcore.ui.timeline.compose.core.timelineViewModel
import com.tripian.trpcore.ui.timeline.compose.nav.LocalTimelineNavigator
import com.tripian.trpcore.ui.timeline.compose.nav.PoiDetailArgs
import com.tripian.trpcore.ui.timeline.poidetail.ACPOIDetail
import com.tripian.trpcore.ui.timeline.poidetail.ACPOIDetailVM
import com.tripian.trpcore.ui.timeline.poidetail.adapter.POIImageGalleryAdapter
import com.tripian.trpcore.ui.timeline.poidetail.adapter.POIOpeningHoursAdapter
import com.tripian.trpcore.ui.timeline.poidetail.adapter.POIProductCardAdapter
import com.tripian.trpcore.util.LanguageConst

private const val PRODUCT_LOAD_MORE_THRESHOLD_DP = 100
private const val BACK_BUTTON_MARGIN_DP = 16
private const val STATE_SCROLL_Y = "state_scroll_y"
private const val STATE_GALLERY_PAGE = "state_gallery_page"
private const val STATE_PRODUCTS = "state_products"
private const val MAP_ZOOM = 14.0
private const val COLLAPSED_DESCRIPTION_LINES = 4

/**
 * Compose route of [ACPOIDetail]: full-screen POI details with gallery,
 * bookable products, key data and meeting point map.
 */
@Composable
internal fun PoiDetailScreen(
    args: PoiDetailArgs,
    viewModel: ACPOIDetailVM = timelineViewModel()
) {
    val navigator = LocalTimelineNavigator.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (viewModel.arguments == null) {
            viewModel.arguments =
                ACPOIDetail.launch(context, args.poi, args.tripStartDate, args.tripEndDate).extras
            viewModel.onViewCreated(null)
            viewModel.initialize(args.poi, args.tripStartDate, args.tripEndDate)
        }
    }

    val density = LocalDensity.current
    var consumedInsets by remember { mutableStateOf(WindowInsets(0, 0, 0, 0)) }
    val statusBarTopPx =
        (WindowInsets.statusBars.getTop(density) - consumedInsets.getTop(density)).coerceAtLeast(0)

    TimelineComposeScreen(viewModel = viewModel, onExit = { navigator.back() }) {
        Box(Modifier.matchParentSize().onConsumedWindowInsetsChanged { consumedInsets = it })
        BindingHost(
            inflate = AcPoiDetailBinding::inflate,
            update = { binding -> binding.applyBackButtonInset(statusBarTopPx) },
            onSaveViewState = { binding, state ->
                state.putInt(STATE_SCROLL_Y, binding.nsvContent.scrollY)
                state.putInt(STATE_GALLERY_PAGE, binding.vpGallery.currentItem)
                binding.rvProducts.layoutManager?.onSaveInstanceState()
                    ?.let { state.putParcelable(STATE_PRODUCTS, it) }
            }
        ) { binding, owner, viewState ->
            val galleryAdapter = POIImageGalleryAdapter()
            val productAdapter = POIProductCardAdapter(
                getLanguage = { key -> viewModel.getLanguageForKey(key) },
                onItemClicked = { product -> viewModel.onProductClicked(product) }
            )
            val openingHoursAdapter = POIOpeningHoursAdapter()
            binding.setupBackButton { navigator.back() }
            binding.setupGallery(galleryAdapter)
            binding.setupProducts(productAdapter, viewModel)
            binding.setupOpeningHours(openingHoursAdapter)
            binding.applyStaticTexts(viewModel)
            binding.observe(viewModel, owner, galleryAdapter, productAdapter, openingHoursAdapter, viewState)
        }
    }
}

private fun AcPoiDetailBinding.setupBackButton(onBack: () -> Unit) {
    btnBack.setOnClickListener { onBack() }
}

/** Keeps the floating back button below the status bar the host has not already padded for. */
private fun AcPoiDetailBinding.applyBackButtonInset(statusBarTopPx: Int) {
    val marginPx = (BACK_BUTTON_MARGIN_DP * root.resources.displayMetrics.density).toInt()
    btnBack.updateLayoutParams<ViewGroup.MarginLayoutParams> {
        topMargin = statusBarTopPx + marginPx
    }
}

private fun AcPoiDetailBinding.setupGallery(galleryAdapter: POIImageGalleryAdapter) {
    vpGallery.adapter = galleryAdapter
    vpGallery.orientation = ViewPager2.ORIENTATION_HORIZONTAL
    vpGallery.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            updatePageIndicator(position)
        }
    })
}

private fun AcPoiDetailBinding.setupProducts(
    productAdapter: POIProductCardAdapter,
    viewModel: ACPOIDetailVM
) {
    val loadMoreThresholdPx = (PRODUCT_LOAD_MORE_THRESHOLD_DP * root.resources.displayMetrics.density).toInt()
    rvProducts.layoutManager = LinearLayoutManager(root.context, LinearLayoutManager.HORIZONTAL, false)
    rvProducts.adapter = productAdapter
    rvProducts.addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (dx <= 0) return
            val remaining = recyclerView.computeHorizontalScrollRange() -
                recyclerView.computeHorizontalScrollExtent() -
                recyclerView.computeHorizontalScrollOffset()
            if (remaining <= loadMoreThresholdPx) {
                viewModel.loadMoreProducts()
            }
        }
    })
}

private fun AcPoiDetailBinding.setupOpeningHours(openingHoursAdapter: POIOpeningHoursAdapter) {
    rvOpeningHours.layoutManager = LinearLayoutManager(root.context)
    rvOpeningHours.adapter = openingHoursAdapter
}

private fun AcPoiDetailBinding.applyStaticTexts(viewModel: ACPOIDetailVM) {
    llFeaturesSection.visibility = View.GONE
    llCuisinesSection.visibility = View.GONE
    tvActivitiesHeader.text = viewModel.getLanguageForKey(LanguageConst.POI_DETAIL_ACTIVITIES)
    tvKeyDataHeader.text = viewModel.getLanguageForKey(LanguageConst.POI_DETAIL_KEY_DATA)
    tvMeetingPointHeader.text = viewModel.getLanguageForKey(LanguageConst.POI_DETAIL_MEETING_POINT)
    tvPhoneLabel.text = viewModel.getLanguageForKey(LanguageConst.POI_DETAIL_PHONE)
    tvOpeningHoursLabel.text = viewModel.getLanguageForKey(LanguageConst.POI_DETAIL_OPENING_HOURS)
    tvViewMap.text = viewModel.getLanguageForKey(LanguageConst.POI_DETAIL_VIEW_MAP)
    tvReadMore.text = viewModel.getLanguageForKey(LanguageConst.POI_DETAIL_READ_FULL)
}

private fun AcPoiDetailBinding.observe(
    viewModel: ACPOIDetailVM,
    owner: LifecycleOwner,
    galleryAdapter: POIImageGalleryAdapter,
    productAdapter: POIProductCardAdapter,
    openingHoursAdapter: POIOpeningHoursAdapter,
    viewState: Bundle
) {
    viewModel.poi.observe(owner) { poi ->
        bindPoiData(poi, viewModel, galleryAdapter, viewState)
    }
    viewModel.isDescriptionExpanded.observe(owner) { isExpanded ->
        tvDescription.maxLines = if (isExpanded) Int.MAX_VALUE else COLLAPSED_DESCRIPTION_LINES
        tvReadMore.text = viewModel.getLanguageForKey(
            if (isExpanded) LanguageConst.POI_DETAIL_CLOSE_FULL else LanguageConst.POI_DETAIL_READ_FULL
        )
        tvReadMore.paintFlags = tvReadMore.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        ivReadMoreChevron.setImageResource(
            if (isExpanded) R.drawable.trp_ic_chevron_up else R.drawable.trp_ic_chevron_down
        )
    }
    viewModel.products.observe(owner) { products ->
        productAdapter.submitList(products) { restoreProductsState(viewState) }
    }
    viewModel.isLoadingProducts.observe(owner) { isLoading ->
        renderProductsLoading(isLoading, productAdapter)
    }
    viewModel.parsedOpeningHours.observe(owner) { hours ->
        openingHoursAdapter.submitList(hours)
    }
    viewModel.showActivitiesSection.observe(owner) { show ->
        llActivitiesSection.visibility = if (show) View.VISIBLE else View.GONE
    }
    viewModel.showKeyDataSection.observe(owner) { show ->
        llKeyDataSection.visibility = if (show) View.VISIBLE else View.GONE
    }
    viewModel.showPhoneRow.observe(owner) { show ->
        llPhoneRow.visibility = if (show) View.VISIBLE else View.GONE
    }
    viewModel.showOpeningHoursRow.observe(owner) { show ->
        llOpeningHoursRow.visibility = if (show) View.VISIBLE else View.GONE
    }
    viewModel.showMeetingPointSection.observe(owner) { show ->
        llMeetingPointSection.visibility = if (show) View.VISIBLE else View.GONE
    }
}

private fun AcPoiDetailBinding.renderProductsLoading(
    isLoading: Boolean,
    productAdapter: POIProductCardAdapter
) {
    val hasProducts = productAdapter.itemCount > 0
    shimmerProductSkeleton.visibility = if (isLoading) View.VISIBLE else View.GONE
    rvProducts.visibility = if (isLoading && !hasProducts) View.GONE else View.VISIBLE
    if (isLoading) {
        shimmerProductSkeleton.startShimmer()
    } else {
        shimmerProductSkeleton.stopShimmer()
    }
}

private fun AcPoiDetailBinding.restoreProductsState(viewState: Bundle) {
    val state = BundleCompat.getParcelable(viewState, STATE_PRODUCTS, Parcelable::class.java) ?: return
    viewState.remove(STATE_PRODUCTS)
    rvProducts.layoutManager?.onRestoreInstanceState(state)
}

private fun AcPoiDetailBinding.bindPoiData(
    poi: Poi,
    viewModel: ACPOIDetailVM,
    galleryAdapter: POIImageGalleryAdapter,
    viewState: Bundle
) {
    val cityName = viewModel.getCityName()
    tvCityName.text = cityName.orEmpty()
    tvCityName.visibility = if (cityName.isNullOrBlank()) View.GONE else View.VISIBLE
    tvPoiName.text = poi.name.orEmpty()
    llRating.visibility = View.GONE

    bindDescription(poi, viewModel)

    val images = poi.gallery ?: listOfNotNull(poi.image)
    galleryAdapter.submitList(images) { restoreGalleryPage(viewState) }
    llPageIndicator.visibility = if (images.size > 1) View.VISIBLE else View.GONE
    if (images.size > 1) {
        setupPageIndicators(images.size)
    }

    tvPhoneValue.text = poi.phone.orEmpty()
    llPhoneRow.setOnClickListener {
        poi.phone?.let { phone ->
            root.context.startActivity(Intent(Intent.ACTION_DIAL).apply { data = "tel:$phone".toUri() })
        }
    }

    tvAddress.text = poi.address.orEmpty()
    tvAddress.visibility = if (poi.address.isNullOrBlank()) View.GONE else View.VISIBLE

    poi.coordinate?.let { coord ->
        setupMap(coord.lat, coord.lng)
        btnViewMap.setOnClickListener { openExternalMap(coord.lat, coord.lng, poi.name.orEmpty()) }
    }

    restoreScrollPosition(viewState)
}

private fun AcPoiDetailBinding.bindDescription(poi: Poi, viewModel: ACPOIDetailVM) {
    if (poi.description.isNullOrBlank()) {
        tvDescription.visibility = View.GONE
        llReadMore.visibility = View.GONE
        return
    }
    tvDescription.text = poi.description
    tvDescription.visibility = View.VISIBLE
    tvReadMore.paintFlags = tvReadMore.paintFlags or Paint.UNDERLINE_TEXT_FLAG
    tvDescription.post {
        val layout = tvDescription.layout ?: return@post
        val lineCount = layout.lineCount
        val isTextTruncated = lineCount > 0 && layout.getEllipsisCount(lineCount - 1) > 0
        llReadMore.visibility = if (isTextTruncated) View.VISIBLE else View.GONE
    }
    llReadMore.setOnClickListener { viewModel.toggleDescription() }
}

private fun AcPoiDetailBinding.restoreGalleryPage(viewState: Bundle) {
    if (!viewState.containsKey(STATE_GALLERY_PAGE)) return
    val page = viewState.getInt(STATE_GALLERY_PAGE)
    viewState.remove(STATE_GALLERY_PAGE)
    vpGallery.setCurrentItem(page, false)
}

private fun AcPoiDetailBinding.restoreScrollPosition(viewState: Bundle) {
    if (!viewState.containsKey(STATE_SCROLL_Y)) return
    val scrollY = viewState.getInt(STATE_SCROLL_Y)
    viewState.remove(STATE_SCROLL_Y)
    nsvContent.post { nsvContent.scrollTo(0, scrollY) }
}

private fun AcPoiDetailBinding.setupPageIndicators(count: Int) {
    llPageIndicator.removeAllViews()
    for (i in 0 until count) {
        val dot = View(root.context).apply {
            layoutParams = indicatorLayoutParams(isActive = i == 0)
            background = indicatorBackground(isActive = i == 0)
        }
        llPageIndicator.addView(dot)
    }
}

private fun AcPoiDetailBinding.updatePageIndicator(position: Int) {
    for (i in 0 until llPageIndicator.childCount) {
        val dot = llPageIndicator.getChildAt(i)
        val isActive = i == position
        dot.layoutParams = indicatorLayoutParams(isActive)
        dot.background = indicatorBackground(isActive)
    }
}

private fun AcPoiDetailBinding.indicatorLayoutParams(isActive: Boolean): LinearLayout.LayoutParams {
    val resources = root.resources
    val width = resources.getDimensionPixelSize(
        if (isActive) R.dimen.trp_poi_indicator_active_width else R.dimen.trp_poi_indicator_inactive_size
    )
    val height = resources.getDimensionPixelSize(R.dimen.trp_poi_indicator_inactive_size)
    return LinearLayout.LayoutParams(width, height).apply {
        marginEnd = resources.getDimensionPixelSize(R.dimen.trp_poi_indicator_margin)
    }
}

private fun AcPoiDetailBinding.indicatorBackground(isActive: Boolean) = ContextCompat.getDrawable(
    root.context,
    if (isActive) R.drawable.trp_bg_poi_page_indicator_active else R.drawable.trp_bg_poi_page_indicator
)

private fun AcPoiDetailBinding.setupMap(lat: Double, lng: Double) {
    mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) {
        mapView.mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(lng, lat))
                .zoom(MAP_ZOOM)
                .build()
        )
        val pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
        val markerBitmap = ContextCompat.getDrawable(root.context, R.drawable.trp_ic_civi_point)?.let { drawable ->
            val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
        markerBitmap?.let { bitmap ->
            pointAnnotationManager.create(
                PointAnnotationOptions()
                    .withPoint(Point.fromLngLat(lng, lat))
                    .withIconImage(bitmap)
            )
        }
    }
}

private fun AcPoiDetailBinding.openExternalMap(lat: Double, lng: Double, label: String) {
    val uri = "geo:$lat,$lng?q=$lat,$lng($label)".toUri()
    val intent = Intent(Intent.ACTION_VIEW, uri)
    if (intent.resolveActivity(root.context.packageManager) != null) {
        root.context.startActivity(intent)
    }
}
