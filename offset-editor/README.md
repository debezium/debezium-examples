# Offset Editor

## Introduction

Offset Editor is a Java application that allows users to read, edit, and save files containing offset data. It offers a command-line interface (CLI) and a graphical user interface (GUI) for convenient editing of offset data files.  This is helpful if you're using Debezium Server and wish to set an offset to a particular value (for example, to process or re-process earlier values).

## Prerequisites

- Java Development Kit (JDK) 11 or higher
- Java JFX 16 or higher

## Building the Project

1. Clone the repository or download the source code to your local machine.
2. Open a terminal/command prompt and navigate to the project directory.
3. To compile the project using Maven, execute the following command:

```bash
mvn clean install
```

This command will generate a shaded JAR file (a JAR file containing all the necessary dependencies) in the `target` directory.

## Usage

### Graphical User Interface

To run the application using the graphical user interface, double-click the offset-editor-1.0-SNAPSHOT-shaded.jar.

This will open the Offset Editor window. Follow the steps below to edit and save your offset data:

1. Click the "Select File" button and choose the input file containing the offset data.
2. The table will be populated with the key-value pairs from the input file. Click on any cell in the "Value" column to edit its value.
3. After editing the values, click the "Save" button to save your changes. You will be prompted to choose a location and file name for the output file.

### Command-Line Interface

You can run the application using the command-line interface by executing the following command:

```bash
java -jar target/OffsetEditor-1.0-SNAPSHOT-jar-with-dependencies.jar <inputFile> <outputFile> <newKey> <newValue>
```
The required parameters are:

- `inputFile`: Path to the input file 
- `outputFile`: Path to the output file
- `newKey`: Key to be edited
- `newValue`: New value for the key (optional)