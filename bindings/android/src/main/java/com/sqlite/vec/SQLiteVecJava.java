package com.sqlite.vec;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Java compatibility wrapper for SQLiteVec
 * 
 * This class provides a Java-friendly API for the sqlite-vec extension,
 * wrapping the Kotlin implementation with Java-compatible method signatures
 * and return types.
 * 
 * Example usage:
 * ```java
 * SQLiteDatabase db = SQLiteDatabase.openDatabase(":memory:", null, SQLiteDatabase.OPEN_READWRITE);
 * SQLiteVecJava.load(db);
 * 
 * String version = SQLiteVecJava.version(db);
 * System.out.println("sqlite-vec version: " + version);
 * 
 * // Create vector table
 * SQLiteVecJava.createVectorTable(db, "documents", "embedding float[384]");
 * 
 * // Insert vectors
 * float[] embedding = {0.1f, 0.2f, 0.3f, 0.4f};
 * byte[] vectorData = SQLiteVecJava.serialize(embedding);
 * db.execSQL("INSERT INTO documents(rowid, embedding) VALUES (?, ?)", new Object[]{1, vectorData});
 * 
 * // Search similar vectors
 * float[] queryVector = {0.2f, 0.2f, 0.3f, 0.3f};
 * List<Map<String, Object>> results = SQLiteVecJava.searchSimilar(db, "documents", "embedding", queryVector, 5);
 * ```
 */
public class SQLiteVecJava {
    private static final String TAG = "SQLiteVecJava";
    
    /**
     * Load sqlite-vec extension into a SQLite database
     * 
     * @param database The SQLite database to load the extension into
     * @throws SQLiteException if loading fails
     */
    public static void load(SQLiteDatabase database) throws SQLiteException {
        SQLiteVec.load(database);
    }
    
    /**
     * Get the version of the sqlite-vec extension
     * 
     * @return Version string (e.g., "v0.1.7-alpha.2")
     */
    public static String getVersion() {
        return SQLiteVec.getVersion();
    }
    
    /**
     * Get the version of sqlite-vec from a loaded database
     * 
     * @param database The SQLite database with sqlite-vec loaded
     * @return Version string from the database
     * @throws SQLiteException if query fails
     */
    public static String version(SQLiteDatabase database) throws SQLiteException {
        return SQLiteVec.version(database);
    }
    
    /**
     * Check if sqlite-vec is available in the database
     * 
     * @param database The SQLite database to check
     * @return true if sqlite-vec is loaded and available
     */
    public static boolean isAvailable(SQLiteDatabase database) {
        return SQLiteVec.isAvailable(database);
    }
    
    /**
     * Load sqlite-vec if not already loaded
     * 
     * @param database The SQLite database
     * @throws SQLiteException if loading fails
     */
    public static void ensureLoaded(SQLiteDatabase database) throws SQLiteException {
        SQLiteVec.ensureLoaded(database);
    }
    
    /**
     * Enable auto-loading of sqlite-vec for future database connections
     */
    public static void autoLoad() {
        SQLiteVec.autoLoad();
    }
    
    /**
     * Disable auto-loading of sqlite-vec
     */
    public static void cancelAutoLoad() {
        SQLiteVec.cancelAutoLoad();
    }
    
    /**
     * Serialize a float array into raw bytes format that sqlite-vec expects
     * 
     * @param vector Array of float values
     * @return byte array containing the serialized vector
     */
    public static byte[] serialize(float[] vector) {
        return SQLiteVec.serialize(vector);
    }
    
    /**
     * Serialize a double array into raw bytes format
     * 
     * @param vector Array of double values
     * @return byte array containing the serialized vector
     */
    public static byte[] serialize(double[] vector) {
        return SQLiteVec.serialize(vector);
    }
    
    /**
     * Serialize a byte array into raw bytes format
     * 
     * @param vector Array of byte values
     * @return byte array containing the serialized vector
     */
    public static byte[] serialize(byte[] vector) {
        return SQLiteVec.serialize(vector);
    }
    
    /**
     * Serialize a float array (Go bindings compatibility)
     * 
     * @param vector Array of float values
     * @return byte array containing the serialized vector
     */
    public static byte[] serializeFloat32(float[] vector) {
        return SQLiteVec.serializeFloat32(vector);
    }
    
