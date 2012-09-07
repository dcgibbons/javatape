/* TapeWinNT.c */

#include <windows.h>

#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <jni.h>

#include "BasicTapeDevice.h"

/*
#define TRUE 1
#define FALSE 0
*/


/* field IDs for commonly used object fields */
static jfieldID td_fdID;
static jfieldID td_eofID;
static jfieldID td_eomID;
static jfieldID IO_fd_fdID;


/* forward reference for utility functions */
static HANDLE getHandle(JNIEnv* env, jobject obj);
static void setHandle(JNIEnv* env, jobject obj, HANDLE fd);
static void throw(JNIEnv* env, int err);


/*
 * Class:     BasicTapeDevice
 * Method:    initFields
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_BasicTapeDevice_initFields
  (JNIEnv *env, jclass cls) 
{
    /* retrieve field IDs for the fd, eof, and eom member variables */
    td_fdID = (*env)->GetFieldID(env, cls, "fd", "Ljava/io/FileDescriptor;");
    td_eofID = (*env)->GetFieldID(env, cls, "eof", "Z");
    td_eomID = (*env)->GetFieldID(env, cls, "eom", "Z");

    /* retrieve the field ID for the private fd member of FileDescriptor */
    cls = (*env)->FindClass(env, "java/io/FileDescriptor");
    IO_fd_fdID = (*env)->GetFieldID(env, cls, "fd", "I");
}


/*
 * Class:     BasicTapeDevice
 * Method:    tapeOpen
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_BasicTapeDevice_tapeOpen
  (JNIEnv *env, jobject this, jstring path)
{
    HANDLE h;
    const wchar_t* p;

    p = (*env)->GetStringChars(env, path, 0);
    h = CreateFileW(p, 
            GENERIC_READ|GENERIC_WRITE, 0, NULL, OPEN_EXISTING, 0, NULL);
    (*env)->ReleaseStringChars(env, path, p);

    if (h == INVALID_HANDLE_VALUE) {
        throw(env, GetLastError());
    } else {
        /* check the tape status to clear any information errors, such as
           ERROR_MEDIA_CHANGED. */
        GetTapeStatus(h);

        setHandle(env, this, h);
    }
}


/*
 * Class:     BasicTapeDevice
 * Method:    tapeClose
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_BasicTapeDevice_tapeClose
  (JNIEnv *env, jobject this)
{
    HANDLE h = getHandle(env, this);
    CloseHandle(h);

    h = INVALID_HANDLE_VALUE;
    setHandle(env, this, h);
}


/*
 * Class:     BasicTapeDevice
 * Method:    tapeRead
 * Signature: ([BII)I
 */
JNIEXPORT jint JNICALL Java_BasicTapeDevice_tapeRead
  (JNIEnv *env, jobject this, jbyteArray buf, jint off, jint len)
{
    BOOL success;
    DWORD n;
    HANDLE h;
    jbyte* bufp;

    n = 0;
    h = getHandle(env, this);
    bufp = (*env)->GetByteArrayElements(env, buf, 0);
    success = ReadFile(h, &bufp[off], len, &n, NULL);
    (*env)->ReleaseByteArrayElements(env, buf, bufp, 0);

    if (!success) {
        DWORD err = GetLastError();
        switch (err) {
            case ERROR_FILEMARK_DETECTED:
            case ERROR_END_OF_MEDIA:
            case ERROR_NO_DATA_DETECTED:
                (*env)->SetBooleanField(env, this, td_eofID, TRUE);
                break;
            default:
                throw(env, err);
        }
    }

    return n;
}


/*
 * Class:     BasicTapeDevice
 * Method:    tapeWrite
 * Signature: ([BII)I
 */
JNIEXPORT jint JNICALL Java_BasicTapeDevice_tapeWrite
  (JNIEnv *env, jobject this, jbyteArray buf, jint off, jint len)
{
    BOOL success;
    DWORD n;
    HANDLE h;
    jbyte* bufp;

    n = 0;
    h = getHandle(env, this);
    bufp = (*env)->GetByteArrayElements(env, buf, 0);
    success = WriteFile(h, &bufp[off], (DWORD) len, &n, NULL);
    (*env)->ReleaseByteArrayElements(env, buf, bufp, 0);

    if (!success) {
        DWORD err = GetLastError();
        if (err == ERROR_END_OF_MEDIA) {
            (*env)->SetBooleanField(env, this, td_eofID, TRUE);
        } else {
            throw(env, err);
        }
    }

    return n;
}


/*
 * Class:     BasicTapeDevice
 * Method:    tapeGetBlockSize
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_BasicTapeDevice_tapeGetBlockSize
  (JNIEnv *env, jobject this)
{
    DWORD err, n;
    HANDLE h;
    TAPE_GET_MEDIA_PARAMETERS mparams;

    memset(&mparams, 0, sizeof(mparams));
    n = sizeof(mparams);
    h = getHandle(env, this);
    err = GetTapeParameters(h, GET_TAPE_MEDIA_INFORMATION, &n, &mparams);
    if (err != NO_ERROR) {
        throw(env, err);
    }

    return mparams.BlockSize;
}


/*
 * Retrieves the internal file descriptor from the BasicTapeDevice object
 */
static HANDLE getHandle(JNIEnv* env, jobject obj) {
    jobject fdobj;

    fdobj = (*env)->GetObjectField(env, obj, td_fdID);
    return (HANDLE) (*env)->GetIntField(env, fdobj, IO_fd_fdID);
}


/*
 * Sets the internal file descriptor of the BasicTapeDevice object
 */
static void setHandle(JNIEnv* env, jobject obj, HANDLE fd)
{
    jobject fdobj = (*env)->GetObjectField(env, obj, td_fdID);
    (*env)->SetIntField(env, fdobj, IO_fd_fdID, (jint) fd);
}


/*
 * Throws a new IOException
 */
static void throw(JNIEnv* env, int err)
{
    DWORD n;
    LPTSTR msgbuf;
    jclass cls;
    
    n = FormatMessage(
            FORMAT_MESSAGE_ALLOCATE_BUFFER |
            FORMAT_MESSAGE_FROM_SYSTEM |
            FORMAT_MESSAGE_IGNORE_INSERTS,
            NULL, err, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
            (LPTSTR) &msgbuf, 0, NULL);

    cls = (*env)->FindClass(env, "java/io/IOException");
    if (cls != NULL) {
        (*env)->ThrowNew(env, cls, msgbuf);
    }

    if (n != 0) {
        LocalFree(msgbuf);
    }
}
