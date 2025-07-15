# SQLiteVec Android Bindings

Android bindings for [sqlite-vec](https://github.com/asg017/sqlite-vec) - A fast vector search extension for SQLite, perfect for building semantic search, recommendation systems, and AI applications on Android.

## Features

- 🚀 **Fast vector search** - Powered by the sqlite-vec SQLite extension
- 📱 **Android optimized** - Works across all Android architectures (ARM, x86)
- 🔧 **Easy to use** - Simple Kotlin/Java API with comprehensive documentation
- 🧮 **Multiple data types** - Support for Float32, Float64, and Int8 vectors
- 📦 **Self-contained** - Statically linked with no external dependencies
- 🎯 **Production ready** - Comprehensive test suite and error handling
- 💾 **Efficient** - Optimized for mobile memory constraints
- 🍎 **Swift compatible** - API designed to match Swift bindings for consistency
- 🛠️ **Kotlin extensions** - Convenient extension methods for fluid API usage
- 🧪 **Comprehensive testing** - Unit tests, instrumented tests, and compatibility tests

## Requirements

- Android API level 21 (Android 5.0) or higher
- Kotlin 1.8+ or Java 8+
- Android Gradle Plugin 8.0+

## Installation

### Gradle (Kotlin DSL)

Add to your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.sqlite:sqlite-vec-android:0.1.7")
}
```

### Gradle (Groovy)

Add to your module's `build.gradle`:

```groovy
dependencies {
    implementation 'com.sqlite:sqlite-vec-android:0.1.7'
}
```

### Manual Installation

1. Download the AAR from [Releases](https://github.com/asg017/sqlite-vec/releases)
2. Place it in your `libs/` folder
3. Add to your `build.gradle`:

```kotlin
dependencies {
    implementation(files("libs/sqlite-vec-android-0.1.7.aar"))
}
```

## Quick Start

### Kotlin

```kotlin
import android.database.sqlite.SQLiteDatabase
import com.sqlite.vec.SQLiteVec

// Create or open database
val db = SQLiteDatabase.openDatabase(
    "path/to/database.db", 
    null, 
    SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.CREATE_IF_NECESSARY
)

// Load sqlite-vec extension
SQLiteVec.load(db)

// Check version
val version = SQLiteVec.version(db)
println("sqlite-vec version: $version")

// Create vector table
SQLiteVec.createVectorTable(
    db, 
    "documents", 
    "embedding float[384]",
    "title TEXT",
    "content TEXT"
)

// Insert vectors
val embedding = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f)
val vectorData = SQLiteVec.serialize(embedding)
db.execSQL(
    "INSERT INTO documents(rowid, embedding, title, content) VALUES (?, ?, ?, ?)",
    arrayOf(1, vectorData, "Document 1", "Content here")
)

// Search similar vectors
val queryVector = floatArrayOf(0.2f, 0.2f, 0.3f, 0.3f)
val results = SQLiteVec.searchSimilar(
    db, 
    "documents", 
    "embedding", 
    queryVector, 
    limit = 5,
    "title", "content"
)

for (result in results) {
    val rowid = result["rowid"] as Long
    val distance = result["distance"] as Double
    val title = result["title"] as String
    val content = result["content"] as String
    
    println("ID: $rowid, Distance: $distance, Title: $title")
}
```

### Java

```java
import android.database.sqlite.SQLiteDatabase;
import com.sqlite.vec.SQLiteVecJava;

// Create or open database
SQLiteDatabase db = SQLiteDatabase.openDatabase(
    "path/to/database.db", 
    null, 
    SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.CREATE_IF_NECESSARY
);

// Load sqlite-vec extension
SQLiteVecJava.load(db);

// Check version
String version = SQLiteVecJava.version(db);
System.out.println("sqlite-vec version: " + version);

// Create vector table
SQLiteVecJava.createVectorTable(
    db, 
    "documents", 
    "embedding float[384]",
    "title TEXT",
    "content TEXT"
);

// Insert vectors
float[] embedding = {0.1f, 0.2f, 0.3f, 0.4f};
byte[] vectorData = SQLiteVecJava.serialize(embedding);
db.execSQL(
    "INSERT INTO documents(rowid, embedding, title, content) VALUES (?, ?, ?, ?)",
    new Object[]{1, vectorData, "Document 1", "Content here"}
);