    /**
     * Serialize a byte array (Go bindings compatibility)
     * 
     * @param vector Array of byte values
     * @return byte array containing the serialized vector
     */
    public static byte[] serializeInt8(byte[] vector) {
        return SQLiteVec.serializeInt8(vector);
    }
    
    /**
     * Deserialize raw bytes into a float array
     * 
     * @param data Raw bytes from sqlite-vec
     * @return float array of deserialized values
     */
    public static float[] deserializeFloat32(byte[] data) {
        return SQLiteVec.deserializeFloat32(data);
    }
    
    /**
     * Deserialize raw bytes into a byte array
     * 
     * @param data Raw bytes from sqlite-vec
     * @return byte array of deserialized values
     */
    public static byte[] deserializeInt8(byte[] data) {
        return SQLiteVec.deserializeInt8(data);
    }
    
    /**
     * Calculate the distance between two vectors using sqlite-vec
     * 
     * @param database The SQLite database with sqlite-vec loaded
     * @param vector1 First vector as float array
     * @param vector2 Second vector as float array
     * @param metric Distance metric ("cosine" or "l2", default: "cosine")
     * @return The calculated distance
     * @throws SQLiteException if calculation fails
     */
    public static double distance(SQLiteDatabase database, float[] vector1, float[] vector2, String metric) throws SQLiteException {
        return SQLiteVec.distance(database, vector1, vector2, metric);
    }
    
    /**
     * Calculate the distance between two vectors using cosine metric
     * 
     * @param database The SQLite database with sqlite-vec loaded
     * @param vector1 First vector as float array
     * @param vector2 Second vector as float array
     * @return The calculated distance
     * @throws SQLiteException if calculation fails
     */
    public static double distance(SQLiteDatabase database, float[] vector1, float[] vector2) throws SQLiteException {
        return SQLiteVec.distance(database, vector1, vector2, "cosine");
    }
    
    /**
     * Get the length (number of dimensions) of a vector
     * 
     * @param database The SQLite database with sqlite-vec loaded
     * @param vector Vector as float array
     * @return The number of dimensions
     * @throws SQLiteException if query fails
     */
    public static int length(SQLiteDatabase database, float[] vector) throws SQLiteException {
        return SQLiteVec.length(database, vector);
    }
    
