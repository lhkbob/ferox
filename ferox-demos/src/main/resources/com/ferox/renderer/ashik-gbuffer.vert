#version 150

uniform mat4 uView; // transform from world to eye space
uniform mat4 uModel; // transform from obj to world space
uniform mat4 uProjection;

uniform vec3 uCamPos;
uniform vec2 uTCScale;

in vec4 aPos;
in vec3 aNorm;
in vec2 aTC;
in vec4 aTan;

//out mat3 vTanToWorld;
out vec3 vN;
out vec3 vT;
out vec3 vB;

out vec3 vEyeDir;

out vec2 vTCa;
out vec2 vTCb;

void main() {
    vec4 wPos = uModel * aPos;

    vN = aNorm;
    vT = aTan.xyz;
    vB = aTan.w * cross(aNorm, aTan.xyz);

    vEyeDir = normalize(uCamPos - wPos.xyz);

    vTCa = uTCScale.x * aTC;
    vTCb = uTCScale.y * aTC;
    gl_Position = uProjection * uView * wPos;
}
