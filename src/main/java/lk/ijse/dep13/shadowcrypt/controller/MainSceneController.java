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

    private void decrypt(File source, String password) throws IOException {
        String decryptedName = source.getName().replace(".encrypted", "");
        File target = new File(source.getParent(), decryptedName);
        prgProgress.setVisible(true);

        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(target)) {

            // Read and verify password
            byte[] storedPassword = new byte[password.length()];
            if (fis.read(storedPassword) != password.length() ||
                    !password.equals(new String(storedPassword))) {
                throw new IOException("Invalid password!");
            }

            byte[] buffer = new byte[1024];
            int read;
            long totalBytes = source.length() - password.length();
            long processedBytes = 0;

            while ((read = fis.read(buffer)) != -1) {
                for (int i = 0; i < read; i++) {
                    buffer[i] = (byte) ~buffer[i];  // Simple XOR decryption
                }
                fos.write(buffer, 0, read);

                processedBytes += read;
                updateProgress(processedBytes, totalBytes);
            }
        }

        if (!source.delete()) {
            throw new IOException("Failed to delete the encrypted file");
        }

        prgProgress.setVisible(false);
    }

    private boolean validateInputs() {
        if (txtFilePath.getText().isEmpty()) {
            showAlert("Error", "Please select a file!");
            return false;
        }
        if (txtPassword.getText().isEmpty()) {
            showAlert("Error", "Please enter a password!");
            return false;
        }
        return true;
    }

    private void updateProgress(long processed, long total) {
        double progress = (double) processed / total;
        prgProgress.setProgress(progress);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void clearFields() {
        txtFilePath.clear();
        txtPassword.clear();
        prgProgress.setProgress(0);
        prgProgress.setVisible(false);
    }
}