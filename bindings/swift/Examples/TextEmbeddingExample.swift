import Foundation
import SQLite
import SQLiteVec

/// A comprehensive example showing sqlite-vec usage patterns similar to Go bindings
/// This example demonstrates text embeddings, batch operations, and advanced queries
class TextEmbeddingExample {
    
    struct Document {
        let id: Int64
        let title: String
        let content: String
        let embedding: [Float32]
        let category: String
        let publishedDate: String
    }
    
    private let database: Connection
    
    init() throws {
        // Create database and load sqlite-vec
        database = try Connection(":memory:")
        try SQLiteVec.load(database: database)
        
        // Create table with metadata columns (similar to Go examples)
        try database.execute("""
            CREATE VIRTUAL TABLE documents USING vec0(
                embedding float[384],
                title TEXT,
                content TEXT,
                category TEXT metadata,
                published_date TEXT metadata
            )
        """)
    }
    
    /// Generate a mock embedding for demo purposes
    /// In real applications, this would use an ML model like OpenAI embeddings
    private func generateMockEmbedding(for text: String) -> [Float32] {
        // Simple mock: hash the text and create a 384-dimensional vector
        let hash = text.hash
        var embedding: [Float32] = []
        
        for i in 0..<384 {
            let value = Float32(sin(Double(hash + i) * 0.01))
            embedding.append(value)
        }
        
        // Normalize the vector
        let norm = sqrt(embedding.reduce(0) { $0 + $1 * $1 })
        return embedding.map { $0 / norm }
    }
    
    func addDocuments() throws {
        print("Adding documents with embeddings...")
        
        let documents = [
            Document(
                id: 1,
                title: "Introduction to Machine Learning",
                content: "Machine learning is a subset of artificial intelligence...",
                embedding: generateMockEmbedding(for: "machine learning artificial intelligence"),
                category: "technology",
                publishedDate: "2024-01-15"
            ),
            Document(
                id: 2,
                title: "Swift Programming Guide",
                content: "Swift is a powerful programming language developed by Apple...",
                embedding: generateMockEmbedding(for: "swift programming language apple development"),
                category: "technology",
                publishedDate: "2024-02-10"
            ),
            Document(
                id: 3,
                title: "Vector Databases Explained",
                content: "Vector databases are specialized databases for storing vectors...",
                embedding: generateMockEmbedding(for: "vector database embeddings similarity search"),
                category: "technology",
                publishedDate: "2024-03-05"
            ),
            Document(
                id: 4,
                title: "Cooking Perfect Pasta",
                content: "The secret to perfect pasta is in the timing and water quality...",
                embedding: generateMockEmbedding(for: "cooking pasta recipe food kitchen"),
                category: "lifestyle",
                publishedDate: "2024-01-20"
            ),
            Document(
                id: 5,
                title: "Financial Planning Basics",
                content: "Good financial planning starts with understanding your goals...",
                embedding: generateMockEmbedding(for: "financial planning money investment budget"),
                category: "finance",
                publishedDate: "2024-02-28"
            )
        ]
        
        // Use batch insert for better performance (Go-style)
        let vectors: [(Int64, [Float32])] = documents.map { ($0.id, $0.embedding) }
        try SQLiteVec.insertVectorsBatch(
            database: database,
            tableName: "documents",
            vectorColumn: "embedding",
            vectors: vectors
        )
        
        // Insert metadata separately
        for doc in documents {
            try database.execute("""
                UPDATE documents 
                SET title = ?, content = ?, category = ?, published_date = ?
                WHERE rowid = ?
            """, [doc.title, doc.content, doc.category, doc.publishedDate, doc.id])
        }
        
        print("Added \(documents.count) documents")
    }
    
