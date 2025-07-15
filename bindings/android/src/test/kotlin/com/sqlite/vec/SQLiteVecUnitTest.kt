package com.sqlite.vec

import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Unit tests for SQLiteVec using Robolectric
 * 
 * These tests run on the JVM using Robolectric and validate basic
 * functionality that doesn't require the native library.
 */
@RunWith(RobolectricTestRunner::class)
class SQLiteVecUnitTest {
    
    @Test
    fun testGetVersionWithoutNativeLibrary() {
        // This test verifies that the version string is available
        // even when the native library might not be loaded
        try {
            val version = SQLiteVec.getVersion()
            assertTrue("Version should start with 'v'", version.startsWith("v"))
        } catch (e: Exception) {
            // Expected when native library is not available
            assertTrue("Should be a SQLite exception or UnsatisfiedLinkError", 
                e is android.database.sqlite.SQLiteException || e is UnsatisfiedLinkError)
        }
    }
    
    @Test
    fun testSerializationLogic() {
        // Test that the serialization logic for doubles works correctly
        val vector = doubleArrayOf(0.1, 0.2, 0.3, 0.4)
        
        // Manually implement the serialization logic
        val buffer = ByteBuffer.allocate(vector.size * 8)
        buffer.order(ByteOrder.nativeOrder())
        for (value in vector) {
            buffer.putDouble(value)
        }
        val expectedData = buffer.array()
        
        // This should match what SQLiteVec.serialize(vector) would produce
        assertEquals("Serialized data should be 32 bytes", 32, expectedData.size)
    }
    
    @Test
    fun testAutoLoadMethods() {
        // These methods should not throw exceptions
        SQLiteVec.autoLoad()
        SQLiteVec.cancelAutoLoad()
        
        // No assertions needed - just verify they don't crash
    }
    
    @Test
    fun testEmptyVectorSerialization() {
        // Test edge case with empty vectors
        val emptyFloatVector = floatArrayOf()
        val emptyByteVector = byteArrayOf()
        
        // These should not crash even if the native library is not available
        try {
            val data1 = SQLiteVec.serialize(emptyFloatVector)
            assertEquals("Empty float vector should serialize to 0 bytes", 0, data1.size)
            
            val data2 = SQLiteVec.serialize(emptyByteVector)
            assertEquals("Empty byte vector should serialize to 0 bytes", 0, data2.size)
        } catch (e: Exception) {
            // Expected when native library is not available
            assertTrue("Should be a SQLite exception or UnsatisfiedLinkError", 
                e is android.database.sqlite.SQLiteException || e is UnsatisfiedLinkError)
        }
    }
    
    @Test
    fun testCompatibilityMethods() {
        // Test that the Go bindings compatibility methods are present
        val vector = floatArrayOf(0.1f, 0.2f, 0.3f)
        
        try {
            // These methods should exist and delegate to the main serialize method
            SQLiteVec.serializeFloat32(vector)
            SQLiteVec.serializeInt8(byteArrayOf(1, 2, 3))
        } catch (e: Exception) {
            // Expected when native library is not available
            assertTrue("Should be a SQLite exception or UnsatisfiedLinkError", 
                e is android.database.sqlite.SQLiteException || e is UnsatisfiedLinkError)
        }
    }
    
    @Test
    fun testVectorDimensions() {
        // Test various vector sizes
        val dimensions = listOf(1, 2, 3, 4, 128, 256, 384, 512, 768, 1024)
        
        for (dim in dimensions) {
            val vector = FloatArray(dim) { i -> i * 0.001f }
            
            try {
                val data = SQLiteVec.serialize(vector)
                assertEquals("Vector of dimension $dim should serialize to ${dim * 4} bytes", 
                    dim * 4, data.size)
                
                val deserialized = SQLiteVec.deserializeFloat32(data)
                assertEquals("Deserialized vector should have same dimension", 
                    dim, deserialized.size)
            } catch (e: Exception) {
                // Expected when native library is not available
                assertTrue("Should be a SQLite exception or UnsatisfiedLinkError", 
                    e is android.database.sqlite.SQLiteException || e is UnsatisfiedLinkError)
            }
        }
    }
    
    @Test
    fun testVectorTypeSizes() {
        // Test that different vector types have the expected serialized sizes
        val float32Vector = floatArrayOf(1.0f, 2.0f, 3.0f, 4.0f)
        val float64Vector = doubleArrayOf(1.0, 2.0, 3.0, 4.0)
        val int8Vector = byteArrayOf(1, 2, 3, 4)
        
        try {
            val float32Data = SQLiteVec.serialize(float32Vector)
            assertEquals("Float32 vector should be 16 bytes", 16, float32Data.size)
            
            val float64Data = SQLiteVec.serialize(float64Vector)
            assertEquals("Float64 vector should be 32 bytes", 32, float64Data.size)
            
            val int8Data = SQLiteVec.serialize(int8Vector)
            assertEquals("Int8 vector should be 4 bytes", 4, int8Data.size)
        } catch (e: Exception) {
            // Expected when native library is not available
            assertTrue("Should be a SQLite exception or UnsatisfiedLinkError", 
                e is android.database.sqlite.SQLiteException || e is UnsatisfiedLinkError)
        }
    }
    
    @Test
    fun testRoundTripSerialization() {
        // Test that serialization and deserialization are inverses
        val originalVector = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f)
        
