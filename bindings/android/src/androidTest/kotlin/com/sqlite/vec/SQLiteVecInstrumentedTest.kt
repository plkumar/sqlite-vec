package com.sqlite.vec

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.io.File

/**
 * Android instrumented tests for SQLiteVec
 * 
 * These tests run on an Android device or emulator and validate the
 * sqlite-vec extension functionality in a real Android environment.
 */
@RunWith(AndroidJUnit4::class)
class SQLiteVecInstrumentedTest {
    
    private lateinit var database: SQLiteDatabase
    private lateinit var tempDbFile: File
    
    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Create temporary database file
        tempDbFile = File.createTempFile("test_sqlite_vec", ".db", context.cacheDir)
        
        // Open database
        database = SQLiteDatabase.openDatabase(
            tempDbFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.CREATE_IF_NECESSARY
        )
        
        // Load sqlite-vec extension
        SQLiteVec.load(database)
    }
    
    @After
    fun tearDown() {
        database.close()
        tempDbFile.delete()
    }
    
    @Test
    fun testVersionQuery() {
        val version = SQLiteVec.version(database)
        assertTrue("Version should start with 'v'", version.startsWith("v"))
        println("sqlite-vec version: $version")
    }
    
    @Test
    fun testGetVersion() {
        val version = SQLiteVec.getVersion()
        assertTrue("Version should start with 'v'", version.startsWith("v"))
        assertNotNull("Version should not be null", version)
    }
    
    @Test
    fun testIsAvailable() {
        assertTrue("sqlite-vec should be available after loading", SQLiteVec.isAvailable(database))
    }
    
    @Test
    fun testEnsureLoaded() {
        // Should not throw since already loaded
        SQLiteVec.ensureLoaded(database)
        assertTrue("sqlite-vec should still be available", SQLiteVec.isAvailable(database))
    }
    
    @Test
    fun testSerializeFloat32() {
        val vector = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f)
        val data = SQLiteVec.serialize(vector)
        
        // Should be 4 floats * 4 bytes each = 16 bytes
        assertEquals("Serialized data should be 16 bytes", 16, data.size)
        
        // Test deserialization
        val deserialized = SQLiteVec.deserializeFloat32(data)
        assertEquals("Deserialized should have 4 elements", 4, deserialized.size)
        assertEquals("First element should match", 0.1f, deserialized[0], 0.001f)
        assertEquals("Second element should match", 0.2f, deserialized[1], 0.001f)
        assertEquals("Third element should match", 0.3f, deserialized[2], 0.001f)
        assertEquals("Fourth element should match", 0.4f, deserialized[3], 0.001f)
    }
    
    @Test
    fun testSerializeDouble() {
        val vector = doubleArrayOf(0.1, 0.2, 0.3, 0.4)
        val data = SQLiteVec.serialize(vector)
        
        // Should be 4 doubles * 8 bytes each = 32 bytes
        assertEquals("Serialized data should be 32 bytes", 32, data.size)
    }
    
    @Test
    fun testSerializeInt8() {
        val vector = byteArrayOf(1, 2, 3, 4, 5)
        val data = SQLiteVec.serialize(vector)
        
        // Should be 5 bytes
        assertEquals("Serialized data should be 5 bytes", 5, data.size)
        
        // Test deserialization
        val deserialized = SQLiteVec.deserializeInt8(data)
        assertArrayEquals("Deserialized should match original", vector, deserialized)
    }
    
    @Test
    fun testSerializeFloat32Compatibility() {
        val vector = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f)
        val data1 = SQLiteVec.serialize(vector)
        val data2 = SQLiteVec.serializeFloat32(vector)
        
        assertArrayEquals("Both serialization methods should produce same result", data1, data2)
    }
    
    @Test
    fun testSerializeInt8Compatibility() {
        val vector = byteArrayOf(1, 2, 3, 4, 5)
        val data1 = SQLiteVec.serialize(vector)
        val data2 = SQLiteVec.serializeInt8(vector)
        
        assertArrayEquals("Both serialization methods should produce same result", data1, data2)
    }
    
    @Test
    fun testVectorLength() {
        val vector = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f)
        val length = SQLiteVec.length(database, vector)
        assertEquals("Vector length should be 4", 4, length)
    }
    
    @Test
    fun testVectorDistance() {
        val vector1 = floatArrayOf(1.0f, 0.0f, 0.0f)
        val vector2 = floatArrayOf(0.0f, 1.0f, 0.0f)
        
        val cosineDistance = SQLiteVec.distance(database, vector1, vector2, "cosine")
        
        // These vectors are orthogonal, so cosine distance should be 1.0
        assertEquals("Cosine distance should be 1.0", 1.0, cosineDistance, 0.001)
    }
    
    @Test
    fun testVectorNormalization() {
        val vector = floatArrayOf(3.0f, 4.0f)  // Length = 5.0
        val normalized = SQLiteVec.normalize(database, vector)
        
        assertEquals("Normalized vector should have 2 dimensions", 2, normalized.size)
        assertEquals("First component should be 0.6", 0.6f, normalized[0], 0.001f)
        assertEquals("Second component should be 0.8", 0.8f, normalized[1], 0.001f)
    }
    
    @Test
    fun testCreateVectorTable() {
        SQLiteVec.createVectorTable(
            database,
            "test_vectors",
            "embedding float[4]",
            "title TEXT",
            "content TEXT"
        )
        
        // Verify table was created by inserting and querying
        val vector = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f)
        val vectorData = SQLiteVec.serialize(vector)
        
        database.execSQL(
            "INSERT INTO test_vectors(rowid, embedding, title) VALUES (?, ?, ?)",
            arrayOf(1, vectorData, "Test Document")
        )
        
        val cursor = database.rawQuery("SELECT COUNT(*) FROM test_vectors", null)
        cursor.use {
            assertTrue("Should have results", it.moveToFirst())
            val count = it.getInt(0)
            assertEquals("Should have 1 row", 1, count)
        }
    }
    
    @Test
    fun testVectorSearch() {
        // Create test table
        SQLiteVec.createVectorTable(database, "documents", "embedding float[3]")
        
        // Insert test vectors
        val vectors = listOf(
            1L to floatArrayOf(1.0f, 0.0f, 0.0f),
            2L to floatArrayOf(0.0f, 1.0f, 0.0f),
            3L to floatArrayOf(0.0f, 0.0f, 1.0f),
            4L to floatArrayOf(0.5f, 0.5f, 0.0f)
        )
        
        for ((id, vector) in vectors) {
            val vectorData = SQLiteVec.serialize(vector)
            database.execSQL(
                "INSERT INTO documents(rowid, embedding) VALUES (?, ?)",
                arrayOf(id, vectorData)
            )
        }
        
        // Search for similar vectors
        val queryVector = floatArrayOf(0.9f, 0.1f, 0.0f)  // Should be closest to vector 1
        val results = SQLiteVec.searchSimilar(database, "documents", "embedding", queryVector, 2)
        
        assertFalse("Should have results", results.isEmpty())
        assertEquals("Should return 2 results", 2, results.size)
        
        val firstResult = results[0]
        assertEquals("First result should be row 1", 1L, firstResult["rowid"])
        assertTrue("First result should have low distance", (firstResult["distance"] as Double) < 0.5)
    }
    
    @Test
    fun testBatchVectorInsert() {
        SQLiteVec.createVectorTable(database, "batch_test", "embedding float[2]")
        
        val vectors = listOf(
            1L to floatArrayOf(1.0f, 0.0f),
            2L to floatArrayOf(0.0f, 1.0f),
            3L to floatArrayOf(1.0f, 1.0f),
            4L to floatArrayOf(-1.0f, 0.0f),
            5L to floatArrayOf(0.0f, -1.0f)
        )
        
        SQLiteVec.insertVectorsBatch(database, "batch_test", "embedding", vectors)
        
        val cursor = database.rawQuery("SELECT COUNT(*) FROM batch_test", null)
        cursor.use {
            assertTrue("Should have results", it.moveToFirst())
            val count = it.getInt(0)
            assertEquals("Should have 5 rows", 5, count)
        }
    }
    
    @Test
    fun testVectorToJSON() {
        val vector = floatArrayOf(0.1f, 0.2f, 0.3f)
        val json = SQLiteVec.toJSON(database, vector)
        
        assertNotNull("JSON should not be null", json)
        assertTrue("JSON should contain numbers", json.contains("0.1"))
        assertTrue("JSON should be array format", json.startsWith("[") && json.endsWith("]"))
    }
    
    @Test
    fun testVectorUtilityFunctions() {
        val vector1 = floatArrayOf(2.0f, 3.0f)
        val vector2 = floatArrayOf(1.0f, 1.0f)
        
        // Test vector addition
        val sum = SQLiteVecUtils.add(database, vector1, vector2)
        assertEquals("Sum should have 2 elements", 2, sum.size)
        assertEquals("First element should be 3.0", 3.0f, sum[0], 0.001f)
        assertEquals("Second element should be 4.0", 4.0f, sum[1], 0.001f)
        
        // Test vector subtraction
        val diff = SQLiteVecUtils.subtract(database, vector1, vector2)
        assertEquals("Difference should have 2 elements", 2, diff.size)
        assertEquals("First element should be 1.0", 1.0f, diff[0], 0.001f)
        assertEquals("Second element should be 2.0", 2.0f, diff[1], 0.001f)
    }
    
    @Test
    fun testDatabaseExtensions() {
        // Test convenience extension methods
        assertTrue("Should be available after loading", database.isSQLiteVecAvailable())
        
        val version = database.sqliteVecVersion()
        assertTrue("Version should start with 'v'", version.startsWith("v"))
        
        // Test vector operations through extensions
        val vector = floatArrayOf(3.0f, 4.0f)
        val length = database.vectorLength(vector)
        assertEquals("Vector length should be 2", 2, length)
        
        val normalized = database.normalizeVector(vector)
        assertEquals("Normalized vector should have 2 dimensions", 2, normalized.size)
    }
    
    @Test
    fun testErrorHandling() {
        // Test that appropriate errors are thrown for invalid operations
        try {
            // Try to use vector operations on a database without sqlite-vec (shouldn't happen in our test)
            val emptyDatabase = SQLiteDatabase.openDatabase(":memory:", null, SQLiteDatabase.OPEN_READWRITE)
            val vector = floatArrayOf(1.0f, 2.0f)
            
            // This should throw an exception because sqlite-vec is not loaded
            SQLiteVec.distance(emptyDatabase, vector, vector)
            fail("Should have thrown an exception")
        } catch (e: Exception) {
            assertTrue("Should be a SQLiteException", e is android.database.sqlite.SQLiteException)
        }
    }
    
    @Test
    fun testLargeVectorOperations() {
        // Test with larger, more realistic vector dimensions
        SQLiteVec.createVectorTable(database, "large_vectors", "embedding float[384]")
        
        val vector = FloatArray(384) { i -> (i * 0.001f) }
        val vectorData = SQLiteVec.serialize(vector)
        
        database.execSQL(
            "INSERT INTO large_vectors(rowid, embedding) VALUES (?, ?)",
            arrayOf(1, vectorData)
        )
        
        val queryVector = FloatArray(384) { i -> (i * 0.001f + 0.01f) }
        val results = SQLiteVec.searchSimilar(database, "large_vectors", "embedding", queryVector, 1)
        
        assertEquals("Should return 1 result", 1, results.size)
    }
    
    @Test
    fun testDifferentVectorTypes() {
        // Test with different vector data types
        val float32Vector = floatArrayOf(0.1f, 0.2f, 0.3f)
        val float64Vector = doubleArrayOf(0.1, 0.2, 0.3)
        val int8Vector = byteArrayOf(1, 2, 3)
        
        val float32Data = SQLiteVec.serialize(float32Vector)
        val float64Data = SQLiteVec.serialize(float64Vector)
        val int8Data = SQLiteVec.serialize(int8Vector)
        
        assertEquals("Float32 should be 12 bytes", 12, float32Data.size)
        assertEquals("Float64 should be 24 bytes", 24, float64Data.size)
        assertEquals("Int8 should be 3 bytes", 3, int8Data.size)
        
        // Test round-trip
        val deserializedFloat32 = SQLiteVec.deserializeFloat32(float32Data)
        val deserializedInt8 = SQLiteVec.deserializeInt8(int8Data)
        
        assertArrayEquals("Float32 round-trip should work", float32Vector, deserializedFloat32, 0.001f)
        assertArrayEquals("Int8 round-trip should work", int8Vector, deserializedInt8)
    }
    
    @Test
    fun testVectorDistanceDefaultMetric() {
        val vector1 = floatArrayOf(1.0f, 0.0f, 0.0f)
        val vector2 = floatArrayOf(0.0f, 1.0f, 0.0f)
        
        val distance1 = SQLiteVec.distance(database, vector1, vector2, "cosine")
        val distance2 = SQLiteVec.distance(database, vector1, vector2) // should default to cosine
        
        assertEquals("Default metric should be cosine", distance1, distance2, 0.001)
    }
    
    @Test
    fun testVectorNormalize() {
        val vector = floatArrayOf(3.0f, 4.0f) // Length 5 vector
        val normalized = SQLiteVec.normalize(database, vector)
        
        assertEquals("Normalized vector should have 2 elements", 2, normalized.size)
        
        // Check that normalized vector has unit length
        val magnitude = kotlin.math.sqrt(normalized[0] * normalized[0] + normalized[1] * normalized[1])
        assertEquals("Normalized vector should have unit length", 1.0f, magnitude, 0.001f)
    }
    
    @Test
    fun testCreateVectorTable() {
        SQLiteVec.createVectorTable(database, "test_vectors", "embedding float[4]")
        
        // Verify table was created by inserting and querying
        val vector = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f)
        val vectorData = SQLiteVec.serialize(vector)
        
        database.execSQL("INSERT INTO test_vectors(rowid, embedding) VALUES (?, ?)", arrayOf(1, vectorData))
        
        database.rawQuery("SELECT COUNT(*) FROM test_vectors", null).use { cursor ->
            assertTrue("Cursor should have data", cursor.moveToFirst())
            assertEquals("Should have 1 row", 1L, cursor.getLong(0))
        }
    }
    
    @Test
    fun testCreateVectorTableWithAdditionalColumns() {
        SQLiteVec.createVectorTable(
            database,
            "test_docs",
            "embedding float[4]",
            "title TEXT",
            "content TEXT"
        )
        
        // Verify table was created with additional columns
        val vector = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f)
        val vectorData = SQLiteVec.serialize(vector)
        
        database.execSQL(
            "INSERT INTO test_docs(rowid, embedding, title, content) VALUES (?, ?, ?, ?)",
            arrayOf(1, vectorData, "Test Title", "Test Content")
        )
        
        database.rawQuery("SELECT title, content FROM test_docs WHERE rowid = 1", null).use { cursor ->
            assertTrue("Cursor should have data", cursor.moveToFirst())
            assertEquals("Title should match", "Test Title", cursor.getString(0))
            assertEquals("Content should match", "Test Content", cursor.getString(1))
        }
    }
    
    @Test
    fun testVectorSearch() {
        // Create test table
        SQLiteVec.createVectorTable(
            database,
            "search_test",
            "embedding float[3]",
            "title TEXT"
        )
        
        // Insert test vectors
        val vectors = listOf(
            Triple(1L, floatArrayOf(1.0f, 0.0f, 0.0f), "Vector 1"),
            Triple(2L, floatArrayOf(0.0f, 1.0f, 0.0f), "Vector 2"),
            Triple(3L, floatArrayOf(0.0f, 0.0f, 1.0f), "Vector 3"),
            Triple(4L, floatArrayOf(0.5f, 0.5f, 0.0f), "Vector 4")
        )
        
        for ((id, vector, title) in vectors) {
            val vectorData = SQLiteVec.serialize(vector)
            database.execSQL(
                "INSERT INTO search_test(rowid, embedding, title) VALUES (?, ?, ?)",
                arrayOf(id, vectorData, title)
            )
        }
        
        // Search for similar vectors
        val queryVector = floatArrayOf(1.0f, 0.1f, 0.0f)
        val results = SQLiteVec.searchSimilar(
            database,
            "search_test",
            "embedding",
            queryVector,
            2,
            "title"
        )
        
        assertEquals("Should return 2 results", 2, results.size)
        
        // First result should be Vector 1 (closest to query)
        val firstResult = results[0]
        assertEquals("First result should be Vector 1", 1L, firstResult["rowid"] as Long)
        assertEquals("First result title should match", "Vector 1", firstResult["title"] as String)
        
        // Check that distance is included
        assertTrue("First result should have distance", firstResult.containsKey("distance"))
        assertTrue("Distance should be a number", firstResult["distance"] is Double)
    }
    
    @Test
    fun testSearchSimilarDefaultLimit() {
        // Create test table
        SQLiteVec.createVectorTable(database, "limit_test", "embedding float[2]")
        
        // Insert more than 10 vectors
        for (i in 1..15) {
            val vector = floatArrayOf(i * 0.1f, i * 0.1f)
            val vectorData = SQLiteVec.serialize(vector)
            database.execSQL("INSERT INTO limit_test(rowid, embedding) VALUES (?, ?)", arrayOf(i, vectorData))
        }
        
        // Search with default limit
        val queryVector = floatArrayOf(0.5f, 0.5f)
        val results = SQLiteVec.searchSimilar(database, "limit_test", "embedding", queryVector)
        
        assertEquals("Should return default limit of 10 results", 10, results.size)
    }
    
    @Test(expected = SQLiteException::class)
    fun testLoadInvalidDatabase() {
        // This should throw an exception
        val invalidDb = SQLiteDatabase.openDatabase(":memory:", null, SQLiteDatabase.OPEN_READONLY)
        SQLiteVec.load(invalidDb)
    }
    
    @Test
    fun testSerializeEmptyVector() {
        val vector = floatArrayOf()
        val data = SQLiteVec.serialize(vector)
        assertEquals("Empty vector should serialize to 0 bytes", 0, data.size)
        
        val deserialized = SQLiteVec.deserializeFloat32(data)
        assertEquals("Deserialized empty vector should have 0 elements", 0, deserialized.size)
    }
    
    @Test
    fun testLargeVector() {
        // Test with a larger vector (512 dimensions)
        val vector = FloatArray(512) { i -> i * 0.001f }
        val data = SQLiteVec.serialize(vector)
        assertEquals("Large vector should serialize correctly", 512 * 4, data.size)
        
        val deserialized = SQLiteVec.deserializeFloat32(data)
        assertEquals("Deserialized large vector should have correct size", 512, deserialized.size)
        assertArrayEquals("Deserialized values should match", vector, deserialized, 0.0001f)
    }
}
