package lk.ijse.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientFormController {

    @FXML
    private Button btnSend;

    @FXML
    private Label lblStatus;

    @FXML
    private TextArea txtChatArea;

    @FXML
    private TextField txtMessage;

    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    @FXML
    public void initialize() {
        connectToServer();
    }

    private void connectToServer() {
        Thread connectThread = new Thread(() -> {
            int retries = 0;
            while (socket == null || socket.isClosed()) {
                try {
                    final int attemptNumber = retries + 1;
                    Platform.runLater(() -> lblStatus.setText("Connecting to server (Attempt " + attemptNumber + ")..."));
                    
                    socket = new Socket("127.0.0.1", 4000);
                    
                    dataInputStream = new DataInputStream(socket.getInputStream());
                    dataOutputStream = new DataOutputStream(socket.getOutputStream());

                    Platform.runLater(() -> {
                        lblStatus.setText("Connected to Server");
                        txtChatArea.appendText("[System: Connected to server]\n");
                    });

                    while (socket != null && !socket.isClosed()) {
                        try {
                            String message = dataInputStream.readUTF();
                            
                            Platform.runLater(() -> txtChatArea.appendText("Server: " + message + "\n"));
                        } catch (IOException e) {
                            Platform.runLater(() -> {
                                lblStatus.setText("Disconnected");
                                txtChatArea.appendText("[System: Connection to server lost]\n");
                            });
                            closeResources();
                            break;
                        }
                    }

                } catch (IOException e) {
                    retries++;
                    Platform.runLater(() -> lblStatus.setText("Disconnected. Retrying in 3s..."));
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ie) {
                        break;
                    }
                }
            }
        });
        connectThread.setDaemon(true);
        connectThread.start();
    }

    @FXML
    void btnSendOnAction(ActionEvent event) {
        sendMessage();
    }

    @FXML
    void txtMessageOnAction(ActionEvent event) {
        sendMessage();
    }

    private void sendMessage() {
        String msg = txtMessage.getText().trim();
        if (!msg.isEmpty()) {
            if (dataOutputStream != null) {
                Thread sendThread = new Thread(() -> {
                    try {
                        dataOutputStream.writeUTF(msg);
                        dataOutputStream.flush();
                        
                        Platform.runLater(() -> {
                            txtChatArea.appendText("Me: " + msg + "\n");
                            txtMessage.clear();
                        });
                    } catch (IOException e) {
                        Platform.runLater(() -> txtChatArea.appendText("[System: Failed to send message]\n"));
                    }
                });
                sendThread.setDaemon(true);
                sendThread.start();
            } else {
                txtChatArea.appendText("[System: Not connected to server]\n");
            }
        }
    }

    private void closeResources() {
        try {
            if (dataInputStream != null) dataInputStream.close();
            if (dataOutputStream != null) dataOutputStream.close();
            if (socket != null) socket.close();
            socket = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
