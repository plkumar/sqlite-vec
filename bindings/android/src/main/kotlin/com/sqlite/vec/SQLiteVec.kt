package com.sqlite.vec

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Android bindings for sqlite-vec - Vector search for SQLite
 * 
 * This class provides Kotlin/Java bindings for the sqlite-vec SQLite extension,
 * enabling vector search capabilities in Android SQLite databases.
 * 
 * Features:
 * - Fast vector search powered by sqlite-vec
 * - Support for Float32, Float64, and Int8 vectors
 * - Cross-platform compatibility across all Android architectures
 * - Easy integration with existing Android SQLite code
 * - Comprehensive error handling and logging
 * 
 * Example usage:
 * ```kotlin
 * val db = SQLiteDatabase.openDatabase(":memory:", null, SQLiteDatabase.OPEN_READWRITE)
 * SQLiteVec.load(db)
 * 
 * val version = SQLiteVec.version(db)
 * println("sqlite-vec version: $version")
 * 
 * // Create vector table
 * SQLiteVec.createVectorTable(db, "documents", "embedding float[384]")
 * 
 * // Insert vectors
 * val embedding = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f)
 * val vectorData = SQLiteVec.serialize(embedding)
 * db.execSQL("INSERT INTO documents(rowid, embedding) VALUES (?, ?)", arrayOf(1, vectorData))
 * 
 * // Search similar vectors
 * val queryVector = floatArrayOf(0.2f, 0.2f, 0.3f, 0.3f)
 * val results = SQLiteVec.searchSimilar(db, "documents", "embedding", queryVector, 5)
 * ```
 */
class SQLiteVec {
    
    companion object {
        private const val TAG = "SQLiteVec"
        private var isLoaded = false
        private var autoLoadEnabled = false
        
        init {
            try {
                System.loadLibrary("sqlite-vec")
                isLoaded = true
                Log.i(TAG, "sqlite-vec native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load sqlite-vec native library", e)
                isLoaded = false
            }
        }
        
        // Native method declarations
        @JvmStatic
        private external fun nativeGetVersion(): String
        
        @JvmStatic
        private external fun nativeInit(dbHandle: Long): Int
        
        @JvmStatic
        private external fun nativeSerializeFloat32(vector: FloatArray): ByteArray?
        
        @JvmStatic
        private external fun nativeDeserializeFloat32(data: ByteArray): FloatArray?
        
        @JvmStatic
        private external fun nativeSerializeInt8(vector: ByteArray): ByteArray?
        
        @JvmStatic
        private external fun nativeDistance(dbHandle: Long, vector1: ByteArray, vector2: ByteArray, metric: String): Double
        
        @JvmStatic
        private external fun nativeVectorLength(dbHandle: Long, vector: ByteArray): Int
        
        @JvmStatic
        private external fun nativeNormalize(dbHandle: Long, vector: ByteArray): ByteArray?
        
        /**
         * Load sqlite-vec extension into a SQLite database
         * 
         * @param database The SQLite database to load the extension into
         * @throws SQLiteException if loading fails
         */
        @JvmStatic
        fun load(database: SQLiteDatabase) {
            if (!isLoaded) {
                throw SQLiteException("sqlite-vec native library not loaded")
            }
            
            try {
                val handle = getDatabaseHandle(database)
                val result = nativeInit(handle)
                if (result != 0) { // SQLITE_OK = 0
                    throw SQLiteException("Failed to initialize sqlite-vec (error code: $result)")
                }
                Log.i(TAG, "sqlite-vec extension loaded successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load sqlite-vec extension", e)
                throw SQLiteException("Failed to load sqlite-vec extension: ${e.message}", e)
            }
        }
        
        /**
         * Get the version of the sqlite-vec extension
         * 
         * @return Version string (e.g., "v0.1.7-alpha.2")
         */
        @JvmStatic
        fun getVersion(): String {
            if (!isLoaded) {
                throw SQLiteException("sqlite-vec native library not loaded")
            }
            return nativeGetVersion()
        }
        
        /**
         * Get the version of sqlite-vec from a loaded database
         * 
         * @param database The SQLite database with sqlite-vec loaded
         * @return Version string from the database
         * @throws SQLiteException if query fails
         */
        @JvmStatic
        fun version(database: SQLiteDatabase): String {
            return database.rawQuery("SELECT vec_version()", null).use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else {
                    throw SQLiteException("Failed to get sqlite-vec version")
                }
            }
        }
        
