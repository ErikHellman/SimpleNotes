package se.hellsoft.simplenotes

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.*
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import okhttp3.Dispatcher
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

interface NotesRepository {
    suspend fun fetchNotes(): List<ApiNoteModel>
    suspend fun fetchNote(id: Long): ApiNoteModel?
    suspend fun saveNote(noteModel: ApiNoteModel): ApiNoteModel
    suspend fun deleteNote(id: Long)
}

class ServerError(val errorCode: Int) : Exception()

@Suppress("unused")
class KtorNotesRepository : NotesRepository {
    companion object {
        const val BASE_URL = "https://api.notes-company.com/v1"
    }

    private val httpClient: HttpClient = HttpClient(OkHttp) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    };

    override suspend fun fetchNotes(): List<ApiNoteModel> {
        return httpClient.get {
            url("$BASE_URL/notes")
            contentType(ContentType.Application.Json)
        }
    }

    override suspend fun fetchNote(id: Long): ApiNoteModel? {
        return httpClient.get {
            url("$BASE_URL/notes/$id")
            contentType(ContentType.Application.Json)
        }
    }

    override suspend fun saveNote(noteModel: ApiNoteModel): ApiNoteModel {
        return httpClient.post {
            url("$BASE_URL/notes/${noteModel.id}")
            contentType(ContentType.Application.Json)
            body = noteModel
        }
    }

    override suspend fun deleteNote(id: Long) {
        httpClient.delete<Unit> {
            url("$BASE_URL/notes/${id}")
            contentType(ContentType.Application.Json)
        }
    }
}

/**
 * A fake API implementation that works on a local JSON file.
 */
@ExperimentalStdlibApi
class FakeNotesRepository(private val jsonFile: File) : NotesRepository, CoroutineScope {
    private val serializer = ApiNoteModel.serializer()
    private val json = Json(JsonConfiguration.Stable)

    override suspend fun fetchNotes(): List<ApiNoteModel> = coroutineScope {
        if (!jsonFile.exists()) {
            return@coroutineScope emptyList<ApiNoteModel>()
        }
        val jsonData = jsonFile.readText();
        return@coroutineScope json.fromJson(serializer.list, json.parseJson(jsonData))
    }

    override suspend fun fetchNote(id: Long): ApiNoteModel? = coroutineScope {
        return@coroutineScope fetchNotes().find { it.id == id }
    }

    override suspend fun saveNote(noteModel: ApiNoteModel): ApiNoteModel = coroutineScope {
        val notes = fetchNotes().toMutableList()
        val index = notes.indexOfFirst { it.id == noteModel.id }
        return@coroutineScope if (index > -1) {
            notes[index] = noteModel
            writeToFile(notes)
            noteModel
        } else {
            val nextId = nextId(notes)
            val newModel = noteModel.copy(id = nextId)
            notes += newModel
            writeToFile(notes)
            newModel
        }
    }

    override suspend fun deleteNote(id: Long) = coroutineScope {
        val notes = fetchNotes().toMutableList().toMutableList()
        notes.removeIf { it.id == id }
        writeToFile(notes)
        return@coroutineScope
    }

    private fun nextId(notes: List<ApiNoteModel>): Long {
        val latest = notes.maxBy { it.id }
        return if (latest != null) {
            latest.id + 1L
        } else {
            1L
        }
    }

    private fun writeToFile(notes: List<ApiNoteModel>) {
        val jsonData = json.stringify(serializer.list, notes)
        if (!jsonFile.exists()) {
            jsonFile.createNewFile()
        }
        jsonFile.writeText(jsonData)
    }

    private val _coroutineContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    override val coroutineContext: CoroutineContext
        get() = _coroutineContext
}