    /**
     * Normalize a vector using sqlite-vec
     * 
     * @param database The SQLite database with sqlite-vec loaded
     * @param vector Vector as float array to normalize
     * @return Normalized vector as float array
     * @throws SQLiteException if normalization fails
     */
    public static float[] normalize(SQLiteDatabase database, float[] vector) throws SQLiteException {
        return SQLiteVec.normalize(database, vector);
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
    public static void createVectorTable(SQLiteDatabase database, String tableName, String vectorColumn, String... additionalColumns) throws SQLiteException {
        SQLiteVec.createVectorTable(database, tableName, vectorColumn, additionalColumns);
    }
    
    /**
     * Search for similar vectors in a vec0 table
     * 
     * @param database The SQLite database with sqlite-vec loaded
     * @param tableName Name of the vec0 table
     * @param vectorColumn Name of the vector column
     * @param queryVector Vector to search for similarities
     * @param limit Maximum number of results
     * @param additionalColumns Additional columns to select
     * @return List of search results as Maps
     * @throws SQLiteException if search fails
     */
    public static List<Map<String, Object>> searchSimilar(SQLiteDatabase database, String tableName, String vectorColumn, float[] queryVector, int limit, String... additionalColumns) throws SQLiteException {
        List<Map<String, Object>> kotlinResults = SQLiteVec.searchSimilar(database, tableName, vectorColumn, queryVector, limit, additionalColumns);
        List<Map<String, Object>> javaResults = new ArrayList<>();
        
        for (Map<String, Object> kotlinResult : kotlinResults) {
            Map<String, Object> javaResult = new HashMap<>(kotlinResult);
            javaResults.add(javaResult);
        }
        
        return javaResults;
    }
    
    /**
     * Search for similar vectors in a vec0 table with default limit of 10
     * 
     * @param database The SQLite database with sqlite-vec loaded
     * @param tableName Name of the vec0 table
     * @param vectorColumn Name of the vector column
     * @param queryVector Vector to search for similarities
     * @return List of search results as Maps
     * @throws SQLiteException if search fails
     */
    public static List<Map<String, Object>> searchSimilar(SQLiteDatabase database, String tableName, String vectorColumn, float[] queryVector) throws SQLiteException {
        return searchSimilar(database, tableName, vectorColumn, queryVector, 10);
    }
    
    // Utility methods (delegating to SQLiteVecUtils)
    
    /**
     * Batch insert multiple vectors for better performance
     * 
     * @param database The SQLite database with sqlite-vec loaded
     * @param tableName Name of the vec0 table
     * @param vectorColumn Name of the vector column
     * @param vectors List of VectorPair objects containing (id, vector) pairs
     * @throws SQLiteException if batch insert fails
     */
    public static void insertVectorsBatch(SQLiteDatabase database, String tableName, String vectorColumn, List<VectorPair> vectors) throws SQLiteException {
        List<kotlin.Pair<Long, float[]>> kotlinPairs = new ArrayList<>();
        for (VectorPair pair : vectors) {
            kotlinPairs.add(new kotlin.Pair<>(pair.getId(), pair.getVector()));
        }
        SQLiteVecUtils.insertVectorsBatch(database, tableName, vectorColumn, kotlinPairs);
    }
    
    /**
     * Vector arithmetic - add two vectors using sqlite-vec
     * 
     * @param database The SQLite database with sqlite-vec loaded
     * @param vector1 First vector as float array
     * @param vector2 Second vector as float array
     * @return Resulting vector as float array
     * @throws SQLiteException if operation fails
     */
    public static float[] add(SQLiteDatabase database, float[] vector1, float[] vector2) throws SQLiteException {
        return SQLiteVecUtils.add(database, vector1, vector2);
    }
    
    /**
     * Vector arithmetic - subtract two vectors using sqlite-vec
     * 
     * @param database The SQLite database with sqlite-vec loaded
     * @param vector1 First vector as float array
     * @param vector2 Second vector as float array
     * @return Resulting vector as float array
     * @throws SQLiteException if operation fails
     */
    public static float[] subtract(SQLiteDatabase database, float[] vector1, float[] vector2) throws SQLiteException {
        return SQLiteVecUtils.subtract(database, vector1, vector2);
    }
    
    /**
     * Calculate dot product between two vectors using sqlite-vec
     * 
     * @param database The SQLite database with sqlite-vec loaded
     * @param vector1 First vector as float array
     * @param vector2 Second vector as float array
     * @return Dot product result
     * @throws SQLiteException if operation fails
     */
    public static double dotProduct(SQLiteDatabase database, float[] vector1, float[] vector2) throws SQLiteException {
        return SQLiteVecUtils.dotProduct(database, vector1, vector2);
    }
    
    /**
     * Convert a vector to JSON representation using sqlite-vec
     * 
     * @param database The SQLite database with sqlite-vec loaded
     * @param vector Vector as float array
     * @return JSON string representation of the vector
     * @throws SQLiteException if operation fails
     */
    public static String toJSON(SQLiteDatabase database, float[] vector) throws SQLiteException {
        return SQLiteVecUtils.toJSON(database, vector);
    }
    
    /**
     * Convert JSON string to vector using sqlite-vec
     * 
     * @param database The SQLite database with sqlite-vec loaded
     * @param json JSON string representation of the vector
     * @return Vector as float array
     * @throws SQLiteException if operation fails
     */
    public static float[] fromJSON(SQLiteDatabase database, String json) throws SQLiteException {
        return SQLiteVecUtils.fromJSON(database, json);
    }
    
    /**
     * Get statistics about a vector table
     * 
     * @param database The SQLite database with sqlite-vec loaded
     * @param tableName Name of the vec0 table
     * @return Map containing table statistics
     * @throws SQLiteException if operation fails
     */
    public static Map<String, Object> getTableStats(SQLiteDatabase database, String tableName) throws SQLiteException {
        Map<String, Object> kotlinStats = SQLiteVecUtils.getTableStats(database, tableName);
        return new HashMap<>(kotlinStats);
    }
    
    /**
     * Helper class for vector batch operations
     */
    public static class VectorPair {
        private final long id;
        private final float[] vector;
        
        public VectorPair(long id, float[] vector) {
            this.id = id;
            this.vector = vector;
        }
        
        public long getId() {
            return id;
        }
        
        public float[] getVector() {
            return vector;
        }
    }
}
