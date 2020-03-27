package se.hellsoft.simplenotes

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext


class NotesViewModel(
    private val repository: NotesRepository,
    private val notesDao: NotesDao
) : ViewModel() {
    val notes = notesDao.loadNotes()

    suspend fun loadNotes(id: Int): NoteModel? = coroutineScope {
        withContext(Dispatchers.IO) {
            notesDao.loadNote(id)
        }
    }

    suspend fun saveNote(noteModel: NoteModel) = coroutineScope {
        withContext(Dispatchers.IO) {
            notesDao.saveNote(noteModel)
            repository.saveNote(noteModel)
        }
    }

    suspend fun deleteNote(noteModel: NoteModel) = coroutineScope {
        withContext(Dispatchers.IO) {
            notesDao.deleteNote(noteModel)
            repository.deleteNote(noteModel)
        }
    }
}