package app.br.chronlog.activitys;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import app.br.chronlog.R;
import app.br.chronlog.activitys.models.CTL0104A.CTL0104A_TermoparLog;
import app.br.chronlog.activitys.models.CTL0104A.CTL0104A_TermoparLogEntry;
import app.br.chronlog.activitys.models.CTL0104B.CTL0104B_TermoparLog;
import app.br.chronlog.activitys.models.CTL0104B.CTL0104B_TermoparLogEntry;
import app.br.chronlog.utils.MyMarkerView;

import static android.graphics.Color.CYAN;
import static android.graphics.Color.GRAY;
import static android.graphics.Color.GREEN;
import static android.graphics.Color.MAGENTA;
import static android.graphics.Color.RED;
import static android.graphics.Color.YELLOW;
import static app.br.chronlog.activitys.DevicesActivity.modelo;
import static app.br.chronlog.utils.Utils.TAG_LOG;

public class ChartViewActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener,
        OnChartValueSelectedListener {

    private LineChart chart;
    private ArrayList<Parcelable> selectedLog;
    private Button btnT1, btnT2, btnT3, btnT4, btnM5, btnM6, btnM7;
    private LineData allData;
    private ArrayList<ILineDataSet> allDataSets;
    private float VALOR_DISCREPANTE = 999;
    private boolean existeValorDiscrepante;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chart);

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            finish();
        } else {
            selectedLog = getIntent().getParcelableArrayListExtra("selectedLog");
            List entriesList;
            if (selectedLog != null) {
                ((TextView) findViewById(R.id.logTitleTxtView)).setText(extras.getString("logName"));
                switch (modelo) {
                    case "CTL0104B":
                        CTL0104B_TermoparLog CTL0104BTermoparLog = (CTL0104B_TermoparLog) selectedLog.get(0);
                        entriesList = CTL0104BTermoparLog.getEntries();
                        acessaDadosDoArquivo(entriesList);
                        break;
                    case "CTL0104A":
                    default:
                        CTL0104A_TermoparLog CTL0104ATermoparLog = (CTL0104A_TermoparLog) selectedLog.get(0);
                        entriesList = CTL0104ATermoparLog.getEntries();
                        acessaDadosDoArquivo(entriesList);
                        break;
                }
            } else {
                Toast.makeText(this, "Falhou ao resgatar os dados!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        switch (modelo) {
            case "CTL0104B":
                btnM5 = findViewById(R.id.m5Button);
                btnM5.setVisibility(View.VISIBLE);

                btnM6 = findViewById(R.id.m6Button);
                btnM6.setVisibility(View.VISIBLE);

                btnM7 = findViewById(R.id.m7Button);
                btnM7.setVisibility(View.VISIBLE);

                btnM5.setOnClickListener((v -> {
                    toogleBtnPressed(v);
                    toggleDataSetVisibility(4);
                }));
                btnM6.setOnClickListener((v -> {
                    toogleBtnPressed(v);
                    toggleDataSetVisibility(5);
                }));
                btnM7.setOnClickListener((v -> {
                    toogleBtnPressed(v);
                    toggleDataSetVisibility(6);
                }));
            case "CTL0104A":
            default:
                btnT1 = findViewById(R.id.t1Button);
                btnT2 = findViewById(R.id.t2Button);
                btnT3 = findViewById(R.id.t3Button);
                btnT4 = findViewById(R.id.t4Button);
                btnT1.setOnClickListener((v) -> {
                    toogleBtnPressed(v);
                    toggleDataSetVisibility(0);
                });
                btnT2.setOnClickListener((v) -> {
                    toogleBtnPressed(v);
                    toggleDataSetVisibility(1);
                });
                btnT3.setOnClickListener((v) -> {
                    toogleBtnPressed(v);
                    toggleDataSetVisibility(2);
                });
                btnT4.setOnClickListener((v -> {
                    toogleBtnPressed(v);
                    toggleDataSetVisibility(3);
                }));
                break;
        }
    }

    private void toogleBtnPressed(View v) {
        if (v.isSelected()) {
            v.setSelected(false);
            v.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            ((Button) v).setTextColor(getResources().getColor(R.color.branco));
        } else {
            v.setSelected(true);
            v.setBackgroundColor(getResources().getColor(R.color.cinzaClaro));
            ((Button) v).setTextColor(getResources().getColor(R.color.cinzaEscuro));
        }
    }

    private void toggleDataSetVisibility(int datasetIdx) {
        List<ILineDataSet> sets = chart.getData().getDataSets();
        List<ILineDataSet> visibleSets = new ArrayList<>();
        ILineDataSet dataset = sets.get(datasetIdx);

        dataset.setVisible(!dataset.isVisible());

        /**Rápida checagem para ver o tamanho do array que irá ser sorteado.
         * Possibilidade de conflito com temperatura "0.0" ao colocar um valor 'nulo (0.0)' no array.
         *
         * Resgata sets visiveis para evitar processamento desnecessário.
         * */

        for (ILineDataSet mySet : sets) {
            if (mySet.isVisible())
                visibleSets.add(mySet);
        }

        float[] myArrayMin = new float[visibleSets.size()];
        float[] myArrayMax = new float[visibleSets.size()];
        int lengthVisible = visibleSets.size();
        if (lengthVisible != 0) {
            for (int i = 0; i < visibleSets.size(); i++) {
                LineDataSet set = (LineDataSet) visibleSets.get(i);
                float setYmin = Float.parseFloat(String.valueOf(set.getYMin()));
                float setYmax = Float.parseFloat(String.valueOf(set.getYMax()));
                /**
                 * (-5) Ajuste necessário para uma posição algumas casas menor/maior
                 * para que se obtenha uma boa visualização e ele não fique grudado no fundo/topo do chart
                 * */
//                if (existeValorDiscrepante) {
                myArrayMin[i] = setYmin - 5;
                myArrayMax[i] = setYmax + 5;
//                } else {
//                    myArrayMin[i] = setYmin - 10;
//                    myArrayMax[i] = setYmax + 10;
//                }
            }

            Arrays.sort(myArrayMin);
            Arrays.sort(myArrayMax);
            chart.getAxisLeft().setAxisMinimum(myArrayMin[0]);
            chart.getAxisLeft().setAxisMaximum(myArrayMax[myArrayMax.length - 1]);
        } else {
            Toast.makeText(this, "Sem dados habilitados!", Toast.LENGTH_SHORT).show();
        }
        chart.invalidate();
        chart.getData().notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.animateX(1500);
    }

    private void acessaDadosDoArquivo(List entriesList) {

        if (chart != null) {
            chart.invalidate();
        }

        {   // // Chart Style // //
            chart = findViewById(R.id.chart);

            // background color
            chart.setBackgroundColor(Color.WHITE);

            // disable description text
            chart.getDescription().setEnabled(false);

            chart.setBorderColor(Color.BLACK);
            chart.setGridBackgroundColor(getResources().getColor(R.color.cinzaClaro));

            chart.setElevation(2);

            // enable touch gestures
            chart.setTouchEnabled(true);

            // set listeners
            chart.setOnChartValueSelectedListener(this);
            chart.setDrawGridBackground(false);

            // enable scaling and dragging
            chart.setDragEnabled(true);
            chart.setScaleEnabled(true);

            // force pinch zoom along both axis
            chart.setPinchZoom(true);

        }

        XAxis xAxis;
        String[] horariosX;
        {
            xAxis = chart.getXAxis();
            horariosX = new String[entriesList.size()];
            // the labels that should be drawn on the XAxis
            switch (modelo) {
                case "CTL0104B":
                    for (int i = 0; i < entriesList.size(); i++) {
                        horariosX[i] = ((CTL0104B_TermoparLogEntry) entriesList.get(i)).getHora();
                    }
                    break;
                case "CTL0104A":
                default:
                    for (int i = 0; i < entriesList.size(); i++) {
                        horariosX[i] = ((CTL0104A_TermoparLogEntry) entriesList.get(i)).getHora();
                    }
                    break;
            }

            ValueFormatter formatter = new ValueFormatter() {
                @Override
                public String getAxisLabel(float value, AxisBase axis) {
                    Log.d(TAG_LOG, "getAxisLabel VALOR: " + value);
                    if (value >= 0) {
                        if (value <= horariosX.length - 1) {
                            return horariosX[(int) value];
                        }
                        return "";
                    }
                    return "";
                }
            };
            xAxis.setAxisMinimum(0);
            xAxis.setAxisMaximum(horariosX.length);
            xAxis.setCenterAxisLabels(true);
//            xAxis.setLabelCount(horariosX.length, true);
            xAxis.setGranularity(1f); // minimum axis-step (interval) is 1
            xAxis.setValueFormatter(formatter);
            // vertical grid lines
            xAxis.enableGridDashedLine(5f, 10f, 0f);
        }
        YAxis yAxis;
        {   // // Y-Axis Style // //
            yAxis = chart.getAxisLeft();

            yAxis.setCenterAxisLabels(true);

            // disable dual axis (only use LEFT axis)
            chart.getAxisRight().setEnabled(false);

            // horizontal grid lines
            yAxis.enableGridDashedLine(5f, 10f, 0f);

        }

        setData(entriesList);

        // create marker to display box when values are selected
        MyMarkerView mv = new MyMarkerView(this, R.layout.custom_marker_view, horariosX);

        // Set the marker to the chart
        mv.setChartView(chart);
        chart.setMarker(mv);

        chart.getData().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                String a = super.getFormattedValue(value);
                return a + "º";
            }
        });

        chart.setPadding(10, 10, 10, 10);

        // draw points over time
        chart.animateX(1500);

        // get the legend (only possible after setting data)
        Legend l = chart.getLegend();
        l.setTextColor(getResources().getColor(R.color.colorPrimary));
        // draw legend entries as lines
        l.setWordWrapEnabled(true);
        l.setForm(Legend.LegendForm.LINE);
    }

    private void setData(List entriesList) {
        allDataSets = new ArrayList<>();
        switch (modelo) {
            case "CTL0104B":
                setCTL0104BData(entriesList);
                break;
            case "CTL0104A":
            default:
                setCTL0104AData(entriesList);
                break;
        }
    }

    private void setCTL0104BData(List entriesList) {
        CTL0104B_TermoparLogEntry CTL0104BTermoparLogEntry;
        for (int z = 0; z < 7; z++) {
            ArrayList<Entry> values = new ArrayList<>();

            for (int i = 0; i < entriesList.size(); i++) {
                CTL0104BTermoparLogEntry = (CTL0104B_TermoparLogEntry) entriesList.get(i);
                String entryHour = CTL0104BTermoparLogEntry.getHora();
                String entryData = CTL0104BTermoparLogEntry.getData();
                try {
                    if (!entryHour.contains("OVUV") && !entryData.contains("OPEN")) {
                        int posicaoTermopar = z + 1;
                        if (posicaoTermopar < 5) {
                            String entryT = (String) CTL0104BTermoparLogEntry.getClass().getMethod("getT" + posicaoTermopar).invoke(CTL0104BTermoparLogEntry);
                            float entryTAsFloat;
                            if (entryT != null) {
                                if (entryT.contains("OVUV") || entryT.contains("OPEN")) {
                                    entryTAsFloat = VALOR_DISCREPANTE;
                                } else {
                                    entryTAsFloat = Float.parseFloat(entryT);
                                }
                                values.add(new Entry(i, entryTAsFloat));
                            }
                        } else {
                            float entryMAsFloat;
                            String entryM = (String) CTL0104BTermoparLogEntry.getClass().getMethod("getM" + posicaoTermopar).invoke(CTL0104BTermoparLogEntry);
                            if (entryM != null) {
                                if (entryM.contains("OVUV") || entryM.contains("OPEN")) {
                                    entryMAsFloat = VALOR_DISCREPANTE;
                                } else {
                                    entryMAsFloat = Float.parseFloat(entryM);
                                }
                            } else {
                                entryMAsFloat = VALOR_DISCREPANTE;
                            }
                            values.add(new Entry(i, entryMAsFloat));
                        }
                    }
                } catch (NumberFormatException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
//            , getResources().getDrawable(R.drawable.star)));
            LineDataSet d;
            switch (z) {
                case 0:
                    d = new LineDataSet(values, "T1");
                    d.setColor(getResources().getColor(R.color.colorPrimary));
                    break;
                case 1:
                    d = new LineDataSet(values, "T2");
                    d.setColor(YELLOW);
                    break;
                case 2:
                    d = new LineDataSet(values, "T3");
                    d.setColor(RED);
                    break;
                case 3:
                    d = new LineDataSet(values, "T4");
                    d.setColor(GREEN);
                    break;
                case 4:
                    d = new LineDataSet(values, "M5");
                    d.setColor(MAGENTA);
                    break;
                case 5:
                    d = new LineDataSet(values, "M6");
                    d.setColor(CYAN);
                    break;
                case 6:
                    d = new LineDataSet(values, "M7");
                    d.setColor(GRAY);
                    break;
                default:
                    Log.e(TAG_LOG, "Z não cadastrado");
                    Toast.makeText(this, "Ocorreu um erro ao recuperar a medição!", Toast.LENGTH_SHORT).show();
                    d = new LineDataSet(values, "M7");
                    d.setColor(GRAY);
                    finish();
                    break;
            }

            d.enableDashedLine(10, 10, 0);
            d.setDrawIcons(false);
            // draw dashed line
            d.enableDashedLine(15f, 0f, 1f);

            d.setCircleColor(RED);
            // line thickness and point size
            d.setLineWidth(2f);
            d.setCircleRadius(3f);
            // draw points as solid circles
            d.setDrawCircleHole(true);
            // customize legend entry
            d.setFormLineWidth(1f);
            d.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
            d.setFormSize(40.f);
            // text size of values
            d.setValueTextSize(10f);
            // draw selection line as dashed
            d.enableDashedHighlightLine(10f, 5f, 0f);
            // set the filled area
            d.setDrawFilled(false);
            d.setFillFormatter((dataSet, dataProvider) -> chart.getAxisLeft().getAxisMinimum());
            allDataSets.add(d);
        }
        allData = new LineData(allDataSets);
        chart.setData(allData);
        chart.setAutoScaleMinMaxEnabled(true);
        chart.invalidate();
        chart.getData().notifyDataChanged();
        chart.notifyDataSetChanged();
    }

    private void setCTL0104AData(List entriesList) {
        CTL0104A_TermoparLogEntry CTL0104ATermoparLogEntry;
        for (int z = 0; z < 4; z++) {
            ArrayList<Entry> values = new ArrayList<>();

            for (int i = 0; i < entriesList.size(); i++) {
                CTL0104ATermoparLogEntry = (CTL0104A_TermoparLogEntry) entriesList.get(i);
                String entryHour = CTL0104ATermoparLogEntry.getHora();
                String entryData = CTL0104ATermoparLogEntry.getData();
                try {
                    if (!entryHour.contains("OVUV") && !entryData.contains("OPEN")) {
                        int posicaoTermopar = z + 1;
                        String entryT = (String) CTL0104ATermoparLogEntry.getClass().getMethod("getT" + posicaoTermopar).invoke(CTL0104ATermoparLogEntry);
                        float entryTAsFloat;
                        if (entryT != null) {
                            if (entryT.contains("OVUV") || entryT.contains("OPEN")) {
                                entryTAsFloat = VALOR_DISCREPANTE;
                            } else {
                                entryTAsFloat = Float.parseFloat(entryT);
                            }
                        } else {
                            entryTAsFloat = VALOR_DISCREPANTE;
                        }
                        values.add(new Entry(i, entryTAsFloat));
                    }
                } catch (NumberFormatException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
//            , getResources().getDrawable(R.drawable.star)));

            LineDataSet d = new LineDataSet(values, "T" + (z + 1));

            if (z == 0) {
                d.setColor(getResources().getColor(R.color.colorPrimary));
            } else if (z == 1) {
                d.setColor(YELLOW);
            } else if (z == 2) {
                d.setColor(RED);
            } else {
                d.setColor(GREEN);
            }

            d.enableDashedLine(10, 10, 0);
            d.setDrawIcons(false);
            // draw dashed line
            d.enableDashedLine(15f, 0f, 1f);

            d.setCircleColor(RED);
            // line thickness and point size
            d.setLineWidth(2f);
            d.setCircleRadius(3f);
            // draw points as solid circles
            d.setDrawCircleHole(true);
            // customize legend entry
            d.setFormLineWidth(1f);
            d.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
            d.setFormSize(40.f);
            // text size of values
            d.setValueTextSize(10f);
            // draw selection line as dashed
            d.enableDashedHighlightLine(10f, 5f, 0f);
            // set the filled area
            d.setDrawFilled(false);
            d.setFillFormatter((dataSet, dataProvider) -> chart.getAxisLeft().getAxisMinimum());
            allDataSets.add(d);
        }
        allData = new LineData(allDataSets);
        chart.setData(allData);
        chart.setAutoScaleMinMaxEnabled(true);
        chart.invalidate();
        chart.getData().notifyDataChanged();
        chart.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.line, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return true;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

//        tvX.setText(String.valueOf(seekBarX.getProgress()));
//        tvY.setText(String.valueOf(seekBarY.getProgress()));
//
//        setData(seekBarX.getProgress(), seekBarY.getProgress());

        // redraw
        chart.invalidate();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        Log.i("Entry selected", e.toString());
        Log.i("LOW HIGH", "low: " + chart.getLowestVisibleX() + ", high: " + chart.getHighestVisibleX());
        Log.i("MIN MAX", "xMin: " + chart.getXChartMin() + ", xMax: " + chart.getXChartMax() + ", yMin: " + chart.getYChartMin() + ", yMax: " + chart.getYChartMax());
    }

    @Override
    public void onNothingSelected() {
        Log.i("Nothing selected", "Nothing selected.");
    }

    public void resetZoom(View view) {
        finish();
        overridePendingTransition(0, 0);
        startActivity(getIntent());
    }

    public void blockX(View v) {
        if (chart.isScaleXEnabled()) {
            ((Button) v).setText(R.string.destravar_x);
            chart.setScaleXEnabled(false);
            v.setBackgroundColor(getResources().getColor(R.color.cinzaClaro));
            ((Button) v).setTextColor(getResources().getColor(R.color.cinzaEscuro));
        } else {
            ((Button) v).setText(R.string.travar_eixo_x);
            chart.setScaleXEnabled(true);
            v.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            ((Button) v).setTextColor(getResources().getColor(R.color.branco));
        }

    }

    public void blockY(View v) {
        if (chart.isScaleYEnabled()) {
            ((Button) v).setText(R.string.destravar_y);
            chart.setScaleYEnabled(false);
            v.setBackgroundColor(getResources().getColor(R.color.cinzaClaro));
            ((Button) v).setTextColor(getResources().getColor(R.color.cinzaEscuro));
        } else {
            ((Button) v).setText(R.string.travar_eixo_y);
            chart.setScaleYEnabled(true);
            v.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            ((Button) v).setTextColor(getResources().getColor(R.color.branco));
        }
    }
}