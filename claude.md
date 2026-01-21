# TRPCore Android SDK - Geliştirici Rehberi

Bu dosya, TRPCore projesinde hızlı ve hatasız geliştirme yapabilmek için hazırlanmış kapsamlı bir referans dökümantasyonudur.

---

## Proje Genel Bakış

**TRPCore**, seyahat planlama özellikleri sunan white-label bir Android SDK'dır. MVVM + Clean Architecture kullanılarak geliştirilmiştir.

### Temel Bilgiler
| Özellik | Değer |
|---------|-------|
| Min SDK | 24 (Android 7.0) |
| Target/Compile SDK | 35 |
| Kotlin Version | 2.0.21 |
| Java Version | 17 |
| DI Framework | Dagger 2 |
| Network | Retrofit + OkHttp |
| Reactive | RxJava 2 |
| Maps | Mapbox v11 |

---

## Dizin Yapısı

```
src/main/java/com/tripian/trpcore/
├── base/                    # Temel sınıflar ve initialization
│   ├── TRPCore.kt          # SDK entry point (Singleton)
│   ├── BaseActivity.kt     # Tüm Activity'lerin parent'ı
│   ├── BaseFragment.kt     # Tüm Fragment'ların parent'ı
│   ├── BaseViewModel.kt    # Tüm ViewModel'lerin parent'ı
│   ├── BaseUseCase.kt      # Tüm UseCase'lerin parent'ı
│   ├── Environment.kt      # DEV/PROD enum
│   └── AppConfig.kt        # Abstract configuration class
│
├── di/                      # Dagger Dependency Injection
│   ├── AppComponent.kt     # Root component
│   ├── AppModule.kt        # Application-wide providers
│   ├── NetworkModule.kt    # OkHttp, Retrofit, TRPOne
│   ├── RepositoryModule.kt # Repository singletons
│   ├── ViewModels.kt       # ViewModel bindings
│   ├── ViewModelFactory.kt # ViewModel creation
│   ├── ViewPages.kt        # Activity/Fragment injection
│   └── scopes/             # Feature scopes (@TripScope, @LoginScope, vb.)
│
├── domain/                  # Business Logic Layer
│   ├── model/              # Domain modelleri (PlaceItem, MapStep, vb.)
│   └── usecase/            # 100+ UseCase sınıfı
│       ├── login/          # Authentication use cases
│       ├── trip/           # Trip CRUD use cases
│       ├── plan/           # Plan management use cases
│       ├── step/           # Step operations use cases
│       ├── poi/            # POI/Places use cases
│       ├── companion/      # Companion use cases
│       ├── favorite/       # Favorites use cases
│       ├── offer/          # Offers use cases
│       └── misc/           # Diğer use cases
│
├── repository/              # Data Access Layer
│   ├── base/               # ResponseModelBase, ErrorModel
│   ├── TripianUserRepository.kt
│   ├── TripRepository.kt
│   ├── PlanRepository.kt
│   ├── StepRepository.kt
│   ├── PoiRepository.kt
│   ├── CompanionRepository.kt
│   ├── FavoriteRepository.kt
│   ├── OfferRepository.kt
│   └── Service.kt          # API interface (50+ endpoint)
│
├── ui/                      # Presentation Layer
│   ├── splash/             # ACSplash - Giriş ekranı
│   ├── login/              # ACLogin - Kimlik doğrulama
│   ├── mytrip/             # ACMyTrip - Trip listesi
│   ├── createtrip/         # ACCreateTrip - Trip oluşturma
│   ├── trip/               # ACTripMode - Aktif trip görünümü
│   ├── overview/           # ACOverView - Trip özeti
│   ├── places/             # ACPlaces - POI tarama
│   ├── profile/            # ACProfile - Kullanıcı profili
│   ├── user/               # ACEditProfile - Profil düzenleme
│   ├── companion/          # ACManageCompanion - Companion yönetimi
│   ├── butterfly/          # ACButterfly - Geri bildirim
│   ├── trip_detail/        # ACTripDetail - Trip detayları
│   └── common/             # Paylaşılan component'ler
│
└── util/                    # Utility sınıfları
    ├── Preferences.kt      # SharedPreferences wrapper
    ├── Strings.kt          # Localization helper
    ├── extensions/         # Kotlin extension functions
    ├── dialogs/            # Dialog helper'ları
    └── enums/              # Enum definitions
```

---

## Architecture Pattern

### MVVM + Clean Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                        │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────┐ │
│  │  Activity   │◄───│  ViewModel  │◄───│  Use Cases      │ │
│  │  Fragment   │    │             │    │  (Domain Layer) │ │
│  └─────────────┘    └─────────────┘    └────────┬────────┘ │
└────────────────────────────────────────────────│───────────┘
                                                 │
┌────────────────────────────────────────────────│───────────┐
│                      DATA LAYER                │           │
│  ┌─────────────────┐    ┌─────────────────────▼─────────┐ │
│  │   Repository    │◄───│         Service               │ │
│  │                 │    │    (API Interface)            │ │
│  └─────────────────┘    └───────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Katman Sorumlulukları

