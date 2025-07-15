package com.sqlite.vec;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteStatement;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Java bindings for the sqlite-vec SQLite extension
 * Provides vector search capabilities in SQLite databases on Android
 * 
 * This implementation follows the sqlite-android package structure and
 * provides functionality similar to the Swift bindings for sqlite-vec.
 */
public class SQLiteVec {
    private static final String TAG = "SQLiteVec";
    private static final String LIBRARY_NAME = "sqlite-vec";
    
    private static boolean isLoaded = false;
    
    static {
        try {
            System.loadLibrary(LIBRARY_NAME);
            isLoaded = true;
            System.out.println("SQLiteVec native library loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Failed to load SQLiteVec native library: " + e.getMessage());
            isLoaded = false;
        } catch (Exception e) {
            // Handle any other exceptions during loading
            System.err.println("Exception during native library loading: " + e.getMessage());
            isLoaded = false;
        }
    }
    
    // Native method declarations
    private static native int nativeInit(long dbPointer);
    private static native String nativeGetVersion();
    private static native byte[] nativeSerializeFloat32(float[] vector);
    private static native float[] nativeDeserializeFloat32(byte[] data);
    private static native byte[] nativeSerializeInt8(byte[] vector);
    private static native byte[] nativeDeserializeInt8(byte[] data);
    private static native boolean nativeIsLoaded(long dbPointer);
    
    /**
     * Check if the native library is loaded
     * @return true if native library is loaded, false otherwise
     */
    public static boolean isNativeLoaded() {
        return isLoaded;
    }
    
    /**
     * Load sqlite-vec into a SupportSQLiteDatabase
     * Similar to Swift SQLiteVec.load(database:)
     * @param database The SupportSQLiteDatabase to load sqlite-vec into
     * @throws SQLiteVecException if loading fails
     */
    public static void load(SupportSQLiteDatabase database) throws SQLiteVecException {
        if (!isLoaded) {
            throw new SQLiteVecException("SQLiteVec native library not loaded");
        }
        
        long dbPointer = getDatabasePointer(database);
        int result = nativeInit(dbPointer);
        
        if (result != 0) { // SQLITE_OK is 0
            throw new SQLiteVecException("Failed to initialize sqlite-vec: " + result);
        }
    }
    
