package com.ucm.tfg.cinema.Tests

import android.opengl.GLSurfaceView
import android.opengl.Matrix;
import com.ucm.tfg.cinema.SampleApplication.SampleAppRenderer
import com.ucm.tfg.cinema.SampleApplication.SampleAppRendererControl
import com.ucm.tfg.cinema.SampleApplication.SampleApplicationSession
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import android.opengl.GLES20
import com.vuforia.*
import com.ucm.tfg.cinema.Polygon
import com.ucm.tfg.cinema.SampleApplication.utils.*
import com.ucm.tfg.cinema.Square
import com.ucm.tfg.cinema.Tests.GLES20.GLES20RendererBuilder


class CloudRenderer constructor(session : SampleApplicationSession, activity : CloudRecoActivity) : GLSurfaceView.Renderer, SampleAppRendererControl {
    private val LOGTAG = "CloudRenderer"

    private val vuforiaAppSession : SampleApplicationSession
    private val activity : CloudRecoActivity
    private val renderer : SampleAppRenderer

    init {
        this.vuforiaAppSession = session
        this.activity = activity

        this.renderer = SampleAppRenderer(this, this.activity, Device.MODE.MODE_AR,false, 0.01f , 5f)
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

    private var isActive = false

    fun updateRenderingPrimitives() {
        renderer.updateRenderingPrimitives()
    }

    fun setActive(active: Boolean) {
        isActive = active

        if (isActive) {
            renderer.configureVideoBackground()
        }
    }

    private fun initRendering() {
        // Define clear color
        GLES20.glClearColor(
            0.0f, 0.0f, 0.0f, if (Vuforia.requiresAlpha())
                0.0f
            else
                1.0f
        )

        textures.forEach {
            GLES20.glGenTextures(1, it.value.mTextureID, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, it.value.mTextureID[0])
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
                it.value.mWidth, it.value.mHeight, 0, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, it.value.mData
            )
        }

        /*
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
        )*/

        //teapot = Teapot()
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

    override fun onDrawFrame(gl: GL10?) {
        if (isActive) {
            renderer.render()
        }
    }

    // The render function.
    // This function is called from the SampleAppRenderer by using the RenderingPrimitives views.
    // The state is owned by SampleAppRenderer which is controlling its lifecycle.
    // NOTE: State should not be cached outside this method.
    override fun renderFrame(state: State, projectionMatrix: FloatArray) {
        // Renders video background replacing CloudRenderer.DrawVideoBackground()
        renderer.renderVideoBackground(state)

        // Set the device pose matrix as identity
        var devicePoseMatrix = SampleMath.Matrix44FIdentity()
        var modelMatrix: Matrix44F

        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)

        // Start the target finder if we can't find an Image Target result.
        // If the Device pose exists, we can assume we will receive two
        // Trackable Results if the ImageTargetResult is available:
        // ImageTargetResult and DeviceTrackableResult
        val numExpectedResults = if (state.getDeviceTrackableResult() == null) 0 else 1
        if (state.getTrackableResults().size() <= numExpectedResults) {
            activity.startFinderIfStopped()
        }

        // Read device pose from the state and create a corresponding view matrix (inverse of the device pose)
        if (state.getDeviceTrackableResult() != null && state.getDeviceTrackableResult().getStatus() !== TrackableResult.STATUS.NO_POSE) {
            modelMatrix = Tool.convertPose2GLMatrix(state.getDeviceTrackableResult().getPose())

            // We transpose here because Matrix44FInverse returns a transposed matrix
            devicePoseMatrix = SampleMath.Matrix44FTranspose(SampleMath.Matrix44FInverse(modelMatrix))
        }

        // Did we find any trackables this frame?
        val trackableResultList = state.getTrackableResults()
        for (result in trackableResultList) {
            modelMatrix = Tool.convertPose2GLMatrix(result.getPose())

            if (result.isOfType(ImageTargetResult.getClassType())) {
                activity.stopFinderIfStarted()

                // Renders the augmentation
                renderModel(projectionMatrix, devicePoseMatrix.data, modelMatrix.data, textures.get(targets[result.trackable.name]))

                SampleUtils.checkGLError("CloudReco renderFrame")
            }
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)

        Renderer.getInstance().end()
    }

