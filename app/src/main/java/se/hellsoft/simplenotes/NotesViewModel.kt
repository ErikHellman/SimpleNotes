package se.hellsoft.simplenotes

import androidx.lifecycle.ViewModel
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class NotesViewModel(
    private val notesDao: NotesDao,
    private val workManager: WorkManager
) : ViewModel() {
    val notes = notesDao.loadNotes()

    suspend fun loadNote(id: Long): NoteModel? = coroutineScope {
        withContext(Dispatchers.IO) {
            notesDao.loadNote(id)
        }
    }

    suspend fun saveNote(noteModel: NoteModel) = coroutineScope {
        withContext(Dispatchers.IO) {
            var id = noteModel.id
            if (id == 0L) {
                id = notesDao.saveNote(noteModel.copy(state = NoteModel.State.Created))
            } else {
                notesDao.saveNote(noteModel.copy(state = NoteModel.State.Updating))
            }
            SaveNoteWorker.saveNoteToBackend(id, workManager)
        }
    }

    suspend fun deleteNote(noteModel: NoteModel) = coroutineScope {
        withContext(Dispatchers.IO) {
            // Do we know if this note exists on the server?
            if (noteModel.serverId == 0L) {
                notesDao.deleteNoteByLocalId(noteModel.id)
            } else {
                notesDao.saveNote(noteModel.copy(state = NoteModel.State.Deleting))
                DeleteNoteWorker.deleteNoteFromBackend(noteModel.serverId, workManager)
            }
        }
    }
}