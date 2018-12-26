package com.ucm.tfg.cinema;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.RelativeLayout;
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

import java.util.ArrayList;
import java.util.Vector;

/**
 * The main activity for the CloudReco sample.
 * Cloud Recognition allows users to detect a dataset using an internet connection by
 * creating and storing the dataset on the cloud (via the Target Manager) as opposed
 * to storing the dataset within your project.
 *
 * This class does high-level handling of the Vuforia lifecycle and any UI updates
 *
 * For CloudReco-specific rendering, check out CloudRecoRenderer.java
 * For the low-level Vuforia lifecycle code, check out SampleApplicationSession.java
 */
public class CloudReco2 extends Activity implements SampleApplicationControl
{
    private static final String LOGTAG = "CloudReco";

    private SampleApplicationSession vuforiaAppSession;

    private SampleApplicationGLView mGlView;

    private CloudRecoRenderer mRenderer;

    private boolean mFinderStarted = false;
    private boolean mResetTargetFinderTrackables = false;

    // The textures we will use for rendering:
    private Vector<Texture> mTextures;

    // The keys necessary in order to use Cloud Recognition
    // Generate your own keys via the Target Manager on the Vuforia developer website
    private static final String kAccessKey = "41c1820fa857bd454e463d285af690c7d151765a";
    private static final String kSecretKey = "d81217a51cd094008b2074640c276a5f6be5c831";
    //private static final String kAccessKey = "7f0e89e71629c9504a8ebe5b5086d50bf7281e81";
    //private static final String kSecretKey = "f164075a59d802f12caa27337399515af29ee007";

