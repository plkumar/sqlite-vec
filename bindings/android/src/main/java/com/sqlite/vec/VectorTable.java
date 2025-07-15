package com.sqlite.vec;

import android.database.Cursor;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteStatement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Additional utility class for managing vector tables (vec0 virtual tables)
 * This class provides convenience methods that delegate to the main SQLiteVec class
 * for backward compatibility and ease of use.
 */
public class VectorTable {
    
    /**
     * Create a vec0 virtual table
     * This is a convenience method that delegates to SQLiteVec.createVectorTable()
     * @param database The SQLite database
     * @param tableName Name of the table to create
     * @param vectorColumn Name and definition of the vector column (e.g., "embedding float[384]")
     * @param additionalColumns Additional column definitions
     * @throws SQLiteVec.SQLiteVecException if the creation fails
     */
    public static void createVectorTable(SupportSQLiteDatabase database, String tableName, 
                                       String vectorColumn, String[] additionalColumns) 
            throws SQLiteVec.SQLiteVecException {
        SQLiteVec.createVectorTable(database, tableName, vectorColumn, additionalColumns);
    }
    
    /**
     * Create a vec0 virtual table with just a vector column
     * @param database The SQLite database
     * @param tableName Name of the table to create
     * @param vectorColumn Name and definition of the vector column
     * @throws SQLiteVec.SQLiteVecException if the creation fails
     */
    public static void createVectorTable(SupportSQLiteDatabase database, String tableName, String vectorColumn) 
            throws SQLiteVec.SQLiteVecException {
        SQLiteVec.createVectorTable(database, tableName, vectorColumn);
    }
    
    /**
     * Insert a vector into a vec0 table
     * @param database The SQLite database
     * @param tableName Name of the vec0 table
     * @param vectorColumn Name of the vector column
     * @param rowId Row ID for the vector
     * @param vector Vector data
     * @throws SQLiteVec.SQLiteVecException if the insertion fails
     */
    public static void insertVector(SupportSQLiteDatabase database, String tableName, 
                                  String vectorColumn, long rowId, float[] vector) 
            throws SQLiteVec.SQLiteVecException {
        SQLiteVec.ensureLoaded(database);
        
        byte[] vectorData = SQLiteVec.serialize(vector);
        
        String sql = "INSERT INTO " + tableName + "(rowid, " + vectorColumn + ") VALUES (?, ?)";
        androidx.sqlite.db.SupportSQLiteStatement statement = database.compileStatement(sql);
        try {
            statement.bindLong(1, rowId);
            statement.bindBlob(2, vectorData);
            statement.executeInsert();
        } finally {
            try {
                statement.close();
            } catch (IOException e) {
                // Log error but don't throw to avoid masking the original exception
            }
        }
    }
    
    /**
     * Insert multiple vectors in a batch operation for better performance
     * This is a convenience method that delegates to SQLiteVec.insertVectorsBatch()
     * @param database The SQLite database
     * @param tableName Name of the vec0 table
     * @param vectorColumn Name of the vector column
     * @param vectors List of VectorEntry objects containing id and vector data
     * @throws SQLiteVec.SQLiteVecException if the batch insertion fails
     */
    public static void insertVectorsBatch(SupportSQLiteDatabase database, String tableName, 
                                        String vectorColumn, List<SQLiteVec.VectorEntry> vectors) 
            throws SQLiteVec.SQLiteVecException {
        SQLiteVec.insertVectorsBatch(database, tableName, vectorColumn, vectors);
    }
    
    /**
     * Search for similar vectors in a vec0 table
     * This is a convenience method that delegates to SQLiteVec.searchSimilar()
     * @param database The SQLite database
     * @param tableName Name of the vec0 table
     * @param vectorColumn Name of the vector column
     * @param queryVector Vector to search for similarities
     * @param limit Maximum number of results
     * @return List of search results
     * @throws SQLiteVec.SQLiteVecException if the search fails
     */
    public static List<SQLiteVec.SearchResult> searchSimilar(SupportSQLiteDatabase database, String tableName, 
                                                 String vectorColumn, float[] queryVector, int limit) 
            throws SQLiteVec.SQLiteVecException {
        return SQLiteVec.searchSimilar(database, tableName, vectorColumn, queryVector, limit);
    }
    
