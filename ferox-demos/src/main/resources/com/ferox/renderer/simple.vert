#version 150

uniform mat4 uView; // transform from world to eye space
uniform mat4 uModel; // transform from obj to world space
uniform mat4 uProjection;

in vec4 aPos;

out vec3 vTC;

void main() {
    vTC = normalize(((uModel) * aPos).xyz);
    gl_Position = uProjection * uView * uModel * aPos;
}
