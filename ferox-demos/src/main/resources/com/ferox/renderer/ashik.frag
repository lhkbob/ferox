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
in vec2 vTCa;
in vec2 vTCb;

out vec4 fColor;

vec2 shininess() {
    return uShininessScale * ((1.0 - uShininessAlpha) * texture(uShininessTexA, vTCa).rg + uShininessAlpha * texture(uShininessTexB, vTCb).rg);
}

vec3 specularNormal() {
    vec3 n1 = 2.0 * texture(uSpecularNormalTexA, vTCa).rgb - 1.0;
    vec3 n2 = 2.0 * texture(uSpecularNormalTexB, vTCb).rgb - 1.0;
    return normalize((1.0 - uNormalAlpha) * n1 + uNormalAlpha * n2);
}

vec3 diffuseNormal() {
    vec3 n1 = 2.0 * texture(uDiffuseNormalTexA, vTCa).rgb - 1.0;
    vec3 n2 = 2.0 * texture(uDiffuseNormalTexB, vTCb).rgb - 1.0;
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
    if (uUseSolidColor) {
        vec3 fN = specularNormal();
        vec3 fL = normalize(vL);
        float ndotl = max(dot(fN, fL), 0.0);
        fColor = ndotl * vec4(vTCa, vTCb) * uSolidColor;
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

        vec3 spec = vec3(0.0);

        float lightToNorm = max(0.0, dot(fN, fL));
        float viewToNorm = max(0.0, dot(fN, fV));
        if (lightToNorm > 0.0 && viewToNorm > 0.0) {
            float lh = dot(fL, fH);
            float th = dot(fT, fH);
            float bh = dot(fB, fH);
            float nh = dot(fN, fH);

            spec = specularAlbedo();
            spec = spec + (vec3(1.0) - spec) * vec3(pow(1.0 - lh, 5));
            float exp = (shine.x * th * th + shine.y * bh * bh) / (1.0 - nh * nh);
            float denom = lh * max(lightToNorm, viewToNorm);
            float pS = sqrt((shine.x + 1.0) * (shine.y + 1.0)) / (8.0 * PI) * pow(nh, exp) / denom;
            spec = pS * spec;
        }

        vec3 diff = vec3(0.0);
        // update normal to use diffuse normal texture
        fN = diffuseNormal();
        lightToNorm = max(0.0, dot(fN, fL));
        viewToNorm = max(0.0, dot(fN, fV));
        if (lightToNorm > 0.0 && viewToNorm > 0.0) {
            float pD = 28.0 / (23.0 * PI) * (1.0 - pow(1.0 - lightToNorm / 2.0, 5.0)) * (1.0 - pow(1.0 - viewToNorm / 2.0, 5.0));
            diff = pD * diffuseAlbedo();
        }

        vec3 color = diff * (1.0 - spec) + spec;
        fColor = vec4(pow(color, vec3(1.0 / 2.2)), 1.0);
    }
}
