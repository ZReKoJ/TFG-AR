package com.ucm.tfg.cinema;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

public class OpenglActivityRenderer extends OpenGLRenderer {

    private static String LOGTAG = "Renderer";

    private Polygon polygon;
    private Square square;

    private float[] projectionMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] mvpMatrix = new float[16];

    @Override
    public void onCreate(int width, int height, boolean contextLost) {
        Log.i(LOGTAG, "width: " + width + " height: " + height);

        GLES20.glViewport(0, 0, width, height);
        GLES20.glClearColor(1f, 1f, 1f, 1f);

        polygon = new Polygon();
        square = new Square();

        float ratio = (float) width / height;
        Matrix.frustumM(projectionMatrix, 0 , -ratio, ratio, -1, 1, 3, 7);
    }

    @Override
    public void onDrawFrame(boolean firstDraw) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        Matrix.setLookAtM(viewMatrix, 0, 0, 0, 3, 0f, 0f, 0f, 0f, 1f, 0f);

        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        square.draw(mvpMatrix);
        polygon.draw(mvpMatrix);
    }
}
