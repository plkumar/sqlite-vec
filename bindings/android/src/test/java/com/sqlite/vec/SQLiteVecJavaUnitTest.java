package com.sqlite.vec;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;

/**
 * Unit tests for SQLiteVecJava using Robolectric
 * 
 * These tests run on the JVM using Robolectric and validate basic
 * functionality that doesn't require the native library.
 */
@RunWith(RobolectricTestRunner.class)
public class SQLiteVecJavaUnitTest {
    
    @Test
    public void testVersionMethodExists() {
        // Test that the version method exists and can be called
        try {
            String version = SQLiteVecJava.getVersion();
            assertTrue("Version should start with 'v'", version.startsWith("v"));
        } catch (Exception e) {
            // Expected when native library is not available
            assertTrue("Should be a SQLite exception or UnsatisfiedLinkError", 
                e instanceof android.database.sqlite.SQLiteException || e instanceof UnsatisfiedLinkError);
        }
    }
    
    @Test
    public void testAutoLoadMethods() {
        // These methods should not throw exceptions
        SQLiteVecJava.autoLoad();
        SQLiteVecJava.cancelAutoLoad();
        
        // No assertions needed - just verify they don't crash
    }
    
    @Test
    public void testVectorPairConstructor() {
        long id = 42L;
        float[] vector = {0.1f, 0.2f, 0.3f};
        
        SQLiteVecJava.VectorPair pair = new SQLiteVecJava.VectorPair(id, vector);
        
        assertEquals("ID should match", id, pair.getId());
        assertArrayEquals("Vector should match", vector, pair.getVector(), 0.001f);
    }
    
    @Test
    public void testSerializationMethodsExist() {
        // Test that serialization methods exist and can be called
        float[] floatVector = {0.1f, 0.2f, 0.3f, 0.4f};
        double[] doubleVector = {0.1, 0.2, 0.3, 0.4};
        byte[] byteVector = {1, 2, 3, 4, 5};
        
        try {
            // These methods should exist
            SQLiteVecJava.serialize(floatVector);
            SQLiteVecJava.serialize(doubleVector);
            SQLiteVecJava.serialize(byteVector);
            SQLiteVecJava.serializeFloat32(floatVector);
            SQLiteVecJava.serializeInt8(byteVector);
        } catch (Exception e) {
            // Expected when native library is not available
            assertTrue("Should be a SQLite exception or UnsatisfiedLinkError", 
                e instanceof android.database.sqlite.SQLiteException || e instanceof UnsatisfiedLinkError);
        }
    }
    
    @Test
    public void testEmptyVectorHandling() {
        // Test edge cases with empty vectors
        float[] emptyFloatVector = {};
        byte[] emptyByteVector = {};
        
        try {
            // These should not crash even if the native library is not available
            SQLiteVecJava.serialize(emptyFloatVector);
            SQLiteVecJava.serialize(emptyByteVector);
        } catch (Exception e) {
            // Expected when native library is not available
            assertTrue("Should be a SQLite exception or UnsatisfiedLinkError", 
                e instanceof android.database.sqlite.SQLiteException || e instanceof UnsatisfiedLinkError);
        }
    }
    
    @Test
    public void testVectorDimensionValidation() {
        // Test various vector sizes
        int[] dimensions = {1, 2, 3, 4, 128, 256, 384, 512, 768, 1024};
        
        for (int dim : dimensions) {
            float[] vector = new float[dim];
            for (int i = 0; i < dim; i++) {
                vector[i] = i * 0.001f;
            }
            
            try {
                byte[] data = SQLiteVecJava.serialize(vector);
                // If serialization succeeds, check the size
                assertEquals("Vector of dimension " + dim + " should serialize to " + (dim * 4) + " bytes", 
                    dim * 4, data.length);
                
                float[] deserialized = SQLiteVecJava.deserializeFloat32(data);
                assertEquals("Deserialized vector should have same dimension", 
                    dim, deserialized.length);
            } catch (Exception e) {
                // Expected when native library is not available
                assertTrue("Should be a SQLite exception or UnsatisfiedLinkError", 
                    e instanceof android.database.sqlite.SQLiteException || e instanceof UnsatisfiedLinkError);
            }
        }
    }
    
    @Test
    public void testMethodOverloads() {
        // Test that method overloads exist and work correctly
        float[] vector = {1.0f, 0.0f, 0.0f};
        
        try {
            // Test distance method overloads (when native library is available)
            // SQLiteVecJava.distance(database, vector, vector, "cosine");
            // SQLiteVecJava.distance(database, vector, vector); // default metric
            
            // For now, just test that the methods exist by checking they can be called
            // without crashing (they will throw exceptions due to null database)
            try {
                SQLiteVecJava.distance(null, vector, vector, "cosine");
                fail("Should throw exception with null database");
            } catch (Exception e) {
                // Expected
            }
            
            try {
                SQLiteVecJava.distance(null, vector, vector);
                fail("Should throw exception with null database");
            } catch (Exception e) {
                // Expected
            }
        } catch (Exception e) {
            // This is fine - we're just testing that the methods exist
        }
    }
    
    @Test
    public void testRoundTripSerialization() {
        // Test that serialization and deserialization are inverses
        float[] originalVector = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
        
        try {
            byte[] serialized = SQLiteVecJava.serialize(originalVector);
            float[] deserialized = SQLiteVecJava.deserializeFloat32(serialized);
            
            assertEquals("Deserialized vector should have same length", 
                originalVector.length, deserialized.length);
            
            for (int i = 0; i < originalVector.length; i++) {
                assertEquals("Element " + i + " should match", 
                    originalVector[i], deserialized[i], 0.0001f);
            }
        } catch (Exception e) {
            // Expected when native library is not available
            assertTrue("Should be a SQLite exception or UnsatisfiedLinkError", 
                e instanceof android.database.sqlite.SQLiteException || e instanceof UnsatisfiedLinkError);
        }
    }
    
    @Test
    public void testInt8RoundTrip() {
        // Test Int8 serialization round trip
        byte[] originalVector = {-128, -1, 0, 1, 127};
        
        try {
            byte[] serialized = SQLiteVecJava.serialize(originalVector);
            byte[] deserialized = SQLiteVecJava.deserializeInt8(serialized);
            
            assertArrayEquals("Int8 round trip should preserve values", 
                originalVector, deserialized);
        } catch (Exception e) {
            // Expected when native library is not available
            assertTrue("Should be a SQLite exception or UnsatisfiedLinkError", 
                e instanceof android.database.sqlite.SQLiteException || e instanceof UnsatisfiedLinkError);
        }
    }
}
