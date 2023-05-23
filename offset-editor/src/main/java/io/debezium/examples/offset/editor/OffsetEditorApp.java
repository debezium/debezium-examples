package io.debezium.examples.offset.editor;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class OffsetEditorApp extends Application {

    private File selectedFile;
    private TableView<Pair> tableView;
    private ObservableList<Pair> data;
    private Map<ByteBuffer, ByteBuffer> originalData;
    private final OffsetFileController offsetFileController;

    public OffsetEditorApp() {
        offsetFileController = new OffsetFileController();
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        initComponents(primaryStage);
    }

    private void initComponents(Stage primaryStage) {
        FileChooser fileChooser = createFileChooser();
        Button selectFileBtn = createSelectFileButton(primaryStage, fileChooser);
        tableView = createTableView();
        Button saveBtn = createSaveButton(fileChooser);
        BorderPane root = createRootLayout(selectFileBtn, saveBtn);
        Scene scene = new Scene(root, 600, 400);

        primaryStage.setTitle("Offset Editor");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private FileChooser createFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select offset file");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Offset File", "*"));
        return fileChooser;
    }

    private Button createSelectFileButton(Stage primaryStage, FileChooser fileChooser) {
        Button selectFileBtn = new Button("Select File");
        selectFileBtn.setOnAction(event -> {
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                selectedFile = file;
                loadData();
            }
        });
        return selectFileBtn;
    }

    private TableView<Pair> createTableView() {
        TableView<Pair> tableView = new TableView<>();
        tableView.setEditable(true);
        TableColumn<Pair, String> keyCol = new TableColumn<>("Key");
        keyCol.setCellValueFactory(new PropertyValueFactory<>("key"));
        TableColumn<Pair, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));
        valueCol.setCellFactory(tc -> createEditableTableCell());
        valueCol.setOnEditCommit(event -> {
            Pair pair = event.getTableView().getItems().get(event.getTablePosition().getRow());
            pair.setValue(event.getNewValue());
            ByteBuffer key = ByteBuffer.wrap(pair.getKey().getBytes(StandardCharsets.US_ASCII));
            ByteBuffer newValue = ByteBuffer.wrap(event.getNewValue().getBytes(StandardCharsets.US_ASCII));
            originalData.put(key, newValue);
        });

        tableView.getColumns().addAll(keyCol, valueCol);
        return tableView;
    }

    private TableCell<Pair, String> createEditableTableCell() {
        TableCell<Pair, String> cell = new TableCell<>() {
            private TextField textField;

            {
                textField = new TextField();
                textField.setOnKeyReleased(event -> {
                    if (event.getCode() == KeyCode.ENTER) {
                        commitEdit(textField.getText());
                    }
                });

                textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
                    if (!newValue) { // focus lost
                        commitEdit(textField.getText());
                    }
                });
            }

            @Override
            public void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else {
                    textField.setText(value);
                    setGraphic(textField);
                    setText(null);
                }
            }

            @Override
            public void commitEdit(String newValue) {
                super.commitEdit(newValue);
                Pair pair = getTableRow().getItem();
                pair.setValue(newValue);
            }
        };
        cell.setAlignment(Pos.CENTER);
        return cell;
    }

    private Button createSaveButton(FileChooser fileChooser) {
        Button saveBtn = new Button("Save");
        saveBtn.setOnAction(event -> {
            if (selectedFile != null) {
                saveData(fileChooser);
            }
        });
        return saveBtn;
    }

    private BorderPane createRootLayout(Button selectFileBtn, Button saveBtn) {
        HBox topBox = new HBox();
        topBox.setPadding(new Insets(10));
        topBox.setAlignment(Pos.CENTER_LEFT);
        topBox.getChildren().add(selectFileBtn);

        HBox bottomBox = new HBox();
        bottomBox.setPadding(new Insets(10));
        bottomBox.setAlignment(Pos.CENTER_RIGHT);
        bottomBox.getChildren().add(saveBtn);

        BorderPane root = new BorderPane();
        root.setTop(topBox);
        root.setCenter(tableView);
        root.setBottom(bottomBox);

        return root;
    }

    private void loadData() {
        data = FXCollections.observableArrayList();
        originalData = new HashMap<>();
        tableView.setItems(data);

        originalData = offsetFileController.readOffsetFile(selectedFile);

        originalData.forEach((key, value) -> {
            String keyString = new String(key.array(), StandardCharsets.UTF_8);
            String valueString = new String(value.array(), StandardCharsets.UTF_8);
            data.add(new Pair(keyString, valueString));
        });
    }

    private void saveData(FileChooser fileChooser) {
        fileChooser.setInitialDirectory(null);
        File outputFile = fileChooser.showSaveDialog(tableView.getScene().getWindow());

        if (outputFile != null) {
            Map<byte[], byte[]> rawData = new HashMap<>();
            for (Pair pair : data) {
                byte[] keyArray = pair.getKey().getBytes(StandardCharsets.US_ASCII);
                byte[] valueArray = pair.getValue().getBytes(StandardCharsets.US_ASCII);
                rawData.put(keyArray, valueArray);
            }

            offsetFileController.writeOffsetFile(outputFile, rawData);
        }
    }

    public static class Pair {
        private String key;
        private String value;

        public Pair(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
