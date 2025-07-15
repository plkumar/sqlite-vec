package com.sqlite.vec

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.io.File

/**
 * Tests to ensure Android bindings behave similarly to Swift bindings
 * This validates API compatibility and similar behavior patterns
 */
@RunWith(AndroidJUnit4::class)
class SwiftCompatibilityTest {
    
    private lateinit var database: SQLiteDatabase
    private lateinit var tempDbFile: File
    
    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        tempDbFile = File.createTempFile("swift_compat", ".db", context.cacheDir)
        database = SQLiteDatabase.openDatabase(
            tempDbFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.CREATE_IF_NECESSARY
        )
        SQLiteVec.load(database)
    }
    
    @After
    fun tearDown() {
        database.close()
        tempDbFile.delete()
    }
    
    @Test
    fun testSwiftStyleVectorSerialization() {
        // Test that Android serialization matches Swift patterns
        val vector = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f)
        
        // Test Float32 serialization (Swift: [Float32])
        val serialized = SQLiteVec.serialize(vector)
        assertEquals("Should be 16 bytes like Swift", 16, serialized.size)
        
        // Test deserialization matches Swift behavior
        val deserialized = SQLiteVec.deserializeFloat32(serialized)
        assertEquals("Should have 4 elements", 4, deserialized.size)
        
        for (i in vector.indices) {
            assertEquals("Element $i should match", vector[i], deserialized[i], 0.0001f)
        }
    }
    
    @Test
    fun testSwiftStyleVectorOperations() {
        // Test vector operations similar to Swift extensions
        val vector1 = floatArrayOf(1.0f, 0.0f, 0.0f)
        val vector2 = floatArrayOf(0.0f, 1.0f, 0.0f)
        
        // Distance calculation (Swift: SQLiteVec.distance)
        val distance = SQLiteVec.distance(database, vector1, vector2, "cosine")
        assertEquals("Cosine distance should be 1.0", 1.0, distance, 0.001)
        
        // Vector length (Swift: SQLiteVec.length)
        val length = SQLiteVec.length(database, vector1)
        assertEquals("Vector length should be 3", 3, length)
        
        // Vector normalization (Swift: SQLiteVec.normalize)
        val vector = floatArrayOf(3.0f, 4.0f)
        val normalized = SQLiteVec.normalize(database, vector)
        assertEquals("Should be normalized", 0.6f, normalized[0], 0.001f)
        assertEquals("Should be normalized", 0.8f, normalized[1], 0.001f)
    }
    
    @Test
    fun testSwiftStyleTableOperations() {
        // Test table creation similar to Swift
        SQLiteVec.createVectorTable(
            database,
            "documents",
            "embedding float[384]",
            "title TEXT",
            "content TEXT"
        )
        
        // Insert vectors (Swift style)
        val embedding = FloatArray(384) { i -> i * 0.001f }
        val vectorData = SQLiteVec.serialize(embedding)
        
        database.execSQL(
            "INSERT INTO documents(rowid, embedding, title) VALUES (?, ?, ?)",
            arrayOf(1, vectorData, "Test Document")
        )
        
        // Search similar vectors (Swift: SQLiteVec.searchSimilar)
        val queryVector = FloatArray(384) { i -> i * 0.001f + 0.01f }
        val results = SQLiteVec.searchSimilar(
            database,
            "documents",
            "embedding",
            queryVector,
            5,
            "title"
        )
        
        assertEquals("Should return 1 result", 1, results.size)
        val result = results[0]
        assertEquals("Should match inserted row", 1L, result["rowid"])
    }
    
    @Test
    fun testSwiftStyleBatchOperations() {
        // Test batch operations similar to Swift insertVectorsBatch
        SQLiteVec.createVectorTable(database, "batch_docs", "embedding float[3]")
        
        val vectors = listOf(
            1L to floatArrayOf(1.0f, 0.0f, 0.0f),
            2L to floatArrayOf(0.0f, 1.0f, 0.0f),
            3L to floatArrayOf(0.0f, 0.0f, 1.0f)
        )
        
        // Swift-style batch insert
        SQLiteVec.insertVectorsBatch(database, "batch_docs", "embedding", vectors)
        
        // Verify all inserted
        val cursor = database.rawQuery("SELECT COUNT(*) FROM batch_docs", null)
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals("Should have 3 rows", 3, it.getInt(0))
        }
    }
    
    @Test
    fun testSwiftStyleConvenienceExtensions() {
        // Test that Android extensions work like Swift Connection extensions
        
        // Swift: database.loadSQLiteVec()
        // Android equivalent already tested in setUp()
        
        // Swift: SQLiteVec.isAvailable(database)
        assertTrue("Should be available", SQLiteVec.isAvailable(database))
        
        // Swift: SQLiteVec.ensureLoaded(database)
        SQLiteVec.ensureLoaded(database)
        
        // Swift: SQLiteVec.version(database)
        val version = SQLiteVec.version(database)
        assertTrue("Version should start with v", version.startsWith("v"))
    }
    
    @Test
    fun testSwiftStyleVectorMath() {
        // Test vector math operations similar to Swift utilities
        val vector1 = floatArrayOf(2.0f, 3.0f)
        val vector2 = floatArrayOf(1.0f, 1.0f)
        
        // Vector addition (Swift: SQLiteVec.add)
        val sum = SQLiteVecUtils.add(database, vector1, vector2)
        assertEquals("Sum first element", 3.0f, sum[0], 0.001f)
        assertEquals("Sum second element", 4.0f, sum[1], 0.001f)
        
        // Vector subtraction (Swift: SQLiteVec.subtract)
        val diff = SQLiteVecUtils.subtract(database, vector1, vector2)
        assertEquals("Diff first element", 1.0f, diff[0], 0.001f)
        assertEquals("Diff second element", 2.0f, diff[1], 0.001f)
        
        // Dot product (Swift: SQLiteVec.dotProduct)
        val dot = SQLiteVecUtils.dotProduct(database, vector1, vector2)
        assertTrue("Dot product should be reasonable", dot > 0.0)
    }
    
    @Test
    fun testSwiftStyleErrorHandling() {
        // Test that Android error handling is similar to Swift
        
        try {
            // Try to search non-existent table (should throw like Swift)
            SQLiteVec.searchSimilar(database, "nonexistent", "embedding", floatArrayOf(1.0f), 5)
            fail("Should have thrown exception")
        } catch (e: Exception) {
            assertTrue("Should be SQLiteException", e is android.database.sqlite.SQLiteException)
        }
        
        try {
            // Try invalid vector operation
            SQLiteVec.distance(database, floatArrayOf(), floatArrayOf(1.0f))
            fail("Should have thrown exception")
        } catch (e: Exception) {
            assertTrue("Should be SQLiteException", e is android.database.sqlite.SQLiteException)
        }
    }
    
    @Test
    fun testSwiftStyleAdvancedSearch() {
        // Test advanced search patterns similar to Swift searchSimilarWithFilter
        SQLiteVec.createVectorTable(
            database,
            "filtered_docs",
            "embedding float[2]",
            "category TEXT",
            "score REAL"
        )
        
        // Insert test data
        val testData = listOf(
            Triple(1L, floatArrayOf(1.0f, 0.0f), "tech"),
            Triple(2L, floatArrayOf(0.0f, 1.0f), "science"),
            Triple(3L, floatArrayOf(0.5f, 0.5f), "tech"),
            Triple(4L, floatArrayOf(-1.0f, 0.0f), "art")
        )
        
        for ((id, vector, category) in testData) {
            val vectorData = SQLiteVec.serialize(vector)
            database.execSQL(
                "INSERT INTO filtered_docs(rowid, embedding, category) VALUES (?, ?, ?)",
                arrayOf(id, vectorData, category)
            )
        }
        
        // Search with filter (Swift-style)
        val queryVector = floatArrayOf(0.8f, 0.2f)
        val results = SQLiteVecUtils.searchSimilarWithFilter(
            database,
            "filtered_docs",
            "embedding",
            queryVector,
            10,
            "category = ?",
            arrayOf("tech")
        )
        
        assertEquals("Should return 2 tech results", 2, results.size)
        
        // Verify all results are from 'tech' category
        for (result in results) {
            val rowId = result["rowid"] as Long
            assertTrue("Should be tech category rows", rowId == 1L || rowId == 3L)
        }
    }
    
    @Test
    fun testSwiftStyleSerializationCompatibility() {
        // Test different vector types like Swift
        
        // Float32 array (Swift: [Float32])
        val float32Vector = floatArrayOf(0.1f, 0.2f, 0.3f)
        val float32Data = SQLiteVec.serialize(float32Vector)
        assertEquals("Float32 should be 12 bytes", 12, float32Data.size)
        
        // Double array (Swift: [Float64])
        val doubleVector = doubleArrayOf(0.1, 0.2, 0.3)
        val doubleData = SQLiteVec.serialize(doubleVector)
        assertEquals("Double should be 24 bytes", 24, doubleData.size)
        
        // Int8 array (Swift: [Int8])
        val int8Vector = byteArrayOf(1, 2, 3)
        val int8Data = SQLiteVec.serialize(int8Vector)
        assertEquals("Int8 should be 3 bytes", 3, int8Data.size)
        
        // Test compatibility methods (Swift compatibility)
        val float32CompatData = SQLiteVec.serializeFloat32(float32Vector)
        assertArrayEquals("serializeFloat32 should match serialize", float32Data, float32CompatData)
        
        val int8CompatData = SQLiteVec.serializeInt8(int8Vector)
        assertArrayEquals("serializeInt8 should match serialize", int8Data, int8CompatData)
    }
    
    @Test
    fun testSwiftStyleAutoLoadingPattern() {
        // Test auto-loading patterns (Swift has autoLoad/cancelAutoLoad)
        SQLiteVec.autoLoad()
        SQLiteVec.cancelAutoLoad()
        
        // These should not throw exceptions (placeholder implementation)
        // In a real implementation, this would affect future database connections
    }
    
    @Test
    fun testSwiftStyleJSONConversion() {
        // Test JSON conversion similar to Swift toJSON
        val vector = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f)
        val json = SQLiteVec.toJSON(database, vector)
        
        assertNotNull("JSON should not be null", json)
        assertTrue("JSON should be array format", json.startsWith("[") && json.endsWith("]"))
        
        // Should contain the vector values
        assertTrue("Should contain 0.1", json.contains("0.1"))
        assertTrue("Should contain 0.4", json.contains("0.4"))
    }
}
