#version 150

uniform mat4 uModelview; // transform from obj to eye space
uniform mat4 uInverseModelview; // inverse transform from eye space to obj space
uniform mat4 uProjection;

uniform vec4 uLightPos; // in eye space already

uniform vec2 uTCScale;

in vec4 aPos;
in vec3 aNorm;
in vec2 aTC;
in vec4 aTan;

out vec3 vV; // eye space vector to viewer
out vec3 vL; // eye space vector to light
out vec2 vTC;

void main() {
    vec4 ePos = uModelview * aPos;

    mat4 objToTan = transpose(mat4(vec4(aTan.xyz, 0.0),
                                   vec4(aTan.w * cross(aNorm, aTan.xyz), 0.0),
                                   vec4(aNorm, 0.0),
                                   vec4(0.0, 0.0, 0.0, 1.0)));
    mat4 eyeToTan = objToTan * uInverseModelview;

    vV = (eyeToTan * vec4(-ePos.xyz, 0.0)).xyz;

    if (uLightPos.w == 0.0) {
        // infinite light
        vL = (eyeToTan * vec4(normalize(uLightPos.xyz), 0.0)).xyz;
    } else {
        // point light
        vL = (eyeToTan * vec4(normalize(uLightPos.xyz - ePos.xyz), 0.0)).xyz;
    }

    vTC = uTCScale * aTC;
    gl_Position = uProjection * ePos;
}
