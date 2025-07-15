package com.sqlite.vec;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit test for SQLiteVec API without native library dependencies
 * This test validates the API structure and basic functionality
 */
public class APIStructureTest {
    
    @Test
    public void testNativeLibraryStatus() {
        // Test that we can check if native library is loaded
        boolean isLoaded = SQLiteVec.isNativeLoaded();
        assertFalse("Native library should not be loaded in unit test environment", isLoaded);
    }
    
    @Test
    public void testVectorOperationsClassExists() {
        // Test that VectorOperations class exists
        assertNotNull("VectorOperations class should exist", VectorOperations.class);
    }
    
    @Test
    public void testVectorTableClassExists() {
        // Test that VectorTable class exists
        assertNotNull("VectorTable class should exist", VectorTable.class);
    }
    
    @Test
    public void testSQLiteVecExceptionExists() {
        // Test that SQLiteVecException class exists
        assertNotNull("SQLiteVecException class should exist", SQLiteVec.SQLiteVecException.class);
    }
    
    @Test
    public void testSQLiteVecExceptionConstructors() {
        // Test SQLiteVecException constructors
        SQLiteVec.SQLiteVecException ex1 = new SQLiteVec.SQLiteVecException("Test message");
        assertEquals("Test message", ex1.getMessage());
        
        Throwable cause = new RuntimeException("Cause");
        SQLiteVec.SQLiteVecException ex2 = new SQLiteVec.SQLiteVecException("Test message", cause);
        assertEquals("Test message", ex2.getMessage());
        assertEquals(cause, ex2.getCause());
    }
    
    @Test
    public void testLoadMethodThrowsWhenNotLoaded() {
        // Test that load method throws when native library is not loaded
        try {
            SQLiteVec.load(null);
            fail("Expected SQLiteVecException when native library not loaded");
        } catch (SQLiteVec.SQLiteVecException e) {
            assertTrue("Exception message should contain 'not loaded'", 
                      e.getMessage().contains("not loaded"));
        }
    }
    
    @Test
    public void testStaticMethodsExist() {
        // Test that key static methods exist (they should just throw exceptions without native library)
        try {
            SQLiteVec.version(null);
            fail("Expected SQLiteVecException when native library not loaded");
        } catch (SQLiteVec.SQLiteVecException e) {
            assertTrue("Exception message should contain 'not loaded'", 
                      e.getMessage().contains("not loaded"));
        }
    }
    
    @Test
    public void testVectorOperationsMethodsExist() {
        // Test that VectorOperations methods exist (they should just throw exceptions without native library)
        try {
            VectorOperations.distance(null, new float[]{1.0f}, new float[]{2.0f}, "cosine");
            fail("Expected SQLiteVecException when native library not loaded");
        } catch (SQLiteVec.SQLiteVecException e) {
            assertTrue("Exception message should contain 'not loaded'", 
                      e.getMessage().contains("not loaded"));
        }
    }
    
    @Test
    public void testVectorTableMethodsExist() {
        // Test that VectorTable methods exist (they should just throw exceptions without native library)
        try {
            VectorTable.createVectorTable(null, "test", "vector");
            fail("Expected exception when native library not loaded");
        } catch (Exception e) {
            // This should throw some kind of exception
            assertNotNull("Exception should be thrown", e);
        }
    }
}
