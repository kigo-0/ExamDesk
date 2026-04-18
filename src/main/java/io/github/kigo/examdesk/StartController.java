package io.github.kigo.examdesk;

import java.io.File;
import java.util.prefs.Preferences;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class StartController {

	private static final String PREF_NODE = "quiz.settings";
	private static final String KEY_DEFAULT_PATH = "defaultPath";
	private static final String KEY_AUTO_OPEN = "autoOpenDefault";

	@FXML
	private TextField countField;

	@FXML
	private Label messageLabel;

	@FXML
	private Button listResultsButton;

	@FXML
	private Button resumeButton;

	@FXML
	private Button settingsButton;

	@FXML
	private void onStart() {
		String text = countField.getText().trim();
		if (text.isEmpty()) {
			messageLabel.setText("問題数を入力してください。");
			return;
		}
		int n;
		try {
			n = Integer.parseInt(text);
			if (n <= 0) {
				messageLabel.setText("正の整数を入力してください。");
				return;
			}
		} catch (NumberFormatException e) {
			messageLabel.setText("整数で入力してください。");
			return;
		}

		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/kigo/examdesk/quiz.fxml"));
			Parent root = loader.load();
			QuizController controller = loader.getController();
			controller.initWithCount(n);

			Stage stage = (Stage) countField.getScene().getWindow();
			Scene scene = new Scene(root, 600, 500);
			stage.setScene(scene);
			stage.setResizable(true);
		} catch (Exception ex) {
			ex.printStackTrace();
			messageLabel.setText("画面遷移に失敗しました: " + ex.getMessage());
		}
	}

	@FXML
	private void onListResults() {
		try {
			Stage stage = (Stage) countField.getScene().getWindow();
			Preferences prefs = Preferences.userRoot().node(PREF_NODE);
			String defaultPath = prefs.get(KEY_DEFAULT_PATH, "");
			boolean autoOpen = prefs.getBoolean(KEY_AUTO_OPEN, false);

			File dir = null;
			if (autoOpen && defaultPath != null && !defaultPath.isEmpty()) {
				File d = new File(defaultPath);
				if (d.isDirectory()) {
					dir = d;
				}
			}

			if (dir == null) {
				DirectoryChooser chooser = new DirectoryChooser();
				chooser.setTitle("結果が入ったフォルダを選択");
				if (defaultPath != null && !defaultPath.isEmpty()) {
					File init = new File(defaultPath);
					if (init.isDirectory())
						chooser.setInitialDirectory(init);
				}
				dir = chooser.showDialog(stage);
				if (dir == null || !dir.isDirectory()) {
					return;
				}
			}

			FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/kigo/examdesk/results.fxml"));
			Parent root = loader.load();
			ResultsController controller = loader.getController();
			controller.loadFromDirectory(dir);

			Scene scene = new Scene(root, 900, 520);
			stage.setScene(scene);
			stage.setResizable(true);
		} catch (Exception e) {
			e.printStackTrace();
			messageLabel.setText("結果一覧の表示に失敗しました: " + e.getMessage());
		}
	}

	@FXML
	private void onResumeExam() {
		try {
			Stage stage = (Stage) countField.getScene().getWindow();
			FileChooser chooser = new FileChooser();
			chooser.setTitle("復元する試験ファイルを選択");
			chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));

			Preferences prefs = Preferences.userRoot().node(PREF_NODE);
			String defaultPath = prefs.get(KEY_DEFAULT_PATH, "");
			if (defaultPath != null && !defaultPath.isEmpty()) {
				File init = new File(defaultPath);
				if (init.isDirectory())
					chooser.setInitialDirectory(init);
			}

			File file = chooser.showOpenDialog(stage);
			if (file == null)
				return;

			FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/kigo/examdesk/quiz.fxml"));
			Parent root = loader.load();
			QuizController controller = loader.getController();

			Scene scene = new Scene(root, 600, 500);
			stage.setScene(scene);
			stage.setResizable(true);

			controller.loadFromFile(file);
		} catch (Exception e) {
			e.printStackTrace();
			messageLabel.setText("試験の復元に失敗しました: " + e.getMessage());
		}
	}

	@FXML
	private void onOpenSettings() {
		try {
			Stage stage = (Stage) countField.getScene().getWindow();
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/kigo/examdesk/settings.fxml"));
			Parent root = loader.load();
			SettingsController controller = loader.getController();
			controller.initSettings();

			Scene scene = new Scene(root, 480, 220);
			Stage dialog = new Stage();
			dialog.initOwner(stage);
			dialog.setTitle("設定");
			dialog.setScene(scene);
			dialog.setResizable(false);
			dialog.show();
		} catch (Exception e) {
			e.printStackTrace();
			messageLabel.setText("設定画面を開けませんでした: " + e.getMessage());
		}
	}
}