// Search similar vectors
float[] queryVector = {0.2f, 0.2f, 0.3f, 0.3f};
List<Map<String, Object>> results = SQLiteVecJava.searchSimilar(
    db, 
    "documents", 
    "embedding", 
    queryVector, 
    5, // limit
    "title", "content"
);

for (Map<String, Object> result : results) {
    Long rowid = (Long) result.get("rowid");
    Double distance = (Double) result.get("distance");
    String title = (String) result.get("title");
    String content = (String) result.get("content");
    
    System.out.println("ID: " + rowid + ", Distance: " + distance + ", Title: " + title);
}
```

## Advanced Usage

### Using SQLiteVecOpenHelper

For automatic sqlite-vec loading:

```kotlin
import com.sqlite.vec.SQLiteVecOpenHelper

class AppDatabaseHelper(context: Context) : SQLiteVecOpenHelper(
    context = context,
    name = "app_database.db",
    version = 1
) {
    override fun onCreate(db: SQLiteDatabase) {
        // sqlite-vec is automatically loaded at this point
        db.execSQL("""
            CREATE VIRTUAL TABLE documents USING vec0(
                embedding float[384],
                title TEXT,
                content TEXT,
                category TEXT metadata,
                published_date INTEGER metadata
            )
        """)
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database upgrades
    }
}

// Usage
val dbHelper = AppDatabaseHelper(context)
val db = dbHelper.writableDatabase
// sqlite-vec is automatically available
```

### Batch Operations

```kotlin
import com.sqlite.vec.SQLiteVecUtils

// Batch insert for better performance
val vectors = listOf(
    Pair(1L, floatArrayOf(0.1f, 0.1f, 0.1f)),
    Pair(2L, floatArrayOf(0.2f, 0.2f, 0.2f)),
    Pair(3L, floatArrayOf(0.3f, 0.3f, 0.3f))
)

SQLiteVecUtils.insertVectorsBatch(db, "documents", "embedding", vectors)

// Search with filters
val results = SQLiteVecUtils.searchSimilarWithFilter(
    db,
    "documents",
    "embedding",
    queryVector,
    limit = 10,
    whereClause = "category = ? AND published_date > ?",
    whereArgs = arrayOf("technology", "1640995200") // Unix timestamp
)
```

### Vector Math Operations

```kotlin
val v1 = floatArrayOf(1.0f, 0.0f, 0.0f)
val v2 = floatArrayOf(0.0f, 1.0f, 0.0f)

// Vector arithmetic
val sum = SQLiteVecUtils.add(db, v1, v2)
val diff = SQLiteVecUtils.subtract(db, v1, v2)
val dot = SQLiteVecUtils.dotProduct(db, v1, v2)

// Distance calculations
val cosineDistance = SQLiteVec.distance(db, v1, v2, "cosine")
val l2Distance = SQLiteVec.distance(db, v1, v2, "l2")

// Vector normalization
val normalized = SQLiteVec.normalize(db, floatArrayOf(3.0f, 4.0f))

// JSON conversion
val jsonString = SQLiteVecUtils.toJSON(db, v1)
val vectorFromJson = SQLiteVecUtils.fromJSON(db, jsonString)
```

### Different Vector Types

```kotlin
// Float32 vectors (most common)
val float32Vector = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f)
val float32Data = SQLiteVec.serialize(float32Vector)

// Float64 vectors (higher precision)
val float64Vector = doubleArrayOf(0.1, 0.2, 0.3, 0.4)
val float64Data = SQLiteVec.serialize(float64Vector)

// Int8 vectors (space efficient)
val int8Vector = byteArrayOf(1, 2, 3, 4, 5)
val int8Data = SQLiteVec.serialize(int8Vector)

// Deserialization
val restoredFloat32 = SQLiteVec.deserializeFloat32(float32Data)
val restoredInt8 = SQLiteVec.deserializeInt8(int8Data)
```

## Android-Specific Features

### Memory Optimization

```kotlin
// Use smaller vector dimensions on lower-end devices
val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
val memoryInfo = ActivityManager.MemoryInfo()
activityManager.getMemoryInfo(memoryInfo)

