package app.br.chronlog.activitys.models.CTL0104A;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class CTL0104A_TermoparLog implements Parcelable {
    private String name, peso;
    private ArrayList<CTL0104A_TermoparLogEntry> entries;

    public CTL0104A_TermoparLog(String name, String peso, ArrayList<CTL0104A_TermoparLogEntry> entries) {
        this.name = name;
        this.peso = peso;
        this.entries = entries;
    }

    public CTL0104A_TermoparLog(Parcel parcelable) {
        this.name = parcelable.readString();
        this.peso = parcelable.readString();
        this.entries = new ArrayList<>();
        parcelable.readList(this.entries, CTL0104A_TermoparLogEntry.class.getClassLoader());
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

    public ArrayList<CTL0104A_TermoparLogEntry> getEntries() {
        return entries;
    }

    public void setEntries(ArrayList<CTL0104A_TermoparLogEntry> entries) {
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

    public static final Parcelable.Creator<CTL0104A_TermoparLog> CREATOR = new Parcelable.Creator<CTL0104A_TermoparLog>() {

        @Override
        public CTL0104A_TermoparLog createFromParcel(Parcel source) {
            return new CTL0104A_TermoparLog(source);
        }

        @Override
        public CTL0104A_TermoparLog[] newArray(int size) {
            return new CTL0104A_TermoparLog[size];
        }
    };
}
