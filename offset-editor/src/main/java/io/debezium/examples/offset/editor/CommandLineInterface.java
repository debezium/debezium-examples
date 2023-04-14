package io.debezium.examples.offset.editor;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CommandLineInterface {
    private final String[] args;
    private final OffsetFileController offsetFileController;

    public CommandLineInterface(String[] args) {
        this.args = args;
        offsetFileController = new OffsetFileController();
    }

    public void run() {
        if (args.length != 3) {
            System.err.println("Invalid arguments. Usage: <inputFile> <outputFile> <newValue>");
            return;
        }

        File inputFile = new File(args[0]);
        File outputFile = new File(args[1]);
        String newValue = args[2];

        Map<ByteBuffer, ByteBuffer> originalData = offsetFileController.readOffsetFile(inputFile);
        Map<byte[], byte[]> rawData = new HashMap<>();

        ByteBuffer valueBuffer = ByteBuffer.wrap(newValue.getBytes(StandardCharsets.US_ASCII));

        for (Map.Entry<ByteBuffer, ByteBuffer> entry : originalData.entrySet()) {
            rawData.put(entry.getKey().array(), valueBuffer.array());
        }

        offsetFileController.writeOffsetFile(outputFile, rawData);
    }
}
