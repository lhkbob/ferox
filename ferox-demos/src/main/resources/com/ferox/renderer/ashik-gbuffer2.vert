#version 150

uniform mat4 uView; // transform from world to eye space
uniform mat4 uModel; // transform from obj to world space
uniform mat4 uProjection;

uniform vec3 uCamPos;

in vec4 aPos;
in vec3 aNorm;
in vec2 aTC;
in vec4 aTan;

out mat3 vTanToWorld;
out vec3 vViewDir;
out vec2 vTC;

void main() {
    vec4 worldPos = uModel * aPos;

    vTanToWorld = mat3(uModel) * mat3(aTan.w * cross(aNorm, aTan.xyz), aTan.xyz, aNorm);
    vViewDir = normalize(uCamPos - worldPos.xyz);

    vTC = aTC;
    gl_Position = uProjection * uView * worldPos;
}
