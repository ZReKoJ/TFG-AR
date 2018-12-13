package com.ucm.tfg.cinema;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import com.ucm.tfg.cinema.SampleApplication.SampleAppRenderer;
import com.ucm.tfg.cinema.SampleApplication.SampleAppRendererControl;
import com.ucm.tfg.cinema.SampleApplication.SampleApplicationSession;
import com.ucm.tfg.cinema.SampleApplication.utils.*;
import com.vuforia.*;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Vector;

/**
 * The renderer class for the CylinderTargets sample.
 *
 * In the renderFrame() function you can render augmentations to display over the Target
 */
public class ImageRendererCopy implements GLSurfaceView.Renderer, SampleAppRendererControl
{
    private static final String LOGTAG = "ImageRendererCopy";

    private final SampleApplicationSession vuforiaAppSession;
    private final WeakReference<ImageTargetActivity> mActivityRef;
    private final SampleAppRenderer mSampleAppRenderer;

    private HashMap<String, Texture> textures;
    private HashMap<String, String> targets;

    private int shaderProgramID;
    private int vertexHandle;
    private int textureCoordHandle;
    private int mvpMatrixHandle;
    private int texSampler2DHandle;

    // Object to be rendered
    private Teapot mTeapot;

    private SampleApplication3DModel mBuildingsModel;

    private boolean mIsActive = false;
    private boolean mModelIsLoaded = false;

    private static final float OBJECT_SCALE_FLOAT = 0.003f;

    ImageRendererCopy(ImageTargetActivity activity, SampleApplicationSession session)
    {
        mActivityRef = new WeakReference<>(activity);
        vuforiaAppSession = session;

        // SampleAppRenderer used to encapsulate the use of RenderingPrimitives setting
        // the device mode AR/VR and stereo mode
        mSampleAppRenderer = new SampleAppRenderer(this, mActivityRef.get(), Device.MODE.MODE_AR,false, 0.01f , 5f);
    }


    @Override
    public void onDrawFrame(GL10 gl)
    {
        if (!mIsActive)
            return;

        // Call our function to render content from SampleAppRenderer class
        mSampleAppRenderer.render();
    }


    public void updateRenderingPrimitives()
    {
        mSampleAppRenderer.updateRenderingPrimitives();
    }


    public void setActive(boolean active)
    {
        mIsActive = active;

        if(mIsActive)
            mSampleAppRenderer.configureVideoBackground();
    }


