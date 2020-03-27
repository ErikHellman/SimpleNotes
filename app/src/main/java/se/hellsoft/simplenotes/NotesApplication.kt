package se.hellsoft.simplenotes

import android.app.Application
import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

class NotesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@NotesApplication)
            modules(appModule)
        }
    }
}

val appModule = module {
    // Use the FakeNotesRepository to fake our network requests
    single<NotesRepository> { FakeNotesRepository() }
    single { Room.databaseBuilder(androidContext(), NotesDatabase::class.java, "notes.db").fallbackToDestructiveMigration().build() }
    single { get<NotesDatabase>().notesDao() }
    viewModel { NotesViewModel(get(), get()) }
}
