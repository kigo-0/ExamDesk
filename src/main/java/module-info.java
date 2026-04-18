module io.github.kigo.examdesk {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;

    requires java.xml;
    requires java.prefs;

    opens io.github.kigo.examdesk to javafx.fxml;
    exports io.github.kigo.examdesk;
}