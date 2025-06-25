// swift-tools-version:5.5

import PackageDescription

let package = Package(
    name: "iOSExample",
    platforms: [
        .macOS(.v10_15), // Match the main package requirements
        .iOS(.v13)
    ],
    dependencies: [
        .package(name: "swift", path: "../../") // Local SQLiteVec package
    ],
    targets: [
        .executableTarget(
            name: "iOSExample",
            dependencies: [
                .product(name: "SQLiteVec", package: "swift")
            ]
        )
    ]
)
