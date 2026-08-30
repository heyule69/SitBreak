package com.sitbreak.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sitbreak.domain.SessionSplitter
import kotlinx.coroutines.flow.Flow

/** 每日统计数据 */
@Entity(tableName = "daily_stat")
data class DailyStat(
    /** yyyy-MM-dd */
    @PrimaryKey val date: String,
    val sedentaryMinutes: Int = 0,
    val standCount: Int = 0,
    val reminderCount: Int = 0,
    /** 其中由智能暂停自动判定的站立次数 */
    val autoStandCount: Int = 0,
)

@Dao
interface DailyStatDao {

    @Query("SELECT * FROM daily_stat WHERE date = :date LIMIT 1")
    suspend fun getOn(date: String): DailyStat?

    @Query("SELECT * FROM daily_stat WHERE date = :date LIMIT 1")
    fun observeOn(date: String): Flow<DailyStat?>

    @Query("SELECT * FROM daily_stat WHERE date >= :fromDate ORDER BY date ASC")
    fun observeSince(fromDate: String): Flow<List<DailyStat>>

    @Query("SELECT * FROM daily_stat ORDER BY date ASC")
    suspend fun getAll(): List<DailyStat>

    @Query("SELECT * FROM daily_stat ORDER BY date ASC")
    fun observeAll(): Flow<List<DailyStat>>

    @Query("SELECT SUM(standCount) FROM daily_stat")
    suspend fun totalStands(): Int?

    @Query("SELECT SUM(sedentaryMinutes) FROM daily_stat")
    suspend fun totalSedentaryMinutes(): Int?

    /** 记一次提醒 */
    @Query(
        "INSERT INTO daily_stat (date, sedentaryMinutes, standCount, reminderCount, autoStandCount) " +
            "VALUES (:date, 0, 0, 1, 0) " +
            "ON CONFLICT(date) DO UPDATE SET reminderCount = reminderCount + 1"
    )
    suspend fun incrementReminder(date: String)

    /** 记一次站立确认；auto 传 1 表示由智能暂停自动判定 */
    @Query(
        "INSERT INTO daily_stat (date, sedentaryMinutes, standCount, reminderCount, autoStandCount) " +
            "VALUES (:date, 0, 1, 0, :auto) " +
            "ON CONFLICT(date) DO UPDATE SET standCount = standCount + 1, " +
            "autoStandCount = autoStandCount + :auto"
    )
    suspend fun incrementStandCount(date: String, auto: Int)

    /** 给某一天累加久坐分钟数（跨午夜时会分别写入两天） */
    @Query(
        "INSERT INTO daily_stat (date, sedentaryMinutes, standCount, reminderCount, autoStandCount) " +
            "VALUES (:date, :minutes, 0, 0, 0) " +
            "ON CONFLICT(date) DO UPDATE SET sedentaryMinutes = sedentaryMinutes + :minutes"
    )
    suspend fun addSedentaryMinutes(date: String, minutes: Int)
}

@Database(entities = [DailyStat::class], version = 2, exportSchema = true)
abstract class StatsDatabase : RoomDatabase() {
    abstract fun dailyStatDao(): DailyStatDao

    companion object {

        /**
         * v1 -> v2：新增 autoStandCount。
         *
         * 这里必须走真实迁移而不是销毁重建：连续打卡天数是本应用的核心激励，
         * 一旦清库用户的历史记录和成就就全部归零。
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_stat ADD COLUMN autoStandCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun build(context: Context): StatsDatabase =
            Room.databaseBuilder(context, StatsDatabase::class.java, "sitbreak.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}

/** 统计仓库：日期与累加逻辑的封装 */
class StatsRepository(private val db: StatsDatabase) {

    private val dao get() = db.dailyStatDao()

    fun today(): String = SessionSplitter.dateOf(System.currentTimeMillis())

    fun observeToday(today: String = today()): Flow<DailyStat?> = dao.observeOn(today)

    fun observeLastDays(days: Int, todayMillis: Long = System.currentTimeMillis()): Flow<List<DailyStat>> =
        dao.observeSince(SessionSplitter.startDateOfLastDays(days, todayMillis))

    suspend fun incrementReminder() = dao.incrementReminder(today())

    /**
     * 结算一轮久坐。
     *
     * 站立次数记在会话结束那天，久坐时长按自然日归属，
     * 这样 23:40~00:20 的会话不会把昨天的 20 分钟算到今天。
     */
    suspend fun addStand(beginMillis: Long, endMillis: Long, auto: Boolean = false) {
        dao.incrementStandCount(SessionSplitter.dateOf(endMillis), if (auto) 1 else 0)
        SessionSplitter.split(beginMillis, endMillis).forEach {
            dao.addSedentaryMinutes(it.date, it.minutes)
        }
    }

    suspend fun allHistory(): List<DailyStat> = dao.getAll()

    /** 整表观察：任何一天的统计落库都会重新发射，供统计页实时刷新 */
    fun observeAllHistory(): Flow<List<DailyStat>> = dao.observeAll()

    suspend fun totalStands(): Int = dao.totalStands() ?: 0
}
