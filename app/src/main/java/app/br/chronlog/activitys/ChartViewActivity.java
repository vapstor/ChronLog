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

import java.util.ArrayList;
import java.util.List;

import app.br.chronlog.R;
import app.br.chronlog.utils.MyLog;
import app.br.chronlog.utils.MyLogEntry;
import app.br.chronlog.utils.MyMarkerView;

import static app.br.chronlog.utils.Utils.TAG_LOG;

public class ChartViewActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener,
        OnChartValueSelectedListener {

    private LineChart chart;
    private ArrayList<Parcelable> selectedLog;
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

            ////            // axis range
//            yAxis.setAxisMaximum(50f);
//            yAxis.setAxisMinimum(-50f);
        }
        //        {   // // Create Limit Lines // //
//            LimitLine llXAxis = new LimitLine(9f, "Index 10");
//            llXAxis.setLineWidth(4f);
//            llXAxis.enableDashedLine(10f, 10f, 0f);
//            llXAxis.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
//            llXAxis.setTextSize(10f);
//            //llXAxis.setTypeface(tfRegular);
//
//            LimitLine ll1 = new LimitLine(150f, "Upper Limit");
//            ll1.setLineWidth(4f);
//            ll1.enableDashedLine(10f, 10f, 0f);
//            ll1.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
//            ll1.setTextSize(10f);
//            //ll1.setTypeface(tfRegular);
//
//            LimitLine ll2 = new LimitLine(-30f, "Lower Limit");
//            ll2.setLineWidth(4f);
//            ll2.enableDashedLine(10f, 10f, 0f);
//            ll2.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
//            ll2.setTextSize(10f);
//            //ll2.setTypeface(tfRegular);
//
//            // draw limit lines behind data instead of on top
//            yAxis.setDrawLimitLinesBehindData(true);
//            xAxis.setDrawLimitLinesBehindData(true);
//
//            // add limit lines
//            yAxis.addLimitLine(ll1);
//            yAxis.addLimitLine(ll2);
//            //xAxis.addLimitLine(llXAxis);
//        }

        // add data
//        setData(45, 180);
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
        MyLogEntry myLogEntry;
        ArrayList<Entry> valuesT1 = new ArrayList<>();
        for (int i = 0; i < entriesList.size(); i++) {
            myLogEntry = (MyLogEntry) entriesList.get(i);

            String entryHour = myLogEntry.getHora();
            String entryData = myLogEntry.getData();
            try {
                if (!entryHour.contains("OVUV") && !entryData.contains("OPEN")) {
                    float entryHourAsFloat = Float.parseFloat(entryHour.replace(":", "")) / 60000;
                    String entryT1 = myLogEntry.getT1();
                    float entryT1AsFloat;
                    if (entryT1.contains("OVUV")) {
                        entryT1AsFloat = 500f;
                    } else if (entryT1.contains("OPEN")) {
                        entryT1AsFloat = 500f;
                    } else {
                        entryT1AsFloat = Float.parseFloat(entryT1);
                    }
                    Entry entry = new Entry(i, entryT1AsFloat);
                    Log.d(TAG_LOG, "ENTRY NO I -> " + i);
                    valuesT1.add(entry);
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
                Toast.makeText(this, "Existem erros no Log!", Toast.LENGTH_SHORT).show();
            }
//            , getResources().getDrawable(R.drawable.star)));
        }

        LineDataSet set1;

        if (chart.getData() != null && chart.getData().getDataSetCount() > 0) {
            set1 = (LineDataSet) chart.getData().getDataSetByIndex(0);
            set1.setValues(valuesT1);
            set1.notifyDataSetChanged();
            chart.getData().notifyDataChanged();
            chart.notifyDataSetChanged();
        } else {

            // create a dataset and give it a type
            set1 = new LineDataSet(valuesT1, "Medição de Temperatura");

            set1.setDrawIcons(false);
            // draw dashed line
            set1.enableDashedLine(15f, 0f, 1f);
            // black lines and points
            set1.setColor(getResources().getColor(R.color.azulClaro));
            set1.setCircleColor(Color.RED);
            // line thickness and point size
            set1.setLineWidth(1f);
            set1.setCircleRadius(3f);
            // draw points as solid circles
            set1.setDrawCircleHole(true);
            // customize legend entry
            set1.setFormLineWidth(1f);
            set1.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
            set1.setFormSize(15.f);
            // text size of values
            set1.setValueTextSize(8f);
            // draw selection line as dashed
            set1.enableDashedHighlightLine(10f, 5f, 0f);
            // set the filled area
            set1.setDrawFilled(false);
            set1.setFillFormatter((dataSet, dataProvider) -> chart.getAxisLeft().getAxisMinimum());
            // set color of filled area
//            if (Utils.getSDKInt() >= 18) {
//                // drawables only supported on api level 18 and above
//                Drawable drawable = ContextCompat.getDrawable(this, R.drawable.fade_red);
//                set1.setFillDrawable(drawable);
//            } else {
//                set1.setFillColor(Color.BLACK);
//            }

            ArrayList<ILineDataSet> dataSets = new ArrayList<>();
            dataSets.add(set1); // add the data sets

            // create a data object with the data sets
            LineData data = new LineData(dataSets);

            // set data
            chart.setData(data);
        }
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
//            set1.notifyDataSetChanged();
//            chart.getData().notifyDataChanged();
//            chart.notifyDataSetChanged();
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
            ((Button) v).setText(R.string.block_x);
            chart.setScaleXEnabled(true);
        }
    }

    public void blockY(View v) {
        if (chart.isScaleYEnabled()) {
            ((Button) v).setText(R.string.unblock_y);
            chart.setScaleYEnabled(false);
        } else {
            ((Button) v).setText(R.string.block_y);
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