    private fun renderModel(projectionMatrix: FloatArray, viewMatrix: FloatArray, modelMatrix: FloatArray, texture: Texture?) {
        /*val modelViewProjection = FloatArray(16)

        // Apply local transformation to our model
        Matrix.translateM(modelMatrix, 0, 0.0f, 0.0f, OBJECT_SCALE_FLOAT)
        Matrix.scaleM(
            modelMatrix, 0, OBJECT_SCALE_FLOAT,
            OBJECT_SCALE_FLOAT, OBJECT_SCALE_FLOAT
        )

        // Combine device pose (view matrix) with model matrix
        Matrix.multiplyMM(modelMatrix, 0, viewMatrix, 0, modelMatrix, 0)

        // Do the final combination with the projection matrix
        Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelMatrix, 0)

        // activate the shader program and bind the vertex/normal/tex coords
        GLES20.glUseProgram(shaderProgramID)
        GLES20.glVertexAttribPointer(
            vertexHandle, 3, GLES20.GL_FLOAT, false,
            0, teapot.getVertices()
        )
        GLES20.glVertexAttribPointer(
            textureCoordHandle, 2, GLES20.GL_FLOAT,
            false, 0, teapot.getTexCoords()
        )

        GLES20.glEnableVertexAttribArray(vertexHandle)
        GLES20.glEnableVertexAttribArray(textureCoordHandle)

        // activate texture 0, bind it, and pass to shader
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(
            GLES20.GL_TEXTURE_2D,
            texture!!.mTextureID[0]
        )
        GLES20.glUniform1i(texSampler2DHandle, 0)

        // pass the model view matrix to the shader
        GLES20.glUniformMatrix4fv(
            mvpMatrixHandle, 1, false,
            modelViewProjection, 0
        )

        // finally draw the teapot
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES, teapot.getNumObjectIndex(),
            GLES20.GL_UNSIGNED_SHORT, teapot.getIndices()
        )

        // disable the enabled arrays
        GLES20.glDisableVertexAttribArray(vertexHandle)
        GLES20.glDisableVertexAttribArray(textureCoordHandle)*/

        val modelViewProjection = FloatArray(16)
        Matrix.multiplyMM(modelMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelMatrix, 0)

        val vertexShaderCode =
            "uniform mat4 uMVPMatrix;\n" +
            "attribute vec3 vPosition;\n" +
            "attribute vec3 vColor;\n" +
            "varying vec4 myColor;\n" +
            "void main()\n" +
            "{\n" +
                "gl_Position = uMVPMatrix * vec4(vPosition, 1.0f);\n" +
                "myColor = vec4(vColor, 1.0f);\n" +
            "}";

        val fragmentShaderCode =
            "precision mediump float;\n" +
            "varying vec4 myColor;\n" +
            "void main()\n" +
            "{\n" +
                "gl_FragColor = myColor;\n" +
            "}"

        val unit = 0.5f

        var coords = floatArrayOf(
            -unit, -unit, 0.0f,
            unit, -unit, 0.0f,
            -unit, unit, 0.0f
        )

        var colors = floatArrayOf(
            1f, 0f, 0f,
            0f, 1f, 0f,
            0f, 0f, 1f
        )

        var order = shortArrayOf(
            0, 1, 2
        )

        GLES20RendererBuilder()
            .addVertexShader(vertexShaderCode)
            .addFragmentShader(fragmentShaderCode)
            .build()
            .addFloatBuffer("vPosition", 3, false, coords)
            .addFloatBuffer("vColor", 3, false, colors)
            .addMatrix("uMVPMatrix", modelViewProjection)
            .draw(order)

    }
}