package com.sqlite.vec;

import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Android instrumented tests for SQLiteVecJava (Java compatibility layer)
 * 
 * These tests validate that the Java API works correctly and provides
 * the same functionality as the Kotlin implementation.
 */
@RunWith(AndroidJUnit4.class)
public class SQLiteVecJavaInstrumentedTest {
    
    private SQLiteDatabase database;
    private File tempDbFile;
    
    @Before
    public void setUp() throws IOException {
        android.content.Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        
        // Create temporary database file
        tempDbFile = File.createTempFile("test_sqlite_vec_java", ".db", context.getCacheDir());
        
        // Open database
        database = SQLiteDatabase.openDatabase(
            tempDbFile.getAbsolutePath(),
            null,
            SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.CREATE_IF_NECESSARY
        );
        
        // Load sqlite-vec extension
        SQLiteVecJava.load(database);
    }
    
    @After
    public void tearDown() {
        if (database != null) {
            database.close();
        }
        if (tempDbFile != null) {
            tempDbFile.delete();
        }
    }
    
    @Test
    public void testVersionQuery() {
        String version = SQLiteVecJava.version(database);
        assertTrue("Version should start with 'v'", version.startsWith("v"));
        System.out.println("sqlite-vec version: " + version);
    }
    
    @Test
    public void testGetVersion() {
        String version = SQLiteVecJava.getVersion();
        assertTrue("Version should start with 'v'", version.startsWith("v"));
        assertNotNull("Version should not be null", version);
    }
    
    @Test
    public void testIsAvailable() {
        assertTrue("sqlite-vec should be available after loading", SQLiteVecJava.isAvailable(database));
    }
    
    @Test
    public void testEnsureLoaded() {
        // Should not throw since already loaded
        SQLiteVecJava.ensureLoaded(database);
        assertTrue("sqlite-vec should still be available", SQLiteVecJava.isAvailable(database));
    }
    
    @Test
    public void testAutoLoadMethods() {
        // These should not throw
        SQLiteVecJava.autoLoad();
        SQLiteVecJava.cancelAutoLoad();
    }
    
    @Test
    public void testSerializeFloat32() {
        float[] vector = {0.1f, 0.2f, 0.3f, 0.4f};
        byte[] data = SQLiteVecJava.serialize(vector);
        
        // Should be 4 floats * 4 bytes each = 16 bytes
        assertEquals("Serialized data should be 16 bytes", 16, data.length);
        
        // Test deserialization
        float[] deserialized = SQLiteVecJava.deserializeFloat32(data);
        assertEquals("Deserialized should have 4 elements", 4, deserialized.length);
        assertEquals("First element should match", 0.1f, deserialized[0], 0.001f);
        assertEquals("Second element should match", 0.2f, deserialized[1], 0.001f);
        assertEquals("Third element should match", 0.3f, deserialized[2], 0.001f);
        assertEquals("Fourth element should match", 0.4f, deserialized[3], 0.001f);
    }
    
    @Test
    public void testSerializeDouble() {
        double[] vector = {0.1, 0.2, 0.3, 0.4};
        byte[] data = SQLiteVecJava.serialize(vector);
        
        // Should be 4 doubles * 8 bytes each = 32 bytes
        assertEquals("Serialized data should be 32 bytes", 32, data.length);
    }
    
    @Test
    public void testSerializeInt8() {
        byte[] vector = {1, 2, 3, 4, 5};
        byte[] data = SQLiteVecJava.serialize(vector);
        
        // Should be 5 bytes
        assertEquals("Serialized data should be 5 bytes", 5, data.length);
        
        // Test deserialization
        byte[] deserialized = SQLiteVecJava.deserializeInt8(data);
        assertArrayEquals("Deserialized should match original", vector, deserialized);
    }
    
    @Test
    public void testSerializeCompatibilityMethods() {
        float[] vector = {0.1f, 0.2f, 0.3f, 0.4f};
        byte[] data1 = SQLiteVecJava.serialize(vector);
        byte[] data2 = SQLiteVecJava.serializeFloat32(vector);
        
        assertArrayEquals("Both serialization methods should produce same result", data1, data2);
        
        byte[] byteVector = {1, 2, 3, 4, 5};
        byte[] byteData1 = SQLiteVecJava.serialize(byteVector);
        byte[] byteData2 = SQLiteVecJava.serializeInt8(byteVector);
        
        assertArrayEquals("Both int8 serialization methods should produce same result", byteData1, byteData2);
    }
    
    @Test
    public void testVectorLength() {
        float[] vector = {0.1f, 0.2f, 0.3f, 0.4f};
        int length = SQLiteVecJava.length(database, vector);
        assertEquals("Vector length should be 4", 4, length);
    }
    
