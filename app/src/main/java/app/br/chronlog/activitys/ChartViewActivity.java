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
import java.util.List;

import app.br.chronlog.R;
import app.br.chronlog.utils.MyLog;
import app.br.chronlog.utils.MyLogEntry;
import app.br.chronlog.utils.MyMarkerView;

import static android.graphics.Color.GREEN;
import static android.graphics.Color.RED;
import static android.graphics.Color.YELLOW;
import static app.br.chronlog.utils.Utils.TAG_LOG;

public class ChartViewActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener,
        OnChartValueSelectedListener {

    private LineChart chart;
    private ArrayList<Parcelable> selectedLog;
    private boolean hideT1, hideT2, hideT3, hideT4;
    private Button btnT1, btnT2, btnT3, btnT4;

//    private SeekBar seekBarX, seekBarY;
//    private TextView tvX, tvY;

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
                MyLog myLog = (MyLog) selectedLog.get(0);
                entriesList = myLog.getEntries();
                acessaDadosDoArquivo(entriesList);
            } else {
                Toast.makeText(this, "Falhou ao resgatar os dados!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        findViewById(R.id.t1Button).setOnClickListener((v) -> {
            hideT1 = true;
            chart.invalidate();
        });
        findViewById(R.id.t2Button).setOnClickListener((v) -> {
            hideT2 = true;
            chart.invalidate();
        });
        findViewById(R.id.t3Button).setOnClickListener((v) -> {
            hideT3 = true;
            chart.invalidate();
        });
        findViewById(R.id.t4Button).setOnClickListener((v -> {
            hideT4 = true;
            chart.invalidate();
        }));
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

            // enable touch gestures
            chart.setTouchEnabled(true);

            // set listeners
            chart.setOnChartValueSelectedListener(this);
            chart.setDrawGridBackground(false);

            // create marker to display box when values are selected
            MyMarkerView mv = new MyMarkerView(this, R.layout.custom_marker_view);

            // Set the marker to the chart
            mv.setChartView(chart);
            chart.setMarker(mv);

            // enable scaling and dragging
            chart.setDragEnabled(true);
            chart.setScaleEnabled(true);
            // chart.setScaleXEnabled(true);
            // chart.setScaleYEnabled(true);

            // force pinch zoom along both axis
            chart.setPinchZoom(true);
        }

        XAxis xAxis;
        String[] horariosX;
        {
            xAxis = chart.getXAxis();
            horariosX = new String[entriesList.size()];
            // the labels that should be drawn on the XAxis
            for (int i = 0; i < entriesList.size(); i++) {
                horariosX[i] = ((MyLogEntry) entriesList.get(i)).getHora();
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
            xAxis.enableGridDashedLine(10f, 10f, 0f);
        }
        YAxis yAxis;
        {   // // Y-Axis Style // //
            yAxis = chart.getAxisLeft();

            // disable dual axis (only use LEFT axis)
            chart.getAxisRight().setEnabled(false);

            // horizontal grid lines
            yAxis.enableGridDashedLine(10f, 10f, 0f);
        }

        setData(entriesList);
        // draw points over time
        chart.animateXY(1500, 1500);

        // get the legend (only possible after setting data)
        Legend l = chart.getLegend();
        l.setTextColor(getResources().getColor(R.color.colorPrimary));
        // draw legend entries as lines
        l.setForm(Legend.LegendForm.LINE);
    }

    private void setData(List entriesList) {
        MyLogEntry myLogEntry;
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();

        for (int z = 0; z < 4; z++) {
            ArrayList<Entry> values = new ArrayList<>();

            for (int i = 0; i < entriesList.size(); i++) {
                myLogEntry = (MyLogEntry) entriesList.get(i);
                String entryHour = myLogEntry.getHora();
                String entryData = myLogEntry.getData();
                try {
                    if (!entryHour.contains("OVUV") && !entryData.contains("OPEN")) {
                        int posicaoTermopar = z + 1;
                        String entryT = (String) myLogEntry.getClass().getMethod("getT" + posicaoTermopar).invoke(myLogEntry);
                        float entryTAsFloat = 50f;
                        if (entryT != null) {
                            if (entryT.contains("OVUV") || entryT.contains("OPEN")) {
                                entryTAsFloat = 50f;
                            } else {
                                entryTAsFloat = Float.parseFloat(entryT);
                            }
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
            d.setFormSize(15.f);
            // text size of values
            d.setValueTextSize(8f);
            // draw selection line as dashed
            d.enableDashedHighlightLine(10f, 5f, 0f);
            // set the filled area
            d.setDrawFilled(false);
            d.setFillFormatter((dataSet, dataProvider) -> chart.getAxisLeft().getAxisMinimum());

            dataSets.add(d);
        }

        if (chart.getData() != null && chart.getData().getDataSetCount() > 0) {
            // make the first DataSet dashed
            if (!hideT1)
                ((LineDataSet) dataSets.get(0)).enableDashedLine(10, 10, 0);
            if (!hideT2)
                ((LineDataSet) dataSets.get(1)).enableDashedLine(10, 10, 0);
            if (!hideT3)
                ((LineDataSet) dataSets.get(2)).enableDashedLine(10, 10, 0);
            if (!hideT4)
                ((LineDataSet) dataSets.get(3)).enableDashedLine(10, 10, 0);
        }


        LineData data = new LineData(dataSets);
        chart.setData(data);
        chart.invalidate();
        chart.getData().notifyDataChanged();
        chart.notifyDataSetChanged();

    }
//    private void setData(int count, float range) {
//
//        ArrayList<Entry> values = new ArrayList<>();
//
//        for (int i = 0; i < count; i++) {
//            float val = (float) (Math.random() * range) - 30;
//            values.add(new Entry(i, val, getResources().getDrawable(R.drawable.star)));
//        }
//
//        LineDataSet set1;
//
//        if (chart.getData() != null && chart.getData().getDataSetCount() > 0) {
//            set1 = (LineDataSet) chart.getData().getDataSetByIndex(0);
//            set1.setValues(values);
//

//        } else {
//            // create a dataset and give it a type
//            set1 = new LineDataSet(values, "Medição de Temperatura");
//            set1.setDrawIcons(false);
//            // draw dashed line
//            set1.enableDashedLine(15f, 0f, 1f);
//            // black lines and points
//            set1.setColor(getResources().getColor(R.color.azulClaro));
//            set1.setCircleColor(Color.RED);
//            // line thickness and point size
//            set1.setLineWidth(1f);
//            set1.setCircleRadius(3f);
//            // draw points as solid circles
//            set1.setDrawCircleHole(true);
//            // customize legend entry
//            set1.setFormLineWidth(1f);
//            set1.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
//            set1.setFormSize(15.f);
//            // text size of values
//            set1.setValueTextSize(8f);
//            // draw selection line as dashed
//            set1.enableDashedHighlightLine(10f, 5f, 0f);
//            // set the filled area
//            set1.setDrawFilled(false);
//            set1.setFillFormatter((dataSet, dataProvider) -> chart.getAxisLeft().getAxisMinimum());
//            // set color of filled area
////            if (Utils.getSDKInt() >= 18) {
////                // drawables only supported on api level 18 and above
////                Drawable drawable = ContextCompat.getDrawable(this, R.drawable.fade_red);
////                set1.setFillDrawable(drawable);
////            } else {
////                set1.setFillColor(Color.BLACK);
////            }
//
//            ArrayList<ILineDataSet> dataSets = new ArrayList<>();
//            dataSets.add(set1); // add the data sets
//
//            // create a data object with the data sets
//            LineData data = new LineData(dataSets);
//
//            // set data
//            chart.setData(data);
//        }
//    }

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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
//        //seta botao positivo
//        DialogInterface.OnClickListener positiveListener = (dialog, which) -> finishAffinity();
//        DialogInterface.OnClickListener negativeListener = (dialog, which) -> {
//            getSharedPreferences(CONFIG_FILE, Context.MODE_PRIVATE).edit().putBoolean("aparelho_verificado", false).apply();
//            finishAffinity();
//        };
//
//        //cria dialogo
//        AlertDialog alert = createDialog(this, "Alerta!", "Deseja salvar device para acesso mais rapidamente?", "SALVAR", "NÃO AGORA", true, true, negativeListener, positiveListener, dialog -> {
//        });
//        alert.show();

    }

    public void resetZoom(View view) {
        if (chart != null) {
            chart.animateXY(500, 500);
            chart.resetZoom();
            chart.fitScreen();
        }
    }

    public void blockX(View v) {
        if (chart.isScaleXEnabled()) {
            ((Button) v).setText(R.string.unblock_x);
            chart.setScaleXEnabled(false);
        } else {
            ((Button) v).setText(R.string.travar_eixo_x);
            chart.setScaleXEnabled(true);
        }
    }

    public void blockY(View v) {
        if (chart.isScaleYEnabled()) {
            ((Button) v).setText(R.string.unblock_y);
            chart.setScaleYEnabled(false);
        } else {
            ((Button) v).setText(R.string.travar_eixo_y);
            chart.setScaleYEnabled(true);
        }
    }

//
//    public class MyFormatter extends ValueFormatter {
//        private final String[] xLabels;
//
//        public MyFormatter(String[] horariosX) {
//            this.xLabels = horariosX;
//        }
//    }
}