package com.sqlite.vec

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.util.Log

/**
 * Additional utility functions for working with sqlite-vec vectors
 */
object SQLiteVecUtils {
    
    private const val TAG = "SQLiteVecUtils"
    
    /**
     * Batch insert multiple vectors for better performance
     * 
     * @param database The SQLite database with sqlite-vec loaded
     * @param tableName Name of the vec0 table
     * @param vectorColumn Name of the vector column
     * @param vectors List of pairs containing (id, vector)
     * @throws SQLiteException if batch insert fails
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
                val vectorData = SQLiteVec.serialize(vector)
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
     * Search similar vectors with additional filter conditions
     * 
     * @param database The SQLite database with sqlite-vec loaded
     * @param tableName Name of the vec0 table
     * @param vectorColumn Name of the vector column
     * @param queryVector Vector to search for similarities
     * @param limit Maximum number of results
     * @param whereClause Additional WHERE conditions (optional)
     * @param whereArgs Parameters for the WHERE clause
     * @return List of search results as Maps
     * @throws SQLiteException if search fails
     */
    @JvmStatic
    fun searchSimilarWithFilter(
        database: SQLiteDatabase,
        tableName: String,
        vectorColumn: String,
        queryVector: FloatArray,
        limit: Int = 10,
        whereClause: String? = null,
        whereArgs: Array<String> = emptyArray()
    ): List<Map<String, Any?>> {
        val queryData = SQLiteVec.serialize(queryVector)
        
        var sql = """
            SELECT rowid, distance
            FROM $tableName
            WHERE $vectorColumn MATCH ?
        """.trimIndent()
        
        val allArgs = mutableListOf<Any>()
        allArgs.add(queryData)
        
        if (!whereClause.isNullOrEmpty()) {
            sql += " AND ($whereClause)"
            allArgs.addAll(whereArgs)
        }
        
        sql += " ORDER BY distance LIMIT ?"
        allArgs.add(limit.toString())
        
        val results = mutableListOf<Map<String, Any?>>()
        
        try {
            database.rawQuery(sql, allArgs.map { it.toString() }.toTypedArray()).use { cursor ->
                while (cursor.moveToNext()) {
                    val row = mapOf(
                        "rowid" to cursor.getLong(0),
                        "distance" to cursor.getDouble(1)
                    )
                    results.add(row)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search similar vectors with filter", e)
            throw SQLiteException("Failed to search similar vectors with filter", e)
        }
        
        return results
    }
    
    /**
     * Vector arithmetic - add two vectors using sqlite-vec
     * 
     * @param database The SQLite database with sqlite-vec loaded
     * @param vector1 First vector as FloatArray
     * @param vector2 Second vector as FloatArray
     * @return Resulting vector as FloatArray
     * @throws SQLiteException if operation fails
     */
    @JvmStatic
    fun add(
        database: SQLiteDatabase,
        vector1: FloatArray,
        vector2: FloatArray
    ): FloatArray {
        val data1 = SQLiteVec.serialize(vector1)
        val data2 = SQLiteVec.serialize(vector2)
        
        return database.rawQuery("SELECT vec_add(?, ?)", arrayOf(data1, data2)).use { cursor ->
            if (cursor.moveToFirst()) {
                val resultData = cursor.getBlob(0)
                SQLiteVec.deserializeFloat32(resultData)
            } else {
                throw SQLiteException("Failed to add vectors")
            }
        }
    }
    
    /**
     * Vector arithmetic - subtract two vectors using sqlite-vec
     * 
     * @param database The SQLite database with sqlite-vec loaded
     * @param vector1 First vector as FloatArray
     * @param vector2 Second vector as FloatArray
     * @return Resulting vector as FloatArray
     * @throws SQLiteException if operation fails
     */
    @JvmStatic
    fun subtract(
        database: SQLiteDatabase,
        vector1: FloatArray,
        vector2: FloatArray
    ): FloatArray {
        val data1 = SQLiteVec.serialize(vector1)
        val data2 = SQLiteVec.serialize(vector2)
        
        return database.rawQuery("SELECT vec_sub(?, ?)", arrayOf(data1, data2)).use { cursor ->
            if (cursor.moveToFirst()) {
                val resultData = cursor.getBlob(0)
                SQLiteVec.deserializeFloat32(resultData)
            } else {
                throw SQLiteException("Failed to subtract vectors")
            }
        }
    }
    
    /**
     * Calculate dot product between two vectors using sqlite-vec
     * 
     * @param database The SQLite database with sqlite-vec loaded
     * @param vector1 First vector as FloatArray
     * @param vector2 Second vector as FloatArray
     * @return Dot product result
     * @throws SQLiteException if operation fails
     */
    @JvmStatic
    fun dotProduct(
        database: SQLiteDatabase,
        vector1: FloatArray,
        vector2: FloatArray
    ): Double {
        val data1 = SQLiteVec.serialize(vector1)
        val data2 = SQLiteVec.serialize(vector2)
        
        return database.rawQuery("SELECT vec_distance_cosine(?, ?)", arrayOf(data1, data2)).use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getDouble(0)
            } else {
                throw SQLiteException("Failed to calculate dot product")
            }
        }
    }
    
