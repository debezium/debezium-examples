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
        if (args.length < 3 || args.length > 4) {
            System.err.println("Invalid arguments. Usage: <inputFile> <outputFile> <newValue> [optional: <newValue>]");
            System.exit(1);
        }

        File inputFile = new File(args[0]);
        File outputFile = new File(args[1]);
        String newKey = args[2];
        String newValue = (args.length == 4) ? args[3] : null;

        Map<ByteBuffer, ByteBuffer> originalData = offsetFileController.readOffsetFile(inputFile);
        Map<byte[], byte[]> rawData = new HashMap<>();

        ByteBuffer keyBuffer = ByteBuffer.wrap(newKey.getBytes(StandardCharsets.US_ASCII));
        ByteBuffer valueBuffer = (newValue != null) ? ByteBuffer.wrap(newValue.getBytes(StandardCharsets.US_ASCII)) : null;

        for (Map.Entry<ByteBuffer, ByteBuffer> entry : originalData.entrySet()) {
            if (newValue != null) {
                if (entry.getKey().equals(keyBuffer)) {
                    // update value
                    rawData.put(entry.getKey().array(), valueBuffer.array());
                } else {
                    // insert new entry
                    rawData.put(keyBuffer.array(), valueBuffer.array());
                }
            } else {
                originalData.remove(keyBuffer);
            }
        }

        offsetFileController.writeOffsetFile(outputFile, rawData);
    }
}
