package com.sqlite.vec;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Before;
import org.junit.After;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test class for SQLiteVec Android bindings
 * Tests the new API structure similar to Swift bindings
 */
public class SQLiteVecTest {
    
    private SupportSQLiteDatabase database;
    private SupportSQLiteOpenHelper helper;
    
    @Before
    public void setUp() {
        // For unit tests, we'll test the API structure without actual database operations
        // Integration tests with real database operations should be in androidTest folder
        database = null; // Will be mocked in individual tests as needed
        helper = null;
    }
    
    @After
    public void tearDown() {
        if (database != null) {
            try {
                database.close();
            } catch (IOException e) {
                // Log error but don't fail the test
            }
        }
        if (helper != null) {
            helper.close();
        }
    }
    
    @Test
    public void testLoadExtension() throws Exception {
        // In unit tests, native library isn't loaded, so we expect exceptions
        try {
            SQLiteVec.load(null);
            fail("Expected SQLiteVecException when native library not loaded");
        } catch (SQLiteVec.SQLiteVecException e) {
            assertTrue("Exception message should contain 'not loaded'", 
                      e.getMessage().contains("not loaded"));
        }
    }
    
    @Test
    public void testEnsureLoaded() throws Exception {
        // In unit tests, isAvailable should return false due to no native library
        assertFalse(SQLiteVec.isAvailable(null));
        
        // ensureLoaded should throw exception when native library not loaded
        try {
            SQLiteVec.ensureLoaded(null);
            fail("Expected SQLiteVecException when native library not loaded");
        } catch (SQLiteVec.SQLiteVecException e) {
            assertTrue("Exception message should contain 'not loaded'", 
                      e.getMessage().contains("not loaded"));
        }
    }
    
    @Test
    public void testSerializeDeserialize() throws Exception {
        // Test vector serialization/deserialization API structure
        float[] originalVector = {0.1f, 0.2f, 0.3f, 0.4f};
        
        // In unit tests, native library isn't loaded, so we expect RuntimeException
        try {
            SQLiteVec.serialize(originalVector);
            fail("Expected RuntimeException when native library not loaded");
        } catch (RuntimeException e) {
            assertTrue("Exception message should contain 'not loaded'", 
                      e.getMessage().contains("not loaded"));
        }
        
        // Test backward compatibility method
        try {
            SQLiteVec.serializeFloat32(originalVector);
            fail("Expected RuntimeException when native library not loaded");
        } catch (RuntimeException e) {
            assertTrue("Exception message should contain 'not loaded'", 
                      e.getMessage().contains("not loaded"));
        }
        
        // Test deserialize
        try {
            SQLiteVec.deserializeFloat32(new byte[16]);
            fail("Expected RuntimeException when native library not loaded");
        } catch (RuntimeException e) {
            assertTrue("Exception message should contain 'not loaded'", 
                      e.getMessage().contains("not loaded"));
        }
    }
    
    @Test
    public void testVectorOperations() throws Exception {
        // In unit tests, all vector operations should fail due to no native library
        float[] vector1 = {1.0f, 2.0f, 3.0f, 4.0f};
        float[] vector2 = {0.5f, 1.0f, 1.5f, 2.0f};
        
        // Test distance calculation - should fail when native library not loaded
        try {
            SQLiteVec.distance(null, vector1, vector2, "cosine");
            fail("Expected SQLiteVecException when native library not loaded");
        } catch (SQLiteVec.SQLiteVecException e) {
            assertTrue("Exception message should contain 'not loaded'", 
                      e.getMessage().contains("not loaded"));
        }
        
        // Test backward compatibility with VectorOperations
        try {
            VectorOperations.cosineDistance(null, vector1, vector2);
            fail("Expected SQLiteVecException when native library not loaded");
        } catch (SQLiteVec.SQLiteVecException e) {
            assertTrue("Exception message should contain 'not loaded'", 
                      e.getMessage().contains("not loaded"));
        }
    }
    
    @Test
    public void testVectorTable() throws Exception {
        // In unit tests, all vector table operations should fail due to no native library
        String tableName = "test_vectors";
        String vectorColumn = "embedding float[4]";
        
        // Test table creation - should fail when native library not loaded
        try {
            SQLiteVec.createVectorTable(null, tableName, vectorColumn);
            fail("Expected exception when native library not loaded");
        } catch (Exception e) {
            // Could be SQLiteVecException or NullPointerException
            assertNotNull("Exception should be thrown", e);
        }
        
        // Test backward compatibility with VectorTable
        try {
            VectorTable.createVectorTable(null, tableName, vectorColumn);
            fail("Expected exception when native library not loaded");
        } catch (Exception e) {
            // Could be SQLiteVecException or NullPointerException
            assertNotNull("Exception should be thrown", e);
        }
    }
    
    @Test
    public void testByteBuffer() throws Exception {
        float[] vector = {0.1f, 0.2f, 0.3f, 0.4f};
        
        // Test conversion to ByteBuffer
        java.nio.ByteBuffer buffer = SQLiteVec.toByteBuffer(vector);
        assertNotNull(buffer);
        assertEquals(vector.length * 4, buffer.capacity());
        
        // Test conversion from ByteBuffer
        float[] result = SQLiteVec.fromByteBuffer(buffer);
        assertNotNull(result);
        assertEquals(vector.length, result.length);
        
        for (int i = 0; i < vector.length; i++) {
            assertEquals(vector[i], result[i], 0.001f);
        }
    }
    
    @Test
    public void testInt8Serialization() throws Exception {
        byte[] vector = {10, 20, 30, 40};
        
        // In unit tests, native library isn't loaded, so we expect RuntimeException
        try {
            SQLiteVec.serialize(vector);
            fail("Expected RuntimeException when native library not loaded");
        } catch (RuntimeException e) {
            assertTrue("Exception message should contain 'not loaded'", 
                      e.getMessage().contains("not loaded"));
        }
        
        // Test backward compatibility method
        try {
            SQLiteVec.serializeInt8(vector);
            fail("Expected RuntimeException when native library not loaded");
        } catch (RuntimeException e) {
            assertTrue("Exception message should contain 'not loaded'", 
                      e.getMessage().contains("not loaded"));
        }
        
        // Test deserialize
        try {
            SQLiteVec.deserializeInt8(new byte[4]);
            fail("Expected RuntimeException when native library not loaded");
        } catch (RuntimeException e) {
            assertTrue("Exception message should contain 'not loaded'", 
                      e.getMessage().contains("not loaded"));
        }
    }
    
    @Test
    public void testVectorEntryClass() throws Exception {
        // Test the VectorEntry data class
        float[] vector = {1.0f, 2.0f, 3.0f};
        SQLiteVec.VectorEntry entry = new SQLiteVec.VectorEntry(123, vector);
        
        assertEquals(123, entry.id);
        assertArrayEquals(vector, entry.vector, 0.001f);
    }
    
    @Test
    public void testSearchResultClass() throws Exception {
        // Test the SearchResult data class
        Object[] additionalData = {"test", 42};
        SQLiteVec.SearchResult result = new SQLiteVec.SearchResult(456, 0.85, additionalData);
        
        assertEquals(456, result.rowId);
        assertEquals(0.85, result.distance, 0.001);
        assertArrayEquals(additionalData, result.additionalData);
        
        // Test constructor without additional data
        SQLiteVec.SearchResult simpleResult = new SQLiteVec.SearchResult(789, 0.92);
        assertEquals(789, simpleResult.rowId);
        assertEquals(0.92, simpleResult.distance, 0.001);
        assertNull(simpleResult.additionalData);
    }
}
