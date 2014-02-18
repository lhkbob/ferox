#version 150

const float PI = 3.1415927;

uniform sampler2D uNormal;
uniform sampler2D uSpecularAlbedoDiffuseB;
uniform sampler2D uShininessXYDiffuseRG;
uniform sampler2D uDepth;

uniform samplerCube uDiffuseIrradiance;

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
    vec4 shineDiffRG = texture(uShininessXYDiffuseRG, vTC);
    vec4 specDiffB = texture(uSpecularAlbedoDiffuseB, vTC);
    vec3 fN = normalize(texture(uNormal, vTC).rgb);

    float viewTerm = (1.0 - pow(1.0 - max(0.0, dot(fN, normalize(uCamPos - worldPos))) / 2.0, 5.0));
    vec3 diff = vec3(shineDiffRG.zw, specDiffB.w);
    diff = viewTerm * diff * (1.0 - specDiffB.rgb) * texture(uDiffuseIrradiance, fN).rgb;
    fColor = vec4(diff, 1.0);
}
