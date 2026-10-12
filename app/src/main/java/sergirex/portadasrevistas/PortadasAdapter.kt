package sergirex.portadasrevistas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import sergirex.portadasrevistas.databinding.PortadaItemBinding
import sergirex.portadasrevistas.domain.model.IssueDate
import sergirex.portadasrevistas.domain.model.RevistaCover

/**
 * ListAdapter (DiffUtil-backed) over the immutable `List<RevistaCover>` snapshot from
 * PortadasViewModel's StateFlow, replacing the old hand-built rows of ImageButtons with a
 * background Bitmap each. Image loading, memory and disk caching are Coil's job.
 */
class PortadasAdapter(
    private val onCoverClick: (RevistaCover) -> Unit,
    private val onCoverLongClick: (View, RevistaCover) -> Unit
) : ListAdapter<RevistaCover, PortadasAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = PortadaItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: PortadaItemBinding) : RecyclerView.ViewHolder(binding.root) {
        /** Out-of-date issues get a ribbon with the day they came out. */
        private fun showRibbon(cover: RevistaCover) {
            val issueDate = cover.issueDate
            val old = cover.isOld && issueDate != null
            binding.oldRibbon.visibility = if (old) View.VISIBLE else View.GONE
            if (old) binding.oldRibbon.text = IssueDate.toRibbonLabel(issueDate.orEmpty())
        }

        fun bind(cover: RevistaCover) {
            binding.portadaImageView.contentDescription = cover.revista.name
            binding.portadaImageView.load(cover.imageUrl)
            showRibbon(cover)
            binding.root.setOnClickListener { onCoverClick(cover) }
            binding.root.setOnLongClickListener {
                onCoverLongClick(binding.root, cover)
                true
            }
        }
    }

    private companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RevistaCover>() {
            override fun areItemsTheSame(oldItem: RevistaCover, newItem: RevistaCover) =
                oldItem.revista.id == newItem.revista.id

            override fun areContentsTheSame(oldItem: RevistaCover, newItem: RevistaCover) = oldItem == newItem
        }
    }
}