| Katman | Sorumluluk | Sınıflar |
|--------|------------|----------|
| **Presentation** | UI, user interaction | Activity, Fragment, ViewModel |
| **Domain** | Business logic | UseCase, Domain Models |
| **Data** | Data operations | Repository, Service, Response Models |

---

## Base Class'lar ve Kullanımları

### 1. BaseActivity<VB, VM>

Tüm Activity'ler bu class'ı extend etmelidir.

```kotlin
class YeniActivity : BaseActivity<ActivityYeniBinding, YeniViewModel>() {

    // ViewBinding oluşturma (ZORUNLU)
    override fun getViewBinding() = ActivityYeniBinding.inflate(layoutInflater)

    // ViewModel class'ı (ZORUNLU)
    override val viewModelClass = YeniViewModel::class.java

    // Toolbar başlığı (ZORUNLU)
    override fun getToolbarTitle() = "Sayfa Başlığı"

    // Back button göster (OPSIYONEL, default: true)
    override fun showBackButton() = true

    // DI injection'dan sonra çağrılır
    override fun afterInjection() {
        // View setup, click listeners, vb.
    }

    // ViewModel hazır olduktan sonra
    override fun onObserveViewModel() {
        // LiveData observers
    }
}
```

**Önemli Metodlar:**
- `showProgress()` / `hideProgress()` - Loading indicator
- `showAlert(title, message, type)` - Alert dialog
- `showSnackBar(message)` - Snackbar mesajı
- `showKeyboard()` / `hideKeyboard()` - Keyboard kontrolü
- `openFragment(fragment)` - Fragment açma

### 2. BaseFragment<VB, VM>

Tüm Fragment'lar bu class'ı extend etmelidir.

```kotlin
class YeniFragment : BaseFragment<FragmentYeniBinding, YeniViewModel>() {

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentYeniBinding.inflate(inflater, container, false)

    override val viewModelClass = YeniViewModel::class.java

    override fun afterInjection() {
        // Setup
    }

    override fun onObserveViewModel() {
        // Observers
    }
}
```

### 3. BaseViewModel

Tüm ViewModel'ler bu class'ı extend etmelidir.

```kotlin
class YeniViewModel @Inject constructor(
    private val yeniUseCase: YeniUseCase,
    private val digerUseCase: DigerUseCase
) : BaseViewModel() {

    // LiveData tanımları
    private val _data = MutableLiveData<DataModel>()
    val data: LiveData<DataModel> = _data

    // UseCase çağırma
    fun fetchData(param: String) {
        showProgress()
        yeniUseCase.execute(
            params = YeniUseCase.Params(param),
            onSuccess = { response ->
                _data.value = response.data
            },
            onError = { error ->
                handleError(error)
            },
            onFinally = {
                hideProgress()
            }
        )
    }

    // Lifecycle metodları (opsiyonel override)
    override fun onCreate() { }
    override fun onViewCreated() { }
    override fun onStart() { }
    override fun onResume() { }
    override fun onPause() { }
    override fun onDestroy() { }
}
```

**Önemli Metodlar:**
- `showProgress()` / `hideProgress()` - Loading state
- `showAlert(title, message, type)` - Alert gösterme
- `handleError(error)` - Hata yönetimi
- `getLanguageForKey(key)` - Localized string alma

### 4. BaseUseCase<Response, Params>

Tüm UseCase'ler bu class'ı extend etmelidir.

```kotlin
class YeniUseCase @Inject constructor(
    private val repository: YeniRepository
) : BaseUseCase<YeniResponse, YeniUseCase.Params>() {

    // Params data class (parametre yoksa Unit kullanılabilir)
    data class Params(
        val id: String,
        val limit: Int = 10
    )

    // API çağrısı (ZORUNLU implement)
    override fun on(params: Params): Observable<YeniResponse> {
        return repository.fetchData(params.id, params.limit)
    }
}
```

**UseCase Çağırma:**
```kotlin
useCase.execute(
    params = UseCase.Params(id = "123"),
    onSuccess = { response -> /* Handle success */ },
    onError = { error -> /* Handle error */ },
    onFinally = { /* Always called */ }
)
```

---

## Dependency Injection (Dagger 2)

### Scope'lar

| Scope | Kullanım Alanı |
|-------|----------------|
| `@Singleton` | Application-wide (Repository, Service) |
| `@LoginScope` | Login feature |
| `@MyTripScope` | My Trips feature |
| `@CreateTripScope` | Create Trip feature |
| `@TripScope` | Trip Mode feature |
| `@OverViewScope` | Overview feature |
| `@PlacesScope` | Places feature |
| `@UserScope` | User/Profile feature |
| `@CompanionScope` | Companion feature |
| `@ButterFlyScope` | Butterfly feature |

### Yeni Feature için DI Kurulumu

1. **Scope oluştur:**
```kotlin
// di/scopes/YeniScope.kt
@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class YeniScope
```

