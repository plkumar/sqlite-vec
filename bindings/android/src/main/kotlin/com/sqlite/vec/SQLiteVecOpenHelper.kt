package com.sqlite.vec

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * SQLiteOpenHelper extension that automatically loads sqlite-vec when database is opened
 * 
 * Example usage:
 * ```kotlin
 * class AppDatabaseHelper(context: Context) : SQLiteVecOpenHelper(
 *     context = context,
 *     name = "app_database.db",
 *     version = 1
 * ) {
 *     override fun onCreate(db: SQLiteDatabase) {
 *         // sqlite-vec is already loaded at this point
 *         db.execSQL("""
 *             CREATE VIRTUAL TABLE documents USING vec0(
 *                 embedding float[384],
 *                 title TEXT,
 *                 content TEXT
 *             )
 *         """)
 *     }
 *     
 *     override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
 *         // Handle database upgrades
 *     }
 * }
 * ```
 */
abstract class SQLiteVecOpenHelper(
    context: Context,
    name: String?,
    factory: SQLiteDatabase.CursorFactory? = null,
    version: Int,
    errorHandler: DatabaseErrorHandler? = null
) : SQLiteOpenHelper(context, name, factory, version, errorHandler) {
    
    companion object {
        private const val TAG = "SQLiteVecOpenHelper"
    }
    
    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        
        // Automatically load sqlite-vec when database is opened
        try {
            SQLiteVec.ensureLoaded(db)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to load sqlite-vec in onOpen", e)
            throw e
        }
    }
    
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        
        // Enable foreign key constraints and other optimizations
        db.setForeignKeyConstraintsEnabled(true)
        db.execSQL("PRAGMA journal_mode=WAL")
        db.execSQL("PRAGMA synchronous=NORMAL")
        db.execSQL("PRAGMA cache_size=10000")
        db.execSQL("PRAGMA temp_store=MEMORY")
    }
}
