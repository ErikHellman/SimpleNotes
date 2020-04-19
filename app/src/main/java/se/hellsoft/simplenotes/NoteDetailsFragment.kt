package se.hellsoft.simplenotes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import se.hellsoft.simplenotes.databinding.NoteDetailsFragmentBinding

class NoteDetailsFragment : Fragment() {
    private val viewModel: NotesViewModel by viewModel()
    private val args: NoteDetailsFragmentArgs by navArgs()
    private lateinit var binding: NoteDetailsFragmentBinding
    private lateinit var noteModel: NoteModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return NoteDetailsFragmentBinding.inflate(inflater, container, false)
            .apply { binding = this }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launchWhenResumed {
            noteModel =
                viewModel.loadNote(args.noteId) ?: NoteModel(0, 0L, "", "", NoteModel.State.Created)
            binding.titleText.setText(noteModel.title)
            binding.contentText.setText(noteModel.content)
        }

        binding.saveButton.setOnClickListener {
            val titleText = binding.titleText.text.toString()
            val contentText = binding.contentText.text.toString()

            lifecycleScope.launch {
                viewModel.saveNote(noteModel.copy(title = titleText, content = contentText))
            }
            findNavController().popBackStack()
        }

        binding.deleteButton.setOnClickListener {
            lifecycleScope.launch {
                viewModel.deleteNote(noteModel)
                findNavController().popBackStack()
            }
        }

        binding.cancelButton.setOnClickListener { findNavController().popBackStack() }
    }
}