package app.br.chronlog.activitys.models.CTL0104B;

import android.os.Parcel;
import android.os.Parcelable;

public class CTL0104B_TermoparLogEntry implements Parcelable {
    private String data, hora, t1, t2, t3, t4, m5, m6, m7;

    public CTL0104B_TermoparLogEntry(String data, String hora, String t1, String t2, String t3, String t4, String m5, String m6, String m7) {
        this.data = data;
        this.hora = hora;
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
        this.t4 = t4;
        this.m5 = m5;
        this.m6 = m6;
        this.m7 = m7;
    }

    public CTL0104B_TermoparLogEntry(Parcel parcel) {
        this.data = parcel.readString();
        this.hora = parcel.readString();
        this.t1 = parcel.readString();
        this.t2 = parcel.readString();
        this.t3 = parcel.readString();
        this.t4 = parcel.readString();
        this.m5 = parcel.readString();
        this.m6 = parcel.readString();
        this.m7 = parcel.readString();
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

    public String getM5() {
        return m5;
    }

    public String getM6() {
        return m6;
    }

    public String getM7() {
        return m7;
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
        dest.writeString(m5);
        dest.writeString(m6);
        dest.writeString(m7);
    }

    public static final Creator<CTL0104B_TermoparLogEntry> CREATOR = new Creator<CTL0104B_TermoparLogEntry>() {

        @Override
        public CTL0104B_TermoparLogEntry createFromParcel(Parcel source) {
            return new CTL0104B_TermoparLogEntry(source);
        }

        @Override
        public CTL0104B_TermoparLogEntry[] newArray(int size) {
            return new CTL0104B_TermoparLogEntry[size];
        }
    };
}
