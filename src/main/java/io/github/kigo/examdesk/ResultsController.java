package io.github.kigo.examdesk;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class ResultsController {

    public static class QuestionRow {
        private final String id;
        private final String studentAnswer;
        private final String correctAnswer;
        private final String isCorrect;

        public QuestionRow(String id, String studentAnswer, String correctAnswer, String isCorrect) {
            this.id = id;
            this.studentAnswer = studentAnswer;
            this.correctAnswer = correctAnswer;
            this.isCorrect = isCorrect;
        }

        public String getId() { return id; }
        public String getStudentAnswer() { return studentAnswer; }
        public String getCorrectAnswer() { return correctAnswer; }
        public String getIsCorrect() { return isCorrect; }
    }

    @FXML
    private TableView<QuestionRow> resultsTable;
    @FXML
    private TableColumn<QuestionRow, String> colId;
    @FXML
    private TableColumn<QuestionRow, String> colStudent;
    @FXML
    private TableColumn<QuestionRow, String> colCorrect;
    @FXML
    private TableColumn<QuestionRow, String> colIsCorrect;

    @FXML
    private Label accuracyLabel;
    @FXML
    private Label examNameLabel;
    @FXML
    private TreeView<File> filesTree;

    private final ObservableList<QuestionRow> data = FXCollections.observableArrayList();
    private File currentDirectory;

    @FXML
    private void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colStudent.setCellValueFactory(new PropertyValueFactory<>("studentAnswer"));
        colCorrect.setCellValueFactory(new PropertyValueFactory<>("correctAnswer"));
        colIsCorrect.setCellValueFactory(new PropertyValueFactory<>("isCorrect"));
        resultsTable.setItems(data);

        filesTree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });

        filesTree.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            TreeItem<File> sel = filesTree.getSelectionModel().getSelectedItem();
            if (sel != null && sel.getValue().isFile()) {
                loadFileToRight(sel.getValue());
            }
        });
    }

    public void loadFromDirectory(File dir) {
        if (dir == null || !dir.isDirectory()) return;
        this.currentDirectory = dir;
        buildTreeForDirectory(dir);
        data.clear();
        examNameLabel.setText("");
        accuracyLabel.setText("");
    }

    private void buildTreeForDirectory(File dir) {
        TreeItem<File> rootItem = createTreeItem(dir);
        filesTree.setRoot(rootItem);
        rootItem.setExpanded(true);
    }

    private TreeItem<File> createTreeItem(File file) {
        TreeItem<File> item = new TreeItem<>(file);
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                List<File> dirs = Stream.of(children)
                        .filter(File::isDirectory)
                        .sorted(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER))
                        .collect(Collectors.toList());
                List<File> xmls = Stream.of(children)
                        .filter(f -> f.isFile() && f.getName().toLowerCase().endsWith(".xml"))
                        .sorted(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER))
                        .collect(Collectors.toList());

                for (File d : dirs) {
                    TreeItem<File> child = createTreeItem(d);
                    if (!child.getChildren().isEmpty()) {
                        item.getChildren().add(child);
                    }
                }
                for (File x : xmls) {
                    item.getChildren().add(new TreeItem<>(x));
                }
            }
        }
        return item;
    }

    private void loadFileToRight(File xmlFile) {
        if (xmlFile == null || !xmlFile.isFile()) return;
        Platform.runLater(() -> {
            try {
                parseAndDisplayXml(xmlFile);
            } catch (Exception e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("読み込みエラー");
                alert.setHeaderText("XMLの読み込みに失敗しました");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        });
    }

    private void parseAndDisplayXml(File file) throws Exception {
        data.clear();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(file);
        doc.getDocumentElement().normalize();

        String examName = "";
        NodeList examNodes = doc.getElementsByTagName("Exam");
        if (examNodes.getLength() > 0) {
            Element examElem = (Element) examNodes.item(0);
            NodeList nameNodes = examElem.getElementsByTagName("Name");
            if (nameNodes.getLength() > 0) {
                examName = nameNodes.item(0).getTextContent();
            }
        }
        examNameLabel.setText("試験名: " + examName + "  (" + file.getName() + ")");

        NodeList qList = doc.getElementsByTagName("Question");
        int total = qList.getLength();
        int correctCount = 0;

        for (int i = 0; i < qList.getLength(); i++) {
            Element qElem = (Element) qList.item(i);
            String id = qElem.getAttribute("id");
            if (id == null || id.isEmpty()) id = String.valueOf(i + 1);
            String student = getTagText(qElem, "StudentAnswer");
            String correct = getTagText(qElem, "CorrectAnswer");
            String isCorrText = getTagText(qElem, "IsCorrect");
            boolean isCorr = Boolean.parseBoolean(isCorrText);
            if (isCorr) correctCount++;
            String mark = isCorr ? "◎" : "×";
            data.add(new QuestionRow(id, student, correct, mark));
        }

        double rate = total == 0 ? 0.0 : (100.0 * correctCount / total);
        accuracyLabel.setText(String.format("正解率: %d / %d (%.1f%%)", correctCount, total, rate));
    }

    private String getTagText(Element parent, String tagName) {
        NodeList nl = parent.getElementsByTagName(tagName);
        if (nl.getLength() == 0) return "";
        return nl.item(0).getTextContent();
    }

    @FXML
    private void onReloadFolder() {
        if (currentDirectory != null) {
            buildTreeForDirectory(currentDirectory);
        }
    }

    @FXML
    private void onBack() {
        try {
            Stage stage = (Stage) resultsTable.getScene().getWindow();
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/io/github/kigo/examdesk/start.fxml"));
            javafx.scene.Parent root = loader.load();
            Scene scene = new Scene(root, 420, 140);
            stage.setScene(scene);
            stage.setResizable(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
