cd src/main/jni
ndk-build
cd ..
#mv libs/armeabi/*.so ../../../library_qrscan/libs/armeabi
mv libs/armeabi-v7a/*.so ../../../library_qrscan/libs/armeabi-v7a
rm -rf libs obj