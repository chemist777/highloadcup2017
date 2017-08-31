gcc -I/usr/java/default/include \
-I/usr/java/default/include/linux \
-shared \
-fPIC \
-Ofast \
-m64 \
-o src/main/resources/nativenet \
src/main/c/ru_chemist_highloadcup_jni_NativeNet.c \
