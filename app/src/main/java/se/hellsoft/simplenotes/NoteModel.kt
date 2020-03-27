package se.hellsoft.simplenotes

import androidx.recyclerview.widget.DiffUtil
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "note")
data class NoteModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val title: String,
    val content: String
)

val NOTES_DIFF_CALLBACK = object : DiffUtil.ItemCallback<NoteModel>() {
    override fun areItemsTheSame(oldItem: NoteModel, newItem: NoteModel): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: NoteModel, newItem: NoteModel): Boolean =
        oldItem == newItem
}