package com.ucm.tfg.cinema.Tests

import android.app.ActionBar
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.ucm.tfg.cinema.SampleApplication.SampleApplicationControl
import com.ucm.tfg.cinema.SampleApplication.SampleApplicationException
import com.ucm.tfg.cinema.SampleApplication.SampleApplicationSession
import com.ucm.tfg.cinema.SampleApplication.utils.SampleApplicationGLView
import com.ucm.tfg.cinema.SampleApplication.utils.Texture
import com.vuforia.*
import java.util.*


class CloudRecoActivity : Activity(), SampleApplicationControl {

    private val LOGTAG = "CloudRecoActivity"
    private val ACCESS_KEY = "41c1820fa857bd454e463d285af690c7d151765a"
    private val SECRET_KEY = "d81217a51cd094008b2074640c276a5f6be5c831"

    private lateinit var vuforiaAppSession : SampleApplicationSession
    private lateinit var graphics : SampleApplicationGLView
    private lateinit var renderer : CloudRenderer
    private lateinit var finder : TargetFinder

    private var finderStarted = false

    // texture name - texture
    private var textures = HashMap<String, Texture>()
    // target name - texture name
    private var targets = HashMap<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vuforiaAppSession = SampleApplicationSession(this)
        vuforiaAppSession.initAR(this, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

        loadData()
    }

    override fun onResume() {
        super.onResume()
        vuforiaAppSession.onResume()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        vuforiaAppSession.onConfigurationChanged()
    }

    override fun onPause() {
        super.onPause()

        graphics.visibility = View.VISIBLE
        graphics.onPause()

        vuforiaAppSession.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()

        finder.deinit()

        try {
            vuforiaAppSession.stopAR()
        } catch (e : SampleApplicationException) {
            Log.e(LOGTAG, e.string)
        }

        textures.clear()
        targets.clear()

        System.gc()
    }

    private fun loadData() {
        targets["stones"] = "teapotBrass"
        targets["chips"] = "teapotBlue"
        targets["tarmac"] = "teapotRed"
        targets["luffy"] = "teapotBrass"
        targets["zoro"] = "teapotBlue"
        targets["nami"] = "teapotRed"
        targets["franky"] = "teapotBrass"
        targets["jinbei"] = "teapotBlue"
        targets["sanji"] = "teapotRed"
        targets["brook"] = "teapotBrass"
        targets["usopp"] = "teapotBlue"
        targets["chopper"] = "teapotRed"
        targets["robin"] = "teapotBrass"
        targets["Z"] = "plane"

        textures["teapotBrass"] = Texture.loadTextureFromApk( "TextureTeapotBrass.png", assets)
        textures["teapotBlue"] = Texture.loadTextureFromApk( "TextureTeapotBlue.png", assets)
        textures["teapotRed"] = Texture.loadTextureFromApk( "TextureTeapotRed.png", assets)
        textures["plane"] = Texture.loadTextureFromApk("vumark_texture.png", assets)
    }

    override fun doInitTrackers(): Boolean {
        if (!vuforiaAppSession.setFusionProviderType(FUSION_PROVIDER_TYPE.FUSION_OPTIMIZE_IMAGE_TARGETS_AND_VUMARKS)){
            return false
        }

        val trackerManager = TrackerManager.getInstance()
        var objectTracker = trackerManager.initTracker(ObjectTracker.getClassType()) as ObjectTracker

        if (objectTracker == null) {
            Log.e(LOGTAG,  "Tracker not initialized. Tracker already initialized or the camera is already started")
            return false
        }

        return true
    }

    override fun doLoadTrackersData(): Boolean {
        val trackerManager = TrackerManager.getInstance()
        var objectTracker = trackerManager.getTracker(ObjectTracker.getClassType()) as ObjectTracker
        var finder = objectTracker.targetFinder as TargetFinder
        finder.startInit(ACCESS_KEY, SECRET_KEY)
        finder.waitUntilInitFinished()

        if (finder.initState != TargetFinder.INIT_SUCCESS){
            Log.e(LOGTAG, "Failed to initialize target finder.");
            return false
        }

        this.finder = finder

        return true
    }

    override fun doStartTrackers(): Boolean {
        val trackerManager = TrackerManager.getInstance()
        var objectTracker = trackerManager.getTracker(ObjectTracker.getClassType()) as ObjectTracker

        if (finder == null) {
            Log.e(LOGTAG, "Tried to start TargetFinder but was not initialized");
            return false
        }

        finder.startRecognition()
        finderStarted = true

        if (objectTracker == null || !objectTracker.start()) {
            Log.e(LOGTAG, "Failed to start Object Tracker")
            return false
        }

        return true
    }

    override fun doStopTrackers(): Boolean {
        val trackerManager = TrackerManager.getInstance()
        var objectTracker = trackerManager.getTracker(ObjectTracker.getClassType()) as ObjectTracker

        if (objectTracker != null) {
            objectTracker.stop()

            if (finder == null) {
                Log.e(LOGTAG, "Tried to stop TargetFinder but was not initialized");
                return false
            }

            var finder = this.finder as TargetFinder
            finder.stop()
            finderStarted = false
            finder.clearTrackables()
        }
        else {
            Log.e(LOGTAG, "Failed to start Object Tracker")
            return false
        }

        objectTracker.stop()

        return true
    }

    override fun doUnloadTrackersData(): Boolean {
        return true
    }

    override fun doDeinitTrackers(): Boolean {
        return TrackerManager.getInstance().deinitTracker(ObjectTracker.getClassType())
    }

    private fun initApplicationAR() {
        val depthSize = 16
        val stencilSize = 0
        val translucent = Vuforia.requiresAlpha()

        graphics = SampleApplicationGLView(this)
        graphics.init(translucent, depthSize, stencilSize)

        renderer = CloudRenderer(vuforiaAppSession, this)

        var t = Vector<Texture>()
        textures.forEach {
            t.add(it.value)
        }

        renderer.setTextures(textures)
        renderer.setTargets(targets)

        graphics.setRenderer(renderer)

    }

    fun startFinderIfStopped(){
        if(!finderStarted) {
            finder.clearTrackables()
            finder.startRecognition()
            finderStarted = true
        }
    }

    fun stopFinderIfStarted() {
        finder.stop()
        finderStarted = false
    }

    override fun onInitARDone(e: SampleApplicationException?) {
        if (e == null) {
            initApplicationAR()

            renderer.setActive(true)
            addContentView(graphics, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT))

            vuforiaAppSession.startAR(CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT)
        } else {
            Log.e(LOGTAG, e.string)
        }
    }

    override fun onVuforiaUpdate(state: State?) {
        var finder = this.finder as TargetFinder

        var queryResult = finder.updateQueryResults()
        if (queryResult.status == TargetFinder.UPDATE_RESULTS_AVAILABLE) {
            var queryResultsList = queryResult.results

            if (!queryResultsList.empty()) {
                val result = queryResultsList.at(0)

                if (result.trackingRating > 0) {
                    finder.enableTracking(result)
                }
            }
        }
    }

    override fun onVuforiaResumed() {
        graphics.visibility = View.VISIBLE
        graphics.onResume()
    }

    override fun onVuforiaStarted() {
        renderer.updateRenderingPrimitives()

        if(!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO))
        {
            if(!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO))
            {
                CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL);
            }
        }
    }

}