    @Test
    public void testVectorDistance() {
        float[] vector1 = {1.0f, 0.0f, 0.0f};
        float[] vector2 = {0.0f, 1.0f, 0.0f};
        
        double cosineDistance = SQLiteVecJava.distance(database, vector1, vector2, "cosine");
        
        // These vectors are orthogonal, so cosine distance should be 1.0
        assertEquals("Cosine distance should be 1.0", 1.0, cosineDistance, 0.001);
    }
    
    @Test
    public void testVectorDistanceDefaultMetric() {
        float[] vector1 = {1.0f, 0.0f, 0.0f};
        float[] vector2 = {0.0f, 1.0f, 0.0f};
        
        double distance1 = SQLiteVecJava.distance(database, vector1, vector2, "cosine");
        double distance2 = SQLiteVecJava.distance(database, vector1, vector2); // should default to cosine
        
        assertEquals("Default metric should be cosine", distance1, distance2, 0.001);
    }
    
    @Test
    public void testVectorNormalize() {
        float[] vector = {3.0f, 4.0f}; // Length 5 vector
        float[] normalized = SQLiteVecJava.normalize(database, vector);
        
        assertEquals("Normalized vector should have 2 elements", 2, normalized.length);
        
        // Check that normalized vector has unit length
        float magnitude = (float) Math.sqrt(normalized[0] * normalized[0] + normalized[1] * normalized[1]);
        assertEquals("Normalized vector should have unit length", 1.0f, magnitude, 0.001f);
    }
    
    @Test
    public void testCreateVectorTable() {
        SQLiteVecJava.createVectorTable(database, "test_vectors", "embedding float[4]");
        
        // Verify table was created by inserting and querying
        float[] vector = {0.1f, 0.2f, 0.3f, 0.4f};
        byte[] vectorData = SQLiteVecJava.serialize(vector);
        
        database.execSQL("INSERT INTO test_vectors(rowid, embedding) VALUES (?, ?)", new Object[]{1, vectorData});
        
        android.database.Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM test_vectors", null);
        try {
            assertTrue("Cursor should have data", cursor.moveToFirst());
            assertEquals("Should have 1 row", 1L, cursor.getLong(0));
        } finally {
            cursor.close();
        }
    }
    
    @Test
    public void testCreateVectorTableWithAdditionalColumns() {
        SQLiteVecJava.createVectorTable(
            database,
            "test_docs",
            "embedding float[4]",
            "title TEXT",
            "content TEXT"
        );
        
        // Verify table was created with additional columns
        float[] vector = {0.1f, 0.2f, 0.3f, 0.4f};
        byte[] vectorData = SQLiteVecJava.serialize(vector);
        
        database.execSQL(
            "INSERT INTO test_docs(rowid, embedding, title, content) VALUES (?, ?, ?, ?)",
            new Object[]{1, vectorData, "Test Title", "Test Content"}
        );
        
        android.database.Cursor cursor = database.rawQuery("SELECT title, content FROM test_docs WHERE rowid = 1", null);
        try {
            assertTrue("Cursor should have data", cursor.moveToFirst());
            assertEquals("Title should match", "Test Title", cursor.getString(0));
            assertEquals("Content should match", "Test Content", cursor.getString(1));
        } finally {
            cursor.close();
        }
    }
    
    @Test
    public void testVectorSearch() {
        // Create test table
        SQLiteVecJava.createVectorTable(
            database,
            "search_test",
            "embedding float[3]",
            "title TEXT"
        );
        
        // Insert test vectors
        float[][] vectors = {
            {1.0f, 0.0f, 0.0f},
            {0.0f, 1.0f, 0.0f},
            {0.0f, 0.0f, 1.0f},
            {0.5f, 0.5f, 0.0f}
        };
        String[] titles = {"Vector 1", "Vector 2", "Vector 3", "Vector 4"};
        
        for (int i = 0; i < vectors.length; i++) {
            byte[] vectorData = SQLiteVecJava.serialize(vectors[i]);
            database.execSQL(
                "INSERT INTO search_test(rowid, embedding, title) VALUES (?, ?, ?)",
                new Object[]{i + 1, vectorData, titles[i]}
            );
        }
        
        // Search for similar vectors
        float[] queryVector = {1.0f, 0.1f, 0.0f};
        List<Map<String, Object>> results = SQLiteVecJava.searchSimilar(
            database,
            "search_test",
            "embedding",
            queryVector,
            2,
            "title"
        );
        
        assertEquals("Should return 2 results", 2, results.size());
        
        // First result should be Vector 1 (closest to query)
        Map<String, Object> firstResult = results.get(0);
        assertEquals("First result should be Vector 1", 1L, firstResult.get("rowid"));
        assertEquals("First result title should match", "Vector 1", firstResult.get("title"));
        
        // Check that distance is included
        assertTrue("First result should have distance", firstResult.containsKey("distance"));
        assertTrue("Distance should be a number", firstResult.get("distance") instanceof Double);
    }
    
