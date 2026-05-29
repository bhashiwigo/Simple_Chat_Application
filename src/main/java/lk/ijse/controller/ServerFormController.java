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
import java.net.ServerSocket;
import java.net.Socket;

public class ServerFormController {

    @FXML
    private Button btnSend;

    @FXML
    private Label lblStatus;

    @FXML
    private TextArea txtChatArea;

    @FXML
    private TextField txtMessage;

    private ServerSocket serverSocket;
    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    @FXML
    public void initialize() {
        Thread serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(4000);
                
                Platform.runLater(() -> lblStatus.setText("Listening on Port 4000..."));

                while (!serverSocket.isClosed()) {
                    socket = serverSocket.accept();
                    
                    Platform.runLater(() -> lblStatus.setText("Connected to Client"));
                    
                    dataInputStream = new DataInputStream(socket.getInputStream());
                    dataOutputStream = new DataOutputStream(socket.getOutputStream());

                    while (socket != null && !socket.isClosed()) {
                        try {
                            String message = dataInputStream.readUTF();
                            
                            Platform.runLater(() -> txtChatArea.appendText("Client: " + message + "\n"));
                        } catch (IOException e) {
                            Platform.runLater(() -> {
                                lblStatus.setText("Disconnected. Listening...");
                                txtChatArea.appendText("[System: Client disconnected]\n");
                            });
                            closeResources();
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                Platform.runLater(() -> lblStatus.setText("Server Error / Stopped"));
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
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
                            txtChatArea.appendText("Server: " + msg + "\n");
                            txtMessage.clear();
                        });
                    } catch (IOException e) {
                        Platform.runLater(() -> txtChatArea.appendText("[System: Failed to send message]\n"));
                    }
                });
                sendThread.setDaemon(true);
                sendThread.start();
            } else {
                txtChatArea.appendText("[System: No client connected]\n");
            }
        }
    }

    private void closeResources() {
        try {
            if (dataInputStream != null) dataInputStream.close();
            if (dataOutputStream != null) dataOutputStream.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