    /**
     * Convert a vector to JSON representation using sqlite-vec
     * 
     * @param database The SQLite database with sqlite-vec loaded
     * @param vector Vector as FloatArray
     * @return JSON string representation of the vector
     * @throws SQLiteException if operation fails
     */
    @JvmStatic
    fun toJSON(database: SQLiteDatabase, vector: FloatArray): String {
        val data = SQLiteVec.serialize(vector)
        
        return database.rawQuery("SELECT vec_to_json(?)", arrayOf(data)).use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(0)
            } else {
                throw SQLiteException("Failed to convert vector to JSON")
            }
        }
    }
    
    /**
     * Convert JSON string to vector using sqlite-vec
     * 
     * @param database The SQLite database with sqlite-vec loaded
     * @param json JSON string representation of the vector
     * @return Vector as FloatArray
     * @throws SQLiteException if operation fails
     */
    @JvmStatic
    fun fromJSON(database: SQLiteDatabase, json: String): FloatArray {
        return database.rawQuery("SELECT vec_from_json(?)", arrayOf(json)).use { cursor ->
            if (cursor.moveToFirst()) {
                val resultData = cursor.getBlob(0)
                SQLiteVec.deserializeFloat32(resultData)
            } else {
                throw SQLiteException("Failed to convert JSON to vector")
            }
        }
    }
    
    /**
     * Get statistics about a vector table
     * 
     * @param database The SQLite database with sqlite-vec loaded
     * @param tableName Name of the vec0 table
     * @return Map containing table statistics
     * @throws SQLiteException if operation fails
     */
    @JvmStatic
    fun getTableStats(database: SQLiteDatabase, tableName: String): Map<String, Any> {
        val stats = mutableMapOf<String, Any>()
        
        try {
            // Get row count
            database.rawQuery("SELECT COUNT(*) FROM $tableName", null).use { cursor ->
                if (cursor.moveToFirst()) {
                    stats["row_count"] = cursor.getLong(0)
                }
            }
            
            // Get table info
            database.rawQuery("PRAGMA table_info($tableName)", null).use { cursor ->
                val columns = mutableListOf<Map<String, Any>>()
                while (cursor.moveToNext()) {
                    columns.add(mapOf(
                        "name" to cursor.getString(1),
                        "type" to cursor.getString(2),
                        "notnull" to cursor.getInt(3) == 1,
                        "pk" to cursor.getInt(5) == 1
                    ))
                }
                stats["columns"] = columns
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get table stats", e)
            throw SQLiteException("Failed to get table stats", e)
        }
        
        return stats
    }
}
