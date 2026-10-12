package sergirex.portadasrevistas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import sergirex.portadasrevistas.databinding.PortadaLayoutBinding
import sergirex.portadasrevistas.domain.model.HomeTab
import sergirex.portadasrevistas.domain.model.IssueDate
import sergirex.portadasrevistas.domain.model.RevistaCover
import sergirex.portadasrevistas.presentation.portadas.PortadasViewModel
import java.io.File

/**
 * One home tab. Replaces Corazon/Cultura/.../Historia/Favoritos.java — eleven Fragments whose only
 * difference was which magazines to load. Loading and caching live behind PortadasViewModel; this
 * class only renders whatever PortadasUiState says and handles the long-press actions.
 */
@AndroidEntryPoint
class PortadasFragment : Fragment() {

    private val viewModel: PortadasViewModel by activityViewModels()
    private val tab: HomeTab by lazy { HomeTab.fromKey(requireArguments().getString(ARG_TAB)) }
    private var _binding: PortadaLayoutBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: PortadasAdapter

    // Only needed below Android 10, where saving to the gallery still requires the permission.
    private val requestStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val message = if (granted) R.string.storage_permission_granted else R.string.storage_permission_denied
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = PortadaLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val spanCount = if (resources.getBoolean(R.bool.isTablet)) 3 else 2
        adapter = PortadasAdapter(onCoverClick = ::openDetail, onCoverLongClick = ::showCoverMenu)
        binding.recyclerView.layoutManager = GridLayoutManager(context, spanCount)
        binding.recyclerView.adapter = adapter
        binding.refreshLayout.setOnRefreshListener { viewModel.refresh(tab) }
        // The grid is wrapped (with the empty-state text) in a FrameLayout, so tell the
        // pull-to-refresh gesture to look at the RecyclerView's scroll position instead.
        binding.refreshLayout.setOnChildScrollUpCallback { _, _ -> binding.recyclerView.canScrollVertically(-1) }

        // Render whenever the page is on screen or being swiped to...
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState(tab).collect { state ->
                    adapter.submitList(state.covers)
                    binding.refreshLayout.isRefreshing = state.isRefreshing
                    binding.emptyStateText.isVisible = state.showLoadFailed
                }
            }
        }
        // ...but only fetch for the page the user is actually on (RESUMED). Pages that are merely
        // passed through when jumping straight to a far tab are STARTED only; fetching for them would
        // queue their requests ahead of the tab the user chose.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) { viewModel.loadIfNeeded(tab) }
        }
    }

    private fun openDetail(clicked: RevistaCover) {
        // The pager of the detail screen swipes between the covers this tab has shown.
        val portadas = viewModel.uiState(tab).value.covers.map { Portada.from(it.revista) }
        startActivity(PortadaDetalle.createIntent(requireContext(), portadas, clicked.revista.id))
    }

    private fun showCoverMenu(anchor: View, cover: RevistaCover) {
        PopupMenu(requireContext(), anchor).apply {
            menuInflater.inflate(R.menu.menu_portada_list, menu)
            setForceShowIcon(true)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.share -> shareCover(cover)
                    R.id.save -> saveCover(anchor, cover)
                    R.id.web -> requireContext().openWebsite(cover.revista.web)
                }
                true
            }
        }.show()
    }

    private fun savePortada() = SavePortada(requireContext(), requestStoragePermission)

    private fun shareCover(cover: RevistaCover) {
        val savePortada = savePortada()
        viewLifecycleOwner.lifecycleScope.launch { savePortada.share(cover.imageUrl, cover.revista.fileName()) }
    }

    private fun saveCover(anchor: View, cover: RevistaCover) {
        val savePortada = savePortada()
        if (!savePortada.isExternalStorageWritable) {
            Toast.makeText(requireContext(), R.string.storage_unavailable, Toast.LENGTH_LONG).show()
            return
        }
        if (!savePortada.checkPermissions()) return
        val file = File(savePortada.albumStorageDir, "${cover.revista.fileName(IssueDate.today())}.jpg")
        if (file.exists()) {
            Toast.makeText(requireContext(), R.string.cover_already_saved, Toast.LENGTH_LONG).show()
        } else {
            viewLifecycleOwner.lifecycleScope.launch { savePortada.saveToGallery(anchor, cover.imageUrl, file) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TAB = "tab"

        fun newInstance(tab: HomeTab): PortadasFragment = PortadasFragment().apply {
            arguments = Bundle().apply { putString(ARG_TAB, tab.key) }
        }
    }
}
