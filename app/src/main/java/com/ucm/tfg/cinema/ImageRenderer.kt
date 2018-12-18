package com.ucm.tfg.cinema

import android.opengl.GLSurfaceView
import com.ucm.tfg.cinema.SampleApplication.SampleAppRenderer
import com.ucm.tfg.cinema.SampleApplication.SampleAppRendererControl
import com.ucm.tfg.cinema.SampleApplication.SampleApplicationSession
import java.lang.ref.WeakReference
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import com.ucm.tfg.cinema.SampleApplication.utils.Texture
import com.google.a.b.a.a.a.e
import com.ucm.tfg.cinema.SampleApplication.utils.SampleApplication3DModel
import com.ucm.tfg.cinema.SampleApplication.utils.Teapot
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.ucm.tfg.cinema.SampleApplication.utils.CubeShaders
import com.ucm.tfg.cinema.SampleApplication.utils.SampleUtils
import java.io.IOException
import android.opengl.Matrix.multiplyMM
import com.ucm.tfg.cinema.SampleApplication.utils.MeshObject
import com.vuforia.Tool.convertPose2GLMatrix
import com.ucm.tfg.cinema.SampleApplication.utils.SampleMath
import com.vuforia.*


class ImageRenderer constructor(activity: ImageTargetActivity, session: SampleApplicationSession): GLSurfaceView.Renderer, SampleAppRendererControl {

    private val LOGTAG = "ImageRenderer"

    private val vuforiaAppSession : SampleApplicationSession
    private val activity : WeakReference<ImageTargetActivity>
    private val renderer : SampleAppRenderer

    init {
        this.vuforiaAppSession = session
        this.activity = WeakReference(activity)

        this.renderer = SampleAppRenderer(this, this.activity.get(), Device.MODE.MODE_AR,false, 0.01f , 5f)
    }

    private lateinit var textures: HashMap<String, Texture>
    private lateinit var targets: HashMap<String, String>

    private lateinit var teapot : Teapot

    fun setTextures(textures: HashMap<String, Texture>) {
        this.textures = textures
    }

    fun setTargets(targets: HashMap<String, String>) {
        this.targets = targets
    }

    private var shaderProgramID: Int = 0
    private var vertexHandle: Int = 0
    private var textureCoordHandle: Int = 0
    private var mvpMatrixHandle: Int = 0
    private var texSampler2DHandle: Int = 0
    private val OBJECT_SCALE_FLOAT = 0.003f

    private var isActive = false
    private var modelIsLoaded = false

    fun updateRenderingPrimitives() {
        renderer.updateRenderingPrimitives()
    }

    fun setActive(active: Boolean) {
        isActive = active

        if (isActive) {
            renderer.configureVideoBackground()
        }
    }

    fun updateConfiguration() {
        renderer.onConfigurationChanged(isActive)
    }

    private fun initRendering() {
        GLES20.glClearColor(
            0.0f, 0.0f, 0.0f, if (Vuforia.requiresAlpha())
                0.0f
            else
                1.0f
        )

        textures.forEach {(key, value) ->
            GLES20.glGenTextures(1, value.mTextureID, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, value.mTextureID[0])
            GLES20.glTexParameterf(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat()
            )
            GLES20.glTexParameterf(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat()
            )
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                value.mWidth, value.mHeight, 0, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, value.mData
            )
        }

        shaderProgramID = SampleUtils.createProgramFromShaderSrc(
            CubeShaders.CUBE_MESH_VERTEX_SHADER,
            CubeShaders.CUBE_MESH_FRAGMENT_SHADER
        )

        vertexHandle = GLES20.glGetAttribLocation(
            shaderProgramID,
            "vertexPosition"
        )
        textureCoordHandle = GLES20.glGetAttribLocation(
            shaderProgramID,
            "vertexTexCoord"
        )
        mvpMatrixHandle = GLES20.glGetUniformLocation(
            shaderProgramID,
            "modelViewProjectionMatrix"
        )
        texSampler2DHandle = GLES20.glGetUniformLocation(
            shaderProgramID,
            "texSampler2D"
        )

        if (!modelIsLoaded) {
            teapot = Teapot()
        }
    }

    private fun renderModel(
        projectionMatrix: FloatArray?,
        viewMatrix: FloatArray,
        modelMatrix: FloatArray,
        texture: Texture?
    ) {
        val model: MeshObject
        val modelViewProjection = FloatArray(16)

        Matrix.translateM(modelMatrix, 0, 0.0f, 0.0f, OBJECT_SCALE_FLOAT)
        Matrix.scaleM(modelMatrix, 0, OBJECT_SCALE_FLOAT, OBJECT_SCALE_FLOAT, OBJECT_SCALE_FLOAT)

        model = teapot

        // Combine device pose (view matrix) with model matrix
        Matrix.multiplyMM(modelMatrix, 0, viewMatrix, 0, modelMatrix, 0)

        // Do the final combination with the projection matrix
        Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelMatrix, 0)

        // Activate the shader program and bind the vertex and tex coords
        GLES20.glUseProgram(shaderProgramID)

        GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, model.vertices)
        GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, model.texCoords)

        GLES20.glEnableVertexAttribArray(vertexHandle)
        GLES20.glEnableVertexAttribArray(textureCoordHandle)

        // Activate texture 0, bind it, pass to shader
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture!!.mTextureID[0])
        GLES20.glUniform1i(texSampler2DHandle, 0)

        // Pass the model view matrix to the shader
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0)

        // Finally draw the model
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, model.numObjectIndex, GLES20.GL_UNSIGNED_SHORT, model.indices)

        // Disable the enabled arrays
        GLES20.glDisableVertexAttribArray(vertexHandle)
        GLES20.glDisableVertexAttribArray(textureCoordHandle)
    }

    override fun onDrawFrame(gl: GL10?) {
        if (isActive) {
            renderer.render()
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        vuforiaAppSession.onSurfaceChanged(width, height)
        renderer.onConfigurationChanged(isActive)
        initRendering()
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        vuforiaAppSession.onSurfaceCreated();
        renderer.onSurfaceCreated();
    }

    override fun renderFrame(state: State?, projectionMatrix: FloatArray?) {
        // Renders video background replacing Renderer.DrawVideoBackground()
        renderer.renderVideoBackground(state)

        // Set the device pose matrix as identity
        var devicePoseMattix = SampleMath.Matrix44FIdentity()
        var modelMatrix: Matrix44F

        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        // handle face culling, we need to detect if we are using reflection
        // to determine the direction of the culling
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)

        // Read device pose from the state and create a corresponding view matrix (inverse of the device pose)
        if (state!!.getDeviceTrackableResult() != null && state!!.getDeviceTrackableResult().status !== TrackableResult.STATUS.NO_POSE) {
            modelMatrix = Tool.convertPose2GLMatrix(state!!.getDeviceTrackableResult().pose)

            // We transpose here because Matrix44FInverse returns a transposed matrix
            devicePoseMattix = SampleMath.Matrix44FTranspose(SampleMath.Matrix44FInverse(modelMatrix))
        }

        // Did we find any trackables this frame?
        val trackableResultList = state!!.getTrackableResults()
        for (result in trackableResultList) {
            val trackable = result.trackable

            if (result.isOfType(ImageTargetResult.getClassType())) {
                modelMatrix = Tool.convertPose2GLMatrix(result.pose)

                renderModel(
                    projectionMatrix,
                    devicePoseMattix.data,
                    modelMatrix.data,
                    textures.get(targets[trackable.name])
                )

                SampleUtils.checkGLError("Image Targets renderFrame")
            }
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
    }

}