val vectorDimension = when {
    memoryInfo.totalMem < 2_000_000_000 -> 128  // < 2GB RAM
    memoryInfo.totalMem < 4_000_000_000 -> 256  // < 4GB RAM
    else -> 384  // >= 4GB RAM
}

SQLiteVec.createVectorTable(db, "embeddings", "vector float[$vectorDimension]")
```

### Background Processing

```kotlin
class VectorSearchService : IntentService("VectorSearchService") {
    override fun onHandleIntent(intent: Intent?) {
        val query = intent?.getStringExtra("query") ?: return
        
        // Process vectors in background
        val db = openDatabase()
        SQLiteVec.ensureLoaded(db)
        
        val queryVector = generateEmbedding(query) // Your embedding logic
        val results = SQLiteVec.searchSimilar(db, "documents", "embedding", queryVector)
        
        // Send results back to UI
        LocalBroadcastManager.getInstance(this).sendBroadcast(
            Intent("SEARCH_RESULTS").putExtra("results", ArrayList(results))
        )
    }
}
```

### Storage Location

```kotlin
// Use appropriate storage location for your use case
class VectorDatabase(private val context: Context) {
    
    fun getInternalDatabase(): SQLiteDatabase {
        // Private to your app, deleted on uninstall
        val dbPath = File(context.filesDir, "vectors.db")
        return SQLiteDatabase.openDatabase(
            dbPath.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.CREATE_IF_NECESSARY
        )
    }
    
    fun getExternalDatabase(): SQLiteDatabase {
        // Survives app uninstall, requires storage permission
        val dbPath = File(context.getExternalFilesDir(null), "vectors.db")
        return SQLiteDatabase.openDatabase(
            dbPath.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.CREATE_IF_NECESSARY
        )
    }
    
    fun getCacheDatabase(): SQLiteDatabase {
        // Temporary storage, may be deleted by system
        val dbPath = File(context.cacheDir, "temp_vectors.db")
        return SQLiteDatabase.openDatabase(
            dbPath.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.CREATE_IF_NECESSARY
        )
    }
}
```

## Example Applications

### 1. Semantic Search App

```kotlin
class SemanticSearchActivity : AppCompatActivity() {
    private lateinit var db: SQLiteDatabase
    private lateinit var searchView: SearchView
    private lateinit var resultsRecyclerView: RecyclerView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        
        // Initialize database
        val dbHelper = SQLiteVecOpenHelper(this, "semantic_search.db", null, 1)
        db = dbHelper.writableDatabase
        
        setupUI()
        loadDocuments()
    }
    
    private fun setupUI() {
        searchView = findViewById(R.id.searchView)
        resultsRecyclerView = findViewById(R.id.resultsRecyclerView)
        
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { performSemanticSearch(it) }
                return true
            }
            
            override fun onQueryTextChange(newText: String?): Boolean = false
        })
    }
    
    private fun performSemanticSearch(query: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Generate embedding for search query (replace with your embedding logic)
                val queryEmbedding = generateEmbedding(query)
                
                // Search similar documents
                val results = SQLiteVec.searchSimilar(
                    db,
                    "documents",
                    "embedding",
                    queryEmbedding,
                    20,
                    "title", "content", "url"
                )
                
                withContext(Dispatchers.Main) {
                    updateSearchResults(results)
                }
            } catch (e: Exception) {
                Log.e("Search", "Search failed", e)
            }
        }
    }
    
    private fun generateEmbedding(text: String): FloatArray {
        // Implement your embedding generation logic here
        // This could call a local ML model or an API
        return FloatArray(384) { Random.nextFloat() } // Placeholder
    }
}
```

### 2. Recommendation Engine

```kotlin
class RecommendationEngine(private val db: SQLiteDatabase) {
    
    init {
        SQLiteVec.ensureLoaded(db)
    }
    
    fun getUserRecommendations(userId: Long, limit: Int = 10): List<Recommendation> {
        // Get user's interaction history
        val userVector = getUserPreferenceVector(userId)
        
        // Find similar items
        val results = SQLiteVec.searchSimilar(
            db,
            "items",
            "features",
            userVector,
            limit * 2, // Get more to filter out already seen
            "item_id", "title", "category", "rating"
        )
        
        // Filter out items user has already interacted with
        val seenItems = getUserSeenItems(userId)
        
        return results
            .filter { (it["item_id"] as Long) !in seenItems }
            .take(limit)
            .map { result ->
                Recommendation(
                    itemId = result["item_id"] as Long,
                    title = result["title"] as String,
                    category = result["category"] as String,
                    rating = result["rating"] as Double,
                    similarity = 1.0 - (result["distance"] as Double)
                )
            }
    }
    
