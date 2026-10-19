package sergirex.portadasrevistas

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import sergirex.portadasrevistas.databinding.RevistaPagerActivityBinding
import sergirex.portadasrevistas.presentation.reader.ReaderViewModel
import kotlin.math.abs

/** The magazine reader: swipe through the pages of one issue, or jump with the slider. */
@AndroidEntryPoint
class RevistaPagerActivity : AppCompatActivity() {

    private lateinit var binding: RevistaPagerActivityBinding
    private val viewModel: ReaderViewModel by viewModels()

    private lateinit var magazineId: String
    private lateinit var issueDate: String
    private var pages = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = RevistaPagerActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val id = intent.getStringExtra(EXTRA_ID)
        val date = intent.getStringExtra(EXTRA_DATE)
        val scale = intent.getStringExtra(EXTRA_SCALE)
        pages = intent.getIntExtra(EXTRA_PAGES, 0)
        if (id == null || date == null || scale == null || pages <= 0) {
            // Launched without what it needs (not possible from the app itself): nothing to read.
            finish()
            return
        }
        magazineId = id
        issueDate = date

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = intent.getStringExtra(EXTRA_TITLE)
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp)
        binding.toolbar.setNavigationContentDescription(R.string.back)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.viewPagerRevista.setPageTransformer(DepthPageTransformer())
        binding.seekBar.max = pages - 1

        // The slider and counter follow the pager (which is the source of truth for the current page).
        binding.viewPagerRevista.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = showPage(position)
        })
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) binding.viewPagerRevista.setCurrentItem(progress, false)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
        })

        // The pages are requested at the sharpest scale the service accepts for this magazine, which
        // has to be looked up first (see PageScaleRepository); the pager starts once it is known.
        binding.loadingProgressBar.visibility = View.VISIBLE
        viewModel.resolveScale(id, date, scale)
        lifecycleScope.launch {
            val resolvedScale = viewModel.scale.filterNotNull().first()
            binding.viewPagerRevista.adapter = PagesAdapter(this@RevistaPagerActivity, id, date, pages, resolvedScale)
            binding.loadingProgressBar.visibility = View.GONE
            // Continue where the reader left off.
            val page = viewModel.savedPage(id, date).coerceIn(0, pages - 1)
            binding.viewPagerRevista.setCurrentItem(page, false)
            showPage(page)
        }
    }

    private fun showPage(position: Int) {
        binding.pageCounter.text = getString(R.string.page_counter, position + 1, pages)
        binding.seekBar.progress = position
    }

    // Saved whenever the reader leaves (back, home, another app), not only on the back button.
    override fun onPause() {
        super.onPause()
        // Before the pager exists the current item is a meaningless 0 that would erase the saved page.
        if (::magazineId.isInitialized && binding.viewPagerRevista.adapter != null) viewModel.savePage(magazineId, issueDate, binding.viewPagerRevista.currentItem)
    }

    private class PagesAdapter(
        activity: FragmentActivity,
        private val magazineId: String,
        private val date: String,
        private val pages: Int,
        private val scale: String
    ) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = pages
        override fun createFragment(position: Int): Fragment =
            RevistaFragmentPage.newInstance(magazineId, date, position + 1, scale)
    }

    /** The current page slides away over the next one, which grows into place. */
    private class DepthPageTransformer : ViewPager2.PageTransformer {
        override fun transformPage(view: View, position: Float) {
            when {
                position <= -1f || position > 1f -> view.alpha = 0f
                position <= 0f -> {
                    view.alpha = 1f
                    view.translationX = 0f
                    view.translationZ = 0f
                    view.scaleX = 1f
                    view.scaleY = 1f
                }
                else -> {
                    view.alpha = 1 - position
                    view.translationX = view.width * -position
                    view.translationZ = -1f
                    val scale = MIN_SCALE + (1 - MIN_SCALE) * (1 - abs(position))
                    view.scaleX = scale
                    view.scaleY = scale
                }
            }
        }

        private companion object {
            const val MIN_SCALE = 0.75f
        }
    }

    companion object {
        private const val EXTRA_ID = "id"
        private const val EXTRA_DATE = "date"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_PAGES = "pages"
        private const val EXTRA_SCALE = "scale"

        fun createIntent(context: Context, portada: Portada, date: String): Intent =
            Intent(context, RevistaPagerActivity::class.java)
                .putExtra(EXTRA_ID, portada.id)
                .putExtra(EXTRA_DATE, date)
                .putExtra(EXTRA_TITLE, portada.name)
                .putExtra(EXTRA_PAGES, portada.pages)
                .putExtra(EXTRA_SCALE, portada.scale)
    }
}
