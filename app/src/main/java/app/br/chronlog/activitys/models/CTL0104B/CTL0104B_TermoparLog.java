package app.br.chronlog.activitys.models.CTL0104B;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class CTL0104B_TermoparLog implements Parcelable {
    private String name, peso;
    private ArrayList<CTL0104B_TermoparLogEntry> entries;

    public CTL0104B_TermoparLog(String name, String peso, ArrayList<CTL0104B_TermoparLogEntry> entries) {
        this.name = name;
        this.peso = peso;
        this.entries = entries;
    }

    public CTL0104B_TermoparLog(Parcel parcelable) {
        this.name = parcelable.readString();
        this.peso = parcelable.readString();
        this.entries = new ArrayList<>();
        parcelable.readList(this.entries, CTL0104B_TermoparLogEntry.class.getClassLoader());
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

    public ArrayList<CTL0104B_TermoparLogEntry> getEntries() {
        return entries;
    }

    public void setEntries(ArrayList<CTL0104B_TermoparLogEntry> entries) {
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

    public static final Creator<CTL0104B_TermoparLog> CREATOR = new Creator<CTL0104B_TermoparLog>() {

        @Override
        public CTL0104B_TermoparLog createFromParcel(Parcel source) {
            return new CTL0104B_TermoparLog(source);
        }

        @Override
        public CTL0104B_TermoparLog[] newArray(int size) {
            return new CTL0104B_TermoparLog[size];
        }
    };
}
