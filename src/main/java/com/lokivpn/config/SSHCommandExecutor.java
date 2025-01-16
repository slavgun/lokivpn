package com.lokivpn.config;

import com.jcraft.jsch.*;

import java.io.InputStream;

public class SSHCommandExecutor {

    public static String executeCommand(String host, String user, String password, String command) {
        StringBuilder output = new StringBuilder();
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(user, host, 22);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setErrStream(System.err);
            InputStream in = channel.getInputStream();

            channel.connect();

            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    output.append(new String(tmp, 0, i));
                }
                if (channel.isClosed()) {
                    break;
                }
                Thread.sleep(1000);
            }
            channel.disconnect();
            session.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output.toString();
    }

    public static void main(String[] args) {
        String host = "46.29.234.231";
        String user = "root";
        String password = "Ckfduey3103"; // Замените на реальный пароль или используйте ключи
        String command = "wg genkey";
        String result = executeCommand(host, user, password, command);
        System.out.println("Output: " + result);
    }
}
