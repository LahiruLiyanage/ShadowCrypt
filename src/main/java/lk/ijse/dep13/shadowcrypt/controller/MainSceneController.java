package lk.ijse.dep13.shadowcrypt.controller;

import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;

public class MainSceneController {
    public Button btnBrowse;
    public Button btnDecrypt;
    public Button btnEncrypt;
    public Label lblStatus;
    public ProgressBar prgProgress;
    public TextField txtFilePath;
    public PasswordField txtPassword;

    private volatile boolean isProcessing = false;

    public void btnBrowseOnAction(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Directory");
        File directory = directoryChooser.showDialog(btnBrowse.getScene().getWindow());
        if (directory != null) {
            txtFilePath.setText(directory.getAbsolutePath());
        }
    }

    public void btnDecryptOnAction(ActionEvent event) {
        if (!validateInputs()) return;
        if (isProcessing) {
            showAlert("Warning", "A process is already running!");
            return;
        }

        try {
            isProcessing = true;
            processDirectory(Paths.get(txtFilePath.getText()), true);
            showAlert("Success", "Directory decrypted successfully!");
            clearFields();
        } catch (Exception e) {
            showAlert("Error", "Failed to decrypt directory: " + e.getMessage());
        } finally {
            isProcessing = false;
        }
    }

    public void btnEncryptOnAction(ActionEvent event) {
        if (!validateInputs()) return;
        if (isProcessing) {
            showAlert("Warning", "A process is already running!");
            return;
        }

        try {
            isProcessing = true;
            processDirectory(Paths.get(txtFilePath.getText()), false);
            showAlert("Success", "Directory encrypted successfully!");
            clearFields();
        } catch (Exception e) {
            showAlert("Error", "Failed to encrypt directory: " + e.getMessage());
        } finally {
            isProcessing = false;
        }
    }

    private void processDirectory(Path directory, boolean isDecrypting) throws IOException {
        // First, calculate total size for progress tracking
        AtomicLong totalSize = new AtomicLong(0);
        AtomicLong processedSize = new AtomicLong(0);

        // Count total size
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String fileName = file.toString();
                if (isDecrypting && fileName.endsWith(".encrypted")) {
                    totalSize.addAndGet(attrs.size());
                } else if (!isDecrypting && !fileName.endsWith(".encrypted")) {
                    totalSize.addAndGet(attrs.size());
                }
                return FileVisitResult.CONTINUE;
            }
        });

        // Process files
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.toString();
                if (isDecrypting && fileName.endsWith(".encrypted")) {
                    decrypt(file.toFile(), txtPassword.getText(), processedSize, totalSize);
                } else if (!isDecrypting && !fileName.endsWith(".encrypted")) {
                    encrypt(file.toFile(), txtPassword.getText(), processedSize, totalSize);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void encrypt(File source, String password, AtomicLong processedSize, AtomicLong totalSize) throws IOException {
        Path target = Paths.get(source.getParent(), source.getName() + ".encrypted");
        prgProgress.setVisible(true);

        try (FileChannel sourceChannel = FileChannel.open(source.toPath(), StandardOpenOption.READ);
             FileChannel targetChannel = FileChannel.open(target,
                     StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {

            // Write password as signature
            ByteBuffer passwordBuffer = ByteBuffer.wrap(password.getBytes());
            targetChannel.write(passwordBuffer);

            // Process file content
            ByteBuffer buffer = ByteBuffer.allocateDirect(8192); // Using direct buffer for better performance
            while (sourceChannel.read(buffer) != -1) {
                buffer.flip();
                byte[] array = new byte[buffer.remaining()];
                buffer.get(array);

                // Encrypt the bytes
                for (int i = 0; i < array.length; i++) {
                    array[i] = (byte) ~array[i];
                }

                targetChannel.write(ByteBuffer.wrap(array));
                processedSize.addAndGet(array.length);
                updateProgress(processedSize.get(), totalSize.get());

                buffer.clear();
            }
        }

        Files.delete(source.toPath());
    }

    private void decrypt(File source, String password, AtomicLong processedSize, AtomicLong totalSize) throws IOException {
        Path target = Paths.get(source.getParent(), source.getName().replace(".encrypted", ""));
        prgProgress.setVisible(true);

        try (FileChannel sourceChannel = FileChannel.open(source.toPath(), StandardOpenOption.READ);
             FileChannel targetChannel = FileChannel.open(target,
                     StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {

            // Verify password
            byte[] storedPassword = new byte[password.length()];
            ByteBuffer passwordBuffer = ByteBuffer.wrap(storedPassword);
            sourceChannel.read(passwordBuffer);
            if (!password.equals(new String(storedPassword))) {
                throw new IOException("Invalid password!");
            }

            // Process file content
            ByteBuffer buffer = ByteBuffer.allocateDirect(8192);
            while (sourceChannel.read(buffer) != -1) {
                buffer.flip();
                byte[] array = new byte[buffer.remaining()];
                buffer.get(array);

                // Decrypt the bytes
                for (int i = 0; i < array.length; i++) {
                    array[i] = (byte) ~array[i];
                }

                targetChannel.write(ByteBuffer.wrap(array));
                processedSize.addAndGet(array.length);
                updateProgress(processedSize.get(), totalSize.get());

                buffer.clear();
            }
        }

        Files.delete(source.toPath());
    }

    private boolean validateInputs() {
        if (txtFilePath.getText().isEmpty()) {
            showAlert("Error", "Please select a directory!");
            return false;
        }
        if (txtPassword.getText().isEmpty()) {
            showAlert("Error", "Please enter a password!");
            return false;
        }
        File dir = new File(txtFilePath.getText());
        if (!dir.exists() || !dir.isDirectory()) {
            showAlert("Error", "Selected path is not a valid directory!");
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