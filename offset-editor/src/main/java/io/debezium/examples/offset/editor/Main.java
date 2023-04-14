package io.debezium.examples.offset.editor;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0) {
            CommandLineInterface cli = new CommandLineInterface(args);
            cli.run();
        } else {
            OffsetEditorApp.main(args);
        }
    }
}
