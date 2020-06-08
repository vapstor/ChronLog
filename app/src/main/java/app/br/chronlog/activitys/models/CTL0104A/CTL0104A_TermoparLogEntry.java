package app.br.chronlog.activitys.models.CTL0104A;

import android.os.Parcel;
import android.os.Parcelable;

public class CTL0104A_TermoparLogEntry implements Parcelable {
    private String data, hora, t1, t2, t3, t4;

    public CTL0104A_TermoparLogEntry(String data, String hora, String t1, String t2, String t3, String t4) {
        this.data = data;
        this.hora = hora;
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
        this.t4 = t4;
    }

    public CTL0104A_TermoparLogEntry(Parcel parcel) {
        this.data = parcel.readString();
        this.hora = parcel.readString();
        this.t1 = parcel.readString();
        this.t2 = parcel.readString();
        this.t3 = parcel.readString();
        this.t4 = parcel.readString();
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


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(data);
        dest.writeString(hora);
        dest.writeString(t1);
        dest.writeString(t2);
        dest.writeString(t3);
        dest.writeString(t4);
    }

    public static final Parcelable.Creator<CTL0104A_TermoparLogEntry> CREATOR = new Parcelable.Creator<CTL0104A_TermoparLogEntry>() {

        @Override
        public CTL0104A_TermoparLogEntry createFromParcel(Parcel source) {
            return new CTL0104A_TermoparLogEntry(source);
        }

        @Override
        public CTL0104A_TermoparLogEntry[] newArray(int size) {
            return new CTL0104A_TermoparLogEntry[size];
        }
    };
}
