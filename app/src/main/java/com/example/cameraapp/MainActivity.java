package com.example.cameraapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.media.Image;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Size;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraProvider;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CAMERA = 83854;
    private static final int FLAG_AF = 1;
    private static final int FLAG_AE = 4;

    //работа с камерой
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private CameraControl cameraControl;
    private CameraSelector cameraSelector;
    private CameraInfo cameraInfo;
    private int cameraSwitcher = CameraSelector.LENS_FACING_FRONT;


    //виджеты
    private ImageButton mCaptureButton;
    private ImageButton mSwitchCameraButton;
    private PreviewView previewView;
    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalysis;
    private Switch mTorchSwitch;
    private SeekBar mZoomSeekBar;
    private TextView mZoomTextView;
    private ProgressBar mAutoFocusProgressBar;
    private ImageView mFrameImageView;







    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFrameImageView = findViewById(R.id.imageFrame);





        mAutoFocusProgressBar = findViewById(R.id.progressBarAutoFocus);
        mAutoFocusProgressBar.setVisibility(View.INVISIBLE);
        mAutoFocusProgressBar.isAnimating();

        previewView =  findViewById(R.id.previewView);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CAMERA);
        } else {
            initializeCamera(1);
        }

        mSwitchCameraButton = findViewById(R.id.switchCameraButton);
        mSwitchCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                cameraProvider.unbindAll();
                if(cameraSwitcher == 1)
                {
                    initializeCamera(1);
                    cameraSwitcher = 0;
                }
                else{
                    initializeCamera(0);
                    cameraSwitcher = 1;
                }
            }
        });

        mTorchSwitch = findViewById(R.id.switch_torch);
        mTorchSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    torchControl();
            }
        });

        mZoomTextView = findViewById(R.id.textViewZoom);
        mZoomSeekBar = findViewById(R.id.seekBar_zoom);
        mZoomSeekBar.setMax(1);
        mZoomSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    cameraControl.setLinearZoom(mZoomSeekBar.getProgress());
                    mZoomTextView.setText(String.valueOf(mZoomSeekBar.getProgress()) + 'f');

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //cameraProviderFuture = ProcessCameraProvider.getInstance(this);



//        cameraProviderFuture.addListener(() -> {
//            try {
//          cameraProvider = cameraProviderFuture.get();
//                bindPreview(cameraProvider);
//            } catch (ExecutionException | InterruptedException e) {
//                // No errors need to be handled for this Future.
//                // This should never be reached.
//            }
//        }, ContextCompat.getMainExecutor(this));

        mCaptureButton = (ImageButton) findViewById(R.id.capture_button);
        mCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                savePicture();
            }
        });

        previewView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                    mFrameImageView.setVisibility(View.VISIBLE);
                    mFrameImageView.setX(motionEvent.getX());
                    mFrameImageView.setY(motionEvent.getY());
                    AutoFocus(motionEvent);
                    //mFrameImageView.setVisibility(View.INVISIBLE);


                return true;


            }
        });


        

    }

    private void initializeCamera(int selectCamera)
    {
        //previewView =  findViewById(R.id.previewView);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);



        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider, selectCamera);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));
    }





    private void AutoFocus(MotionEvent motion)
    {
//        mAutoFocusProgressBar.isAnimating();
//        mAutoFocusProgressBar.setVisibility(View.VISIBLE);

        final float x = motion.getX();
        final float y = motion.getY();
        MeteringPointFactory pointFactory = previewView.getMeteringPointFactory();
        float afPointWidth = 1.0f / 6.0f;  // 1/6 total area
        float aePointWidth = afPointWidth * 1.5f;
        MeteringPoint afPoint = pointFactory.createPoint(x, y, afPointWidth);
        MeteringPoint aePoint = pointFactory.createPoint(x, y, aePointWidth);



        MeteringPoint meteringPoint = previewView
                .getMeteringPointFactory()
                .createPoint(x, y);
        FocusMeteringAction focusMeteringAction = new FocusMeteringAction.Builder(meteringPoint)
                .addPoint(afPoint, FLAG_AF)
                .addPoint(aePoint, FLAG_AE)
                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                .build();
        try{
            cameraControl.startFocusAndMetering(focusMeteringAction);



        }
        catch(Exception e)
        {
        }




    }


    private void savePicture()
    {
        mAutoFocusProgressBar.setVisibility(View.VISIBLE);
        long timestamp = System.currentTimeMillis();

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, timestamp);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");


        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions
                        .Builder(getContentResolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                        .build();
        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                        // insert your code here.

                        Toast.makeText(MainActivity.this, "Photo has been saved successfully ", Toast.LENGTH_SHORT).show();
                        mAutoFocusProgressBar.setVisibility(View.INVISIBLE);

                    }

                    @Override
                    public void onError(ImageCaptureException error) {
                        // insert your code here.
                        Toast.makeText(MainActivity.this, "Photo hasn't been saved " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void torchControl() {
        if (mTorchSwitch.isChecked() == true) {
            cameraControl = camera.getCameraControl();
            cameraControl.enableTorch(true);
        } else {
            cameraControl = camera.getCameraControl();
            cameraControl.enableTorch(false);
        }
    }









    private void bindPreview(ProcessCameraProvider cameraProvider, int selectCamera) {
        Preview preview = new Preview.Builder().build();

        if(selectCamera == 1)
        {
            cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        }
        else
        {
            cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();

        }
        //cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();


        imageCapture = new ImageCapture.Builder()
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageCapture, preview);


        cameraControl = camera.getCameraControl();
        cameraInfo = camera.getCameraInfo();





    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeCamera(1);

        }

    }


}