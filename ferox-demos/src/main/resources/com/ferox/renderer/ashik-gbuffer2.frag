#version 150

const float PI = 3.1415927;

uniform sampler2D uNormalTexA;
uniform sampler2D uNormalTexB;
uniform float uNormalAlpha;

uniform sampler2D uDiffuseAlbedoA;
uniform sampler2D uDiffuseAlbedoB;
uniform float uDiffuseAlbedoAlpha;

uniform sampler2D uSpecularAlbedoA;
uniform sampler2D uSpecularAlbedoB;
uniform float uSpecularAlbedoAlpha;

uniform sampler2D uShininessA;
uniform sampler2D uShininessB;
uniform float uShininessAlpha;

uniform float uShinyOverride;
uniform vec2 uShininessScale;

uniform vec2 uTCScale;

in mat3 vTanToWorld;
in vec3 vViewDir;
in vec2 vTC;

out vec4 fNormalTangent;
out vec4 fShininessAndView;
out vec4 fDiffuseAlbedo;
out vec4 fSpecularAlbedo;

vec3 diffuseAlbedo() {
    vec3 c1 = texture(uDiffuseAlbedoA, vTC * uTCScale.x).rgb;
    vec3 c2 = texture(uDiffuseAlbedoB, vTC * uTCScale.y).rgb;
    return mix(c1, c2, uDiffuseAlbedoAlpha);
}

vec3 specularAlbedo() {
    vec3 c1 = texture(uSpecularAlbedoA, vTC * uTCScale.x).rgb;
    vec3 c2 = texture(uSpecularAlbedoB, vTC * uTCScale.y).rgb;
    return mix(c1, c2, uSpecularAlbedoAlpha);
}

vec2 shininess() {
    if (uShinyOverride <= 0.0) {
        vec2 s1 = texture(uShininessA, vTC * uTCScale.x).rg;
        vec2 s2 = texture(uShininessB, vTC * uTCScale.y).rg;
        return uShininessScale * mix(s1, s2, uShininessAlpha);
    } else {
        return uShininessScale * vec2(uShinyOverride);
    }
}

vec3 specularNormal() {
    vec3 n1 = 2.0 * texture(uNormalTexA, vTC * uTCScale.x).rgb - 1.0;
    vec3 n2 = 2.0 * texture(uNormalTexB, vTC * uTCScale.y).rgb - 1.0;
    return normalize((1.0 - uNormalAlpha) * n1 + uNormalAlpha * n2);
}

vec2 encode(vec3 n) {
    vec2 r = normalize(n.xy) * sqrt(-n.z * 0.5 + 0.5);
    r = 0.5 * r + 0.5;
//    vec2 r = vec2(0.5 * atan(n.y, n.x) / PI + 0.5, acos(n.z) / PI);
    return r;
}

void main() {
    vec3 tN = specularNormal();
    vec3 tT = normalize(cross(vec3(0.0, 1.0, 0.0), tN));
    tN = normalize(vTanToWorld * tN);
    tT = normalize(vTanToWorld * tT);

    fNormalTangent = vec4(encode(tN), encode(tT));
    fShininessAndView = vec4(shininess(), encode(vViewDir));
    fDiffuseAlbedo = vec4(diffuseAlbedo(), 1.0);
    fSpecularAlbedo = vec4(specularAlbedo(), 1.0);
}
