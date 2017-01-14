#!/bin/bash
NUMBER=$#
echo "Number of Arguments=$NUMBER"

if [ "$NUMBER" == "0" ]; then
echo "There is no arguments."
echo "environment JarFile"
elif [ "$NUMBER" == "1" ]; then
test ! -e $1 && echo "file $1 don't exist." && exit 1 || FILEPATH=$(pwd)/$1
test ! -e $FILEPATH && echo "The path is not correct." && exit 1 || echo "The path is correct"
echo "add $1 into HADOOP_CLASSPATH"
export HADOOP_CLASSPATH=$HADOOP_CLASSPATH:$FILEPATH
else
for i in $@
do
	test ! -e $i && echo "file $i don't exist." && exit 1 || FILEPATH=$(pwd)/$i
	test ! -e $FILEPATH && echo "The path is not correct." && exit 1 || echo "The path is correct"
	echo "add $i into HADOOP_CLASSPATH"
	export HADOOP_CLASSPATH=$HADOOP_CLASSPATH:$FILEPATH
	#echo $HADOOP_CLASSPATH
	#echo "add arguemnts $i into HADOOP_CLASSPATH"
done
#echo "There are many arguments."
#echo "environment JarFile"
fi
echo $HADOOP_CLASSPATH
