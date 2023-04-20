#!/usr/bin/env python3

import gzip
import os
import struct
import sys
import urllib.request

MNIST_TRAIN_LABEL_FILE = "train-labels-idx1-ubyte.gz"
MNIST_TRAIN_IMAGE_FILE = "train-images-idx3-ubyte.gz"
MNIST_TEST_LABEL_FILE = "t10k-labels-idx1-ubyte.gz"
MNIST_TEST_IMAGE_FILE = "t10k-images-idx3-ubyte.gz"
MNIST_URL_PREFIX = "http://yann.lecun.com/exdb/mnist/"

MNIST_LABEL_MAGIC = 2049
MNIST_IMAGE_MAGIC = 2051

TRAIN_TABLE = "mnist_train"
TEST_TABLE = "mnist_test"
CREATE_STMT = ("CREATE TABLE {}(id SERIAL NOT NULL PRIMARY KEY, "
               "label SMALLINT, pixels BYTEA);\n")
INSERT_STMT = "INSERT INTO {}(label, pixels) VALUES({}, '\\x{}');\n"

FILE_DESTINATION = "postgres"
TRAIN_SQL_FILE = TRAIN_TABLE + ".sql"
TEST_SQL_FILE = TEST_TABLE + ".sql"


def download_mnist_files(dest):
    """
    Downloads MNIST data samples into specified destination.
    """
    print("Downloading MNIST file with training labels")
    urllib.request.urlretrieve(
        MNIST_URL_PREFIX + MNIST_TRAIN_LABEL_FILE,
        os.path.join(dest, MNIST_TRAIN_LABEL_FILE))

    print("Downloading MNIST file with training images")
    urllib.request.urlretrieve(
        MNIST_URL_PREFIX + MNIST_TRAIN_IMAGE_FILE,
        os.path.join(dest, MNIST_TRAIN_IMAGE_FILE))

    print("Downloading MNIST file with test labels")
    urllib.request.urlretrieve(
        MNIST_URL_PREFIX + MNIST_TEST_LABEL_FILE,
        os.path.join(dest, MNIST_TEST_LABEL_FILE))

    print("Downloading MNIST file with test images")
    urllib.request.urlretrieve(
        MNIST_URL_PREFIX + MNIST_TEST_IMAGE_FILE,
        os.path.join(dest, MNIST_TEST_IMAGE_FILE))


def prepare_sql(label_path, img_path, sql_path, table_name):
    """
    Combines label file with images from the other file and create new file
    with SQL commands for creating and populating specified DB table with
    these values. Images bytes are encoded as HEX strings.
    """
    with open(sql_path, mode='w', encoding="utf-8") as sql_file:
        sql_file.write(CREATE_STMT.format(table_name))

        with gzip.open(label_path, "rb") as label_file:
            with gzip.open(img_path, "rb") as image_file:
                magic_label, num_label = struct.unpack(
                    ">II", label_file.read(8))
                magic_img, num_img, rows, columns = struct.unpack(
                    ">IIII", image_file.read(16))

                if MNIST_LABEL_MAGIC != magic_label:
                    raise ValueError("Unexpected label magic: {magic_label}")
                if MNIST_IMAGE_MAGIC != magic_img:
                    raise ValueError("Unexpected image magic: {magic_img}")
                if num_label != num_img:
                    raise ValueError(
                        "Size of labels {num_label} doesn't match with size of"
                        " image sample {num_img}")

                img_size = rows * columns
                for _ in range(0, num_label):
                    label = struct.unpack(">b", label_file.read(1))
                    pixels = image_file.read(img_size)
                    sql_file.write(
                        INSERT_STMT.format(table_name, label[0], pixels.hex()))


if len(sys.argv) > 1 and sys.argv[1] == "--download":
    print("Downloading MNIST sample")
    download_mnist_files(FILE_DESTINATION)


print("Creating SQL file with training data")
prepare_sql(
    os.path.join(FILE_DESTINATION, MNIST_TRAIN_LABEL_FILE),
    os.path.join(FILE_DESTINATION, MNIST_TRAIN_IMAGE_FILE),
    os.path.join(FILE_DESTINATION, TRAIN_SQL_FILE),
    TRAIN_TABLE
)

print("Creating SQL file with test data")
prepare_sql(
    os.path.join(FILE_DESTINATION, MNIST_TEST_LABEL_FILE),
    os.path.join(FILE_DESTINATION, MNIST_TEST_IMAGE_FILE),
    os.path.join(FILE_DESTINATION, TEST_SQL_FILE),
    TEST_TABLE
)