2. **Module oluştur:**
```kotlin
// di/YeniModule.kt
@Module
abstract class YeniModule {

    @YeniScope
    @ContributesAndroidInjector
    abstract fun contributeYeniActivity(): YeniActivity

    @YeniScope
    @ContributesAndroidInjector
    abstract fun contributeYeniFragment(): YeniFragment
}
```

3. **ViewPages'e ekle:**
```kotlin
// di/ViewPages.kt
@Module(includes = [
    // ... mevcut module'ler
    YeniModule::class
])
abstract class ViewPages
```

4. **ViewModel'i bind et:**
```kotlin
// di/ViewModels.kt
@Binds
@IntoMap
@ViewModelKey(YeniViewModel::class)
abstract fun bindYeniViewModel(viewModel: YeniViewModel): ViewModel
```

---

## API Endpoint'leri

### Service.kt Ana Endpoint'ler

#### User Management
```kotlin
fun login(request: LoginRequest): Observable<LoginResponse>
fun guestLogin(request: GuestLoginRequest): Observable<LoginResponse>
fun lightLogin(request: LightLoginRequest): Observable<LoginResponse>
fun register(request: RegisterRequest): Observable<LoginResponse>
fun getUser(): Observable<UserResponse>
fun updateUser(request: UpdateUserRequest): Observable<UserResponse>
fun deleteUser(): Observable<EmptyResponse>
fun logout(): Observable<EmptyResponse>
```

#### Trip Management
```kotlin
fun createTrip(request: TripRequest): Observable<TripResponse>
fun updateTrip(tripHash: String, request: TripRequest): Observable<TripResponse>
fun deleteTrip(tripHash: String): Observable<DeleteResponse>
fun fetchTrip(tripHash: String): Observable<TripResponse>
fun getUserTrip(from: String?, to: String?, limit: Int, page: Int): Observable<TripsResponse>
```

#### Plan & Steps
```kotlin
fun fetchPlan(planId: Int): Observable<PlanResponse>
fun updatePlan(planId: Int, request: UpdatePlanRequest): Observable<PlanResponse>
fun addStep(request: AddStepRequest): Observable<StepResponse>
fun updateStep(stepId: Int, request: UpdateStepRequest): Observable<StepResponse>
fun deleteStep(stepId: Int): Observable<DeleteResponse>
fun getStepAlternatives(tripHash: String, planId: Int, stepId: Int): Observable<StepAlternativesResponse>
```

#### POI (Points of Interest)
```kotlin
fun getPoi(
    cityId: Int?,
    poiCategories: String?,
    searchFor: String?,
    boundaryNorthEast: String?,
    boundarySouthWest: String?,
    mustTryPoi: Boolean?,
    showOnlyLocalExperiences: Boolean?,
    typeId: Int?,
    limit: Int,
    page: Int
): Observable<PoisResponse>

fun getPoiInfo(poiId: String): Observable<PoiResponse>
fun getCities(search: String?, limit: Int, page: Int): Observable<GetCitiesResponse>
```

#### Companion
```kotlin
fun getUserCompanions(limit: Int, page: Int): Observable<CompanionsResponse>
fun addCompanion(request: CompanionRequest): Observable<CompanionResponse>
fun updateCompanion(companionId: Int, request: CompanionRequest): Observable<CompanionResponse>
fun deleteCompanion(companionId: Int): Observable<DeleteResponse>
```

#### Favorites & Reactions
```kotlin
fun addUserFavorites(request: FavoriteRequest): Observable<FavoriteResponse>
fun deleteUserFavorites(favoriteId: Int): Observable<DeleteResponse>
fun getUserFavorites(cityId: Int?, limit: Int, page: Int): Observable<FavoritesResponse>
fun addReaction(request: ReactionRequest): Observable<ReactionResponse>
fun updateReaction(reactionId: Int, request: ReactionRequest): Observable<ReactionResponse>
fun deleteReaction(reactionId: Int): Observable<DeleteResponse>
```

---

## Yeni Feature Ekleme Rehberi

### Adım 1: Domain Layer

**UseCase oluştur:**
```kotlin
// domain/usecase/yeni/YeniUseCase.kt
class YeniUseCase @Inject constructor(
    private val repository: YeniRepository
) : BaseUseCase<YeniResponse, YeniUseCase.Params>() {

    data class Params(val id: String)

    override fun on(params: Params): Observable<YeniResponse> {
        return repository.getData(params.id)
    }
}
```

### Adım 2: Data Layer

**Repository oluştur (gerekirse):**
```kotlin
// repository/YeniRepository.kt
class YeniRepository @Inject constructor(
    private val service: Service
) {
    fun getData(id: String): Observable<YeniResponse> {
        return service.getYeniData(id)
    }
}
```

**Service'e endpoint ekle:**
```kotlin
// repository/Service.kt
fun getYeniData(id: String): Observable<YeniResponse>
```

### Adım 3: Presentation Layer

