package sergirex.portadasrevistas.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import sergirex.portadasrevistas.data.catalog.BundledCatalog
import sergirex.portadasrevistas.data.catalog.CatalogCache
import sergirex.portadasrevistas.data.catalog.CatalogConfig
import sergirex.portadasrevistas.data.catalog.RemoteCatalogSource
import java.io.File
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import sergirex.portadasrevistas.data.local.DataStoreIssueSeenRepository
import sergirex.portadasrevistas.data.local.DataStoreScaleStore
import sergirex.portadasrevistas.data.local.ScaleStore
import sergirex.portadasrevistas.data.network.CoverCacheControlInterceptor
import sergirex.portadasrevistas.data.repository.EngagementRepositoryImpl
import sergirex.portadasrevistas.data.repository.AdsEntitlementRepositoryImpl
import sergirex.portadasrevistas.data.repository.WidgetStateRepositoryImpl
import sergirex.portadasrevistas.data.repository.FavoriteRevistasRepositoryImpl
import sergirex.portadasrevistas.data.repository.IssueCalendarRepositoryImpl
import sergirex.portadasrevistas.data.repository.PageScaleRepositoryImpl
import sergirex.portadasrevistas.data.repository.ReadingProgressRepositoryImpl
import sergirex.portadasrevistas.data.repository.RevistaCoverRepositoryImpl
import sergirex.portadasrevistas.data.repository.RevistasRepositoryImpl
import sergirex.portadasrevistas.data.repository.SettingsRepositoryImpl
import sergirex.portadasrevistas.data.repository.ThemeRepositoryImpl
import sergirex.portadasrevistas.domain.repository.EngagementRepository
import sergirex.portadasrevistas.domain.repository.AdsEntitlementRepository
import sergirex.portadasrevistas.domain.repository.WidgetStateRepository
import sergirex.portadasrevistas.domain.repository.FavoriteRevistasRepository
import sergirex.portadasrevistas.domain.repository.IssueCalendarRepository
import sergirex.portadasrevistas.domain.repository.IssueSeenRepository
import sergirex.portadasrevistas.domain.repository.PageScaleRepository
import sergirex.portadasrevistas.domain.repository.ReadingProgressRepository
import sergirex.portadasrevistas.domain.repository.RevistaCoverRepository
import sergirex.portadasrevistas.domain.repository.RevistasRepository
import sergirex.portadasrevistas.domain.repository.SettingsRepository
import sergirex.portadasrevistas.domain.repository.ThemeRepository
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Wires domain-layer interfaces to their data-layer implementations. This is the one place that
 * knows the concrete classes exist; use cases and ViewModels depend only on the interfaces, which
 * is what makes them testable with fakes.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindRevistasRepository(impl: RevistasRepositoryImpl): RevistasRepository

    @Binds
    @Singleton
    abstract fun bindRevistaCoverRepository(impl: RevistaCoverRepositoryImpl): RevistaCoverRepository

    @Binds
    @Singleton
    abstract fun bindIssueCalendarRepository(impl: IssueCalendarRepositoryImpl): IssueCalendarRepository

    @Binds
    @Singleton
    abstract fun bindFavoriteRevistasRepository(impl: FavoriteRevistasRepositoryImpl): FavoriteRevistasRepository

    @Binds
    @Singleton
    abstract fun bindThemeRepository(impl: ThemeRepositoryImpl): ThemeRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindEngagementRepository(impl: EngagementRepositoryImpl): EngagementRepository

    @Binds
    @Singleton
    abstract fun bindAdsEntitlementRepository(impl: AdsEntitlementRepositoryImpl): AdsEntitlementRepository

    @Binds
    @Singleton
    abstract fun bindWidgetStateRepository(impl: WidgetStateRepositoryImpl): WidgetStateRepository

    @Binds
    @Singleton
    abstract fun bindReadingProgressRepository(impl: ReadingProgressRepositoryImpl): ReadingProgressRepository

    @Binds
    @Singleton
    abstract fun bindPageScaleRepository(impl: PageScaleRepositoryImpl): PageScaleRepository

    @Binds
    @Singleton
    abstract fun bindIssueSeenRepository(impl: DataStoreIssueSeenRepository): IssueSeenRepository

    @Binds
    @Singleton
    abstract fun bindScaleStore(impl: DataStoreScaleStore): ScaleStore

    companion object {
        @Provides
        @Singleton
        fun provideBundledCatalog(@ApplicationContext context: Context): BundledCatalog =
            BundledCatalog { context.assets.open("revistas.json").bufferedReader().use { it.readText() } }

        @Provides
        @Singleton
        fun provideCatalogCache(@ApplicationContext context: Context): CatalogCache =
            CatalogCache(File(context.filesDir, "catalog"))

        @Provides
        @Singleton
        fun provideRemoteCatalogSource(client: OkHttpClient): RemoteCatalogSource =
            RemoteCatalogSource(client, CatalogConfig.URL)

        /** Shared by Coil (image downloads) and the cover/calendar requests, so all reuse connections. */
        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient =
            OkHttpClient.Builder()
                // Most requests go to two hosts; the default of 5 concurrent requests per host made a
                // tab wait behind another tab's probes and thumbnails.
                .dispatcher(Dispatcher().apply { maxRequestsPerHost = 12 })
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .addNetworkInterceptor(CoverCacheControlInterceptor())
                .build()
    }
}
