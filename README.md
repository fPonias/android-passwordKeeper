android-passwordKeeper
======================

simple password storage using AES for security.

The included project files work with Eclipse.  Dependencies include the android-support-v7-appcompat project
 that can be downloaded from the extras section of the SDK manager.  
 
 This project currently has been compiled with SDK version 19.
 
 There is a jni portion to this project as well for the custom AES and MD5 functionality.  I've had terrible luck
 using the built in Java encryption libraries cross platform where the MD5 hashes wouldn't match and AES ciphertext
 would be indecipherable accross different versions of Java.  I found the most consistent solution was to use simple
 implementations of both that I found on the Google.  The first being the RSA Data Security, Inc. MD5 Message Digest Algorithm
 and the second being the Byte-oriented AES-256 implementation by Ilya O. Levin and Hal Finney
 
 If you don't want to bother with setting up the project, there's a precompiled apk in the root directory called PasswordKeeper.apk 