**ViewModel oluştur:**
```kotlin
// ui/yeni/YeniViewModel.kt
class YeniViewModel @Inject constructor(
    private val yeniUseCase: YeniUseCase
) : BaseViewModel() {

    private val _data = MutableLiveData<YeniModel>()
    val data: LiveData<YeniModel> = _data

    fun loadData(id: String) {
        yeniUseCase.execute(
            params = YeniUseCase.Params(id),
            onSuccess = { _data.value = it.data },
            onError = { handleError(it) }
        )
    }
}
```

**Activity/Fragment oluştur:**
```kotlin
// ui/yeni/YeniActivity.kt
class YeniActivity : BaseActivity<ActivityYeniBinding, YeniViewModel>() {

    override fun getViewBinding() = ActivityYeniBinding.inflate(layoutInflater)
    override val viewModelClass = YeniViewModel::class.java
    override fun getToolbarTitle() = "Yeni Sayfa"

    override fun afterInjection() {
        viewModel.loadData("123")
    }

    override fun onObserveViewModel() {
        viewModel.data.observe(this) { data ->
            // UI güncelle
        }
    }
}
```

### Adım 4: DI Kurulumu

```kotlin
// 1. Scope (opsiyonel, mevcut scope kullanılabilir)
@Scope
annotation class YeniScope

// 2. Module
@Module
abstract class YeniModule {
    @YeniScope
    @ContributesAndroidInjector
    abstract fun contributeYeniActivity(): YeniActivity
}

// 3. ViewPages'e ekle
// 4. ViewModel'i bind et
```

### Adım 5: Manifest'e Ekle

```xml
<activity
    android:name=".ui.yeni.YeniActivity"
    android:screenOrientation="portrait" />
```

---

## Bug Fix Rehberi

### Hata Ayıklama Yaklaşımı

1. **Log'ları kontrol et:**
   - `TrpLog.d(TAG, message)` - Debug log
   - `TrpLog.e(TAG, message)` - Error log

2. **API hatalarını incele:**
   - `BaseUseCase` içindeki `handleApiError()` metodunu kontrol et
   - HTTP status code'ları: 200 success, 401 unauthorized, 500 server error

3. **Lifecycle sorunları:**
   - `BaseViewModel` lifecycle metodlarını kontrol et
   - Disposable'ların düzgün temizlendiğinden emin ol

### Sık Karşılaşılan Sorunlar

| Sorun | Olası Neden | Çözüm |
|-------|-------------|-------|
| NullPointerException | ViewBinding erken erişim | `afterInjection()` kullan |
| API 401 hatası | Token expired | Token refresh mekanizmasını kontrol et |
| Memory leak | Observer temizlenmemiş | `onDestroy()`'da temizle |
| UI güncellenmedi | LiveData observe edilmemiş | `onObserveViewModel()` kontrol et |

---

## Localization (Çoklu Dil Desteği)

### Desteklenen Diller
- English (en)
- Turkish (tr)
- German (de)
- French (fr)
- Spanish (es)

### Kullanım

```kotlin
// ViewModel'den
val text = getLanguageForKey(LanguageConst.ERROR_ENTER_EMAIL)

// Activity/Fragment'tan
val text = viewModel.getLanguageForKey(LanguageConst.COMMON_ERROR)
```

### Yeni String Ekleme

1. `util/LanguageConst.kt`'ye constant ekle
2. Backend'de tüm dillere çeviri ekle
3. `getLanguageValues()` API'sinden otomatik çekilir

---

## SharedPreferences

### Preferences.kt Kullanımı

```kotlin
// Injection
@Inject lateinit var preferences: Preferences

// Değer kaydetme
preferences.saveString(Preferences.USER_EMAIL, "user@example.com")
preferences.saveBoolean(Preferences.USER_LOGIN, true)

// Değer okuma
val email = preferences.getString(Preferences.USER_EMAIL, "")
val isLoggedIn = preferences.getBoolean(Preferences.USER_LOGIN, false)
```

### Mevcut Key'ler

| Key | Tip | Açıklama |
|-----|-----|----------|
| `DEVICE_ID` | String | Unique device identifier |
| `USER_LOGIN` | Boolean | Login durumu |
| `USER_LOGIN_TIME` | Long | Son login zamanı |
| `APP_LANGUAGE` | String | Seçili dil kodu |
| `APP_LANGUAGE_TRANSLATIONS` | String | Cache'lenmiş çeviriler (JSON) |
| `TOKEN_TYPE` | String | Token tipi (Bearer) |
| `ACCESS_TOKEN` | String | Access token |
| `REFRESH_TOKEN` | String | Refresh token |

---

## EventBus Kullanımı

### Event Gönderme

```kotlin
EventBus.getDefault().post(YeniEvent(data))
```

### Event Alma

```kotlin
@Subscribe(threadMode = ThreadMode.MAIN)
fun onYeniEvent(event: YeniEvent) {
    // Handle event
}

// BaseViewModel otomatik register/unregister yapar
```

### Mevcut Event'ler

