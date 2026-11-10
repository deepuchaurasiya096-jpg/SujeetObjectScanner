package com.sujeet.objectscanner;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabelerOptions;
import com.google.mlkit.vision.label.ImageLabeling;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sujeet Object Scanner
 * Live camera image labeling using CameraX + bundled on-device ML Kit model.
 * The app intentionally does not include QR, document, or 3D scanning features.
 */
public class MainActivity extends ComponentActivity {
    private static final int CAMERA_PERMISSION_REQUEST = 410;
    private static final int PICK_IMAGE_REQUEST = 411;
    private static final int DARK = Color.rgb(8, 11, 20);
    private static final int PANEL = Color.rgb(18, 25, 40);
    private static final int PANEL_ALT = Color.rgb(26, 35, 54);
    private static final int PURPLE = Color.rgb(167, 139, 250);
    private static final int CYAN = Color.rgb(34, 211, 238);
    private static final int WHITE = Color.rgb(245, 247, 255);
    private static final int MUTED = Color.rgb(159, 171, 194);
    private static final int GREEN = Color.rgb(52, 211, 153);

    private PreviewView previewView;
    private TextView statusText;
    private TextView mainLabelText;
    private TextView confidenceText;
    private TextView extraLabelsText;
    private TextView historyText;
    private TextView scanStateText;
    private TextView pauseButton;
    private TextView flashButton;

    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private ImageAnalysis imageAnalysis;
    private ExecutorService cameraExecutor;
    private ImageLabeler imageLabeler;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private long lastAnalysisTime = 0L;
    private boolean isPaused = false;
    private boolean flashEnabled = false;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private final List<String> recentScans = new ArrayList<>();
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(DARK);
        getWindow().setNavigationBarColor(DARK);
        getWindow().getDecorView().setSystemUiVisibility(0);

