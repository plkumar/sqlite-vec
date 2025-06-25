# SQLiteVec iOS Example

This example demonstrates how to use SQLiteVec in an iOS application, including mobile-specific optimizations and best practices.

## Features Demonstrated

- **iOS Document Storage**: Using the iOS Documents directory for persistent vector databases
- **Memory Management**: Optimized vector operations for mobile devices  
- **Device Capabilities**: Checking device memory and recommending vector dimensions
- **Background Processing**: Vector search that works with iOS app lifecycle
- **App Backup/Restore**: Exporting vector databases for backup
- **Vector Operations**: Complete demonstration of SQLiteVec capabilities on iOS

## Key iOS Considerations

### Storage Location
```swift
let documentsPath = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true).first!
let dbPath = "\(documentsPath)/vector_search.db"
```

### Memory Optimization
The example includes device capability checking:
- < 2GB RAM: Recommends 128-dimensional vectors
- < 4GB RAM: Recommends 384-dimensional vectors  
- ≥ 4GB RAM: Supports 768+ dimensional vectors

### Auto-loading for App Lifecycle
```swift
// Enable auto-loading for all database connections in the app
SQLiteVec.autoLoad()
```

## Running the Example

```bash
cd Examples/iOSExample
swift run
```

## Integration in Xcode Projects

1. Add the SQLiteVec Swift package to your Xcode project
2. Import the modules:
   ```swift
   import SQLite
   import SQLiteVec
   ```
3. Copy the example patterns for your use case

## Performance Notes

- Vector operations are optimized for mobile processors
- Batch operations reduce database overhead
- Memory usage is monitored for iOS memory constraints
- SQLite WAL mode works well with iOS background processing

## App Store Compatibility

This example demonstrates App Store-compatible usage:
- ✅ No external dependencies
- ✅ Statically linked SQLite extension
- ✅ Standard iOS file system usage
- ✅ No prohibited APIs or network dependencies
