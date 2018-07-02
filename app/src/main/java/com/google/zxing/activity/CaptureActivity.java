package com.google.zxing.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.camera.CameraManager;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.decoding.CaptureActivityHandler;
import com.google.zxing.decoding.InactivityTimer;
import com.google.zxing.decoding.RGBLuminanceSource;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.view.ViewfinderView;
import com.joker.annotation.PermissionsDenied;
import com.joker.annotation.PermissionsGranted;
import com.joker.annotation.PermissionsRationale;
import com.joker.annotation.PermissionsRequestSync;
import com.joker.api.Permissions4M;
import com.zealens.listory.R;
import com.zealens.listory.activity.BaseActivity;
import com.zealens.listory.constant.PermissionConstants;

import java.io.IOException;
import java.util.Hashtable;


/**
 * Initial the camera
 *
 * @author Ryan.Tang
 */
@PermissionsRequestSync(permission = {Manifest.permission.CAMERA, Manifest.permission.VIBRATE},
        value = {PermissionConstants.CAMERA_CODE, PermissionConstants.VIBRATE_CODE})
public class CaptureActivity extends BaseActivity implements Callback, View.OnClickListener {
    private static final float BEEP_VOLUME = 1f;
    private static final int REQUEST_CODE_SCAN_GALLERY = 100;

    private ImageView mBackImageView;
    private CaptureActivityHandler mCaptionHandler;
    private ViewfinderView mViewfinderView;
    private InactivityTimer mInactivityTimer;
    private MediaPlayer mMediaPlayer;
    private ProgressDialog mProgress;
    private boolean hasSurface;
    private boolean vibrate;
    private boolean playBeep;

    //==========================================Privilege request start====================================
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[]
            grantResults) {
        Permissions4M.onRequestPermissionsResult(CaptureActivity.this, requestCode, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @PermissionsGranted({PermissionConstants.STORAGE_READ_CODE, PermissionConstants.STORAGE_WRITE_CODE})
    public void syncGranted(int code) {
    }

    @PermissionsDenied({PermissionConstants.STORAGE_READ_CODE, PermissionConstants.STORAGE_WRITE_CODE})
    public void syncDenied(int code) {
        Toast.makeText(CaptureActivity.this, "授权失败", Toast.LENGTH_SHORT).show();
    }

    @PermissionsRationale({PermissionConstants.STORAGE_READ_CODE, PermissionConstants.STORAGE_WRITE_CODE})
    public void syncRationale(int code) {
        Toast.makeText(CaptureActivity.this, "请开启存储授权", Toast.LENGTH_SHORT).show();
    }
    //==========================================Privilege request end====================================

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        CameraManager.init(getApplication());
        mViewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_content);
        hasSurface = false;
        mInactivityTimer = new InactivityTimer(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        mBackImageView = (ImageView) findViewById(R.id.toolbar_back);
        mBackImageView.setOnClickListener(this);

        setSupportActionBar(toolbar);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.toolbar_back:
                finish();
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scanner_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.scan_local:
                //打开手机中的相册
                Intent innerIntent = new Intent(Intent.ACTION_GET_CONTENT); //"android.intent.action.GET_CONTENT"
                innerIntent.setType("image/*");
                Intent wrapperIntent = Intent.createChooser(innerIntent, "选择二维码图片");
                this.startActivityForResult(wrapperIntent, REQUEST_CODE_SCAN_GALLERY);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_SCAN_GALLERY:
                    //获取选中图片的路径
                    mProgress = new ProgressDialog(CaptureActivity.this);
                    mProgress.setMessage("扫描中...");
                    mProgress.setCancelable(false);
                    mProgress.show();
                    mCoreContext.executeAsyncTask(() -> {
                        mProgress.dismiss();
                        Result result = scanningImage(data.getData());
                        handleScanResult(result.getText());
                    });
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.scanner_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        playBeep = true;
        AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
            playBeep = false;
        }
        initBeepSound();
        vibrate = true;
        Permissions4M
                .get(CaptureActivity.this)
                .requestSync();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCaptionHandler != null) {
            mCaptionHandler.quitSynchronously();
            mCaptionHandler = null;
        }
        CameraManager.get().closeDriver();
    }

    @Override
    protected void onDestroy() {
        mInactivityTimer.shutdown();
        mMediaPlayer.setOnCompletionListener(null);
        mMediaPlayer.release();
        mMediaPlayer = null;
        super.onDestroy();
    }

    /**
     * Handler scan result
     *
     * @param result
     * @param barcode
     */
    @RequiresPermission(Manifest.permission.VIBRATE)
    public void handleDecode(Result result, Bitmap barcode) {
        handleScanResult(result.getText());
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;

    }

    public ViewfinderView getViewfinderView() {
        return mViewfinderView;
    }

    public Handler getHandler() {
        return mCaptionHandler;
    }

    public void drawViewfinder() {
        mViewfinderView.drawViewfinder();
    }

    private void handleScanResult(final String resultString) {
        mInactivityTimer.onActivity();
        playBeepSoundAndVibrate();
        runOnUiThread(() -> {
            if (TextUtils.isEmpty(resultString)) {
                Toast.makeText(CaptureActivity.this, "扫描失败!", Toast.LENGTH_SHORT).show();
            } else {
                Intent resultIntent = new Intent();
                Bundle bundle = new Bundle();
                bundle.putString("result", resultString);
                resultIntent.putExtras(bundle);
                CaptureActivity.this.setResult(RESULT_OK, resultIntent);
            }
            CaptureActivity.this.finish();
        });
    }

    /**
     * 扫描二维码图片的方法
     * @param imageUri
     * @return
     */
    private Result scanningImage(final Uri imageUri) {
        if(imageUri == null) {
            return null;
        }
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            RGBLuminanceSource source = new RGBLuminanceSource(bitmap);
            BinaryBitmap bitmap1 = new BinaryBitmap(new HybridBinarizer(source));
            QRCodeReader reader = new QRCodeReader();
            Hashtable<DecodeHintType, String> hints = new Hashtable<>();
            hints.put(DecodeHintType.CHARACTER_SET, "UTF8");
            bitmap.recycle();
            return reader.decode(bitmap1, hints);
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (ChecksumException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
           e.printStackTrace();
        }
        return null;
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            CameraManager.get().openDriver(surfaceHolder);
        } catch (IOException ioe) {
            return;
        } catch (RuntimeException e) {
            return;
        }
        if (mCaptionHandler == null) {
            mCaptionHandler = new CaptureActivityHandler(this, null,
                    null);
        }
    }

    private void initBeepSound() {
        if (playBeep && mMediaPlayer == null) {
            // The volume on STREAM_SYSTEM is not adjustable, and users found it
            // too loud,
            // so we now play on the music stream.
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setOnCompletionListener(mediaPlayer -> mMediaPlayer.seekTo(0));

            AssetFileDescriptor file = getResources().openRawResourceFd(
                    R.raw.beep);
            try {
                mMediaPlayer.setDataSource(file.getFileDescriptor(),
                        file.getStartOffset(), file.getLength());
                file.close();
                mMediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
                mMediaPlayer.prepare();
            } catch (IOException e) {
                mMediaPlayer = null;
            }
        }
    }

    private static final long VIBRATE_DURATION = 200L;

    @RequiresPermission(Manifest.permission.VIBRATE)
    private void playBeepSoundAndVibrate() {
        if (playBeep && mMediaPlayer != null) {
            mMediaPlayer.start();
        }
        if (vibrate) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATE_DURATION);
        }
    }
}