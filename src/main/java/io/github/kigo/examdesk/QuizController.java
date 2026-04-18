package io.github.kigo.examdesk;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

public class QuizController {

    private static final String PREF_NODE = "quiz.settings";
    private static final String KEY_DEFAULT_PATH = "defaultPath";
    private static final String KEY_AUTO_OPEN = "autoOpenDefault";

    @FXML
    private VBox questionsContainer;

    @FXML
    private Button finishAnswersButton;

    @FXML
    private Button gradeButton;

    @FXML
    private Label infoLabel;

    @FXML
    private Label scoreLabel;

    @FXML
    private Button saveButton;

    @FXML
    private Button backButton;

    @FXML
    private Button tempSaveButton;

    private final List<TextField> answerFields = new ArrayList<>();
    private final List<TextField> correctFields = new ArrayList<>();
    private final List<Label> resultMarks = new ArrayList<>();
    private final List<Boolean> isCorrectList = new ArrayList<>();

    public void initWithCount(int count) {
        infoLabel.setText("問題数: " + count + "問");
        buildQuestionRows(count);
        updateFinishButtonState();
        updateGradeButtonState();
        scoreLabel.setText("");
        saveButton.setVisible(false);
    }

    private void buildQuestionRows(int count) {
        questionsContainer.getChildren().clear();
        answerFields.clear();
        correctFields.clear();
        resultMarks.clear();
        isCorrectList.clear();

        for (int i = 1; i <= count; i++) {
            Label qLabel = new Label(i + "：");
            qLabel.setMinWidth(30);

            TextField answerField = new TextField();
            answerField.setPromptText("解答");
            answerField.setPrefWidth(220);

            TextField correctField = new TextField();
            correctField.setPromptText("正解");
            correctField.setPrefWidth(220);
            correctField.setDisable(true);

            Label markLabel = new Label("");
            markLabel.setMinWidth(30);

            answerField.textProperty().addListener((obs, oldV, newV) -> updateFinishButtonState());
            correctField.textProperty().addListener((obs, oldV, newV) -> updateGradeButtonState());

            HBox row = new HBox(10, qLabel, answerField, correctField, markLabel);
            row.setSpacing(10);
            questionsContainer.getChildren().add(row);

            answerFields.add(answerField);
            correctFields.add(correctField);
            resultMarks.add(markLabel);
            isCorrectList.add(false);
        }
    }

    private void updateFinishButtonState() {
        boolean allFilled = answerFields.stream()
                .allMatch(tf -> !tf.getText().trim().isEmpty());
        finishAnswersButton.setDisable(!allFilled);
    }

    private void updateGradeButtonState() {
        boolean allFilled = correctFields.stream()
                .allMatch(tf -> !tf.getText().trim().isEmpty());
        gradeButton.setDisable(!allFilled);
    }

    @FXML
    private void onFinishAnswers() {
        correctFields.forEach(tf -> tf.setDisable(false));
        answerFields.forEach(tf -> tf.setEditable(false));
        updateGradeButtonState();
        finishAnswersButton.setDisable(true);
    }

    @FXML
    private void onGrade() {
        int total = answerFields.size();
        int correctCount = 0;

        for (int i = 0; i < total; i++) {
            String ans = answerFields.get(i).getText().trim();
            String corr = correctFields.get(i).getText().trim();
            boolean isCorrect = ans.equals(corr);
            isCorrectList.set(i, isCorrect);
            if (isCorrect) {
                correctCount++;
                resultMarks.get(i).setText("◎");
            } else {
                resultMarks.get(i).setText("×");
            }
        }

        double rate = total == 0 ? 0.0 : (100.0 * correctCount / total);
        String rateText = String.format("正解率: %d / %d (%.1f%%)", correctCount, total, rate);
        scoreLabel.setText(rateText);

        saveButton.setVisible(true);
        gradeButton.setDisable(true);
    }

