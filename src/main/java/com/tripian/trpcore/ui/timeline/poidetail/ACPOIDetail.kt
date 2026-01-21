package com.tripian.trpcore.ui.timeline.poidetail

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.tripian.one.api.pois.model.Poi
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.AcPoiDetailBinding
import com.tripian.trpcore.ui.timeline.poidetail.adapter.POIFeatureTagAdapter
import com.tripian.trpcore.ui.timeline.poidetail.adapter.POIImageGalleryAdapter
import com.tripian.trpcore.ui.timeline.poidetail.adapter.POIOpeningHoursAdapter
import com.tripian.trpcore.ui.timeline.poidetail.adapter.POIProductCardAdapter
import com.tripian.trpcore.util.LanguageConst
import java.text.NumberFormat
import java.util.Locale

/**
 * ACPOIDetail
 * Full screen Activity for displaying POI details
 * Entry points: Timeline (manual_poi, itinerary steps), POI Listing
 */
class ACPOIDetail : BaseActivity<AcPoiDetailBinding, ACPOIDetailVM>() {

    private var galleryAdapter: POIImageGalleryAdapter? = null
    private var productAdapter: POIProductCardAdapter? = null
    private var openingHoursAdapter: POIOpeningHoursAdapter? = null
    private var featureTagAdapter: POIFeatureTagAdapter? = null

    override fun getViewBinding() = AcPoiDetailBinding.inflate(layoutInflater)

    override fun setListeners() {
        setupEdgeToEdge()
        setupBackButton()
        setupGallery()
        setupProductsRecyclerView()
        setupOpeningHoursRecyclerView()
        setupFeaturesRecyclerView()

        // Initialize from intent
        @Suppress("DEPRECATION")
        val poi = intent.getSerializableExtra(EXTRA_POI) as? Poi
        poi?.let {
            viewModel.initialize(it)
        }
    }

