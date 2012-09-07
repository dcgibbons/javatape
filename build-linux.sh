#!/bin/sh

rm -fr classes ; mkdir classes
rm -fr include ; mkdir include
rm -fr lib ; mkdir lib

javac -classpath ./classes -d ./classes -sourcepath ./src ./src/BasicTapeDevice.java ./src/TestBasicTapeDevice.java ./src/SpeedTest.java
javah -classpath ./classes -d ./include BasicTapeDevice
gcc -o ./lib/libTapeLinux.so -shared -Wl,-soname,libTapeLinux.so -I/usr/java/jdk1.2.2/include -I/usr/java/jdk1.2.2/include/linux -I./include src/TapeLinux.c
