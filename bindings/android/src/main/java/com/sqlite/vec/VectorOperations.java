package com.sqlite.vec;

import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Additional utility class for vector operations with sqlite-vec
 * This class provides convenience methods that delegate to the main SQLiteVec class
 * for backward compatibility and ease of use.
 */
public class VectorOperations {
    
    /**
     * Calculate the distance between two vectors using sqlite-vec
     * This is a convenience method that delegates to SQLiteVec.distance()
     * @param database The SQLite database
     * @param vector1 First vector as float array
     * @param vector2 Second vector as float array
     * @param metric Distance metric ("cosine", "l2", "l1", "hamming")
     * @return The calculated distance
     * @throws SQLiteVec.SQLiteVecException if the operation fails
     */
    public static double distance(SupportSQLiteDatabase database, float[] vector1, float[] vector2, String metric) 
            throws SQLiteVec.SQLiteVecException {
        return SQLiteVec.distance(database, vector1, vector2, metric);
    }
    
    /**
     * Calculate cosine distance between two vectors
     * @param database The SQLite database
     * @param vector1 First vector
     * @param vector2 Second vector
     * @return Cosine distance
     * @throws SQLiteVec.SQLiteVecException if the operation fails
     */
    public static double cosineDistance(SupportSQLiteDatabase database, float[] vector1, float[] vector2) 
            throws SQLiteVec.SQLiteVecException {
        return SQLiteVec.distance(database, vector1, vector2, "cosine");
    }
    
    /**
     * Calculate L2 distance between two vectors
     * @param database The SQLite database
     * @param vector1 First vector
     * @param vector2 Second vector
     * @return L2 distance
     * @throws SQLiteVec.SQLiteVecException if the operation fails
     */
    public static double l2Distance(SupportSQLiteDatabase database, float[] vector1, float[] vector2) 
            throws SQLiteVec.SQLiteVecException {
        return SQLiteVec.distance(database, vector1, vector2, "l2");
    }
    
    /**
     * Get the length (number of dimensions) of a vector
     * @param database The SQLite database
     * @param vector Vector as float array
     * @return The number of dimensions
     * @throws SQLiteVec.SQLiteVecException if the operation fails
     */
    public static int vectorLength(SupportSQLiteDatabase database, float[] vector) 
            throws SQLiteVec.SQLiteVecException {
        return SQLiteVec.length(database, vector);
    }
    
    /**
     * Normalize a vector using sqlite-vec
     * @param database The SQLite database
     * @param vector Vector as float array to normalize
     * @return Normalized vector as float array
     * @throws SQLiteVec.SQLiteVecException if the operation fails
     */
    public static float[] normalize(SupportSQLiteDatabase database, float[] vector) 
            throws SQLiteVec.SQLiteVecException {
        return SQLiteVec.normalize(database, vector);
    }
    
    /**
     * Add two vectors using sqlite-vec
     * @param database The SQLite database
     * @param vector1 First vector as float array
     * @param vector2 Second vector as float array
     * @return The resulting vector as float array
     * @throws SQLiteVec.SQLiteVecException if the operation fails
     */
    public static float[] add(SupportSQLiteDatabase database, float[] vector1, float[] vector2) 
            throws SQLiteVec.SQLiteVecException {
        return SQLiteVec.add(database, vector1, vector2);
    }
    
    /**
     * Subtract two vectors using sqlite-vec
     * @param database The SQLite database
     * @param vector1 First vector as float array
     * @param vector2 Second vector as float array
     * @return The resulting vector as float array
     * @throws SQLiteVec.SQLiteVecException if the operation fails
     */
    public static float[] subtract(SupportSQLiteDatabase database, float[] vector1, float[] vector2) 
            throws SQLiteVec.SQLiteVecException {
        return SQLiteVec.subtract(database, vector1, vector2);
    }
    
    /**
     * Convert a vector to JSON representation using sqlite-vec
     * @param database The SQLite database
     * @param vector Vector as float array
     * @return JSON string representation of the vector
     * @throws SQLiteVec.SQLiteVecException if the operation fails
     */
    public static String toJson(SupportSQLiteDatabase database, float[] vector) 
            throws SQLiteVec.SQLiteVecException {
        return SQLiteVec.toJson(database, vector);
    }
}
