#version 150

const float PI = 3.1415927;

uniform sampler2D uNormal;
uniform sampler2D uTangent;
uniform sampler2D uSpecularAlbedoDiffuseB;
uniform sampler2D uShininessXYDiffuseRG;
uniform sampler2D uDepth;

uniform samplerCube uEnvMap;

uniform vec3 uLightDirection;
uniform vec3 uCamPos;

uniform mat4 uInvProjection;
uniform mat4 uInvView;

in vec2 vTC;

out vec4 fColor;

void main() {
    // reconstruct position in world space
    float depth = texture(uDepth, vTC).r;
    vec4 p = uInvProjection * (vec4((vTC * 2.0 - 1.0), 2.0 * depth - 1.0, 1.0));
    vec3 worldPos = (uInvView * (p / p.w)).xyz;

    // reconstruct lighting model and surface parameters from gbuffer
    vec2 shine = texture(uShininessXYDiffuseRG, vTC).rg;
    vec3 fN = normalize(texture(uNormal, vTC).rgb);
    vec3 fT = normalize(texture(uTangent, vTC).rgb);
    vec3 fB = normalize(cross(fN, fT));

    vec3 fV = normalize(uCamPos - worldPos);
    vec3 fL = normalize(uLightDirection);
    vec3 fH = normalize(fV + fL);

    vec3 spec = vec3(0.0);

    float lightToNorm = max(0.0, dot(fN, fL));
    float viewToNorm = max(0.0, dot(fN, fV));
    if (lightToNorm > 0.0 && viewToNorm > 0.0) {
        float lh = dot(fL, fH);
        float th = dot(fT, fH);
        float bh = dot(fB, fH);
        float nh = dot(fN, fH);

        spec = texture(uSpecularAlbedoDiffuseB, vTC).rgb; // Rs
        spec = spec + (vec3(1.0) - spec) * vec3(pow(1.0 - lh, 5)); // fresnel
        float exp = (shine.x * th * th + shine.y * bh * bh) / (1.0 - nh * nh);
        float denom = lh * max(lightToNorm, viewToNorm);
        float pS = sqrt((shine.x + 1.0) * (shine.y + 1.0)) / (8.0 * PI) * pow(nh, exp) / denom;
        spec = pS * spec * texture(uEnvMap, fL).rgb;
    }

    fColor = vec4(spec, 1.0);
}
