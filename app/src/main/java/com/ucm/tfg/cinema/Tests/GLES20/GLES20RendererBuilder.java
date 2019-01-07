package com.ucm.tfg.cinema.Tests.GLES20;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Stack;

public class GLES20RendererBuilder {

    private final String LOGTAG = "GLES20RendererBuilder";

    private int program;

    private int vertexShader;
    private int fragmentShader;

    private String vertexShaderCode;
    private String fragmentShaderCode;

    private Stack<Integer> enabledBuffers;

    public GLES20RendererBuilder() {
        this(GLES20.glCreateProgram());
    }

    public GLES20RendererBuilder(int program) {
        this.program = program;
        this.vertexShaderCode = null;
        this.fragmentShaderCode = null;
        enabledBuffers = new Stack<>();
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    public GLES20RendererBuilder addVertexShader(String code) {
        this.vertexShaderCode = code;
        this.vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, this.vertexShaderCode);
        return this;
    }

    public GLES20RendererBuilder addFragmentShader(String code) {
        this.fragmentShaderCode = code;
        this.fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, this.fragmentShaderCode);
        return this;
    }

    public GLES20RendererBuilder build(){
        if (this.vertexShaderCode == null) {
            Log.e(LOGTAG, "There is no Vertex Shader Code");
            return null;
        }
        if (this.fragmentShaderCode == null) {
            Log.e(LOGTAG, "There is no Fragment Shader Code");
            return null;
        }
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        GLES20.glUseProgram(program);
        return this;
    }

    public GLES20RendererBuilder addFloatBuffer(String name, int size, boolean normalized, float[] data) {
        int index = GLES20.glGetAttribLocation(program, name);
        GLES20.glEnableVertexAttribArray(index);
        enabledBuffers.push(index);
        GLES20.glVertexAttribPointer(index, size, GLES20.GL_FLOAT, normalized, size * 4, getFloatBuffer(data));
        return this;
    }

    public GLES20RendererBuilder addMatrix(String name, float[] matrix) {
        int index = GLES20.glGetUniformLocation(program, name);
        GLES20.glUniformMatrix4fv(index, 1, false, matrix, 0);
        return this;
    }

    public void draw(short[] order) {
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, order.length, GLES20.GL_UNSIGNED_SHORT, getShortBuffer(order));
        destruct();
    }

    private void destruct() {
        while (!enabledBuffers.empty()) {
            int index = enabledBuffers.pop();
            GLES20.glDisableVertexAttribArray(index);
        }
        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);
    }

    private FloatBuffer getFloatBuffer(float[] data){
        // (# of coordinate values * 4 bytes per float)
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());

        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
        floatBuffer.put(data);
        floatBuffer.position(0);

        return floatBuffer;
    }

    private ShortBuffer getShortBuffer(short[] data){
        // (# of coordinate values * 2 bytes per short)
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length * 2);
        byteBuffer.order(ByteOrder.nativeOrder());

        ShortBuffer shortBuffer = byteBuffer.asShortBuffer();
        shortBuffer.put(data);
        shortBuffer.position(0);

        return shortBuffer;
    }

}
