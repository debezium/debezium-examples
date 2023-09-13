#!/usr/bin/env python3

import numpy as np
from sklearn import datasets
from sklearn import model_selection

TRAIN_FILE1 = "./postgres/iris_train1.sql"
TRAIN_FILE2 = "./postgres/iris_train2.sql"
TEST_FILE = "./postgres/iris_test.sql"

TRAIN_TABLE = "iris_train"
TEST_TABLE = "iris_test"
COLUMNS = [
    "sepal_length",
    "sepal_width",
    "petal_length",
    "petal_width",
    "iris_class",
]
CREATE_STMT = "CREATE TABLE IF NOT EXISTS {}" \
    "(id SERIAL NOT NULL PRIMARY KEY, " \
    "{} float, {} float, {} float, {} float, {} int);\n"
INSERT_STMT = "INSERT INTO {}({}, {}, {}, {}, {}) " \
    "VALUES({}, {}, {}, {}, {});\n"

def prepare_sql(data, file_name, table_name):
    with open(file_name, 'w') as f:
        f.write(CREATE_STMT.format(table_name, *COLUMNS))
        for row in data:
            f.write(INSERT_STMT.format(table_name, *COLUMNS, *row))

iris = datasets.load_iris()
iris_merged = np.column_stack((iris.data, iris.target))

# convert labels from float to int
iris_merged = iris_merged.astype('object')
iris_merged[:, 4] = iris_merged[:, 4].astype(int)

train, test = model_selection.train_test_split(
    iris_merged, test_size=0.07)

train1, train2 = model_selection.train_test_split(train, test_size=0.99)

prepare_sql(train1, TRAIN_FILE1, TRAIN_TABLE)
prepare_sql(train2, TRAIN_FILE2, TRAIN_TABLE)
prepare_sql(test, TEST_FILE, TEST_TABLE)
