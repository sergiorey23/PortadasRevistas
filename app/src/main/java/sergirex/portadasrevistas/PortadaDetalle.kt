package sergirex.portadasrevistas

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.RotateDrawable
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.color.MaterialColors
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import sergirex.portadasrevistas.databinding.ActivityPortadaDetalleBinding
import sergirex.portadasrevistas.domain.model.CoverUrls
import sergirex.portadasrevistas.domain.model.InterstitialPolicy
import sergirex.portadasrevistas.domain.model.IssueDate
import sergirex.portadasrevistas.presentation.ads.AdsViewModel
import sergirex.portadasrevistas.presentation.detail.PortadaDetalleViewModel
import sergirex.portadasrevistas.ui.ActivityTransitions
import sergirex.portadasrevistas.ui.PureBlackTheme
import sergirex.portadasrevistas.ui.SpringMotion
import java.io.File
import javax.inject.Inject

/**
 * The magazine detail screen: a pager of the covers of the tab the user came from, with the
 * actions of the current one (favorite, website, share, save, pick an issue) and the entry to the
 * reader of the issue on screen. The cover grows into it from its grid cell and shrinks back on return.
 */
@AndroidEntryPoint
class PortadaDetalle : AppCompatActivity(), PortadaDetalleHost {

    private lateinit var binding: ActivityPortadaDetalleBinding
    private val viewModel: PortadaDetalleViewModel by viewModels()
    private val adsViewModel: AdsViewModel by viewModels()

    private var pagerAdapter: ViewPagerAdapter? = null
    private var bannerRequested = false
    private val adManagerDelegate = lazy { AdManager(this) }
    private val adManager by adManagerDelegate

    @Inject
    lateinit var interstitialPolicy: InterstitialPolicy

    // Whether this open asked for a Play review: that is enough of an interruption, so no interstitial on the way out.
    private var reviewPromptShown = false

