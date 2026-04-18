package io.github.kigo.examdesk;


import java.io.File;
import java.util.prefs.Preferences;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class SettingsController {

    private static final String PREF_NODE = "quiz.settings";
    private static final String KEY_DEFAULT_PATH = "defaultPath";
    private static final String KEY_AUTO_OPEN = "autoOpenDefault";

    @FXML
    private Button chooseDefaultPathButton;

    @FXML
    private Label currentPathLabel;

    @FXML
    private CheckBox autoOpenCheckBox;

    private String selectedPath;

    public void initSettings() {
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        String defaultPath = prefs.get(KEY_DEFAULT_PATH, "");
        boolean autoOpen = prefs.getBoolean(KEY_AUTO_OPEN, false);
        selectedPath = defaultPath;
        currentPathLabel.setText(defaultPath == null || defaultPath.isEmpty() ? "(未設定)" : defaultPath);
        autoOpenCheckBox.setSelected(autoOpen);
    }

    @FXML
    private void onChooseDefaultPath() {
        Stage stage = (Stage) chooseDefaultPathButton.getScene().getWindow();
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("デフォルトフォルダを選択");
        if (selectedPath != null && !selectedPath.isEmpty()) {
            File init = new File(selectedPath);
            if (init.isDirectory()) chooser.setInitialDirectory(init);
        }
        File dir = chooser.showDialog(stage);
        if (dir != null && dir.isDirectory()) {
            selectedPath = dir.getAbsolutePath();
            currentPathLabel.setText(selectedPath);
        }
    }

    @FXML
    private void onSave() {
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        prefs.put(KEY_DEFAULT_PATH, selectedPath == null ? "" : selectedPath);
        prefs.putBoolean(KEY_AUTO_OPEN, autoOpenCheckBox.isSelected());
        // 閉じる
        Stage stage = (Stage) chooseDefaultPathButton.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void onCancel() {
        Stage stage = (Stage) chooseDefaultPathButton.getScene().getWindow();
        stage.close();
    }
}
