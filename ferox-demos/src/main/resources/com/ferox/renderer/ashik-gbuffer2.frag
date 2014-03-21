#version 150

const float PI = 3.1415927;

uniform samplerCube uDiffuseIrradiance;

uniform sampler2D uNormalTex;
uniform float uNormalWeight;

uniform sampler2D uDiffuseAlbedo;
uniform float uDiffuseAlbedoWeight;

uniform sampler2D uSpecularAlbedo;
uniform float uSpecularAlbedoWeight;

uniform sampler2D uShininess;
uniform float uShininessWeight;

uniform float uShinyOverride;
uniform vec2 uShininessScale;
uniform vec3 uDiffuseScale;
uniform vec3 uSpecularScale;

uniform vec2 uTCScale;

in mat3 vTanToView;
in vec2 vTC;

out vec4 fNormalTangentXY;
out vec4 fShininessAndNTZ;
out vec4 fDiffuseAlbedo;
out vec4 fSpecularAlbedo;

vec3 diffuseAlbedo() {
    vec3 c1 = texture(uDiffuseAlbedo, vTC * uTCScale).rgb;
    return uDiffuseAlbedoWeight * min(uDiffuseScale * (c1 + vec3(0.001)), 1.0);
}

vec3 specularAlbedo() {
    vec3 c1 = texture(uSpecularAlbedo, vTC * uTCScale).rgb;
    return uSpecularAlbedoWeight * min(uSpecularScale * (c1 + vec3(0.001)), 1.0);
}

vec2 shininess() {
    if (uShinyOverride <= 0.0) {
        vec2 s1 = texture(uShininess, vTC * uTCScale).rg;
        return uShininessWeight * uShininessScale * s1;
    } else {
        return uShininessWeight * uShininessScale * vec2(uShinyOverride);
    }
}

vec3 specularNormal() {
    // must mix weight in later since we have to compute tangent space from this
    return normalize(2.0 * texture(uNormalTex, vTC * uTCScale).rgb - 1.0);
}

void main() {
    vec3 tN = specularNormal();
    vec3 tT = normalize(cross(vec3(0.0, 1.0, 0.0), tN));
    tN = uNormalWeight * normalize(vTanToView * tN);
    tT = uNormalWeight * normalize(vTanToView * tT);

    fNormalTangentXY = vec4(tN.xy, tT.xy);
    fShininessAndNTZ = vec4(shininess(), tN.z, tT.z);
    fDiffuseAlbedo = vec4(diffuseAlbedo(), 0.0);
    fSpecularAlbedo = vec4(specularAlbedo(), 0.0);
}
