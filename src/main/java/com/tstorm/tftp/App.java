package com.tstorm.tftp;

import com.tstorm.tftp.client.Sender;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Optional;

/**
 * A small file transfer application using UDP datagrams to implement a TCP-like Sliding Windows protocol
 */
public class App extends JFrame implements ActionListener {

    private static final int WIDTH = 300;
    private static final int HEIGHT = 175;

    private JTextField hostField, fileField;
    private JButton fileButton, uploadButton;

    public App() {
        super("TinyFTP");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        addComponents(getContentPane());
        centerDialog();
        this.fileButton.addActionListener(this);
        this.uploadButton.addActionListener(this);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::new);
//        Sender s = new Sender("127.0.0.1");
//        s.upload("contact.html");
    }

    private void addComponents(Container contentPane) {
        contentPane.setLayout(new GridLayout(3, 1));
        JPanel host = new JPanel(new MigLayout("", "[][grow, fill]", ""));
        JLabel hostLabel = new JLabel("Host");
        this.hostField = new JTextField();
        host.add(hostLabel);
        host.add(this.hostField);
        add(host);

        JPanel file = new JPanel(new MigLayout("", "[][grow, fill][]", ""));
        JLabel fileLabel = new JLabel("File");
        this.fileField = new JTextField();
        this.fileButton = new JButton(("..."));
        file.add(fileLabel);
        file.add(this.fileField);
        file.add(this.fileButton);
        add(file);

        this.uploadButton = new JButton("Upload");
        add(this.uploadButton);
    }

    private void centerDialog() {
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final int x = (screenSize.width - WIDTH) >> 1;
        final int y = (screenSize.height - HEIGHT) >> 1;
        setLocation(x, y);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.fileButton) {
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.showOpenDialog(this);
            File f = fileChooser.getSelectedFile();
            this.fileField.setText(f.getPath());
        } else if (e.getSource() == this.uploadButton) {
            if (validateInput()) {
                setInterfaceEnabled(false);
                sendFile(this.fileField.getText());
            }
        }
    }

    private boolean validateInput() {
        if (this.hostField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a hostname", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } else if (this.fileField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a filename", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } else {
            return true;
        }
    }

    private void setInterfaceEnabled(boolean enabled) {
        this.hostField.setEditable(enabled);
        this.fileField.setEditable(enabled);
        this.fileButton.setEnabled(enabled);
        this.uploadButton.setEnabled(enabled);
    }

    private void sendFile(String filename) {
        try {
            Sender sender = new Sender(this.hostField.getText());
            sender.upload(filename).join();
            JOptionPane.showMessageDialog(this, "Upload complete", "Success", JOptionPane.INFORMATION_MESSAGE);
            setInterfaceEnabled(true);
            System.exit(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (RuntimeException exception) {
            StringBuilder builder = new StringBuilder();
            Optional<String> msg = Optional.ofNullable(exception.getMessage());
            if (msg.isPresent()) {
                builder.append(msg.get());
            } else {
                builder.append("Unknown error");
            }
            builder.append("\nTinyFTP will now close");
            JOptionPane.showMessageDialog(this, builder.toString(), "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

}