    /**
     * Get the version of sqlite-vec
     * Similar to Swift SQLiteVec.version(database:)
     * @param database The database to check version from
     * @return The version string
     * @throws SQLiteVecException if the query fails
     */
    public static String version(SupportSQLiteDatabase database) throws SQLiteVecException {
        ensureLoaded(database);
        
        Cursor cursor = database.query("SELECT vec_version()", null);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }
            throw new SQLiteVecException("Failed to get version");
        } finally {
            cursor.close();
        }
    }
    
    /**
     * Get the version of sqlite-vec (static method)
     * @return The version string
     */
    public static String getVersion() {
        if (!isLoaded) {
            return "Unknown (library not loaded)";
        }
        return nativeGetVersion();
    }
    
    /**
     * Check if sqlite-vec is loaded in the given database
     * Similar to Swift SQLiteVec.isAvailable(database:)
     * @param database The database to check
     * @return true if sqlite-vec is loaded, false otherwise
     */
    public static boolean isAvailable(SupportSQLiteDatabase database) {
        if (!isLoaded) {
            return false;
        }
        
        try {
            long dbPointer = getDatabasePointer(database);
            return nativeIsLoaded(dbPointer);
        } catch (Exception e) {
            Log.e(TAG, "Error checking if sqlite-vec is available", e);
            return false;
        }
    }
    
    /**
     * Ensure sqlite-vec is loaded in the database
     * Similar to Swift SQLiteVec.ensureLoaded(database:)
     * @param database The database to ensure sqlite-vec is loaded into
     * @throws SQLiteVecException if loading fails
     */
    public static void ensureLoaded(SupportSQLiteDatabase database) throws SQLiteVecException {
        if (!isAvailable(database)) {
            load(database);
        }
    }
    
    // MARK: - Vector Serialization (similar to Swift extension)
    
    /**
     * Serialize a float array into raw bytes format that sqlite-vec expects
     * Similar to Swift SQLiteVec.serialize(_:) and SQLiteVec.serializeFloat32(_:)
     * @param vector Array of float values
     * @return Serialized vector data
     */
    public static byte[] serialize(float[] vector) {
        if (!isLoaded) {
            throw new RuntimeException("SQLiteVec native library not loaded");
        }
        return nativeSerializeFloat32(vector);
    }
    
    /**
     * Serialize a float array into raw bytes format that sqlite-vec expects
     * Alias for serialize(float[]) for Go bindings compatibility
     * @param vector Array of float values
     * @return Serialized vector data
     */
    public static byte[] serializeFloat32(float[] vector) {
        return serialize(vector);
    }
    
    /**
     * Deserialize raw bytes into a float array
     * Similar to Swift SQLiteVec.deserializeFloat32(_:)
     * @param data Raw bytes from sqlite-vec
     * @return Array of float values
     */
    public static float[] deserializeFloat32(byte[] data) {
        if (!isLoaded) {
            throw new RuntimeException("SQLiteVec native library not loaded");
        }
        return nativeDeserializeFloat32(data);
    }
    
    /**
     * Serialize an int8 array into raw bytes format that sqlite-vec expects
     * Similar to Swift SQLiteVec.serialize(_:) for Int8
     * @param vector Array of byte values
     * @return Serialized vector data
     */
    public static byte[] serialize(byte[] vector) {
        if (!isLoaded) {
            throw new RuntimeException("SQLiteVec native library not loaded");
        }
        return nativeSerializeInt8(vector);
    }
    
    /**
     * Serialize an int8 array into raw bytes format that sqlite-vec expects
     * Alias for serialize(byte[]) for Go bindings compatibility
     * @param vector Array of byte values
     * @return Serialized vector data
     */
    public static byte[] serializeInt8(byte[] vector) {
        return serialize(vector);
    }
    
    /**
     * Deserialize raw bytes into an int8 array
     * Similar to Swift SQLiteVec.deserializeInt8(_:)
     * @param data Raw bytes from sqlite-vec
     * @return Array of byte values
     */
    public static byte[] deserializeInt8(byte[] data) {
        if (!isLoaded) {
            throw new RuntimeException("SQLiteVec native library not loaded");
        }
        return nativeDeserializeInt8(data);
    }
    
    // MARK: - Vector Operations (similar to Swift extension)
    
    /**
     * Calculate the distance between two vectors using sqlite-vec
     * Similar to Swift SQLiteVec.distance(database:vector1:vector2:metric:)
     * @param database The SQLite database
     * @param vector1 First vector as float array
     * @param vector2 Second vector as float array
     * @param metric Distance metric ("cosine", "l2", "l1", "hamming")
     * @return The calculated distance
     * @throws SQLiteVecException if the operation fails
     */
    public static double distance(SupportSQLiteDatabase database, float[] vector1, float[] vector2, String metric) 
            throws SQLiteVecException {
        ensureLoaded(database);
        
        byte[] data1 = serialize(vector1);
        byte[] data2 = serialize(vector2);
        
        String functionName;
        switch (metric.toLowerCase()) {
            case "cosine":
                functionName = "vec_distance_cosine";
                break;
            case "l2":
                functionName = "vec_distance_l2";
                break;
            case "l1":
                functionName = "vec_distance_l1";
                break;
            case "hamming":
                functionName = "vec_distance_hamming";
                break;
            default:
                functionName = "vec_distance_cosine";
        }
        
        String sql = "SELECT " + functionName + "(?, ?)";
        
        // Convert blob data to hex format for rawQuery
        StringBuilder hex1 = new StringBuilder();
        for (byte b : data1) {
            hex1.append(String.format("%02x", b));
        }
        StringBuilder hex2 = new StringBuilder();
        for (byte b : data2) {
            hex2.append(String.format("%02x", b));
        }
        
        String formattedSql = String.format("SELECT %s(X'%s', X'%s')", functionName, hex1.toString(), hex2.toString());
        
        Cursor cursor = database.query(formattedSql);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getDouble(0);
            }
            return 0.0;
        } finally {
            cursor.close();
        }
    }
    
    /**
     * Calculate the cosine distance between two vectors (default metric)
     * Similar to Swift SQLiteVec.distance(database:vector1:vector2:)
     * @param database The SQLite database
     * @param vector1 First vector as float array
     * @param vector2 Second vector as float array
     * @return The calculated distance
     * @throws SQLiteVecException if the operation fails
     */
    public static double distance(SupportSQLiteDatabase database, float[] vector1, float[] vector2) 
            throws SQLiteVecException {
        return distance(database, vector1, vector2, "cosine");
    }
    
    /**
     * Get the length (number of dimensions) of a vector
     * Similar to Swift SQLiteVec.length(database:vector:)
     * @param database The SQLite database
     * @param vector Vector as float array
     * @return The number of dimensions
     * @throws SQLiteVecException if the operation fails
     */
    public static int length(SupportSQLiteDatabase database, float[] vector) 
            throws SQLiteVecException {
        ensureLoaded(database);
        
        byte[] data = serialize(vector);
        
        // Convert blob data to hex format for rawQuery
        StringBuilder hex = new StringBuilder();
        for (byte b : data) {
            hex.append(String.format("%02x", b));
        }
        
        String sql = String.format("SELECT vec_length(X'%s')", hex.toString());
        
        Cursor cursor = database.query(sql);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
            return 0;
        } finally {
            cursor.close();
        }
    }
    
    /**
     * Normalize a vector using sqlite-vec
     * Similar to Swift SQLiteVec.normalize(database:vector:)
     * @param database The SQLite database
     * @param vector Vector as float array to normalize
     * @return Normalized vector as float array
     * @throws SQLiteVecException if the operation fails
     */
    public static float[] normalize(SupportSQLiteDatabase database, float[] vector) 
            throws SQLiteVecException {
        ensureLoaded(database);
        
        byte[] data = serialize(vector);
        
        // Convert blob data to hex format for rawQuery
        StringBuilder hex = new StringBuilder();
        for (byte b : data) {
            hex.append(String.format("%02x", b));
        }
        
        String sql = String.format("SELECT vec_normalize(X'%s')", hex.toString());
        
        Cursor cursor = database.query(sql);
        try {
            if (cursor.moveToFirst()) {
                byte[] resultData = cursor.getBlob(0);
                return deserializeFloat32(resultData);
            }
            return new float[0];
        } finally {
            cursor.close();
        }
    }
    
    /**
     * Add two vectors using sqlite-vec
     * Similar to Swift SQLiteVec.add(database:vector1:vector2:)
     * @param database The SQLite database
     * @param vector1 First vector as float array
     * @param vector2 Second vector as float array
     * @return The resulting vector as float array
     * @throws SQLiteVecException if the operation fails
     */
    public static float[] add(SupportSQLiteDatabase database, float[] vector1, float[] vector2) 
            throws SQLiteVecException {
        ensureLoaded(database);
        
        byte[] data1 = serialize(vector1);
        byte[] data2 = serialize(vector2);
        
        // Convert blob data to hex format for rawQuery
        StringBuilder hex1 = new StringBuilder();
        for (byte b : data1) {
            hex1.append(String.format("%02x", b));
        }
        StringBuilder hex2 = new StringBuilder();
        for (byte b : data2) {
            hex2.append(String.format("%02x", b));
        }
        
        String sql = String.format("SELECT vec_add(X'%s', X'%s')", hex1.toString(), hex2.toString());
        
        Cursor cursor = database.query(sql);
        try {
            if (cursor.moveToFirst()) {
                byte[] resultData = cursor.getBlob(0);
                return deserializeFloat32(resultData);
            }
            return new float[0];
        } finally {
            cursor.close();
        }
    }
    
    /**
     * Subtract two vectors using sqlite-vec
     * Similar to Swift SQLiteVec.subtract(database:vector1:vector2:)
     * @param database The SQLite database
     * @param vector1 First vector as float array
     * @param vector2 Second vector as float array
     * @return The resulting vector as float array
     * @throws SQLiteVecException if the operation fails
     */
    public static float[] subtract(SupportSQLiteDatabase database, float[] vector1, float[] vector2) 
            throws SQLiteVecException {
        ensureLoaded(database);
        
        byte[] data1 = serialize(vector1);
        byte[] data2 = serialize(vector2);
        
        // Convert blob data to hex format for rawQuery
        StringBuilder hex1 = new StringBuilder();
        for (byte b : data1) {
            hex1.append(String.format("%02x", b));
        }
        StringBuilder hex2 = new StringBuilder();
        for (byte b : data2) {
            hex2.append(String.format("%02x", b));
        }
        
        String sql = String.format("SELECT vec_sub(X'%s', X'%s')", hex1.toString(), hex2.toString());
        
        Cursor cursor = database.query(sql);
        try {
            if (cursor.moveToFirst()) {
                byte[] resultData = cursor.getBlob(0);
                return deserializeFloat32(resultData);
            }
            return new float[0];
        } finally {
            cursor.close();
        }
    }
    
    /**
     * Convert a vector to JSON representation using sqlite-vec
     * Similar to Swift SQLiteVec.toJSON(database:vector:)
     * @param database The SQLite database
     * @param vector Vector as float array
     * @return JSON string representation of the vector
     * @throws SQLiteVecException if the operation fails
     */
    public static String toJson(SupportSQLiteDatabase database, float[] vector) 
            throws SQLiteVecException {
        ensureLoaded(database);
        
        byte[] data = serialize(vector);
        
        // Convert blob data to hex format for rawQuery
        StringBuilder hex = new StringBuilder();
        for (byte b : data) {
            hex.append(String.format("%02x", b));
        }
        
        String sql = String.format("SELECT vec_to_json(X'%s')", hex.toString());
        
        Cursor cursor = database.query(sql);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }
            return "[]";
        } finally {
            cursor.close();
        }
    }
    
    // MARK: - Vector Table Operations (similar to Swift extension)
    
    /**
     * Create a vec0 virtual table
     * Similar to Swift SQLiteVec.createVectorTable(database:tableName:vectorColumn:additionalColumns:)
     * @param database The SQLite database
     * @param tableName Name of the table to create
     * @param vectorColumn Name and definition of the vector column (e.g., "embedding float[384]")
     * @param additionalColumns Additional column definitions (can be null)
     * @throws SQLiteVecException if the creation fails
     */
    public static void createVectorTable(SupportSQLiteDatabase database, String tableName, 
                                       String vectorColumn, String[] additionalColumns) 
            throws SQLiteVecException {
        ensureLoaded(database);
        
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE VIRTUAL TABLE ").append(tableName).append(" USING vec0(");
        sql.append(vectorColumn);
        
        if (additionalColumns != null && additionalColumns.length > 0) {
            for (String column : additionalColumns) {
                sql.append(", ").append(column);
            }
        }
        
        sql.append(")");
        
        database.execSQL(sql.toString());
    }
    
    /**
     * Create a vec0 virtual table with just a vector column
     * @param database The SQLite database
     * @param tableName Name of the table to create
     * @param vectorColumn Name and definition of the vector column
     * @throws SQLiteVecException if the creation fails
     */
    public static void createVectorTable(SupportSQLiteDatabase database, String tableName, String vectorColumn) 
            throws SQLiteVecException {
        createVectorTable(database, tableName, vectorColumn, null);
    }
    
    /**
     * Search for similar vectors in a vec0 table
     * Similar to Swift SQLiteVec.searchSimilar(database:tableName:vectorColumn:queryVector:limit:additionalColumns:)
     * @param database The SQLite database
     * @param tableName Name of the vec0 table
     * @param vectorColumn Name of the vector column
     * @param queryVector Vector to search for similarities
     * @param limit Maximum number of results
     * @param additionalColumns Additional columns to select (can be null)
     * @return List of search results
     * @throws SQLiteVecException if the search fails
     */
    public static List<SearchResult> searchSimilar(SupportSQLiteDatabase database, String tableName, 
                                                 String vectorColumn, float[] queryVector, int limit,
                                                 String[] additionalColumns) 
            throws SQLiteVecException {
        ensureLoaded(database);
        
        byte[] queryData = serialize(queryVector);
        
        // Build select clause
        StringBuilder selectClause = new StringBuilder("rowid, distance");
        if (additionalColumns != null && additionalColumns.length > 0) {
            for (String column : additionalColumns) {
                selectClause.append(", ").append(column);
            }
        }
        
        String sql = "SELECT " + selectClause.toString() + " FROM " + tableName + 
                    " WHERE " + vectorColumn + " MATCH X'%s' ORDER BY distance LIMIT %d";
        
        List<SearchResult> results = new ArrayList<>();
        
        // Convert blob to hex string for query (SQLite standard approach)
        StringBuilder hexString = new StringBuilder();
        for (byte b : queryData) {
            hexString.append(String.format("%02x", b));
        }
        
        String formattedSql = String.format(sql, hexString.toString(), limit);
        
        // Use rawQuery to execute the vector search
        Cursor cursor = database.query(formattedSql);
        try {
            while (cursor.moveToNext()) {
                long rowId = cursor.getLong(0);
                double distance = cursor.getDouble(1);
                
                Object[] additionalData = null;
                if (additionalColumns != null && additionalColumns.length > 0) {
                    additionalData = new Object[additionalColumns.length];
                    for (int i = 0; i < additionalColumns.length; i++) {
                        additionalData[i] = cursor.getString(i + 2);
                    }
                }
                
                results.add(new SearchResult(rowId, distance, additionalData));
            }
            return results;
        } finally {
            cursor.close();
        }
    }
    
    /**
     * Search for similar vectors in a vec0 table (simple version)
     * @param database The SQLite database
     * @param tableName Name of the vec0 table
     * @param vectorColumn Name of the vector column
     * @param queryVector Vector to search for similarities
     * @param limit Maximum number of results
     * @return List of search results
     * @throws SQLiteVecException if the search fails
     */
    public static List<SearchResult> searchSimilar(SupportSQLiteDatabase database, String tableName, 
                                                 String vectorColumn, float[] queryVector, int limit) 
            throws SQLiteVecException {
        return searchSimilar(database, tableName, vectorColumn, queryVector, limit, null);
    }
    
    // MARK: - Batch Operations (Go-style, similar to Swift)
    
    /**
     * Insert multiple vectors in a batch operation for better performance
     * Similar to Swift SQLiteVec.insertVectorsBatch(database:tableName:vectorColumn:vectors:)
     * @param database The SQLite database
     * @param tableName Name of the vec0 table
     * @param vectorColumn Name of the vector column
     * @param vectors List of VectorEntry objects containing id and vector data
     * @throws SQLiteVecException if the batch insertion fails
     */
    public static void insertVectorsBatch(SupportSQLiteDatabase database, String tableName, 
                                        String vectorColumn, List<VectorEntry> vectors) 
            throws SQLiteVecException {
        ensureLoaded(database);
        
        database.beginTransaction();
        try {
            String sql = "INSERT INTO " + tableName + "(rowid, " + vectorColumn + ") VALUES (?, ?)";
            SupportSQLiteStatement statement = database.compileStatement(sql);
            try {
                for (VectorEntry entry : vectors) {
                    byte[] vectorData = serialize(entry.vector);
                    statement.bindLong(1, entry.id);
                    statement.bindBlob(2, vectorData);
                    statement.executeInsert();
                    statement.clearBindings();
                }
                database.setTransactionSuccessful();
            } finally {
                try {
                    statement.close();
                } catch (IOException e) {
                    // Log error but don't throw to avoid masking the original exception
                }
            }
        } finally {
            database.endTransaction();
        }
    }
    
    /**
     * Helper method to convert a float array to ByteBuffer for direct binding to SQLite
     * Similar to Swift serialization approach
     * @param vector Array of float values
     * @return ByteBuffer containing the serialized vector
     */
    public static ByteBuffer toByteBuffer(float[] vector) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(vector.length * 4);
        buffer.order(ByteOrder.nativeOrder());
        FloatBuffer floatBuffer = buffer.asFloatBuffer();
        floatBuffer.put(vector);
        buffer.rewind();
        return buffer;
    }
    
    /**
     * Helper method to convert a ByteBuffer to float array
     * Similar to Swift deserialization approach
     * @param buffer ByteBuffer containing serialized vector data
     * @return Array of float values
     */
    public static float[] fromByteBuffer(ByteBuffer buffer) {
        buffer.rewind();
        FloatBuffer floatBuffer = buffer.asFloatBuffer();
        float[] result = new float[floatBuffer.remaining()];
        floatBuffer.get(result);
        return result;
    }
    
    /**
     * Get the native database pointer from a SupportSQLiteDatabase
     * Uses reflection to access the native pointer
     * @param database The SupportSQLiteDatabase
     * @return Native database pointer
     * @throws SQLiteVecException if unable to get pointer
     */
    private static long getDatabasePointer(SupportSQLiteDatabase database) throws SQLiteVecException {
        try {
            // Try to get the native pointer through reflection
            // This works with the standard Android SQLite implementation
            
            // First try to get the wrapped database
            Method getWrappedDb = database.getClass().getMethod("getWrappedDb");
            Object wrappedDb = getWrappedDb.invoke(database);
            
            // Then get the native pointer
            Method getNativePtr = wrappedDb.getClass().getMethod("getNativePtr");
            return (Long) getNativePtr.invoke(wrappedDb);
            
        } catch (Exception e) {
            // Fallback: try different approaches for different SQLite implementations
            try {
                // Try Room database approach
                Class<?> roomDbClass = Class.forName("androidx.room.RoomDatabase");
                if (roomDbClass.isInstance(database)) {
                    Method getInternalDatabase = roomDbClass.getMethod("getInternalDatabase");
                    Object internalDb = getInternalDatabase.invoke(database);
                    
                    Method getNativePtr = internalDb.getClass().getMethod("getNativePtr");
                    return (Long) getNativePtr.invoke(internalDb);
                }
            } catch (Exception e2) {
                // Ignore and continue
            }
            
            throw new SQLiteVecException("Unable to get native database pointer", e);
        }
    }
    
    // MARK: - Data Classes (similar to Swift structs)
    
    /**
     * Represents a search result from a vector table
     * Similar to Swift tuple return types
     */
    public static class SearchResult {
        public final long rowId;
        public final double distance;
        public final Object[] additionalData;
        
        public SearchResult(long rowId, double distance, Object[] additionalData) {
            this.rowId = rowId;
            this.distance = distance;
            this.additionalData = additionalData;
        }
        
        public SearchResult(long rowId, double distance) {
            this(rowId, distance, null);
        }
    }
    
    /**
     * Represents a vector entry for batch operations
     * Similar to Swift tuple types used in batch operations
     */
    public static class VectorEntry {
        public final long id;
        public final float[] vector;
        
        public VectorEntry(long id, float[] vector) {
            this.id = id;
            this.vector = vector;
        }
    }
    
    /**
     * Exception class for SQLiteVec-related errors
     * Similar to Swift error throwing
     */
    public static class SQLiteVecException extends Exception {
        public SQLiteVecException(String message) {
            super(message);
        }
        
        public SQLiteVecException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
