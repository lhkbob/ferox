#version 150

uniform mat4 uView; // transform from world to eye space
uniform mat4 uModel; // transform from obj to world space
uniform mat4 uProjection;

in vec4 aPos;
in vec4 aColor;

out vec3 vTC;
out vec4 vColor;

void main() {
    vColor = aColor;
    vTC = normalize(((uModel) * aPos).xyz);
    gl_Position = uProjection * uView * uModel * aPos;
}
