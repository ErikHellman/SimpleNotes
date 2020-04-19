package se.hellsoft.simplenotes

import android.content.Context
import androidx.work.*
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit

class SaveNoteWorker(
    private val notesDao: NotesDao,
    private val notesRepository: NotesRepository,
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    companion object {
        const val ID = "id"
        const val TAG = "saveNote"

        fun saveNoteToBackend(id: Long, workManager: WorkManager) {
            val data = Data.Builder().putLong(ID, id).build()
            val constraints =
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            val request = OneTimeWorkRequestBuilder<SaveNoteWorker>()
                .setInputData(data)
                .setConstraints(constraints)
                .addTag(TAG)
                .build();
            workManager.enqueueUniqueWork(
                "save_$id",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        val id = inputData.getLong(ID, 0)
        val note = notesDao.loadNote(id) ?: return Result.failure()
        Timber.d("Save note to backend: $note")
        val apiNote = ApiNoteModel(note.serverId, note.title, note.content)
        val updatedNote = notesRepository.saveNote(apiNote)
        notesDao.saveNote(
            NoteModel(
                note.id,
                updatedNote.id,
                updatedNote.title,
                updatedNote.content,
                state = NoteModel.State.Default
            )
        )
        Timber.d("Note saved and updated successfully")

        return Result.success()
    }
}

class DeleteNoteWorker(
    private val notesDao: NotesDao,
    private val notesRepository: NotesRepository,
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    companion object {
        const val ID = "id"
        const val TAG = "deleteNote"

        fun deleteNoteFromBackend(serverId: Long, workManager: WorkManager) {
            val data = Data.Builder().putLong(ID, serverId).build()
            val constraints =
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            val request = OneTimeWorkRequestBuilder<DeleteNoteWorker>()
                .setInputData(data)
                .setConstraints(constraints)
                .build();
            workManager.enqueueUniqueWork(
                "delete_$serverId",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        val id = inputData.getLong(ID, 0)
        if (this.runAttemptCount > 10) {
            notesDao.deleteNoteByServerId(id)
            return Result.success()
        }

        Timber.d("Delete note with ID $id")

        try {
            notesRepository.deleteNote(id)
            notesDao.deleteNoteByServerId(id)
        } catch (e: ServerError) {
            when (e.errorCode) {
                404 -> notesDao.deleteNoteByServerId(id)
                else -> return Result.retry()
            }
        } catch (e: IOException) {
            return Result.retry()
        }
        Timber.d("Note $id deleted successfully")
        return Result.success()
    }
}

class SyncNotesWorker(
    private val notesDao: NotesDao,
    private val notesRepository: NotesRepository,
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "works"
        const val NAME = "syncWorker"

        fun syncNotes(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request =
                PeriodicWorkRequest.Builder(SyncNotesWorker::class.java, 15L, TimeUnit.MINUTES)
                    .addTag(TAG)
                    .setConstraints(constraints)
                    .build()
            workManager.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.REPLACE, request)
        }
    }

    override suspend fun doWork(): Result {
        val serverNotes = notesRepository.fetchNotes()
        Timber.d("Start syncing ${serverNotes.size} notes from server")
        notesDao.syncNotes(serverNotes)
        Timber.d("Notes synced successfully")
        return Result.success()
    }
}