package app.br.chronlog.utils;

import java.util.ArrayList;

public class MyLog {
    private String name, peso;
    private ArrayList<MyLogEntry> entries;

    public MyLog(String name, String peso, ArrayList<MyLogEntry> entries) {
        this.name = name;
        this.peso = peso;
        this.entries = entries;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPeso() {
        return peso;
    }

    public void setPeso(String peso) {
        this.peso = peso;
    }

    public ArrayList<MyLogEntry> getEntries() {
        return entries;
    }

    public void setEntries(ArrayList<MyLogEntry> entries) {
        this.entries = entries;
    }
}
