#version 150

const float PI = 3.1415927;

uniform bool uUseSolidColor;
uniform vec4 uSolidColor;

uniform sampler2D uSpecularNormalTex;
uniform sampler2D uSpecularAlbedoTex;
uniform sampler2D uDiffuseAlbedoTex;
uniform sampler2D uDiffuseNormalTex;
uniform sampler2D uShininessTex;

in vec3 vV; // tangent space vector to viewer
in vec3 vL; // tangent space vector to light
in vec2 vTC;

out vec4 fColor;

void main() {
    if (uUseSolidColor) {
        fColor = uSolidColor;
    } else {
        vec2 shine = texture(uShininessTex, vTC).rg;

        vec3 fN = normalize(2.0 * texture(uSpecularNormalTex, vTC).rgb - 1.0);
        // warp tangent (1,0,0) and bitangent (0,1,0) by perturbed normal
        vec3 fT = normalize(cross(vec3(0.0, 1.0, 0.0), fN));
        vec3 fB = normalize(cross(fN, fT));

        // normalize after linear interpolation
        vec3 fV = normalize(vV);
        vec3 fL = normalize(vL);
        vec3 fH = normalize(fV + fL);

        vec3 spec = texture(uSpecularAlbedoTex, vTC).rgb;
        vec3 diff = texture(uDiffuseAlbedoTex, vTC).rgb;

        vec3 fresnel = vec3(0.0);
        float pS = 0.0;
        if (dot(fN, fL) > 0.0 && dot(fN, fV) > 0.0) {
            fresnel = spec + (vec3(1.0) - spec) * vec3(pow(1.0 - dot(fL, fH), 5));
            float exp = (shine.x * dot(fH, fT) * dot(fH, fT) + shine.y * dot(fH, fB) * dot(fH, fB)) / (1.0 - dot(fH, fN) * dot(fH, fN));
            float denom = dot(fH, fL) * max(dot(fN, fL), dot(fN, fV));
            pS = sqrt((shine.x + 1.0) * (shine.y + 1.0)) / (8.0 * PI) * pow(dot(fN, fH), exp) / denom;
        }

        vec3 fNd = normalize(2.0 * texture(uDiffuseNormalTex, vTC).rgb - 1.0);
        float pD = 0.0;
        if (dot(fNd, fL) > 0.0 && dot(fNd, fV) > 0.0)
            pD = 28.0 / (23.0 * PI) * (1.0 - pow(1.0 - dot(fNd, fL) / 2.0, 5)) * (1.0 - pow(1.0 - dot(fNd, fV) / 2.0, 5));

    //    vec3 color = pD * diff * (vec3(1.0) - spec) + pS * fresnel;
        vec3 color = pD * diff * (vec3(1.0) - pS * fresnel) + pS * fresnel;
        fColor = vec4(pow(color, vec3(1.0 / 2.2)), 1.0);
    }
}
