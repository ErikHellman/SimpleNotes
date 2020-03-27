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

interface NotesRepository {
    suspend fun fetchNotes(): List<NoteModel>
    suspend fun fetchNote(id: Int): NoteModel?
    suspend fun saveNote(noteModel: NoteModel): NoteModel
    suspend fun deleteNote(noteModel: NoteModel)
}

class KtorNotesRepository : NotesRepository {
    companion object {
        const val BASE_URL = "https://api.notes-company.com/v1"
    }

    private val httpClient: HttpClient = HttpClient(OkHttp) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    };

    override suspend fun fetchNotes(): List<NoteModel> {
        return httpClient.get {
            url("$BASE_URL/notes")
            contentType(ContentType.Application.Json)
        }
    }

    override suspend fun fetchNote(id: Int): NoteModel? {
        return httpClient.get {
            url("$BASE_URL/notes/$id")
            contentType(ContentType.Application.Json)
        }
    }

    override suspend fun saveNote(noteModel: NoteModel): NoteModel {
        return httpClient.post {
            url("$BASE_URL/notes/${noteModel.id}")
            contentType(ContentType.Application.Json)
            body = noteModel
        }
    }

    override suspend fun deleteNote(noteModel: NoteModel) {
        httpClient.delete<Unit> {
            url("$BASE_URL/notes/${noteModel.id}")
            contentType(ContentType.Application.Json)
        }
    }
}

class FakeNotesRepository : NotesRepository {
    private val notes = mutableListOf<NoteModel>()

    override suspend fun fetchNotes(): List<NoteModel> {
        return notes;
    }

    override suspend fun fetchNote(id: Int): NoteModel? {
        return notes.find { it.id == id }
    }

    override suspend fun saveNote(noteModel: NoteModel): NoteModel {
        val index = notes.indexOfFirst { it.id == noteModel.id }
        if (index != -1) {
            notes[index] = noteModel
        } else {
            notes += noteModel
        }
        return noteModel
    }

    override suspend fun deleteNote(noteModel: NoteModel) {
        val index = notes.indexOfFirst { it.id == noteModel.id }
        if (index != -1) {
            notes.removeAt(index)
        }
    }
}