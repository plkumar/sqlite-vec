import XCTest
import SQLite
@testable import SQLiteVec

final class SQLiteVecTests: XCTestCase {
    
    var database: Connection!
    
    override func setUp() {
        super.setUp()
        
        do {
            // Create an in-memory database
            database = try Connection(":memory:")
            
            // Load sqlite-vec extension
            try SQLiteVec.load(database: database)
        } catch {
            XCTFail("Failed to set up test database: \(error)")
        }
    }
    
    override func tearDown() {
        database = nil
        super.tearDown()
    }
    
    func testVersionQuery() throws {
        let version = try SQLiteVec.version(database: database)
        XCTAssertTrue(version.starts(with: "v"), "Version should start with 'v'")
        print("sqlite-vec version: \(version)")
    }
    
    func testSerializeFloat32() {
        let vector: [Float32] = [0.1, 0.2, 0.3, 0.4]
        let data = SQLiteVec.serialize(vector)
        
        // Should be 4 floats * 4 bytes each = 16 bytes
        XCTAssertEqual(data.count, 16)
        
        // Test deserialization
        let deserialized = SQLiteVec.deserializeFloat32(data)
        XCTAssertEqual(deserialized.count, 4)
        XCTAssertEqual(deserialized[0], 0.1, accuracy: 0.001)
        XCTAssertEqual(deserialized[1], 0.2, accuracy: 0.001)
        XCTAssertEqual(deserialized[2], 0.3, accuracy: 0.001)
        XCTAssertEqual(deserialized[3], 0.4, accuracy: 0.001)
    }
    
    func testSerializeInt8() {
        let vector: [Int8] = [1, 2, 3, 4, 5]
        let data = SQLiteVec.serialize(vector)
        
        // Should be 5 bytes
        XCTAssertEqual(data.count, 5)
        
        // Test deserialization
        let deserialized = SQLiteVec.deserializeInt8(data)
        XCTAssertEqual(deserialized, vector)
    }
    
    func testVectorLength() throws {
        let vector: [Float32] = [0.1, 0.2, 0.3, 0.4]
        let length = try SQLiteVec.length(database: database, vector: vector)
        XCTAssertEqual(length, 4)
    }
    
    func testVectorDistance() throws {
        let vector1: [Float32] = [1.0, 0.0, 0.0]
        let vector2: [Float32] = [0.0, 1.0, 0.0]
        
        let cosineDistance = try SQLiteVec.distance(
            database: database,
            vector1: vector1,
            vector2: vector2,
            metric: "cosine"
        )
        
        // These vectors are orthogonal, so cosine distance should be 1.0
        XCTAssertEqual(cosineDistance, 1.0, accuracy: 0.001)
    }
    
    func testCreateVectorTable() throws {
        try SQLiteVec.createVectorTable(
            database: database,
            tableName: "test_vectors",
            vectorColumn: "embedding float[4]"
        )
        
        // Verify table was created by inserting and querying
        let vector: [Float32] = [0.1, 0.2, 0.3, 0.4]
        let vectorData = SQLiteVec.serialize(vector)
        
        try database.execute("INSERT INTO test_vectors(rowid, embedding) VALUES (1, ?)", [vectorData])
        
        let count = try database.scalar("SELECT COUNT(*) FROM test_vectors") as! Int64
        XCTAssertEqual(count, 1)
    }
    
    func testVectorSearch() throws {
        // Create test table
        try SQLiteVec.createVectorTable(
            database: database,
            tableName: "items",
            vectorColumn: "embedding float[4]"
        )
        
        // Insert test data
        let items: [(Int, [Float32])] = [
            (1, [0.1, 0.1, 0.1, 0.1]),
            (2, [0.2, 0.2, 0.2, 0.2]),
            (3, [0.3, 0.3, 0.3, 0.3]),
            (4, [0.4, 0.4, 0.4, 0.4]),
            (5, [0.5, 0.5, 0.5, 0.5])
        ]
        
        for (id, vector) in items {
            let vectorData = SQLiteVec.serialize(vector)
            try database.execute("INSERT INTO items(rowid, embedding) VALUES (?, ?)", [id, vectorData])
        }
        
        // Search for similar vectors
        let queryVector: [Float32] = [0.3, 0.3, 0.3, 0.3]
        let results = try SQLiteVec.searchSimilar(
            database: database,
            tableName: "items",
            vectorColumn: "embedding",
            queryVector: queryVector,
            limit: 3
        )
        
        XCTAssertEqual(results.count, 3)
        
        // The closest should be item 3 (exact match)
        let firstResult = results[0]
        let firstRowId = firstResult[0] as! Int64
        XCTAssertEqual(firstRowId, 3)
        
        let firstDistance = firstResult[1] as! Double
        XCTAssertEqual(firstDistance, 0.0, accuracy: 0.001)
    }
    
