// swift-tools-version:5.5

import PackageDescription

let package = Package(
    name: "SQLiteVec",
    platforms: [
        .macOS(.v10_15),
        .iOS(.v13),
        .watchOS(.v6),
        .tvOS(.v13)
    ],
    products: [
        .library(
            name: "SQLiteVec",
            targets: ["SQLiteVec"]
        ),
    ],
    dependencies: [
        .package(url: "https://github.com/stephencelis/SQLite.swift.git", from: "0.14.1")
    ],
    targets: [
        .target(
            name: "CSQLiteVec",
            dependencies: [],
            sources: ["sqlite-vec.c"],
            publicHeadersPath: "include",
            cSettings: [
                .define("SQLITE_CORE"),
                .define("SQLITE_VEC_STATIC"),
                .headerSearchPath("include")
            ]
        ),
        .target(
            name: "SQLiteVec",
            dependencies: [
                "CSQLiteVec",
                .product(name: "SQLite", package: "SQLite.swift")
            ]
        ),
        .testTarget(
            name: "SQLiteVecTests",
            dependencies: ["SQLiteVec"]
        ),
    ]
)
