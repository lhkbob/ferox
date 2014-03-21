#version 150

const float PI = 3.1415927;

uniform sampler2D uNormalAndTangent;
uniform sampler2D uShininessAndNTZ;
uniform sampler2D uDiffuseAlbedo;
uniform sampler2D uSpecularAlbedo;
uniform sampler2D uDepth;

uniform mat3 uInvView;
uniform mat4 uInvProj;
uniform samplerCube uDiffuseIrradiance;
uniform mat3 uEnvTransform;

in vec2 vTC;
out vec4 fColor;

void main() {
    vec4 nt = texture(uNormalAndTangent, vTC);
    vec4 sz = texture(uShininessAndNTZ, vTC);
    float depth = texture(uDepth, vTC).r;

    // reconstruct packed vectors
    vec3 fN = normalize(vec3(nt.xy, sz.z));

    vec4 viewPos = uInvProj * (2.0 * vec4(vTC, depth, 1.0) - 1.0);
    viewPos = viewPos / viewPos.w;
    vec3 fV = -normalize(viewPos.xyz);

    float viewTerm = (1.0 - pow(1.0 - max(0.0, dot(fN, fV)) / 2.0, 5.0));
    vec3 diff = texture(uDiffuseAlbedo, vTC).rgb;
    vec3 spec = texture(uSpecularAlbedo, vTC).rgb;
    diff = viewTerm * diff * (1.0 - spec) * texture(uDiffuseIrradiance, uEnvTransform * uInvView * fN).rgb;
    fColor = vec4(diff, 1.0);
}
