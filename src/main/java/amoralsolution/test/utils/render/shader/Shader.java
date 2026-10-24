package amoralsolution.test.utils.render.shader;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.util.HashMap;
import java.util.Map;

public class Shader {

    private final int programId;
    private final Map<String, Integer> uniformLocationCache = new HashMap<String, Integer>();

    public Shader(String vertexSource, String fragmentSource) {
        int vertexShader = createShader(vertexSource, GL20.GL_VERTEX_SHADER);
        int fragmentShader = createShader(fragmentSource, GL20.GL_FRAGMENT_SHADER);

        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertexShader);
        GL20.glAttachShader(program, fragmentShader);
        GL20.glLinkProgram(program);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(program, 4096);
            throw new RuntimeException("Shader program linking failed:\n" + log);
        }

        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);

        this.programId = program;
    }

    private static int createShader(String source, int type) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);

        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader, 4096);
            String typeName = (type == GL20.GL_VERTEX_SHADER ? "Vertex" : "Fragment");
            throw new RuntimeException(typeName + " shader compilation failed:\n" + log);
        }

        return shader;
    }

    public void bind() {
        GL20.glUseProgram(this.programId);
    }

    public static void unbind() {
        GL20.glUseProgram(0);
    }

    public int getUniformLocation(String uniformName) {
        Integer loc = uniformLocationCache.get(uniformName);
        if (loc == null) {
            loc = GL20.glGetUniformLocation(this.programId, uniformName);
            uniformLocationCache.put(uniformName, loc);
        }
        return loc;
    }

    public void setUniform1f(String name, float v) {
        int loc = getUniformLocation(name);
        if (loc != -1) GL20.glUniform1f(loc, v);
    }

    public void setUniform2f(String name, float v1, float v2) {
        int loc = getUniformLocation(name);
        if (loc != -1) GL20.glUniform2f(loc, v1, v2);
    }

    public void setUniform3f(String name, float v1, float v2, float v3) {
        int loc = getUniformLocation(name);
        if (loc != -1) GL20.glUniform3f(loc, v1, v2, v3);
    }

    public void setUniform4f(String name, float v1, float v2, float v3, float v4) {
        int loc = getUniformLocation(name);
        if (loc != -1) GL20.glUniform4f(loc, v1, v2, v3, v4);
    }

    public void setUniform1i(String name, int v) {
        int loc = getUniformLocation(name);
        if (loc != -1) GL20.glUniform1i(loc, v);
    }

    public int getProgramId() {
        return this.programId;
    }

    public void delete() {
        GL20.glDeleteProgram(this.programId);
    }
}
