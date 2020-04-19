package se.hellsoft.simplenotes

import androidx.recyclerview.widget.DiffUtil
import androidx.room.*
import kotlinx.serialization.Serializable

@Entity(tableName = "note")
@TypeConverters(NoteModel.State::class)
data class NoteModel(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val id: Long,
    @ColumnInfo(name = "server_id")
    val serverId: Long,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "content")
    val content: String,
    @ColumnInfo(name = "state")
    val state: State = State.Created
) {
    enum class State(val value: Int) {
        Created(1), Updating(2), Deleting(3), Default(4);

        companion object {

            @JvmStatic
            @TypeConverter
            fun fromDb(value: Int): State {
                return when (value) {
                    1 -> State.Created
                    2 -> State.Updating
                    3 -> State.Deleting
                    4 -> State.Default
                    else -> throw IllegalArgumentException("Unknown state $value!")
                }
            }

            @JvmStatic
            @TypeConverter
            fun toDb(state: State): Int {
                return when (state) {
                    State.Created -> 1
                    State.Updating -> 2
                    State.Deleting -> 3
                    State.Default -> 4
                }
            }
        }
    }
}


@Serializable
data class ApiNoteModel(
    val id: Long,
    val title: String,
    val content: String
)

val NOTES_DIFF_CALLBACK = object : DiffUtil.ItemCallback<NoteModel>() {
    override fun areItemsTheSame(oldItem: NoteModel, newItem: NoteModel): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: NoteModel, newItem: NoteModel): Boolean =
        oldItem == newItem
}