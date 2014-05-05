#version 150

uniform mat4 uView; // transform from world to eye space
uniform mat4 uModel; // transform from obj to world space
uniform mat4 uProjection;

in vec4 aPos;
in vec4 aColor;
in vec2 aTC;

out vec3 vTCDir;
out vec2 vTC;
out vec4 vColor;

void main() {
    vColor = aColor;
    vTCDir = normalize(((uModel) * aPos).xyz);
    vTC = aTC;
    gl_Position = uProjection * uView * uModel * aPos;
}
