package se.hellsoft.simplenotes

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface NotesDao {
    @Query("SELECT * FROM note")
    fun loadNotes(): LiveData<List<NoteModel>>

    @Query("SELECT * FROM note WHERE id = :id")
    suspend fun loadNote(id: Int): NoteModel

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveNote(noteModel: NoteModel)

    @Delete
    suspend fun deleteNote(noteModel: NoteModel)
}

@Database(entities = [NoteModel::class], version = 2)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun notesDao(): NotesDao
}