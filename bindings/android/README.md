# SQLiteVec Android Bindings

Android bindings for [sqlite-vec](https://github.com/asg017/sqlite-vec), providing vector search capabilities in SQLite databases on Android.

## Features

- 🚀 **Fast vector search** - Powered by the sqlite-vec SQLite extension
- 📱 **Android optimized** - Built specifically for Android using SupportSQLiteDatabase
- 🔧 **Easy to use** - Simple Java API similar to Swift bindings
- 🧮 **Multiple data types** - Support for Float32 and Int8 vectors
- 📦 **No external dependencies** - Uses only standard Android SQLite libraries
- 🔄 **Batch operations** - Efficient bulk vector operations
- 🎯 **Similar to Swift bindings** - Consistent API across platforms

## Requirements

- Android API 16+
- NDK for building native components
- androidx.sqlite library

## Installation

### Using gradle (if published)

```gradle
dependencies {
    implementation 'com.sqlite:sqlite-vec:0.1.7'
}
```

### Manual Installation

1. Copy the `bindings/android` directory to your project
2. Add the native library to your `app/src/main/jniLibs/` directory
3. Add dependencies to your `build.gradle`:

```gradle
dependencies {
    implementation 'androidx.sqlite:sqlite:2.4.0'
    implementation 'androidx.sqlite:sqlite-framework:2.4.0'
}
```

## Quick Start

### Basic Usage

```java
import com.sqlite.vec.SQLiteVec;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;

// Create database helper
SupportSQLiteOpenHelper.Configuration config = SupportSQLiteOpenHelper.Configuration.builder(context)
    .name("vector_db")
    .callback(new SupportSQLiteOpenHelper.Callback(1) {
        @Override
        public void onCreate(SupportSQLiteDatabase db) {
            // Load sqlite-vec extension
            try {
                SQLiteVec.load(db);
            } catch (SQLiteVec.SQLiteVecException e) {
                Log.e("DB", "Failed to load sqlite-vec", e);
            }
        }
        
        @Override
        public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
            // Handle upgrades
        }
    })
    .build();

SupportSQLiteOpenHelper helper = new FrameworkSQLiteOpenHelperFactory().create(config);
SupportSQLiteDatabase database = helper.getWritableDatabase();

// Check version
String version = SQLiteVec.version(database);
Log.i("SQLiteVec", "Version: " + version);
```

### Creating Vector Tables

```java
// Create a vector table
SQLiteVec.createVectorTable(database, "embeddings", "vector float[384]");

// Create with additional columns
SQLiteVec.createVectorTable(database, "documents", "embedding float[384]", 
    new String[]{"title TEXT", "content TEXT", "category TEXT"});
```

### Vector Operations

```java
float[] vector1 = {0.1f, 0.2f, 0.3f, 0.4f};
float[] vector2 = {0.2f, 0.3f, 0.4f, 0.5f};

// Calculate distances
double cosineDistance = SQLiteVec.distance(database, vector1, vector2, "cosine");
double l2Distance = SQLiteVec.distance(database, vector1, vector2, "l2");

// Vector math operations
float[] normalized = SQLiteVec.normalize(database, vector1);
float[] sum = SQLiteVec.add(database, vector1, vector2);
float[] diff = SQLiteVec.subtract(database, vector1, vector2);

// Get vector length
int dimensions = SQLiteVec.length(database, vector1);

// Convert to JSON
String json = SQLiteVec.toJson(database, vector1);
```

### Vector Search

```java
// Insert vectors
List<SQLiteVec.VectorEntry> vectors = new ArrayList<>();
vectors.add(new SQLiteVec.VectorEntry(1, new float[]{0.1f, 0.1f, 0.1f, 0.1f}));
vectors.add(new SQLiteVec.VectorEntry(2, new float[]{0.2f, 0.2f, 0.2f, 0.2f}));
vectors.add(new SQLiteVec.VectorEntry(3, new float[]{0.3f, 0.3f, 0.3f, 0.3f}));

SQLiteVec.insertVectorsBatch(database, "embeddings", "vector", vectors);

// Search for similar vectors
float[] queryVector = {0.15f, 0.15f, 0.15f, 0.15f};
List<SQLiteVec.SearchResult> results = SQLiteVec.searchSimilar(
    database, "embeddings", "vector", queryVector, 10);

for (SQLiteVec.SearchResult result : results) {
    Log.i("Search", "Row ID: " + result.rowId + ", Distance: " + result.distance);
}
```

### Serialization

```java
// Serialize vectors for storage
float[] vector = {1.0f, 2.0f, 3.0f, 4.0f};
byte[] serialized = SQLiteVec.serialize(vector);

// Deserialize vectors
float[] deserialized = SQLiteVec.deserializeFloat32(serialized);

// Int8 vectors
byte[] int8Vector = {1, 2, 3, 4, 5};
byte[] serializedInt8 = SQLiteVec.serialize(int8Vector);
byte[] deserializedInt8 = SQLiteVec.deserializeInt8(serializedInt8);
```

## Advanced Usage

### Batch Operations

```java
// Efficient batch insertion
List<SQLiteVec.VectorEntry> largeVectorSet = new ArrayList<>();
for (int i = 0; i < 10000; i++) {
    float[] randomVector = generateRandomVector(384);
    largeVectorSet.add(new SQLiteVec.VectorEntry(i, randomVector));
}

SQLiteVec.insertVectorsBatch(database, "large_embeddings", "vector", largeVectorSet);
```

### Custom Database Setup

```java
// Custom database setup with sqlite-vec
SupportSQLiteOpenHelper.Configuration config = SupportSQLiteOpenHelper.Configuration.builder(context)
    .name("my_vector_db")
    .callback(new SupportSQLiteOpenHelper.Callback(1) {
        @Override
        public void onCreate(SupportSQLiteDatabase db) {
            try {
                // Load sqlite-vec
                SQLiteVec.load(db);
                
                // Create multiple vector tables
                SQLiteVec.createVectorTable(db, "user_profiles", "preferences float[256]");
                SQLiteVec.createVectorTable(db, "product_embeddings", "features float[512]", 
                    new String[]{"product_id TEXT", "category TEXT", "price REAL"});
                
            } catch (SQLiteVec.SQLiteVecException e) {
                Log.e("DB", "Database setup failed", e);
            }
        }
        
        @Override
        public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
            // Handle schema upgrades
        }
    })
    .build();
```

### Error Handling

```java
try {
    // Check if sqlite-vec is available
    if (SQLiteVec.isAvailable(database)) {
        // Perform vector operations
        float[] result = SQLiteVec.normalize(database, inputVector);
    } else {
        // Load sqlite-vec if not available
        SQLiteVec.ensureLoaded(database);
    }
} catch (SQLiteVec.SQLiteVecException e) {
    Log.e("SQLiteVec", "Vector operation failed", e);
    // Handle error appropriately
}
```

## API Reference

### Main Classes

- **SQLiteVec** - Main class with all sqlite-vec functionality
- **VectorOperations** - Utility class for vector math operations (legacy compatibility)
- **VectorTable** - Utility class for vector table management (legacy compatibility)

### Key Methods

#### SQLiteVec

- `load(SupportSQLiteDatabase)` - Load sqlite-vec extension
- `version(SupportSQLiteDatabase)` - Get sqlite-vec version
- `isAvailable(SupportSQLiteDatabase)` - Check if sqlite-vec is loaded
- `ensureLoaded(SupportSQLiteDatabase)` - Load sqlite-vec if not already loaded
- `serialize(float[])` - Serialize float array to bytes
- `deserializeFloat32(byte[])` - Deserialize bytes to float array
- `distance(SupportSQLiteDatabase, float[], float[], String)` - Calculate vector distance
- `normalize(SupportSQLiteDatabase, float[])` - Normalize vector
- `add(SupportSQLiteDatabase, float[], float[])` - Add vectors
- `subtract(SupportSQLiteDatabase, float[], float[])` - Subtract vectors
- `createVectorTable(SupportSQLiteDatabase, String, String, String[])` - Create vector table
- `searchSimilar(SupportSQLiteDatabase, String, String, float[], int)` - Search similar vectors
- `insertVectorsBatch(SupportSQLiteDatabase, String, String, List<VectorEntry>)` - Batch insert vectors

#### Data Classes

- **SQLiteVec.VectorEntry** - Represents a vector with ID for batch operations
- **SQLiteVec.SearchResult** - Represents a search result with row ID and distance
- **SQLiteVec.SQLiteVecException** - Exception thrown by SQLiteVec operations

## Comparison with Swift Bindings

The Android bindings provide similar functionality to the Swift bindings:

### Android vs Swift

```java
// Android
SQLiteVec.load(database);
String version = SQLiteVec.version(database);
double distance = SQLiteVec.distance(database, vec1, vec2, "cosine");
float[] normalized = SQLiteVec.normalize(database, vector);
```

```swift
// Swift
try SQLiteVec.load(database: db)
let version = try SQLiteVec.version(database: db)
let distance = try SQLiteVec.distance(database: db, vector1: vec1, vector2: vec2, metric: "cosine")
let normalized = try SQLiteVec.normalize(database: db, vector: vector)
```

### Key Differences

1. **Error Handling**: Android uses checked exceptions, Swift uses throw/try
2. **Database Interface**: Android uses SupportSQLiteDatabase, Swift uses SQLite.swift Connection
3. **Memory Management**: Android uses automatic GC, Swift uses ARC
4. **Null Safety**: Android uses traditional null checks, Swift uses optionals

## Building from Source

1. Clone the repository
2. Install Android NDK
3. Run `./gradlew assembleRelease` in the `bindings/android` directory
4. The AAR file will be generated in `build/outputs/aar/`

## Examples

See the `example` directory for complete usage examples:
- `SQLiteVecDemo.java` - Basic usage demonstration
- Advanced examples with Room database integration
- Performance benchmarks and optimization tips

## License

This project follows the same license as the main sqlite-vec project.

## Contributing

Contributions are welcome! Please ensure:
1. Code follows Android best practices
2. Tests pass on multiple Android versions
3. Documentation is updated for new features
4. Compatibility with the Swift bindings API where possible

## Support

For Android-specific issues, please check:
1. Android version compatibility
2. NDK version requirements
3. ProGuard/R8 configuration if using code obfuscation
4. JNI library loading issues on different architectures

For general sqlite-vec issues, see the main [sqlite-vec repository](https://github.com/asg017/sqlite-vec).