    @Test
    public void testSearchSimilarDefaultLimit() {
        // Create test table
        SQLiteVecJava.createVectorTable(database, "limit_test", "embedding float[2]");
        
        // Insert more than 10 vectors
        for (int i = 1; i <= 15; i++) {
            float[] vector = {i * 0.1f, i * 0.1f};
            byte[] vectorData = SQLiteVecJava.serialize(vector);
            database.execSQL("INSERT INTO limit_test(rowid, embedding) VALUES (?, ?)", new Object[]{i, vectorData});
        }
        
        // Search with default limit
        float[] queryVector = {0.5f, 0.5f};
        List<Map<String, Object>> results = SQLiteVecJava.searchSimilar(database, "limit_test", "embedding", queryVector);
        
        assertEquals("Should return default limit of 10 results", 10, results.size());
    }
    
    @Test
    public void testVectorUtilityMethods() {
        float[] vector1 = {1.0f, 2.0f, 3.0f, 4.0f};
        float[] vector2 = {0.5f, 0.5f, 0.5f, 0.5f};
        
        // Test vector addition
        float[] sum = SQLiteVecJava.add(database, vector1, vector2);
        assertEquals("Sum should have 4 elements", 4, sum.length);
        assertEquals("First element should be 1.5", 1.5f, sum[0], 0.001f);
        assertEquals("Second element should be 2.5", 2.5f, sum[1], 0.001f);
        
        // Test vector subtraction
        float[] diff = SQLiteVecJava.subtract(database, vector1, vector2);
        assertEquals("Difference should have 4 elements", 4, diff.length);
        assertEquals("First element should be 0.5", 0.5f, diff[0], 0.001f);
        assertEquals("Second element should be 1.5", 1.5f, diff[1], 0.001f);
        
        // Test dot product
        double dotProduct = SQLiteVecJava.dotProduct(database, vector1, vector2);
        assertTrue("Dot product should be a valid number", Double.isFinite(dotProduct));
        
        // Test JSON conversion
        String json = SQLiteVecJava.toJSON(database, vector1);
        assertNotNull("JSON should not be null", json);
        assertTrue("JSON should contain array notation", json.contains("[") && json.contains("]"));
        
        float[] reconstructed = SQLiteVecJava.fromJSON(database, json);
        assertEquals("Reconstructed should have same length", vector1.length, reconstructed.length);
        assertArrayEquals("Reconstructed should match original", vector1, reconstructed, 0.001f);
    }
    
    @Test
    public void testBatchVectorInsertion() {
        SQLiteVecJava.createVectorTable(database, "batch_test", "embedding float[3]");
        
        // Create test data
        java.util.List<SQLiteVecJava.VectorPair> vectors = Arrays.asList(
            new SQLiteVecJava.VectorPair(1L, new float[]{1.0f, 0.0f, 0.0f}),
            new SQLiteVecJava.VectorPair(2L, new float[]{0.0f, 1.0f, 0.0f}),
            new SQLiteVecJava.VectorPair(3L, new float[]{0.0f, 0.0f, 1.0f})
        );
        
        SQLiteVecJava.insertVectorsBatch(database, "batch_test", "embedding", vectors);
        
        // Verify all vectors were inserted
        android.database.Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM batch_test", null);
        try {
            assertTrue("Cursor should have data", cursor.moveToFirst());
            assertEquals("Should have 3 rows", 3L, cursor.getLong(0));
        } finally {
            cursor.close();
        }
    }
    
    @Test
    public void testGetTableStats() {
        SQLiteVecJava.createVectorTable(database, "stats_test", "embedding float[3]", "title TEXT");
        
        // Insert some test data
        float[] vector = {1.0f, 0.0f, 0.0f};
        byte[] vectorData = SQLiteVecJava.serialize(vector);
        database.execSQL("INSERT INTO stats_test(rowid, embedding, title) VALUES (?, ?, ?)", 
                        new Object[]{1, vectorData, "Test"});
        
        Map<String, Object> stats = SQLiteVecJava.getTableStats(database, "stats_test");
        
        assertTrue("Stats should contain row_count", stats.containsKey("row_count"));
        assertEquals("Row count should be 1", 1L, stats.get("row_count"));
        
        assertTrue("Stats should contain columns", stats.containsKey("columns"));
        assertTrue("Should have column info", stats.get("columns") instanceof java.util.List);
    }
    
    @Test
    public void testVectorPairClass() {
        long id = 42L;
        float[] vector = {0.1f, 0.2f, 0.3f};
        
        SQLiteVecJava.VectorPair pair = new SQLiteVecJava.VectorPair(id, vector);
        
        assertEquals("ID should match", id, pair.getId());
        assertArrayEquals("Vector should match", vector, pair.getVector(), 0.001f);
    }
}