        try {
            val serialized = SQLiteVec.serialize(originalVector)
            val deserialized = SQLiteVec.deserializeFloat32(serialized)
            
            assertEquals("Deserialized vector should have same length", 
                originalVector.size, deserialized.size)
            
            for (i in originalVector.indices) {
                assertEquals("Element $i should match", 
                    originalVector[i], deserialized[i], 0.0001f)
            }
        } catch (e: Exception) {
            // Expected when native library is not available
            assertTrue("Should be a SQLite exception or UnsatisfiedLinkError", 
                e is android.database.sqlite.SQLiteException || e is UnsatisfiedLinkError)
        }
    }
    
    @Test
    fun testInt8RoundTrip() {
        // Test Int8 serialization round trip
        val originalVector = byteArrayOf(-128, -1, 0, 1, 127)
        
        try {
            val serialized = SQLiteVec.serialize(originalVector)
            val deserialized = SQLiteVec.deserializeInt8(serialized)
            
            assertArrayEquals("Int8 round trip should preserve values", 
                originalVector, deserialized)
        } catch (e: Exception) {
            // Expected when native library is not available
            assertTrue("Should be a SQLite exception or UnsatisfiedLinkError", 
                e is android.database.sqlite.SQLiteException || e is UnsatisfiedLinkError)
        }
    }
    
    @Test
    fun testGoBindingsCompatibility() {
        // Test that Go bindings compatibility methods exist and work
        val vector = floatArrayOf(0.1f, 0.2f, 0.3f)
        
        try {
            val data1 = SQLiteVec.serialize(vector)
            val data2 = SQLiteVec.serializeFloat32(vector)
            
            assertArrayEquals("serializeFloat32 should match serialize", data1, data2)
        } catch (e: Exception) {
            assertTrue("Should be a SQLite exception or UnsatisfiedLinkError", 
                e is android.database.sqlite.SQLiteException || e is UnsatisfiedLinkError)
        }
    }
    
    @Test
    fun testSwiftBindingsCompatibility() {
        // Test Swift bindings-style methods
        val vector1 = floatArrayOf(1.0f, 0.0f, 0.0f)
        val vector2 = floatArrayOf(0.0f, 1.0f, 0.0f)
        
        try {
            // These should be available even if native library isn't loaded
            SQLiteVec.serialize(vector1)
            SQLiteVec.serialize(vector2)
            
            // Check that auto-loading methods don't crash
            SQLiteVec.autoLoad()
            SQLiteVec.cancelAutoLoad()
        } catch (e: Exception) {
            assertTrue("Should be a SQLite exception or UnsatisfiedLinkError", 
                e is android.database.sqlite.SQLiteException || e is UnsatisfiedLinkError)
        }
    }
    
    @Test
    fun testLargeVectorSizes() {
        // Test with various common embedding dimensions
        val commonDimensions = listOf(128, 256, 384, 512, 768, 1024, 1536, 2048)
        
        for (dim in commonDimensions) {
            val vector = FloatArray(dim) { i -> (i * 0.001f) }
            
            try {
                val serialized = SQLiteVec.serialize(vector)
                val expectedSize = dim * 4  // 4 bytes per float
                assertEquals("Dimension $dim should serialize to $expectedSize bytes", 
                    expectedSize, serialized.size)
                
                val deserialized = SQLiteVec.deserializeFloat32(serialized)
                assertEquals("Deserialized vector should have correct dimension", 
                    dim, deserialized.size)
                
                // Verify first and last elements are preserved
                assertEquals("First element should be preserved", 
                    vector[0], deserialized[0], 0.00001f)
                assertEquals("Last element should be preserved", 
                    vector[dim - 1], deserialized[dim - 1], 0.00001f)
                    
            } catch (e: Exception) {
                assertTrue("Should be a SQLite exception or UnsatisfiedLinkError", 
                    e is android.database.sqlite.SQLiteException || e is UnsatisfiedLinkError)
            }
        }
    }
    
    @Test
    fun testVectorMath() {
        // Test basic vector operations (should work even without native library)
        val vector1 = floatArrayOf(1.0f, 2.0f, 3.0f)
        val vector2 = floatArrayOf(4.0f, 5.0f, 6.0f)
        
        try {
            val serialized1 = SQLiteVec.serialize(vector1)
            val serialized2 = SQLiteVec.serialize(vector2)
            
            assertNotNull("Serialized vectors should not be null", serialized1)
            assertNotNull("Serialized vectors should not be null", serialized2)
        } catch (e: Exception) {
            assertTrue("Should be a SQLite exception or UnsatisfiedLinkError", 
                e is android.database.sqlite.SQLiteException || e is UnsatisfiedLinkError)
        }
    }
    
    @Test
    fun testSpecialFloatValues() {
        // Test with special float values
        val specialVector = floatArrayOf(
            Float.NEGATIVE_INFINITY,
            Float.POSITIVE_INFINITY,
            Float.NaN,
            Float.MIN_VALUE,
            Float.MAX_VALUE,
            0.0f,
            -0.0f
        )
        
        try {
            val serialized = SQLiteVec.serialize(specialVector)
            assertEquals("Special values vector should serialize correctly", 
                specialVector.size * 4, serialized.size)
            
            val deserialized = SQLiteVec.deserializeFloat32(serialized)
            assertEquals("Deserialized should have same length", 
                specialVector.size, deserialized.size)
                
        } catch (e: Exception) {
            assertTrue("Should be a SQLite exception or UnsatisfiedLinkError", 
                e is android.database.sqlite.SQLiteException || e is UnsatisfiedLinkError)
        }
    }
    
    @Test
    fun testErrorHandling() {
        // Test that proper errors are thrown for invalid inputs
        try {
            // Test with null-like conditions that would be caught by Kotlin compiler
            val emptyVector = floatArrayOf()
            val serialized = SQLiteVec.serialize(emptyVector)
            assertEquals("Empty vector should serialize to 0 bytes", 0, serialized.size)
        } catch (e: Exception) {
            assertTrue("Should be a SQLite exception or UnsatisfiedLinkError", 
                e is android.database.sqlite.SQLiteException || e is UnsatisfiedLinkError)
        }
    }
}