    /**
     * Shows the interstitial when the user leaves, which is a natural pause (unlike on open, which would delay what they
     * came to see). Enabled only while one is loaded and the policy allows it.
     */
    private val interstitialBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            isEnabled = false
            interstitialPolicy.recordShown()
            if (!adManager.showInterstitial { finishAfterTransition() }) finishAfterTransition()
        }
    }

    // Must be registered before STARTED (as a field, not inside a click handler) per the Activity
    // Result API contract; SavePortada can't register its own since only an Activity/Fragment can.
    private val requestStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val message = if (granted) R.string.storage_permission_granted else R.string.storage_permission_denied
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        PureBlackTheme.apply(this)
        ActivityTransitions.prepareExpandingChild(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPortadaDetalleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ActivityTransitions.expandFrom(this, binding.detailContent, R.id.detailAppBar)

        val portadas = IntentCompat.getParcelableArrayListExtra(intent, EXTRA_PORTADAS, Portada::class.java).orEmpty()
        if (portadas.isEmpty()) {
            // Launched without the covers it should show (not possible from the app itself): nothing to display.
            finish()
            return
        }

        onBackPressedDispatcher.addCallback(this, interstitialBackCallback)
        keepContentAboveNavigationBar()
        setupToolbar()
        setupFabs()
        setupPager(portadas, intent.getStringExtra(EXTRA_SELECTED_ID))

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.favoriteIds.collect { renderFavorite() } }
                launch { viewModel.fabsExpanded.collect { toggleFabs(it) } }
            }
        }
        showBannerAd()

        // Count the visit once per real open (not when the system recreates the Activity).
        if (savedInstanceState == null) {
            interstitialPolicy.recordOpen()
            lifecycleScope.launch {
                if (viewModel.onCoverOpened()) {
                    reviewPromptShown = true
                    interstitialBackCallback.isEnabled = false
                    showInAppReviewPrompt()
                }
            }
        }
    }

    private var navigationBarInset = 0

    /**
     * Keeps the bottom-most content off the navigation/gesture bar: the banner is padded by it, and without a banner the
     * content area is, plus a little extra so the image and buttons aren't glued to the bottom of the screen.
     */
    private fun keepContentAboveNavigationBar() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            navigationBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            updateBottomSpace()
            insets
        }
    }

    private fun updateBottomSpace() {
        val bannerShown = binding.bannerContainerDetail.visibility == View.VISIBLE
        binding.bannerContainerDetail.updatePadding(bottom = if (bannerShown) navigationBarInset else 0)
        binding.detailContent.updatePadding(
            bottom = if (bannerShown) 0 else navigationBarInset + resources.getDimensionPixelSize(R.dimen.detail_no_banner_bottom_space)
        )
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp)
        binding.toolbar.setNavigationContentDescription(R.string.back)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    // The "+" turns into an "x" by rotating only the icon (not the whole button, whose shape would turn into a diamond).
    // The drawable's level 0..10000 maps to 0..90 degrees, so a bouncy spring can overshoot the resting 45.
    private val addIcon: RotateDrawable by lazy {
        RotateDrawable().apply {
            drawable = AppCompatResources.getDrawable(this@PortadaDetalle, R.drawable.baseline_add_24)
            fromDegrees = 0f
            toDegrees = 90f
            isPivotXRelative = true
            pivotX = 0.5f
            isPivotYRelative = true
            pivotY = 0.5f
        }
    }

    // What the button menu shows right now, so a repeated state from the ViewModel (e.g. on restart) doesn't animate.
    private var fabsShownExpanded: Boolean? = null

    private fun setupFabs() {
        binding.addFab.setImageDrawable(addIcon)
        binding.addFab.setOnClickListener { viewModel.toggleFabs() }
        binding.httpButton.setOnClickListener { currentPortada()?.let { openWebsite(it.web) } }
        binding.favButton.setOnClickListener { toggleFavorite() }
        // Initial state (also after a rotation) without animating.
        toggleFabs(viewModel.fabsExpanded.value, animate = false)
    }

    private fun toggleFabs(isExpanded: Boolean, animate: Boolean = true) {
        if (fabsShownExpanded == isExpanded) return
        fabsShownExpanded = isExpanded
        val minis = listOf(binding.httpButton, binding.favButton)
        minis.forEach { it.isClickable = isExpanded }

        if (!animate) {
            addIcon.level = if (isExpanded) ADD_ICON_OPEN_LEVEL.toInt() else 0
            minis.forEach { fab ->
                fab.visibility = if (isExpanded) View.VISIBLE else View.INVISIBLE
                fab.translationY = if (isExpanded) 0f else fabTravel()
                fab.alpha = if (isExpanded) 1f else 0f
                fab.scaleX = if (isExpanded) 1f else FAB_HIDDEN_SCALE
                fab.scaleY = fab.scaleX
            }
            return
        }

        SpringMotion.animateProperty(this, addIcon, ICON_LEVEL, if (isExpanded) ADD_ICON_OPEN_LEVEL else 0f, SpringMotion.Spring.FAST_SPATIAL)
        minis.forEachIndexed { index, fab ->
            if (isExpanded && fab.visibility != View.VISIBLE) {
                // Start from just below and small, so the springs have somewhere to travel from.
                fab.translationY = fabTravel()
                fab.alpha = 0f
                fab.scaleX = FAB_HIDDEN_SCALE
                fab.scaleY = FAB_HIDDEN_SCALE
                fab.visibility = View.VISIBLE
            }
            // Each button starts a moment after the previous one; they leave in the opposite order.
            val delay = (if (isExpanded) index else minis.lastIndex - index) * FAB_STAGGER_MS
            fab.postDelayed({
                if (viewModel.fabsExpanded.value != isExpanded) return@postDelayed // toggled again meanwhile
                val scale = if (isExpanded) 1f else FAB_HIDDEN_SCALE
                SpringMotion.animate(fab, DynamicAnimation.TRANSLATION_Y, if (isExpanded) 0f else fabTravel(), SpringMotion.Spring.DEFAULT_SPATIAL)
                SpringMotion.animate(fab, DynamicAnimation.SCALE_X, scale, SpringMotion.Spring.DEFAULT_SPATIAL)
                SpringMotion.animate(fab, DynamicAnimation.SCALE_Y, scale, SpringMotion.Spring.DEFAULT_SPATIAL)
                SpringMotion.animate(fab, DynamicAnimation.ALPHA, if (isExpanded) 1f else 0f, SpringMotion.Spring.DEFAULT_EFFECTS) {
                    if (!isExpanded) fab.visibility = View.INVISIBLE
                }
            }, delay)
        }
    }

    /** How far below its place a hidden menu button waits. */
    private fun fabTravel() = FAB_TRAVEL_DP * resources.displayMetrics.density

    private fun setupPager(portadas: List<Portada>, selectedId: String?) {
        val adapter = ViewPagerAdapter(this, portadas)
        pagerAdapter = adapter
        binding.viewpager2.adapter = adapter
        // Listen before choosing the first page: ViewPager2 reports a page change synchronously inside
        // setCurrentItem(), and doesn't report the initial page at all when it is the first one.
        binding.viewpager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = updateUiForPage()
        })
        portadas.indexOfFirst { it.id == selectedId }.takeIf { it >= 0 }?.let { binding.viewpager2.setCurrentItem(it, false) }
        // Don't depend on any callback for the page that is showing from the start (also after a rotation).
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
        // Same heart in both states: the color tells them apart (a favorite is colored, otherwise it's muted).
        val tint = if (isFavorite) {
            ContextCompat.getColor(this, R.color.favorite_active)
        } else {
            MaterialColors.getColor(binding.favButton, com.google.android.material.R.attr.colorOnSurfaceVariant)
        }
        binding.favButton.imageTintList = ColorStateList.valueOf(tint)
        val label = getString(if (isFavorite) R.string.fav_remove else R.string.fav_add)
        binding.favButton.contentDescription = label
        TooltipCompat.setTooltipText(binding.favButton, label)
    }

    private fun toggleFavorite() {
        val portada = currentPortada() ?: return
        val wasFavorite = portada.id in viewModel.favoriteIds.value
        viewModel.toggleFavorite(portada.toRef())
        Snackbar.make(binding.root, if (wasFavorite) R.string.fav_removed else R.string.fav_added, Snackbar.LENGTH_SHORT)
            .setAnchorView(binding.addFab)
            .apply { animationMode = Snackbar.ANIMATION_MODE_FADE }
            .show()
    }

    /** Shows the banner unless the user owns "remove ads"; reacts if the purchase happens while this screen is open. */
    private fun showBannerAd() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                adsViewModel.adsRemoved.collect { removed ->
                    when (removed) {
                        null -> Unit
                        true -> {
                            if (adManagerDelegate.isInitialized()) adManager.destroy()
                            bannerRequested = false
                            interstitialBackCallback.isEnabled = false
                            binding.bannerContainerDetail.visibility = View.GONE
                            updateBottomSpace()
                        }
                        false -> {
                            binding.bannerContainerDetail.visibility = View.VISIBLE
                            updateBottomSpace()
                            if (!bannerRequested) {
                                bannerRequested = true
                                adManager.loadBanner(binding.bannerContainerDetail, DETAIL_BANNER_PLACEMENT)
                                // Only ask for one when this exit is due: a request nobody will see just wastes fill.
                                if (interstitialPolicy.shouldShow()) {
                                    adManager.loadInterstitial(DETAIL_INTERSTITIAL_PLACEMENT) {
                                        interstitialBackCallback.isEnabled = !reviewPromptShown && interstitialPolicy.shouldShow()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun finishAfterTransition() {
        // The cover grew out of one grid cell; if the user swiped to another cover, don't shrink into the wrong cell.
        val showing = currentPortada()?.id
        if (showing != null && showing != intent.getStringExtra(EXTRA_SELECTED_ID)) {
            ActivityTransitions.stopSharing(this, binding.detailContent, R.id.detailRoot)
        }
        // The detail window's opaque background normally fades out over the whole return, which keeps the home screen (and
        // its toolbar) hidden until the end; drop it right away so the toolbars swap as the return starts.
        window.setTransitionBackgroundFadeDuration(0)
        // Don't leave the app bar to the window transition, which keeps it on screen until the return is over: fade it
        // out ourselves right now, so the home toolbar underneath shows from the start.
        binding.detailAppBar.animate().alpha(0f).setDuration(APP_BAR_FADE_MS).start()
        super.finishAfterTransition()
    }

    override fun onCoverClicked(magazineId: String) {
        val portada = pagerAdapter?.portadas?.firstOrNull { it.id == magazineId } ?: return
        val date = viewModel.currentIssue(magazineId).selected ?: return
        ActivityTransitions.launch(this, RevistaPagerActivity.createIntent(this, portada, date))
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
        // Don't build (and initialize the SDK for) an AdManager just to destroy it.
        if (adManagerDelegate.isInitialized()) adManager.destroy()
        super.onDestroy()
    }

    /** One page per magazine; each page is a [PortadaDetalleFragment]. */
    private class ViewPagerAdapter(activity: AppCompatActivity, val portadas: List<Portada>) : FragmentStateAdapter(activity) {
        fun portadaAt(position: Int): Portada? = portadas.getOrNull(position)

        override fun getItemCount(): Int = portadas.size
        override fun createFragment(position: Int): Fragment =
            PortadaDetalleFragment.newInstance(portadas[position].id, portadas[position].category, portadas[position].previewUrl)
    }

    companion object {
        private const val TAG = "PortadaDetalle"
        private const val EXTRA_PORTADAS = "portadas"
        private const val EXTRA_SELECTED_ID = "selected_id"
        private const val APP_BAR_FADE_MS = 150L
        private const val DETAIL_BANNER_PLACEMENT = "483584450995789_487598390594395"
        private const val DETAIL_INTERSTITIAL_PLACEMENT = "483584450995789_487598570594377"

        // Button menu motion
        private const val ADD_ICON_OPEN_LEVEL = 5000f // 45 degrees of the 0..90 range
        private const val FAB_HIDDEN_SCALE = 0.5f
        private const val FAB_TRAVEL_DP = 56f
        private const val FAB_STAGGER_MS = 40L

        private val ICON_LEVEL = object : FloatPropertyCompat<RotateDrawable>("level") {
            override fun getValue(drawable: RotateDrawable) = drawable.level.toFloat()
            override fun setValue(drawable: RotateDrawable, value: Float) {
                drawable.level = value.toInt().coerceIn(0, 10_000)
            }
        }

        fun createIntent(context: Context, portadas: List<Portada>, selectedId: String): Intent =
            Intent(context, PortadaDetalle::class.java)
                .putParcelableArrayListExtra(EXTRA_PORTADAS, ArrayList(portadas))
                .putExtra(EXTRA_SELECTED_ID, selectedId)
    }
}
