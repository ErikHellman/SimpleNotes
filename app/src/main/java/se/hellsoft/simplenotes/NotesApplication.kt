package se.hellsoft.simplenotes

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.work.*
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import se.hellsoft.simplenotes.SyncNotesWorker.Companion.syncNotes
import timber.log.Timber
import java.io.File


class NotesApplication : Application() {
    @ExperimentalStdlibApi
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        startKoin {
            androidLogger()
            androidContext(this@NotesApplication)
            modules(appModule)
        }

        // Start the period sync work...
        val workerFactory: WorkerFactory = get()
        val configuration = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
        WorkManager.initialize(this, configuration)
        syncNotes(WorkManager.getInstance(this))
    }
}

class MyWorkerFactory(
    private val notesDao: NotesDao,
    private val notesRepository: NotesRepository
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            SaveNoteWorker::class.java.name -> SaveNoteWorker(
                notesDao,
                notesRepository,
                appContext,
                workerParameters
            )
            DeleteNoteWorker::class.java.name -> DeleteNoteWorker(
                notesDao,
                notesRepository,
                appContext,
                workerParameters
            )
            SyncNotesWorker::class.java.name -> SyncNotesWorker(
                notesDao,
                notesRepository,
                appContext,
                workerParameters
            )
            else -> null
        }
    }
}

@ExperimentalStdlibApi
val appModule = module {
    // Use the FakeNotesRepository to fake our network requests
    single<NotesRepository> { FakeNotesRepository(File(androidContext().filesDir, "notes.json")) }
    single {
        Room.databaseBuilder(androidContext(), NotesDatabase::class.java, "notes.db")
            .fallbackToDestructiveMigration().build()
    }
    single { get<NotesDatabase>().notesDao() }
    single<WorkerFactory> { MyWorkerFactory(get(), get()) }
    viewModel { NotesViewModel(get(), WorkManager.getInstance(get())) }
}
