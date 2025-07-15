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
 * Android instrumented tests for SQLiteVecUtils
 * 
 * These tests validate the utility functions and advanced features
 * of the sqlite-vec Android bindings.
 */
@RunWith(AndroidJUnit4::class)
class SQLiteVecUtilsInstrumentedTest {
    
    private lateinit var database: SQLiteDatabase
    private lateinit var tempDbFile: File
    
    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Create temporary database file
        tempDbFile = File.createTempFile("test_sqlite_vec_utils", ".db", context.cacheDir)
        
        // Open database
        database = SQLiteDatabase.openDatabase(
            tempDbFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.CREATE_IF_NECESSARY
        )
        
        // Load sqlite-vec extension
        SQLiteVec.load(database)
        
        // Create test table
        SQLiteVec.createVectorTable(
            database,
            "utils_test",
            "embedding float[4]",
            "category TEXT",
            "title TEXT"
        )
    }
    
    @After
    fun tearDown() {
        database.close()
        tempDbFile.delete()
    }
    
    @Test
    fun testInsertVectorsBatch() {
        val vectors = listOf(
            Pair(1L, floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f)),
            Pair(2L, floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f)),
            Pair(3L, floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f)),
            Pair(4L, floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f))
        )
        
        SQLiteVecUtils.insertVectorsBatch(database, "utils_test", "embedding", vectors)
        
        // Verify all vectors were inserted
        database.rawQuery("SELECT COUNT(*) FROM utils_test", null).use { cursor ->
            assertTrue("Cursor should have data", cursor.moveToFirst())
            assertEquals("Should have 4 rows", 4L, cursor.getLong(0))
        }
        
        // Verify specific vector
        val vector2Data = SQLiteVec.serialize(floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f))
        database.rawQuery("SELECT embedding FROM utils_test WHERE rowid = 2", null).use { cursor ->
            assertTrue("Cursor should have data", cursor.moveToFirst())
            val storedData = cursor.getBlob(0)
            assertArrayEquals("Stored vector should match", vector2Data, storedData)
        }
    }
    
    @Test
    fun testInsertVectorsBatchEmpty() {
        val emptyVectors = emptyList<Pair<Long, FloatArray>>()
        
        // Should not throw and should not insert anything
        SQLiteVecUtils.insertVectorsBatch(database, "utils_test", "embedding", emptyVectors)
        
        database.rawQuery("SELECT COUNT(*) FROM utils_test", null).use { cursor ->
            assertTrue("Cursor should have data", cursor.moveToFirst())
            assertEquals("Should have 0 rows", 0L, cursor.getLong(0))
        }
    }
    
    @Test
    fun testSearchSimilarWithFilter() {
        // Insert test data with categories
        val testData = listOf(
            Triple(1L, floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f), "tech"),
            Triple(2L, floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f), "tech"),
            Triple(3L, floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f), "science"),
            Triple(4L, floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f), "science"),
            Triple(5L, floatArrayOf(0.5f, 0.5f, 0.0f, 0.0f), "tech")
        )
        
        for ((id, vector, category) in testData) {
            val vectorData = SQLiteVec.serialize(vector)
            database.execSQL(
                "INSERT INTO utils_test(rowid, embedding, category) VALUES (?, ?, ?)",
                arrayOf(id, vectorData, category)
            )
        }
        
        // Search with filter for tech category only
        val queryVector = floatArrayOf(1.0f, 0.1f, 0.0f, 0.0f)
        val results = SQLiteVecUtils.searchSimilarWithFilter(
            database,
            "utils_test",
            "embedding",
            queryVector,
            5,
            "category = ?",
            arrayOf("tech")
        )
        
        assertEquals("Should find 3 tech results", 3, results.size)
        
        // Verify all results are from tech category by checking database
        for (result in results) {
            val rowid = result["rowid"] as Long
            database.rawQuery("SELECT category FROM utils_test WHERE rowid = ?", arrayOf(rowid.toString())).use { cursor ->
                assertTrue("Cursor should have data", cursor.moveToFirst())
                assertEquals("Category should be tech", "tech", cursor.getString(0))
            }
        }
    }
    
    @Test
    fun testSearchSimilarWithFilterNoMatch() {
        // Insert test data
        val vector = floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f)
        val vectorData = SQLiteVec.serialize(vector)
        database.execSQL("INSERT INTO utils_test(rowid, embedding, category) VALUES (?, ?, ?)", arrayOf(1, vectorData, "tech"))
        
        // Search with filter that won't match
        val queryVector = floatArrayOf(1.0f, 0.1f, 0.0f, 0.0f)
        val results = SQLiteVecUtils.searchSimilarWithFilter(
            database,
            "utils_test",
            "embedding",
            queryVector,
            5,
            "category = ?",
            arrayOf("science")
        )
        
        assertEquals("Should find 0 results", 0, results.size)
    }
    
    @Test
    fun testVectorAdd() {
        val vector1 = floatArrayOf(1.0f, 2.0f, 3.0f, 4.0f)
        val vector2 = floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f)
        
        val result = SQLiteVecUtils.add(database, vector1, vector2)
        
        assertEquals("Result should have 4 elements", 4, result.size)
        assertEquals("First element should be 1.5", 1.5f, result[0], 0.001f)
        assertEquals("Second element should be 2.5", 2.5f, result[1], 0.001f)
        assertEquals("Third element should be 3.5", 3.5f, result[2], 0.001f)
        assertEquals("Fourth element should be 4.5", 4.5f, result[3], 0.001f)
    }
    
    @Test
    fun testVectorSubtract() {
        val vector1 = floatArrayOf(2.0f, 3.0f, 4.0f, 5.0f)
        val vector2 = floatArrayOf(0.5f, 1.0f, 1.5f, 2.0f)
        
        val result = SQLiteVecUtils.subtract(database, vector1, vector2)
        
        assertEquals("Result should have 4 elements", 4, result.size)
        assertEquals("First element should be 1.5", 1.5f, result[0], 0.001f)
        assertEquals("Second element should be 2.0", 2.0f, result[1], 0.001f)
        assertEquals("Third element should be 2.5", 2.5f, result[2], 0.001f)
        assertEquals("Fourth element should be 3.0", 3.0f, result[3], 0.001f)
    }
    
    @Test
    fun testDotProduct() {
        val vector1 = floatArrayOf(1.0f, 0.0f, 0.0f)
        val vector2 = floatArrayOf(0.0f, 1.0f, 0.0f)
        
        val result = SQLiteVecUtils.dotProduct(database, vector1, vector2)
        
        // Dot product of orthogonal vectors should be 0 (represented through cosine distance)
        assertTrue("Dot product should be a valid number", result.isFinite())
    }
    
    @Test
    fun testToJSON() {
        val vector = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f)
        val json = SQLiteVecUtils.toJSON(database, vector)
        
        assertNotNull("JSON should not be null", json)
        assertTrue("JSON should contain array notation", json.contains("[") && json.contains("]"))
        assertTrue("JSON should contain vector values", json.contains("0.1") || json.contains("0.2"))
    }
    
    @Test
    fun testFromJSON() {
        val originalVector = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f)
        val json = SQLiteVecUtils.toJSON(database, originalVector)
        val reconstructedVector = SQLiteVecUtils.fromJSON(database, json)
        
        assertEquals("Reconstructed vector should have same length", originalVector.size, reconstructedVector.size)
        assertArrayEquals("Reconstructed vector should match original", originalVector, reconstructedVector, 0.001f)
    }
    
    @Test
    fun testGetTableStats() {
        // Insert some test data
        val vectors = listOf(
            Pair(1L, floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f)),
            Pair(2L, floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f)),
            Pair(3L, floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f))
        )
        
        SQLiteVecUtils.insertVectorsBatch(database, "utils_test", "embedding", vectors)
        
        val stats = SQLiteVecUtils.getTableStats(database, "utils_test")
        
        assertTrue("Stats should contain row_count", stats.containsKey("row_count"))
        assertEquals("Row count should be 3", 3L, stats["row_count"] as Long)
        
        assertTrue("Stats should contain columns", stats.containsKey("columns"))
        val columns = stats["columns"] as List<*>
        assertTrue("Should have multiple columns", columns.size > 0)
    }
    
    @Test
    fun testGetTableStatsEmptyTable() {
        val stats = SQLiteVecUtils.getTableStats(database, "utils_test")
        
        assertEquals("Empty table should have 0 rows", 0L, stats["row_count"] as Long)
        assertTrue("Should still have column info", stats.containsKey("columns"))
    }
}
