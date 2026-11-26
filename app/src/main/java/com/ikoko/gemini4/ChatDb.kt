package com.ikoko.gemini4

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class ChatRow(val role: String, val content: String, val createdAt: Long)
data class ChatSession(val sessionId: String, val lastMessage: String, val updatedAt: Long, val messageCount: Int)

class ChatDb(context: Context) : SQLiteOpenHelper(context, "gikoko_gemini.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE messages (id INTEGER PRIMARY KEY AUTOINCREMENT, session_id TEXT NOT NULL, role TEXT NOT NULL, content TEXT NOT NULL, created_at INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX idx_messages_session ON messages(session_id, id)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    fun add(session: String, role: String, content: String) {
        val values = ContentValues().apply {
            put("session_id", session); put("role", role); put("content", content)
            put("created_at", System.currentTimeMillis())
        }
        writableDatabase.insert("messages", null, values)
    }
    fun recent(session: String, limit: Int = 30): List<ChatRow> {
        val rows = mutableListOf<ChatRow>()
        readableDatabase.rawQuery("SELECT role, content, created_at FROM messages WHERE session_id=? ORDER BY id DESC LIMIT ?", arrayOf(session, limit.toString())).use { c ->
            while (c.moveToNext()) rows.add(ChatRow(c.getString(0), c.getString(1), c.getLong(2)))
        }
        return rows.asReversed()
    }
    fun sessions(limit: Int = 100): List<ChatSession> {
        val rows = mutableListOf<ChatSession>()
        readableDatabase.rawQuery(
            "SELECT m.session_id, m.content, m.created_at, (SELECT COUNT(*) FROM messages c WHERE c.session_id=m.session_id) FROM messages m WHERE m.id=(SELECT MAX(x.id) FROM messages x WHERE x.session_id=m.session_id) ORDER BY m.created_at DESC LIMIT ?",
            arrayOf(limit.toString())
        ).use { c ->
            while (c.moveToNext()) rows.add(ChatSession(c.getString(0), c.getString(1), c.getLong(2), c.getInt(3)))
        }
        return rows
    }
    fun history(limit: Int = 100): List<ChatRow> {
        val rows = mutableListOf<ChatRow>()
        readableDatabase.rawQuery("SELECT role, content, created_at FROM messages ORDER BY id DESC LIMIT ?", arrayOf(limit.toString())).use { c ->
            while (c.moveToNext()) rows.add(ChatRow(c.getString(0), c.getString(1), c.getLong(2)))
        }
        return rows
    }
}
