package com.ucm.tfg.cinema;

import android.opengl.GLSurfaceView;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public abstract class OpenGLRenderer implements GLSurfaceView.Renderer {

    private final String LOGTAG = "OpenGLRenderer";

    private Boolean firstDraw;
    private Boolean surfaceCreated;

    private int width;
    private int height;

    private long lastTime;
    private int framePerSecond;

    public OpenGLRenderer() {
        Log.i(LOGTAG, "Class created");

        firstDraw = true;

        surfaceCreated = false;

        width = -1;
        height = -1;

        lastTime = System.currentTimeMillis();
        framePerSecond = 0;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.i(LOGTAG, "Surface created.");

        surfaceCreated = true;
        width = -1;
        height = -1;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (!this.surfaceCreated && width == this.width && height == this.height) {
            Log.i(LOGTAG, "Surface changed but already handled.");
            return;
        }
        String msg = "Surface changed width:" + width + " height:" + height;
        if (surfaceCreated) {
            msg += " context lost.";
        } else {
            msg += ".";
        }
        Log.i(LOGTAG, msg);

        this.width = width;
        this.height = height;
        onCreate(this.width, this.height, this.surfaceCreated);
        this.surfaceCreated = false;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        onDrawFrame(this.firstDraw);

        framePerSecond++;
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTime >= 1000) {
            framePerSecond = 0;
            lastTime = currentTime;
        }

        if(this.firstDraw){
            this.firstDraw = false;
        }
    }

    public int getFramePerSecond(){
        return framePerSecond;
    }

    public abstract void onCreate(int width, int height, boolean contextLost);

    public abstract void onDrawFrame(boolean firstDraw);
}
