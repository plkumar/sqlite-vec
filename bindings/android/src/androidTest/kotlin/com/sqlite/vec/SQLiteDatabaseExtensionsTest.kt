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
 * Comprehensive instrumented tests for SQLite database extensions
 * Tests the convenience extension methods similar to Swift bindings
 */
@RunWith(AndroidJUnit4::class)
class SQLiteDatabaseExtensionsTest {
    
    private lateinit var database: SQLiteDatabase
    private lateinit var tempDbFile: File
    
    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Create temporary database file
        tempDbFile = File.createTempFile("test_extensions", ".db", context.cacheDir)
        
        // Open database
        database = SQLiteDatabase.openDatabase(
            tempDbFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.CREATE_IF_NECESSARY
        )
        
        // Load sqlite-vec extension using extension method
        database.loadSQLiteVec()
    }
    
    @After
    fun tearDown() {
        database.close()
        tempDbFile.delete()
    }
    
    @Test
    fun testDatabaseExtensionLoading() {
        // Test that extension methods work for loading
        assertTrue("SQLiteVec should be available", database.isSQLiteVecAvailable())
        
        val version = database.sqliteVecVersion()
        assertTrue("Version should start with 'v'", version.startsWith("v"))
        
        // Test ensure loaded (should not fail since already loaded)
        database.ensureSQLiteVecLoaded()
        assertTrue("Should still be available", database.isSQLiteVecAvailable())
    }
    
    @Test
    fun testVectorTableCreationExtensions() {
        // Test creating vector table through extension
        database.createVectorTable(
            "test_docs",
            "embedding float[128]",
            "title TEXT",
            "content TEXT",
            "category TEXT"
        )
        
        // Verify table exists and has correct structure
        val cursor = database.rawQuery(
            "SELECT sql FROM sqlite_master WHERE type='table' AND name='test_docs'",
            null
        )
        
        cursor.use {
            assertTrue("Table should exist", it.moveToFirst())
            val createSql = it.getString(0)
            assertTrue("Should be vec0 table", createSql.contains("vec0"))
            assertTrue("Should have embedding column", createSql.contains("embedding float[128]"))
        }
    }
    
    @Test
    fun testVectorOperationsExtensions() {
        val vector1 = floatArrayOf(3.0f, 4.0f)
        val vector2 = floatArrayOf(1.0f, 0.0f)
        
        // Test vector distance using extension
        val distance = database.vectorDistance(vector1, vector2, "cosine")
        assertTrue("Distance should be positive", distance >= 0.0)
        
        // Test vector length using extension
        val length = database.vectorLength(vector1)
        assertEquals("Vector should have 2 dimensions", 2, length)
        
        // Test vector normalization using extension
        val normalized = database.normalizeVector(vector1)
        assertEquals("Normalized vector should have 2 dimensions", 2, normalized.size)
        
        // Verify normalization (3,4) -> (0.6, 0.8)
        assertEquals("First component should be 0.6", 0.6f, normalized[0], 0.001f)
        assertEquals("Second component should be 0.8", 0.8f, normalized[1], 0.001f)
    }
    
    @Test
    fun testVectorSearchExtensions() {
        // Create table and insert test data
        database.createVectorTable("search_test", "embedding float[3]")
        
        val testVectors = listOf(
            1L to floatArrayOf(1.0f, 0.0f, 0.0f),
            2L to floatArrayOf(0.0f, 1.0f, 0.0f),
            3L to floatArrayOf(0.0f, 0.0f, 1.0f),
            4L to floatArrayOf(0.7f, 0.7f, 0.0f)
        )
        
        // Insert using batch extension
        database.insertVectorsBatch("search_test", "embedding", testVectors)
        
        // Search using extension
        val queryVector = floatArrayOf(0.8f, 0.6f, 0.0f)
        val results = database.searchSimilar("search_test", "embedding", queryVector, 2)
        
        assertEquals("Should return 2 results", 2, results.size)
        
        val firstResult = results[0]
        val firstRowId = firstResult["rowid"] as Long
        assertTrue("First result should be row 4 or 1", firstRowId == 4L || firstRowId == 1L)
    }
    
    @Test
    fun testVectorJSONExtension() {
        val vector = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f)
        val json = database.vectorToJSON(vector)
        
        assertNotNull("JSON should not be null", json)
        assertTrue("JSON should start with [", json.startsWith("["))
        assertTrue("JSON should end with ]", json.endsWith("]"))
        assertTrue("JSON should contain vector values", json.contains("0.1"))
    }
    
    @Test
    fun testBatchInsertExtension() {
        database.createVectorTable("batch_extension_test", "embedding float[2]")
        
        val vectors = (1..100).map { i ->
            i.toLong() to floatArrayOf(i * 0.01f, (i + 1) * 0.01f)
        }
        
        // Test batch insert through extension
        database.insertVectorsBatch("batch_extension_test", "embedding", vectors)
        
        val cursor = database.rawQuery("SELECT COUNT(*) FROM batch_extension_test", null)
        cursor.use {
            assertTrue("Should have results", it.moveToFirst())
            val count = it.getInt(0)
            assertEquals("Should have 100 rows", 100, count)
        }
    }
    
    @Test
    fun testExtensionChaining() {
        // Test that extension methods can be chained nicely
        val vector = floatArrayOf(2.0f, 1.0f)
        
        // Create table, normalize vector, convert to JSON - all through extensions
        database.createVectorTable("chain_test", "embedding float[2]")
        
        val normalized = database.normalizeVector(vector)
        val json = database.vectorToJSON(normalized)
        val length = database.vectorLength(normalized)
        
        assertEquals("Normalized vector should have 2 dimensions", 2, length)
        assertNotNull("JSON conversion should work", json)
        assertTrue("JSON should contain normalized values", json.contains("0.89") || json.contains("0.44"))
    }
    
    @Test
    fun testExtensionErrorHandling() {
        // Test that extensions properly handle errors
        val emptyVector = floatArrayOf()
        
        try {
            database.vectorLength(emptyVector)
            // This might succeed with length 0, depending on implementation
        } catch (e: SQLiteException) {
            // This is also acceptable - depends on sqlite-vec behavior
            assertNotNull("Exception should have a message", e.message)
        }
        
        // Test invalid table operations
        try {
            database.searchSimilar("nonexistent_table", "embedding", floatArrayOf(1.0f), 5)
            fail("Should have thrown an exception for nonexistent table")
        } catch (e: SQLiteException) {
            assertTrue("Should be about missing table", 
                e.message?.contains("no such table") == true || 
                e.message?.contains("nonexistent_table") == true)
        }
    }
    
    @Test
    fun testExtensionPerformance() {
        // Test that extension methods don't add significant overhead
        database.createVectorTable("perf_test", "embedding float[128]")
        
        val vectors = (1..1000).map { i ->
            i.toLong() to FloatArray(128) { j -> (i * j * 0.0001f) }
        }
        
        val startTime = System.currentTimeMillis()
        
        // Batch insert through extension
        database.insertVectorsBatch("perf_test", "embedding", vectors)
        
        val insertTime = System.currentTimeMillis() - startTime
        
        // Search through extension
        val queryVector = FloatArray(128) { i -> i * 0.001f }
        val searchStartTime = System.currentTimeMillis()
        
        val results = database.searchSimilar("perf_test", "embedding", queryVector, 10)
        
        val searchTime = System.currentTimeMillis() - searchStartTime
        
        // Verify operations completed in reasonable time (very lenient bounds)
        assertTrue("Insert should complete in under 10 seconds", insertTime < 10000)
        assertTrue("Search should complete in under 5 seconds", searchTime < 5000)
        assertEquals("Should return 10 results", 10, results.size)
    }
}