    // The TargetFinder is used to dynamically search
    // for targets using an internet connection
    private TargetFinder mTargetFinder;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);

        vuforiaAppSession = new SampleApplicationSession(this);
        vuforiaAppSession
                .initAR(this, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mTextures = new Vector<>();
        loadTextures();
    }

    // Load specific textures from the APK, which we will later use for rendering.
    private void loadTextures()
    {
        mTextures.add(Texture.loadTextureFromApk("TextureTeapotRed.png", getAssets()));
    }


    @Override
    protected void onResume()
    {
        Log.d(LOGTAG, "onResume");
        super.onResume();

        vuforiaAppSession.onResume();
    }


    // Called whenever the device orientation or screen resolution changes
    @Override
    public void onConfigurationChanged(Configuration config)
    {
        Log.d(LOGTAG, "onConfigurationChanged");
        super.onConfigurationChanged(config);

        vuforiaAppSession.onConfigurationChanged();
    }


    @Override
    protected void onPause()
    {
        Log.d(LOGTAG, "onPause");
        super.onPause();

        vuforiaAppSession.onPause();

        if (mGlView != null)
        {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }
    }


    @Override
    protected void onDestroy()
    {
        Log.d(LOGTAG, "onDestroy");
        super.onDestroy();

        deinitCloudReco();

        try
        {
            vuforiaAppSession.stopAR();
        } catch (SampleApplicationException e)
        {
            Log.e(LOGTAG, e.getString());
        }

        System.gc();
    }


    private void deinitCloudReco()
    {
        if (mTargetFinder == null)
        {
            Log.e(LOGTAG,
                    "Could not deinit cloud reco because it was not initialized");
            return;
        }

        mTargetFinder.deinit();
    }

    private void initApplicationAR()
    {
        // Create OpenGL ES view:
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();

        // Initialize the GLView with proper flags
        mGlView = new SampleApplicationGLView(this);
        mGlView.init(translucent, depthSize, stencilSize);

        // Sets up the CloudRenderer of the GLView
        mRenderer = new CloudRecoRenderer(vuforiaAppSession, this);
        mRenderer.setTextures(mTextures);
        mGlView.setRenderer(mRenderer);
    }

    public void startFinderIfStopped()
    {
        if(!mFinderStarted)
        {
            if (mTargetFinder == null)
            {
                Log.e(LOGTAG, "Tried to start TargetFinder but was not initialized");
                return;
            }

            mTargetFinder.clearTrackables();
            mTargetFinder.startRecognition();

            mFinderStarted = true;
        }
    }


    public void stopFinderIfStarted()
    {
        if(mFinderStarted)
        {
            if (mTargetFinder == null)
            {
                Log.e(LOGTAG, "Tried to stop TargetFinder but was not initialized");
                return;
            }

            mTargetFinder.stop();

            mFinderStarted = false;
        }
    }


    @Override
    public boolean doLoadTrackersData()
    {
        Log.d(LOGTAG, "initCloudReco");

        // Get the object tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());

        // Start the target finder using keys
        TargetFinder targetFinder = objectTracker.getTargetFinder();
        targetFinder.startInit(kAccessKey, kSecretKey);

        targetFinder.waitUntilInitFinished();

        int resultCode = targetFinder.getInitState();
        if (resultCode != TargetFinder.INIT_SUCCESS)
        {
            Log.e(LOGTAG, "Failed to initialize target finder.");
            return false;
        }

        mTargetFinder = targetFinder;

        return true;
    }


    @Override
    public boolean doUnloadTrackersData()
    {
        return true;
    }

    @Override
    public void onVuforiaResumed()
    {
        if (mGlView != null)
        {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }
    }


    // Called once Vuforia has been initialized or
    // an error has caused Vuforia initialization to stop
    @Override
    public void onInitARDone(SampleApplicationException exception)
    {

        if (exception == null)
        {
            initApplicationAR();

            mRenderer.setActive(true);

            // Now add the GL surface view. It is important
            // that the OpenGL ES surface view gets added
            // BEFORE the camera is started and video
            // background is configured.
            addContentView(mGlView, new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));

            vuforiaAppSession.startAR(CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT);

        }
    }


    @Override
    public void onVuforiaStarted()
    {
        mRenderer.updateRenderingPrimitives();

        // Set camera focus mode
        if(!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO))
        {
            // If continuous autofocus mode fails, attempt to set to a different mode
            if(!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO))
            {
                CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL);
            }
        }
    }

    // Called every frame
    @Override
    public void onVuforiaUpdate(State state)
    {

        TargetFinder finder = mTargetFinder;
        if (finder == null)
        {
            Log.e(LOGTAG, "Tried to query TargetFinder but was not initialized");
            return;
        }

        // Check if there are new results available:
        TargetFinderQueryResult queryResult = finder.updateQueryResults();
        int queryStatus = queryResult.getStatus();

        // Show a message if we encountered an error:
        if (queryStatus < 0)
        {

        }
        else if (queryStatus == TargetFinder.UPDATE_RESULTS_AVAILABLE)
        {
            TargetSearchResultList queryResultsList = queryResult.getResults();

            // Process new search results
            if (!queryResultsList.empty())
            {
                TargetSearchResult result = queryResultsList.at(0);

                // Check if this target is suitable for tracking:
                if (result.getTrackingRating() > 0)
                {
                    finder.enableTracking(result);
                }
            }
        }

        if(mResetTargetFinderTrackables)
        {
            finder.clearTrackables();
            mResetTargetFinderTrackables = false;
        }
    }


    @Override
    public boolean doInitTrackers()
    {
        // Indicate if the trackers were initialized correctly
        boolean result = true;

        // For CloudReco, the recommended fusion provider mode is
        // the one recommended by the FUSION_OPTIMIZE_IMAGE_TARGETS_AND_VUMARKS enum
        if (!vuforiaAppSession.setFusionProviderType(
                FUSION_PROVIDER_TYPE.FUSION_OPTIMIZE_IMAGE_TARGETS_AND_VUMARKS))
        {
            return false;
        }

        TrackerManager tManager = TrackerManager.getInstance();
        Tracker tracker;

        tracker = tManager.initTracker(ObjectTracker.getClassType());
        if (tracker == null)
        {
            Log.e(LOGTAG,
                    "Tracker not initialized. Tracker already initialized or the camera is already started");
            result = false;
        }
        else
        {
            Log.i(LOGTAG, "Tracker successfully initialized");
        }

        // Initialize the Positional Device Tracker
        PositionalDeviceTracker deviceTracker = (PositionalDeviceTracker)
                tManager.initTracker(PositionalDeviceTracker.getClassType());

        if (deviceTracker != null)
        {
            Log.i(LOGTAG, "Successfully initialized Device Tracker");
        }
        else
        {
            Log.e(LOGTAG, "Failed to initialize Device Tracker");
        }

        return result;
    }


    @Override
    public boolean doStartTrackers()
    {
        // Indicate if the trackers were started correctly
        boolean result = true;

        if (mTargetFinder == null)
        {
            Log.e(LOGTAG, "Tried to start TargetFinder but was not initialized");
            return false;
        }

        mTargetFinder.startRecognition();
        mFinderStarted = true;

        // Start the Object Tracker
        // The Object Tracker tracks the target recognized by the target finder
        Tracker objectTracker = TrackerManager.getInstance().getTracker(
                ObjectTracker.getClassType());

        if (objectTracker != null)
        {
            result = objectTracker.start();
        }

        return result;
    }


    @Override
    public boolean doStopTrackers()
    {
        // Indicate if the trackers were stopped correctly
        boolean result = true;

        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());

        if(objectTracker != null)
        {
            objectTracker.stop();

            if (mTargetFinder == null)
            {
                Log.e(LOGTAG, "Tried to stop TargetFinder but was not initialized");
                return false;
            }

            TargetFinder targetFinder = mTargetFinder;
            targetFinder.stop();
            mFinderStarted = false;

            // Clears the trackables
            targetFinder.clearTrackables();
        }
        else
        {
            result = false;
        }

        return result;
    }


    @Override
    public boolean doDeinitTrackers()
    {
        TrackerManager tManager = TrackerManager.getInstance();

        // Indicate if the trackers were deinitialized correctly
        boolean result = tManager.deinitTracker(ObjectTracker.getClassType());
        tManager.deinitTracker(PositionalDeviceTracker.getClassType());

        return result;
    }

}