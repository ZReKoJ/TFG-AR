package com.ucm.tfg.cinema

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

class ImageTargetActivity : Activity(), SampleApplicationControl {

    private val LOGTAG = "ImageTargetActivity"

    private lateinit var vuforiaAppSession : SampleApplicationSession
    private lateinit var graphics : SampleApplicationGLView
    private lateinit var renderer : ImageRenderer

    // xml name - dataset
    private var databases = HashMap<String, DataSet?>()
    // target name - texture name
    private var targets = HashMap<String, String>()
    // texture name - texture
    private var textures = HashMap<String, Texture>()

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

        try {
            vuforiaAppSession.stopAR()
        } catch (e : SampleApplicationException) {
            Log.e(LOGTAG, e.string)
        }

        databases.clear()
        targets.clear()
        textures.clear()

        System.gc()
    }

    private fun loadData() {
        databases["OnePiece.xml"] = null
        databases["StonesAndChips.xml"] = null
        databases["Tarmac.xml"] = null

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
        targets["Z"] = "teapotBlue"

        textures["teapotBrass"] = Texture.loadTextureFromApk( "TextureTeapotBrass.png", assets)
        textures["teapotBlue"] = Texture.loadTextureFromApk( "TextureTeapotBlue.png", assets)
        textures["teapotRed"] = Texture.loadTextureFromApk( "TextureTeapotRed.png", assets)
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

        if (objectTracker == null){
            return false
        }

        databases.forEach { (key, value) ->
            if (value == null) {
                databases[key] = objectTracker.createDataSet()
            }
            if (databases[key]?.load(key, STORAGE_TYPE.STORAGE_APPRESOURCE) == false) {
                return false
            }
            if (!objectTracker.activateDataSet(databases[key])) {
                return false
            }
        }

        return true
    }

    override fun doStartTrackers(): Boolean {
        val trackerManager = TrackerManager.getInstance()

        var objectTracker = trackerManager.getTracker(ObjectTracker.getClassType()) as ObjectTracker

        if (objectTracker == null || !objectTracker.start()) {
            Log.e(LOGTAG, "Failed to start Object Tracker")
            return false
        }

        return true
    }

    override fun doStopTrackers(): Boolean {
        val trackerManager = TrackerManager.getInstance()

        var objectTracker = trackerManager.getTracker(ObjectTracker.getClassType()) as ObjectTracker

        if (objectTracker == null) {
            Log.e(LOGTAG, "Failed to start Object Tracker")
            return false
        }

        objectTracker.stop()

        return true
    }

    override fun doUnloadTrackersData(): Boolean {
        val trackerManager = TrackerManager.getInstance()
        var objectTracker = trackerManager.getTracker(ObjectTracker.getClassType()) as ObjectTracker?

        if (objectTracker == null){
            return false
        }

        databases.forEach { (key, value) ->
            if (value != null && value.isActive){
                if (!objectTracker.destroyDataSet(value)){
                    return false
                }
                databases[key] = null
            }
        }

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

        renderer = ImageRenderer(this, vuforiaAppSession)

        renderer.setTextures(textures)
        renderer.setTargets(targets)

        graphics.setRenderer(renderer)

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
        // nothing
    }

    override fun onVuforiaResumed() {
        graphics.visibility = View.VISIBLE
        graphics.onResume()
    }

    override fun onVuforiaStarted() {
        renderer.updateRenderingPrimitives()
        renderer.updateConfiguration()
    }

}