/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_munger_passwordkeeper_struct_AES256 */

#ifndef _Included_com_munger_passwordkeeper_struct_AES256
#define _Included_com_munger_passwordkeeper_struct_AES256
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_munger_passwordkeeper_struct_AES256
 * Method:    init
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_munger_passwordkeeper_struct_AES256_init
  (JNIEnv *, jobject, jstring);

/*
 * Class:     com_munger_passwordkeeper_struct_AES256
 * Method:    destroy
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_munger_passwordkeeper_struct_AES256_destroy
  (JNIEnv *, jobject, jint);

/*
 * Class:     com_munger_passwordkeeper_struct_AES256
 * Method:    encode
 * Signature: (ILjava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_munger_passwordkeeper_struct_AES256_encode
  (JNIEnv *, jobject, jint, jstring);

/*
 * Class:     com_munger_passwordkeeper_struct_AES256
 * Method:    decode
 * Signature: (ILjava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_munger_passwordkeeper_struct_AES256_decode
  (JNIEnv *, jobject, jint, jstring);

#ifdef __cplusplus
}
#endif
#endif
