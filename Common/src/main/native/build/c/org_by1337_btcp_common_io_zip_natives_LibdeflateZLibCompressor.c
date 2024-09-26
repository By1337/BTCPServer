#include <jni.h>
#include "org_by1337_btcp_common_io_zip_natives_LibdeflateZLibCompressor.h"
#include "libdeflate/libdeflate.h"


JNIEXPORT jlong JNICALL Java_org_by1337_btcp_common_io_zip_natives_LibdeflateZLibCompressor_allocCompressor
(JNIEnv *env, jobject obj, const jint level) {
    struct libdeflate_compressor *compressor = libdeflate_alloc_compressor(level);
    if (compressor == NULL) {
        const jclass exceptionClass = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
        (*env)->ThrowNew(env, exceptionClass, "libdeflate allocate compressor");
        return 0;
    }
    return (jlong) compressor;
}

JNIEXPORT void JNICALL Java_org_by1337_btcp_common_io_zip_natives_LibdeflateZLibCompressor_freeCompressor
(JNIEnv *env, jobject obj, jlong compressorPtr) {
    libdeflate_free_compressor((struct libdeflate_compressor *) compressorPtr);
}

JNIEXPORT jlong JNICALL Java_org_by1337_btcp_common_io_zip_natives_LibdeflateZLibCompressor_compress0
(JNIEnv *env, jobject obj, jlong compressorPtr, jlong sourceAddress, jint sourceLength, jlong destAddress,
 jint destLength) {
    struct libdeflate_compressor *compressor = (struct libdeflate_compressor *) compressorPtr;
    const size_t produced = libdeflate_zlib_compress(compressor, (void *) sourceAddress, sourceLength,
                                                     (void *) destAddress, destLength);
    return (jlong) produced;
}
