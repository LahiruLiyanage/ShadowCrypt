package lk.ijse.dep13.shadowcrypt.controller;

import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import java.io.*;

public class MainSceneController {
    public Button btnBrowse;
    public Button btnDecrypt;
    public Button btnEncrypt;
    public Label lblStatus;
    public ProgressBar prgProgress;
    public TextField txtFilePath;
    public PasswordField txtPassword;

    public void btnBrowseOnAction(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File");
        File file = fileChooser.showOpenDialog(btnBrowse.getScene().getWindow());
        if (file != null) {
            txtFilePath.setText(file.getAbsolutePath());
        }
    }

    public void btnDecryptOnAction(ActionEvent event) {
        if (!validateInputs()) return;

        File sourceFile = new File(txtFilePath.getText());
        if (!sourceFile.getName().endsWith(".encrypted")) {
            showAlert("Error", "Selected file is not an encrypted file!");
            return;
        }

        try {
            decrypt(sourceFile, txtPassword.getText());
            showAlert("Success", "File decrypted successfully!");
            clearFields();
        } catch (IOException e) {
            showAlert("Error", "Failed to decrypt file: " + e.getMessage());
        }
    }


}