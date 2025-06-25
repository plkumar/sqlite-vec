import Foundation
import SQLite
import CSQLiteVec
import SQLite3

/// Swift bindings for the sqlite-vec SQLite extension
public class SQLiteVec {
    
    /// Load sqlite-vec into a SQLite.swift database connection
    /// - Parameter database: The SQLite.swift database connection
    /// - Throws: Database.Error if loading fails
    public static func load(database: Connection) throws {
        // Get the raw sqlite3 handle from SQLite.swift
        let handle = database.handle
        
        // Initialize sqlite-vec directly using the C function
        var errorMessage: UnsafeMutablePointer<CChar>? = nil
        let result = sqlite3_vec_init(handle, &errorMessage, nil)
        
        if result != SQLITE_OK {
            let error = errorMessage != nil ? String(cString: errorMessage!) : "Unknown error loading sqlite-vec"
            if let errorMessage = errorMessage {
                sqlite3_free(errorMessage)
            }
            throw NSError(domain: "SQLiteVec", code: Int(result), userInfo: [NSLocalizedDescriptionKey: error])
        }
    }
    
    /// Initialize sqlite-vec for auto-loading in all future database connections
    /// Note: Auto-extension is deprecated on Apple platforms. Consider using manual loading instead.
    public static func autoLoad() {
        #if !os(iOS) && !os(macOS) && !os(watchOS) && !os(tvOS)
        let autoExtensionFunction: @convention(c) (OpaquePointer?, UnsafeMutablePointer<UnsafeMutablePointer<CChar>?>?, UnsafePointer<sqlite3_api_routines>?) -> Int32 = { db, pzErrMsg, pApi in
            return sqlite3_vec_init(db, pzErrMsg, pApi)
        }
        sqlite3_auto_extension(unsafeBitCast(autoExtensionFunction, to: (@convention(c) () -> Void).self))
        #else
        print("Warning: Auto-extension is deprecated on Apple platforms. Use manual loading with SQLiteVec.load(database:) instead.")
        #endif
    }
    
    /// Cancel auto-loading of sqlite-vec for future database connections
    /// Note: Auto-extension is deprecated on Apple platforms.
    public static func cancelAutoLoad() {
        #if !os(iOS) && !os(macOS) && !os(watchOS) && !os(tvOS)
        let autoExtensionFunction: @convention(c) (OpaquePointer?, UnsafeMutablePointer<UnsafeMutablePointer<CChar>?>?, UnsafePointer<sqlite3_api_routines>?) -> Int32 = { db, pzErrMsg, pApi in
            return sqlite3_vec_init(db, pzErrMsg, pApi)
        }
        sqlite3_cancel_auto_extension(unsafeBitCast(autoExtensionFunction, to: (@convention(c) () -> Void).self))
        #else
        print("Warning: Auto-extension cancellation is not supported on Apple platforms.")
        #endif
    }
    
    /// Get the version of sqlite-vec
    /// - Parameter database: The SQLite.swift database connection
    /// - Returns: The version string
    /// - Throws: Database.Error if the query fails
    public static func version(database: Connection) throws -> String {
        let result = try database.scalar("SELECT vec_version()") as! String
        return result
    }
}

// MARK: - Vector Serialization

extension SQLiteVec {
    
    /// Serialize a Float32 array into raw bytes format that sqlite-vec expects
    /// - Parameter vector: Array of Float32 values
    /// - Returns: Data containing the serialized vector
    public static func serialize(_ vector: [Float32]) -> Data {
        return Data(bytes: vector, count: vector.count * MemoryLayout<Float32>.size)
    }
    
    /// Serialize a Float64 array into raw bytes format that sqlite-vec expects
    /// - Parameter vector: Array of Float64 values  
    /// - Returns: Data containing the serialized vector
    public static func serialize(_ vector: [Float64]) -> Data {
        return Data(bytes: vector, count: vector.count * MemoryLayout<Float64>.size)
    }
    
    /// Serialize an Int8 array into raw bytes format that sqlite-vec expects
    /// - Parameter vector: Array of Int8 values
    /// - Returns: Data containing the serialized vector
    public static func serialize(_ vector: [Int8]) -> Data {
        return Data(bytes: vector, count: vector.count * MemoryLayout<Int8>.size)
    }
    
