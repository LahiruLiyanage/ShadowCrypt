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

    public void btnEncryptOnAction(ActionEvent event) {
        if (!validateInputs()) return;

        File sourceFile = new File(txtFilePath.getText());
        if (sourceFile.getName().endsWith(".encrypted")) {
            showAlert("Error", "File is already encrypted!");
            return;
        }

        try {
            encrypt(sourceFile, txtPassword.getText());
            showAlert("Success", "File encrypted successfully!");
            clearFields();
        } catch (IOException e) {
            showAlert("Error", "Failed to encrypt file: " + e.getMessage());
        }
    }

    private void encrypt(File source, String password) throws IOException {
        File target = new File(source.getParent(), source.getName() + ".encrypted");
        prgProgress.setVisible(true);

        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(target)) {

            // Write password as signature
            fos.write(password.getBytes());

            byte[] buffer = new byte[1024];
            int read;
            long totalBytes = source.length();
            long processedBytes = 0;

            while ((read = fis.read(buffer)) != -1) {
                for (int i = 0; i < read; i++) {
                    buffer[i] = (byte) ~buffer[i];  // Simple XOR encryption
                }
                fos.write(buffer, 0, read);

                processedBytes += read;
                updateProgress(processedBytes, totalBytes);
            }
        }

        if (!source.delete()) {
            throw new IOException("Failed to delete the original file");
        }

        prgProgress.setVisible(false);
    }


}