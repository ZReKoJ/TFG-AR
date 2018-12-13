package com.ucm.tfg.cinema;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Handler;
import android.os.Build;
import android.os.Bundle;
import android.service.autofill.Dataset;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;
import com.ucm.tfg.cinema.SampleAppMenu.SampleAppMenu;
import com.ucm.tfg.cinema.SampleAppMenu.SampleAppMenuGroup;
import com.ucm.tfg.cinema.SampleAppMenu.SampleAppMenuInterface;
import com.ucm.tfg.cinema.SampleApplication.SampleApplicationControl;
import com.ucm.tfg.cinema.SampleApplication.SampleApplicationException;
import com.ucm.tfg.cinema.SampleApplication.SampleApplicationSession;
import com.ucm.tfg.cinema.SampleApplication.utils.LoadingDialogHandler;
import com.ucm.tfg.cinema.SampleApplication.utils.SampleApplicationGLView;
import com.ucm.tfg.cinema.SampleApplication.utils.Texture;
import com.vuforia.*;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * The main activity for the ImageTargets sample. Image Targets allows users to
 * create 2D targets for detection and tracking
 *
 * This class does high-level handling of the Vuforia lifecycle and any UI
 * updates
 *
 * For ImageTarget-specific rendering, check out ImageTargetRenderer.java For
 * the low-level Vuforia lifecycle code, check out SampleApplicationSession.java
 */
public class ImageTargets2 extends Activity implements SampleApplicationControl {
    private static final String LOGTAG = "ImageTargets2";

    private SampleApplicationSession vuforiaAppSession;

    private SampleApplicationGLView mGlView;

    private ImageTargetRenderer mRenderer;

    private HashMap<String, DataSet> databases = new HashMap<>();
    private HashMap<String, String> targets = new HashMap<>();
    private HashMap<String, Texture> textures = new HashMap<>();

    private RelativeLayout mUILayout;

