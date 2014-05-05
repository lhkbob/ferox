#version 150

uniform mat4 uProjection;

in vec4 aPos;
in vec2 aTC;

out vec2 vTC;

void main() {
    vTC = aTC;
    gl_Position = uProjection * aPos;
}