    func testAutoLoad() throws {
        // Test auto-loading functionality
        SQLiteVec.autoLoad()
        
        // Create a new database connection
        let newDatabase = try Connection(":memory:")
        
        // sqlite-vec should be automatically available
        let version = try newDatabase.scalar("SELECT vec_version()") as! String
        XCTAssertTrue(version.starts(with: "v"))
        
        // Cancel auto-loading for future tests
        SQLiteVec.cancelAutoLoad()
    }
    
    func testConvenienceExtension() throws {
        let newDatabase = try Connection(":memory:")
        try newDatabase.loadSQLiteVec()
        
        let version = try newDatabase.scalar("SELECT vec_version()") as! String
        XCTAssertTrue(version.starts(with: "v"))
    }
    
    func testNormalization() throws {
        let vector: [Float32] = [3.0, 4.0]  // Length should be 5
        let normalized = try SQLiteVec.normalize(database: database, vector: vector)
        
        // Normalized vector should have length 1
        let length = sqrt(normalized[0] * normalized[0] + normalized[1] * normalized[1])
        XCTAssertEqual(length, 1.0, accuracy: 0.001)
        
        // Values should be 0.6 and 0.8
        XCTAssertEqual(normalized[0], 0.6, accuracy: 0.001)
        XCTAssertEqual(normalized[1], 0.8, accuracy: 0.001)
    }
    
    // MARK: - Tests for Go-style compatibility functions
    
    func testSerializeFloat32Compatibility() {
        let vector: [Float32] = [0.1, 0.2, 0.3, 0.4]
        let data1 = SQLiteVec.serialize(vector)
        let data2 = SQLiteVec.serializeFloat32(vector)
        
        // Both methods should produce identical results
        XCTAssertEqual(data1, data2)
    }
    
    func testSerializeInt8Compatibility() {
        let vector: [Int8] = [1, 2, 3, 4]
        let data1 = SQLiteVec.serialize(vector)
        let data2 = SQLiteVec.serializeInt8(vector)
        
        // Both methods should produce identical results
        XCTAssertEqual(data1, data2)
    }
    
    func testIsAvailable() {
        // Should be available since we loaded it in setUp
        XCTAssertTrue(SQLiteVec.isAvailable(database: database))
        
        // Test with a fresh database without loading
        do {
            let freshDb = try Connection(":memory:")
            XCTAssertFalse(SQLiteVec.isAvailable(database: freshDb))
        } catch {
            XCTFail("Failed to create fresh database: \(error)")
        }
    }
    
    func testEnsureLoaded() throws {
        // Test with a fresh database
        let freshDb = try Connection(":memory:")
        XCTAssertFalse(SQLiteVec.isAvailable(database: freshDb))
        
        // Ensure loaded should load it
        try SQLiteVec.ensureLoaded(database: freshDb)
        XCTAssertTrue(SQLiteVec.isAvailable(database: freshDb))
        
        // Calling again should not fail
        try SQLiteVec.ensureLoaded(database: freshDb)
        XCTAssertTrue(SQLiteVec.isAvailable(database: freshDb))
    }
    
