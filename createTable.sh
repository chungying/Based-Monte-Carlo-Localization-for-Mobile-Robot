#!/bin/bash
#created by Jolly on 15th Jan, 2017.
#no default TABLENAME

if [ "$#" == "0" ]; then
echo 'no table name.'
echo "wrong arguments. ex: ./createTable.sh TABLENAME"
exit 1
#TABLE=simmap.360.split
elif [ "$#" == "1" ]; then
echo 'A table is going to be created.'
TABLE=$1
else
        echo "wrong arguments. ex: ./createTable.sh TABLENAME"
        exit 1
fi

DISTANCE="{NAME =>'distance',BLOCKSIZE=>'8192',IN_MEMORY=>'true', COMPRESSION=>'SNAPPY'}"
ENERGY="{NAME =>'energy',BLOCKSIZE=>'8192',IN_MEMORY=>'true', COMPRESSION=>'SNAPPY'}"

echo "
create '$TABLE', $DISTANCE, $ENERGY, $SPLITKEYS" | hbase shell
