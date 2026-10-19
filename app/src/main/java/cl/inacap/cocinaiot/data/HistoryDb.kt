package cl.inacap.cocinaiot.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** SQLite local: historial limitado a las 150 lecturas más recientes. */
class HistoryDb(context: Context) : SQLiteOpenHelper(context, "cocina_iot.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE readings (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            timestamp INTEGER NOT NULL,
            humidity REAL NOT NULL,
            temperature REAL NOT NULL,
            state TEXT NOT NULL,
            source TEXT NOT NULL
        )""")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    @Synchronized fun insert(reading: SensorReading) {
        val db = writableDatabase
        val row = ContentValues().apply {
            put("timestamp", reading.receivedAt)
            put("humidity", reading.humidity)
            put("temperature", reading.temperature)
            put("state", reading.condition)
            put("source", reading.source)
        }
        db.insertOrThrow("readings", null, row)
        db.execSQL("DELETE FROM readings WHERE id NOT IN (SELECT id FROM readings ORDER BY id DESC LIMIT 150)")
    }

    @Synchronized fun latest(limit: Int = 12): List<ReadingRecord> {
        val result = mutableListOf<ReadingRecord>()
        readableDatabase.query(
            "readings", arrayOf("timestamp", "humidity", "temperature", "state", "source"),
            null, null, null, null, "id DESC", limit.coerceIn(1, 150).toString()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += ReadingRecord(
                    cursor.getLong(0), cursor.getDouble(1), cursor.getDouble(2),
                    cursor.getString(3), cursor.getString(4)
                )
            }
        }
        return result
    }
}
