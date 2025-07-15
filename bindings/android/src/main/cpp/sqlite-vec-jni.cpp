#include <jni.h>
#include <string>
#include <vector>
#include <cstring>
#include <android/log.h>

// Include SQLite amalgamation or headers
#include "sqlite3.h"

// Include sqlite-vec
extern "C" {
    #include "sqlite-vec.h"
}

#define LOG_TAG "SQLiteVec"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Helper function to get database handle from Java SQLiteDatabase
sqlite3* getDatabaseHandle(JNIEnv* env, jobject database) {
    // This would need to be implemented based on the specific SQLite library being used
    // For Android's built-in SQLite, this requires reflection
    jclass databaseClass = env->GetObjectClass(database);
    jfieldID handleField = env->GetFieldID(databaseClass, "mNativeHandle", "J");
    if (handleField == nullptr) {
        // Try alternative field name for different Android versions
        handleField = env->GetFieldID(databaseClass, "mConnectionPtr", "J");
    }
    
    if (handleField != nullptr) {
        jlong handle = env->GetLongField(database, handleField);
        return reinterpret_cast<sqlite3*>(handle);
    }
    
    return nullptr;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_sqlite_vec_SQLiteVec_nativeGetVersion(JNIEnv* env, jclass clazz) {
    return env->NewStringUTF(SQLITE_VEC_VERSION);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_sqlite_vec_SQLiteVec_nativeInit(JNIEnv* env, jclass clazz, jlong dbHandle) {
    sqlite3* db = reinterpret_cast<sqlite3*>(dbHandle);
    if (db == nullptr) {
        LOGE("Invalid database handle");
        return SQLITE_ERROR;
    }
    
    char* errorMessage = nullptr;
    int result = sqlite3_vec_init(db, &errorMessage, nullptr);
    
    if (result != SQLITE_OK) {
        LOGE("Failed to initialize sqlite-vec: %s", errorMessage ? errorMessage : "Unknown error");
        if (errorMessage) {
            sqlite3_free(errorMessage);
        }
    } else {
        LOGI("sqlite-vec initialized successfully");
    }
    
    return result;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_sqlite_vec_SQLiteVec_nativeSerializeFloat32(JNIEnv* env, jclass clazz, jfloatArray vector) {
    jsize length = env->GetArrayLength(vector);
    jfloat* elements = env->GetFloatArrayElements(vector, nullptr);
    
    if (elements == nullptr) {
        return nullptr;
    }
    
    // Create byte array with the size of float array * sizeof(float)
    jsize byteLength = length * sizeof(float);
    jbyteArray result = env->NewByteArray(byteLength);
    
    if (result != nullptr) {
        env->SetByteArrayRegion(result, 0, byteLength, reinterpret_cast<const jbyte*>(elements));
    }
    
    env->ReleaseFloatArrayElements(vector, elements, JNI_ABORT);
    return result;
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_sqlite_vec_SQLiteVec_nativeDeserializeFloat32(JNIEnv* env, jclass clazz, jbyteArray data) {
    jsize byteLength = env->GetArrayLength(data);
    jbyte* bytes = env->GetByteArrayElements(data, nullptr);
    
    if (bytes == nullptr || byteLength % sizeof(float) != 0) {
        if (bytes) env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
        return nullptr;
    }
    
    jsize floatLength = byteLength / sizeof(float);
    jfloatArray result = env->NewFloatArray(floatLength);
    
    if (result != nullptr) {
        const float* floats = reinterpret_cast<const float*>(bytes);
        env->SetFloatArrayRegion(result, 0, floatLength, floats);
    }
    
    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
    return result;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_sqlite_vec_SQLiteVec_nativeSerializeInt8(JNIEnv* env, jclass clazz, jbyteArray vector) {
    // For Int8, we can just return a copy of the input array
    jsize length = env->GetArrayLength(vector);
    jbyteArray result = env->NewByteArray(length);
    
    if (result != nullptr) {
        jbyte* elements = env->GetByteArrayElements(vector, nullptr);
        if (elements != nullptr) {
            env->SetByteArrayRegion(result, 0, length, elements);
            env->ReleaseByteArrayElements(vector, elements, JNI_ABORT);
        }
    }
    
    return result;
}

extern "C" JNIEXPORT jdouble JNICALL
Java_com_sqlite_vec_SQLiteVec_nativeDistance(JNIEnv* env, jclass clazz, jlong dbHandle, 
                                              jbyteArray vector1, jbyteArray vector2, jstring metric) {
    sqlite3* db = reinterpret_cast<sqlite3*>(dbHandle);
    if (db == nullptr) {
        return -1.0;
    }
    
    const char* metricStr = env->GetStringUTFChars(metric, nullptr);
    
    // Prepare SQL statement based on metric
    std::string sql;
    if (strcmp(metricStr, "cosine") == 0) {
        sql = "SELECT vec_distance_cosine(?, ?)";
    } else if (strcmp(metricStr, "l2") == 0) {
        sql = "SELECT vec_distance_l2(?, ?)";
    } else {
        sql = "SELECT vec_distance_cosine(?, ?)"; // default to cosine
    }
    
    env->ReleaseStringUTFChars(metric, metricStr);
    
    sqlite3_stmt* stmt;
    int rc = sqlite3_prepare_v2(db, sql.c_str(), -1, &stmt, nullptr);
    if (rc != SQLITE_OK) {
        LOGE("Failed to prepare distance query: %s", sqlite3_errmsg(db));
        return -1.0;
    }
    
    // Bind vector parameters
    jsize len1 = env->GetArrayLength(vector1);
    jbyte* bytes1 = env->GetByteArrayElements(vector1, nullptr);
    sqlite3_bind_blob(stmt, 1, bytes1, len1, SQLITE_STATIC);
    
    jsize len2 = env->GetArrayLength(vector2);
    jbyte* bytes2 = env->GetByteArrayElements(vector2, nullptr);
    sqlite3_bind_blob(stmt, 2, bytes2, len2, SQLITE_STATIC);
    
    double result = -1.0;
    if (sqlite3_step(stmt) == SQLITE_ROW) {
        result = sqlite3_column_double(stmt, 0);
    }
    
    env->ReleaseByteArrayElements(vector1, bytes1, JNI_ABORT);
    env->ReleaseByteArrayElements(vector2, bytes2, JNI_ABORT);
    sqlite3_finalize(stmt);
    
    return result;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_sqlite_vec_SQLiteVec_nativeVectorLength(JNIEnv* env, jclass clazz, jlong dbHandle, jbyteArray vector) {
    sqlite3* db = reinterpret_cast<sqlite3*>(dbHandle);
    if (db == nullptr) {
        return -1;
    }
    
    sqlite3_stmt* stmt;
    int rc = sqlite3_prepare_v2(db, "SELECT vec_length(?)", -1, &stmt, nullptr);
    if (rc != SQLITE_OK) {
        return -1;
    }
    
    jsize len = env->GetArrayLength(vector);
    jbyte* bytes = env->GetByteArrayElements(vector, nullptr);
    sqlite3_bind_blob(stmt, 1, bytes, len, SQLITE_STATIC);
    
    int result = -1;
    if (sqlite3_step(stmt) == SQLITE_ROW) {
        result = sqlite3_column_int(stmt, 0);
    }
    
    env->ReleaseByteArrayElements(vector, bytes, JNI_ABORT);
    sqlite3_finalize(stmt);
    
    return result;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_sqlite_vec_SQLiteVec_nativeNormalize(JNIEnv* env, jclass clazz, jlong dbHandle, jbyteArray vector) {
    sqlite3* db = reinterpret_cast<sqlite3*>(dbHandle);
    if (db == nullptr) {
        return nullptr;
    }
    
    sqlite3_stmt* stmt;
    int rc = sqlite3_prepare_v2(db, "SELECT vec_normalize(?)", -1, &stmt, nullptr);
    if (rc != SQLITE_OK) {
        return nullptr;
    }
    
    jsize len = env->GetArrayLength(vector);
    jbyte* bytes = env->GetByteArrayElements(vector, nullptr);
    sqlite3_bind_blob(stmt, 1, bytes, len, SQLITE_STATIC);
    
    jbyteArray result = nullptr;
    if (sqlite3_step(stmt) == SQLITE_ROW) {
        const void* blob = sqlite3_column_blob(stmt, 0);
        int blobSize = sqlite3_column_bytes(stmt, 0);
        
        result = env->NewByteArray(blobSize);
        if (result != nullptr) {
            env->SetByteArrayRegion(result, 0, blobSize, static_cast<const jbyte*>(blob));
        }
    }
    
    env->ReleaseByteArrayElements(vector, bytes, JNI_ABORT);
    sqlite3_finalize(stmt);
    
    return result;
}
