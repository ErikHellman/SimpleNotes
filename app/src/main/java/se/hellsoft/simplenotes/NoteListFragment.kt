package se.hellsoft.simplenotes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkManager
import org.koin.androidx.viewmodel.ext.android.viewModel
import se.hellsoft.simplenotes.databinding.NoteItemBinding
import se.hellsoft.simplenotes.databinding.NoteListFragmentBinding
import timber.log.Timber

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class NoteListFragment : Fragment() {
    private val viewModel: NotesViewModel by viewModel()

    private lateinit var binding: NoteListFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = NoteListFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val noteListAdapter = NoteListAdapter()
        binding.noteList.adapter = noteListAdapter
        viewModel.notes.observe(viewLifecycleOwner,
            Observer { notes -> noteListAdapter.submitList(notes) })

        binding.fab.setOnClickListener {
            val action = NoteListFragmentDirections.actionListToDetails(0)
            it.findNavController().navigate(action)
        }

        binding.swipeToRefresh.setOnRefreshListener {
            Timber.d("Reschedule notes sync!")
            SyncNotesWorker.syncNotes(WorkManager.getInstance(requireContext()))
            binding.swipeToRefresh.isRefreshing = false
        }
    }

    override fun onResume() {
        super.onResume()
    }
}

class NoteViewHolder(private val binding: NoteItemBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(noteModel: NoteModel) {
        binding.noteTitle.text = noteModel.title
    }
}

class NoteListAdapter() : ListAdapter<NoteModel, NoteViewHolder>(NOTES_DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = NoteItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val noteViewHolder = NoteViewHolder(binding)
        binding.root.setOnClickListener {
            val noteModel = getItem(noteViewHolder.adapterPosition)
            val action = NoteListFragmentDirections.actionListToDetails(noteModel.id)
            it.findNavController().navigate(action)
        }

        return noteViewHolder
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}