- `FavoriteAddedEvent` - Favori eklendi
- `FavoriteRemovedEvent` - Favori silindi
- `StepDeletedEvent` - Step silindi
- `TripUpdatedEvent` - Trip güncellendi
- `LanguageChangedEvent` - Dil değişti

---

## Test Yazma

### Unit Test (UseCase)

```kotlin
@Test
fun `test fetch data success`() {
    val mockRepository = mock<YeniRepository>()
    val useCase = YeniUseCase(mockRepository)

    whenever(mockRepository.getData("123"))
        .thenReturn(Observable.just(mockResponse))

    useCase.execute(
        params = YeniUseCase.Params("123"),
        onSuccess = { result ->
            assertEquals(expected, result)
        }
    )
}
```

### Instrumentation Test (UI)

```kotlin
@Test
fun testYeniActivityLaunches() {
    val scenario = launchActivity<YeniActivity>()
    scenario.onActivity { activity ->
        assertNotNull(activity.binding)
    }
}
```

---

## Önemli Notlar

### Kod Standartları

1. **Naming Convention:**
   - Activity: `AC` prefix (örn: `ACMyTrip`)
   - Fragment: `FR` prefix (örn: `FRTripList`)
   - ViewModel: `VM` suffix yok, sadece `ViewModel` (örn: `MyTripViewModel`)
   - UseCase: İşlem ismi (örn: `FetchTripsUseCase`, `CreateTripUseCase`)

2. **Package Structure:**
   - Feature-based organization
   - Her feature kendi package'ında

3. **ViewBinding:**
   - Her ekran ViewBinding kullanmalı
   - `binding` property'si `afterInjection()`'dan sonra güvenli

4. **Statik Metin Kullanılmaz (ÖNEMLİ):**
   - Hiçbir UI metni hardcoded/statik olarak yazılmamalı
   - **strings.xml KULLANILMAZ** - `getString(R.string.xxx)` veya `@string/xxx` asla kullanılmamalı
   - Tüm metinler language service üzerinden alınmalı:
     - ViewModel'den: `getLanguageForKey(LanguageConst.XXX)`
     - Adapter/ViewHolder'dan: `TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.XXX)`
   - XML layout'larda `android:text` yerine `tools:text` kullanılmalı (sadece preview için)
   - Button, TextView, hint, title vb. tüm metinler Activity/Fragment/ViewHolder'da dinamik olarak set edilmeli
   - Yeni metin gerektiğinde önce `LanguageConsts.kt`'ye key eklenmeli
   - Örnek:
     ```kotlin
     // YANLIŞ - Statik metin
     binding.btnFilter.text = "Filters"

     // YANLIŞ - strings.xml kullanımı
     binding.btnFilter.text = getString(R.string.filters)

     // DOĞRU - Language service'den (ViewModel)
     binding.btnFilter.text = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_FILTERS)

     // DOĞRU - Language service'den (Adapter/ViewHolder)
     binding.btnFilter.text = TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.ADD_PLAN_FILTERS)
     ```

### Performance İpuçları

1. **RxJava:**
   - `subscribeOn(Schedulers.io())` - Network işlemleri
   - `observeOn(AndroidSchedulers.mainThread())` - UI güncellemeleri
   - BaseUseCase otomatik halleder

2. **Memory:**
   - Disposable'ları temizle
   - Large bitmap'leri Glide ile yükle
   - Context leak'lerden kaçın

3. **Network:**
   - Timeout: 120 saniye
   - Retry mekanizması BaseUseCase'de

---

## Bağımlılıklar ve Versiyonlar

### Core Dependencies

```gradle
// Dagger
implementation "com.google.dagger:dagger:2.28.3"
kapt "com.google.dagger:dagger-compiler:2.28.3"
implementation "com.google.dagger:dagger-android-support:2.22.1"
kapt "com.google.dagger:dagger-android-processor:2.22.1"

// RxJava
implementation "io.reactivex.rxjava2:rxjava:2.2.21"
implementation "io.reactivex.rxjava2:rxandroid:2.1.1"

// Retrofit
implementation "com.squareup.retrofit2:retrofit:2.6.1"
implementation "com.squareup.retrofit2:converter-gson:2.6.1"
implementation "com.squareup.retrofit2:adapter-rxjava2:2.6.1"

// Mapbox
implementation "com.mapbox.maps:android:11.16.0"

// Firebase
implementation platform("com.google.firebase:firebase-bom:33.1.2")
implementation "com.google.firebase:firebase-crashlytics-ktx"
implementation "com.google.firebase:firebase-analytics-ktx"
```

### Internal Tripian Dependencies

```gradle
implementation "com.github.AhmetKutsworking:TRPProviderKit-Android:1.0.0"
implementation "com.github.AhmetKutsworking:TRPOne-Android:1.0.0"
implementation "com.github.AhmetKutsworking:TRPAuth-Android:1.0.0"
implementation "com.github.AhmetKutsworking:TRPGyg-Android:1.0.0"
implementation "com.github.Tripian-inc:TRPFoundation-Android:1.0.0"
```

---

## Hızlı Referans

### Yeni Ekran Checklist