        preferences = getSharedPreferences("scanner_history", MODE_PRIVATE);
        loadHistory();
        cameraExecutor = Executors.newSingleThreadExecutor();
        ImageLabelerOptions options = new ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.45f)
                .build();
        imageLabeler = ImageLabeling.getClient(options);

        setContentView(createScreen());
        updateHistoryText();
        checkCameraPermissionAndStart();
    }

    private View createScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(10));
        root.setBackgroundColor(DARK);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);

        TextView logo = text("⌗", 27, CYAN, true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(roundRect(PANEL_ALT, 16, PURPLE, 1));
        header.addView(logo, new LinearLayout.LayoutParams(dp(52), dp(52)));

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setPadding(dp(12), 0, 0, 0);
        TextView title = text("SUJEET SCANNER", 19, WHITE, true);
        TextView subtitle = text("OBJECT RECOGNITION  •  ON-DEVICE AI", 9, MUTED, true);
        titleBox.addView(title);
        titleBox.addView(subtitle);
        header.addView(titleBox, new LinearLayout.LayoutParams(0, -2, 1));

        TextView liveBadge = text("● LIVE", 10, GREEN, true);
        liveBadge.setGravity(Gravity.CENTER);
        liveBadge.setPadding(dp(11), dp(8), dp(11), dp(8));
        liveBadge.setBackground(roundRect(Color.rgb(14, 48, 43), 24, Color.rgb(31, 110, 91), 1));
        header.addView(liveBadge);
        root.addView(header);

        TextView intro = text("Point your camera at something. The scanner will identify visible objects.", 12, MUTED, false);
        intro.setPadding(0, dp(10), 0, dp(12));
        root.addView(intro);

        FrameLayout stage = new FrameLayout(this);
        stage.setBackground(roundRect(Color.BLACK, 22, Color.rgb(79, 67, 120), 1));
        stage.setClipToOutline(true);
        LinearLayout.LayoutParams stageParams = new LinearLayout.LayoutParams(-1, 0, 1f);
        stageParams.bottomMargin = dp(12);
        root.addView(stage, stageParams);

        previewView = new PreviewView(this);
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        stage.addView(previewView, new FrameLayout.LayoutParams(-1, -1));

        TextView cameraHint = text("ALIGN OBJECT IN FRAME", 10, WHITE, true);
        cameraHint.setGravity(Gravity.CENTER);
        cameraHint.setPadding(dp(12), dp(8), dp(12), dp(8));
        cameraHint.setBackground(roundRect(0xAA080B14, 18, 0x667B68B7, 1));
        FrameLayout.LayoutParams hintParams = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        hintParams.topMargin = dp(14);
        stage.addView(cameraHint, hintParams);

        TextView crosshair = text("＋", 42, 0x99FFFFFF, false);
        crosshair.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams crossParams = new FrameLayout.LayoutParams(dp(70), dp(70), Gravity.CENTER);
        stage.addView(crosshair, crossParams);

        TextView stageStatus = text("CAMERA STARTING…", 10, WHITE, true);
        stageStatus.setPadding(dp(12), dp(8), dp(12), dp(8));
        stageStatus.setBackground(roundRect(0xCC080B14, 12, 0x557B68B7, 1));
        FrameLayout.LayoutParams stageStatusParams = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.START);
        stageStatusParams.setMargins(dp(12), 0, dp(12), dp(12));
        stage.addView(stageStatus, stageStatusParams);
        this.scanStateText = stageStatus;

        LinearLayout resultCard = new LinearLayout(this);
        resultCard.setOrientation(LinearLayout.VERTICAL);
        resultCard.setPadding(dp(15), dp(13), dp(15), dp(13));
        resultCard.setBackground(roundRect(PANEL, 18, Color.rgb(44, 54, 78), 1));

        LinearLayout resultHeader = new LinearLayout(this);
        resultHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView resultTitle = text("WHAT I SEE", 11, CYAN, true);
        resultHeader.addView(resultTitle, new LinearLayout.LayoutParams(0, -2, 1));
        statusText = text("WAITING FOR CAMERA", 9, MUTED, true);
        resultHeader.addView(statusText);
        resultCard.addView(resultHeader);

        LinearLayout primaryRow = new LinearLayout(this);
        primaryRow.setGravity(Gravity.CENTER_VERTICAL);
        primaryRow.setPadding(0, dp(10), 0, dp(2));
        mainLabelText = text("No object scanned yet", 19, WHITE, true);
        primaryRow.addView(mainLabelText, new LinearLayout.LayoutParams(0, -2, 1));
        confidenceText = text("—", 13, GREEN, true);
        primaryRow.addView(confidenceText);
        resultCard.addView(primaryRow);

        extraLabelsText = text("Keep one object clearly visible for better results.", 11, MUTED, false);
        extraLabelsText.setPadding(0, dp(5), 0, dp(2));
        resultCard.addView(extraLabelsText);

        TextView historyTitle = text("RECENT SCANS", 9, PURPLE, true);
        historyTitle.setPadding(0, dp(10), 0, dp(4));
        resultCard.addView(historyTitle);
        historyText = text("No scans yet", 10, MUTED, false);
        resultCard.addView(historyText);

        root.addView(resultCard, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(0, dp(10), 0, dp(2));

        TextView photoButton = controlButton("PHOTO", CYAN);
        photoButton.setOnClickListener(v -> choosePhoto());
        controls.addView(photoButton, weightedButton());

        pauseButton = controlButton("PAUSE", PURPLE);
        pauseButton.setOnClickListener(v -> togglePause());
        LinearLayout.LayoutParams p2 = weightedButton();
        p2.leftMargin = dp(7);
        controls.addView(pauseButton, p2);

        TextView flipButton = controlButton("FLIP", WHITE);
        flipButton.setOnClickListener(v -> switchCamera());
        LinearLayout.LayoutParams p3 = weightedButton();
        p3.leftMargin = dp(7);
        controls.addView(flipButton, p3);

        flashButton = controlButton("FLASH OFF", WHITE);
        flashButton.setOnClickListener(v -> toggleFlash());
        LinearLayout.LayoutParams p4 = weightedButton();
        p4.leftMargin = dp(7);
        controls.addView(flashButton, p4);

        root.addView(controls);

        TextView privacy = text("ON-DEVICE SCANNING  •  CAMERA IMAGES ARE NOT UPLOADED BY THIS APP", 8, MUTED, false);
        privacy.setGravity(Gravity.CENTER);
        privacy.setPadding(0, dp(7), 0, 0);
        root.addView(privacy);

        return root;
    }

    private LinearLayout.LayoutParams weightedButton() {
        return new LinearLayout.LayoutParams(0, dp(44), 1f);
    }

    private TextView controlButton(String label, int accent) {
        TextView button = text(label, 10, accent, true);
        button.setGravity(Gravity.CENTER);
        button.setBackground(roundRect(PANEL_ALT, 13, accent, 1));
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private GradientDrawable roundRect(int color, int radiusDp, int strokeColor, int strokeDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) drawable.setStroke(dp(strokeDp), strokeColor);
        return drawable;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                showCameraPermissionMessage();
            }
        }
    }

    private void showCameraPermissionMessage() {
        statusText.setText("CAMERA PERMISSION NEEDED");
        mainLabelText.setText("Allow camera access");
        extraLabelsText.setText("Open Android Settings and allow Camera permission to use live object scanning.");
        scanStateText.setText("CAMERA ACCESS BLOCKED");
        Toast.makeText(this, "Camera permission is needed for object scanning.", Toast.LENGTH_LONG).show();
    }

    private void startCamera() {
        statusText.setText("STARTING CAMERA");
        scanStateText.setText("CONNECTING TO CAMERA…");
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (Exception e) {
                statusText.setText("CAMERA ERROR");
                scanStateText.setText("CAMERA UNAVAILABLE");
                extraLabelsText.setText("Could not start the camera. Close other camera apps and try again.");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) return;
        CameraSelector selector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();
        if (!cameraProvider.hasCamera(selector)) {
            Toast.makeText(this, "This camera is not available on this device.", Toast.LENGTH_SHORT).show();
            lensFacing = CameraSelector.LENS_FACING_BACK;
            selector = CameraSelector.DEFAULT_BACK_CAMERA;
        }

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new android.util.Size(480, 360))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeFrame);

        cameraProvider.unbindAll();
        camera = cameraProvider.bindToLifecycle(this, selector, preview, imageAnalysis);
        flashEnabled = false;
        flashButton.setText("FLASH OFF");
        flashButton.setTextColor(WHITE);
        statusText.setText("SCANNING LIVE");
        scanStateText.setText("AI VISION ACTIVE");
    }

    private void analyzeFrame(ImageProxy imageProxy) {
        long now = SystemClock.elapsedRealtime();
        if (isPaused || now - lastAnalysisTime < 550 || !isProcessing.compareAndSet(false, true)) {
            imageProxy.close();
            return;
        }
        lastAnalysisTime = now;
        android.media.Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            isProcessing.set(false);
            imageProxy.close();
            return;
        }

        InputImage inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
        imageLabeler.process(inputImage)
                .addOnSuccessListener(labels -> showLabels(labels, "LIVE CAMERA"))
                .addOnFailureListener(e -> runOnUiThread(() -> {
                    statusText.setText("SCAN RETRY");
                    extraLabelsText.setText("Could not read this frame. Try better lighting and hold steady.");
                }))
                .addOnCompleteListener(task -> {
                    isProcessing.set(false);
                    imageProxy.close();
                });
    }

    private void showLabels(List<ImageLabel> labels, String source) {
        List<ImageLabel> sorted = new ArrayList<>(labels);
        Collections.sort(sorted, (a, b) -> Float.compare(b.getConfidence(), a.getConfidence()));
        runOnUiThread(() -> {
            if (sorted.isEmpty()) {
                statusText.setText("NO CLEAR LABEL");
                mainLabelText.setText("Not sure yet");
                confidenceText.setText("—");
                extraLabelsText.setText("Move closer, improve lighting, and keep the object in view.");
                return;
            }
            ImageLabel best = sorted.get(0);
            String primary = best.getText();
            int percent = Math.round(best.getConfidence() * 100f);
            statusText.setText(source + "  •  " + sorted.size() + " LABELS");
            mainLabelText.setText(primary);
            confidenceText.setText(percent + "%");

            StringBuilder details = new StringBuilder();
            int count = Math.min(4, sorted.size());
            for (int i = 0; i < count; i++) {
                ImageLabel label = sorted.get(i);
                if (i > 0) details.append("   •   ");
                details.append(label.getText()).append(" ").append(Math.round(label.getConfidence() * 100f)).append("%");
            }
            extraLabelsText.setText(details.toString());
            addHistory(primary, percent);
        });
    }

    private void addHistory(String label, int confidence) {
        String item = label + "  " + confidence + "%";
        if (!recentScans.isEmpty() && recentScans.get(0).equals(item)) return;
        recentScans.remove(item);
        recentScans.add(0, item);
        while (recentScans.size() > 5) recentScans.remove(recentScans.size() - 1);
        String joined = joinStrings(recentScans, "\n");
        preferences.edit().putString("recent", joined).apply();
        updateHistoryText();
    }

    private void loadHistory() {
        String saved = preferences.getString("recent", "");
        if (saved == null || saved.isEmpty()) return;
        String[] rows = saved.split("\\n");
        for (String row : rows) {
            if (!row.trim().isEmpty() && recentScans.size() < 5) recentScans.add(row);
        }
    }

    private void updateHistoryText() {
        if (historyText == null) return;
        if (recentScans.isEmpty()) {
            historyText.setText("No scans yet");
        } else {
            historyText.setText(joinStrings(recentScans, "   •   "));
        }
    }

    private String joinStrings(List<String> values, String separator) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) builder.append(separator);
            builder.append(value);
        }
        return builder.toString();
    }

    private void togglePause() {
        isPaused = !isPaused;
        pauseButton.setText(isPaused ? "RESUME" : "PAUSE");
        statusText.setText(isPaused ? "SCANNING PAUSED" : "SCANNING LIVE");
        scanStateText.setText(isPaused ? "PAUSED" : "AI VISION ACTIVE");
    }

    private void switchCamera() {
        if (cameraProvider == null) return;
        lensFacing = lensFacing == CameraSelector.LENS_FACING_BACK
                ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
        try {
            bindCameraUseCases();
        } catch (Exception e) {
            lensFacing = CameraSelector.LENS_FACING_BACK;
            bindCameraUseCases();
        }
    }

    private void toggleFlash() {
        if (camera == null || !camera.getCameraInfo().hasFlashUnit()) {
            Toast.makeText(this, "Flash is not available for this camera.", Toast.LENGTH_SHORT).show();
            return;
        }
        flashEnabled = !flashEnabled;
        camera.getCameraControl().enableTorch(flashEnabled);
        flashButton.setText(flashEnabled ? "FLASH ON" : "FLASH OFF");
        flashButton.setTextColor(flashEnabled ? GREEN : WHITE);
    }

    private void choosePhoto() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Choose a photo to scan"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                InputImage image = InputImage.fromFilePath(this, uri);
                statusText.setText("SCANNING PHOTO");
                mainLabelText.setText("Analysing image…");
                imageLabeler.process(image)
                        .addOnSuccessListener(labels -> showLabels(labels, "PHOTO SCAN"))
                        .addOnFailureListener(e -> {
                            statusText.setText("PHOTO SCAN FAILED");
                            extraLabelsText.setText("This image could not be analysed. Try another photo.");
                        });
            } catch (IOException e) {
                Toast.makeText(this, "Could not open that image.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraProvider != null) cameraProvider.unbindAll();
        if (imageLabeler != null) imageLabeler.close();
        if (cameraExecutor != null) cameraExecutor.shutdown();
    }
}
