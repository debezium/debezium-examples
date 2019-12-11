#/bin/bash

DBNAME=$1
DB2DIR=/opt/ibm/db2/V11.5
rc=1
echo "waiting for DB2 start ( $DBNAME ) ."
while [ "$rc" -ne 0 ]
do
   sleep 5
   db2 connect to $DBNAME
   rc=$?
   echo '.'
done

# enable metacatalog read via JDBC
cd $HOME/sqllib/bnd
db2 bind db2schema.bnd blocking all grant public sqlerror continue 

# do a backup and restart the db
db2 backup db $DBNAME to /dev/null
db2 restart db $DBNAME

db2 connect to $DBNAME

cp /asncdctools/src/asncdc /database/config/db2inst1/sqllib/function
chmod 777 /database/config/db2inst1/sqllib/function

# add UDF / start stop asncap
db2 -tvmf /asncdctools/src/asncdc_UDF.sql

# create asntables
db2 -tvmf /asncdctools/src/asncdctables.sql

# add UDF / add remove asntables

db2 -tvmf /asncdctools/src/asncdcaddremove.sql




# create sample table and datat
db2 -tvmf /asncdctools/src/inventory.sql 
db2 -tvmf /asncdctools/src/startup-agent.sql 
sleep 10
db2 -tvmf /asncdctools/src/startup-cdc-demo.sql 




echo "done"