    @FXML
    private void onSaveResults() {
        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle("結果保存");
        nameDialog.setHeaderText("保存する試験名を入力してください");
        nameDialog.setContentText("Name:");
        nameDialog.getEditor().setText("Exam");
        nameDialog.initOwner(getWindow());

        nameDialog.showAndWait().ifPresent(name -> {
            try {
                Preferences prefs = Preferences.userRoot().node(PREF_NODE);
                String defaultPath = prefs.get(KEY_DEFAULT_PATH, "");
                boolean autoOpen = prefs.getBoolean(KEY_AUTO_OPEN, false);

                File initialDir = null;
                if (autoOpen && defaultPath != null && !defaultPath.isEmpty()) {
                    File d = new File(defaultPath);
                    if (d.isDirectory()) initialDir = d;
                }

                File file;
                FileChooser fc = new FileChooser();
                fc.setTitle("XMLファイルを保存");
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
                fc.setInitialFileName(name + ".xml");
                if (initialDir != null) {
                    fc.setInitialDirectory(initialDir);
                }
                file = fc.showSaveDialog(getWindow());

                if (file != null) {
                    writeResultsToXml(file, name);
                    Alert ok = new Alert(Alert.AlertType.INFORMATION);
                    ok.setTitle("保存完了");
                    ok.setHeaderText(null);
                    ok.setContentText("結果を保存しました: " + file.getAbsolutePath());
                    ok.initOwner(getWindow());
                    ok.showAndWait();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("保存エラー");
                err.setHeaderText("保存に失敗しました");
                err.setContentText(e.getMessage());
                err.initOwner(getWindow());
                err.showAndWait();
            }
        });
    }

    @FXML
    private void onTempSave() {
        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle("一時保存");
        nameDialog.setHeaderText("一時保存する試験名を入力してください");
        nameDialog.setContentText("Name:");
        nameDialog.getEditor().setText("TempExam");
        nameDialog.initOwner(getWindow());

        nameDialog.showAndWait().ifPresent(name -> {
            try {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("一時保存先を選択");
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
                fileChooser.setInitialFileName(name + ".xml");
                // 一時保存ではデフォルトパスは使用しない（仕様どおり）
                File file = fileChooser.showSaveDialog(getWindow());
                if (file != null) {
                    writeResultsToXml(file, name);
                    Alert ok = new Alert(Alert.AlertType.INFORMATION);
                    ok.setTitle("保存完了");
                    ok.setHeaderText(null);
                    ok.setContentText("一時保存しました: " + file.getAbsolutePath());
                    ok.initOwner(getWindow());
                    ok.showAndWait();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("保存エラー");
                err.setHeaderText("一時保存に失敗しました");
                err.setContentText(e.getMessage());
                err.initOwner(getWindow());
                err.showAndWait();
            }
        });
    }

    @FXML
    private void onBack() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("試験中止の確認");
        confirm.setHeaderText("本当に試験を中止していいですか？");
        ButtonType yes = new ButtonType("はい", ButtonBar.ButtonData.YES);
        ButtonType no = new ButtonType("いいえ", ButtonBar.ButtonData.NO);
        confirm.getButtonTypes().setAll(yes, no);
        confirm.initOwner(getWindow());

        confirm.showAndWait().ifPresent(choice -> {
            if (choice == yes) {
                try {
                    Stage stage = (Stage) getWindow();
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/kigo/examdesk/start.fxml"));
                    Scene scene = new Scene(loader.load(), 420, 140);
                    stage.setScene(scene);
                    stage.setResizable(false);
                } catch (Exception e) {
                    e.printStackTrace();
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("画面遷移エラー");
                    err.setHeaderText("開始画面に戻れませんでした");
                    err.setContentText(e.getMessage());
                    err.initOwner(getWindow());
                    err.showAndWait();
                }
            } else {
                ;
            }
        });
    }

    private Window getWindow() {
        return questionsContainer.getScene().getWindow();
    }


    private void writeResultsToXml(File file, String examName) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

        Document doc = dBuilder.newDocument();
        Element root = doc.createElement("ExamResults");
        doc.appendChild(root);

        Element exam = doc.createElement("Exam");
        Element nameElem = doc.createElement("Name");
        nameElem.setTextContent(examName);
        exam.appendChild(nameElem);
        root.appendChild(exam);

        Element results = doc.createElement("Results");
        root.appendChild(results);

        for (int i = 0; i < answerFields.size(); i++) {
            Element qElem = doc.createElement("Question");
            qElem.setAttribute("id", String.valueOf(i + 1));

            Element student = doc.createElement("StudentAnswer");

            student.setTextContent(answerFields.get(i).getText());
            qElem.appendChild(student);

            Element correct = doc.createElement("CorrectAnswer");
            correct.setTextContent(correctFields.get(i).getText());
            qElem.appendChild(correct);

            Element isCorr = doc.createElement("IsCorrect");
            boolean val = (i < isCorrectList.size()) ? isCorrectList.get(i) : false;
            isCorr.setTextContent(String.valueOf(val));
            qElem.appendChild(isCorr);

            results.appendChild(qElem);
        }

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        try (FileOutputStream fos = new FileOutputStream(file)) {
            transformer.transform(new DOMSource(doc), new StreamResult(fos));
        }
    }

    public void loadFromFile(File file) throws Exception {
        if (file == null || !file.isFile()) return;

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(file);
        doc.getDocumentElement().normalize();

        final String examName;
        {
            String tmpName = "";
            NodeList examNodes = doc.getElementsByTagName("Exam");
            if (examNodes.getLength() > 0) {
                Element examElem = (Element) examNodes.item(0);
                NodeList nameNodes = examElem.getElementsByTagName("Name");
                if (nameNodes.getLength() > 0) {
                    tmpName = nameNodes.item(0).getTextContent();
                }
            }
            examName = tmpName;
        }

        NodeList qList = doc.getElementsByTagName("Question");
        final int total = qList.getLength();
        final List<String> students = new ArrayList<>(total);
        final List<String> corrects = new ArrayList<>(total);
        final List<Boolean> isCorrs = new ArrayList<>(total);

        for (int i = 0; i < qList.getLength(); i++) {
            Element qElem = (Element) qList.item(i);
            students.add(getTagText(qElem, "StudentAnswer"));
            corrects.add(getTagText(qElem, "CorrectAnswer"));
            isCorrs.add(Boolean.parseBoolean(getTagText(qElem, "IsCorrect")));
        }

        Platform.runLater(() -> {
            if (questionsContainer == null) {
                System.err.println("questionsContainer is null — UI not ready yet");
                return;
            }

            initWithCount(total);

            if (examName != null && !examName.isEmpty()) {
                infoLabel.setText("試験名: " + examName);
            }

            for (int i = 0; i < total; i++) {
                if (i < answerFields.size()) answerFields.get(i).setText(students.get(i));
                if (i < correctFields.size()) {
                    correctFields.get(i).setText(corrects.get(i));
                }
                if (i < resultMarks.size()) resultMarks.get(i).setText("");
                if (i < isCorrectList.size()) isCorrectList.set(i, false);
            }

            scoreLabel.setText("");
            saveButton.setVisible(false);

            updateFinishButtonState();
            updateGradeButtonState();
        });
    }

    private String getTagText(Element parent, String tagName) {
        NodeList nl = parent.getElementsByTagName(tagName);
        if (nl.getLength() == 0) return "";
        return nl.item(0).getTextContent();
    }
}