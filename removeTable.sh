#bash 2014/0410 made by Jolly.
#default table name is map.1024.split.reverse.
if [ "$#" == "0" ]; then
TABLE=map.1024.split.revers212e
elif [ "$#" == "1" ]; then
TABLE=$1
else
        echo "wrong arguments. ex: ./removeTable.sh TABLENAME"
        exit 1
fi

echo "disable '$TABLE'" | hbase shell
echo "drop '$TABLE'" | hbase shell
