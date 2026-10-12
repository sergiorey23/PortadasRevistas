package sergirex.portadasrevistas

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import sergirex.portadasrevistas.databinding.AboutBinding
import sergirex.portadasrevistas.databinding.ActivityPortadasBinding
import sergirex.portadasrevistas.domain.model.Entitlements
import sergirex.portadasrevistas.domain.model.HomeTab
import sergirex.portadasrevistas.domain.repository.EntitlementsRepository
import sergirex.portadasrevistas.presentation.EntitlementsViewModel
import sergirex.portadasrevistas.presentation.portadas.PortadasViewModel
import sergirex.portadasrevistas.presentation.portadas.titleRes
import sergirex.portadasrevistas.presentation.settings.SettingsViewModel
import javax.inject.Inject

/** The home screen: a pager of magazine-cover tabs, the navigation drawer and the toolbar actions. */
@AndroidEntryPoint
class Portadas : AppCompatActivity() {

    @Inject
    lateinit var entitlementsRepository: EntitlementsRepository

    private lateinit var binding: ActivityPortadasBinding
    private var billingManager: BillingManager? = null
    private var adManager: AdManager? = null
    private var alertDialog: AlertDialog? = null

    private val viewModel: PortadasViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val entitlementsViewModel: EntitlementsViewModel by viewModels()
    private lateinit var tabsAdapter: TabsAdapter

    // Must be registered before STARTED per the Activity Result API contract.
    private val requestPostNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            settingsViewModel.onNotificationPermissionResult()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Shows the launch splash and switches to Theme.Portadas (postSplashScreenTheme) once drawn.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPortadasBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        billingManager = BillingManager(this, lifecycleScope, entitlementsRepository) { binding.drawerLayout }
        adManager = AdManager(this)