        /**
         * Check if sqlite-vec is available in the database
         * 
         * @param database The SQLite database to check
         * @return true if sqlite-vec is loaded and available
         */
        @JvmStatic
        fun isAvailable(database: SQLiteDatabase): Boolean {
            return try {
                version(database)
                true
            } catch (e: Exception) {
                false
            }
        }
        
        /**
         * Load sqlite-vec if not already loaded
         * 
         * @param database The SQLite database
         * @throws SQLiteException if loading fails
         */
        @JvmStatic
        fun ensureLoaded(database: SQLiteDatabase) {
            if (!isAvailable(database)) {
                load(database)
            }
        }
        
        /**
         * Enable auto-loading of sqlite-vec for future database connections
         * Note: This is a placeholder - actual implementation would require
         * hooking into SQLiteOpenHelper or database factory
         */
        @JvmStatic
        fun autoLoad() {
            autoLoadEnabled = true
            Log.i(TAG, "sqlite-vec auto-loading enabled")
        }
        
        /**
         * Disable auto-loading of sqlite-vec
         */
        @JvmStatic
        fun cancelAutoLoad() {
            autoLoadEnabled = false
            Log.i(TAG, "sqlite-vec auto-loading disabled")
        }
        
        /**
         * Serialize a Float32 array into raw bytes format that sqlite-vec expects
         * 
         * @param vector Array of Float values
         * @return ByteArray containing the serialized vector
         */
        @JvmStatic
        fun serialize(vector: FloatArray): ByteArray {
            if (!isLoaded) {
                throw SQLiteException("sqlite-vec native library not loaded")
            }
            return nativeSerializeFloat32(vector) 
                ?: throw SQLiteException("Failed to serialize Float32 vector")
        }
        
        /**
         * Serialize a Double array into raw bytes format
         * 
         * @param vector Array of Double values
         * @return ByteArray containing the serialized vector
         */
        @JvmStatic
        fun serialize(vector: DoubleArray): ByteArray {
            val buffer = ByteBuffer.allocate(vector.size * 8)
            buffer.order(ByteOrder.nativeOrder())
            for (value in vector) {
                buffer.putDouble(value)
            }
            return buffer.array()
        }
        
        /**
         * Serialize an Int8 array (ByteArray) into raw bytes format
         * 
         * @param vector Array of Byte values
         * @return ByteArray containing the serialized vector
         */
        @JvmStatic
        fun serialize(vector: ByteArray): ByteArray {
            if (!isLoaded) {
                throw SQLiteException("sqlite-vec native library not loaded")
            }
            return nativeSerializeInt8(vector) 
                ?: throw SQLiteException("Failed to serialize Int8 vector")
        }
        
        /**
         * Serialize a Float32 array (Go bindings compatibility)
         * 
         * @param vector Array of Float values
         * @return ByteArray containing the serialized vector
         */
        @JvmStatic
        fun serializeFloat32(vector: FloatArray): ByteArray = serialize(vector)
        
        /**
         * Serialize an Int8 array (Go bindings compatibility)
         * 
         * @param vector Array of Byte values
         * @return ByteArray containing the serialized vector
         */
        @JvmStatic
        fun serializeInt8(vector: ByteArray): ByteArray = serialize(vector)
        
        /**
         * Deserialize raw bytes into a Float32 array
         * 
         * @param data Raw bytes from sqlite-vec
         * @return FloatArray of deserialized values
         */
        @JvmStatic
        fun deserializeFloat32(data: ByteArray): FloatArray {
            if (!isLoaded) {
                throw SQLiteException("sqlite-vec native library not loaded")
            }
            return nativeDeserializeFloat32(data) 
                ?: throw SQLiteException("Failed to deserialize Float32 vector")
        }
        
