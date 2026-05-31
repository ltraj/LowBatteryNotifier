package com.example.lowbatterynotifier.analytics.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class BatteryAnalyticsStore(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DB_NAME,
    null,
    DB_VERSION,
) {

    fun insertDrainSample(sample: DrainSampleEntity): Long {
        val values = ContentValues().apply {
            put(COL_TIMESTAMP, sample.timestampMillis)
            put(COL_BATTERY_PERCENT, sample.batteryPercent)
            put(COL_IS_CHARGING, if (sample.isCharging) 1 else 0)
        }
        return writableDatabase.insert(TABLE_DRAIN_SAMPLES, null, values)
    }

    fun getLatestDrainSample(): DrainSampleEntity? {
        return readableDatabase.query(
            TABLE_DRAIN_SAMPLES,
            drainColumns,
            null,
            null,
            null,
            null,
            "$COL_TIMESTAMP DESC",
            "1",
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            cursor.toDrainSample()
        }
    }

    fun getDrainSamplesSince(sinceMillis: Long): List<DrainSampleEntity> {
        return readableDatabase.query(
            TABLE_DRAIN_SAMPLES,
            drainColumns,
            "$COL_TIMESTAMP >= ?",
            arrayOf(sinceMillis.toString()),
            null,
            null,
            "$COL_TIMESTAMP ASC",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.toDrainSample())
                }
            }
        }
    }

    fun deleteDrainSamplesOlderThan(beforeMillis: Long): Int =
        writableDatabase.delete(
            TABLE_DRAIN_SAMPLES,
            "$COL_TIMESTAMP < ?",
            arrayOf(beforeMillis.toString()),
        )

    fun insertChargingSession(session: ChargingSessionEntity): Long {
        val values = ContentValues().apply {
            put(COL_START_TIME, session.startTimeMillis)
            put(COL_END_TIME, session.endTimeMillis)
            put(COL_START_PERCENT, session.startPercent)
            put(COL_END_PERCENT, session.endPercent)
        }
        return writableDatabase.insert(TABLE_CHARGING_SESSIONS, null, values)
    }

    fun getOpenChargingSession(): ChargingSessionEntity? {
        return readableDatabase.query(
            TABLE_CHARGING_SESSIONS,
            chargingColumns,
            "$COL_END_TIME IS NULL",
            null,
            null,
            null,
            "$COL_START_TIME DESC",
            "1",
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            cursor.toChargingSession()
        }
    }

    fun closeChargingSession(sessionId: Long, endTimeMillis: Long, endPercent: Int) {
        val values = ContentValues().apply {
            put(COL_END_TIME, endTimeMillis)
            put(COL_END_PERCENT, endPercent)
        }
        writableDatabase.update(
            TABLE_CHARGING_SESSIONS,
            values,
            "$COL_ID = ?",
            arrayOf(sessionId.toString()),
        )
    }

    fun getRecentChargingSessions(limit: Int): List<ChargingSessionEntity> {
        return readableDatabase.query(
            TABLE_CHARGING_SESSIONS,
            chargingColumns,
            null,
            null,
            null,
            null,
            "$COL_START_TIME DESC",
            limit.toString(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.toChargingSession())
                }
            }
        }
    }

    fun deleteCompletedChargingSessionsOlderThan(beforeMillis: Long): Int =
        writableDatabase.delete(
            TABLE_CHARGING_SESSIONS,
            "$COL_START_TIME < ? AND $COL_END_TIME IS NOT NULL",
            arrayOf(beforeMillis.toString()),
        )

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_DRAIN_SAMPLES (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_BATTERY_PERCENT INTEGER NOT NULL,
                $COL_IS_CHARGING INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE $TABLE_CHARGING_SESSIONS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_START_TIME INTEGER NOT NULL,
                $COL_END_TIME INTEGER,
                $COL_START_PERCENT INTEGER NOT NULL,
                $COL_END_PERCENT INTEGER
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX idx_drain_timestamp ON $TABLE_DRAIN_SAMPLES($COL_TIMESTAMP)",
        )
        db.execSQL(
            "CREATE INDEX idx_charge_start ON $TABLE_CHARGING_SESSIONS($COL_START_TIME)",
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // No-op for v1
    }

    companion object {
        private const val DB_NAME = "battery_analytics.db"
        private const val DB_VERSION = 1

        private const val TABLE_DRAIN_SAMPLES = "drain_samples"
        private const val TABLE_CHARGING_SESSIONS = "charging_sessions"

        private const val COL_ID = "id"
        private const val COL_TIMESTAMP = "timestamp_millis"
        private const val COL_BATTERY_PERCENT = "battery_percent"
        private const val COL_IS_CHARGING = "is_charging"
        private const val COL_START_TIME = "start_time_millis"
        private const val COL_END_TIME = "end_time_millis"
        private const val COL_START_PERCENT = "start_percent"
        private const val COL_END_PERCENT = "end_percent"

        private val drainColumns = arrayOf(COL_ID, COL_TIMESTAMP, COL_BATTERY_PERCENT, COL_IS_CHARGING)
        private val chargingColumns = arrayOf(
            COL_ID,
            COL_START_TIME,
            COL_END_TIME,
            COL_START_PERCENT,
            COL_END_PERCENT,
        )

        @Volatile
        private var instance: BatteryAnalyticsStore? = null

        fun getInstance(context: Context): BatteryAnalyticsStore =
            instance ?: synchronized(this) {
                instance ?: BatteryAnalyticsStore(context.applicationContext).also { instance = it }
            }
    }
}

private fun android.database.Cursor.toDrainSample(): DrainSampleEntity =
    DrainSampleEntity(
        id = getLong(0),
        timestampMillis = getLong(1),
        batteryPercent = getInt(2),
        isCharging = getInt(3) == 1,
    )

private fun android.database.Cursor.toChargingSession(): ChargingSessionEntity =
    ChargingSessionEntity(
        id = getLong(0),
        startTimeMillis = getLong(1),
        endTimeMillis = if (isNull(2)) null else getLong(2),
        startPercent = getInt(3),
        endPercent = if (isNull(4)) null else getInt(4),
    )