    override fun setReceivers() {
        // Observe POI data
        viewModel.poi.observe(this) { poi ->
            bindPoiData(poi)
        }

        // Observe description expanded state
        viewModel.isDescriptionExpanded.observe(this) { isExpanded ->
            binding.tvDescription.maxLines = if (isExpanded) Int.MAX_VALUE else 4
            binding.tvReadMore.text = if (isExpanded) {
                getLanguageForKey(LanguageConst.POI_DETAIL_CLOSE_FULL)
            } else {
                getLanguageForKey(LanguageConst.POI_DETAIL_READ_FULL)
            }
            // Add underline to text
            binding.tvReadMore.paintFlags = binding.tvReadMore.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            // Toggle chevron icon
            binding.ivReadMoreChevron.setImageResource(
                if (isExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down
            )
        }

        // Observe products
        viewModel.products.observe(this) { products ->
            productAdapter?.submitList(products)
        }

        // Observe opening hours
        viewModel.parsedOpeningHours.observe(this) { hours ->
            openingHoursAdapter?.submitList(hours)
        }

        // Observe section visibility
        viewModel.showActivitiesSection.observe(this) { show ->
            binding.llActivitiesSection.visibility = if (show) View.VISIBLE else View.GONE
        }

        viewModel.showKeyDataSection.observe(this) { show ->
            binding.llKeyDataSection.visibility = if (show) View.VISIBLE else View.GONE
        }

        viewModel.showPhoneRow.observe(this) { show ->
            binding.llPhoneRow.visibility = if (show) View.VISIBLE else View.GONE
        }

        viewModel.showOpeningHoursRow.observe(this) { show ->
            binding.llOpeningHoursRow.visibility = if (show) View.VISIBLE else View.GONE
        }

        viewModel.showMeetingPointSection.observe(this) { show ->
            binding.llMeetingPointSection.visibility = if (show) View.VISIBLE else View.GONE
        }

        viewModel.showFeaturesSection.observe(this) { show ->
            binding.llFeaturesSection.visibility = if (show) View.VISIBLE else View.GONE
        }

        // Set section header texts
        binding.tvActivitiesHeader.text = getLanguageForKey(LanguageConst.POI_DETAIL_ACTIVITIES)
        binding.tvKeyDataHeader.text = getLanguageForKey(LanguageConst.POI_DETAIL_KEY_DATA)
        binding.tvMeetingPointHeader.text = getLanguageForKey(LanguageConst.POI_DETAIL_MEETING_POINT)
        binding.tvFeaturesHeader.text = getLanguageForKey(LanguageConst.POI_DETAIL_FEATURES)
        binding.tvPhoneLabel.text = getLanguageForKey(LanguageConst.POI_DETAIL_PHONE)
        binding.tvOpeningHoursLabel.text = getLanguageForKey(LanguageConst.POI_DETAIL_OPENING_HOURS)
        binding.tvViewMap.text = getLanguageForKey(LanguageConst.POI_DETAIL_VIEW_MAP)
        binding.tvReadMore.text = getLanguageForKey(LanguageConst.POI_DETAIL_READ_FULL)
    }

    private fun setupEdgeToEdge() {
        // Make status bar transparent and draw behind it
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

        // Apply insets to back button so it's below the status bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.btnBack) { view, windowInsets ->
            val statusBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            val marginDp = 16
            val marginPx = (marginDp * resources.displayMetrics.density).toInt()
            view.updateLayoutParams<android.view.ViewGroup.MarginLayoutParams> {
                topMargin = statusBarInsets.top + marginPx
            }
            windowInsets
        }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupGallery() {
        galleryAdapter = POIImageGalleryAdapter()
        binding.vpGallery.adapter = galleryAdapter
        binding.vpGallery.orientation = ViewPager2.ORIENTATION_HORIZONTAL

        // Setup page indicator
        binding.vpGallery.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updatePageIndicator(position)
            }
        })
    }

    private fun setupProductsRecyclerView() {
        productAdapter = POIProductCardAdapter(
            getLanguage = { key -> getLanguageForKey(key) },
            onItemClicked = { product -> viewModel.onProductClicked(product) }
        )
        binding.rvProducts.apply {
            layoutManager = LinearLayoutManager(this@ACPOIDetail, LinearLayoutManager.HORIZONTAL, false)
            adapter = productAdapter
        }
    }

    private fun setupOpeningHoursRecyclerView() {
        openingHoursAdapter = POIOpeningHoursAdapter()
        binding.rvOpeningHours.apply {
            layoutManager = LinearLayoutManager(this@ACPOIDetail)
            adapter = openingHoursAdapter
        }
    }

    private fun setupFeaturesRecyclerView() {
        featureTagAdapter = POIFeatureTagAdapter()
        binding.rvFeatures.apply {
            layoutManager = FlexboxLayoutManager(this@ACPOIDetail).apply {
                flexDirection = FlexDirection.ROW
                flexWrap = FlexWrap.WRAP
                justifyContent = JustifyContent.FLEX_START
            }
            adapter = featureTagAdapter
        }
    }

    private fun bindPoiData(poi: Poi) {
        // City name
        val cityName = viewModel.getCityName()
        binding.tvCityName.text = cityName ?: ""
        binding.tvCityName.visibility = if (cityName.isNullOrBlank()) View.GONE else View.VISIBLE

        // POI name
        binding.tvPoiName.text = poi.name ?: ""

        // Rating
        val rating = poi.rating
        val reviewCount = poi.ratingCount
        val turkishLocale = Locale("tr", "TR")
        val reviewCountFormat = NumberFormat.getNumberInstance(turkishLocale)
        if (rating > 0) {
            binding.tvRating.text = String.format(turkishLocale, "%.1f", rating)

            if (reviewCount != null && reviewCount > 0) {
                val opinionsText = TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.ADD_PLAN_OPINIONS)
                binding.tvRatingCount.text = "${reviewCountFormat.format(reviewCount)} $opinionsText"
                binding.tvRatingCount.visibility = View.VISIBLE
            } else {
                binding.tvRatingCount.visibility = View.GONE
            }

            binding.llRating.visibility = View.VISIBLE
        } else {
            binding.llRating.visibility = View.GONE
        }

        // Description
        if (!poi.description.isNullOrBlank()) {
            binding.tvDescription.text = poi.description
            binding.tvDescription.visibility = View.VISIBLE
            // Set initial underline
            binding.tvReadMore.paintFlags = binding.tvReadMore.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            binding.tvDescription.post {
                val layout = binding.tvDescription.layout
                if (layout != null) {
                    val lineCount = layout.lineCount
                    // Check if text is truncated (ellipsis on last visible line)
                    val isTextTruncated = lineCount > 0 && layout.getEllipsisCount(lineCount - 1) > 0
                    binding.llReadMore.visibility = if (isTextTruncated) View.VISIBLE else View.GONE
                }
            }
            binding.llReadMore.setOnClickListener {
                viewModel.toggleDescription()
            }
        } else {
            binding.tvDescription.visibility = View.GONE
            binding.llReadMore.visibility = View.GONE
        }

        // Gallery
        val images = poi.gallery ?: listOfNotNull(poi.image)
        galleryAdapter?.submitList(images)
        binding.llPageIndicator.visibility = if (images.size > 1) View.VISIBLE else View.GONE
        if (images.size > 1) {
            setupPageIndicators(images.size)
        }

        // Phone
        binding.tvPhoneValue.text = poi.phone ?: ""
        binding.llPhoneRow.setOnClickListener {
            poi.phone?.let { phone ->
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = "tel:$phone".toUri()
                }
                startActivity(intent)
            }
        }

        // Address
        binding.tvAddress.text = poi.address ?: ""
        binding.tvAddress.visibility = if (poi.address.isNullOrBlank()) View.GONE else View.VISIBLE

        // Map
        poi.coordinate?.let { coord ->
            setupMap(coord.lat, coord.lng)
            binding.btnViewMap.setOnClickListener {
                openExternalMap(coord.lat, coord.lng, poi.name ?: "")
            }
        }

        // Features (tags)
        poi.tags?.let { tags ->
            featureTagAdapter?.submitList(tags)
        }
    }

    private fun setupPageIndicators(count: Int) {
        binding.llPageIndicator.removeAllViews()
        for (i in 0 until count) {
            val dot = View(this).apply {
                val size = if (i == 0) {
                    resources.getDimensionPixelSize(R.dimen.poi_indicator_active_width)
                } else {
                    resources.getDimensionPixelSize(R.dimen.poi_indicator_inactive_size)
                }
                val height = resources.getDimensionPixelSize(R.dimen.poi_indicator_inactive_size)
                layoutParams = android.widget.LinearLayout.LayoutParams(size, height).apply {
                    marginEnd = resources.getDimensionPixelSize(R.dimen.poi_indicator_margin)
                }
                background = if (i == 0) {
                    ContextCompat.getDrawable(context, R.drawable.bg_poi_page_indicator_active)
                } else {
                    ContextCompat.getDrawable(context, R.drawable.bg_poi_page_indicator)
                }
            }
            binding.llPageIndicator.addView(dot)
        }
    }

    private fun updatePageIndicator(position: Int) {
        for (i in 0 until binding.llPageIndicator.childCount) {
            val dot = binding.llPageIndicator.getChildAt(i)
            val isActive = i == position
            val size = if (isActive) {
                resources.getDimensionPixelSize(R.dimen.poi_indicator_active_width)
            } else {
                resources.getDimensionPixelSize(R.dimen.poi_indicator_inactive_size)
            }
            val height = resources.getDimensionPixelSize(R.dimen.poi_indicator_inactive_size)
            dot.layoutParams = android.widget.LinearLayout.LayoutParams(size, height).apply {
                marginEnd = resources.getDimensionPixelSize(R.dimen.poi_indicator_margin)
            }
            dot.background = if (isActive) {
                ContextCompat.getDrawable(this, R.drawable.bg_poi_page_indicator_active)
            } else {
                ContextCompat.getDrawable(this, R.drawable.bg_poi_page_indicator)
            }
        }
    }

    private fun setupMap(lat: Double, lng: Double) {
        binding.mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) { style ->
            // Set camera position
            binding.mapView.mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(lng, lat))
                    .zoom(14.0)
                    .build()
            )

            // Add marker
            val annotationApi = binding.mapView.annotations
            val pointAnnotationManager = annotationApi.createPointAnnotationManager()

            val markerBitmap = ContextCompat.getDrawable(this, R.drawable.ic_civi_point)?.let { drawable ->
                val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
                val canvas = android.graphics.Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }

            markerBitmap?.let { bitmap ->
                val pointAnnotationOptions = PointAnnotationOptions()
                    .withPoint(Point.fromLngLat(lng, lat))
                    .withIconImage(bitmap)
                pointAnnotationManager.create(pointAnnotationOptions)
            }
        }
    }

    private fun openExternalMap(lat: Double, lng: Double, label: String) {
        val uri = "geo:$lat,$lng?q=$lat,$lng($label)".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    companion object {
        const val EXTRA_POI = "extra_poi"

        /**
         * Create intent to launch POI Detail screen
         * @param context Context
         * @param poi Poi object to display
         * @return Intent to start ACPOIDetail
         */
        fun launch(context: Context, poi: Poi): Intent {
            return Intent(context, ACPOIDetail::class.java).apply {
                putExtra(EXTRA_POI, poi)
            }
        }
    }
}
