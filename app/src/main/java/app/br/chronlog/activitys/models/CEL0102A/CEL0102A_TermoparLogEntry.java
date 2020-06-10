package app.br.chronlog.activitys.models.CEL0102A;

import android.os.Parcel;
import android.os.Parcelable;

public class CEL0102A_TermoparLogEntry implements Parcelable {
    private String data, hora, v, i, p, e, fp, dht;

    public CEL0102A_TermoparLogEntry(String data, String hora, String v, String i, String p, String e, String fp, String dht) {
        this.data = data;
        this.hora = hora;
        this.v = v;
        this.i = i;
        this.p = p;
        this.e = e;
        this.fp = fp;
        this.dht = dht;
    }

    public CEL0102A_TermoparLogEntry(Parcel parcel) {
        this.data = parcel.readString();
        this.hora = parcel.readString();
        this.v = parcel.readString();
        this.i = parcel.readString();
        this.p = parcel.readString();
        this.e = parcel.readString();
        this.fp = parcel.readString();
        this.dht = parcel.readString();
    }

    public String getData() {
        return data;
    }

    public String getHora() {
        return hora;
    }

    public String getV() {
        return v;
    }

    public String getI() {
        return i;
    }

    public String getP() {
        return p;
    }

    public String getE() {
        return e;
    }

    public String getFp() {
        return fp;
    }

    public String getDht() {
        return dht;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(data);
        dest.writeString(hora);
        dest.writeString(v);
        dest.writeString(i);
        dest.writeString(p);
        dest.writeString(e);
        dest.writeString(fp);
        dest.writeString(dht);
    }

    public static final Creator<CEL0102A_TermoparLogEntry> CREATOR = new Creator<CEL0102A_TermoparLogEntry>() {

        @Override
        public CEL0102A_TermoparLogEntry createFromParcel(Parcel source) {
            return new CEL0102A_TermoparLogEntry(source);
        }

        @Override
        public CEL0102A_TermoparLogEntry[] newArray(int size) {
            return new CEL0102A_TermoparLogEntry[size];
        }
    };
}
