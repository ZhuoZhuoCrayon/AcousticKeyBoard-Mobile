package tech.oom.idealrecorderdemo;

import android.content.DialogInterface;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;
import com.yanzhenjie.permission.PermissionListener;
import com.yanzhenjie.permission.Rationale;
import com.yanzhenjie.permission.RationaleListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jaygoo.widget.wlv.WaveLineView;
import okhttp3.*;
import tech.oom.idealrecorder.IdealRecorder;
import tech.oom.idealrecorder.StatusListener;
import tech.oom.idealrecorder.utils.Log;
import tech.oom.idealrecorderdemo.ak.AkApi;
import tech.oom.idealrecorderdemo.ak.AkClient;
import tech.oom.idealrecorderdemo.dto.AkResBase;
import tech.oom.idealrecorderdemo.dto.ModelInstDebugDto;
import tech.oom.idealrecorderdemo.widget.WaveView;

public class MainActivity extends AppCompatActivity {

    private static final boolean saveAudio = false;
    private int saveAudioCount;
    private boolean isRecording;

    private Button recordBtn;
    private WaveView waveView;
    private WaveLineView waveLineView;
    private TextView tips;

    private IdealRecorder idealRecorder;

    private IdealRecorder.RecordConfig recordConfig;


    // shortcut data
    private short[] fragmentationData;

    private List<Integer> signalQueue;

    private ArrayList<Integer> lastSignal;

    private Runnable showSignalRunnable = new Runnable() {

        @Override
        public void run() {
            for (int i = 0; i < fragmentationData.length; i += 20) {
                waveView.addData(fragmentationData[i]);
            }
        }
    };

    // ????????????
    private Button redressConfirmBtn;
    private TextView redressEditText;

    // DEBUG
    private Button debugBtn;

