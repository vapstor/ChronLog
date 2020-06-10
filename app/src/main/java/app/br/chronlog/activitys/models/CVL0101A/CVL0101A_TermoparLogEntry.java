package app.br.chronlog.activitys.models.CVL0101A;

import android.os.Parcel;
import android.os.Parcelable;

public class CVL0101A_TermoparLogEntry implements Parcelable {
    private String data, hora, vMax, vMin, vMed, THD;

    public CVL0101A_TermoparLogEntry(String data, String hora, String vMax, String vMin, String vMed, String THD) {
        this.data = data;
        this.hora = hora;
        this.vMax = vMax;
        this.vMin = vMin;
        this.vMed = vMed;
        this.THD = THD;
    }

    public CVL0101A_TermoparLogEntry(Parcel parcel) {
        this.data = parcel.readString();
        this.hora = parcel.readString();
        this.vMax = parcel.readString();
        this.vMin = parcel.readString();
        this.vMed = parcel.readString();
        this.THD = parcel.readString();
    }

    public String getData() {
        return data;
    }

    public String getHora() {
        return hora;
    }

    public String getvMax() {
        return vMax;
    }

    public String getvMin() {
        return vMin;
    }

    public String getvMed() {
        return vMed;
    }

    public String getTHD() {
        return THD;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(data);
        dest.writeString(hora);
        dest.writeString(vMax);
        dest.writeString(vMin);
        dest.writeString(vMed);
        dest.writeString(THD);
    }

    public static final Creator<CVL0101A_TermoparLogEntry> CREATOR = new Creator<CVL0101A_TermoparLogEntry>() {

        @Override
        public CVL0101A_TermoparLogEntry createFromParcel(Parcel source) {
            return new CVL0101A_TermoparLogEntry(source);
        }

        @Override
        public CVL0101A_TermoparLogEntry[] newArray(int size) {
            return new CVL0101A_TermoparLogEntry[size];
        }
    };
}