    /// Serialize an array of floats into raw bytes format
    /// - Parameter vector: Array of float values
    /// - Returns: Data containing the serialized vector
    public static func serializeFloat32(_ vector: [Float]) -> Data {
        let float32Vector = vector.map { Float32($0) }
        return serialize(float32Vector)
    }
    
    /// Deserialize raw bytes into a Float32 array
    /// - Parameter data: Raw bytes from sqlite-vec
    /// - Returns: Array of Float32 values
    public static func deserializeFloat32(_ data: Data) -> [Float32] {
        return data.withUnsafeBytes { bytes in
            let buffer = bytes.bindMemory(to: Float32.self)
            return Array(buffer)
        }
    }
    
    /// Deserialize raw bytes into an Int8 array
    /// - Parameter data: Raw bytes from sqlite-vec
    /// - Returns: Array of Int8 values
    public static func deserializeInt8(_ data: Data) -> [Int8] {
        return data.withUnsafeBytes { bytes in
            let buffer = bytes.bindMemory(to: Int8.self)
            return Array(buffer)
        }
    }
}

// MARK: - Convenience Extensions

extension Connection {
    
    /// Load the sqlite-vec extension into this database connection
    /// - Throws: Database.Error if loading fails
    public func loadSQLiteVec() throws {
        try SQLiteVec.load(database: self)
    }
}

// MARK: - Vector Operations

extension SQLiteVec {
    
    /// Calculate the distance between two vectors using sqlite-vec
    /// - Parameters:
    ///   - database: The SQLite.swift database connection
    ///   - vector1: First vector as Float32 array
    ///   - vector2: Second vector as Float32 array
    ///   - metric: Distance metric (default: "cosine")
    /// - Returns: The calculated distance
    /// - Throws: Database.Error if the query fails
    public static func distance(
        database: Connection,
        vector1: [Float32],
        vector2: [Float32],
        metric: String = "cosine"
    ) throws -> Double {
        let data1 = serialize(vector1)
        let data2 = serialize(vector2)
        
        let query: String
        switch metric.lowercased() {
        case "cosine":
            query = "SELECT vec_distance_cosine(?, ?)"
        case "l2":
            query = "SELECT vec_distance_l2(?, ?)"
        default:
            query = "SELECT vec_distance_cosine(?, ?)"
        }
        
        let result = try database.scalar(query, [Blob(bytes: [UInt8](data1)), Blob(bytes: [UInt8](data2))]) as! Double
        return result
    }
    
    /// Get the length (number of dimensions) of a vector
    /// - Parameters:
    ///   - database: The SQLite.swift database connection
    ///   - vector: Vector as Float32 array
    /// - Returns: The number of dimensions
    /// - Throws: Database.Error if the query fails
    public static func length(database: Connection, vector: [Float32]) throws -> Int {
        let data = serialize(vector)
        let result = try database.scalar("SELECT vec_length(?)", [Blob(bytes: [UInt8](data))]) as! Int64
        return Int(result)
    }
    
    /// Normalize a vector using sqlite-vec
    /// - Parameters:
    ///   - database: The SQLite.swift database connection
    ///   - vector: Vector as Float32 array to normalize
    /// - Returns: Normalized vector as Float32 array
    /// - Throws: Database.Error if the query fails
    public static func normalize(database: Connection, vector: [Float32]) throws -> [Float32] {
        let data = serialize(vector)
        let resultData = try database.scalar("SELECT vec_normalize(?)", [Blob(bytes: [UInt8](data))]) as! Blob
        return deserializeFloat32(Data(resultData.bytes))
    }
}

// MARK: - Vector Table Operations

extension SQLiteVec {
    
