package app.br.chronlog.utils.bluetooth;

import app.br.chronlog.BuildConfig;

public class Constants {

    // values have to be globally unique
    public static final String INTENT_ACTION_DISCONNECT = BuildConfig.APPLICATION_ID + ".Disconnect";
    public static final String INTENT_ACTION_CONNECT = BuildConfig.APPLICATION_ID + ".Connect";
    public static final String NOTIFICATION_CHANNEL = BuildConfig.APPLICATION_ID + ".Channel";
    public static final String INTENT_CLASS_MAIN_ACTIVITY = BuildConfig.APPLICATION_ID + ".MainActivity";

    public static final String CONEXAO_FALHOU = "conexão falhou";
    public static final String CONEXAO_PERDIDA = "conexão perdida";
    public static final String JA_CONECTADO = "ja conectado";
    public static final String CONECTANDO_ = "conectando...";

    // values have to be unique within each app
    public static final int NOTIFY_MANAGER_START_FOREGROUND_SERVICE = 1001;

    private Constants() {}
}
