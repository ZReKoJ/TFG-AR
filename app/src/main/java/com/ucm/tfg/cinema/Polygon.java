package com.ucm.tfg.cinema;

import android.opengl.GLES20;

import javax.microedition.khronos.opengles.GL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Polygon {

    private String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
            "attribute vec4 vPosition;" +
            "void main() {" +
            "  gl_Position = uMVPMatrix * vPosition;" +
            "}";

    private String fragmentShaderCode =
            "void main()" +
            "{" +
            "   gl_FragColor = vec4(1.0f, 0.5f, 0.2f, 1.0f);" +
            "}";

    private int program;

    private float coords[] = {
            0.5f,  0.5f, 0.0f,
            0.5f, -0.5f, 0.0f,
            -0.5f,  0.5f, 0.0f
    };

    private short order[] = {
            0, 1, 2
    };

    public Polygon() {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
    }

    public void draw(float[] mvpMatrix) {
        GLES20.glUseProgram(program);

        int position = GLES20.glGetAttribLocation(program, "vPosition");
        GLES20.glEnableVertexAttribArray(position);
        GLES20.glVertexAttribPointer(position, 3, GLES20.GL_FLOAT, false, 3 * 4, getFloatBuffer(coords));

        int mMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, order.length, GLES20.GL_UNSIGNED_SHORT, getShortBuffer(order));

        GLES20.glDisableVertexAttribArray(position);

        //GLES20.glDeleteShader(vertexShader);
        //GLES20.glDeleteShader(fragmentShader);
    }

    int loadShader(int type, String shaderCode){
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    FloatBuffer getFloatBuffer(float[] data){
        // (# of coordinate values * 4 bytes per float)
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());

        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
        floatBuffer.put(data);
        floatBuffer.position(0);

        return floatBuffer;
    }

    ShortBuffer getShortBuffer(short[] data){
        // (# of coordinate values * 2 bytes per short)
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length * 2);
        byteBuffer.order(ByteOrder.nativeOrder());

        ShortBuffer shortBuffer = byteBuffer.asShortBuffer();
        shortBuffer.put(data);
        shortBuffer.position(0);

        return shortBuffer;
    }
}
