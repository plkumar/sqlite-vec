package com.sqlite.vec;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Simple API structure tests for SQLiteVec Android bindings
 * These tests verify the API structure without requiring database operations
 */
public class SimpleAPITest {
    
    @Test
    public void testSQLiteVecClassExists() {
        // Test that the main class exists
        assertNotNull(SQLiteVec.class);
    }
    
    @Test
    public void testVectorOperationsClassExists() {
        // Test that utility class exists
        assertNotNull(VectorOperations.class);
    }
    
    @Test
    public void testVectorTableClassExists() {
        // Test that utility class exists
        assertNotNull(VectorTable.class);
    }
    
    @Test
    public void testSearchResultClassExists() {
        // Test that SearchResult class exists
        assertNotNull(SQLiteVec.SearchResult.class);
        
        // Test SearchResult constructor
        SQLiteVec.SearchResult result = new SQLiteVec.SearchResult(1L, 0.5, null);
        assertEquals(1L, result.rowId);
        assertEquals(0.5, result.distance, 0.001);
        assertNull(result.additionalData);
    }
    
    @Test
    public void testVectorEntryClassExists() {
        // Test that VectorEntry class exists
        assertNotNull(SQLiteVec.VectorEntry.class);
        
        // Test VectorEntry constructor
        float[] vector = {0.1f, 0.2f, 0.3f};
        SQLiteVec.VectorEntry entry = new SQLiteVec.VectorEntry(1L, vector);
        assertEquals(1L, entry.id);
        assertArrayEquals(vector, entry.vector, 0.001f);
    }
    
    @Test
    public void testExceptionClassExists() {
        // Test that custom exception class exists
        assertNotNull(SQLiteVec.SQLiteVecException.class);
        
        // Test exception constructor
        SQLiteVec.SQLiteVecException exception = new SQLiteVec.SQLiteVecException("Test message");
        assertEquals("Test message", exception.getMessage());
    }
    
    @Test
    public void testVectorSerialization() throws Exception {
        // Test vector serialization methods exist and work
        float[] vector = {0.1f, 0.2f, 0.3f, 0.4f};
        
        // In unit tests, native library isn't loaded, so we expect RuntimeException
        try {
            SQLiteVec.serialize(vector);
            fail("Expected RuntimeException when native library not loaded");
        } catch (RuntimeException e) {
            assertTrue("Exception message should contain 'not loaded'", 
                      e.getMessage().contains("not loaded"));
        }
        
        // Test serializeFloat32 method
        try {
            SQLiteVec.serializeFloat32(vector);
            fail("Expected RuntimeException when native library not loaded");
        } catch (RuntimeException e) {
            assertTrue("Exception message should contain 'not loaded'", 
                      e.getMessage().contains("not loaded"));
        }
        
        // Test deserialize method
        try {
            SQLiteVec.deserializeFloat32(new byte[16]);
            fail("Expected RuntimeException when native library not loaded");
        } catch (RuntimeException e) {
            assertTrue("Exception message should contain 'not loaded'", 
                      e.getMessage().contains("not loaded"));
        }
    }
    
    @Test
    public void testInt8Serialization() throws Exception {
        // Test int8 serialization
        byte[] vector = {1, 2, 3, 4};
        
        // In unit tests, native library isn't loaded, so we expect RuntimeException
        try {
            SQLiteVec.serializeInt8(vector);
            fail("Expected RuntimeException when native library not loaded");
        } catch (RuntimeException e) {
            assertTrue("Exception message should contain 'not loaded'", 
                      e.getMessage().contains("not loaded"));
        }
        
        try {
            SQLiteVec.deserializeInt8(new byte[4]);
            fail("Expected RuntimeException when native library not loaded");
        } catch (RuntimeException e) {
            assertTrue("Exception message should contain 'not loaded'", 
                      e.getMessage().contains("not loaded"));
        }
    }
    
    @Test
    public void testAPIMethodsExist() {
        // Test that key API methods exist by checking method count
        java.lang.reflect.Method[] methods = SQLiteVec.class.getDeclaredMethods();
        
        // We expect a good number of public static methods
        int publicStaticMethods = 0;
        for (java.lang.reflect.Method method : methods) {
            if (java.lang.reflect.Modifier.isPublic(method.getModifiers()) && 
                java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                publicStaticMethods++;
            }
        }
        
        // We should have at least 20 public static methods
        assertTrue("Expected at least 20 public static methods, found: " + publicStaticMethods, 
                   publicStaticMethods >= 20);
    }
}
