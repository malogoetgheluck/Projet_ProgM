import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Games(
    @PrimaryKey val uid: Int,
    @ColumnInfo(name = "game_name") val gameName: String?,
    @ColumnInfo(name = "game_activity") val gameActivity: String?
)