    func testVectorMathOperations() throws {
        let vector1: [Float32] = [1.0, 0.0, 0.0]
        let vector2: [Float32] = [0.0, 1.0, 0.0]
        
        // Test dot product
        let dotProduct = try SQLiteVec.dotProduct(database: database, vector1: vector1, vector2: vector2)
        XCTAssertEqual(dotProduct, 0.0, accuracy: 0.001)
        
        // Test vector addition
        let sum = try SQLiteVec.add(database: database, vector1: vector1, vector2: vector2)
        XCTAssertEqual(sum.count, 3)
        XCTAssertEqual(sum[0], 1.0, accuracy: 0.001)
        XCTAssertEqual(sum[1], 1.0, accuracy: 0.001)
        XCTAssertEqual(sum[2], 0.0, accuracy: 0.001)
        
        // Test vector subtraction
        let diff = try SQLiteVec.subtract(database: database, vector1: vector1, vector2: vector2)
        XCTAssertEqual(diff.count, 3)
        XCTAssertEqual(diff[0], 1.0, accuracy: 0.001)
        XCTAssertEqual(diff[1], -1.0, accuracy: 0.001)
        XCTAssertEqual(diff[2], 0.0, accuracy: 0.001)
    }
    
    func testToJSON() throws {
        let vector: [Float32] = [0.1, 0.2, 0.3]
        let json = try SQLiteVec.toJSON(database: database, vector: vector)
        
        // Should be a valid JSON array
        XCTAssertTrue(json.hasPrefix("["))
        XCTAssertTrue(json.hasSuffix("]"))
        XCTAssertTrue(json.contains("0.1"))
        XCTAssertTrue(json.contains("0.2"))
        XCTAssertTrue(json.contains("0.3"))
    }
    
    func testBatchInsert() throws {
        // Create test table
        try database.execute("CREATE VIRTUAL TABLE test_vectors USING vec0(embedding float[3])")
        
        let vectors: [(Int64, [Float32])] = [
            (1, [0.1, 0.2, 0.3]),
            (2, [0.4, 0.5, 0.6]),
            (3, [0.7, 0.8, 0.9])
        ]
        
        // Test batch insert
        try SQLiteVec.insertVectorsBatch(
            database: database,
            tableName: "test_vectors",
            vectorColumn: "embedding",
            vectors: vectors
        )
        
        // Verify all vectors were inserted
        let count = try database.scalar("SELECT COUNT(*) FROM test_vectors") as! Int64
        XCTAssertEqual(count, 3)
    }
    
    func testSearchSimilarWithFilter() throws {
        // Create test table with metadata
        try database.execute("CREATE VIRTUAL TABLE filtered_vectors USING vec0(embedding float[3], category TEXT)")
        
        // Insert test data
        try database.execute("INSERT INTO filtered_vectors(rowid, embedding, category) VALUES (1, ?, 'A')", [SQLiteVec.serialize([0.1, 0.2, 0.3])])
        try database.execute("INSERT INTO filtered_vectors(rowid, embedding, category) VALUES (2, ?, 'B')", [SQLiteVec.serialize([0.4, 0.5, 0.6])])
        try database.execute("INSERT INTO filtered_vectors(rowid, embedding, category) VALUES (3, ?, 'A')", [SQLiteVec.serialize([0.7, 0.8, 0.9])])
        
        let queryVector: [Float32] = [0.2, 0.3, 0.4]
        
        // Search with filter
        let results = try SQLiteVec.searchSimilarWithFilter(
            database: database,
            tableName: "filtered_vectors",
            vectorColumn: "embedding",
            queryVector: queryVector,
            limit: 2,
            whereClause: "category = ?",
            parameters: ["A"]
        )
        
        // Should only return vectors with category 'A'
        XCTAssertEqual(results.count, 2)
        
        // Verify the returned rowids are 1 and 3 (both have category 'A')
        let rowids = results.map { $0[0] as! Int64 }.sorted()
        XCTAssertEqual(rowids, [1, 3])
    }
    
    func testAutoLoadingMechanism() throws {
        // Enable auto-loading
        SQLiteVec.autoLoad()
        
        // Create a new connection
        let autoDb = try Connection(":memory:")
        
        // sqlite-vec should be automatically available
        XCTAssertTrue(SQLiteVec.isAvailable(database: autoDb))
        
        // Disable auto-loading
        SQLiteVec.cancelAutoLoad()
        
        // Note: This test may need to be adjusted based on how SQLite3
        // handles auto extensions across different connections
    }
}
