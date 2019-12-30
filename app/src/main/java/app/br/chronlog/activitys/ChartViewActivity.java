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
import java.util.Arrays;
import java.util.List;

import app.br.chronlog.R;
import app.br.chronlog.utils.MyMarkerView;
import app.br.chronlog.utils.TermoparLog;
import app.br.chronlog.utils.TermoparLogEntry;

import static android.graphics.Color.GREEN;
import static android.graphics.Color.RED;
import static android.graphics.Color.YELLOW;
import static app.br.chronlog.utils.Utils.TAG_LOG;

public class ChartViewActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener,
        OnChartValueSelectedListener {

    private LineChart chart;
    private ArrayList<Parcelable> selectedLog;
    private Button btnT1, btnT2, btnT3, btnT4;
    private LineData allData;
    private ArrayList<ILineDataSet> allDataSets;
    private static ArrayList<ILineDataSet> myNewDataSets;

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
                TermoparLog termoparLog = (TermoparLog) selectedLog.get(0);
                entriesList = termoparLog.getEntries();
                acessaDadosDoArquivo(entriesList);
            } else {
                Toast.makeText(this, "Falhou ao resgatar os dados!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        btnT1 = findViewById(R.id.t1Button);
        btnT2 = findViewById(R.id.t2Button);
        btnT3 = findViewById(R.id.t3Button);
        btnT4 = findViewById(R.id.t4Button);

        btnT1.setOnClickListener((v) -> {
            toogleBtnPressed(v);
            toggleDataSetVisibility("T1");
        });
        btnT2.setOnClickListener((v) -> {
            toogleBtnPressed(v);
            toggleDataSetVisibility("T2");
        });
        btnT3.setOnClickListener((v) -> {
            toogleBtnPressed(v);
            toggleDataSetVisibility("T3");
        });
        btnT4.setOnClickListener((v -> {
            toogleBtnPressed(v);
            toggleDataSetVisibility("T4");
        }));
    }

    private void toogleBtnPressed(View v) {
        if (v.getId() == btnT1.getId()) {
            if (btnT1.isSelected()) {
                btnT1.setSelected(false);
            } else {
                btnT1.setSelected(true);
            }
        }
        if (v.getId() == btnT2.getId()) {
            if (btnT2.isSelected()) {
                btnT2.setSelected(false);
            } else {
                btnT2.setSelected(true);
            }
        }

        if (v.getId() == btnT3.getId()) {
            if (btnT3.isSelected()) {
                btnT3.setSelected(false);
            } else {
                btnT3.setSelected(true);
            }
        }

        if (v.getId() == btnT4.getId()) {
            if (btnT4.isSelected()) {
                btnT4.setSelected(false);
            } else {
                btnT4.setSelected(true);
            }
        }
    }

    private void toggleDataSetVisibility(String label) {
        int idx;
        switch (label) {
            case "T1":
                idx = 0;
                break;
            case "T2":
                idx = 1;
                break;
            case "T3":
                idx = 2;
                break;
            case "T4":
                idx = 3;
                break;
            default:
                idx = -1;
                break;
        }

        if (idx != -1) {
            List<ILineDataSet> sets = chart.getData().getDataSets();
            ILineDataSet dataset = sets.get(idx);

            dataset.setVisible(!dataset.isVisible());

            float minAxysYValueT1 = -1500, maxAxysYValueT1 = 1500;
            float minAxysYValueT2 = -1500, maxAxysYValueT2 = 1500;
            float minAxysYValueT3 = -1500, maxAxysYValueT3 = 1500;
            float minAxysYValueT4 = -1500, maxAxysYValueT4 = 1500;

            for (int i = 0; i < sets.size(); i++) {
                LineDataSet set = (LineDataSet) sets.get(i);
                if (set.isVisible()) {
                    float setYmin = Float.parseFloat(String.valueOf(set.getYMin()));
                    float setYmax = Float.parseFloat(String.valueOf(set.getYMax()));
                    if (i != idx) {
                        if (i == 0) {
                            minAxysYValueT1 = setYmin;
                            maxAxysYValueT1 = setYmax;
                        }
                        if (i == 1) {
                            minAxysYValueT2 = setYmin;
                            maxAxysYValueT2 = setYmax;
                        }
                        if (i == 2) {
                            minAxysYValueT3 = setYmin;
                            maxAxysYValueT3 = setYmax;
                        }
                        if (i == 3) {
                            minAxysYValueT4 = setYmin;
                            maxAxysYValueT4 = setYmax;
                        }
                    }
                }
            }

            float[] myArrayMin = new float[]{minAxysYValueT1, minAxysYValueT2, minAxysYValueT3, minAxysYValueT4};
            float[] myArrayMax = new float[]{maxAxysYValueT1, maxAxysYValueT2, maxAxysYValueT3, maxAxysYValueT4};
            Arrays.sort(myArrayMin);
            Arrays.sort(myArrayMax);

            chart.getAxisLeft().setAxisMinimum(myArrayMin[0]);
            chart.getAxisLeft().setAxisMaximum(myArrayMax[myArrayMax.length - 1]);

//            chart.getAxisLeft().resetAxisMinimum();
//            chart.getAxisLeft().resetAxisMaximum();
            chart.offsetTopAndBottom(15);  //'padding top'
            chart.invalidate();
            chart.getData().notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.animateX(1500);
        } else {
            Toast.makeText(this, "DataSet não encontrado!", Toast.LENGTH_SHORT).show();
        }
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
                horariosX[i] = ((TermoparLogEntry) entriesList.get(i)).getHora();
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
        // draw points over time
        chart.animateX(1500);

        // get the legend (only possible after setting data)
        Legend l = chart.getLegend();
        l.setTextColor(getResources().getColor(R.color.colorPrimary));
        // draw legend entries as lines
        l.setForm(Legend.LegendForm.LINE);
    }

    private void setData(List entriesList) {
        TermoparLogEntry termoparLogEntry;
        allDataSets = new ArrayList<>();

        for (int z = 0; z < 4; z++) {
            ArrayList<Entry> values = new ArrayList<>();

            for (int i = 0; i < entriesList.size(); i++) {
                termoparLogEntry = (TermoparLogEntry) entriesList.get(i);
                String entryHour = termoparLogEntry.getHora();
                String entryData = termoparLogEntry.getData();
                try {
                    if (!entryHour.contains("OVUV") && !entryData.contains("OPEN")) {
                        int posicaoTermopar = z + 1;
                        String entryT = (String) termoparLogEntry.getClass().getMethod("getT" + posicaoTermopar).invoke(termoparLogEntry);
                        float entryTAsFloat;
                        if (entryT != null) {
                            if (entryT.contains("OVUV") || entryT.contains("OPEN")) {
                                entryTAsFloat = 999f;
                            } else {
                                entryTAsFloat = Float.parseFloat(entryT);
                            }
                        } else {
                            entryTAsFloat = 999f;
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
        if (chart != null) {
            btnT4.setSelected(false);
            btnT3.setSelected(false);
            btnT2.setSelected(false);
            btnT1.setSelected(false);

            chart.setData(allData);
            chart.invalidate();
            chart.getData().notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.animateX(1500);
            chart.resetZoom();
            chart.fitScreen();
        }
    }

    public void blockX(View v) {
        if (chart.isScaleXEnabled()) {
            ((Button) v).setText(R.string.destravar_x);
            chart.setScaleXEnabled(false);
        } else {
            ((Button) v).setText(R.string.travar_eixo_x);
            chart.setScaleXEnabled(true);
        }
    }

    public void blockY(View v) {
        if (chart.isScaleYEnabled()) {
            ((Button) v).setText(R.string.destravar_y);
            chart.setScaleYEnabled(false);
        } else {
            ((Button) v).setText(R.string.travar_eixo_y);
            chart.setScaleYEnabled(true);
        }
    }
}