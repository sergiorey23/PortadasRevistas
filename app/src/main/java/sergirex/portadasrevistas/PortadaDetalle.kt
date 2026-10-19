package sergirex.portadasrevistas

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import sergirex.portadasrevistas.databinding.ActivityPortadaDetalleBinding
import sergirex.portadasrevistas.domain.model.CoverUrls
import sergirex.portadasrevistas.domain.model.Entitlements
import sergirex.portadasrevistas.domain.model.IssueDate
import sergirex.portadasrevistas.presentation.EntitlementsViewModel
import sergirex.portadasrevistas.presentation.detail.PortadaDetalleViewModel
import java.io.File

/**
 * The magazine detail screen: a pager of the covers of the tab the user came from, with the
 * actions of the current one (favorite, website, share, save, pick an issue) and the entry to the
 * reader of the issue on screen.
 */
@AndroidEntryPoint
class PortadaDetalle : AppCompatActivity(), PortadaDetalleHost {

    private lateinit var binding: ActivityPortadaDetalleBinding
    private val viewModel: PortadaDetalleViewModel by viewModels()
    private val entitlementsViewModel: EntitlementsViewModel by viewModels()

    private var pagerAdapter: ViewPagerAdapter? = null
    private var adManager: AdManager? = null

    // Must be registered before STARTED (as a field, not inside a click handler) per the Activity
    // Result API contract; SavePortada can't register its own since only an Activity/Fragment can.
    private val requestStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val message = if (granted) R.string.storage_permission_granted else R.string.storage_permission_denied
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }

    // Animations are loaded once.
    private val rotateOpen: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_open_anim) }
    private val rotateClose: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_close_anim) }
    private val fromBottom: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.from_bottom_anim) }
    private val toBottom: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.to_bottom_anim) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPortadaDetalleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val portadas = IntentCompat.getParcelableArrayListExtra(intent, EXTRA_PORTADAS, Portada::class.java).orEmpty()
        if (portadas.isEmpty()) {
            // Launched without the covers it should show (not possible from the app itself): nothing to display.
            finish()
            return
        }

        adManager = AdManager(this)

        keepBannerAboveNavigationBar()
        setupToolbar()
        setupFabs()
        setupPager(portadas, intent.getStringExtra(EXTRA_SELECTED_ID))

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.favoriteIds.collect { renderFavorite() } }
                launch { viewModel.fabsExpanded.collect(::renderFabs) }
                launch { entitlementsViewModel.entitlements.collect(::onEntitlementsChanged) }
            }
        }

        // Count the visit once per real open (not when the system recreates the Activity).
        if (savedInstanceState == null) {
            lifecycleScope.launch {
                val outcome = viewModel.onCoverOpened()
                when {
                    outcome.promptReview -> showInAppReviewPrompt()
                    outcome.showInterstitial && entitlementsViewModel.entitlements.value.showAds ->
                        adManager?.showInterstitial(INTERSTITIAL_PLACEMENT)
                }
            }
        }
    }

    /** The banner is the bottom-most view: pad it by the navigation/gesture bar so it isn't drawn under it. */
    private fun keepBannerAboveNavigationBar() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.bannerContainerDetail) { view, insets ->
            view.updatePadding(bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom)
            insets
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp)
        binding.toolbar.setNavigationContentDescription(R.string.back)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupFabs() {
        binding.addFab.setOnClickListener { viewModel.toggleFabs() }
        binding.httpButton.setOnClickListener { currentPortada()?.let { openWebsite(it.web) } }
        binding.favButton.setOnClickListener { currentPortada()?.let { viewModel.toggleFavorite(it.toRef()) } }
        // Initial state (also after a rotation) without animating.
        renderFabs(viewModel.fabsExpanded.value, animate = false)
    }

    private fun renderFabs(expanded: Boolean, animate: Boolean = true) {
        if (animate) {
            binding.httpButton.startAnimation(if (expanded) fromBottom else toBottom)
            binding.favButton.startAnimation(if (expanded) fromBottom else toBottom)
            binding.addFab.startAnimation(if (expanded) rotateOpen else rotateClose)
        }
        val visibility = if (expanded) View.VISIBLE else View.INVISIBLE
        binding.httpButton.visibility = visibility
        binding.favButton.visibility = visibility
        binding.httpButton.isClickable = expanded
        binding.favButton.isClickable = expanded
    }

    private fun setupPager(portadas: List<Portada>, selectedId: String?) {
        val adapter = ViewPagerAdapter(this, portadas)
        pagerAdapter = adapter
        binding.viewpager2.adapter = adapter
        portadas.indexOfFirst { it.id == selectedId }.takeIf { it >= 0 }?.let { binding.viewpager2.setCurrentItem(it, false) }
        binding.viewpager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = updateUiForPage()
        })
        updateUiForPage()
    }

    private fun currentPortada(): Portada? = pagerAdapter?.portadaAt(binding.viewpager2.currentItem)

    private fun updateUiForPage() {
        supportActionBar?.title = currentPortada()?.name
        renderFavorite()
    }

    private fun renderFavorite() {
        val portada = currentPortada() ?: return
        val isFavorite = portada.id in viewModel.favoriteIds.value
        binding.favButton.setImageResource(if (isFavorite) R.drawable.ic_favorite_black_24dp else R.drawable.ic_favorite_border_black_24dp)
    }

    /** Ads are shown only to people who haven't bought anything; a purchase takes the banner away at once. */
    private fun onEntitlementsChanged(entitlements: Entitlements) {
        if (entitlements.showAds) {
            binding.bannerContainerDetail.visibility = View.VISIBLE
            if (binding.bannerContainerDetail.childCount == 0) {
                adManager?.loadBanner(binding.bannerContainerDetail, DETAIL_BANNER_PLACEMENT)
            }
        } else {
            binding.bannerContainerDetail.removeAllViews()
            binding.bannerContainerDetail.visibility = View.GONE
        }
    }

    override fun onCoverClicked(magazineId: String) {
        val portada = pagerAdapter?.portadas?.firstOrNull { it.id == magazineId } ?: return
        val date = viewModel.currentIssue(magazineId).selected ?: return
        startActivity(RevistaPagerActivity.createIntent(this, portada, date))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_portada, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val portada = currentPortada() ?: return false
        val date = viewModel.currentIssue(portada.id).selected
        // The issue on screen, or the latest cover while the calendar hasn't arrived yet.
        val imageUrl = date?.let { CoverUrls.issueCover(portada.id, it) } ?: CoverUrls.latest(portada.id)
        val savePortada = SavePortada(this, requestStoragePermission)

        when (item.itemId) {
            R.id.share -> lifecycleScope.launch { savePortada.share(imageUrl, portada.toRef().fileName(date)) }
            R.id.save -> {
                if (savePortada.isExternalStorageWritable && savePortada.checkPermissions()) {
                    val file = File(savePortada.albumStorageDir, "${portada.toRef().fileName(date ?: IssueDate.today())}.jpg")
                    if (file.exists()) {
                        Toast.makeText(this, R.string.cover_already_saved, Toast.LENGTH_LONG).show()
                    } else {
                        lifecycleScope.launch { savePortada.saveToGallery(binding.root, imageUrl, file) }
                    }
                } else if (!savePortada.isExternalStorageWritable) {
                    Toast.makeText(this, R.string.storage_unavailable, Toast.LENGTH_LONG).show()
                }
            }
            R.id.date -> showDatePicker(portada.id)
            else -> return false
        }
        return true
    }

    /** Lets the user pick one of the issues the magazine has published (and only those). */
    private fun showDatePicker(magazineId: String) {
        val issue = viewModel.currentIssue(magazineId)
        val days = issue.dates.mapNotNull(IssueDate::toUtcMillis).sorted()
        val selected = issue.selected?.let(IssueDate::toUtcMillis)
        if (days.isEmpty() || selected == null) {
            Toast.makeText(this, R.string.issues_not_loaded, Toast.LENGTH_SHORT).show()
            return
        }
        val constraints = CalendarConstraints.Builder()
            .setStart(days.first())
            .setEnd(days.last())
            .setOpenAt(selected)
            .setValidator(IssueDateValidator(days.toLongArray()))
            .build()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.select_date)
            .setSelection(selected)
            .setCalendarConstraints(constraints)
            .build()
        picker.addOnPositiveButtonClickListener { viewModel.selectIssue(magazineId, IssueDate.fromUtcMillis(it)) }
        picker.show(supportFragmentManager, "MATERIAL_DATE_PICKER")
    }

    private fun showInAppReviewPrompt() {
        val reviewManager = ReviewManagerFactory.create(this)
        reviewManager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val flow = reviewManager.launchReviewFlow(this, task.result)
                // The API doesn't say whether the dialog was shown or the user reviewed, so any
                // completion counts as a prompt, to avoid asking again too soon.
                flow.addOnCompleteListener { viewModel.onReviewPromptShown() }
            } else {
                Log.e(TAG, "Review flow request failed.", task.exception)
            }
        }
    }

    override fun onDestroy() {
        adManager?.destroy()
        super.onDestroy()
    }

    /** One page per magazine; each page is a [PortadaDetalleFragment]. */
    private class ViewPagerAdapter(activity: AppCompatActivity, val portadas: List<Portada>) : FragmentStateAdapter(activity) {
        fun portadaAt(position: Int): Portada? = portadas.getOrNull(position)

        override fun getItemCount(): Int = portadas.size
        override fun createFragment(position: Int): Fragment = PortadaDetalleFragment.newInstance(portadas[position].id, portadas[position].category)
    }

    companion object {
        private const val TAG = "PortadaDetalle"
        private const val EXTRA_PORTADAS = "portadas"
        private const val EXTRA_SELECTED_ID = "selected_id"
        private const val DETAIL_BANNER_PLACEMENT = "483584450995789_487598390594395"
        private const val INTERSTITIAL_PLACEMENT = "483584450995789_487598570594377"

        fun createIntent(context: Context, portadas: List<Portada>, selectedId: String): Intent =
            Intent(context, PortadaDetalle::class.java)
                .putParcelableArrayListExtra(EXTRA_PORTADAS, ArrayList(portadas))
                .putExtra(EXTRA_SELECTED_ID, selectedId)
    }
}
