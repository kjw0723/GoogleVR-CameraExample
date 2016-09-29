package com.example.kju.googlevrcameraexample;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;

import java.util.Arrays;


/**
 *
 */
public class MainActivity extends GvrActivity implements GvrRenderer.GvrRendererEvents {

	private static final String TAG = "MainActivity";

	private GvrView cameraView;
	private GvrRenderer gvrRenderer;

	private CameraDevice cameraDevice;
	private CameraManager cameraManager;
	private CaptureRequest.Builder previewBuilder;
	private CameraCaptureSession previewSession;
	private SurfaceTexture surfaceTexture;
	private StreamConfigurationMap map;
	private Size previewSize;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		cameraView = (GvrView) findViewById(R.id.camera_view);
		setGvrView(cameraView);

		gvrRenderer = new GvrRenderer(cameraView, this);
		cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
	}
	@Override
	protected void onResume() {
		super.onResume();
		cameraView.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		cameraView.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

	}

	private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
		@Override
		public void onOpened(CameraDevice camera) {
			cameraDevice = camera;
			startPreview();
		}

		@Override
		public void onDisconnected(CameraDevice cameraDevice) {
			Log.d(TAG, "onDisconnected");
		}

		@Override
		public void onError(CameraDevice cameraDevice, int i) {
			Log.e(TAG, "onError");
		}
	};

	private void openCamera()
	{
		try {
			String cameraId = cameraManager.getCameraIdList()[0];
			CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
			map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
			previewSize = map.getOutputSizes(SurfaceTexture.class)[0];
			cameraManager.openCamera(cameraId, stateCallback, cameraView.getHandler());
		} catch (CameraAccessException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	protected void startPreview()
	{
		if (cameraDevice == null) {
			Log.e(TAG, "preview failed");
			return;
		}

		Surface surface = new Surface(surfaceTexture);

		try {
			previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
		} catch (CameraAccessException e) {
			e.printStackTrace();
		}

		previewBuilder.addTarget(surface);

		try {
			cameraDevice.createCaptureSession(Arrays.asList(surface),
					new CameraCaptureSession.StateCallback() {
						@Override
						public void onConfigured(CameraCaptureSession session) {
							previewSession = session;
							updatePreview();
						}

						@Override
						public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
							Log.e(TAG, "onConfigureFailed");
						}
					}, null);
		} catch (CameraAccessException e) {
			e.printStackTrace();
		}
	}

	protected void updatePreview() {
		if (null == cameraDevice) {
			Log.e(TAG, "updatePreivew error, return");
		}

		previewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

		HandlerThread thread = new HandlerThread("CameraPreview");
		thread.start();
		Handler backgroundHandler = new Handler(thread.getLooper());

		try {
			previewSession.setRepeatingRequest(previewBuilder.build(), null, backgroundHandler);
		} catch (CameraAccessException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onSurfaceTextureCreated(SurfaceTexture surfaceTexture) {
		this.surfaceTexture = surfaceTexture;
		openCamera();
	}
}