    final LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);

        vuforiaAppSession = new SampleApplicationSession(this);

        startLoadingAnimation();

        vuforiaAppSession.initAR(this, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Load any sample specific textures:
        loadData();
    }

    // Load specific textures from the APK, which we will later use for rendering.
    private void loadData() {
        databases.put("OnePiece.xml", null);
        databases.put("StonesAndChips.xml", null);
        databases.put("Tarmac.xml", null);

        targets.put("stones", "teapotBrass");
        targets.put("chips", "teapotBlue");
        targets.put("tarmac", "teapotRed");
        targets.put("luffy", "teapotBrass");
        targets.put("zoro", "teapotBlue");
        targets.put("nami", "teapotRed");
        targets.put("franky", "teapotBrass");
        targets.put("jinbei", "teapotBlue");
        targets.put("sanji", "teapotRed");
        targets.put("brook", "teapotBrass");
        targets.put("usopp", "teapotBlue");
        targets.put("chopper", "teapotRed");
        targets.put("robin", "teapotBrass");
        targets.put("Z", "teapotBlue");

        textures.put("teapotBrass", Texture.loadTextureFromApk("TextureTeapotBrass.png", getAssets()));
        textures.put("teapotBlue", Texture.loadTextureFromApk("TextureTeapotBlue.png", getAssets()));
        textures.put("teapotRed", Texture.loadTextureFromApk("TextureTeapotRed.png", getAssets()));
    }

    @Override
    protected void onResume() {
        Log.d(LOGTAG, "onResume");
        super.onResume();

        showProgressIndicator(true);

        vuforiaAppSession.onResume();
    }

    // Called whenever the device orientation or screen resolution changes
    @Override
    public void onConfigurationChanged(Configuration config) {
        Log.d(LOGTAG, "onConfigurationChanged");
        super.onConfigurationChanged(config);

        vuforiaAppSession.onConfigurationChanged();
    }

    @Override
    protected void onPause() {
        Log.d(LOGTAG, "onPause");
        super.onPause();

        if (mGlView != null) {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }

        vuforiaAppSession.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d(LOGTAG, "onDestroy");
        super.onDestroy();

        try {
            vuforiaAppSession.stopAR();
        } catch (SampleApplicationException e) {
            Log.e(LOGTAG, e.getString());
        }

        System.gc();
    }

    private void initApplicationAR() {
        // Create OpenGL ES view:
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();

        mGlView = new SampleApplicationGLView(getApplicationContext());
        mGlView.init(translucent, depthSize, stencilSize);

        mRenderer = new ImageTargetRenderer(this, vuforiaAppSession);

        Vector<Texture> mTextures = new Vector();
        for (Map.Entry<String, Texture> entry : textures.entrySet()) {
            mTextures.add(entry.getValue());
        }

        mRenderer.setTextures(mTextures);
        mGlView.setRenderer(mRenderer);
    }

    private void startLoadingAnimation() {
        mUILayout = (RelativeLayout) View.inflate(getApplicationContext(), R.layout.camera_overlay, null);

        mUILayout.setVisibility(View.VISIBLE);
        mUILayout.setBackgroundColor(Color.BLACK);

        // Gets a reference to the loading dialog
        loadingDialogHandler.mLoadingDialogContainer = mUILayout.findViewById(R.id.loading_indicator);

        // Shows the loading indicator at start
        loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);

        // Adds the inflated layout to the view
        addContentView(mUILayout, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

    }

    @Override
    public boolean doLoadTrackersData() {
        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager.getTracker(ObjectTracker.getClassType());
        if (objectTracker == null) {
            return false;
        }

        for (Map.Entry<String, DataSet> entry : databases.entrySet()) {

            if (entry.getValue() == null) {
                entry.setValue(objectTracker.createDataSet());
            }

            if (entry.getValue() == null) {
                return false;
            }

            if (!entry.getValue().load(entry.getKey(), STORAGE_TYPE.STORAGE_APPRESOURCE)) {
                return false;
            }

            if (!objectTracker.activateDataSet(entry.getValue())) {
                return false;
            }

            TrackableList trackableList = entry.getValue().getTrackables();
            for (Trackable trackable : trackableList) {
                String name = "Current Dataset : " + trackable.getName();
                trackable.setUserData(name);
                Log.d(LOGTAG, "UserData:Set the following user data " + trackable.getUserData());
            }
        }

        return true;
    }

    @Override
    public boolean doUnloadTrackersData() {
        // Indicate if the trackers were unloaded correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager.getTracker(ObjectTracker.getClassType());

        if (objectTracker == null) {
            return false;
        }

        for (Map.Entry<String, DataSet> entry : databases.entrySet()) {
            if (entry.getValue() != null && entry.getValue().isActive()) {
                if (objectTracker.getActiveDataSets().at(0).equals(entry.getValue())
                        && !objectTracker.deactivateDataSet(entry.getValue())) {
                    result = false;
                } else if (!objectTracker.destroyDataSet(entry.getValue())) {
                    result = false;
                }

                entry.setValue(null);
            }
        }

        return result;
    }

    @Override
    public void onVuforiaResumed() {
        if (mGlView != null) {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }
    }

    @Override
    public void onVuforiaStarted() {
        mRenderer.updateRenderingPrimitives();
        mRenderer.updateConfiguration();

        showProgressIndicator(false);
    }

    private void showProgressIndicator(boolean show) {
        if (show) {
            loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);
        } else {
            loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
        }
    }

    // Called once Vuforia has been initialized or
    // an error has caused Vuforia initialization to stop
    @Override
    public void onInitARDone(SampleApplicationException exception) {
        if (exception == null) {
            initApplicationAR();

            mRenderer.setActive(true);

            addContentView(mGlView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

            mUILayout.bringToFront();

            mUILayout.setBackgroundColor(Color.TRANSPARENT);

            vuforiaAppSession.startAR(CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT);
        } else {
            Log.e(LOGTAG, exception.getString());
        }
    }

    // Called every frame
    @Override
    public void onVuforiaUpdate(State state) {

    }

    @Override
    public boolean doInitTrackers() {
        // Indicate if the trackers were initialized correctly
        boolean result = true;

        // For ImageTargets, the recommended fusion provider mode is
        // the one recommended by the FUSION_OPTIMIZE_IMAGE_TARGETS_AND_VUMARKS enum
        if (!vuforiaAppSession.setFusionProviderType(FUSION_PROVIDER_TYPE.FUSION_OPTIMIZE_IMAGE_TARGETS_AND_VUMARKS)) {
            return false;
        }

        TrackerManager tManager = TrackerManager.getInstance();

        Tracker tracker = tManager.initTracker(ObjectTracker.getClassType());
        if (tracker == null) {
            Log.e(LOGTAG, "Tracker not initialized. Tracker already initialized or the camera is already started");
            result = false;
        } else {
            Log.i(LOGTAG, "Tracker successfully initialized");
        }

        // Initialize the Positional Device Tracker
        DeviceTracker deviceTracker = (PositionalDeviceTracker) tManager
                .initTracker(PositionalDeviceTracker.getClassType());

        if (deviceTracker != null) {
            Log.i(LOGTAG, "Successfully initialized Device Tracker");
        } else {
            Log.e(LOGTAG, "Failed to initialize Device Tracker");
        }

        return result;
    }

    @Override
    public boolean doStartTrackers() {
        // Indicate if the trackers were started correctly
        boolean result = true;

        TrackerManager trackerManager = TrackerManager.getInstance();

        Tracker objectTracker = trackerManager.getTracker(ObjectTracker.getClassType());

        if (objectTracker != null && objectTracker.start()) {
            Log.i(LOGTAG, "Successfully started Object Tracker");
        } else {
            Log.e(LOGTAG, "Failed to start Object Tracker");
            result = false;
        }

        return result;
    }

    @Override
    public boolean doStopTrackers() {
        // Indicate if the trackers were stopped correctly
        boolean result = true;

        TrackerManager trackerManager = TrackerManager.getInstance();

        Tracker objectTracker = trackerManager.getTracker(ObjectTracker.getClassType());
        if (objectTracker != null) {
            objectTracker.stop();
            Log.i(LOGTAG, "Successfully stopped object tracker");
        } else {
            Log.e(LOGTAG, "Failed to stop object tracker");
            result = false;
        }

        return result;
    }

    @Override
    public boolean doDeinitTrackers() {
        TrackerManager tManager = TrackerManager.getInstance();

        // Indicate if the trackers were deinitialized correctly
        boolean result = tManager.deinitTracker(ObjectTracker.getClassType());
        tManager.deinitTracker(PositionalDeviceTracker.getClassType());

        return result;
    }

    boolean isDeviceTrackingActive() {
        return false;
    }
}