    /**
     * Search for similar vectors in a vec0 table with additional columns
     * @param database The SQLite database
     * @param tableName Name of the vec0 table
     * @param vectorColumn Name of the vector column
     * @param queryVector Vector to search for similarities
     * @param limit Maximum number of results
     * @param additionalColumns Additional columns to select
     * @return List of search results
     * @throws SQLiteVec.SQLiteVecException if the search fails
     */
    public static List<SQLiteVec.SearchResult> searchSimilar(SupportSQLiteDatabase database, String tableName, 
                                                 String vectorColumn, float[] queryVector, int limit,
                                                 String[] additionalColumns) 
            throws SQLiteVec.SQLiteVecException {
        return SQLiteVec.searchSimilar(database, tableName, vectorColumn, queryVector, limit, additionalColumns);
    }
    
    /**
     * Update a vector in a vec0 table
     * @param database The SQLite database
     * @param tableName Name of the vec0 table
     * @param vectorColumn Name of the vector column
     * @param rowId Row ID to update
     * @param vector New vector data
     * @throws SQLiteVec.SQLiteVecException if the update fails
     */
    public static void updateVector(SupportSQLiteDatabase database, String tableName, 
                                  String vectorColumn, long rowId, float[] vector) 
            throws SQLiteVec.SQLiteVecException {
        SQLiteVec.ensureLoaded(database);
        
        byte[] vectorData = SQLiteVec.serialize(vector);
        
        String sql = "UPDATE " + tableName + " SET " + vectorColumn + " = ? WHERE rowid = ?";
        androidx.sqlite.db.SupportSQLiteStatement statement = database.compileStatement(sql);
        try {
            statement.bindBlob(1, vectorData);
            statement.bindLong(2, rowId);
            statement.executeUpdateDelete();
        } finally {
            try {
                statement.close();
            } catch (IOException e) {
                // Log error but don't throw to avoid masking the original exception
            }
        }
    }
    
    /**
     * Delete a vector from a vec0 table
     * @param database The SQLite database
     * @param tableName Name of the vec0 table
     * @param rowId Row ID to delete
     * @throws SQLiteVec.SQLiteVecException if the deletion fails
     */
    public static void deleteVector(SupportSQLiteDatabase database, String tableName, long rowId) 
            throws SQLiteVec.SQLiteVecException {
        SQLiteVec.ensureLoaded(database);
        
        String sql = "DELETE FROM " + tableName + " WHERE rowid = ?";
        androidx.sqlite.db.SupportSQLiteStatement statement = database.compileStatement(sql);
        try {
            statement.bindLong(1, rowId);
            statement.executeUpdateDelete();
        } finally {
            try {
                statement.close();
            } catch (IOException e) {
                // Log error but don't throw to avoid masking the original exception
            }
        }
    }
    
    // Legacy compatibility classes
    
    /**
     * Represents a search result from a vector table
     * @deprecated Use SQLiteVec.SearchResult instead
     */
    @Deprecated
    public static class SearchResult {
        public final long rowId;
        public final double distance;
        public final Object[] additionalData;
        
        public SearchResult(long rowId, double distance, Object[] additionalData) {
            this.rowId = rowId;
            this.distance = distance;
            this.additionalData = additionalData;
        }
    }
    
    /**
     * Represents a vector entry for batch operations
     * @deprecated Use SQLiteVec.VectorEntry instead
     */
    @Deprecated
    public static class VectorEntry {
        public final long id;
        public final float[] vector;
        
        public VectorEntry(long id, float[] vector) {
            this.id = id;
            this.vector = vector;
        }
    }
}
