import Foundation
import SQLite
import SQLiteVec

/// Example demonstrating SQLiteVec usage in an iOS application
/// This example shows how to use sqlite-vec for semantic search in iOS apps
class iOSVectorSearchExample {
    private var db: Connection
    
    init() throws {
        // Use Documents directory for persistent storage on iOS
        let documentsPath = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true).first!
        let dbPath = "\(documentsPath)/vector_search.db"
        
        self.db = try Connection(dbPath)
        
        // Load sqlite-vec extension
        try SQLiteVec.load(database: db)
        print("✅ SQLiteVec loaded successfully for iOS")
        
        // Enable auto-loading for future connections
        SQLiteVec.autoLoad()
        
        // Setup database schema
        try setupDatabase()
    }
    
    private func setupDatabase() throws {
        // Create table for storing app content with embeddings
        try SQLiteVec.createVectorTable(
            database: db,
            tableName: "app_content",
            vectorColumn: "embedding float[384]",
            additionalColumns: [
                "id TEXT PRIMARY KEY",
                "title TEXT NOT NULL",
                "content TEXT NOT NULL",
                "category TEXT",
                "created_at INTEGER DEFAULT (strftime('%s', 'now'))"
            ]
        )
        
        print("✅ Database schema created")
    }
    
    /// Add content with vector embeddings (simulating text embeddings)
    func addContent(_ items: [(id: String, title: String, content: String, category: String, embedding: [Float32])]) throws {
        let vectors: [(String, [Float32])] = items.map { ($0.id, $0.embedding) }
        
        // First insert the content
        for item in items {
            try db.execute("""
                INSERT OR REPLACE INTO app_content (id, title, content, category, embedding)
                VALUES (?, ?, ?, ?, ?)
            """, [item.id, item.title, item.content, item.category, SQLiteVec.serialize(item.embedding)])
        }
        
        print("✅ Added \(items.count) content items with embeddings")
    }
    
    /// Search for similar content
    func searchSimilar(to query: [Float32], category: String? = nil, limit: Int = 10) throws -> [(id: String, title: String, content: String, similarity: Double)] {
        var sql = """
            SELECT id, title, content, 1 - distance as similarity
            FROM app_content
            WHERE embedding MATCH ?
        """
        
        var parameters: [Any] = [SQLiteVec.serialize(query)]
        
        if let category = category {
            sql += " AND category = ?"
            parameters.append(category)
        }
        
        sql += " ORDER BY distance LIMIT ?"
        parameters.append(limit)
        
        let results = try db.prepare(sql, parameters)
        
        return try results.map { row in
            let id = row[0] as! String
            let title = row[1] as! String
            let content = row[2] as! String
            let similarity = row[3] as! Double
            
            return (id: id, title: title, content: content, similarity: similarity)
        }
    }
    
    /// Get content statistics
    func getStats() throws -> (totalItems: Int, categories: [String]) {
        let totalItems = try db.scalar("SELECT COUNT(*) FROM app_content") as! Int64
        let categoryResults = try db.prepare("SELECT DISTINCT category FROM app_content ORDER BY category")
        let categories = try categoryResults.map { row in
            row[0] as! String
        }
        
        return (totalItems: Int(totalItems), categories: categories)
    }
    
    /// Demonstrate vector operations
    func demonstrateVectorOperations() throws {
        let vector1: [Float32] = Array(repeating: 0.5, count: 384)
        let vector2: [Float32] = Array(repeating: 0.3, count: 384)
        
        // Calculate similarity
        let distance = try SQLiteVec.distance(database: db, vector1: vector1, vector2: vector2, metric: "cosine")
        print("📐 Cosine distance: \(distance)")
        
        // Normalize vectors
        let normalized = try SQLiteVec.normalize(database: db, vector: vector1)
        print("🔢 Normalized vector length: \(normalized.count)")
        
        // Vector math
        let dotProduct = try SQLiteVec.dotProduct(database: db, vector1: vector1, vector2: vector2)
        print("• Dot product: \(dotProduct)")
    }
}

// MARK: - iOS-specific utilities

extension iOSVectorSearchExample {
    /// Check device capabilities and memory for vector operations
    func checkDeviceCapabilities() {
        let processInfo = ProcessInfo.processInfo
        let memorySize = processInfo.physicalMemory
        
        print("📱 Device Info:")
        print("   Memory: \(ByteCountFormatter.string(fromByteCount: Int64(memorySize), countStyle: .memory))")
        print("   iOS Version: \(processInfo.operatingSystemVersionString)")
        
        // Recommend vector dimensions based on memory
        let recommendedDimensions: Int
        if memorySize < 2_000_000_000 { // < 2GB
            recommendedDimensions = 128
        } else if memorySize < 4_000_000_000 { // < 4GB
            recommendedDimensions = 384
        } else {
            recommendedDimensions = 768
        }
        
        print("   Recommended vector dimensions: \(recommendedDimensions)")
    }
    
    /// Export database for app backup/restore
    func exportToDocuments(fileName: String = "vector_backup.db") throws -> URL {
        let documentsPath = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true).first!
        let backupPath = URL(fileURLWithPath: documentsPath).appendingPathComponent(fileName)
        
        // Simple backup by copying the database file
        let currentDbPath = db.description // Get current database path
        try FileManager.default.copyItem(atPath: currentDbPath, toPath: backupPath.path)
        
        return backupPath
    }
}

// MARK: - Example Usage

func runIOSExample() {
    do {
        let example = try iOSVectorSearchExample()
        
        // Check device capabilities
        example.checkDeviceCapabilities()
        
        // Sample data (in a real app, these embeddings would come from an ML model)
        let sampleContent = [
            (
                id: "article_1",
                title: "iOS Development Best Practices",
                content: "Learn about Swift, SwiftUI, and modern iOS development techniques...",
                category: "Technology",
                embedding: Array(repeating: Float32.random(in: 0...1), count: 384)
            ),
            (
                id: "article_2", 
                title: "Healthy Recipes for Busy People",
                content: "Quick and nutritious meal ideas that you can prepare in minutes...",
                category: "Health",
                embedding: Array(repeating: Float32.random(in: 0...1), count: 384)
            ),
            (
                id: "article_3",
                title: "Vector Databases Explained",
                content: "Understanding vector databases and their applications in AI...",
                category: "Technology",
                embedding: Array(repeating: Float32.random(in: 0...1), count: 384)
            )
        ]
        
        // Add content
        try example.addContent(sampleContent)
        
        // Search for technology-related content
        let queryEmbedding = Array(repeating: Float32.random(in: 0...1), count: 384)
        let results = try example.searchSimilar(to: queryEmbedding, category: "Technology", limit: 5)
        
        print("\n🔍 Search Results:")
        for result in results {
            print("   📄 \(result.title) (similarity: \(String(format: "%.3f", result.similarity)))")
        }
        
        // Get statistics
        let stats = try example.getStats()
        print("\n📊 Database Stats:")
        print("   Total items: \(stats.totalItems)")
        print("   Categories: \(stats.categories.joined(separator: ", "))")
        
        // Demonstrate vector operations
        try example.demonstrateVectorOperations()
        
        // Export backup
        let backupURL = try example.exportToDocuments()
        print("\n💾 Database backed up to: \(backupURL.path)")
        
        print("\n✅ iOS example completed successfully!")
        
    } catch {
        print("❌ Error: \(error)")
    }
}

// Run the example
print("🚀 Starting SQLiteVec iOS Example...")
runIOSExample()