    private final RationaleListener rationaleListener = new RationaleListener() {
        @Override
        public void showRequestPermissionRationale(int requestCode, final Rationale rationale) {
            com.yanzhenjie.alertdialog.AlertDialog.newBuilder(MainActivity.this)
                    .setTitle("????????????")
                    .setMessage("?????????????????????????????????????????????????????????????????????????????????")
                    .setPositiveButton("????????????", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            rationale.resume();
                        }
                    }).setNegativeButton("???????????????", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            rationale.cancel();
                        }
                    }).create().show();
        }
    };

    private long sum(List<Integer> array) {
        long sum = 0;
        for (int item: array) {
            sum += item;
        }
        return sum;
    }


    private final StatusListener statusListener = new StatusListener() {
        @Override
        public void onStartRecording() {
            waveLineView.startAnim();
            tips.setText("????????????");
        }

        @Override
        public void onRecordData(short[] data, int length) {

            int delay = 4800 * 2;

            fragmentationData = data;

            // ???????????????
            new Thread(showSignalRunnable).start();

            synchronized (signalQueue) {
                for (int vp: data) signalQueue.add(vp);
                Log.d("MainActivity", "extend data -> " + signalQueue.size());

                if (signalQueue.size() >= delay) {
                    int maxValue = Collections.max(signalQueue);
                    if (maxValue > 8000) {
                        int maxIndex = signalQueue.lastIndexOf(maxValue);
                        if(maxIndex - 450 >= 0 && maxIndex + 1800 <= signalQueue.size()){
                            String label = redressEditText.getText().toString();
                            ArrayList<Integer> hitSignal = new ArrayList<>(signalQueue.subList(maxIndex - 450, maxIndex + 1800));

                            if (lastSignal == null || sum(hitSignal) != sum(lastSignal)) {
                                Toast.makeText(
                                        MainActivity.this,
                                        "??????????????????: max -> " + maxValue + "label -> " + label,
                                        Toast.LENGTH_SHORT
                                ).show();
                                ModelInstDebugDto modelInstDebugDto = new ModelInstDebugDto(10, "blstm", label, hitSignal);
                                AkClient.post(AkApi.modelInstDebug, modelInstDebugDto, modelInstDebugHandler(true));

                                lastSignal = (ArrayList<Integer>) hitSignal.clone();
                            }
                        }
                    }
                }
                int maxWid = delay + 4800;
                if (signalQueue.size() > maxWid) {
                    signalQueue = signalQueue.subList(signalQueue.size() - maxWid, signalQueue.size());
                }
            }
            Log.d("MainActivity", "signalQueue length -> " + signalQueue.size());
        }

        @Override
        public void onVoiceVolume(int volume) {
            double myVolume = (volume - 40) * 4;
            waveLineView.setVolume((int) myVolume);
            Log.d("MainActivity", "current volume is " + volume);
        }

        @Override
        public void onRecordError(int code, String errorMsg) {
            tips.setText("????????????" + errorMsg);
        }

        @Override
        public void onFileSaveFailed(String error) {
            Toast.makeText(MainActivity.this, "??????????????????", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFileSaveSuccess(String fileUri) {
            Toast.makeText(MainActivity.this, "??????????????????,?????????" + fileUri, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onStopRecording() {
            tips.setText("????????????");
            recordBtn.setText("????????????");
            recordBtn.setBackground(getResources().getDrawable(R.drawable.recorder_btn));
            waveLineView.stopAnim();
        }
    };


    private final PermissionListener permissionListener = new PermissionListener() {
        @Override
        public void onSucceed(int requestCode, List<String> grantedPermissions) {

            if (requestCode == 100) {
                record();
            }
        }

        @Override
        public void onFailed(int requestCode, List<String> deniedPermissions) {
            // ???????????????????????????
            if (requestCode == 100) {
                Toast.makeText(MainActivity.this, "??????????????????????????????????????????????????????", Toast.LENGTH_SHORT).show();
            }
            if (AndPermission.hasAlwaysDeniedPermission(MainActivity.this, deniedPermissions)) {
                AndPermission.defaultSettingDialog(MainActivity.this, 300).show();
            }
        }

    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recordBtn = (Button) findViewById(R.id.register_record_btn);
        waveView = (WaveView) findViewById(R.id.wave_view);
        waveLineView = (WaveLineView) findViewById(R.id.waveLineView);
        tips = (TextView) findViewById(R.id.tips);
        idealRecorder = IdealRecorder.getInstance();

        tips.setMovementMethod(new ScrollingMovementMethod());
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);


        // shortcut data
        saveAudioCount = 0;
        isRecording = false;
        fragmentationData = null;
        signalQueue = new ArrayList<>();
        lastSignal = null;

        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                synchronized (this) {
                    if (!isRecording) {
                        readyRecord();
                        recordBtn.setText("????????????");
                        recordBtn.setBackground(getResources().getDrawable(R.drawable.recorder_btn_r));
                    } else {
                        recordBtn.setText("????????????");
                        recordBtn.setBackground(getResources().getDrawable(R.drawable.recorder_btn));
                        stopRecord();
                    }
                    isRecording = !isRecording;
                }
            }
        });

        // ????????????
        redressConfirmBtn = (Button) findViewById(R.id.redress_confirm);
        redressEditText = (TextView) findViewById(R.id.redress_edit);

        // debug
        debugBtn = (Button) findViewById(R.id.debug_btn);

        debugBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                debugBtn.setClickable(false);
                debugBtn.setBackgroundColor(Color.GRAY);

                List<Integer> testSignal = new ArrayList<>();
                String label = redressEditText.getText().toString();
                Log.d("MainActivity", "redressConfirmBtn label -> " + label);

                for (int i = 0; i < 4000; ++i) testSignal.add(i);

                ModelInstDebugDto modelInstDebugDto = new ModelInstDebugDto(10,  "blstm", label, testSignal);
                AkClient.post(AkApi.modelInstDebug, modelInstDebugDto, modelInstDebugHandler(false));
            }
        });

        recordConfig = new IdealRecorder.RecordConfig(MediaRecorder.AudioSource.MIC, 48000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

    }

    private Callback modelInstDebugHandler(final boolean isSaveSignal) {
        String tip = "debug???????????????????????????";
        if(isSaveSignal) tip = "????????????????????????";
        final String finalTip = tip;

        return new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Toast.makeText(MainActivity.this, "??????????????????", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull final Response response) throws IOException {
                if (response.isSuccessful()) {
                    Gson gson = new Gson();
                    final AkResBase responseObj = gson.fromJson(response.body().string(), AkResBase.class);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!responseObj.result) {
                                Toast.makeText(
                                        MainActivity.this,
                                        responseObj.code + ": " + responseObj.message, Toast.LENGTH_SHORT
                                ).show();
                            } else {
                                Toast.makeText(MainActivity.this, finalTip, Toast.LENGTH_SHORT).show();
                            }

                            if(!isSaveSignal) {
                                debugBtn.setBackgroundColor(0xFF03A9F4);
                                debugBtn.setClickable(true);
                            }
                        }
                    });
                }
            }
        };
    }

    /**
     * ???????????? ???????????? ??????????????????????????????
     */
    private void readyRecord() {

        AndPermission.with(this)
                .requestCode(100)
                .permission(Permission.MICROPHONE, Permission.STORAGE)
                .rationale(rationaleListener).callback(permissionListener).start();

    }

    /**
     * ????????????
     */
    private void record() {
        //??????????????????????????????  ???????????????????????????????????????  ???????????????onRecordData ??????????????????  ????????? ??????????????????
        idealRecorder.setRecordFilePath(getSaveFilePath());
//        idealRecorder.setWavFormat(false);
        //?????????????????? ?????????????????? ?????????????????????????????????
        idealRecorder.setRecordConfig(recordConfig).setMaxRecordTime(1000 * 60 * 10).setVolumeInterval(200);
        //????????????????????????????????????
        idealRecorder.setStatusListener(statusListener);
        idealRecorder.start(); //????????????

    }

    /**
     * ????????????????????????
     *
     * @return
     */
    private String getSaveFilePath() {
        if (!saveAudio) return null;

        File file = new File(Environment.getExternalStorageDirectory(), "Audio");
        if (!file.exists()) {
            file.mkdirs();
        }
        int maxSaveAudioNum = 10;
        this.saveAudioCount = (this.saveAudioCount + 1) % maxSaveAudioNum;
        File wavFile = new File(file, "signal-" + this.saveAudioCount + ".wav");
        String savePath = wavFile.getAbsolutePath();
        Log.d("getSaveFilePath", "saveFilePath -> " + savePath);
        return savePath;
    }


    /**
     * ????????????
     */
    private void stopRecord() {
        //????????????
        idealRecorder.stop();
    }
}