    func searchSimilarDocuments() throws {
        print("\n--- Semantic Search Examples ---")
        
        // Query about programming
        let programmingQuery = generateMockEmbedding(for: "programming software development")
        print("\nSearching for documents about 'programming software development':")
        
        let programmingResults = try SQLiteVec.searchSimilar(
            database: database,
            tableName: "documents",
            vectorColumn: "embedding",
            queryVector: programmingQuery,
            limit: 3,
            additionalColumns: ["title", "category"]
        )
        
        for result in programmingResults {
            let rowid = result[0] as! Int64
            let distance = result[1] as! Double
            let title = result[2] as! String
            let category = result[3] as! String
            print("  - \(title) (category: \(category), distance: \(String(format: "%.4f", distance)))")
        }
        
        // Search with category filter (Go-style advanced querying)
        print("\nSearching only in 'technology' category:")
        
        let techResults = try SQLiteVec.searchSimilarWithFilter(
            database: database,
            tableName: "documents",
            vectorColumn: "embedding",
            queryVector: programmingQuery,
            limit: 5,
            whereClause: "category = ?",
            parameters: ["technology"]
        )
        
        for result in techResults {
            let rowid = result[0] as! Int64
            let distance = result[1] as! Double
            print("  - Document ID \(rowid) (distance: \(String(format: "%.4f", distance)))")
        }
        
        // Search with date filter
        print("\nSearching for recent documents (after 2024-02-01):")
        
        let recentResults = try SQLiteVec.searchSimilarWithFilter(
            database: database,
            tableName: "documents", 
            vectorColumn: "embedding",
            queryVector: programmingQuery,
            limit: 5,
            whereClause: "published_date > ?",
            parameters: ["2024-02-01"]
        )
        
        print("Found \(recentResults.count) recent documents")
    }
    
    func demonstrateVectorOperations() throws {
        print("\n--- Vector Math Operations ---")
        
        let techVector = generateMockEmbedding(for: "technology programming")
        let financeVector = generateMockEmbedding(for: "finance money")
        
        // Calculate similarity using different metrics
        let cosineDistance = try SQLiteVec.distance(
            database: database,
            vector1: techVector,
            vector2: financeVector,
            metric: "cosine"
        )
        
        let l2Distance = try SQLiteVec.distance(
            database: database,
            vector1: techVector,
            vector2: financeVector,
            metric: "l2"
        )
        
        print("Distance between 'technology' and 'finance' vectors:")
        print("  Cosine: \(String(format: "%.4f", cosineDistance))")
        print("  L2: \(String(format: "%.4f", l2Distance))")
        
        // Vector arithmetic
        let combinedVector = try SQLiteVec.add(
            database: database,
            vector1: techVector,
            vector2: financeVector
        )
        
        let normalizedCombined = try SQLiteVec.normalize(
            database: database,
            vector: combinedVector
        )
        
        print("Combined vector length: \(try SQLiteVec.length(database: database, vector: normalizedCombined))")
        
        // Convert to JSON for inspection
        let jsonRepr = try SQLiteVec.toJSON(database: database, vector: Array(normalizedCombined.prefix(5)))
        print("First 5 dimensions as JSON: \(jsonRepr)")
    }
    
    func runBenchmark() throws {
        print("\n--- Performance Benchmark ---")
        
        let startTime = CFAbsoluteTimeGetCurrent()
        
        // Generate and insert many vectors
        let batchSize = 1000
        var allVectors: [(Int64, [Float32])] = []
        
        for i in 0..<batchSize {
            let text = "document \(i) content for testing performance"
            let embedding = generateMockEmbedding(for: text)
            allVectors.append((Int64(1000 + i), embedding))
        }
        
        // Create a new table for benchmarking
        try database.execute("CREATE VIRTUAL TABLE benchmark_docs USING vec0(embedding float[384])")
        
        // Batch insert
        try SQLiteVec.insertVectorsBatch(
            database: database,
            tableName: "benchmark_docs",
            vectorColumn: "embedding",
            vectors: allVectors
        )
        
        let insertTime = CFAbsoluteTimeGetCurrent() - startTime
        
        // Benchmark search
        let queryStart = CFAbsoluteTimeGetCurrent()
        let queryVector = generateMockEmbedding(for: "benchmark query test")
        
        for _ in 0..<10 {
            _ = try SQLiteVec.searchSimilar(
                database: database,
                tableName: "benchmark_docs",
                vectorColumn: "embedding",
                queryVector: queryVector,
                limit: 10
            )
        }
        
        let searchTime = (CFAbsoluteTimeGetCurrent() - queryStart) / 10
        
        print("Inserted \(batchSize) vectors in \(String(format: "%.3f", insertTime))s")
        print("Average search time: \(String(format: "%.3f", searchTime * 1000))ms")
    }
}

// Main execution
do {
    print("=== SQLiteVec Swift Text Embedding Example ===")
    print("This example demonstrates Go-style patterns for sqlite-vec in Swift")
    
    let example = try TextEmbeddingExample()
    
    try example.addDocuments()
    try example.searchSimilarDocuments()
    try example.demonstrateVectorOperations()
    
    if CommandLine.arguments.contains("--benchmark") {
        try example.runBenchmark()
    }
    
    print("\n=== Example completed successfully ===")
    print("Run with --benchmark flag to see performance tests")
    
} catch {
    print("Error: \(error)")
    exit(1)
}
