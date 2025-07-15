#include <jni.h>
#include <string.h>
#include <android/log.h>
#include "sqlite-vec.h"
#include "sqlite3.h"

#define LOG_TAG "SQLiteVec"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// JNI function to initialize sqlite-vec
JNIEXPORT jint JNICALL
Java_com_sqlite_vec_SQLiteVec_nativeInit(JNIEnv *env, jclass clazz, jlong db_pointer) {
    sqlite3 *db = (sqlite3 *) db_pointer;
    char *error_msg = NULL;
    
    int result = sqlite3_vec_init(db, &error_msg, NULL);
    
    if (result != SQLITE_OK && error_msg) {
        LOGE("sqlite3_vec_init failed: %s", error_msg);
        sqlite3_free(error_msg);
    }
    
    return result;
}

// JNI function to get sqlite-vec version
JNIEXPORT jstring JNICALL
Java_com_sqlite_vec_SQLiteVec_nativeGetVersion(JNIEnv *env, jclass clazz) {
    return (*env)->NewStringUTF(env, SQLITE_VEC_VERSION);
}

// JNI function to serialize float32 array
JNIEXPORT jbyteArray JNICALL
Java_com_sqlite_vec_SQLiteVec_nativeSerializeFloat32(JNIEnv *env, jclass clazz, jfloatArray vector) {
    jsize length = (*env)->GetArrayLength(env, vector);
    jfloat *elements = (*env)->GetFloatArrayElements(env, vector, NULL);
    
    // Create byte array for serialized data
    jbyteArray result = (*env)->NewByteArray(env, length * sizeof(float));
    (*env)->SetByteArrayRegion(env, result, 0, length * sizeof(float), (jbyte *) elements);
    
    (*env)->ReleaseFloatArrayElements(env, vector, elements, 0);
    
    return result;
}

// JNI function to deserialize float32 array
JNIEXPORT jfloatArray JNICALL
Java_com_sqlite_vec_SQLiteVec_nativeDeserializeFloat32(JNIEnv *env, jclass clazz, jbyteArray data) {
    jsize data_length = (*env)->GetArrayLength(env, data);
    jbyte *data_bytes = (*env)->GetByteArrayElements(env, data, NULL);
    
    // Calculate number of floats
    jsize float_count = data_length / sizeof(float);
    
    // Create float array
    jfloatArray result = (*env)->NewFloatArray(env, float_count);
    (*env)->SetFloatArrayRegion(env, result, 0, float_count, (jfloat *) data_bytes);
    
    (*env)->ReleaseByteArrayElements(env, data, data_bytes, 0);
    
    return result;
}

// JNI function to serialize int8 array
JNIEXPORT jbyteArray JNICALL
Java_com_sqlite_vec_SQLiteVec_nativeSerializeInt8(JNIEnv *env, jclass clazz, jbyteArray vector) {
    jsize length = (*env)->GetArrayLength(env, vector);
    jbyte *elements = (*env)->GetByteArrayElements(env, vector, NULL);
    
    // Create byte array for serialized data (same as input for int8)
    jbyteArray result = (*env)->NewByteArray(env, length);
    (*env)->SetByteArrayRegion(env, result, 0, length, elements);
    
    (*env)->ReleaseByteArrayElements(env, vector, elements, 0);
    
    return result;
}

// JNI function to deserialize int8 array
JNIEXPORT jbyteArray JNICALL
Java_com_sqlite_vec_SQLiteVec_nativeDeserializeInt8(JNIEnv *env, jclass clazz, jbyteArray data) {
    jsize data_length = (*env)->GetArrayLength(env, data);
    jbyte *data_bytes = (*env)->GetByteArrayElements(env, data, NULL);
    
    // Create byte array (same as input for int8)
    jbyteArray result = (*env)->NewByteArray(env, data_length);
    (*env)->SetByteArrayRegion(env, result, 0, data_length, data_bytes);
    
    (*env)->ReleaseByteArrayElements(env, data, data_bytes, 0);
    
    return result;
}

// JNI function to check if sqlite-vec is loaded
JNIEXPORT jboolean JNICALL
Java_com_sqlite_vec_SQLiteVec_nativeIsLoaded(JNIEnv *env, jclass clazz, jlong db_pointer) {
    sqlite3 *db = (sqlite3 *) db_pointer;
    sqlite3_stmt *stmt;
    
    int rc = sqlite3_prepare_v2(db, "SELECT vec_version()", -1, &stmt, NULL);
    if (rc != SQLITE_OK) {
        return JNI_FALSE;
    }
    
    rc = sqlite3_step(stmt);
    sqlite3_finalize(stmt);
    
    return (rc == SQLITE_ROW) ? JNI_TRUE : JNI_FALSE;
}