        /**
         * Deserialize raw bytes into an Int8 array
         * 
         * @param data Raw bytes from sqlite-vec
         * @return ByteArray of deserialized values
         */
        @JvmStatic
        fun deserializeInt8(data: ByteArray): ByteArray {
            // For Int8, no conversion needed
            return data.clone()
        }
        
        /**
         * Calculate the distance between two vectors using sqlite-vec
         * 
         * @param database The SQLite database with sqlite-vec loaded
         * @param vector1 First vector as FloatArray
         * @param vector2 Second vector as FloatArray
         * @param metric Distance metric ("cosine" or "l2", default: "cosine")
         * @return The calculated distance
         * @throws SQLiteException if calculation fails
         */
        @JvmStatic
        fun distance(
            database: SQLiteDatabase,
            vector1: FloatArray,
            vector2: FloatArray,
            metric: String = "cosine"
        ): Double {
            if (!isLoaded) {
                throw SQLiteException("sqlite-vec native library not loaded")
            }
            
            val data1 = serialize(vector1)
            val data2 = serialize(vector2)
            val handle = getDatabaseHandle(database)
            
            val result = nativeDistance(handle, data1, data2, metric)
            if (result < 0) {
                throw SQLiteException("Failed to calculate vector distance")
            }
            return result
        }
        
        /**
         * Get the length (number of dimensions) of a vector
         * 
         * @param database The SQLite database with sqlite-vec loaded
         * @param vector Vector as FloatArray
         * @return The number of dimensions
         * @throws SQLiteException if query fails
         */
        @JvmStatic
        fun length(database: SQLiteDatabase, vector: FloatArray): Int {
            if (!isLoaded) {
                throw SQLiteException("sqlite-vec native library not loaded")
            }
            
            val data = serialize(vector)
            val handle = getDatabaseHandle(database)
            
            val result = nativeVectorLength(handle, data)
            if (result < 0) {
                throw SQLiteException("Failed to get vector length")
            }
            return result
        }
        
        /**
         * Normalize a vector using sqlite-vec
         * 
         * @param database The SQLite database with sqlite-vec loaded
         * @param vector Vector as FloatArray to normalize
         * @return Normalized vector as FloatArray
         * @throws SQLiteException if normalization fails
         */
        @JvmStatic
        fun normalize(database: SQLiteDatabase, vector: FloatArray): FloatArray {
            if (!isLoaded) {
                throw SQLiteException("sqlite-vec native library not loaded")
            }
            
            val data = serialize(vector)
            val handle = getDatabaseHandle(database)
            
            val resultData = nativeNormalize(handle, data)
                ?: throw SQLiteException("Failed to normalize vector")
                
            return deserializeFloat32(resultData)
        }
        
        /**
         * Create a vec0 virtual table
         * 
         * @param database The SQLite database with sqlite-vec loaded
         * @param tableName Name of the table to create
         * @param vectorColumn Name and definition of the vector column (e.g., "embedding float[384]")
         * @param additionalColumns Additional column definitions
         * @throws SQLiteException if table creation fails
         */
        @JvmStatic
        fun createVectorTable(
            database: SQLiteDatabase,
            tableName: String,
            vectorColumn: String,
            vararg additionalColumns: String
        ) {
            val columns = mutableListOf(vectorColumn)
            columns.addAll(additionalColumns)
            val columnDefinitions = columns.joinToString(", ")
            
            val sql = "CREATE VIRTUAL TABLE $tableName USING vec0($columnDefinitions)"
            try {
                database.execSQL(sql)
                Log.d(TAG, "Created vector table: $tableName")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create vector table: $tableName", e)
                throw SQLiteException("Failed to create vector table: $tableName", e)
            }
        }
        
