package app.br.chronlog.utils;

public class MyLogEntry {
    private String data, hora, t1, t2, t3, t4;

    public MyLogEntry(String data, String hora, String t1, String t2, String t3, String t4) {
        this.data = data;
        this.hora = hora;
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
        this.t4 = t4;
    }

    public String getData() {
        return data;
    }

    public String getHora() {
        return hora;
    }

    public String getT1() {
        return t1;
    }

    public String getT2() {
        return t2;
    }

    public String getT3() {
        return t3;
    }

    public String getT4() {
        return t4;
    }

}
