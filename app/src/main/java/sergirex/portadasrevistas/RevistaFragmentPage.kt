package sergirex.portadasrevistas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import coil3.load
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import dagger.hilt.android.AndroidEntryPoint
import sergirex.portadasrevistas.databinding.FragmentRevistaPageBinding
import sergirex.portadasrevistas.domain.model.CoverUrls

/** One zoomable page of an issue in the reader. */
@AndroidEntryPoint
class RevistaFragmentPage : Fragment() {

    private var _binding: FragmentRevistaPageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRevistaPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        val url = CoverUrls.issuePage(
            id = args.getString(ARG_ID).orEmpty(),
            date = args.getString(ARG_DATE).orEmpty(),
            page = args.getInt(ARG_PAGE),
            scale = args.getString(ARG_SCALE).orEmpty()
        )
        binding.loadingProgressBar.isVisible = true
        binding.page.load(url) {
            listener(object : ImageRequest.Listener {
                override fun onSuccess(request: ImageRequest, result: SuccessResult) {
                    _binding?.loadingProgressBar?.isVisible = false
                }

                override fun onError(request: ImageRequest, result: ErrorResult) {
                    _binding?.loadingProgressBar?.isVisible = false
                    _binding?.errorText?.isVisible = true
                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ID = "id"
        private const val ARG_DATE = "date"
        private const val ARG_PAGE = "page"
        private const val ARG_SCALE = "scale"

        fun newInstance(id: String, date: String, page: Int, scale: String) = RevistaFragmentPage().apply {
            arguments = Bundle().apply {
                putString(ARG_ID, id)
                putString(ARG_DATE, date)
                putInt(ARG_PAGE, page)
                putString(ARG_SCALE, scale)
            }
        }
    }
}
