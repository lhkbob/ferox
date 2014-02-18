#version 150

uniform mat4 uView; // transform from world to eye space
uniform mat4 uModel; // transform from obj to world space
uniform mat4 uProjection;

uniform vec3 uCamPos; // in world space already
uniform vec4 uLightPos; // in world space already

uniform vec2 uTCScale;

in vec4 aPos;
in vec3 aNorm;
in vec2 aTC;
in vec4 aTan;

out vec3 vV; // tan space vector to viewer
out vec3 vL; // tan space vector to light
out vec2 vTCa;
out vec2 vTCb;

void main() {
    vec4 wPos = uModel * aPos;

    mat3 tanToObj = mat3(aTan.xyz, aTan.w * cross(aNorm, aTan.xyz), aNorm);
    mat3 objToTan = transpose(tanToObj);
    mat3 worldToTan = objToTan * transpose(mat3(uModel));

    vV = (worldToTan * (uCamPos - wPos.xyz));
    if (uLightPos.w == 0.0) {
        // infinite light
        vL = (worldToTan * normalize(uLightPos.xyz));
    } else {
        // point light
        vL = (worldToTan * normalize(uLightPos.xyz - wPos.xyz));
    }

    vTCa = uTCScale.x * aTC;
    vTCb = uTCScale.y * aTC;
    gl_Position = uProjection * uView * wPos;
}