        setUpTabs()
        setUpDrawer()
        askNotificationPermissionOnce()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                entitlementsViewModel.entitlements.collect(::onEntitlementsChanged)
            }
        }
    }

    private fun setUpDrawer() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    moveTaskToBack(true)
                }
            }
        })

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.closeDrawers()
            when (menuItem.itemId) {
                R.id.nav_help -> showDialog().show()
                R.id.nav_subscribe -> billingManager?.showPurchaseDialog(BillingManager.Product.PREMIUM)
                R.id.nav_remove_ads -> billingManager?.showPurchaseDialog(BillingManager.Product.REMOVE_ADS)
                R.id.nav_rate -> launchMarket()
                R.id.nav_share -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_full_name))
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Descarga la app de Portadas gratis!\n\nhttps://play.google.com/store/apps/details?id=$packageName"
                        )
                    }
                    startActivityOrToast(Intent.createChooser(intent, null))
                }
                R.id.nav_about -> showAboutInfo()
                R.id.nav_settings -> startActivity(Intent(applicationContext, SettingsActivity::class.java))
            }
            true
        }

        val drawerToggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar, R.string.open_menu, R.string.close_menu
        )
        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
    }

    private fun setUpTabs() {
        tabsAdapter = TabsAdapter(this)
        binding.viewpager.adapter = tabsAdapter
        // Lets a tab two swipes away be ready by the time it is reached, like the old pager did.
        binding.viewpager.offscreenPageLimit = 2
        // The mediator observes the adapter, so tabs added/removed by the adapter update the TabLayout by themselves.
        TabLayoutMediator(binding.tabs, binding.viewpager) { tab, position ->
            tab.setText(tabsAdapter.tabs[position].titleRes)
        }.attach()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.tabs.collect(::onTabsChanged)
            }
        }
    }

    private suspend fun onTabsChanged(tabs: List<HomeTab>) {
        if (tabs.isEmpty()) return
        val current = tabsAdapter.tabs.getOrNull(binding.viewpager.currentItem)
        tabsAdapter.submit(tabs)

        val selected = when {
            !viewModel.initialTabApplied -> viewModel.initialTab().also { viewModel.initialTabApplied = true }
            else -> current // keep the user on the tab they were on when tabs are added/removed
        }
        binding.viewpager.setCurrentItem(tabs.indexOf(selected).coerceAtLeast(0), false)
    }

    /** Ads are shown only to people who haven't bought anything; a purchase takes the banner away at once. */
    private fun onEntitlementsChanged(entitlements: Entitlements) {
        val menu = binding.navView.menu
        menu.findItem(R.id.nav_remove_ads).isVisible = !entitlements.adsRemoved && !entitlements.premium
        menu.findItem(R.id.nav_subscribe).isVisible = !entitlements.premium

        if (entitlements.showAds) {
            binding.bannerContainer.visibility = View.VISIBLE
            if (binding.bannerContainer.childCount == 0) {
                adManager?.loadBanner(binding.bannerContainer, MAIN_BANNER_PLACEMENT)
            }
        } else {
            adManager?.destroy()
            binding.bannerContainer.removeAllViews()
            binding.bannerContainer.visibility = View.GONE
        }
    }

    private fun askNotificationPermissionOnce() {
        lifecycleScope.launch {
            val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this@Portadas, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            if (needsPermission && settingsViewModel.shouldAskNotificationPermission()) {
                settingsViewModel.onNotificationPermissionRequested()
                requestPostNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun showAboutInfo() {
        val aboutBinding = AboutBinding.inflate(layoutInflater, binding.drawerLayout, false)

        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        aboutBinding.appVersion.text = "v${versionName ?: "N/A"}"

        aboutBinding.appSource.setOnClickListener {
            startActivityOrToast(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.lasportadas.es/")))
        }

        aboutBinding.privacyPolicy.setOnClickListener {
            startActivityOrToast(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.lasportadas.es/privacy.php")))
        }

        aboutBinding.contact.setOnClickListener {
            startActivityOrToast(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:sssergiooo23@gmail.com")))
        }

        aboutBinding.portadasPeriodicos.setOnClickListener {
            startActivityOrToast(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=sergirex.portadasperiodicos")))
        }

        MaterialAlertDialogBuilder(this)
            .setPositiveButton(R.string.close, null)
            .setView(aboutBinding.root)
            .show()
    }

    private fun showDialog(): AlertDialog {
        alertDialog?.let { return it }

        val message = "<Big>${getString(R.string.help_description)}<br/><br/>" +
            "${getString(R.string.help_description2)}</Big><br/><br/>${getString(R.string.rate_app)}"
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.help)
            .setIcon(R.mipmap.magazines_icon)
            .setPositiveButton("OK", null)
            .create()
        dialog.setMessage(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY)
            else
                @Suppress("DEPRECATION") Html.fromHtml(message)
        )
        alertDialog = dialog
        return dialog
    }

    private fun launchMarket() {
        startActivityOrToast(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
    }

    /** Pager adapter over [HomeTab]s; changes are diffed so only the affected pages are added or removed. */
    private class TabsAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        var tabs: List<HomeTab> = emptyList()
            private set

        fun submit(newTabs: List<HomeTab>) {
            val old = tabs
            val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize() = old.size
                override fun getNewListSize() = newTabs.size
                override fun areItemsTheSame(oldPosition: Int, newPosition: Int) = old[oldPosition] == newTabs[newPosition]
                override fun areContentsTheSame(oldPosition: Int, newPosition: Int) = true
            })
            tabs = newTabs
            diff.dispatchUpdatesTo(this)
        }

        override fun getItemCount() = tabs.size
        override fun createFragment(position: Int): Fragment = PortadasFragment.newInstance(tabs[position])
        override fun getItemId(position: Int) = tabs[position].key.hashCode().toLong()
        override fun containsItem(itemId: Long) = tabs.any { it.key.hashCode().toLong() == itemId }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.fav) {
            val favoritesIndex = tabsAdapter.tabs.indexOf(HomeTab.Favorites)
            if (favoritesIndex >= 0) {
                binding.viewpager.currentItem = favoritesIndex
            } else {
                Snackbar.make(binding.drawerLayout, R.string.no_favorites, Snackbar.LENGTH_SHORT)
                    .apply { animationMode = Snackbar.ANIMATION_MODE_FADE }
                    .show()
            }
        }
        return true
    }

    override fun onDestroy() {
        // A dialog still showing when the Activity goes away would leak its window.
        alertDialog?.dismiss()
        billingManager?.destroy()
        adManager?.destroy()
        super.onDestroy()
    }

    private companion object {
        const val MAIN_BANNER_PLACEMENT = "483584450995789_487598283927739"
    }
}
