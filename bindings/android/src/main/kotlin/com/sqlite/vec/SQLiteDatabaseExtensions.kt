package com.sqlite.vec

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException

/**
 * Convenience extensions for SQLiteDatabase to make working with sqlite-vec easier
 * Similar to Swift bindings approach
 */

/**
 * Load the sqlite-vec extension into this database
 * @throws SQLiteException if loading fails
 */
fun SQLiteDatabase.loadSQLiteVec() {
    SQLiteVec.load(this)
}

/**
 * Check if sqlite-vec is available in this database
 * @return true if sqlite-vec is loaded and available
 */
fun SQLiteDatabase.isSQLiteVecAvailable(): Boolean {
    return SQLiteVec.isAvailable(this)
}

/**
 * Load sqlite-vec if not already loaded
 * @throws SQLiteException if loading fails
 */
fun SQLiteDatabase.ensureSQLiteVecLoaded() {
    SQLiteVec.ensureLoaded(this)
}

/**
 * Get the sqlite-vec version from this database
 * @return Version string
 * @throws SQLiteException if query fails
 */
fun SQLiteDatabase.sqliteVecVersion(): String {
    return SQLiteVec.version(this)
}

/**
 * Create a vec0 virtual table in this database
 * @param tableName Name of the table to create
 * @param vectorColumn Name and definition of the vector column (e.g., "embedding float[384]")
 * @param additionalColumns Additional column definitions
 * @throws SQLiteException if table creation fails
 */
fun SQLiteDatabase.createVectorTable(
    tableName: String,
    vectorColumn: String,
    vararg additionalColumns: String
) {
    SQLiteVec.createVectorTable(this, tableName, vectorColumn, *additionalColumns)
}

/**
 * Search for similar vectors in a vec0 table
 * @param tableName Name of the vec0 table
 * @param vectorColumn Name of the vector column
 * @param queryVector Vector to search for similarities
 * @param limit Maximum number of results (default: 10)
 * @param additionalColumns Additional columns to select
 * @return List of search results as Maps
 * @throws SQLiteException if search fails
 */
fun SQLiteDatabase.searchSimilar(
    tableName: String,
    vectorColumn: String,
    queryVector: FloatArray,
    limit: Int = 10,
    vararg additionalColumns: String
): List<Map<String, Any?>> {
    return SQLiteVec.searchSimilar(this, tableName, vectorColumn, queryVector, limit, *additionalColumns)
}

/**
 * Calculate the distance between two vectors
 * @param vector1 First vector as FloatArray
 * @param vector2 Second vector as FloatArray
 * @param metric Distance metric ("cosine" or "l2", default: "cosine")
 * @return The calculated distance
 * @throws SQLiteException if calculation fails
 */
fun SQLiteDatabase.vectorDistance(
    vector1: FloatArray,
    vector2: FloatArray,
    metric: String = "cosine"
): Double {
    return SQLiteVec.distance(this, vector1, vector2, metric)
}

/**
 * Get the length (number of dimensions) of a vector
 * @param vector Vector as FloatArray
 * @return The number of dimensions
 * @throws SQLiteException if query fails
 */
fun SQLiteDatabase.vectorLength(vector: FloatArray): Int {
    return SQLiteVec.length(this, vector)
}

/**
 * Normalize a vector
 * @param vector Vector as FloatArray to normalize
 * @return Normalized vector as FloatArray
 * @throws SQLiteException if normalization fails
 */
fun SQLiteDatabase.normalizeVector(vector: FloatArray): FloatArray {
    return SQLiteVec.normalize(this, vector)
}

/**
 * Convert a vector to JSON representation
 * @param vector Vector as FloatArray
 * @return JSON string representation of the vector
 * @throws SQLiteException if query fails
 */
fun SQLiteDatabase.vectorToJSON(vector: FloatArray): String {
    return SQLiteVec.toJSON(this, vector)
}

/**
 * Insert multiple vectors in a batch operation for better performance
 * @param tableName Name of the vec0 table
 * @param vectorColumn Name of the vector column
 * @param vectors List of pairs containing (id, vector)
 * @throws SQLiteException if the operation fails
 */
fun SQLiteDatabase.insertVectorsBatch(
    tableName: String,
    vectorColumn: String,
    vectors: List<Pair<Long, FloatArray>>
) {
    SQLiteVec.insertVectorsBatch(this, tableName, vectorColumn, vectors)
}