    /// Create a vec0 virtual table
    /// - Parameters:
    ///   - database: The SQLite.swift database connection
    ///   - tableName: Name of the table to create
    ///   - vectorColumn: Name and definition of the vector column (e.g., "embedding float[384]")
    ///   - additionalColumns: Additional column definitions
    /// - Throws: Database.Error if the creation fails
    public static func createVectorTable(
        database: Connection,
        tableName: String,
        vectorColumn: String,
        additionalColumns: [String] = []
    ) throws {
        var columns = [vectorColumn]
        columns.append(contentsOf: additionalColumns)
        let columnDefinitions = columns.joined(separator: ", ")
        
        let sql = "CREATE VIRTUAL TABLE \(tableName) USING vec0(\(columnDefinitions))"
        try database.execute(sql)
    }
    
    /// Search for similar vectors in a vec0 table
    /// - Parameters:
    ///   - database: The SQLite.swift database connection
    ///   - tableName: Name of the vec0 table
    ///   - vectorColumn: Name of the vector column
    ///   - queryVector: Vector to search for similarities
    ///   - limit: Maximum number of results (default: 10)
    ///   - additionalColumns: Additional columns to select
    /// - Returns: Array of search results
    /// - Throws: Database.Error if the search fails
    public static func searchSimilar(
        database: Connection,
        tableName: String,
        vectorColumn: String,
        queryVector: [Float32],
        limit: Int = 10,
        additionalColumns: [String] = []
    ) throws -> [[Any?]] {
        let queryData = serialize(queryVector)
        
        var selectColumns = ["rowid", "distance"]
        selectColumns.append(contentsOf: additionalColumns)
        let selectClause = selectColumns.joined(separator: ", ")
        
        let sql = """
            SELECT \(selectClause)
            FROM \(tableName)
            WHERE \(vectorColumn) MATCH ?
            ORDER BY distance
            LIMIT ?
        """
        
        var results: [[Any?]] = []
        for row in try database.prepare(sql, [Blob(bytes: [UInt8](queryData)), limit]) {
            var rowData: [Any?] = []
            for i in 0..<selectColumns.count {
                rowData.append(row[i])
            }
            results.append(rowData)
        }
        
        return results
    }
}

// MARK: - Additional Utility Functions (Go bindings compatibility)

extension SQLiteVec {
    
    /// Serialize a vector array for Int8 vectors similar to Go bindings
    /// - Parameter vector: Array of Int8 values  
    /// - Returns: Data containing the serialized vector
    public static func serializeInt8(_ vector: [Int8]) -> Data {
        return serialize(vector)
    }
    
    /// Check if sqlite-vec is available in the database
    /// - Parameter database: The SQLite.swift database connection
    /// - Returns: True if sqlite-vec is loaded and available
    public static func isAvailable(database: Connection) -> Bool {
        do {
            _ = try version(database: database)
            return true
        } catch {
            return false
        }
    }
    
    /// Load sqlite-vec if not already loaded
    /// - Parameter database: The SQLite.swift database connection
    /// - Throws: Database.Error if loading fails
    public static func ensureLoaded(database: Connection) throws {
        if !isAvailable(database: database) {
            try load(database: database)
        }
    }
}

// MARK: - Vector Math Utilities

extension SQLiteVec {
    
    /// Calculate dot product between two vectors using sqlite-vec
    /// - Parameters:
    ///   - database: The SQLite.swift database connection
    ///   - vector1: First vector as Float32 array
    ///   - vector2: Second vector as Float32 array
    /// - Returns: The dot product result
    /// - Throws: Database.Error if the query fails
    public static func dotProduct(
        database: Connection,
        vector1: [Float32],
        vector2: [Float32]
    ) throws -> Double {
        let data1 = serialize(vector1)
        let data2 = serialize(vector2)
        
        let result = try database.scalar("SELECT vec_distance_cosine(?, ?)", [Blob(bytes: [UInt8](data1)), Blob(bytes: [UInt8](data2))]) as! Double
        return result
    }
    
    /// Add two vectors using sqlite-vec
    /// - Parameters:
    ///   - database: The SQLite.swift database connection
    ///   - vector1: First vector as Float32 array
    ///   - vector2: Second vector as Float32 array
    /// - Returns: The resulting vector as Float32 array
    /// - Throws: Database.Error if the query fails
    public static func add(
        database: Connection,
        vector1: [Float32],
        vector2: [Float32]
    ) throws -> [Float32] {
        let data1 = serialize(vector1)
        let data2 = serialize(vector2)
        
        let resultData = try database.scalar("SELECT vec_add(?, ?)", [Blob(bytes: [UInt8](data1)), Blob(bytes: [UInt8](data2))]) as! Blob
        return deserializeFloat32(Data(resultData.bytes))
    }
    
