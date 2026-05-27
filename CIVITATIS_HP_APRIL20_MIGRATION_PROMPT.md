# Civitatis-HP-April20 → Android Migration Prompt

> **Hedef:** iOS `TRPCoreKit-SPM` projesinin `civitatis-hp-april20` branch'inde, `civitatis` branch'inden sonra yapılan tüm geliştirmeleri Android `TRPCore` projesine (`com.tripian.trpcore`) **hatasız** olarak aktarmak.
>
> **Önemli:** `civitatis` branch'ine kadar olan tüm yapı Android'de zaten mevcut. Yalnızca aşağıda listelenen 23 commit'in getirdiği yenilikleri uygulayacaksın. Başka hiçbir şeyi yeniden yazma, refactor etme.

---

## 0. Referans, Kurallar ve Çalışma Şekli

### Kaynak Referans Yolu (iOS)
Bu Mac'te iOS projesi şurada: `/Users/cemcaygoz/Documents/Tripian Works/Tripian One/TRPCoreKit-SPM/TRPCoreKit-SPM`

- Doğru branch: `civitatis-hp-april20` (varsayılan görünür)
- Karşılaştırma tabanı: `civitatis`
- İçerikten emin değilsen, ilgili Swift dosyasını oku — listelenen tüm dosyalar bu path altında.
- Spesifik değişikliği görmek için:
  ```bash
  cd "/Users/cemcaygoz/Documents/Tripian Works/Tripian One/TRPCoreKit-SPM/TRPCoreKit-SPM"
  git diff civitatis..civitatis-hp-april20 -- <path>
  git show <commit-hash> -- <path>
  ```

### Android Tarafı Çalışma Kuralları (CLAUDE.md'den)
- **Min SDK 24, Kotlin 2.0.21, Java 17, Dagger 2, RxJava 2, Mapbox 11**
- **MVVM + Clean Architecture**: `ui/ → domain/usecase/ → repository/ → TRPOne SDK`
- **ViewBinding zorunlu**; `BaseActivity<VB, VM>` / `BaseFragment<VB, VM>` / `BaseViewModel` / `BaseUseCase<Response, Params>` kullan
- **strings.xml ASLA kullanma**. Tüm metinler `LanguageConst.*` üzerinden:
  - VM/Fragment/Activity: `viewModel.getLanguageForKey(LanguageConst.XXX)` veya `getLanguageForKey(...)`
  - Adapter/ViewHolder: `TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.XXX)`
  - XML preview için sadece `tools:text` kullan
- **Test yazma** (kullanıcı tercihi). Mevcut testlere dokunma.
- **Build çalıştırma**: User izin vermedikçe `./gradlew` çağırma.
- **DI yeni feature**: Scope + Module + `ViewPages.kt`'ye dahil et + `ViewModels.kt`'de bind et.
- Senior Android developer seviyesinde, hatasız kod yaz.

### iOS Konseptlerinin Android Karşılıkları
| iOS | Android |
|---|---|
| `ValueObserver<T>` | `LiveData<T>` veya `BehaviorSubject<T>` (RxJava 2) |
| `protocol X: ViewModelDelegate` | `interface X` + ViewModel'de `LiveData` |
| `weak var delegate` | `interface` callback + WeakReference (gerekirse) |
| UITableViewCell | RecyclerView ViewHolder + multi-type adapter |
| UICollectionView | RecyclerView (horizontal LayoutManager) |
| `present(_:animated:)` (sheet) | `BottomSheetDialogFragment` |
| `present(_:animated:)` (full) | `Activity` veya `DialogFragment(STYLE_NORMAL, R.style.FullScreenDialog)` |
| `UIView+addSubview` Auto Layout | `ConstraintLayout` |
| Singleton (`.shared`) | `object` veya `@Singleton` Dagger provider |
| Codable JSON | Gson (`@SerializedName`) |
| `Bundle.module` resource | `res/raw/` veya `res/drawable/` |

### Çalışma Akışı
1. **Önce ilgili iOS dosyasını oku** (Read tool).
2. Android tarafında ilgili dosyaları bul (Glob/Grep).
3. **Mevcut Android implementasyonu varsa onu güncelle**, yoksa CLAUDE.md'deki naming convention'a göre yeni dosya oluştur.
4. Her özelliği uyguladıktan sonra commit etmeden bekle (user commit kararını verecek).
5. Tüm değişiklikleri **bir feature branch**'inde topla (örn: `feature/civitatis-hp-april20-parity`).

---

## 1. Tema Listesi (23 Commit, Bağımlılık Sırasına Göre)

Aşağıdaki sıralamayla uygula. Tema bağımlılığı: her tema kendinden öncekilere dayanabilir.

1. **Lottie Loading System** — yeni loader API'sinin altyapısı
2. **Tour Facets, Slots & Category Icons** — yeni tour modelleri
3. **Past Day Handling** — UI genelinde geçmiş gün desteği
4. **Flexible-Time Activities** — yeni cell tipi ve UI
5. **Tour Schedule Availability & Product Lookup** — yeni API'ler
6. **No-Location Support** — koordinatı olmayan aktiviteler
7. **Time Badge Warning Status Row** — badge'e durum satırı
8. **Conflict Detection & Banner** — çakışma tespiti
9. **Time Selection Enhancements** — "Show more" + sold-out banner
10. **Timeline Refresh State** — global refresh observable
11. **POI Sort & POI Listing Improvements** — sıralama + skeleton + sold-out
12. **Collapsible Section Headers** — başlık daraltma
13. **Manual POIs & Optional Step Handling** — manual POI iyileştirme
14. **Layout Spacing & U+2212 Minus** — badge metni `−` (U+2212)
15. **POI Cards Toggle on Map Tap** — harita preview toggle
16. **AddPlan Scrolling & Day Selection Fixes** — bug fix'ler
17. **Availability Sweep (Post-Load Check)** — timeline yüklendikten sonra availability taraması

---

## 2. THEMES

### Theme 1: Lottie Loading System

> **iOS commit'ler:** `33d5297`, `7539929`, `ea182ab`, `ad5ced6`, `b8c3c55`

#### Amaç
Tüm long-running operasyonlarda (timeline yaratma, segment yaratma, step ekleme, listeleme refresh) gösterilecek, hem full-screen hem bottom-sheet hem in-view modlarında çalışabilen bir Lottie tabanlı loading ekranı. Full-screen modda metinler 3.5 saniyede bir döner ("Organizando tu itinerario...", "Buscando experiencias..." vb.).