    // Called when the surface is created or recreated.
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config)
    {
        Log.d(LOGTAG, "GLRenderer.onSurfaceCreated");

        // Call Vuforia function to (re)initialize rendering after first use
        // or after OpenGL ES context was lost (e.g. after onPause/onResume):
        vuforiaAppSession.onSurfaceCreated();

        mSampleAppRenderer.onSurfaceCreated();
    }


    // Called when the surface changes size.
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height)
    {
        Log.d(LOGTAG, "GLRenderer.onSurfaceChanged");

        // Call Vuforia function to handle render surface size changes:
        vuforiaAppSession.onSurfaceChanged(width, height);

        // RenderingPrimitives to be updated when some rendering change is done
        mSampleAppRenderer.onConfigurationChanged(mIsActive);

        initRendering();
    }


    private void initRendering()
    {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
                : 1.0f);

        for (HashMap.Entry<String, Texture> entry : textures.entrySet())
        {
            GLES20.glGenTextures(1, entry.getValue().mTextureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, entry.getValue().mTextureID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                    entry.getValue().mWidth, entry.getValue().mHeight, 0, GLES20.GL_RGBA,
                    GLES20.GL_UNSIGNED_BYTE, entry.getValue().mData);
        }

        shaderProgramID = SampleUtils.createProgramFromShaderSrc(
                CubeShaders.CUBE_MESH_VERTEX_SHADER,
                CubeShaders.CUBE_MESH_FRAGMENT_SHADER);

        vertexHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexPosition");
        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexTexCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "modelViewProjectionMatrix");
        texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "texSampler2D");

        if(!mModelIsLoaded) {
            mTeapot = new Teapot();

            try {
                mBuildingsModel = new SampleApplication3DModel();
                mBuildingsModel.loadModel(mActivityRef.get().getResources().getAssets(),
                        "ImageTargets/Buildings.txt");
                mModelIsLoaded = true;
            } catch (IOException e) {
                Log.e(LOGTAG, "Unable to load buildings");
            }

            // Hide the Loading Dialog
            //mActivityRef.get().loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
        }
    }

    public void updateConfiguration()
    {
        mSampleAppRenderer.onConfigurationChanged(mIsActive);
    }

    // The render function.
    // This function is called from the SampleAppRenderer by using the RenderingPrimitives views.
    // The state is owned by SampleAppRenderer which is controlling its lifecycle.
    // NOTE: State should not be cached outside this method.
    public void renderFrame(State state, float[] projectionMatrix)
    {
        // Renders video background replacing Renderer.DrawVideoBackground()
        mSampleAppRenderer.renderVideoBackground(state);

        // Set the device pose matrix as identity
        Matrix44F devicePoseMattix = SampleMath.Matrix44FIdentity();
        Matrix44F modelMatrix;

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // handle face culling, we need to detect if we are using reflection
        // to determine the direction of the culling
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);

        // Read device pose from the state and create a corresponding view matrix (inverse of the device pose)
        if (state.getDeviceTrackableResult() != null
                && state.getDeviceTrackableResult().getStatus() != TrackableResult.STATUS.NO_POSE)
        {
            modelMatrix = Tool.convertPose2GLMatrix(state.getDeviceTrackableResult().getPose());

            // We transpose here because Matrix44FInverse returns a transposed matrix
            devicePoseMattix = SampleMath.Matrix44FTranspose(SampleMath.Matrix44FInverse(modelMatrix));
        }

        // Did we find any trackables this frame?
        TrackableResultList trackableResultList = state.getTrackableResults();
        for (TrackableResult result : trackableResultList)
        {
            Trackable trackable = result.getTrackable();

            if (result.isOfType(ImageTargetResult.getClassType()))
            {
                int textureIndex = 0;
                modelMatrix = Tool.convertPose2GLMatrix(result.getPose());

                Log.e(LOGTAG, trackable.getName());
                switch (trackable.getName()) {
                    case "stones": textureIndex = 0; break;
                    case "chips": textureIndex = 1; break;
                    case "tarmac": textureIndex = 2; break;
                    case "luffy": textureIndex = 1; break;
                    case "zoro": textureIndex = 0; break;
                    case "nami": textureIndex = 0; break;
                    case "franky": textureIndex = 0; break;
                    case "jinbei": textureIndex = 0; break;
                    case "sanji": textureIndex = 0; break;
                    case "brook": textureIndex = 0; break;
                    case "usopp": textureIndex = 0; break;
                    case "chopper": textureIndex = 0; break;
                    case "robin": textureIndex = 0; break;
                    case "Z": textureIndex = 0; break;
                }

                renderModel(projectionMatrix, devicePoseMattix.getData(), modelMatrix.getData(), textures.get(targets.get(trackable.getName())));

                SampleUtils.checkGLError("Image Targets renderFrame");
            }
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
    }


    private void renderModel(float[] projectionMatrix, float[] viewMatrix, float[] modelMatrix, Texture texture)
    {
        MeshObject model;
        float[] modelViewProjection = new float[16];

        Matrix.translateM(modelMatrix, 0, 0, 0, OBJECT_SCALE_FLOAT);
        Matrix.scaleM(modelMatrix, 0, OBJECT_SCALE_FLOAT, OBJECT_SCALE_FLOAT, OBJECT_SCALE_FLOAT);

        model = mTeapot;

        // Combine device pose (view matrix) with model matrix
        Matrix.multiplyMM(modelMatrix, 0, viewMatrix, 0, modelMatrix, 0);

        // Do the final combination with the projection matrix
        Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelMatrix, 0);

        // Activate the shader program and bind the vertex and tex coords
        GLES20.glUseProgram(shaderProgramID);

        GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, model.getVertices());
        GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, model.getTexCoords());

        GLES20.glEnableVertexAttribArray(vertexHandle);
        GLES20.glEnableVertexAttribArray(textureCoordHandle);

        // Activate texture 0, bind it, pass to shader
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture.mTextureID[0]);
        GLES20.glUniform1i(texSampler2DHandle, 0);

        // Pass the model view matrix to the shader
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);

        // Finally draw the model
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, model.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT, model.getIndices());

        // Disable the enabled arrays
        GLES20.glDisableVertexAttribArray(vertexHandle);
        GLES20.glDisableVertexAttribArray(textureCoordHandle);
    }

    public void setTextures(HashMap<String, Texture> textures)
    {
        this.textures = textures;
    }

    public void setTargets(HashMap<String, String> targets) {
        this.targets = targets;
    }
}

