package sergirex.portadasrevistas.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import sergirex.portadasrevistas.domain.model.HomeTab
import sergirex.portadasrevistas.domain.model.Revista
import sergirex.portadasrevistas.domain.model.RevistaCategory
import sergirex.portadasrevistas.domain.model.RevistaCover
import sergirex.portadasrevistas.domain.model.RevistaRef
import sergirex.portadasrevistas.domain.repository.FavoriteRevistasRepository
import sergirex.portadasrevistas.domain.repository.RevistaCoverRepository
import sergirex.portadasrevistas.domain.repository.RevistasRepository
import sergirex.portadasrevistas.domain.repository.SettingsRepository
import sergirex.portadasrevistas.domain.usecase.GetFavoriteRevistasUseCase
import sergirex.portadasrevistas.domain.usecase.GetRevistaCoversUseCase
import sergirex.portadasrevistas.domain.usecase.GetRevistasByCategoryUseCase
import sergirex.portadasrevistas.domain.usecase.ObserveFavoritesUseCase
import sergirex.portadasrevistas.presentation.portadas.PortadasViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class PortadasViewModelTest {

    private val corazon = HomeTab.Category(RevistaCategory.CORAZON)
    private val semana = RevistaRef("3845", "Semana", "semana.es", 108, "76")
    private val lecturas = RevistaRef("3741", "Lecturas", "lecturas.com", 100, "74")

    private val favoritesFlow = MutableStateFlow<List<RevistaRef>>(emptyList())
    private var coverRequests = 0

    private val catalog = object : RevistasRepository {
        override suspend fun getById(id: String): Revista? = null
        override suspend fun getByCategory(category: RevistaCategory) = Result.success(
            listOf(
                Revista(semana.id, semana.name, semana.web, semana.pages, semana.scale, category),
                Revista(lecturas.id, lecturas.name, lecturas.web, lecturas.pages, lecturas.scale, category)
            )
        )
    }
    private val favorites = object : FavoriteRevistasRepository {
        override val favorites: Flow<List<RevistaRef>> = favoritesFlow
        override suspend fun toggle(revista: RevistaRef) = Unit
    }
    private val covers = object : RevistaCoverRepository {
        override fun getCovers(revistas: List<RevistaRef>, forceRefresh: Boolean): Flow<RevistaCover> {
            coverRequests++
            return flow { revistas.forEach { emit(RevistaCover(it, "url/${it.id}")) } }
        }
    }
    private val settings = object : SettingsRepository {
        override val initialTab = flowOf(HomeTab.default)
        override val dailyNotificationEnabled = flowOf(true)
        override val notificationPermissionRequested = flowOf(false)
        override suspend fun setInitialTab(tab: HomeTab) = Unit
        override suspend fun setDailyNotificationEnabled(enabled: Boolean) = Unit
        override suspend fun setNotificationPermissionRequested() = Unit
    }

    @Before
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(coverRepository: RevistaCoverRepository = covers) = PortadasViewModel(
        GetRevistasByCategoryUseCase(catalog),
        GetFavoriteRevistasUseCase(favorites),
        GetRevistaCoversUseCase(coverRepository),
        settings,
        ObserveFavoritesUseCase(favorites)
    )

    @Test
    fun `a tab loads once and stays loaded`() = runTest {
        val vm = viewModel()
        vm.loadIfNeeded(corazon)
        advanceUntilIdle()
        vm.loadIfNeeded(corazon)
        advanceUntilIdle()

        assertEquals(listOf("3845", "3741"), vm.uiState(corazon).value.covers.map { it.revista.id })
        assertEquals(1, coverRequests)
    }

    @Test
    fun `refresh asks again`() = runTest {
        val vm = viewModel()
        vm.loadIfNeeded(corazon)
        advanceUntilIdle()
        vm.refresh(corazon)
        advanceUntilIdle()

        assertEquals(2, coverRequests)
    }

    @Test
    fun `favorites tab appears only while there is a favorite, and its content follows the list`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(HomeTab.categories, vm.tabs.value)

        favoritesFlow.value = listOf(semana)
        advanceUntilIdle()
        assertEquals(listOf(HomeTab.Favorites) + HomeTab.categories, vm.tabs.value)

        vm.loadIfNeeded(HomeTab.Favorites)
        advanceUntilIdle()
        assertEquals(listOf("3845"), vm.uiState(HomeTab.Favorites).value.covers.map { it.revista.id })

        favoritesFlow.value = listOf(semana, lecturas)
        vm.loadIfNeeded(HomeTab.Favorites)
        advanceUntilIdle()
        assertEquals(listOf("3845", "3741"), vm.uiState(HomeTab.Favorites).value.covers.map { it.revista.id })

        favoritesFlow.value = emptyList()
        advanceUntilIdle()
        assertEquals(HomeTab.categories, vm.tabs.value)
    }

    @Test
    fun `a load that finds nothing is retried next time and flags the failure`() = runTest {
        val failing = object : RevistaCoverRepository {
            override fun getCovers(revistas: List<RevistaRef>, forceRefresh: Boolean): Flow<RevistaCover> {
                coverRequests++
                return emptyFlow()
            }
        }
        val vm = viewModel(failing)
        vm.loadIfNeeded(corazon)
        advanceUntilIdle()
        assertTrue(vm.uiState(corazon).value.showLoadFailed)

        vm.loadIfNeeded(corazon)
        advanceUntilIdle()
        assertEquals(2, coverRequests)
    }
}
