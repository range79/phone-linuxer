module com.range.phonelinuxerdesktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;


    opens com.range.phonelinuxerdesktop to javafx.fxml;
    exports com.range.phonelinuxerdesktop;
}