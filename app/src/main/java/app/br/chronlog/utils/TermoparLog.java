package app.br.chronlog.utils;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class TermoparLog implements Parcelable {
    private String name, peso;
    private ArrayList<TermoparLogEntry> entries;

    public TermoparLog(String name, String peso, ArrayList<TermoparLogEntry> entries) {
        this.name = name;
        this.peso = peso;
        this.entries = entries;
    }

    public TermoparLog(Parcel parcelable) {
        this.name = parcelable.readString();
        this.peso = parcelable.readString();
        this.entries = new ArrayList<>();
        parcelable.readList(this.entries, TermoparLogEntry.class.getClassLoader());
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

    public ArrayList<TermoparLogEntry> getEntries() {
        return entries;
    }

    public void setEntries(ArrayList<TermoparLogEntry> entries) {
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

    public static final Parcelable.Creator<TermoparLog> CREATOR = new Parcelable.Creator<TermoparLog>() {

        @Override
        public TermoparLog createFromParcel(Parcel source) {
            return new TermoparLog(source);
        }

        @Override
        public TermoparLog[] newArray(int size) {
            return new TermoparLog[size];
        }
    };
}
