#include <stdio.h>
#include <jni.h>
#include "org_by1337_btcp_common_io_zip_natives_LibdeflateZLibDecompressor.h"
#include "libdeflate/libdeflate.h"

void throwException(JNIEnv *env, char *type, const char *msg) {
    const jclass clazz = (*env)->FindClass(env, type);
    if (clazz != 0) {
        (*env)->ThrowNew(env, clazz, msg);
    }
}

JNIEXPORT jlong JNICALL Java_org_by1337_btcp_common_io_zip_natives_LibdeflateZLibDecompressor_allocDecompressor
(JNIEnv *env, jobject obj) {
    struct libdeflate_decompressor *decompress = libdeflate_alloc_decompressor();
    if (decompress == NULL) {
        throwException(env, "java/lang/OutOfMemoryError", "libdeflate allocate decompressor");
        return 0;
    }

    return (jlong) decompress;
}

JNIEXPORT void JNICALL Java_org_by1337_btcp_common_io_zip_natives_LibdeflateZLibDecompressor_freeDecompressor
(JNIEnv *env, jobject obj, jlong compressorPtr) {
    libdeflate_free_decompressor((struct libdeflate_decompressor *) compressorPtr);
}

JNIEXPORT jint JNICALL Java_org_by1337_btcp_common_io_zip_natives_LibdeflateZLibDecompressor_decompress0
(JNIEnv *env, jobject obj, jlong decompressorPtr, jlong sourceAddress, jint sourceLength, jlong destAddress,
 jint originalSize) {
    struct libdeflate_decompressor *decompress = (struct libdeflate_decompressor *) decompressorPtr;

    enum libdeflate_result result = libdeflate_zlib_decompress(decompress, (void *) sourceAddress,
                                                               sourceLength, (void *) destAddress,
                                                               originalSize, NULL);
    return (jint) result;
}