- [ ] Layout XML oluştur (`res/layout/activity_xxx.xml`)
- [ ] ViewBinding class'ı otomatik generate edilecek
- [ ] ViewModel oluştur
- [ ] Activity/Fragment oluştur
- [ ] UseCase(ler) oluştur
- [ ] DI Module oluştur veya mevcut module'e ekle
- [ ] ViewModels.kt'ye bind ekle
- [ ] ViewPages.kt'ye module ekle
- [ ] AndroidManifest.xml'e activity ekle

### Yeni API Endpoint Checklist

- [ ] Service.kt'ye method ekle
- [ ] Request/Response model'leri oluştur (veya TRPOne'dan kullan)
- [ ] Repository'ye method ekle (veya yeni repository oluştur)
- [ ] UseCase oluştur
- [ ] RepositoryModule'e inject ekle (yeni repository ise)

---

## Internal SDK'lar (Aynı Repo'da)

Tüm internal SDK'ların kaynak koduna erişim mevcuttur. Lokasyon: `/Users/cemcaygoz/Documents/GitHub/Android/Tripian Public Modules /`

### SDK Dependency Grafiği

```
TRPCore (Ana Modül)
├── TRPOne (REST API Client)
│   └── Token Management (internal)
├── TRPAuth (AWS Cognito Authentication)
├── TRPGyg (GetYourGuide Tours)
├── TRPProvider (Yelp Reservations)
└── TRPFoundation (Base Utilities)
```

### 1. TRPOne - Core API Client

**Konum:** `../TRPOne/`

**Amaç:** Tüm Tripian REST API iletişimini yöneten ana client library.

**Ana Sınıflar:**
| Sınıf | Sorumluluk |
|-------|------------|
| `TRPRest` | Public API facade, tüm endpoint'leri expose eder |
| `TRPRestBase` | Coroutine tabanlı request handling, token refresh |
| `TNetwork` | Retrofit/OkHttp konfigürasyonu |
| `TConfig` | API URL, key, device info, language settings |

**Service Sınıfları (Lazy Initialized):**
- `TUsers` - Authentication, profile
- `TTrips` - Trip CRUD, step management
- `TCities` - City search
- `TPois` - POI search/details
- `TCompanions` - Companion management
- `TFavorites` - Favorites
- `TBookings` - Reservations
- `TOffers` - Offers
- `TMisc` - Languages, configs
- `TTours` - GetYourGuide integration
- `TTimeline` - Timeline itinerary

**TRPCore'da Kullanımı:**
```kotlin
// NetworkModule.kt'de initialize edilir
@Provides
@Singleton
fun provideTRPOne(app: Application): TRPRest {
    return TRPRest.Builder()
        .setContext(app)
        .setApiKey(tripianApiKey)
        .setBaseUrl(baseUrl)
        .build()
}
```

### 2. TRPAuth - AWS Cognito Authentication

**Konum:** `../TRPAuth/`

**Amaç:** OAuth 2.0 + PKCE flow ile AWS Cognito authentication.

**Ana Sınıflar:**
| Sınıf | Sorumluluk |
|-------|------------|
| `Auth` | Builder pattern ile Cognito setup |
| `AuthClient` | Token caching, refresh, Chrome Custom Tabs |
| `AuthUserSession` | ID/Access/Refresh token container |
| `Pkce` | PKCE code challenge generation |

**Initialization:**
```kotlin
Auth.Builder()
    .setApplicationContext(context)
    .setUserPoolId(userPoolId)
    .setAppClientId(clientId)
    .setAppCognitoWebDomain(domain)
    .setSignInRedirect(redirectUri)
    .setSignOutRedirect(logoutUri)
    .setScopes(scopes)
    .setAuthHandler(handler)
    .build()
```

**Public API:**
- `getSession(activity)` - Token al, gerekirse browser aç
- `getSessionWithoutWebUI()` - Cache'den token al
- `isAuthenticated()` - Login kontrolü
- `signOut()` - Logout
- `getTokens(uri)` - Auth code exchange

### 3. TRPGyg - GetYourGuide Integration

**Konum:** `../TRPGyg/`

**Amaç:** GetYourGuide tur/aktivite arama, detay ve booking flow.

**Ana Sınıflar:**
| Sınıf | Sorumluluk |
|-------|------------|
| `Tripian` | Singleton config class |
| `ACExperiences` | Tur listesi ekranı |
| `ACExperienceDetail` | Tur detay ekranı |
| `ACBook` | Booking flow |

**Initialization:**
```kotlin
Tripian.init(apiKey)
Tripian.setGetLanguage { key -> translateKey(key) }
Tripian.setSuccessListener { paymentResponse -> handleSuccess() }
```

**UI Ekranları:**
- `ACExperiences` - Browse tours
- `ACExperienceDetail` - Tour details
- `ACExperienceReviews` - Reviews
- `ACBook` - Booking with payment (Adyen)

### 4. TRPProvider - Yelp Reservations

**Konum:** `../TRPProvider/`

**Amaç:** Yelp üzerinden restoran rezervasyonları.

