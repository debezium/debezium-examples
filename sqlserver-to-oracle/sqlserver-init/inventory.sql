-- Source database for the SQL Server -> Oracle replication example.
--
-- NVARCHAR columns are used on purpose: they exercise the Unicode path all the
-- way through to the Oracle NVARCHAR2 columns on the sink side.

CREATE DATABASE inventory;
GO

USE inventory;
GO

EXEC sys.sp_cdc_enable_db;
GO

CREATE TABLE customers (
    id INT IDENTITY(1001,1) NOT NULL PRIMARY KEY,
    first_name NVARCHAR(255) NOT NULL,
    last_name NVARCHAR(255) NOT NULL,
    email NVARCHAR(255) NOT NULL UNIQUE
);
GO

INSERT INTO customers(first_name, last_name, email) VALUES
    (N'Sally', N'Thomas', N'sally.thomas@acme.com'),
    (N'George', N'Bailey', N'gbailey@foobar.com'),
    (N'Edward', N'Walker', N'ed@walker.com'),
    (N'Anne', N'Kretchmar', N'annek@noanswer.org');
GO

EXEC sys.sp_cdc_enable_table
    @source_schema = N'dbo',
    @source_name   = N'customers',
    @role_name     = NULL,
    @supports_net_changes = 0;
GO
