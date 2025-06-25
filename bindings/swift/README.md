# SQLiteVec - Swift Bindings

Swift bindings for the [sqlite-vec](https://github.com/asg017/sqlite-vec) SQLite extension, providing vector search capabilities in SQLite databases.

## Features

- 🚀 **Fast vector search** - Powered by the sqlite-vec SQLite extension
- 📱 **Cross-platform** - Works on macOS, iOS, watchOS, and tvOS
- 🔧 **Easy to use** - Simple Swift API built on top of SQLite.swift
- 🧮 **Multiple data types** - Support for Float32, Float64, and Int8 vectors
- 📦 **No dependencies** - Bundles sqlite-vec statically compiled via symbolic link to root

## Requirements

- iOS 13.0+ / macOS 10.15+ / watchOS 6.0+ / tvOS 13.0+
- Swift 5.5+
- Xcode 13.0+

## Installation

### Swift Package Manager

Add the following to your `Package.swift` file:

```swift
dependencies: [
    .package(url: "https://github.com/asg017/sqlite-vec.git", from: "0.1.7")
]
```

Or add it through Xcode:
1. File → Add Package Dependencies
2. Enter: `https://github.com/asg017/sqlite-vec`
3. Select the `SQLiteVec` product

## Quick Start

```swift
import SQLite
import SQLiteVec

// Create database connection
let db = try Connection(":memory:")

// Load sqlite-vec extension
try SQLiteVec.load(database: db)

// Or use the convenience method
try db.loadSQLiteVec()

// Check version
let version = try SQLiteVec.version(database: db)
print("sqlite-vec version: \(version)")
```

## Usage

### Creating Vector Tables

```swift
// Create a table for storing 384-dimensional embeddings
try SQLiteVec.createVectorTable(
    database: db,
    tableName: "documents",
    vectorColumn: "embedding float[384]",
    additionalColumns: ["title TEXT", "content TEXT"]
)
```

### Inserting Vectors

```swift
let embedding: [Float32] = [0.1, 0.2, 0.3, 0.4] // Your vector data
let vectorData = SQLiteVec.serialize(embedding)

try db.execute("""
    INSERT INTO documents(rowid, embedding, title, content) 
    VALUES (?, ?, ?, ?)
""", [1, vectorData, "Document 1", "Content here"])
```

### Vector Search

```swift
let queryVector: [Float32] = [0.2, 0.2, 0.3, 0.3]

let results = try SQLiteVec.searchSimilar(
    database: db,
    tableName: "documents",
    vectorColumn: "embedding",
    queryVector: queryVector,
    limit: 5,
    additionalColumns: ["title", "content"]
)

for result in results {
    let rowid = result[0] as! Int64
    let distance = result[1] as! Double
    let title = result[2] as! String
    let content = result[3] as! String
    
    print("ID: \(rowid), Distance: \(distance), Title: \(title)")
}
```

### Vector Operations

```swift
let vector1: [Float32] = [1.0, 0.0, 0.0]
let vector2: [Float32] = [0.0, 1.0, 0.0]

// Calculate distance
let distance = try SQLiteVec.distance(
    database: db,
    vector1: vector1,
    vector2: vector2,
    metric: "cosine"
)

// Get vector length
let length = try SQLiteVec.length(database: db, vector: vector1)

// Normalize vector
let normalized = try SQLiteVec.normalize(database: db, vector: [3.0, 4.0])
```

### Auto-loading

For applications that create multiple database connections:

```swift
// Enable auto-loading for all future connections
SQLiteVec.autoLoad()

let db1 = try Connection(":memory:")
let db2 = try Connection("app.db")
// sqlite-vec is automatically available in both

// Disable auto-loading
SQLiteVec.cancelAutoLoad()
```

### Serialization

```swift
// Serialize different vector types
let float32Vector: [Float32] = [0.1, 0.2, 0.3]
let float64Vector: [Float64] = [0.1, 0.2, 0.3]
let int8Vector: [Int8] = [1, 2, 3]

let data1 = SQLiteVec.serialize(float32Vector)
let data2 = SQLiteVec.serialize(float64Vector)
let data3 = SQLiteVec.serialize(int8Vector)

// Deserialize back to arrays
let restored1 = SQLiteVec.deserializeFloat32(data1)
let restored2 = SQLiteVec.deserializeInt8(data3)
```

## Advanced Usage

### Raw SQL with Vector Functions

```swift
// Use sqlite-vec functions directly
let result = try db.scalar("""
    SELECT vec_distance_cosine(?, ?) as distance
""", [vectorData1, vectorData2]) as! Double

// Get vector as JSON
let vectorJson = try db.scalar("""
    SELECT vec_to_json(?)
""", [vectorData]) as! String

// Vector arithmetic
let sumData = try db.scalar("""
    SELECT vec_add(?, ?)
""", [vectorData1, vectorData2]) as! Data
```

### Metadata and Auxiliary Columns

```swift
// Create table with metadata columns for filtering
try db.execute("""
    CREATE VIRTUAL TABLE docs USING vec0(
        embedding float[384],
        category TEXT metadata,
        published_date INTEGER metadata,
        author TEXT auxiliary
    )
""")

// Search with filters
let filteredResults = try db.prepare("""
    SELECT rowid, distance, author
    FROM docs
    WHERE embedding MATCH ?
    AND category = 'technology'
    AND published_date > ?
    ORDER BY distance
    LIMIT 10
""", [queryVector, Date().timeIntervalSince1970 - 86400])
```

### Go-Style Compatibility Functions

For developers familiar with the Go bindings, the Swift bindings provide compatible function names and patterns:

```swift
// Serialization (compatible with Go's SerializeFloat32)
let vector: [Float32] = [0.1, 0.2, 0.3, 0.4]
let data = SQLiteVec.serializeFloat32(vector)  // Same as SQLiteVec.serialize(vector)

// Check availability (similar to Go error handling patterns)
if SQLiteVec.isAvailable(database: db) {
    print("sqlite-vec is ready")
} else {
    try SQLiteVec.ensureLoaded(database: db)
}

// Batch operations (Go-style bulk inserts)
let vectors: [(Int64, [Float32])] = [
    (1, [0.1, 0.1, 0.1]),
    (2, [0.2, 0.2, 0.2]),
    (3, [0.3, 0.3, 0.3])
]

try SQLiteVec.insertVectorsBatch(
    database: db,
    tableName: "vectors",
    vectorColumn: "embedding", 
    vectors: vectors
)
```

### Vector Math Operations

```swift
let v1: [Float32] = [1.0, 0.0, 0.0]
let v2: [Float32] = [0.0, 1.0, 0.0]

// Vector arithmetic
let sum = try SQLiteVec.add(database: db, vector1: v1, vector2: v2)
let diff = try SQLiteVec.subtract(database: db, vector1: v1, vector2: v2)
let dot = try SQLiteVec.dotProduct(database: db, vector1: v1, vector2: v2)

// Convert to JSON
let jsonString = try SQLiteVec.toJSON(database: db, vector: v1)
```

### Advanced Search with Filters

```swift
// Search with additional filtering (similar to Go's flexible SQL patterns)
let results = try SQLiteVec.searchSimilarWithFilter(
    database: db,
    tableName: "documents",
    vectorColumn: "embedding",
    queryVector: queryVector,
    limit: 10,
    whereClause: "category = ? AND date > ?",
    parameters: ["technology", "2024-01-01"]
)
```

## Examples

The package includes several examples demonstrating different use cases:

- **SimpleDemo.swift** - Basic vector operations and search
- **TextEmbeddingExample.swift** - Text similarity search example  
- **iOSExample/** - Complete iOS app example with mobile-specific optimizations

To run the examples:

```bash
# Run simple demo
swift run SimpleDemo

# Run text embedding example
swift run TextEmbeddingExample

# Run iOS-specific example
cd Examples/iOSExample
swift run
```

## iOS Compatibility

The Swift bindings are fully compatible with iOS 13.0+ and include specific optimizations for mobile development:

### iOS-Specific Features

- **Document Directory Storage**: Automatically uses iOS Documents directory for persistent databases
- **Memory Management**: Optimized for mobile memory constraints
- **Background Processing**: Compatible with iOS background app refresh
- **App Store Compliance**: Statically linked with no external dependencies

### iOS Example

```swift
import SQLite
import SQLiteVec

class MobileVectorSearch {
    private var db: Connection
    
    init() throws {
        // Use iOS Documents directory
        let documentsPath = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true).first!
        let dbPath = "\(documentsPath)/app_vectors.db"
        
        self.db = try Connection(dbPath)
        try SQLiteVec.load(database: db)
        
        // Enable auto-loading for app lifecycle
        SQLiteVec.autoLoad()
    }
    
    func searchContent(query: [Float32]) throws -> [(title: String, similarity: Double)] {
        let results = try SQLiteVec.searchSimilar(
            database: db,
            tableName: "content",
            vectorColumn: "embedding",
            queryVector: query,
            limit: 10
        )
        
        return results.map { row in
            (title: row[2] as! String, similarity: 1.0 - (row[1] as! Double))
        }
    }
}
```

### Memory Recommendations

For optimal iOS performance:
- Use 128-384 dimensional vectors on devices with < 4GB RAM
- Use 384-768 dimensional vectors on devices with ≥ 4GB RAM
- Consider batch processing for large vector operations

### App Store Submission

The SQLiteVec Swift package is App Store compatible:
- ✅ Statically linked (no dynamic library issues)
- ✅ No external network dependencies
- ✅ Standard SQLite extension architecture
- ✅ No prohibited APIs

## Comparison with Go Bindings

The Swift bindings provide feature parity with the Go bindings and follow similar patterns:

| Feature | Go Bindings | Swift Bindings |
|---------|-------------|----------------|
| Auto-loading | `sqlite_vec.Auto()` | `SQLiteVec.autoLoad()` |
| Manual loading | Direct C function call | `SQLiteVec.load(database:)` |
| Vector serialization | `sqlite_vec.SerializeFloat32()` | `SQLiteVec.serializeFloat32()` |
| Batch operations | Manual loop with transactions | `SQLiteVec.insertVectorsBatch()` |
| Error handling | Go error returns | Swift throws/try |
| Static linking | CGO compilation | Swift Package C target |

### Migration from Go

If you're familiar with the Go bindings, here are the equivalent patterns:

```swift
// Go: sqlite_vec.Auto()
SQLiteVec.autoLoad()

// Go: v, err := sqlite_vec.SerializeFloat32(values)
let data = SQLiteVec.serializeFloat32(values)

// Go: Manual transaction loops
try SQLiteVec.insertVectorsBatch(database: db, tableName: "vectors", vectorColumn: "embedding", vectors: vectors)

// Go: if err != nil { ... }
do {
    try SQLiteVec.load(database: db)
} catch {
    print("Error: \(error)")
}
```
