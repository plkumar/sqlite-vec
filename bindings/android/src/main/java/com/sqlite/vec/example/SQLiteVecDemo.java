package com.sqlite.vec.example;

import android.util.Log;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;

import com.sqlite.vec.SQLiteVec;
import com.sqlite.vec.VectorOperations;
import com.sqlite.vec.VectorTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple demo showing sqlite-vec usage in Android Java, similar to Swift examples
 * This demonstrates the main features of the Android sqlite-vec bindings
 */
public class SQLiteVecDemo {
    private static final String TAG = "SQLiteVecDemo";
    private static final String DATABASE_NAME = "vector_demo.db";
    private static final int DATABASE_VERSION = 1;
    
    public static void runDemo() {
        Log.i(TAG, "=== SQLiteVec Android Demo ===");
        
        try {
            // Create an in-memory database for demo
            SupportSQLiteOpenHelper.Configuration config = SupportSQLiteOpenHelper.Configuration.builder(null)
                    .name(null) // null name creates in-memory database
                    .callback(new SupportSQLiteOpenHelper.Callback(DATABASE_VERSION) {
                        @Override
                        public void onCreate(SupportSQLiteDatabase db) {
                            Log.i(TAG, "Database created");
                        }
                        
                        @Override
                        public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
                            Log.i(TAG, "Database upgraded from " + oldVersion + " to " + newVersion);
                        }
                    })
                    .build();
            
            SupportSQLiteOpenHelper helper = new FrameworkSQLiteOpenHelperFactory().create(config);
            SupportSQLiteDatabase database = helper.getWritableDatabase();
            
            // Load sqlite-vec extension
            SQLiteVec.load(database);
            
            // Get version info
            String version = SQLiteVec.version(database);
            Log.i(TAG, "sqlite-vec version: " + version);
            
            // Create a vector table
            SQLiteVec.createVectorTable(database, "vec_items", "embedding float[4]");
            
            // Define test vectors similar to Swift examples
            List<SQLiteVec.VectorEntry> items = new ArrayList<>();
            items.add(new SQLiteVec.VectorEntry(1, new float[]{0.1f, 0.1f, 0.1f, 0.1f}));
            items.add(new SQLiteVec.VectorEntry(2, new float[]{0.2f, 0.2f, 0.2f, 0.2f}));
            items.add(new SQLiteVec.VectorEntry(3, new float[]{0.3f, 0.3f, 0.3f, 0.3f}));
            items.add(new SQLiteVec.VectorEntry(4, new float[]{0.4f, 0.4f, 0.4f, 0.4f}));
            items.add(new SQLiteVec.VectorEntry(5, new float[]{0.5f, 0.5f, 0.5f, 0.5f}));
            
            // Insert vectors using batch operation
            SQLiteVec.insertVectorsBatch(database, "vec_items", "embedding", items);
            Log.i(TAG, "Inserted " + items.size() + " vectors");
            
            // Test vector operations
            demoVectorOperations(database);
            
            // Test vector search
            demoVectorSearch(database);
            
            // Test serialization
            demoSerialization();
            
            // Close database
            database.close();
            helper.close();
            
            Log.i(TAG, "Demo completed successfully!");
            
        } catch (Exception e) {
            Log.e(TAG, "Demo failed with error: " + e.getMessage(), e);
        }
    }
    
    private static void demoVectorOperations(SupportSQLiteDatabase database) throws SQLiteVec.SQLiteVecException {
        Log.i(TAG, "\n--- Vector Operations Demo ---");
        
        float[] vector1 = {1.0f, 2.0f, 3.0f, 4.0f};
        float[] vector2 = {2.0f, 3.0f, 4.0f, 5.0f};
        
        // Test distance calculations
        double cosineDistance = SQLiteVec.distance(database, vector1, vector2, "cosine");
        double l2Distance = SQLiteVec.distance(database, vector1, vector2, "l2");
        
        Log.i(TAG, "Cosine distance: " + cosineDistance);
        Log.i(TAG, "L2 distance: " + l2Distance);
        
        // Test vector length
        int length = SQLiteVec.length(database, vector1);
        Log.i(TAG, "Vector length: " + length);
        
        // Test vector normalization
        float[] normalized = SQLiteVec.normalize(database, vector1);
        Log.i(TAG, "Normalized vector: " + java.util.Arrays.toString(normalized));
        
        // Test vector addition
        float[] sum = SQLiteVec.add(database, vector1, vector2);
        Log.i(TAG, "Vector sum: " + java.util.Arrays.toString(sum));
        
        // Test vector subtraction
        float[] diff = SQLiteVec.subtract(database, vector1, vector2);
        Log.i(TAG, "Vector difference: " + java.util.Arrays.toString(diff));
        
        // Test vector to JSON
        String json = SQLiteVec.toJson(database, vector1);
        Log.i(TAG, "Vector as JSON: " + json);
    }
    
    private static void demoVectorSearch(SupportSQLiteDatabase database) throws SQLiteVec.SQLiteVecException {
        Log.i(TAG, "\n--- Vector Search Demo ---");
        
        // Query vector similar to item 2
        float[] queryVector = {0.25f, 0.25f, 0.25f, 0.25f};
        
        // Search for similar vectors
        List<SQLiteVec.SearchResult> results = SQLiteVec.searchSimilar(
            database, "vec_items", "embedding", queryVector, 3);
        
        Log.i(TAG, "Search results for query vector " + java.util.Arrays.toString(queryVector) + ":");
        for (SQLiteVec.SearchResult result : results) {
            Log.i(TAG, "  Row ID: " + result.rowId + ", Distance: " + result.distance);
        }
        
        // Test with VectorOperations compatibility methods
        double distance = VectorOperations.cosineDistance(database, queryVector, new float[]{0.2f, 0.2f, 0.2f, 0.2f});
        Log.i(TAG, "Direct cosine distance to item 2: " + distance);
    }
    
    private static void demoSerialization() {
        Log.i(TAG, "\n--- Serialization Demo ---");
        
        // Test Float32 serialization
        float[] originalVector = {1.5f, 2.5f, 3.5f, 4.5f};
        byte[] serialized = SQLiteVec.serialize(originalVector);
        float[] deserialized = SQLiteVec.deserializeFloat32(serialized);
        
        Log.i(TAG, "Original vector: " + java.util.Arrays.toString(originalVector));
        Log.i(TAG, "Serialized size: " + serialized.length + " bytes");
        Log.i(TAG, "Deserialized vector: " + java.util.Arrays.toString(deserialized));
        
        // Test Int8 serialization
        byte[] originalInt8 = {1, 2, 3, 4, 5};
        byte[] serializedInt8 = SQLiteVec.serialize(originalInt8);
        byte[] deserializedInt8 = SQLiteVec.deserializeInt8(serializedInt8);
        
        Log.i(TAG, "Original int8 vector: " + java.util.Arrays.toString(originalInt8));
        Log.i(TAG, "Deserialized int8 vector: " + java.util.Arrays.toString(deserializedInt8));
        
        // Test compatibility methods
        byte[] compatSerialized = SQLiteVec.serializeFloat32(originalVector);
        Log.i(TAG, "Compatible serialization works: " + java.util.Arrays.equals(serialized, compatSerialized));
    }
    
    /**
     * Example of using SQLiteVec with a more complex scenario
     */
    public static void advancedDemo() {
        Log.i(TAG, "\n=== Advanced SQLiteVec Demo ===");
        
        try {
            // Create database with callback for complex setup
            SupportSQLiteOpenHelper.Configuration config = SupportSQLiteOpenHelper.Configuration.builder(null)
                    .name(null)
                    .callback(new SupportSQLiteOpenHelper.Callback(DATABASE_VERSION) {
                        @Override
                        public void onCreate(SupportSQLiteDatabase db) {
                            try {
                                // Load sqlite-vec in the callback
                                SQLiteVec.load(db);
                                
                                // Create vector table with additional columns
                                SQLiteVec.createVectorTable(db, "documents", "embedding float[384]", 
                                    new String[]{"title TEXT", "content TEXT", "category TEXT"});
                                
                                Log.i(TAG, "Advanced database setup completed");
                            } catch (SQLiteVec.SQLiteVecException e) {
                                Log.e(TAG, "Error setting up advanced database", e);
                            }
                        }
                        
                        @Override
                        public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
                            // Handle upgrades
                        }
                    })
                    .build();
            
            SupportSQLiteOpenHelper helper = new FrameworkSQLiteOpenHelperFactory().create(config);
            SupportSQLiteDatabase database = helper.getWritableDatabase();
            
            // Check if sqlite-vec is available
            if (SQLiteVec.isAvailable(database)) {
                Log.i(TAG, "sqlite-vec is available and ready");
                
                // Create sample document embeddings (384 dimensions)
                float[] docEmbedding = new float[384];
                for (int i = 0; i < 384; i++) {
                    docEmbedding[i] = (float) Math.random();
                }
                
                // Insert document with vector
                database.execSQL("INSERT INTO documents (rowid, title, content, category, embedding) VALUES (?, ?, ?, ?, ?)",
                    new Object[]{1, "Sample Document", "This is a sample document", "tech", SQLiteVec.serialize(docEmbedding)});
                
                // Query with vector search
                float[] queryEmbedding = new float[384];
                for (int i = 0; i < 384; i++) {
                    queryEmbedding[i] = (float) Math.random();
                }
                
                List<SQLiteVec.SearchResult> results = SQLiteVec.searchSimilar(
                    database, "documents", "embedding", queryEmbedding, 5, 
                    new String[]{"title", "category"});
                
                Log.i(TAG, "Found " + results.size() + " similar documents");
                
            } else {
                Log.w(TAG, "sqlite-vec is not available");
            }
            
            database.close();
            helper.close();
            
        } catch (Exception e) {
            Log.e(TAG, "Advanced demo failed", e);
        }
    }
}