**Ana Sınıflar:**
| Sınıf | Sorumluluk |
|-------|------------|
| `ProviderCore` | Singleton, DI container |
| `ACProvider` | Main provider activity |
| `ReservationStatus` | Rezervasyon durumu kontrolü |
| `CancelReservation` | Rezervasyon iptali |

**Initialization:**
```kotlin
ProviderCore().init(app, yelpApiKey)
```

**Public API:**
```kotlin
// Rezervasyon durumu
ProviderCore.reservationStatus(reservationId) { status ->
    // status.active, status.covers, status.date, status.time
}

// Rezervasyon iptali
ProviderCore.cancelReservation(reservationId) { result ->
    // Handle cancellation
}
```

### 5. TRPFoundation - Base Utilities

**Konum:** `../TRPFoundation-Android/`

**Amaç:** Tüm SDK'lar için ortak base class'lar ve utility'ler.

**İçerik:**
- Base Activity/Fragment classes
- Base ViewModel structures
- Extension functions (String, Date, View)
- Coroutine helpers
- Resource management utilities

---

## Geliştirme Kuralları

### Genel İlkeler
- **Senior Android Developer seviyesinde** kod yazılmalı
- **Hatasız geliştirme** - Compile error ve runtime crash olmamalı
- **Design pattern'lere uyum** - MVVM, Repository Pattern, Clean Architecture
- **Soru sormaktan çekinme** - Belirsiz durumlarda kullanıcıya danış

### Test Politikası
- **Test yazılmayacak** - Kullanıcı tercihi
- Mevcut testler korunacak ama yeni test eklenmeyecek

### Kod Değişikliği Yapılacak Modüller

| Değişiklik Tipi | Modül | Dikkat Edilecekler |
|-----------------|-------|-------------------|
| Yeni UI ekranı | TRPCore | Base class'ları kullan, DI kur |
| Yeni API endpoint | TRPOne | Service interface'e ekle |
| Auth değişikliği | TRPAuth | Token flow'u bozma |
| Tour feature | TRPGyg | Tripian singleton'ı kullan |
| Reservation | TRPProvider | ProviderCore'u kullan |

### Cross-Module Değişiklik Sırası

Birden fazla modülde değişiklik gerektiğinde:

1. **TRPFoundation** - Base class değişiklikleri
2. **TRPOne** - API değişiklikleri
3. **TRPAuth** - Auth değişiklikleri (gerekirse)
4. **TRPGyg/TRPProvider** - Feature modülleri
5. **TRPCore** - UI ve integration

---

## Sık Kullanılan Dosya Konumları

### TRPCore
```
ui/                          → Ekranlar
domain/usecase/              → Business logic
repository/                  → Data layer
di/                          → Dependency injection
util/                        → Utilities
```

### TRPOne
```
src/main/java/com/tripian/trpone/
├── TRPRest.kt              → Ana API facade
├── network/TNetwork.kt     → Network config
├── config/TConfig.kt       → Configuration
└── services/               → Feature services
```

### TRPGyg
```
src/main/java/com/tripian/trpgyg/
├── Tripian.kt              → Singleton config
├── ui/                     → Activities & Fragments
└── data/                   → Models & API
```

### TRPProvider
```
src/main/java/com/tripian/trpprovider/
├── ProviderCore.kt         → Entry point
├── ui/                     → Activities & Fragments
└── domain/                 → Use cases
```

---

## Timeline Modülü Implementasyonu

### Genel Bakış

Timeline Itinerary, kullanıcıların seyahat planlarını görüntülemesini ve yönetmesini sağlayan ana modül.

**Özellikler:**
- Gün bazlı filtreleme (horizontal scrollable day selector)
- Şehir gruplama (aynı günde birden fazla şehir varsa)
- 4 segment tipi: itinerary, bookedActivity, reservedActivity, manualPoi
- Smart Recommendations (AI destekli öneri oluşturma)
- AddPlan Flow (BottomSheet ile yeni aktivite ekleme)
- Harita görünümü (Mapbox)

### TRPOne'da Mevcut Timeline API'leri

TRPOne SDK'da Timeline metodları **callback pattern** ile çalışır:

```kotlin
// TRPRest üzerinden erişim
trpRest.getTimeline(hash, success, error)
trpRest.createTimeline(settings, success, error)
trpRest.editTimelineSegment(hash, segment, success, error)
trpRest.deleteTimelineSegment(hash, segmentIndex, success, error)
trpRest.addTimelineStep(planId, poiId, startTime, endTime, order, success, error)
trpRest.deleteTimelineStep(stepId, success, error)
trpRest.getUserTimelines(dateFrom, dateTo, limit, success, error)
```

### Timeline Dosya Yapısı (Oluşturulacak)

