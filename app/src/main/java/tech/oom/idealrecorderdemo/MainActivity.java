package tech.oom.idealrecorderdemo;

import android.content.DialogInterface;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;
import com.yanzhenjie.permission.PermissionListener;
import com.yanzhenjie.permission.Rationale;
import com.yanzhenjie.permission.RationaleListener;

import java.io.File;
import java.util.List;

import jaygoo.widget.wlv.WaveLineView;
import tech.oom.idealrecorder.IdealRecorder;
import tech.oom.idealrecorder.StatusListener;
import tech.oom.idealrecorder.utils.Log;
import tech.oom.idealrecorderdemo.widget.WaveView;

public class MainActivity extends AppCompatActivity {

    private final int maxSaveAudioNum = 10;
    private int count;
    private int saveAudioCount;
    private boolean isRecording;
    private Button recordBtn;
    private WaveView waveView;
    private WaveLineView waveLineView;
    private TextView tips;

    private IdealRecorder idealRecorder;

    private IdealRecorder.RecordConfig recordConfig;

    private final RationaleListener rationaleListener = new RationaleListener() {
        @Override
        public void showRequestPermissionRationale(int requestCode, final Rationale rationale) {
            com.yanzhenjie.alertdialog.AlertDialog.newBuilder(MainActivity.this)
                    .setTitle("友好提醒")
                    .setMessage("录制声音保存录音需要录音和读取文件相关权限哦，爱给不给")
                    .setPositiveButton("好，给你", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            rationale.resume();
                        }
                    }).setNegativeButton("我是拒绝的", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            rationale.cancel();
                        }
                    }).create().show();
        }
    };


    private final StatusListener statusListener = new StatusListener() {
        @Override
        public void onStartRecording() {
            waveLineView.startAnim();
            tips.setText("开始录音");
        }

        @Override
        public void onRecordData(short[] data, int length) {

            for (int i = 0; i < length; i += 60) {
                waveView.addData(data[i]);
            }
            for (short signal: data)
                System.out.print(signal + " ");
            System.out.println();
            synchronized (this) {
                ++count;
                Log.d("MainActivity", "current buffer size -> " + length + ", count -> " + count);
            }
        }

        @Override
        public void onVoiceVolume(int volume) {
            double myVolume = (volume - 40) * 4;
            waveLineView.setVolume((int) myVolume);
            Log.d("MainActivity", "current volume is " + volume);
        }

        @Override
        public void onRecordError(int code, String errorMsg) {
            tips.setText("录音错误" + errorMsg);
        }

        @Override
        public void onFileSaveFailed(String error) {
            Toast.makeText(MainActivity.this, "文件保存失败", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFileSaveSuccess(String fileUri) {
            Toast.makeText(MainActivity.this, "文件保存成功,路径是" + fileUri, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onStopRecording() {
            tips.setText("AAAAAAAAAAAAAAAAAAdsgsifdgjoivasdnogivaisdbopvifandpbinpianefdibniofenioneoinbeorinoienrnernesfgwhfsbdoaijdadhgsibu lfnailudsvbnaiugsidovbnsdfigwbargsoibiwrgiuabgriuebiruebgiurbiurebiubreigbreibgrebgrebguirebigrbegregrugrbgrireggbgbgrugrbegbugerbgeriegrigrbgber");
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
            // 权限申请失败回调。
            if (requestCode == 100) {
                Toast.makeText(MainActivity.this, "没有录音和文件读取权限，你自己看着办", Toast.LENGTH_SHORT).show();
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
        count = 0;
        saveAudioCount = 0;
        recordBtn = (Button) findViewById(R.id.register_record_btn);
        waveView = (WaveView) findViewById(R.id.wave_view);
        waveLineView = (WaveLineView) findViewById(R.id.waveLineView);
        tips = (TextView) findViewById(R.id.tips);
        idealRecorder = IdealRecorder.getInstance();

        tips.setMovementMethod(new ScrollingMovementMethod());
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        isRecording = false;

        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                synchronized (this) {
                    if (!isRecording) {
                        readyRecord();
                        recordBtn.setText("正在监听");
                        recordBtn.setBackground(getResources().getDrawable(R.drawable.recorder_btn_r));
                    } else {
                        stopRecord();
                        recordBtn.setText("启动识别");
                        recordBtn.setBackground(getResources().getDrawable(R.drawable.recorder_btn));
                    }
                    isRecording = !isRecording;
                }
            }
        });

//        recordBtn.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View v) {
//                readyRecord();
//                recordBtn.setText("开始录音");
//                return true;
//            }
//        });
//        recordBtn.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                int action = event.getAction();
//                switch (action) {
//                    case MotionEvent.ACTION_UP:
//                        stopRecord();
//                        recordBtn.setText("按住说话");
//                        return false;
//
//                }
//                return false;
//            }
//        });
        recordConfig = new IdealRecorder.RecordConfig(MediaRecorder.AudioSource.MIC, 48000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

    }

    /**
     * 准备录音 录音之前 先判断是否有相关权限
     */
    private void readyRecord() {

        AndPermission.with(this)
                .requestCode(100)
                .permission(Permission.MICROPHONE, Permission.STORAGE)
                .rationale(rationaleListener).callback(permissionListener).start();

    }

    /**
     * 开始录音
     */
    private void record() {
        //如果需要保存录音文件  设置好保存路径就会自动保存  也可以通过onRecordData 回调自己保存  不设置 不会保存录音
        idealRecorder.setRecordFilePath(getSaveFilePath());
//        idealRecorder.setWavFormat(false);
        //设置录音配置 最长录音时长 以及音量回调的时间间隔
        idealRecorder.setRecordConfig(recordConfig).setMaxRecordTime(20000).setVolumeInterval(200);
        //设置录音时各种状态的监听
        idealRecorder.setStatusListener(statusListener);
        idealRecorder.start(); //开始录音

    }

    /**
     * 获取文件保存路径
     *
     * @return
     */
    private String getSaveFilePath() {
        File file = new File(Environment.getExternalStorageDirectory(), "Audio");
        if (!file.exists()) {
            file.mkdirs();
        }
        this.saveAudioCount = (this.saveAudioCount + 1) % this.maxSaveAudioNum;
        File wavFile = new File(file, "signal-" + this.saveAudioCount + ".wav");
        String savePath = wavFile.getAbsolutePath();
        Log.d("getSaveFilePath", "saveFilePath -> " + savePath);
        return savePath;
    }


    /**
     * 停止录音
     */
    private void stopRecord() {
        //停止录音
        idealRecorder.stop();
    }
}