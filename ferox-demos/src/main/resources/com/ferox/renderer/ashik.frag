#version 150

const float PI = 3.1415927;

uniform bool uUseSolidColor;
uniform vec4 uSolidColor;

uniform sampler2D uSpecularNormalTexA;
uniform sampler2D uSpecularAlbedoTexA;
uniform sampler2D uDiffuseAlbedoTexA;
uniform sampler2D uDiffuseNormalTexA;
uniform sampler2D uShininessTexA;

uniform sampler2D uSpecularNormalTexB;
uniform sampler2D uSpecularAlbedoTexB;
uniform sampler2D uDiffuseAlbedoTexB;
uniform sampler2D uDiffuseNormalTexB;
uniform sampler2D uShininessTexB;

uniform float uNormalAlpha;
uniform float uSpecularAlpha;
uniform float uDiffuseAlpha;
uniform float uShininessAlpha;

uniform vec2 uShininessScale;

in vec3 vV; // tangent space vector to viewer
in vec3 vL; // tangent space vector to light
in vec2 vTC;

out vec4 fColor;

vec2 shininess() {
    return uShininessScale * ((1.0 - uShininessAlpha) * texture(uShininessTexA, vTC).rg + uShininessAlpha * texture(uShininessTexB, vTC).rg);
}

vec3 specularNormal() {
    vec3 n1 = 2.0 * texture(uSpecularNormalTexA, vTC).rgb - 1.0;
    vec3 n2 = 2.0 * texture(uSpecularNormalTexB, vTC).rgb - 1.0;
    return normalize((1.0 - uNormalAlpha) * n1 + uNormalAlpha * n2);
}

vec3 diffuseNormal() {
    vec3 n1 = 2.0 * texture(uDiffuseNormalTexA, vTC).rgb - 1.0;
    vec3 n2 = 2.0 * texture(uDiffuseNormalTexB, vTC).rgb - 1.0;
    return normalize((1.0 - uNormalAlpha) * n1 + uNormalAlpha * n2);
}

vec3 specularAlbedo() {
    vec3 c1 = texture(uSpecularAlbedoTexA, vTC).rgb;
    vec3 c2 = texture(uSpecularAlbedoTexB, vTC).rgb;
    return (1.0 - uSpecularAlpha) * c1 + uSpecularAlpha * c2;
}

vec3 diffuseAlbedo() {
    vec3 c1 = texture(uDiffuseAlbedoTexA, vTC).rgb;
    vec3 c2 = texture(uDiffuseAlbedoTexB, vTC).rgb;
    return (1.0 - uDiffuseAlpha) * c1 + uDiffuseAlpha * c2;
}

void main() {
    if (uUseSolidColor) {
        fColor = uSolidColor;
    } else {
        vec2 shine = shininess();

        vec3 fN = specularNormal();
        // warp tangent (1,0,0) and bitangent (0,1,0) by perturbed normal
        vec3 fT = normalize(cross(vec3(0.0, 1.0, 0.0), fN));
        vec3 fB = normalize(cross(fN, fT));

        // normalize after linear interpolation
        vec3 fV = normalize(vV);
        vec3 fL = normalize(vL);
        vec3 fH = normalize(fV + fL);

        vec3 spec = specularAlbedo();
        vec3 diff = diffuseAlbedo();

        vec3 fresnel = vec3(0.0);
        float pS = 0.0;
        if (dot(fN, fL) > 0.0 && dot(fN, fV) > 0.0) {
            fresnel = spec + (vec3(1.0) - spec) * vec3(pow(1.0 - dot(fL, fH), 5));
            float exp = (shine.x * dot(fH, fT) * dot(fH, fT) + shine.y * dot(fH, fB) * dot(fH, fB)) / (1.0 - dot(fH, fN) * dot(fH, fN));
            float denom = dot(fH, fL) * max(dot(fN, fL), dot(fN, fV));
            pS = sqrt((shine.x + 1.0) * (shine.y + 1.0)) / (8.0 * PI) * pow(dot(fN, fH), exp) / denom;
        }

        vec3 fNd = diffuseNormal();
        float pD = 0.0;
        if (dot(fNd, fL) > 0.0 && dot(fNd, fV) > 0.0)
            pD = 28.0 / (23.0 * PI) * (1.0 - pow(1.0 - dot(fNd, fL) / 2.0, 5)) * (1.0 - pow(1.0 - dot(fNd, fV) / 2.0, 5));

//        vec3 color = pD * diff * (vec3(1.0) - spec) + pS * fresnel;
        vec3 color = pD * diff * (vec3(1.0) - pS * fresnel) + pS * fresnel;
        fColor = vec4(pow(color, vec3(1.0 / 2.2)), 1.0);
    }
}
