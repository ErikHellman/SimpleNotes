package se.hellsoft.simplenotes

import androidx.lifecycle.LiveData
import androidx.room.*
import timber.log.Timber

@Dao
interface NotesDao {
    @Query("SELECT * FROM note WHERE state != 3")
    fun loadNotes(): LiveData<List<NoteModel>>

    @Query("SELECT * FROM note")
    suspend fun allNotes(): List<NoteModel>

    @Query("SELECT * FROM note WHERE _id = :id")
    suspend fun loadNote(id: Long): NoteModel?

    @Query("SELECT * FROM note WHERE server_id = :serverId")
    suspend fun loadNoteByServerId(serverId: Long): NoteModel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveNote(noteModel: NoteModel): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveNotes(notes: List<NoteModel>)

    @Query("DELETE FROM note WHERE server_id = :serverId")
    suspend fun deleteNoteByServerId(serverId: Long)

    @Query("DELETE FROM note WHERE _id = :id")
    suspend fun deleteNoteByLocalId(id: Long)

    @Query("DELETE FROM note WHERE _id IN (:ids)")
    suspend fun deleteNotes(ids: List<Long>)

    @Transaction
    suspend fun syncNotes(serverNotes: List<ApiNoteModel>) {
        val notesForInsertion = mutableListOf<NoteModel>()
        for (serverNote in serverNotes) {
            val existingNote = loadNoteByServerId(serverNote.id)
            // Is it a new serverNote or is it not queued for update?
            if (existingNote == null
                || existingNote.state == NoteModel.State.Default
            ) {
                notesForInsertion += NoteModel(
                    id = existingNote?.id ?: 0,
                    serverId = serverNote.id,
                    title = serverNote.title,
                    content = serverNote.content,
                    state = NoteModel.State.Default
                )
            }
        }
        Timber.d("Store ${notesForInsertion.size} notes from server")
        saveNotes(notesForInsertion)

        val allNotes = allNotes();
        val notesForDeletion = mutableListOf<Long>()
        for (note in allNotes) {
            // Is it *not* scheduled for update and is it *not* among the notes from the API?
            if (note.state == NoteModel.State.Default
                && serverNotes.find { it.id == note.serverId } == null
            ) {
                notesForDeletion += note.id;
            }
        }
        Timber.d("Delete ${notesForDeletion.size} notes missing from server")
        // The IN clause in SQLite can take a maximum of 999 items. However, it seems that
        // performance is affected long before that size, so let's chunk up this list to
        // 100 items at a time at most.
        notesForDeletion.chunked(100).forEach { deleteNotes(it) }
    }
}

@Database(entities = [NoteModel::class], version = 2)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun notesDao(): NotesDao
}