        /**
         * Search for similar vectors in a vec0 table
         * 
         * @param database The SQLite database with sqlite-vec loaded
         * @param tableName Name of the vec0 table
         * @param vectorColumn Name of the vector column
         * @param queryVector Vector to search for similarities
         * @param limit Maximum number of results (default: 10)
         * @param additionalColumns Additional columns to select
         * @return List of search results as Maps
         * @throws SQLiteException if search fails
         */
        @JvmStatic
        fun searchSimilar(
            database: SQLiteDatabase,
            tableName: String,
            vectorColumn: String,
            queryVector: FloatArray,
            limit: Int = 10,
            vararg additionalColumns: String
        ): List<Map<String, Any?>> {
            val queryData = serialize(queryVector)
            
            val selectColumns = mutableListOf("rowid", "distance")
            selectColumns.addAll(additionalColumns)
            val selectClause = selectColumns.joinToString(", ")
            
            val sql = """
                SELECT $selectClause
                FROM $tableName
                WHERE $vectorColumn MATCH ?
                ORDER BY distance
                LIMIT ?
            """.trimIndent()
            
            val results = mutableListOf<Map<String, Any?>>()
            
            try {
                database.rawQuery(sql, arrayOf(queryData, limit.toString())).use { cursor ->
                    while (cursor.moveToNext()) {
                        val row = mutableMapOf<String, Any?>()
                        for (i in 0 until cursor.columnCount) {
                            val columnName = cursor.getColumnName(i)
                            val value = when (cursor.getType(i)) {
                                Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i)
                                Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(i)
                                Cursor.FIELD_TYPE_STRING -> cursor.getString(i)
                                Cursor.FIELD_TYPE_BLOB -> cursor.getBlob(i)
                                else -> null
                            }
                            row[columnName] = value
                        }
                        results.add(row)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to search similar vectors", e)
                throw SQLiteException("Failed to search similar vectors", e)
            }
            
            return results
        }
        
        /**
         * Convert a vector to JSON representation using sqlite-vec
         * 
         * @param database The SQLite database with sqlite-vec loaded
         * @param vector Vector as FloatArray
         * @return JSON string representation of the vector
         * @throws SQLiteException if query fails
         */
        @JvmStatic
        fun toJSON(database: SQLiteDatabase, vector: FloatArray): String {
            val data = serialize(vector)
            
            return database.rawQuery("SELECT vec_to_json(?)", arrayOf(data)).use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else {
                    throw SQLiteException("Failed to convert vector to JSON")
                }
            }
        }
        
        /**
         * Insert multiple vectors in a batch operation for better performance
         * Similar to Swift bindings pattern for bulk inserts
         * 
         * @param database The SQLite database with sqlite-vec loaded
         * @param tableName Name of the vec0 table
         * @param vectorColumn Name of the vector column
         * @param vectors List of pairs containing (id, vector)
         * @throws SQLiteException if the operation fails
         */
        @JvmStatic
        fun insertVectorsBatch(
            database: SQLiteDatabase,
            tableName: String,
            vectorColumn: String,
            vectors: List<Pair<Long, FloatArray>>
        ) {
            if (vectors.isEmpty()) return
            
            val sql = "INSERT INTO $tableName(rowid, $vectorColumn) VALUES (?, ?)"
            
            try {
                database.beginTransaction()
                
                for ((id, vector) in vectors) {
                    val vectorData = serialize(vector)
                    database.execSQL(sql, arrayOf(id, vectorData))
                }
                
                database.setTransactionSuccessful()
                Log.d(TAG, "Successfully inserted ${vectors.size} vectors")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to insert vectors batch", e)
                throw SQLiteException("Failed to insert vectors batch", e)
            } finally {
                database.endTransaction()
            }
        }
        
        /**
         * Helper function to get database handle from SQLiteDatabase
         * This uses reflection as Android doesn't expose the native handle directly
         */
        private fun getDatabaseHandle(database: SQLiteDatabase): Long {
            try {
                // Try different field names based on Android version
                val handleFields = listOf("mNativeHandle", "mConnectionPtr", "mConnection")
                
                for (fieldName in handleFields) {
                    try {
                        val field = database.javaClass.getDeclaredField(fieldName)
                        field.isAccessible = true
                        val handle = field.getLong(database)
                        if (handle != 0L) {
                            return handle
                        }
                    } catch (e: NoSuchFieldException) {
                        continue
                    }
                }
                
                throw SQLiteException("Could not access database handle")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get database handle", e)
                throw SQLiteException("Failed to get database handle", e)
            }
        }
    }
}