    /// Subtract two vectors using sqlite-vec
    /// - Parameters:
    ///   - database: The SQLite.swift database connection
    ///   - vector1: First vector as Float32 array
    ///   - vector2: Second vector as Float32 array
    /// - Returns: The resulting vector as Float32 array
    /// - Throws: Database.Error if the query fails
    public static func subtract(
        database: Connection,
        vector1: [Float32],
        vector2: [Float32]
    ) throws -> [Float32] {
        let data1 = serialize(vector1)
        let data2 = serialize(vector2)
        
        let resultData = try database.scalar("SELECT vec_sub(?, ?)", [Blob(bytes: [UInt8](data1)), Blob(bytes: [UInt8](data2))]) as! Blob
        return deserializeFloat32(Data(resultData.bytes))
    }
    
    /// Convert a vector to JSON representation using sqlite-vec
    /// - Parameters:
    ///   - database: The SQLite.swift database connection
    ///   - vector: Vector as Float32 array
    /// - Returns: JSON string representation of the vector
    /// - Throws: Database.Error if the query fails
    public static func toJSON(database: Connection, vector: [Float32]) throws -> String {
        let data = serialize(vector)
        let result = try database.scalar("SELECT vec_to_json(?)", [Blob(bytes: [UInt8](data))]) as! String
        return result
    }
}

// MARK: - Batch Operations (Go-style)

extension SQLiteVec {
    
    /// Insert multiple vectors in a batch operation for better performance
    /// Similar to Go bindings pattern for bulk inserts
    /// - Parameters:
    ///   - database: The SQLite.swift database connection
    ///   - tableName: Name of the vec0 table
    ///   - vectorColumn: Name of the vector column
    ///   - vectors: Array of tuples containing (id, vector) pairs
    /// - Throws: Database.Error if the operation fails
    public static func insertVectorsBatch(
        database: Connection,
        tableName: String,
        vectorColumn: String,
        vectors: [(Int64, [Float32])]
    ) throws {
        try database.transaction {
            let sql = "INSERT INTO \(tableName)(rowid, \(vectorColumn)) VALUES (?, ?)"
            for (id, vector) in vectors {
                let vectorData = serialize(vector)
                try database.run(sql, id, Blob(bytes: [UInt8](vectorData)))
            }
        }
    }
    
    /// Search similar vectors with additional filter conditions
    /// More flexible version similar to Go bindings approach
    /// - Parameters:
    ///   - database: The SQLite.swift database connection
    ///   - tableName: Name of the vec0 table
    ///   - vectorColumn: Name of the vector column
    ///   - queryVector: Vector to search for similarities
    ///   - limit: Maximum number of results
    ///   - whereClause: Additional WHERE conditions (optional)
    ///   - parameters: Parameters for the WHERE clause
    /// - Returns: Array of search results
    /// - Throws: Database.Error if the search fails
    public static func searchSimilarWithFilter(
        database: Connection,
        tableName: String,
        vectorColumn: String,
        queryVector: [Float32],
        limit: Int = 10,
        whereClause: String? = nil,
        parameters: [Binding?] = []
    ) throws -> [[Any?]] {
        let queryData = serialize(queryVector)
        
        var sql = """
            SELECT rowid, distance
            FROM \(tableName)
            WHERE \(vectorColumn) MATCH ?
        """
        
        var allParameters: [Binding?] = [Blob(bytes: [UInt8](queryData))]
        
        if let whereClause = whereClause {
            sql += " AND (\(whereClause))"
            allParameters.append(contentsOf: parameters)
        }
        
        sql += " ORDER BY distance LIMIT ?"
        allParameters.append(limit)
        
        var results: [[Any?]] = []
        for row in try database.prepare(sql, allParameters) {
            results.append([row[0], row[1]])
        }
        
        return results
    }
}
