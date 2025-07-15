plugins {
    id("com.android.library") version "8.1.0"
    id("org.jetbrains.kotlin.android") version "1.9.0"
    id("maven-publish")
    id("signing")
}

android {
    namespace = "com.sqlite.vec"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        targetSdk = 34

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }

        externalNativeBuild {
            cmake {
                cppFlags.add("-std=c++17")
                arguments.add("-DSQLITE_VEC_STATIC=1")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.sqlite:sqlite:2.4.0")
    implementation("androidx.sqlite:sqlite-ktx:2.4.0")

    // Testing dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.sqlite"
            artifactId = "sqlite-vec-android"
            version = "0.1.7"

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("SQLite Vec Android Bindings")
                description.set("Android bindings for sqlite-vec - Vector search for SQLite")
                url.set("https://github.com/asg017/sqlite-vec")
                
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                
                developers {
                    developer {
                        id.set("asg017")
                        name.set("Alex Garcia")
                        email.set("alex@alexgarcia.xyz")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/asg017/sqlite-vec.git")
                    developerConnection.set("scm:git:ssh://github.com:asg017/sqlite-vec.git")
                    url.set("https://github.com/asg017/sqlite-vec")
                }
            }
        }
    }
}
