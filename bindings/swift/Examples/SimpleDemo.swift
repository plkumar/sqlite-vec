import Foundation
import SQLite
import SQLiteVec

/// Simple demo showing sqlite-vec usage in Swift, similar to Go examples
class SQLiteVecDemo {
    
    static func runDemo() throws {
        print("=== SQLiteVec Swift Demo ===")
        
        // Create an in-memory database
        let db = try Connection(":memory:")
        
        // Load sqlite-vec extension 
        try SQLiteVec.load(database: db)
        
        // Get version info
        let version = try SQLiteVec.version(database: db)
        print("sqlite-vec version: \(version)")
        
        // Create a vector table
        try db.execute("CREATE VIRTUAL TABLE vec_items USING vec0(embedding float[4])")
        
        // Define test vectors similar to Go examples
        let items: [(Int64, [Float32])] = [
            (1, [0.1, 0.1, 0.1, 0.1]),
            (2, [0.2, 0.2, 0.2, 0.2]),
            (3, [0.3, 0.3, 0.3, 0.3]),
            (4, [0.4, 0.4, 0.4, 0.4]),
            (5, [0.5, 0.5, 0.5, 0.5])
        ]
        
        // Insert vectors using batch operation
        try SQLiteVec.insertVectorsBatch(
            database: db,
            tableName: "vec_items",
            vectorColumn: "embedding",
            vectors: items
        )
        
        print("Inserted \(items.count) vectors")
        
        // Query vector
        let queryVector: [Float32] = [0.3, 0.3, 0.3, 0.3]
        
        // Search for similar vectors
        let results = try SQLiteVec.searchSimilar(
            database: db,
            tableName: "vec_items",
            vectorColumn: "embedding",
            queryVector: queryVector,
            limit: 3
        )
        
        print("\nTop 3 similar vectors to \(queryVector):")
        for result in results {
            let rowid = result[0] as! Int64
            let distance = result[1] as! Double
            print("  rowid: \(rowid), distance: \(String(format: "%.6f", distance))")
        }
        
        // Demonstrate vector operations
        let vector1: [Float32] = [1.0, 0.0, 0.0]
        let vector2: [Float32] = [0.0, 1.0, 0.0]
        
        // Calculate cosine distance
        let distance = try SQLiteVec.distance(
            database: db,
            vector1: vector1,
            vector2: vector2,
            metric: "cosine"
        )
        print("\nCosine distance between \(vector1) and \(vector2): \(distance)")
        
        // Vector addition
        let sum = try SQLiteVec.add(database: db, vector1: vector1, vector2: vector2)
        print("Vector addition: \(vector1) + \(vector2) = \(sum)")
        
        // Vector normalization
        let unnormalized: [Float32] = [3.0, 4.0, 0.0]
        let normalized = try SQLiteVec.normalize(database: db, vector: unnormalized)
        print("Normalized \(unnormalized) = \(normalized)")
        
        // Convert to JSON
        let jsonRepr = try SQLiteVec.toJSON(database: db, vector: queryVector)
        print("Vector as JSON: \(jsonRepr)")
        
        print("\n=== Demo completed successfully ===")
    }
}

// Example of using auto-loading (similar to Go's Auto() function)
class AutoLoadingExample {
    
    static func demonstrateAutoLoading() throws {
        print("\n=== Auto-loading Demo ===")
        
        // Enable auto-loading for all future connections
        SQLiteVec.autoLoad()
        
        // Create a new connection - sqlite-vec should be automatically available
        let db1 = try Connection(":memory:")
        
        // Verify sqlite-vec is available without explicit loading
        if SQLiteVec.isAvailable(database: db1) {
            let version = try SQLiteVec.version(database: db1)
            print("Auto-loaded sqlite-vec version: \(version)")
        } else {
            print("Auto-loading failed")
        }
        
        // Create another connection
        let db2 = try Connection(":memory:")
        
        // This should also have sqlite-vec available
        if SQLiteVec.isAvailable(database: db2) {
            print("Second connection also has sqlite-vec available")
        }
        
        // Disable auto-loading
        SQLiteVec.cancelAutoLoad()
        print("Auto-loading disabled")
        
        print("=== Auto-loading demo completed ===")
    }
}

// For command line usage
if CommandLine.argc > 1 && CommandLine.arguments[1] == "demo" {
    do {
        try SQLiteVecDemo.runDemo()
        try AutoLoadingExample.demonstrateAutoLoading()
    } catch {
        print("Error: \(error)")
        exit(1)
    }
}