#### iOS Referans Dosyalar
- `TRPCoreKit/TRPUIKit/TRPLoader/TRPLottieLoadingVC.swift` (yeni, ~535 satır)
- `TRPCoreKit/Resources/Animations/loader.json` (Lottie JSON — Android'de aynı dosyayı kullan)
- `TRPCoreKit/Resources/Animations/loading_animation.json` (alternatif)
- `TRPCoreKit/ViewController/TimelineItinerary/LoadingLocalizationKeys.swift` (yeni — tüm loading metinleri)
- `TRPCoreKit/Protocols/ViewModelDelegate.swift` (Lottie API eklendi)
- `TRPCoreKit/ViewController/Common/TRPBaseUIViewController.swift` (helper'lar)
- `TRPCoreKit/ViewController/SplashViewController.swift` (entegrasyon örneği)
- `TRPCoreKit/Coordinaters/TRPTimelineCoordinator.swift` (entegrasyon örneği)

#### Android'de Yapılacaklar

**1. Dependency ekle** — `build.gradle`:
```gradle
implementation 'com.airbnb.android:lottie:6.6.0'
```

**2. Lottie JSON dosyalarını kopyala**:
- Kaynak: `iOS/TRPCoreKit/Resources/Animations/loader.json` ve `loading_animation.json`
- Hedef: `src/main/res/raw/loader.json` ve `src/main/res/raw/loading_animation.json`

**3. `LoadingLocalizationKeys` Android karşılığı** — `util/LanguageConsts.kt`'ye ekle. iOS'taki `LoadingLocalizationKeys.swift`'i oku ve tüm key'leri kopyala (yaklaşık 12-15 rotating text + bottom-sheet için statik metinler). Örnek key isimleri:
```
LOADING_ROTATING_ORGANIZING     = "loading.rotating.organizing"
LOADING_ROTATING_FINDING        = "loading.rotating.findingExperiences"
LOADING_ROTATING_PREPARING      = "loading.rotating.preparingRecommendations"
LOADING_ROTATING_FINALIZING     = "loading.rotating.finalizing"
LOADING_BOTTOM_REMOVING_ACTIVITY = "loading.bottom.removingActivity"
LOADING_BOTTOM_UPDATING_TIME    = "loading.bottom.updatingTime"
LOADING_BOTTOM_ADDING_ACTIVITY  = "loading.bottom.addingActivity"
LOADING_BOTTOM_REFRESHING       = "loading.bottom.refreshing"
... (iOS dosyasından TÜMÜNÜ kopyala)
```

**4. `LottieLoadingDialog` — yeni dosya** `ui/common/LottieLoadingDialog.kt`:
- Tipler:
  ```kotlin
  enum class LottieLoadingPresentation {
      FULL_SCREEN,    // Activity üstüne overlay; metin rotation aktif
      BOTTOM_SHEET    // BottomSheetDialogFragment; tek metin
  }

  sealed class LottieLoadingText {
      object None : LottieLoadingText()
      data class Single(val text: String) : LottieLoadingText()
      data class Rotating(val texts: List<String>) : LottieLoadingText() {
          companion object {
              fun defaultRotating(): Rotating = Rotating(
                  listOf(
                      LanguageConst.LOADING_ROTATING_ORGANIZING,
                      LanguageConst.LOADING_ROTATING_FINDING,
                      LanguageConst.LOADING_ROTATING_PREPARING,
                      LanguageConst.LOADING_ROTATING_FINALIZING
                  ).map { TRPCore.core.miscRepository.getLanguageValueForKey(it) }
              )
          }
      }
  }
  ```
- Sınıflar:
  - `LottieFullScreenDialog : DialogFragment` — `R.style.FullScreenTransparentDialog` ile gösterilir; arka plan yarı saydam siyah (alpha ~0.5); 120dp `LottieAnimationView` (`loader.json`, infinite loop); altında 16sp text. Rotating modda 3.5 sn'lik Handler ile metni değiştir, son metin sabit kalır.
  - `LottieBottomSheetDialog : BottomSheetDialogFragment` — Sticky, drag disabled, 200dp height; 120dp Lottie + statik tek satır metin. State `STATE_EXPANDED` zorla.
- Public API (singleton helper):
  ```kotlin
  object LottieLoading {
      fun show(activity: FragmentActivity, presentation: LottieLoadingPresentation, text: LottieLoadingText): String  // tagId döner
      fun hide(activity: FragmentActivity, tagId: String? = null)  // tagId nullsa hepsini kapat
  }
  ```
- "Already presenting" çakışmasını önle: `supportFragmentManager` üzerinden mevcut tag'leri kontrol et; aynı tag varsa yenisini ekleme.

**5. `BaseViewModel` ve `ViewModelDelegate` paritesi**:
- iOS'ta `protocol ViewModelDelegate` üzerinden `showLottieLoading(_:textMode:presentation:)` eklendi. Android tarafında `BaseViewModel`'da iki yeni LiveData/Event ekle:
  ```kotlin
  data class LottieLoadingEvent(
      val show: Boolean,
      val presentation: LottieLoadingPresentation = LottieLoadingPresentation.FULL_SCREEN,
      val text: LottieLoadingText = LottieLoadingText.Rotating.defaultRotating()
  )

  protected val _lottieLoadingEvent = SingleLiveEvent<LottieLoadingEvent>()
  val lottieLoadingEvent: LiveData<LottieLoadingEvent> = _lottieLoadingEvent

  fun showLottieLoading(presentation: LottieLoadingPresentation, text: LottieLoadingText = LottieLoadingText.Rotating.defaultRotating()) {
      _lottieLoadingEvent.postValue(LottieLoadingEvent(true, presentation, text))
  }

  fun hideLottieLoading() {
      _lottieLoadingEvent.postValue(LottieLoadingEvent(false))
  }
  ```
- `BaseActivity` ve `BaseFragment`'da bunu observe edip `LottieLoading.show/hide` çağır.
- Mevcut "preloader" (basit spinner) çağrıları **olduğu gibi kalsın**, sadece Lottie yeni operasyonlar için kullanılacak.

**6. Entegrasyon noktaları** (iOS'ta hangi yerlere eklendiyse, Android'de aynısı):
- `ACTimelineVM.kt` — Timeline ilk fetch + create: `showLottieLoading(FULL_SCREEN, Rotating.defaultRotating())`
- `AddPlanContainerVM.kt` — Smart segment creation: `showLottieLoading(FULL_SCREEN, ...)`
- Manual POI / Activity segment creation (TimelineVM): aynı şekilde
- Step delete / edit-time / add-step: `BOTTOM_SHEET` + uygun bottom key (örn. `LOADING_BOTTOM_REMOVING_ACTIVITY`)
- Saved Plan → Timeline ekleme: `BOTTOM_SHEET` + `LOADING_BOTTOM_ADDING_ACTIVITY`
- AddPlanActivityListing / POIListing: kategori değişimi (sort/filter inline skeleton **değil** full-screen değil) için `BOTTOM_SHEET` + `LOADING_BOTTOM_REFRESHING`
- Splash: ilk login/auth aşamasında `FULL_SCREEN` + `Rotating.defaultRotating()`

**7. Loader API birleşimi (`b8c3c55` commit)**:
- Mevcut `showProgress() / hideProgress()` (basit spinner) korunur **ancak** "long-running" işlemler artık `showLottieLoading(...)` kullanır.
- `BaseViewModel`'a kolaylık fonksiyonu ekle:
  ```kotlin
  fun showRefreshLoader() = showLottieLoading(
      LottieLoadingPresentation.BOTTOM_SHEET,
      LottieLoadingText.Single(TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.LOADING_BOTTOM_REFRESHING))
  )
  ```

#### Doğrulama
- Manual POI eklerken full-screen Lottie + dönen metinler gösterilmeli.
- Step silerken bottom-sheet Lottie + tek satır metin gösterilmeli.
- Çift gösterim sorunu olmamalı (rapid tap'lerde tek dialog).

---

### Theme 2: Tour Facets, Slots & Category Icons

> **iOS commit:** `a881ba2`

#### Amaç
Tour arama sonuçlarına faceted filter desteği (kategori, fiyat range, süre range) eklemek; her tour search response'unda `slots` listesi (search anında availability ipucu) ve `facets` listesi ile dönen kategori chip'lerini AddPlan Activity Listing ekranında göstermek.

#### iOS Referans Dosyalar
- `TRPCoreKit/TRPDataLayer/Domain/Models/Tour/TRPTourFacets.swift` (yeni)
- `TRPCoreKit/TRPDataLayer/Domain/Models/Tour/TRPTourSlot.swift` (yeni)
- `TRPCoreKit/TRPDataLayer/Domain/Models/Tour/TRPTourProduct.swift` (slots field eklendi)
- `TRPCoreKit/TRPDataLayer/Domain/Mappers/Tour/TourMapper.swift` (facets+slots map'leme)
- `TRPCoreKit/TRPDataLayer/Data/RemoteApi/Tour/TRPTourRemoteApi.swift` (response handling)
- `TRPCoreKit/TRPDataLayer/Data/Repositories/Tour/TRPTourRepository.swift`
- `TRPCoreKit/Utility/TRPTourCategoryIconMapper.swift` (yeni — key → asset adı)
- `TRPCoreKit/Assets.xcassets/ic_cat_*.imageset/` — yeni kategori ikonları (`ic_cat_actions`, `ic_cat_experiences`, `ic_cat_passes`, `ic_cat_services`)
- `TRPCoreKit/ViewController/TimelineItinerary/AddPlan/ActivityListing/AddPlanActivityListingVC.swift`
- `TRPCoreKit/ViewController/TimelineItinerary/AddPlan/ActivityListing/AddPlanActivityListingViewModel.swift`
- `TRPCoreKit/ViewController/TimelineItinerary/AddPlan/ActivityListing/ActivityCardCell.swift`
- `TRPCoreKit/ViewController/TimelineItinerary/AddPlan/ActivityListing/AddPlanFilterVC.swift`

#### Android'de Yapılacaklar

**1. Domain modelleri** — `domain/model/tour/`:
```kotlin
data class TourFacets(
    val categories: List<TourCategoryFacet>,
    val priceRange: TourPriceRangeFacet?,
    val durationRange: TourDurationRangeFacet?
)

data class TourCategoryFacet(
    val id: String,
    val key: String?,    // örn: "activity_main_category_1"
    val label: String,
    val count: Int
)

data class TourPriceRangeFacet(
    val minAmount: Double,
    val maxAmount: Double,
    val currency: String
)

data class TourDurationRangeFacet(
    val minMinutes: Int,
    val maxMinutes: Int
)

data class TourSlot(
    val date: String,        // "yyyy-MM-dd"
    val time: String?,       // "HH:mm" veya null (flexible)
    val price: Double?
) {
    val isFlexible: Boolean get() = time == null
}

data class TourSearchOutcome(
    val products: List<TourProduct>,
    val facets: TourFacets?,
    val pagination: Pagination?
)
```

**2. `TourProduct` model güncellemesi** — `slots: List<TourSlot>?` field'ı ekle (search response'unda gelebilir). `repository/model/TourProductModel.kt` veya yeni domain modeli neyse o.

**3. Mapper** — `domain/mapper/tour/TourMapper.kt` (veya repository içinde):
- Backend response → domain mapping. Slots ve facets'i map'le. Eğer slot'un `time` field'ı response'da yoksa `null` bırak. iOS `TourMapper.swift`'i oku ve aynı mantığı uygula.

**4. `TRPTourCategoryIconMapper` Android karşılığı** — `util/TourCategoryIconMapper.kt`:
```kotlin
object TourCategoryIconMapper {
    const val ALL_CATEGORIES_ICON = R.drawable.ic_all_categories

    fun iconRes(key: String?): Int = when (key) {
        "activity_main_category_1"  -> R.drawable.ic_activities
        "activity_main_category_2"  -> R.drawable.ic_cat_excursions
        "activity_main_category_9"  -> R.drawable.ic_cat_food_drinks
        "activity_main_category_3"  -> R.drawable.ic_cat_passes
        "activity_main_category_4"  -> R.drawable.ic_cat_services
        "activity_main_category_5"  -> R.drawable.ic_cat_actions
        "activity_main_category_6"  -> R.drawable.ic_cat_shows
        // iOS dosyasındaki TÜM mapping'leri kopyala
        else -> R.drawable.ic_cat_experiences
    }
}
```

**5. Yeni drawable'lar** — iOS'taki SVG'leri (`ic_cat_actions.svg`, `ic_cat_experiences.svg`, `ic_cat_passes.svg`, `ic_cat_services.svg`) vector drawable'a çevir veya elindeki tasarımlardan ekle:
- `src/main/res/drawable/ic_cat_actions.xml`
- `src/main/res/drawable/ic_cat_experiences.xml`
- `src/main/res/drawable/ic_cat_passes.xml`
- `src/main/res/drawable/ic_cat_services.xml`
- (Diğer mevcut `ic_cat_*` zaten varsa dokunma.)

**6. Repository / UseCase güncellemesi**:
- `TimelineRepository.searchTours(...)` veya tour repository → `Observable<TourSearchOutcome>` döndürsün (artık sadece `List<TourProduct>` değil).
- Yeni `SearchToursUseCase` veya mevcut UseCase'i güncelle: response `TourSearchOutcome` döndürmeli.
- Geriye dönük uyumluluk: eski caller'lar facets'i kullanmıyorsa null/empty geçilebilir.

**7. AddPlan Activity Listing UI** (en kritik kısım) — `ui/timeline/addplan/activity/`:
- ViewModel'de yeni state:
  ```kotlin
  var searchText: String = ""
  var selectedSortOption: SortOption = SortOption.POPULARITY
  var filterData: FilterData = FilterData()
  var selectedFacetCategoryIds: MutableSet<String> = mutableSetOf()
  var facetCategories: List<TourCategoryFacet> = emptyList()
  var priceRangeFacet: TourPriceRangeFacet? = null
  var durationRangeFacet: TourDurationRangeFacet? = null
  ```
- Kategori chip'leri için yatay RecyclerView. Adapter:
  - Index 0: "All" chip — seçili kategoriyi temizler (set boş).
  - Index 1+: facet'lerden gelen kategoriler. `TourCategoryIconMapper.iconRes(facet.key)` ile ikon.
  - Seçim: tek seçim **değil**, multi-toggle. (Tüm seçili olunca "All" deselect.)
- Çoklu chip seçimi → API'ye yeni search yapılır. Her chip değişiminde search retrigger.
- Filter ekranı (`AddPlanFilterVC`):
  - Price range slider: bounds `priceRangeFacet`'ten gelir, yoksa 0–1500 fallback.
  - Duration range slider: bounds `durationRangeFacet`'ten gelir, yoksa 0–1440 (24 saat) fallback.
  - "Apply" tıklanınca filterData güncellenir → search retrigger.

**8. ActivityCardCell güncellemesi** (Android: ViewHolder + item XML):
- iOS'ta cell layout'u büyük ölçüde değişti — image (16dp corner), title, rating row, duration row, price (sağ alt).
- iOS dosyasını oku, layout'u XML'e çevir. uniqueId (productId formatı `C_{productId}_{providerId}`) support.
- Fiyat 0 ise **"FREE"** göster (localized: `ACTIVITY_LISTING_FREE`).

**9. Search retrigger debounce**: search text değişiminde 300ms debounce ile API çağrısı. Mevcut implementasyonda zaten varsa bozma.

#### Doğrulama
- AddPlan Activity Listing'de yatay kategori chip'leri görünmeli; "All" + facet kategoriler.
- Chip tıklayınca filtrelenmiş sonuç gelmeli.
- Filter modalında price ve duration range slider çalışmalı, facet bounds ile başlamalı.
- Fiyatı 0 olan tour'larda "FREE" yazmalı.

---

### Theme 3: Past Day Handling

> **iOS commit:** `c76c60c`

#### Amaç
Geçmiş günler (selected day < today) Timeline ve AddPlan'da tutarlı şekilde işaretlenmeli:
- **Timeline modu:** Past gün metinleri/butonları soluklaştırılır (gri) ama tıklanabilir kalır; cell'lerdeki aksiyon button handler'ları no-op olur.
- **AddPlan modu:** Past gün day filter'da grileşir ve **seçilemez** (tap görmezden gelinir).

#### iOS Referans Dosyalar
- `TRPCoreKit/Extension/Date+Extensions.swift` — `isPastDay()` helper'ı (gün bazlı, saat görmezden gelinir)
- `TRPCoreKit/Extension/UIView+Extensions.swift` — `trp_recolorLabelsAndBorders(to:)` recursive helper
- `TRPCoreKit/ViewController/TimelineItinerary/TRPTimelineDayFilterView.swift` — `Mode { case timeline, addPlan }`
- `TRPCoreKit/ViewController/TimelineItinerary/Cells/*` — tüm cell'lere `isPastDayMode: Bool` flag eklendi
- `TRPCoreKit/ViewController/TimelineItinerary/TRPTimelineItineraryViewModel.swift` — `isSelectedDayPast: Bool`
- `TRPCoreKit/ViewController/TimelineItinerary/TRPTimelineItineraryViewModel+DataProcessing.swift`
- `TRPCoreKit/ViewController/TimelineItinerary/AddPlan/SelectDay/AddPlanSelectDayVC.swift` (lines ~668-676)
- `TRPCoreKit/ViewController/TimelineItinerary/AddPlan/SelectDay/AddPlanSelectDayViewModel.swift`
- `TRPCoreKit/ViewController/TimelineItinerary/AddPlan/Container/AddPlanContainerViewModel.swift` (lines ~44-57: forward-jump)
- `TRPCoreKit/ViewController/TimelineItinerary/AddPlan/TimeAndTravelers/AddPlanTimeAndTravelersVC.swift`
- `TRPCoreKit/ViewController/TimelineItinerary/AddPlan/TimeSelection/AddPlanTimeSelectionVC.swift`

#### Android'de Yapılacaklar

**1. Date extension** — `util/extensions/DateExtensions.kt`:
```kotlin
fun Date.isPastDay(): Boolean {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.time
    val target = Calendar.getInstance().apply {
        time = this@isPastDay
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.time
    return target.before(today)
}

fun Date.isToday(): Boolean { /* benzer */ }
```

**2. View extension** — `util/extensions/ViewExtensions.kt`:
```kotlin
fun View.recolorLabelsAndBorders(@ColorInt color: Int) {
    when (this) {
        is TextView -> setTextColor(color)
        is ImageView -> imageTintList = ColorStateList.valueOf(color)
        is ViewGroup -> children.forEach { it.recolorLabelsAndBorders(color) }
    }
    // Border: background drawable'ı GradientDrawable ise stroke rengini değiştir
    (background as? GradientDrawable)?.setStroke(
        (background as GradientDrawable).let { /* mevcut stroke width'i çek */ 1.dpToPx() },
        color
    )
}
```
> Not: `CAShapeLayer` Android'de yok. Custom view'ler (örn. `FlexibleTimeBadgeView`) için ayrı `applyMutedStyle(color)` metodu yaz, çünkü recursive helper border'ları yakalamayabilir.

**3. DayFilter Mode enum** — `TimelineDayFilterView.kt`:
```kotlin
enum class Mode { TIMELINE, ADD_PLAN }
var mode: Mode = Mode.TIMELINE
```
- Adapter'da: `mode == ADD_PLAN && day.isPastDay()` → cell gri arka plan, click listener no-op.
- `mode == TIMELINE && day.isPastDay()` → cell metin rengi soluk ama tıklanabilir.
- Public method: `setUnavailableDayIndices(indices: Set<Int>)` — AddPlan availability sweep sonrası ek günleri disable etmek için (toplu tour availability geldiğinde).

**4. ViewModel — isSelectedDayPast**:
- `ACTimelineVM.kt`'ye property ekle:
  ```kotlin
  val isSelectedDayPast: Boolean
      get() = selectedDate?.isPastDay() == true
  ```

**5. Cell'lere `isPastDayMode` flag'i**:
- Tüm Timeline ViewHolder'larında (`BookedActivityVH`, `ReservedActivityVH`, `ManualPoiVH`, `RecommendationsVH`, `StepActivityVH`, `StepPoiVH`):
  ```kotlin
  var isPastDayMode: Boolean = false
  ```
- `bind()` sırasında bu flag set edilir (`bind(data: TimelineDisplayItem, isPastDay: Boolean)`).
- `applyPastDayStyle()` çağırılır:
  - Tüm metin/ikonlar `ColorSet.fgWeaker` (yumuşak gri) ile boyanır.
  - Reservation CTA butonu **görünmez** (`isVisible = false`).
  - Change-time, remove butonları görünür ve enabled kalır AMA click handler:
    ```kotlin
    onChangeTimeClick = {
        if (isPastDayMode) return@onChangeTimeClick  // no-op
        // normal action
    }
    ```
  - Image: ColorMatrix ile grayscale.
- ViewHolder `recycle()`'da (`onViewRecycled`) `isPastDayMode = false` reset et.

**6. AddPlan SelectDay forward-jump**:
- `AddPlanContainerVM.kt` init:
  ```kotlin
  init {
      val candidate = profile.selectedDay
      if (candidate?.isPastDay() == true) {
          planData.selectedDay = days.firstOrNull { it.isToday() }
              ?: days.firstOrNull { !it.isPastDay() }
              ?: days.last()
      }
  }
  ```
- DayFilter callback'i:
  ```kotlin
  override fun onDaySelected(day: Date, index: Int) {
      if (day.isPastDay()) return  // no-op
      viewModel.selectDay(day)
  }
  ```

**7. AddPlanTimeSelectionVC**: day filter Mode.ADD_PLAN olarak ayarla.

#### Doğrulama
- Geçmiş bir gün Timeline'da seçildiğinde tüm cell'ler gri görünmeli, butonlar görünür ama tıklayınca etki yapmamalı.
- AddPlan'da geçmiş günler **seçilemez** olmalı.
- AddPlan açılışında, profile'da geçmiş gün varsa otomatik olarak bugüne sıçramalı.

---

### Theme 4: Flexible-Time Activities

> **iOS commit'ler:** `34681e4`, `536ecf5`

#### Amaç
GetYourGuide'da bazı aktivitelerin sabit saati yok (örn: "Bu bilet 24 saat geçerli" gibi). Bu aktiviteler:
- `additionalData.duration == -1` **VE** start/end zamanları `{"00:00", "23:59"}` aralığında.
- Timeline'da farklı görselleştirilir: dashed border'lı badge, order yerine `−` (U+2212 MINUS), "Flexible entry" başlığı, "Check the timetable" alt başlığı.
- Şehir grubunda kronolojik öncekiler arasına değil **en üste** pin'lenir.
- Conflict detection'a dahil değildir (00:00–23:59 placeholder'ı gerçek zaman temsil etmez).

#### iOS Referans Dosyalar
- `TRPCoreKit/TRPDataLayer/Domain/Models/Tour/TRPTourSlot.swift` (yeni — `time: String?`)
- `TRPCoreKit/TRPDataLayer/Domain/Models/Tour/TRPTourSchedule.swift` (range support)
- `TRPCoreKit/TRPDataLayer/Domain/Models/Timeline/TRPMergedTimelineItem.swift` (`isFlexibleActivity` getter eklendi)
- `TRPCoreKit/TRPDataLayer/Domain/Models/Timeline/TRPMapDisplayItem.swift` (`isFlexibleActivity`)
- `TRPCoreKit/ViewController/TimelineItinerary/Cells/TRPTimelineFlexibleActivityCell.swift` (**yeni cell**, ~375 satır)
- `TRPCoreKit/ViewController/TimelineItinerary/Views/TRPTimelineFlexibleTimeBadgeView.swift` (**yeni**, ~162 satır)
- `TRPCoreKit/ViewController/TimelineItinerary/Models/TimelineCellData.swift` (`FlexibleActivityCellData` eklendi)
- `TRPCoreKit/ViewController/TimelineItinerary/TRPTimelineItineraryViewModel+DataProcessing.swift` (sorting: flex first)
- `TRPCoreKit/ViewController/TimelineItinerary/AddPlan/TimeSelection/AddPlanTimeSelectionVC.swift` (flexible info card UI)
- `TRPCoreKit/ViewController/TimelineItinerary/AddPlan/TimeSelection/AddPlanTimeSelectionViewModel.swift`
- `TRPCoreKit/ViewController/TimelineItinerary/Cells/TRPTimelineMapPOIPreviewCell.swift` (`−` badge map preview'da da)
- `TRPCoreKit/Extension/Date+Extensions.swift` (yardımcı helper'lar)
- `TRPCoreKit/Extension/TRPDisabledControlAwareTapDelegate.swift` (yeni — disabled control tap gesture)

#### Android'de Yapılacaklar

**1. Model güncellemesi**:
- `TourSlot.time: String?` — null = flexible. (Theme 2'de eklenmişti, kontrol et.)
- `TourSchedule` artık range (multi-date) destekler:
  ```kotlin
  data class TourSchedule(
      val title: String,
      val dates: List<TourScheduleDay>  // ≥1 entry
  ) {
      val allSlots: List<TourSlot> get() = dates.flatMap { it.slots }
  }
  data class TourScheduleDay(val date: String, val slots: List<TourSlot>)
  ```
- `MergedTimelineItem` (Android equivalent of `TRPMergedTimelineItem`) — `isFlexibleActivity` getter ekle:
  ```kotlin
  val isFlexibleActivity: Boolean
      get() = isReservedActivity &&
              additionalData?.duration == -1.0 &&
              startTimeString in setOf("00:00", "23:59") &&
              endTimeString in setOf("00:00", "23:59")
  ```

**2. `TimelineDisplayItem` sealed class'ına yeni tip ekle**:
```kotlin
data class FlexibleActivity(
    val segment: TimelineSegment,
    val order: Int,  // Aslında kullanılmaz, görsel olarak "−" gösterilecek
    val title: String,
    val imageUrl: String?,
    val isReserved: Boolean,
    val hasConflict: Boolean = false,  // ALWAYS false (flexible çatışmaya girmez)
    val isAvailabilityExpired: Boolean = false,
    val isNoLocation: Boolean = false,
    val rating: Float?,
    val ratingCount: Int?,
    val cancellation: String?,
    val adultCount: Int = 1
) : TimelineDisplayItem()
```

**3. `FlexibleTimeBadgeView` — custom View** `ui/timeline/views/FlexibleTimeBadgeView.kt`:
- Dashed border (4dp dash, 3dp gap). Android'de: custom `Drawable` veya `View.background` olarak XML'de `<shape>` ile `<stroke android:dashWidth="4dp" android:dashGap="3dp" />`.
- Order chip: 20dp daire, içinde `−` (Unicode U+2212 — `−`, hyphen değil!). Düz bir TextView kullan ama metin: `"−"`.
- Title TextView ("Flexible entry"), Subtitle TextView ("Check the timetable"). Localization key'ler `TimelineLocalizationKeys.swift`'ten kopyala (`timeline.flexible.title`, `timeline.flexible.subtitle`, `timeline.flexible.short` vb.).
- Public method: `applyMutedStyle(@ColorInt color: Int)` — past-day modunda çağrılır.

**4. `FlexibleActivityVH` — yeni ViewHolder** `ui/timeline/adapter/FlexibleActivityVH.kt`:
- iOS `TRPTimelineFlexibleActivityCell.swift`'i baştan sona oku ve layout'u XML olarak yaz.
- Layout: image (sol, 80×80 corner radius 16dp) + içerik stack (sağ).
- İçerik: `FlexibleTimeBadgeView` (üstte), title (16sp SemiBold), rating row (varsa), no-location badge (varsa), cancellation row.
- Reservation CTA button (yalnız reserved tip için, full width primary button).
- Butonlar: change-time, remove. Past-day modunda görünür ama no-op.

**5. Sorting: flexible items first**:
- ViewModel'de timeline'ı şehir grubuna böldükten sonra her grup içinde sırala:
  ```kotlin
  group.items.sortedWith(compareByDescending<MergedTimelineItem> { it.isFlexibleActivity }
      .thenBy { it.startDate ?: Date(Long.MAX_VALUE) })
  ```
- Unified order map hesaplarken flexible item'ları atla — order verme, `−` gösterilecek.

**6. AddPlan TimeSelection flexible info card UI**:
- Eğer aktivitenin o gün için sadece flexible slot varsa (tek slot, `time == null`):
  - 4 sütunlu time grid GÖSTERME.
  - Bunun yerine bir info card göster: ikon + "Flexible time activity" başlık + "You can use this ticket any time during the day. Add it to top of your timeline." açıklama.
  - "Pin to top" hint metni.
  - Continue butonu enable — kullanıcı continue'ye basınca segment 00:00 - 23:59 start/end ile yaratılır, duration `-1`.
- Mixed gün (hem flexible hem timed slot var): timed slot'ları grid'de göster + flex'i ekstra "Any time" button olarak.

**7. Map POI Preview Cell**:
- Map'te flexible activity preview cell'inde de `−` badge göster (regular numara değil). `MapBottomListAdapter` veya `TimelineMapPOIPreviewCell` Android karşılığı neyse oraya yansıt.

**8. DisabledControlAwareTapDelegate Android karşılığı**:
- iOS'ta `UITapGestureRecognizer` disabled button'lara da tap event'i geçiriyor (parent gesture conflict önlemek için).
- Android'de: button `isEnabled = true` ama click handler içinde flag kontrol et. Veya disabled state'te button'ın `isClickable = true` ama `OnClickListener` boş set et (gesture'ı yutmak için).

#### Doğrulama
- iOS'ta `civitatis` test trip'inde "Hop-on hop-off" tarzı flexible bir aktivite varsa, Timeline'da dashed border + `−` + "Flexible entry" görünüyor.
- Bu aktivite zaman çakışması göstermiyor.
- AddPlan TimeSelection'da bu tour için "Flexible info card" çıkıyor.
- Past-day modunda flexible badge de gri renge bürünüyor (CAShapeLayer karşılığı düzgün muted oluyor).

---

### Theme 5: Tour Schedule Availability & Product Lookup

> **iOS commit'ler:** `f14afd4`, `23d0430`

#### Amaç
İki yeni backend endpoint entegrasyonu:
1. **`tour-api/product-lookup`** — `providerId + productId` ile tek tour'un tüm detayını (koordinat, kategori, city dahil) getirir. No-location aktiviteler için kullanılır (Theme 6).
2. **`tour-api/schedule-availability`** — Birden fazla item için aynı tarihte batch availability sorgusu (`items=[C_xxx_15, C_yyy_15], date=2026-06-12`).

#### iOS Referans Dosyalar
- `TRPCoreKit/TRPDataLayer/Domain/Models/Tour/TRPTourScheduleAvailability.swift` (yeni)
- `TRPCoreKit/TRPDataLayer/Data/RemoteApi/Tour/TRPTourRemoteApi.swift` (`lookupTourProduct`, `getTourScheduleAvailability`)
- `TRPCoreKit/TRPDataLayer/Data/RemoteApi/Tour/TourRemoteApi.swift` (protocol additions)
- `TRPCoreKit/TRPDataLayer/Data/Repositories/Tour/TRPTourRepository.swift`
- `TRPCoreKit/TRPDataLayer/Data/Repositories/Tour/TourRepository.swift`
- `TRPCoreKit/TRPDataLayer/Domain/UseCases/Tour/TRPTourUseCases.swift`
- `TRPCoreKit/TRPDataLayer/Domain/UseCases/Tour/TourUseCases.swift`
- `TRPCoreKit/TRPDataLayer/Domain/Mappers/Tour/TourMapper.swift`
- `TRPCoreKit/Extension/String+Extensions.swift` (helper'lar — activity id parse)

#### Android'de Yapılacaklar

**1. Domain modeli** — `domain/model/tour/TourScheduleAvailability.kt`:
```kotlin
data class TourScheduleAvailability(
    val activityId: String,
    val schedule: TourSchedule?  // null = sold out / no slots
) {
    val hasAvailability: Boolean
        get() = schedule?.allSlots?.isNotEmpty() == true
}
```
**Kritik invariant**: `schedule == null` → o tarihte sold-out. Empty list değil, **null**.

**2. TRPOne SDK kontrolü**:
- `TRPOne/src/main/java/com/tripian/one/api/tour/TTours.kt` dosyasını oku.
- Eğer `lookupTourProduct` ve `getTourScheduleAvailability` metodları yoksa **TRPOne SDK'da yok demektir**. Bu durumda iki seçenek:
  - (Tercih) `TRPCore` repository içinde Retrofit endpoint olarak doğrudan tanımla.
  - Veya TRPOne SDK'ya ekle (ayrı proje, kullanıcı onayı gerekir).
- Endpoint pattern'leri:
  ```
  GET tour-api/product-lookup?providerId=15&productId=15423
  GET tour-api/schedule-availability?items=C_xxx_15,C_yyy_15&date=2026-06-12&currency=EUR&lang=en
  ```

**3. Repository extension** — `repository/TimelineRepository.kt` veya yeni `TourRepository.kt`:
```kotlin
fun lookupTourProduct(providerId: Int, productId: String): Single<TourProduct>

fun getTourScheduleAvailability(
    items: List<String>,         // ["C_15423_15", ...]
    date: String,                // "yyyy-MM-dd"
    currency: String? = null,
    lang: String? = null
): Single<List<TourScheduleAvailability>>
```

**4. UseCase'ler** — `domain/usecase/tour/`:
```kotlin
class LookupTourProductUseCase @Inject constructor(private val repo: TourRepository) :
    BaseUseCase<TourProduct, LookupTourProductUseCase.Params>() {
    data class Params(val providerId: Int, val productId: String)
    override fun on(params: Params) = repo.lookupTourProduct(params.providerId, params.productId).toObservable()
}

class GetTourScheduleAvailabilityUseCase @Inject constructor(private val repo: TourRepository) :
    BaseUseCase<List<TourScheduleAvailability>, GetTourScheduleAvailabilityUseCase.Params>() {
    data class Params(val items: List<String>, val date: String, val currency: String? = null, val lang: String? = null)
    override fun on(params: Params) = repo.getTourScheduleAvailability(params.items, params.date, params.currency, params.lang).toObservable()
}
```
- DI: ilgili module'e ekle, ViewModelFactory'de inject et.

**5. ActivityId format helper** — `util/extensions/StringExtensions.kt`:
```kotlin
// iOS'ta String+Extensions.swift'te yardımcılar var. Aynı mantığı kopyala.
fun String.normalizedActivityId(providerId: Int = 15): String =
    if (startsWith("C_")) this else "C_${this}_$providerId"
```

**6. Kullanım yerleri**:
- **No-location handling** (Theme 6): `lookupTourProduct` ile koordinat alınır.
- **Availability sweep** (Theme 17): `getTourScheduleAvailability` ile post-load batch sorgu.

#### Doğrulama
- Bir reserved activity'nin product detayı eksik olduğunda lookup ile city resolve edilebiliyor.
- Schedule availability call'ı doğru format query parameter ile yapılıyor (`items=A,B,C&date=...`).

---

### Theme 6: No-Location Support

> **iOS commit:** `24bd4c8`

#### Amaç
Bazı aktivitelerin gerçek dünya koordinatı yok (örn: "online tour", "audio guide download"). Bu aktivitelerin:
- Server'dan gelen `additionalData.isNoLocation: true` flag'i tanınır.
- Timeline cell'inde "No exact location" badge (mavi) gösterilir.
- Map'te bu marker gösterilmez (veya city center'a yerleştirilir).
- City resolution `cities/resolve` API'siyle yapılmaz, Theme 5'teki `lookupTourProduct` ile alınır.

#### iOS Referans Dosyalar
- `TRPCoreKit/TRPDataLayer/Domain/Mappers/Timeline/TimelineProfileMapper.swift` (decode update)
- `TRPCoreKit/TRPDataLayer/Domain/Mappers/Timeline/TimelineSegmentMapper.swift` (decode update)
- `TRPCoreKit/TRPDataLayer/Domain/Models/TRPItineraryWithActivities.swift` (`TRPSegmentActivityItem.isNoLocation`)
- `TRPCoreKit/TRPDataLayer/Domain/Models/Timeline/TRPMergedTimelineItem.swift` (`isNoLocation` getter)
- `TRPCoreKit/TRPDataLayer/Domain/Models/Timeline/TRPMapDisplayItem.swift` (`isNoLocation`)
- `TRPCoreKit/ViewController/TimelineItinerary/Cells/TRPTimelineReservedActivityCell.swift` (badge UI)
- `TRPCoreKit/ViewController/TimelineItinerary/Cells/TRPTimelineMapPOIPreviewCell.swift` (badge UI)
- `TRPCoreKit/ViewController/TimelineItinerary/Models/TimelineCellData.swift` (`isNoLocation` field)
- `TRPCoreKit/ViewController/TimelineItinerary/TRPTimelineItineraryVC+AddPlan.swift`
- `TRPCoreKit/ViewController/TimelineItinerary/TRPTimelineItineraryVC+Map.swift` (skip no-location markers)
- `TRPCoreKit/ViewController/TimelineItinerary/TRPTimelineItineraryViewModel+MapHelpers.swift`
- `TRPCoreKit/Utility/TRPTheme.swift` (yeni renkler: noLocation blue)

#### Android'de Yapılacaklar

**1. Backend model**:
- Server response'da `additional_data.is_no_location: Boolean` (veya benzeri). Mevcut `TimelineSegment` Gson model'inde bu field varsa kontrol et, yoksa ekle:
  ```kotlin
  @SerializedName("is_no_location")
  var isNoLocation: Boolean = false
  ```
- Tüm map'leme/dönüşüm zinciri boyunca propagate et (`TimelineSegment` → `MergedTimelineItem` → `TimelineDisplayItem`).

**2. Cell UI** (Booked, Reserved, ManualPoi, FlexibleActivity, MapPOIPreview):
- "Activity" badge'inin altında küçük bir secondary badge ekle: ikon (info/location-off) + metin "No exact location".
- Renk: arka plan ColorSet.infoBlueLight, metin/ikon: ColorSet.infoBlue. (iOS `TRPTheme.swift`'teki renkleri Android'de `colors.xml`'e ekle: `color_no_location_bg`, `color_no_location_fg`.)
- Localization key: `TIMELINE_NO_EXACT_LOCATION = "timeline.label.noExactLocation"`.
- Sadece `isNoLocation == true` ise göster, normalde `visibility = GONE`.

**3. Map'te no-location handling**:
- `MapView.kt` veya `ACTimeline+Map` Android karşılığında: marker oluştururken `if (item.isNoLocation) skip` veya marker'ı city center'a koy ama numara badge'ini değiştir (tartışma — iOS davranışı: marker oluşturulmaz). iOS'taki `TRPTimelineItineraryVC+Map.swift`'i oku ve birebir uygula.
- Map preview bottom collection'da no-location item gösterilir mi? iOS'a bak. Görünür ama "No exact location" badge'i var.

**4. City resolution flow**:
- Timeline ilk yüklendiğinde, segment'ler için city resolve yaparken: `additionalData.isNoLocation == true` olan aktiviteleri **`cities/resolve`'a göndermeyin**. Bunlar için Theme 5'teki `lookupTourProduct(providerId, productId)` çağrısı yap. Response'tan `cityId` al, segment'e yaz.
- iOS dosyası: `TRPTimelineItineraryViewModel+TimelineOperations.swift` — `resolveMissingCityIds` fonksiyonu.

#### Doğrulama
- API'den `is_no_location: true` gelen segment için cell'de mavi "No exact location" badge görünür.
- Map'te bu segment için POI marker görünmez.
- Manual no-location aktivite şehre doğru atanır (lookup ile).

---

### Theme 7: Time Badge Warning Status Row

> **iOS commit:** `2ef053e`

#### Amaç
Time badge (`TimeBadgeView`) altına opsiyonel durum satırı eklenir:
- Renkli dot (status indicator) + opsiyonel warning ikonu + status metni.
- Üç state:
  - **Normal** — status row gizli.
  - **Conflict** — `civiOrange` renkli dot + warning ikon + "Time Overlap" metni (sadece **reserved** aktivitede metin gösterilir; **booked**'ta dot ve ikon var ama metin gösterilmez).
  - **Availability Expired** — `errorRed` renkli dot + warning ikon + "Not available" metni. **AvailabilityExpired önceliklidir** (hem expired hem conflict varsa expired gösterilir).

#### iOS Referans Dosyalar
- `TRPCoreKit/ViewController/TimelineItinerary/Views/TRPTimelineTimeBadgeView.swift` (configure() metoduna `hasConflict`, `showTimeOverlapText`, `isAvailabilityExpired` parametreleri eklendi)
- `TRPCoreKit/Assets.xcassets/ic_warning.imageset/` (yeni asset)
- `TRPCoreKit/Utility/TRPTheme.swift` (warning renkleri)
- `TRPCoreKit/ViewController/TimelineItinerary/AddPlan/ActivityListing/AddPlanActivityListingVC.swift` (warning kullanımı)

#### Android'de Yapılacaklar

**1. Asset**: `ic_warning.svg`'yi vector drawable olarak ekle (`src/main/res/drawable/ic_warning.xml`). iOS asset'ini oku, path data'sını kopyala.

**2. Theme renkleri** — `res/values/colors.xml` (veya theme palette neyse):
```xml
<color name="civi_orange">#F39C12</color>
<color name="civi_orange_bg">#FFF3E0</color>     <!-- pale yellow -->
<color name="civi_orange_border">#FFE0B2</color>
<color name="error_icon">#E53935</color>
<color name="error_bg">#FFEBEE</color>
```

**3. TimeBadgeView güncellemesi**:
- `ui/timeline/views/TimelineTimeBadgeView.kt` (yoksa oluştur) — bir custom View veya `ConstraintLayout` subclass.
- Layout: Order chip (20dp circle) + "HH:mm - HH:mm" + opsiyonel **statusRow** (dot + warning icon + text).
- Public method:
  ```kotlin
  fun configure(
      order: Int,                       // negative veya 0 → "−" göster
      startTime: String,
      endTime: String,
      hasConflict: Boolean = false,
      showTimeOverlapText: Boolean = false,
      isAvailabilityExpired: Boolean = false
  )
  ```
- Renk mantığı (iOS'taki birebir):
  ```kotlin
  when {
      isAvailabilityExpired -> {
          // Red border, red order chip, status row görünür: red dot + warning + "Not available"
      }
      hasConflict -> {
          // Orange border, orange order chip + yellow bg
          // Status row görünürlüğü: showTimeOverlapText ise label görünür
          // Sadece dot ve ikon her conflict durumunda görünür
      }
      else -> {
          // Normal — status row tamamen GONE
      }
  }
  ```
- Localization key: `timeline.label.timeOverlap` ("Time Overlap"), `timeline.label.notAvailable` ("Not available").

**4. Cell'lerden geçirilen parametreler**:
- `BookedActivityVH`: `hasConflict = true, showTimeOverlapText = false` (red badge ama label yok).
- `ReservedActivityVH`: `hasConflict = data.hasConflict, showTimeOverlapText = data.showTimeOverlapText`.
- `ManualPoiVH`: aynı reserved gibi.
- `RecommendationsVH` step'leri: aynı kural.

#### Doğrulama
- Çakışan iki reserved aktivite varsa, time badge'de turuncu dot + warning + "Time Overlap" gözükmeli.
- Bir aktivite expired olduğunda kırmızı dot + warning + "Not available" gözükmeli.
- Booked aktivite çakıştığında turuncu badge ama metin yok.

---

### Theme 8: Conflict Detection & Banner

> **iOS commit'ler:** `295dc13`, `28cf5b7`, `2567ff8`

#### Amaç
Timeline'da aynı gün çakışan aktiviteler tespit edilir. ViewModel'de overlap algoritması çalışır, çakışan tüm item'lar işaretlenir. UI'da:
- Çakışan item'ların time badge'inde Theme 7'deki conflict styling.
- Tablo üstünde dismissable bir **conflict warning banner** çıkar ("You have overlapping activities on this day. Tap to review."), kullanıcı dismiss edebilir, dismiss'lik gün değişince reset olur.

#### iOS Referans Dosyalar
- `TRPCoreKit/ViewController/TimelineItinerary/Views/TRPTimelineConflictWarningView.swift` (**yeni** ~133 satır)
- `TRPCoreKit/ViewController/TimelineItinerary/TRPTimelineItineraryVC.swift` (banner property, visibility logic, dismiss tracking)
- `TRPCoreKit/ViewController/TimelineItinerary/TRPTimelineItineraryVC+Setup.swift`
- `TRPCoreKit/ViewController/TimelineItinerary/TRPTimelineItineraryViewModel+DataProcessing.swift` (`detectTimeConflicts` algorithm)
- `TRPCoreKit/ViewController/TimelineItinerary/Models/TimelineCellData.swift` (`hasConflict`, `showTimeOverlapText` fields)
- `TRPCoreKit/ViewController/TimelineItinerary/TimelineLocalizationKeys.swift` (yeni keys)

#### Android'de Yapılacaklar

**1. Conflict detection algoritması** — `ACTimelineVM.kt` (veya DataProcessing helper):
```kotlin
private fun detectTimeConflicts(items: List<MergedTimelineItem>) {
    // 1. Reset all flags
    items.forEach { it.hasConflict = false; it.showTimeOverlapText = false }

    // 2. Flexible items EXCLUDED from conflict check
    val candidates = items.filter { !it.isFlexibleActivity && it.startDate != null && it.endDate != null }

    // 3. Pairwise overlap check
    for (i in candidates.indices) {
        for (j in (i+1) until candidates.size) {
            val a = candidates[i]; val b = candidates[j]
            if (a.startDate!! < b.endDate!! && b.startDate!! < a.endDate!!) {
                a.hasConflict = true; b.hasConflict = true
                // Reserved gets the text label, booked does not
                if (a.isReservedActivity || a.isManualPoi) a.showTimeOverlapText = true
                if (b.isReservedActivity || b.isManualPoi) b.showTimeOverlapText = true
            }
        }
    }
}
```
> Komşu zamanlar (a.end == b.start) çakışmaz — strict less-than.

**2. ConflictWarningView** — `ui/timeline/views/ConflictWarningView.kt`:
- Layout: ConstraintLayout. Pale yellow bg, warning ikon (sol), metin (orta, 14sp), close X butonu (sağ).
- Metin: "You have overlapping activities on this day. Tap to review." (localization key: `timeline.conflict.banner`)
- Tap → ViewModel'e bildir (opsiyonel scroll-to-first-conflict).
- Close → kendini gizle ve dismiss callback'ini çağır.

**3. ACTimeline.kt banner entegrasyonu**:
- Sticky banner: timeline RecyclerView'in üzerinde, day filter altında.
- Property: `var conflictBannerDismissedDayIndex: Int = -1`.
- Update logic:
  ```kotlin
  fun updateConflictWarningVisibility() {
      val hasConflict = viewModel.displayItems.any { it.hasConflict }
      val isCurrentDayDismissed = conflictBannerDismissedDayIndex == viewModel.selectedDayIndex
      conflictWarningView.isVisible = hasConflict && !isCurrentDayDismissed
  }
  ```
- Çağrı yerleri: gün değiştiğinde, timeline refresh sonrası, viewModel'den `didUpdateTimeline` event'i geldiğinde.
- Reset: `setSelectedDay(...)` çağrıldığında `conflictBannerDismissedDayIndex = -1` (her gün dismiss bağımsız).
- Timeline tam refresh sonrasında da reset edilir.

**4. Localization keys** (`TimelineLocalizationKeys`'ten kopyala):
```
TIMELINE_TIME_OVERLAP            = "timeline.label.timeOverlap"
TIMELINE_NOT_AVAILABLE           = "timeline.label.notAvailable"
TIMELINE_CONFLICT_BANNER         = "timeline.conflict.banner"
TIMELINE_CONFLICT_BANNER_BUTTON  = "timeline.conflict.banner.action"
```

#### Doğrulama
- Aynı gün overlap'li iki aktivite eklendiğinde banner çıkar.
- Banner'da X'e basınca banner kaybolur ve aynı gün için tekrar gösterilmez.
- Başka güne geçip dönünce banner tekrar çıkar (per-day dismiss).

---

### Theme 9: Time Selection Enhancements

> **iOS commit'ler:** `295dc13`, `025645d`, `971b8b5`, `2567ff8`

#### Amaç
AddPlan TimeSelection ekranında üç yeni özellik:
1. **"Show more" slot cell** — 8'den fazla slot varsa ilk 7'sini gösterip sonuncusu olarak "Show more" cell, tıklayınca tüm slot'ları açar.
2. **Sold-out banner (edit mode)** — Reserved aktivitenin time'ı düzenlenirken, kaydedilen saat artık schedule'da yoksa kırmızı banner: "Your saved time is no longer available. Please pick a new one."
3. **Booking-availability banner** — Bilet sold-out olduğunda info banner.

#### iOS Referans Dosyalar
- `TRPCoreKit/ViewController/TimelineItinerary/AddPlan/TimeSelection/AddPlanShowMoreSlotCell.swift` (**yeni**)
- `TRPCoreKit/ViewController/TimelineItinerary/AddPlan/TimeSelection/AddPlanTimeSlotCell.swift` (sold-out state eklendi)
- `TRPCoreKit/ViewController/TimelineItinerary/AddPlan/TimeSelection/AddPlanTimeSelectionVC.swift` (banner + show more logic)
- `TRPCoreKit/ViewController/TimelineItinerary/AddPlan/TimeSelection/AddPlanTimeSelectionViewModel.swift` (`collapsedSlotThreshold = 8`, `collapsedSlotCount = 7`, `isTimeSlotsExpanded` state)
- `TRPCoreKit/ViewController/TimelineItinerary/AddPlan/AddPlanLocalizationKeys.swift` (yeni keys)

#### Android'de Yapılacaklar

**1. ViewModel state** — `ActivityTimeSelectionVM.kt`:
```kotlin
private val collapsedSlotThreshold = 8
private val collapsedSlotCount = 7
var isTimeSlotsExpanded: Boolean = false

fun getDisplayedSlots(): List<DisplayTimeSlot> {
    return if (allSlots.size >= collapsedSlotThreshold && !isTimeSlotsExpanded)
        allSlots.take(collapsedSlotCount)
    else allSlots
}

val shouldShowMoreCell: Boolean
    get() = allSlots.size >= collapsedSlotThreshold && !isTimeSlotsExpanded
```

**2. ShowMoreSlotCell** — `ui/timeline/addplan/activity/ShowMoreSlotVH.kt`:
- Layout: tek satır underline'lı text "Show more times" (localization key: `addPlan.timeSelection.showMore`).
- Tıklayınca: ViewModel.isTimeSlotsExpanded = true → grid recompose.

**3. Adapter multi-type**: Grid adapter iki tip:
- `TYPE_SLOT` (`AddPlanTimeSlotVH`)
- `TYPE_SHOW_MORE` (`ShowMoreSlotVH`)
- Grid layout 4 sütun. Show more cell ise (`shouldShowMoreCell`) en son cell olarak eklenir.

**4. Sold-out cell state** (edit modunda):
- Edit modunda kaydedilen saat (`editingTimeString`) schedule'da yoksa, bu saat **placeholder slot** olarak grid'de gösterilir:
  - Arka plan gri (`bgDisabled`).
  - Border yok.
  - Text beyaz/gri ama strike-through veya farklı style.
  - **Disabled** — tıklanamaz.
  - Adapter'da `DisplayTimeSlot(time, price = null, isDisabled = true)` ile temsil edilir.

**5. Sold-out banner**:
- Edit modunda + missing time durumunda TimeSelection VC'nin üstünde kırmızı banner görünür.
- Layout: warning ikonu + text + close yok (informational).
- Text: localization key `addPlan.timeSelection.soldOutBanner` ("Your saved time is no longer available. Please pick a new one.")

**6. Booking-availability banner**:
- Aktivite o tarih için sold-out (schedule null veya boş): info banner çık. Localization key `addPlan.timeSelection.bookingUnavailable` ("This activity is not available on this day.").
- Continue butonu disabled.

**7. Localization keys**:
```
ADD_PLAN_TIME_SELECTION_SHOW_MORE         = "addPlan.timeSelection.showMore"
ADD_PLAN_TIME_SELECTION_SOLD_OUT_BANNER   = "addPlan.timeSelection.soldOutBanner"
ADD_PLAN_TIME_SELECTION_BOOKING_UNAVAIL   = "addPlan.timeSelection.bookingUnavailable"
ADD_PLAN_TIME_SELECTION_NO_AVAILABLE      = "addPlan.timeSelection.noAvailable"
```

#### Doğrulama
- 10 slot varsa grid'de 7 slot + "Show more" görünmeli. Tap → tüm 10 slot.
- Edit'te kaydedilen saat artık yoksa o slot disabled placeholder + üstte kırmızı banner.

---

### Theme 10: Timeline Refresh State (Global)

> **iOS commit:** `971b8b5`

#### Amaç
Timeline refresh aşaması (segment yaratma sonrası polling, vb.) artık sadece TimelineVC'nin delegate'iyle değil **global observable** üzerinden takip edilir. Böylece SavedPlansVC, AddPlanTimeSelectionVC gibi açık başka ekranlar da bu state'i izleyip kendi loader'larını gösterebilir.

#### iOS Referans Dosyalar
- `TRPCoreKit/TRPDataLayer/Domain/Models/Timeline/TRPTimelineRefreshState.swift` (yeni, ~42 satır)
- `TRPCoreKit/ViewController/TimelineItinerary/SavedPlans/SavedPlansVC.swift` (observer)
- `TRPCoreKit/ViewController/TimelineItinerary/SavedPlans/SavedPlansViewModel.swift`
- `TRPCoreKit/ViewController/TimelineItinerary/AddPlan/TimeSelection/AddPlanTimeSelectionVC.swift` (observer)
- `TRPCoreKit/ViewController/TimelineItinerary/AddPlan/TimeSelection/AddPlanTimeSelectionViewModel.swift`
- `TRPCoreKit/ViewController/TimelineItinerary/TRPTimelineItineraryViewModel+SegmentCreation.swift` (state set'ler)
- `TRPCoreKit/ViewController/TimelineItinerary/TRPTimelineItineraryViewModel+StepOperations.swift`

#### Android'de Yapılacaklar

**1. Singleton observable** — `domain/manager/TimelineRefreshState.kt`:
```kotlin
sealed class TimelineRefreshStatus {
    object Idle : TimelineRefreshStatus()
    object Refreshing : TimelineRefreshStatus()
    object Completed : TimelineRefreshStatus()
    data class Failed(val error: Throwable) : TimelineRefreshStatus()
}

object TimelineRefreshState {
    private val _status = BehaviorSubject.createDefault<TimelineRefreshStatus>(TimelineRefreshStatus.Idle)
    val status: Observable<TimelineRefreshStatus> = _status.hide()

    fun setRefreshing() = _status.onNext(TimelineRefreshStatus.Refreshing)
    fun setCompleted() = _status.onNext(TimelineRefreshStatus.Completed)
    fun setFailed(error: Throwable) = _status.onNext(TimelineRefreshStatus.Failed(error))
    fun setIdle() = _status.onNext(TimelineRefreshStatus.Idle)
}
```

**2. ACTimelineVM** segment/step operasyonlarında set'le:
- Segment yaratma başlangıcında: `TimelineRefreshState.setRefreshing()`
- Polling complete (timeline yenilendikten sonra): `setCompleted()`, sonra kısa süre içinde `setIdle()` (kullanıcı görsel feedback için)
- Error durumunda: `setFailed(e)`

**3. Subscribe noktaları**:
- `ACSavedPlans.kt` — `onResume`'da `TimelineRefreshState.status.subscribe { ... }` → refreshing iken bottom-sheet Lottie göster, completed olunca toast: "Activity added to your timeline."
- `ActivityTimeSelectionBottomSheet.kt` — edit modunda timeline refresh sırasında bottom-sheet Lottie göster.
- `onPause` veya `onDestroy`'da subscription dispose et (DisposeBag pattern).

**4. PopupAlert güncellemesi**:
- iOS'ta `PopupAlert.swift` da güncellenmiş (Refresh state ile uyum için minor düzeltme). Mevcut Android Popup/Alert helper'ı varsa kontrol et, generally büyük değişiklik beklenmiyor.

#### Doğrulama
- Saved Plans'tan aktivite ekledikten sonra Saved Plans açık kalır, bottom-sheet Lottie döner, complete olunca toast gözükür.
- AddPlan TimeSelection'da edit-time çağrısı sonrası Timeline refresh ederken ekranda loading state görünür.

---

### Theme 11: POI Sort & POI Listing Improvements

> **iOS commit:** `025645d`

#### Amaç
- AddPlan POI Listing ekranına sıralama seçenekleri eklenir (popularity, rating, price, duration).
- POI yüklenirken skeleton cells.
- Sold-out POI cell styling.

#### iOS Referans Dosyalar
- `TRPCoreKit/TRPDataLayer/Domain/Models/Poi/PoiParameters.swift` (`sort`, `order` field'ları)
- `TRPCoreKit/TRPDataLayer/Data/RemoteApi/Poi/TRPRemoteApi.swift`
- `TRPCoreKit/TRPDataLayer/Domain/UseCases/Poi/TRPPoiUseCases.swift`
- `TRPCoreKit/TRPDataLayer/Domain/UseCases/Poi/PoiUseCases.swift`
- `TRPCoreKit/ViewController/TimelineItinerary/AddPlan/POIListing/AddPlanPOIListingViewModel.swift`
- `TRPCoreKit/ViewController/TimelineItinerary/AddPlan/ActivityListing/AddPlanSortByVC.swift` (POI için de aynı VC kullanılıyor)
- `TRPCoreKit/Assets.xcassets/ic_error_popup.imageset/` (yeni)

#### Android'de Yapılacaklar

**1. PoiParameters'a sort/order ekle** — repository/model:
```kotlin
data class PoiParameters(
    val cityId: Int?,
    val search: String?,
    val categoryIds: List<Int>?,
    // ... mevcut field'lar
    val sort: String? = null,     // "rating", "price", "duration"
    val order: String? = null     // "desc", "asc"
)
```

**2. TRPOne'da sort/order destekleniyor mu kontrol et** (`TPois.getPoi`'a parametreler eklendi mi). Yoksa repository wrapper'da Retrofit query parametreleri olarak geçir.

**3. POIListing sort modal**:
- AddPlan POI Listing VC'ye sort button ekle (Activity listing'deki gibi).
- Tıklayınca `ActivitySortBottomSheet`'i POI mode'da aç (veya yeni `PoiSortBottomSheet` yarat).
- Sort options:
  ```kotlin
  enum class PoiSortOption(val labelKey: String, val sortQuery: String?, val orderQuery: String?) {
      POPULARITY(LanguageConst.SORT_BY_POPULARITY, null, null),      // default
      RATING(LanguageConst.SORT_BY_RATING, "rating", "desc"),
      PRICE_LOW_HIGH(LanguageConst.SORT_BY_PRICE_LOW_HIGH, "price", "asc"),
      DURATION_SHORT_LONG(LanguageConst.SORT_BY_DURATION_SHORT_LONG, "duration", "asc")
  }
  ```

**4. Skeleton cells**:
- `POIListingCell` için skeleton variant: gri kutucuklar (image, title, address) — Shimmer kütüphanesi veya basit alpha animasyon.
- Adapter'da loading state göster: ilk fetch sırasında 6 skeleton cell.

**5. Sold-out POI cell styling**:
- POI cell'inde "Sold out" badge veya overlay (poi `isAvailable: false` ise).
- Image grayscale + "Not available" badge.

**6. Asset**: `ic_error_popup.svg`'yi vector drawable olarak ekle.

#### Doğrulama
- POI Listing'de sort button çalışıyor, seçilen option API parametresine yansıyor.
- İlk fetch sırasında skeleton cell'ler gözüküyor.

---

### Theme 12: Collapsible Section Headers

> **iOS commit:** `28cf5b7`

#### Amaç
Timeline'da çoklu şehirli günlerde section header (şehir adı) tıklanabilir, tap ile o section daraltılır/genişletilir. State `[Int: Bool]` map'inde tutulur.

#### iOS Referans Dosyalar
- `TRPCoreKit/ViewController/TimelineItinerary/TRPTimelineItineraryVC.swift` (`sectionCollapseStates: [Int: Bool]`)
- `TRPCoreKit/ViewController/TimelineItinerary/Views/TRPTimelineTimeBadgeView.swift` (warning renkleri)
- `TRPCoreKit/Utility/TRPTheme.swift`
- iOS dosyasındaki section header tap handler ve adapter logic.

#### Android'de Yapılacaklar

**1. ACTimeline state**:
```kotlin
private val sectionCollapseStates = mutableMapOf<Int, Boolean>()

fun toggleSection(sectionIndex: Int) {
    val current = sectionCollapseStates[sectionIndex] ?: false
    sectionCollapseStates[sectionIndex] = !current
    timelineAdapter.notifyDataSetChanged()
}

fun isSectionCollapsed(sectionIndex: Int): Boolean = sectionCollapseStates[sectionIndex] ?: false
```

**2. Section Header VH**:
- Tıklanabilir hale getir.
- Chevron ikonu ekle (down = expanded, right = collapsed).
- Tap → `toggleSection(adapterPosition)`.

**3. Adapter logic**:
- Items oluşturulurken collapsed section'ların item'larını listeden hariç tut (sadece header + footer kalır).
- `getItemViewType` ve `getItemCount` collapsed state'i hesaba kat.

**4. Section state reset**:
- Gün değiştiğinde collapse state'leri sıfırla.
- Timeline refresh sonrasında da reset.

#### Doğrulama
- Çoklu şehirli gün açıldığında her şehir section header'ı tıklanır.
- Tıklayınca o şehrin item'ları gizlenir, header görünmeye devam eder.

---

### Theme 13: Manual POIs & Optional Step Handling

> **iOS commit:** `5a8081b`

#### Amaç
Manual POI segment'lerinin AddPlan flow'unda doğru ele alınması. Bazı manual POI segment'lerinde step yokken bile (henüz oluşturulmamış) UI çökmeyecek.

#### iOS Referans Dosyalar
- `TRPCoreKit/ViewController/TimelineItinerary/TRPTimelineItineraryVC+AddPlan.swift`

#### Android'de Yapılacaklar

**1. Null safety pass**:
- `ACTimeline+AddPlan` Android karşılığında manual POI handling'i kontrol et.
- `plan?.steps?.firstOrNull()` veya `plan == null` durumlarında crash etmesin.
- Manual POI segment yaratırken response gelene kadar step listesi boş olabilir; UI bunu doğru handle etmeli.

**2. Defensive coding**:
- `manualPoi` getter (MergedTimelineItem):
  ```kotlin
  val manualPoi: TRPPoi?
      get() = if (segmentType == SegmentType.MANUAL_POI) plan?.steps?.firstOrNull()?.poi else null
  ```
- Cell bind'ta `manualPoi?.let { ... } ?: showPlaceholder()`.

#### Doğrulama
- Manual POI ekleme sırasında loading anında crash olmuyor.
- Step'siz manual POI segment'i UI'de placeholder gösteriyor.

---

### Theme 14: Layout Spacing & U+2212 Minus

> **iOS commit:** `ed4f8b5`

#### Amaç
Order badge'inde flexible item'lar için kullanılan `-` (regular hyphen) yerine `−` (U+2212 MINUS SIGN) — daha iyi dikey hizalama.

#### iOS Referans Dosyalar
- `TRPCoreKit/ViewController/TimelineItinerary/Cells/TRPTimelineFlexibleActivityCell.swift`
- `TRPCoreKit/ViewController/TimelineItinerary/Cells/TRPTimelineManualPoiCell.swift`
- `TRPCoreKit/ViewController/TimelineItinerary/Cells/TRPTimelineMapPOIPreviewCell.swift`
- `TRPCoreKit/ViewController/TimelineItinerary/Cells/TRPTimelineRecommendationsCell.swift`
- `TRPCoreKit/ViewController/TimelineItinerary/Views/TRPTimelineFlexibleTimeBadgeView.swift`

#### Android'de Yapılacaklar

**1. Constant**:
```kotlin
const val MINUS_SIGN = "−"  // U+2212 MINUS SIGN, NOT "-"
```

**2. Kullanım yerleri**:
- `FlexibleTimeBadgeView` order chip text: `MINUS_SIGN`
- `TimelineTimeBadgeView` flexible mode: `MINUS_SIGN`
- Map POI Preview Cell badge text: `MINUS_SIGN`
- Recommendations Cell'in accommodation row badge: `MINUS_SIGN`
- ManualPoi Cell badge: order 0 ise `MINUS_SIGN`

**3. Layout spacing**:
- iOS'ta birkaç padding/margin tweak'i var. Diff'e bakıp birebir uygula:
  ```bash
  git diff civitatis..civitatis-hp-april20 -- TRPCoreKit/ViewController/TimelineItinerary/Cells/TRPTimelineFlexibleActivityCell.swift
  ```
- Sayısal değerleri Android dp olarak çevir (1pt ≈ 1dp).

#### Doğrulama
- Order yerine `−` gösterilen cell'lerde karakter düzgün dikey ortalanmış görünür.

---

### Theme 15: POI Cards Toggle on Map Tap & Refresh on Reload

> **iOS commit:** `bc5815e`

#### Amaç
Timeline map mode'da:
- POI marker'ına tıklanınca bottom POI preview card'ı açılır/kapanır (toggle).
- Timeline reload edildiğinde map ve preview card'lar otomatik refresh olur.

#### iOS Referans Dosyalar
- `TRPCoreKit/ViewController/TimelineItinerary/TRPTimelineItineraryVC+Map.swift`
- `TRPCoreKit/ViewController/TimelineItinerary/TRPTimelineItineraryVC.swift`

#### Android'de Yapılacaklar

**1. Map marker click handler**:
- Aynı marker'a tekrar tıklanırsa preview card kapanır.
- Farklı marker'a tıklanırsa preview card'taki içerik değişir, marker selection state güncellenir.

**2. Timeline reload sırasında map refresh**:
- ViewModel `didUpdateTimeline` event'i geldiğinde:
  - Mevcut map state'i (selected marker, camera position) koru.
  - Marker'ları yeniden çiz.
  - Preview card'taki POI hâlâ mevcut mu? Mevcut ise card'ı koru, yoksa kapat.

**3. State management**:
```kotlin
var selectedPoiId: String? = null
fun onMarkerTapped(poiId: String) {
    selectedPoiId = if (selectedPoiId == poiId) null else poiId
    updatePreviewCards()
    updateMarkerSelection()
}
```

#### Doğrulama
- Map'te bir marker'a tıklayınca card açılır, aynı marker'a tekrar tıklayınca kapanır.
- Timeline refresh sonrası map state korunuyor.

---

### Theme 16: AddPlan Scrolling, Date Scope & Day Select Fixes

> **iOS commit:** `aa48976`

#### Amaç
AddPlan'da çeşitli scroll, date scope ve day selection bug fix'leri:
- Activity Listing infinite scroll bug fix (scroll listener leak).
- Date scope: AddPlan day filter trip date'leri ile sınırlı kalmalı.
- Day selection edge case'leri.

#### iOS Referans Dosyalar
- `TRPCoreKit/ViewController/TimelineItinerary/AddPlan/ActivityListing/AddPlanActivityListingVC.swift`
- `TRPCoreKit/ViewController/TimelineItinerary/AddPlan/ActivityListing/AddPlanActivityListingViewModel.swift`
- `TRPCoreKit/ViewController/TimelineItinerary/AddPlan/POIListing/AddPlanPOIListingVC.swift`
- `TRPCoreKit/ViewController/TimelineItinerary/TRPTimelineItineraryVC+AddPlan.swift`

#### Android'de Yapılacaklar

**1. Scroll listener fix**:
- Activity/POI Listing RecyclerView'lerde infinite scroll listener'ı `onPause` veya `onDestroyView`'da kaldır.
- Aynı listener iki kez add etme — `isLoadingMore` flag kontrolünü `onScrollStateChanged` içinde sıkılaştır.

**2. Date scope**:
- `AddPlanContainerVM.kt` init:
  ```kotlin
  // availableDays'i trip startDate-endDate aralığıyla sınırla
  planData.availableDays = profile.tripDays.filter {
      !it.before(profile.tripStartDate) && !it.after(profile.tripEndDate)
  }
  ```

**3. Day selection edge cases**:
- selectedDay null ise ve availableDays boş ise → graceful empty state.
- selectedDay availableDays'te yok ise → first non-past day'e jump.

**4. iOS dosyalarındaki spesifik fix'leri okuyup birebir uygula**:
```bash
git show aa48976 -- TRPCoreKit/ViewController/TimelineItinerary/AddPlan/ActivityListing/AddPlanActivityListingVC.swift
```

#### Doğrulama
- AddPlan'da aşağı kaydırınca sayfa 2, 3... düzgün yükleniyor.
- Trip dışı tarih AddPlan day filter'da görünmüyor.

---

### Theme 17: Availability Sweep (Post-Load Check)

> **iOS commit:** `d494127`

#### Amaç
Timeline yüklendikten sonra **arka planda** tüm reserved activity'ler ve itinerary step'leri (activity tipinde olanlar) için Theme 5'teki `getTourScheduleAvailability` batch API ile availability kontrolü yapılır. Sonuç:
- Booked time o gün artık yok ise → `step.isAvailabilityExpired = true` veya `segment.additionalData.isAvailabilityExpired = true`.
- Cell UI: Theme 7'deki kırmızı badge + "Not available" text + image grayscale.

Sweep özellikleri:
- **One-shot**: timeline ilk yüklendikten sonra tek sefer çalışır (`hasRunInitialAvailabilityCheck` flag).
- **Cancellation**: yeni timeline load tetiklendiğinde önceki sweep iptal edilir (`availabilityCheckGeneration` token).
- **Selected day first**: kullanıcının baktığı gün önce kontrol edilir.
- **Past days skipped**: geçmiş günler sorgulanmaz.
- **Sequential per-day**: gün başına 1 API call (batch içinde tüm o günkü aktiviteler).

#### iOS Referans Dosyalar
- `TRPCoreKit/ViewController/TimelineItinerary/TRPTimelineItineraryViewModel+AvailabilityCheck.swift` (**yeni**, ~307 satır)
- `TRPCoreKit/TRPDataLayer/Domain/Models/Timeline/TRPTimelineStep.swift` (`isAvailabilityExpired: Bool` transient field)
- `TRPCoreKit/TRPDataLayer/Domain/Models/Timeline/TRPMergedTimelineItem.swift`
- `TRPCoreKit/TRPDataLayer/Domain/Models/TRPItineraryWithActivities.swift` (`TRPSegmentActivityItem.isAvailabilityExpired`)
- `TRPCoreKit/ViewController/TimelineItinerary/TRPTimelineItineraryViewModel.swift` (entry point)
- `TRPCoreKit/ViewController/TimelineItinerary/TRPTimelineItineraryViewModel+DataProcessing.swift`
- `TRPCoreKit/ViewController/TimelineItinerary/TRPTimelineItineraryViewModel+TimelineOperations.swift`
- `TRPCoreKit/ViewController/TimelineItinerary/Models/TimelineCellData.swift` (`isAvailabilityExpired` field tüm cell data'lara)
- `TRPCoreKit/ViewController/TimelineItinerary/Cells/TRPTimelineReservedActivityCell.swift` (expired styling)
- `TRPCoreKit/ViewController/TimelineItinerary/Cells/TRPTimelineRecommendationsCell.swift`

#### Android'de Yapılacaklar

**1. Transient flags** modellere ekle:
- `TimelineStep.isAvailabilityExpired: Boolean = false` (Gson `@Transient`).
- `TimelineSegment.additionalData.isAvailabilityExpired: Boolean = false` (transient).
- `MergedTimelineItem.isAvailabilityExpired` getter (mirror).

**2. AvailabilityCheckManager** — yeni helper class veya UseCase:
```kotlin
class AvailabilityCheckManager @Inject constructor(
    private val getTourScheduleAvailability: GetTourScheduleAvailabilityUseCase
) {
    private var hasRunInitialCheck: Boolean = false
    private var currentGeneration: Int = 0
    private var disposables: CompositeDisposable = CompositeDisposable()

    fun runInitialAvailabilityCheck(
        timeline: Timeline,
        selectedDate: Date?,
        currency: String,
        lang: String,
        onItemUpdated: (segmentIndex: Int, stepId: Int?, isExpired: Boolean) -> Unit,
        onCompleted: () -> Unit
    ) {
        if (hasRunInitialCheck) return
        hasRunInitialCheck = true
        currentGeneration++
        val gen = currentGeneration

        // 1. Collect targets
        val days = collectNonPastDaysWithSelectedFirst(timeline, selectedDate)
        // 2. Sequential per-day
        Observable.fromIterable(days)
            .concatMap { dayInfo ->
                if (gen != currentGeneration) return@concatMap Observable.empty()
                val items = collectActivityIdsForDay(timeline, dayInfo)
                if (items.isEmpty()) return@concatMap Observable.empty()
                getTourScheduleAvailability.executeRx(items, dayInfo.dateString, currency, lang)
                    .doOnNext { availability -> processResults(availability, dayInfo, onItemUpdated) }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { if (gen == currentGeneration) onCompleted() }
            .subscribe(...)
            .also { disposables.add(it) }
    }

    fun cancel() {
        currentGeneration++
        disposables.clear()
    }
}
```

**3. Target collection**:
- Her gün için:
  - Reserved segment'ler: `additionalData.activityId` veya `"C_{rawId}_15"` formatına çevir.
  - Itinerary step'leri (stepType == "activity"): `"C_{productId}_{providerId}"` formatla.
- Beklenen time: flexible için null, timed için "HH:mm".

**4. Availability rules** (response geldiğinde):
- Product missing from response → expired = true
- `schedule == null` → expired = true
- Timed target:
  - Schedule slot'larından hiçbiri `slot.time == expectedHHmm` değilse VE flexible slot da yoksa (`slot.time == null`) → expired = true
- Flexible target: hiç slot yoksa → expired = true

**5. ACTimelineVM entegrasyonu**:
- Timeline fetch tamamlanır tamamlanmaz `availabilityCheckManager.runInitialAvailabilityCheck(...)` çağır.
- `onItemUpdated` callback'i: ilgili segment/step'in flag'ini set et, sonra `_displayItems.value`'yi recompute et (LiveData re-emit).
- `setSelectedDay` çağrıldığında manager içindeki "selected day first" hesaplaması yeniden olabilir mi? Sweep zaten çalıştıysa hayır; çalışmadıysa selected day'i ilk sıraya al.

**6. Cell UI** (expired state):
- ReservedActivityVH ve RecommendationsVH step row:
  - Time badge: Theme 7'deki red expired styling.
  - Image: grayscale ColorMatrix.
  - "Not available" text status.
  - Reservation CTA hala görünür ama tıklayınca info: "Please refresh your booking." (optional).

**7. Cancel hijyeni**:
- ACTimelineVM `onCleared()`'da `availabilityCheckManager.cancel()`.
- Yeni timeline yüklendiğinde de cancel + reset (`hasRunInitialCheck = false`).

#### Doğrulama
- Timeline yüklendikten birkaç saniye sonra (network bağlı), eğer bir reserved aktivitenin saati artık schedule'da yoksa cell kırmızı "Not available" durumuna geçer.
- Gün değiştirince sweep önce baktığım günü kontrol eder.
- Geçmiş günler için API çağrısı yapılmaz.

---

## 3. Genel İmplementasyon Sırası (Önerilen)

1. **Theme 1: Lottie Loading System** — altyapı, sonraki tema'ların loader ihtiyacı bunu kullanacak.
2. **Theme 2: Tour Facets, Slots, Category Icons** — model katmanı, sonraki tema'lar bu modelleri kullanır.
3. **Theme 5: Tour Schedule Availability & Product Lookup** — yeni API'ler.
4. **Theme 6: No-Location Support** — Theme 5'e bağımlı (lookup ile city resolve).
5. **Theme 4: Flexible-Time Activities** — Theme 2'deki yeni TourSlot/Schedule model'ine bağımlı.
6. **Theme 7: Time Badge Warning Status Row** — sonraki conflict & availability tema'larının UI temeli.
7. **Theme 8: Conflict Detection & Banner** — Theme 7 UI'sını kullanır.
8. **Theme 17: Availability Sweep** — Theme 5 (API) + Theme 7 (UI) bağımlı.
9. **Theme 9: Time Selection Enhancements** — Theme 2 (slots) + Theme 4 (flexible) bağımlı.
10. **Theme 3: Past Day Handling** — birçok cell ve VM'de cross-cutting.
11. **Theme 10: Timeline Refresh State** — Theme 9 ve Saved Plans için ihtiyaç.
12. **Theme 11: POI Sort & Listing Improvements** — bağımsız.
13. **Theme 12: Collapsible Section Headers** — bağımsız.
14. **Theme 13: Manual POI Optional Step** — bağımsız bug fix.
15. **Theme 14: Layout Spacing & U+2212** — Theme 4, 7'den sonra polish.
16. **Theme 15: POI Cards Toggle on Map Tap** — bağımsız.
17. **Theme 16: AddPlan Scrolling Fixes** — son polish.

---

## 4. Doğrulama Checklist (Tamamlandığında)

- [ ] Timeline create akışı full-screen Lottie + rotating text gösteriyor.
- [ ] AddPlan Activity Listing'de yatay kategori chip'leri çalışıyor (All + facet kategoriler).
- [ ] Activity Listing filter modalında price/duration range slider'lar facet bounds ile başlıyor.
- [ ] Bir flexible activity timeline'da dashed border + `−` + "Flexible entry" gösteriyor.
- [ ] Geçmiş günler Timeline'da gri ama tıklanabilir, AddPlan'da gri ve seçilemez.
- [ ] Çakışan iki aktivite turuncu time badge + reserved'da "Time Overlap" metni gösteriyor.
- [ ] Conflict warning banner dismissable ve per-day track ediliyor.
- [ ] No-location aktivite cell'inde mavi "No exact location" badge görünüyor.
- [ ] Availability sweep timeline yüklendikten sonra çalışıyor, expired aktivite kırmızı "Not available" badge gösteriyor.
- [ ] TimeSelection 8+ slot varsa "Show more" cell gösteriyor.
- [ ] Edit time'da sold-out saat disabled placeholder + üstte kırmızı banner.
- [ ] TimelineRefreshState global olarak SavedPlans ve TimeSelection'da observe ediliyor.
- [ ] POI Listing sort modal çalışıyor, skeleton cell'ler ilk yüklemede görünüyor.
- [ ] Çoklu şehirli günlerde section header tıklayıp collapse olabiliyor.
- [ ] Order `−` karakterleri (U+2212) doğru gösteriliyor.
- [ ] Map'te marker'a tekrar tıklayınca preview card kapanıyor.
- [ ] AddPlan infinite scroll bug'sız çalışıyor.

---

## 5. Notlar

- **Build çalıştırma**: Her tema tamamlandığında kullanıcıdan onay alarak `./gradlew :TRPCore:assembleDebug` çalıştırılabilir. Otomatik build çalıştırma!
- **Commit**: User commit kararını verecek. Bekleyin.
- **Belirsizliklerde**: iOS'taki ilgili dosyayı oku ve ona göre karar ver. Tahmin yapma.
- **Test**: Test yazma. Mevcut testleri bozma.
- **Refactor**: Bu prompt'tan TALEP EDİLMEYEN refactor'leri yapma.

İyi çalışmalar.
