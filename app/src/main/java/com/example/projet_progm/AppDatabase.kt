import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Games::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}