    private fun getUserPreferenceVector(userId: Long): FloatArray {
        // Aggregate user's preferences into a vector
        // This could be based on ratings, views, purchases, etc.
        // Placeholder implementation
        return FloatArray(100) { Random.nextFloat() }
    }
    
    private fun getUserSeenItems(userId: Long): Set<Long> {
        // Get items user has already seen/interacted with
        val cursor = db.rawQuery(
            "SELECT item_id FROM user_interactions WHERE user_id = ?",
            arrayOf(userId.toString())
        )
        
        val seenItems = mutableSetOf<Long>()
        cursor.use {
            while (it.moveToNext()) {
                seenItems.add(it.getLong(0))
            }
        }
        
        return seenItems
    }
}

data class Recommendation(
    val itemId: Long,
    val title: String,
    val category: String,
    val rating: Double,
    val similarity: Double
)
```

### 3. Image Similarity App

```kotlin
class ImageSimilarityActivity : AppCompatActivity() {
    private lateinit var db: SQLiteDatabase
    private lateinit var imageView: ImageView
    private lateinit var similarImagesGrid: RecyclerView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_similarity)
        
        setupDatabase()
        setupUI()
    }
    
    private fun setupDatabase() {
        val dbHelper = object : SQLiteVecOpenHelper(this, "images.db", null, 1) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL("""
                    CREATE VIRTUAL TABLE images USING vec0(
                        features float[2048],
                        file_path TEXT,
                        file_name TEXT,
                        width INTEGER metadata,
                        height INTEGER metadata,
                        size_bytes INTEGER metadata
                    )
                """)
            }
            
            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
                // Handle upgrades
            }
        }
        
        db = dbHelper.writableDatabase
    }
    
    private fun findSimilarImages(imagePath: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Extract features from the selected image
                val features = extractImageFeatures(imagePath)
                
                // Find similar images
                val results = SQLiteVec.searchSimilar(
                    db,
                    "images",
                    "features",
                    features,
                    20,
                    "file_path", "file_name"
                )
                
                withContext(Dispatchers.Main) {
                    displaySimilarImages(results)
                }
            } catch (e: Exception) {
                Log.e("ImageSimilarity", "Failed to find similar images", e)
            }
        }
    }
    
    private fun extractImageFeatures(imagePath: String): FloatArray {
        // Extract features using a pre-trained model
        // This could use TensorFlow Lite, ML Kit, or another library
        // Placeholder implementation
        return FloatArray(2048) { Random.nextFloat() }
    }
    
    private fun addImageToDatabase(imagePath: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val features = extractImageFeatures(imagePath)
                val featureData = SQLiteVec.serialize(features)
                
                val file = File(imagePath)
                db.execSQL("""
                    INSERT INTO images(features, file_path, file_name, size_bytes) 
                    VALUES (?, ?, ?, ?)
                """, arrayOf(
                    featureData,
                    imagePath,
                    file.name,
                    file.length()
                ))
                
                Log.d("ImageSimilarity", "Added image to database: ${file.name}")
            } catch (e: Exception) {
                Log.e("ImageSimilarity", "Failed to add image to database", e)
            }
        }
    }
}
```

## Performance Tips

### 1. Vector Dimensions

```kotlin
// Choose dimensions based on your use case and device constraints
val dimensions = when (useCase) {
    "mobile_search" -> 128     // Fast, good for mobile
    "text_similarity" -> 384   // Good balance for text
    "image_features" -> 512    // Detailed image features
    "high_precision" -> 1024   // Maximum precision
    else -> 256                // Default
}
```

### 2. Batch Operations

```kotlin
// Use batch operations for better performance
db.beginTransaction()
try {
    val vectors = generateVectorsBatch(documents)
    SQLiteVecUtils.insertVectorsBatch(db, "documents", "embedding", vectors)
    db.setTransactionSuccessful()
} finally {
    db.endTransaction()
}
```

### 3. Indexing Strategy

```kotlin
// Create indexes on frequently queried metadata columns
db.execSQL("CREATE INDEX idx_category ON documents(category)")
db.execSQL("CREATE INDEX idx_date ON documents(published_date)")

