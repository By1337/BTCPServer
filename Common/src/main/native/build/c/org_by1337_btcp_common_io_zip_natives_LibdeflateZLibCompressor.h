/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_by1337_btcp_common_io_zip_natives_LibdeflateZLibCompressor */

#ifndef _Included_org_by1337_btcp_common_io_zip_natives_LibdeflateZLibCompressor
#define _Included_org_by1337_btcp_common_io_zip_natives_LibdeflateZLibCompressor
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_by1337_btcp_common_io_zip_natives_LibdeflateZLibCompressor
 * Method:    allocCompressor
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_org_by1337_btcp_common_io_zip_natives_LibdeflateZLibCompressor_allocCompressor
  (JNIEnv *, jobject, jint);

/*
 * Class:     org_by1337_btcp_common_io_zip_natives_LibdeflateZLibCompressor
 * Method:    freeCompressor
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_by1337_btcp_common_io_zip_natives_LibdeflateZLibCompressor_freeCompressor
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_by1337_btcp_common_io_zip_natives_LibdeflateZLibCompressor
 * Method:    compress0
 * Signature: (JJIJI)J
 */
JNIEXPORT jlong JNICALL Java_org_by1337_btcp_common_io_zip_natives_LibdeflateZLibCompressor_compress0
  (JNIEnv *, jobject, jlong, jlong, jint, jlong, jint);

#ifdef __cplusplus
}
#endif
#endif
