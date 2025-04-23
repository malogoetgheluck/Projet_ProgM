import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    @Query("SELECT * FROM games")
    suspend fun getAll(): List<Games>

    @Query("UPDATE games SET highscore = :newScore WHERE uid = :id")
    suspend fun updateHighScore(id: Int, newScore: Int)

    @Query("SELECT * FROM games WHERE uid IN (:userIds)")
    suspend fun loadAllByIds(userIds: IntArray): List<Games>

    @Query("SELECT highScore FROM games WHERE uid = :userId")
    suspend fun getHighScore(userId: Int): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg users: Games)

    @Delete
    suspend fun delete(user: Games)
}