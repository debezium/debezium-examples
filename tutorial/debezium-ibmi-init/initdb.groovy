@Grab('net.sf.jt400:jt400:20.0.7')

import java.sql.DriverManager

def driver = new com.ibm.as400.access.AS400JDBCDriver()

if (args.length < 4 || args.length > 5) {
    println 'Usage: initdb.groovy <host> <data-library> <username> <password> [<sql-statement>]'
    return -1
}

def host = args[0]
def dataLibrary = args[1]
def username = args[2]
def password = args[3]

def sql = args.length > 4 ? [args[4]] : ('inventory.sql' as File).text.replace('##LIBRARY##', dataLibrary).trim().split(';')

try (
    def connection = driver.connect("jdbc:as400://$host/", new Properties(['user': username, 'password': password]))
    def statement = connection.createStatement()
    ) {

    sql.each {
        statement.executeUpdate(it)
    }
}

