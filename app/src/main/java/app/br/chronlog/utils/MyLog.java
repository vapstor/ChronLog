package app.br.chronlog.utils;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class MyLog implements Parcelable {
    private String name, peso;
    private ArrayList<MyLogEntry> entries;

    public MyLog(String name, String peso, ArrayList<MyLogEntry> entries) {
        this.name = name;
        this.peso = peso;
        this.entries = entries;
    }

    public MyLog(Parcel parcelable) {
        this.name = parcelable.readString();
        this.peso = parcelable.readString();
        this.entries = new ArrayList<>();
        parcelable.readList(this.entries, MyLogEntry.class.getClassLoader());
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


    /**
     * Parcelable para transmitir objeto para atividade
     */
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(peso);
        dest.writeList(entries);
    }

    public static final Parcelable.Creator<MyLog> CREATOR = new Parcelable.Creator<MyLog>() {

        @Override
        public MyLog createFromParcel(Parcel source) {
            return new MyLog(source);
        }

        @Override
        public MyLog[] newArray(int size) {
            return new MyLog[size];
        }
    };
}
