package sergirex.portadasrevistas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil3.load
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import sergirex.portadasrevistas.databinding.FragmentPortadaDetalleBinding
import sergirex.portadasrevistas.domain.model.CoverUrls
import sergirex.portadasrevistas.domain.model.IssueDate
import sergirex.portadasrevistas.domain.model.OldIssuePolicy
import sergirex.portadasrevistas.domain.model.RevistaCategory
import sergirex.portadasrevistas.presentation.detail.IssueState
import sergirex.portadasrevistas.presentation.detail.PortadaDetalleViewModel

/** What a magazine's page needs from the Activity that hosts the pager. */
interface PortadaDetalleHost {
    /** The user tapped the cover of magazine [magazineId]: open the reader, or offer the subscription. */
    fun onCoverClicked(magazineId: String)
}

/**
 * One magazine in the detail pager: its cover for the selected issue. The cover the grid already
 * showed appears at once (Coil has it cached) while the issue calendar loads; when the calendar
 * says which issue is selected, that issue's full-size cover replaces it.
 */
@AndroidEntryPoint
class PortadaDetalleFragment : Fragment() {

    private val viewModel: PortadaDetalleViewModel by activityViewModels()
    private val magazineId: String by lazy { requireArguments().getString(ARG_ID).orEmpty() }
    private val category: RevistaCategory? by lazy {
        requireArguments().getString(ARG_CATEGORY)?.let { name -> RevistaCategory.entries.firstOrNull { it.name == name } }
    }

    private var _binding: FragmentPortadaDetalleBinding? = null
    private val binding get() = _binding!!

    /** The issue date whose full-size cover is on screen (or being loaded). */
    private var shownDate: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPortadaDetalleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cover.load(CoverUrls.latest(magazineId))
        binding.cover.setOnClickListener { (activity as? PortadaDetalleHost)?.onCoverClicked(magazineId) }

        viewModel.loadIssues(magazineId)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.issue(magazineId).collect(::render)
            }
        }
    }

    private fun render(issue: IssueState) {
        val selected = issue.selected
        when {
            selected != null && selected != shownDate -> showIssueCover(issue, selected)
            issue.failed -> {
                binding.loadingProgressBar.visibility = View.GONE
                Toast.makeText(requireContext(), R.string.cover_not_found, Toast.LENGTH_LONG).show()
            }
            selected != null -> renderDateLabel(issue)
        }
    }

    private fun showIssueCover(issue: IssueState, date: String) {
        shownDate = date
        binding.loadingProgressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            // Applied by hand: `ImageRequest.target()` combined with `execute()` was found to hang (see loadBitmap).
            val bitmap = requireContext().loadBitmap(CoverUrls.issueCover(magazineId, date))
            binding.loadingProgressBar.visibility = View.GONE
            if (bitmap == null) {
                shownDate = null
                Toast.makeText(requireContext(), R.string.cover_not_found, Toast.LENGTH_LONG).show()
            } else {
                binding.cover.setImageBitmap(bitmap)
                renderDateLabel(issue)
            }
        }
    }

    private fun renderDateLabel(issue: IssueState) {
        val selected = issue.selected
        binding.issueDate.visibility = if (selected != null && !issue.isLatestSelected) View.VISIBLE else View.GONE
        if (selected != null) binding.issueDate.text = IssueDate.toDisplay(selected)
        val old = selected != null && OldIssuePolicy.isOld(selected, category)
        binding.oldRibbon.visibility = if (old) View.VISIBLE else View.GONE
        if (old) binding.oldRibbon.text = IssueDate.toRibbonLabel(selected.orEmpty())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Avoid memory leaks by nullifying the binding reference.
        _binding = null
    }

    companion object {
        private const val ARG_ID = "id"
        private const val ARG_CATEGORY = "category"

        fun newInstance(magazineId: String, category: RevistaCategory?) = PortadaDetalleFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_ID, magazineId)
                putString(ARG_CATEGORY, category?.name)
            }
        }
    }
}
