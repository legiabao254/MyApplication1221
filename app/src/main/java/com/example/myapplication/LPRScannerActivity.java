package com.example.myapplication;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LPRScannerActivity extends AppCompatActivity {

    private static final String TAG = "LPRScannerActivity";
    private PreviewView previewView;
    private Button captureButton;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private TextRecognizer textRecognizer;

    // Trình khởi chạy để yêu cầu quyền
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Đã cấp quyền Camera", Toast.LENGTH_SHORT).show();
                    startCamera();
                } else {
                    Toast.makeText(this, "Không thể sử dụng tính năng này nếu không có quyền Camera", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lprscanner);

        previewView = findViewById(R.id.camera_preview);
        captureButton = findViewById(R.id.capture_button);

        cameraExecutor = Executors.newSingleThreadExecutor();
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        // 1. Kiểm tra và yêu cầu quyền Camera
        checkCameraPermission();

        // 2. Thiết lập sự kiện cho nút chụp ảnh
        captureButton.setOnClickListener(v -> takePhoto());
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Đã có quyền, khởi động camera
            startCamera();
        } else {
            // Chưa có quyền, yêu cầu quyền
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi khởi tạo camera", e);
                Toast.makeText(this, "Lỗi khi khởi tạo camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (imageCapture == null) {
            return;
        }

        captureButton.setEnabled(false); // Vô hiệu hóa nút chụp để tránh nhấn nhiều lần

        File photoFile = new File(getExternalFilesDir(null),
                new SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg");

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Uri savedUri = outputFileResults.getSavedUri();
                if (savedUri == null) {
                    savedUri = Uri.fromFile(photoFile);
                }
                Toast.makeText(LPRScannerActivity.this, "Đang xử lý ảnh...", Toast.LENGTH_SHORT).show();

                // 3. Gọi hàm xử lý ảnh bằng ML Kit
                processImageForLicensePlate(savedUri);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Lỗi khi chụp ảnh", exception);
                Toast.makeText(LPRScannerActivity.this, "Lỗi khi chụp ảnh: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                captureButton.setEnabled(true);
            }
        });
    }

    private void processImageForLicensePlate(Uri imageUri) {
        try {
            InputImage image = InputImage.fromFilePath(this, imageUri);
            textRecognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        String fullText = visionText.getText();
                        // TODO: Thêm logic để lọc và tìm ra chuỗi là biển số xe từ `fullText`
                        // Ví dụ đơn giản: lấy dòng chữ đầu tiên không rỗng
                        String licensePlate = "Không tìm thấy";
                        for (String block : fullText.split("\n")) {
                            if (!block.trim().isEmpty()) {
                                licensePlate = block.trim();
                                break;
                            }
                        }
                        returnScannedLicensePlate(licensePlate);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "ML Kit xử lý ảnh thất bại", e);
                        Toast.makeText(this, "Xử lý ảnh thất bại", Toast.LENGTH_SHORT).show();
                        returnScannedLicensePlate("Lỗi xử lý");
                    });
        } catch (IOException e) {
            Log.e(TAG, "Không thể tạo InputImage từ Uri", e);
            Toast.makeText(this, "Không thể đọc file ảnh", Toast.LENGTH_SHORT).show();
            captureButton.setEnabled(true);
        }
    }


    private void returnScannedLicensePlate(String licensePlate) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("LICENSE_PLATE_RESULT", licensePlate);
        setResult(RESULT_OK, resultIntent);
        finish(); // Đóng Activity quét và trả kết quả về
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        textRecognizer.close();
    }
}
