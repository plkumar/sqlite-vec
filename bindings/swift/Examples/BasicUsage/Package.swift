// swift-tools-version:5.5

import PackageDescription

let package = Package(
    name: "BasicUsage",
    platforms: [
        .macOS(.v10_15)
    ],
    dependencies: [
        .package(path: "../..")
    ],
    targets: [
        .executableTarget(
            name: "BasicUsage",
            dependencies: ["SQLiteVec"]
        ),
    ]
)
