package app.br.chronlog.activitys.models.CVL0101A;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class CVL0101A_TermoparLog implements Parcelable {
    private String name, peso;
    private ArrayList<CVL0101A_TermoparLogEntry> entries;

    public CVL0101A_TermoparLog(String name, String peso, ArrayList<CVL0101A_TermoparLogEntry> entries) {
        this.name = name;
        this.peso = peso;
        this.entries = entries;
    }

    public CVL0101A_TermoparLog(Parcel parcelable) {
        this.name = parcelable.readString();
        this.peso = parcelable.readString();
        this.entries = new ArrayList<>();
        parcelable.readList(this.entries, CVL0101A_TermoparLogEntry.class.getClassLoader());
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

    public ArrayList<CVL0101A_TermoparLogEntry> getEntries() {
        return entries;
    }

    public void setEntries(ArrayList<CVL0101A_TermoparLogEntry> entries) {
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

    public static final Creator<CVL0101A_TermoparLog> CREATOR = new Creator<CVL0101A_TermoparLog>() {

        @Override
        public CVL0101A_TermoparLog createFromParcel(Parcel source) {
            return new CVL0101A_TermoparLog(source);
        }

        @Override
        public CVL0101A_TermoparLog[] newArray(int size) {
            return new CVL0101A_TermoparLog[size];
        }
    };
}
