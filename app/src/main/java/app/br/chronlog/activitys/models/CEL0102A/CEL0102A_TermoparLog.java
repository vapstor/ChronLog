package app.br.chronlog.activitys.models.CEL0102A;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class CEL0102A_TermoparLog implements Parcelable {
    private String name, peso;
    private ArrayList<CEL0102A_TermoparLogEntry> entries;

    public CEL0102A_TermoparLog(String name, String peso, ArrayList<CEL0102A_TermoparLogEntry> entries) {
        this.name = name;
        this.peso = peso;
        this.entries = entries;
    }

    public CEL0102A_TermoparLog(Parcel parcelable) {
        this.name = parcelable.readString();
        this.peso = parcelable.readString();
        this.entries = new ArrayList<>();
        parcelable.readList(this.entries, CEL0102A_TermoparLogEntry.class.getClassLoader());
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

    public ArrayList<CEL0102A_TermoparLogEntry> getEntries() {
        return entries;
    }

    public void setEntries(ArrayList<CEL0102A_TermoparLogEntry> entries) {
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

    public static final Creator<CEL0102A_TermoparLog> CREATOR = new Creator<CEL0102A_TermoparLog>() {

        @Override
        public CEL0102A_TermoparLog createFromParcel(Parcel source) {
            return new CEL0102A_TermoparLog(source);
        }

        @Override
        public CEL0102A_TermoparLog[] newArray(int size) {
            return new CEL0102A_TermoparLog[size];
        }
    };
}
