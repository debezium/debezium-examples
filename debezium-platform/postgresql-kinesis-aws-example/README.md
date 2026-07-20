Debezium Platform on AWS: PostgreSQL to Amazon Kinesis
===

Database setup scripts for the pipeline described in [Running the Debezium Platform on AWS: PostgreSQL to Amazon Kinesis](https://debezium.io/blog/2026/07/13/debezium-platform-on-aws-postgres-to-kinesis/). The blog post covers the full walkthrough — provisioning the k3s cluster on EC2, the Kinesis streams and their IAM policy, and configuring the pipeline in the Platform UI. This folder holds only the SQL it refers to.

| File | Purpose |
| --- | --- |
| `setup.sql` | Provisions the `ecommerce` database on RDS: the `app_owner` and `debezium` roles, schema, seed data, and the `dbz_ecommerce` publication. |
| `generate_products.sql` | Appends up to 500 synthetic products per run to generate CDC traffic. Safe to re-run. |

Both are idempotent, and both run from your workstation through an SSH tunnel to the private RDS endpoint, as the RDS master user:

```sh
ssh -N -L 5432:<rds-endpoint>:5432 <your-ec2-host>        # terminal 1

psql -h localhost -p 5432 -U <master-user> -d postgres   -f ./setup.sql
psql -h localhost -p 5432 -U <master-user> -d ecommerce  -f ./generate_products.sql
```

> **_NOTE:_**
Logical replication must be enabled before the connector can capture anything, and it cannot be set over SQL. In the RDS parameter group set `rds.logical_replication = 1` and reboot the instance, then verify with `SHOW wal_level;` — it must report `logical`.

The passwords in `setup.sql` are throwaway demo defaults; change them for anything else.