```
src/main/java/com/tripian/trpcore/
├── domain/
│   ├── model/timeline/
│   │   ├── TimelineDisplayItem.kt      // Sealed class (UI modeli)
│   │   └── AddPlanData.kt              // AddPlan flow verisi
│   └── usecase/timeline/
│       ├── FetchTimelineUseCase.kt
│       ├── CreateSegmentUseCase.kt
│       ├── DeleteSegmentUseCase.kt
│       ├── AddStepUseCase.kt
│       └── WaitForGenerationUseCase.kt
│
├── repository/
│   └── TimelineRepository.kt           // TRPOne wrapper (RxJava)
│
├── ui/timeline/
│   ├── ACTimeline.kt                   // Ana Activity
│   ├── ACTimelineVM.kt                 // ViewModel
│   ├── adapter/
│   │   ├── TimelineAdapter.kt
│   │   ├── BookedActivityVH.kt
│   │   ├── RecommendationsVH.kt
│   │   ├── SectionHeaderVH.kt
│   │   └── DayFilterAdapter.kt
│   ├── views/
│   │   ├── TimelineNavigationBar.kt
│   │   ├── TimelineDayFilterView.kt
│   │   ├── TimelineTabView.kt
│   │   └── SavedPlansButton.kt
│   └── addplan/
│       ├── AddPlanBottomSheet.kt
│       ├── FRSelectDay.kt
│       ├── FRTimeAndTravelers.kt
│       ├── FRCategorySelection.kt
│       └── ACPOISelection.kt
│
└── di/
    ├── scopes/TimelineScope.kt
    └── TimelineModule.kt
```

### Segment Tipleri

| Segment Type | İçerik | UI Görünümü |
|--------------|--------|-------------|
| `itinerary` | Plans ve steps içerir | Recommendations card |
| `booked_activity` | NO plans, additionalData'da bilgi | "Confirmed" badge |
| `reserved_activity` | NO plans, additionalData'da bilgi | "Reservation" button |
| `manual_poi` | Custom POI | Düzenlenebilir |

### Kritik Kurallar

#### 1. Duration/Price Gösterimi
```kotlin
// SADECE reserved_activity için göster
if (isReserved && duration != null && duration > 0) {
    llDuration.visibility = View.VISIBLE
} else {
    llDuration.visibility = View.GONE  // booked için HER ZAMAN gizle
}
```

#### 2. Tek Şehir Kontrolü
```kotlin
// Tek şehir varsa city selection butonunu gizle
if (cities.size <= 1) {
    btnCitySelection.visibility = View.GONE
    // Şehri otomatik seç
}
```

#### 3. Near Me Button Görünürlüğü
```kotlin
// Kullanıcı şehirde değilse gizle (50km radius kontrolü)
fun isUserInCity(userLocation: Location, city: City): Boolean
```

#### 4. Şehir Gruplama
- Aynı günde birden fazla şehir varsa section header ekle
- Kronolojik olarak ilk item'ın şehri önce gösterilir
- Diğer şehirler alfabetik sırada

#### 5. Segment Title Oluşturma
```kotlin
// "Recommendations" varsa "Recommendations 2", "Recommendations 3" vb.
fun generateSegmentTitle(city: City, date: Date): String
```

### Generation Polling

Segment oluşturduktan sonra polling ile bekle:
- Interval: 2 saniye
- Max retry: 30 (toplam 60 saniye)
- `timeline.isGenerated()` true olana kadar bekle

### TimelineRepository Pattern

```kotlin
class TimelineRepository @Inject constructor(
    private val trpRest: TRPRest
) {
    // Callback → RxJava Single dönüşümü
    fun fetchTimeline(tripHash: String): Single<Timeline> {
        return Single.create { emitter ->
            trpRest.getTimeline(
                hash = tripHash,
                success = { response ->
                    response.data?.let { emitter.onSuccess(it) }
                        ?: emitter.onError(Exception("Data null"))
                },
                error = { emitter.onError(it ?: Exception("Unknown")) }
            )
        }
    }
}
```

### Implementasyon Sırası

1. **Repository Layer**
   - TimelineRepository (TRPOne wrapper)

2. **Domain Layer**
   - TimelineDisplayItem sealed class
   - UseCases (Fetch, Create, Delete, Wait)

3. **DI Setup**
   - TimelineScope, TimelineModule
   - ViewModels bindings

4. **UI - Ana Ekran**
   - ACTimeline + ACTimelineVM
   - TimelineAdapter + ViewHolders
   - Custom Views (DayFilter, TabView, etc.)

5. **UI - AddPlan Flow**
   - AddPlanBottomSheet
   - FRSelectDay, FRTimeAndTravelers, FRCategorySelection
   - ACPOISelection

6. **Resources**
   - Layout XMLs
   - Drawables (icons)
   - strings.xml, colors.xml, dimens.xml

### Referans Doküman

Detaylı implementasyon için:
`/Users/cemcaygoz/Documents/Tripian Works/Tripian One/TRPCoreKit-SPM/TRPCoreKit-SPM/ANDROID_TIMELINE_IMPLEMENTATION_PROMPT.md`

---

*Bu döküman, TRPCore projesinde hızlı ve tutarlı geliştirme yapabilmek için hazırlanmıştır. Güncel tutulmalıdır.*