// Use metadata columns for efficient filtering
val results = SQLiteVecUtils.searchSimilarWithFilter(
    db,
    "documents",
    "embedding",
    queryVector,
    whereClause = "category = ? AND published_date > ?",
    whereArgs = arrayOf("tech", yesterday.toString())
)
```

### 4. Memory Management

```kotlin
// Close cursors and databases properly
db.rawQuery("SELECT * FROM documents", null).use { cursor ->
    while (cursor.moveToNext()) {
        // Process results
    }
} // Cursor automatically closed

// Use application context for long-lived database helpers
class VectorDatabaseSingleton private constructor(context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: VectorDatabaseSingleton? = null
        
        fun getInstance(context: Context): VectorDatabaseSingleton {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VectorDatabaseSingleton(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
```

## Troubleshooting

### Common Issues

1. **UnsatisfiedLinkError**: Make sure all ABI variants are included in your APK
2. **SQLiteException**: Ensure sqlite-vec is loaded before using vector functions
3. **OutOfMemoryError**: Use smaller vector dimensions or batch processing
4. **Performance issues**: Add indexes on metadata columns and use filters

### Debug Tips

```kotlin
// Check if sqlite-vec is available
if (!SQLiteVec.isAvailable(db)) {
    Log.e("SQLiteVec", "sqlite-vec not available")
    SQLiteVec.load(db)
}

// Log vector dimensions and data sizes
val vector = floatArrayOf(0.1f, 0.2f, 0.3f)
val data = SQLiteVec.serialize(vector)
Log.d("SQLiteVec", "Vector size: ${vector.size}, Data size: ${data.size} bytes")

// Check table statistics
val stats = SQLiteVecUtils.getTableStats(db, "documents")
Log.d("SQLiteVec", "Table stats: $stats")
```

## Migration from Other Libraries

### From Room with vector search

```kotlin
// Before (Room + custom vector search)
@Entity
data class Document(
    @PrimaryKey val id: Long,
    val title: String,
    val content: String,
    val embedding: String // JSON string
)

// After (SQLiteVec)
// Create vec0 table instead of regular table
SQLiteVec.createVectorTable(
    db,
    "documents",
    "embedding float[384]",
    "title TEXT",
    "content TEXT"
)

// Use binary vector data instead of JSON
val embedding = floatArrayOf(/* your vector */)
val vectorData = SQLiteVec.serialize(embedding)
db.execSQL("INSERT INTO documents(rowid, embedding, title, content) VALUES (?, ?, ?, ?)",
           arrayOf(id, vectorData, title, content))
```

## API Reference

### Core Classes

- **`SQLiteVec`** - Main Kotlin API
- **`SQLiteVecJava`** - Java compatibility wrapper
- **`SQLiteVecUtils`** - Utility functions
- **`SQLiteVecOpenHelper`** - Auto-loading database helper

### Key Methods

| Method | Description |
|--------|-------------|
| `SQLiteVec.load(db)` | Load sqlite-vec into database |
| `SQLiteVec.serialize(vector)` | Convert vector to binary data |
| `SQLiteVec.searchSimilar()` | Find similar vectors |
| `SQLiteVec.distance()` | Calculate vector distance |
| `SQLiteVec.createVectorTable()` | Create vec0 table |

## License

This project is dual-licensed under:
- MIT License
- Apache License 2.0

See [LICENSE-MIT](LICENSE-MIT) and [LICENSE-APACHE](LICENSE-APACHE) for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Run the test suite: `./gradlew test connectedAndroidTest`
5. Submit a pull request

## Support

- 📖 [Documentation](https://sqlite-vec.vercel.app/)
- 🐛 [Issue Tracker](https://github.com/asg017/sqlite-vec/issues)
- 💬 [Discussions](https://github.com/asg017/sqlite-vec/discussions)
- 📧 [Email Support](mailto:alex@alexgarcia.xyz)

## Changelog

### v0.1.7 (2024-12-14)
- Initial Android bindings release
- Kotlin and Java API support
- Comprehensive test suite
- Example applications
- Performance optimizations for mobile
