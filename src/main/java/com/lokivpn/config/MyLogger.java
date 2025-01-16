package com.lokivpn.config;

public class MyLogger implements com.jcraft.jsch.Logger {
    static java.util.Hashtable<Integer, String> name = new java.util.Hashtable<>();
    static {
        name.put(DEBUG, "DEBUG: ");
        name.put(INFO, "INFO: ");
        name.put(WARN, "WARN: ");
        name.put(ERROR, "ERROR: ");
        name.put(FATAL, "FATAL: ");
    }

    @Override
    public boolean isEnabled(int level) {
        return true; // Включить логирование для всех уровней
    }

    @Override
    public void log(int level, String message) {
        System.out.print(name.get(level));
        System.out.println(message);
    }
}



