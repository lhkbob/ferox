#version 150

const float PI = 3.1415927;

uniform sampler2D uSpecularNormalTexA;
uniform sampler2D uSpecularAlbedoTexA;
uniform sampler2D uDiffuseAlbedoTexA;
uniform sampler2D uShininessTexA;

uniform sampler2D uSpecularNormalTexB;
uniform sampler2D uSpecularAlbedoTexB;
uniform sampler2D uDiffuseAlbedoTexB;
uniform sampler2D uShininessTexB;

uniform float uNormalAlpha;
uniform float uSpecularAlpha;
uniform float uDiffuseAlpha;
uniform float uShininessAlpha;

uniform vec2 uShininessScale;

uniform mat4 uModel;

in vec3 vN;
in vec3 vT;
in vec3 vB;

in vec3 vEyeDir; // world space vector to camera

in vec2 vTCa;
in vec2 vTCb;

out vec3 fNormal;
out vec3 fTangent;
out vec4 fSpecularAlbedoDiffuseB;
out vec4 fShininessXYDiffuseRG;

vec2 shininess() {
    return uShininessScale * ((1.0 - uShininessAlpha) * texture(uShininessTexA, vTCa).rg + uShininessAlpha * texture(uShininessTexB, vTCb).rg);
}

vec3 specularNormal() {
    vec3 n1 = 2.0 * texture(uSpecularNormalTexA, vTCa).rgb - 1.0;
    vec3 n2 = 2.0 * texture(uSpecularNormalTexB, vTCb).rgb - 1.0;
    return normalize((1.0 - uNormalAlpha) * n1 + uNormalAlpha * n2);
}

vec3 specularAlbedo() {
    vec3 c1 = texture(uSpecularAlbedoTexA, vTCa).rgb;
    vec3 c2 = texture(uSpecularAlbedoTexB, vTCb).rgb;
    return (1.0 - uSpecularAlpha) * c1 + uSpecularAlpha * c2;
}

vec3 diffuseAlbedo() {
    vec3 c1 = texture(uDiffuseAlbedoTexA, vTCa).rgb;
    vec3 c2 = texture(uDiffuseAlbedoTexB, vTCb).rgb;
    return (1.0 - uDiffuseAlpha) * c1 + uDiffuseAlpha * c2;
}

void main() {
    mat3 tanToWorld = mat3(uModel) * mat3(normalize(vT), normalize(vB), normalize(vN));

    vec2 shine = shininess();
    vec3 tN = specularNormal();
    float factor = 1.0 - pow(1.0 - max(0.0, dot(normalize(vEyeDir), tanToWorld[2])), 3);
    tN = factor * tN + (1.0 - factor) * vec3(0.0, 0.0, 1.0);

    vec3 tT = normalize(cross(vec3(0.0, 1.0, 0.0), tN));

    vec3 diff = diffuseAlbedo();

    fNormal = tanToWorld * tN;
    fTangent = tanToWorld * tT;
    fShininessXYDiffuseRG = vec4(shine, diff.rg);
    fSpecularAlbedoDiffuseB = vec4(specularAlbedo(), diff.b);
}
