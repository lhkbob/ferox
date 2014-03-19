#version 150

const float PI = 3.1415927;

uniform sampler2D uNormalAndTangent;
uniform sampler2D uShininessAndNTZ;
uniform sampler2D uDiffuseAlbedo;
uniform sampler2D uSpecularAlbedo;
uniform sampler2D uDepth;

uniform mat4 uInvProj;
uniform mat3 uInvView;

uniform samplerCube uDiffuseIrradiance;
uniform samplerCube uSpecularRadiance; // assumes premultipled solid angle

const int SAMPLES = 64;
//uniform float u1[SAMPLES];
//uniform float u2[SAMPLES];

in vec2 vTC;
out vec4 fColor;

float rand(vec2 co)
{
   return fract(sin(dot(co.xy,vec2(12.9898,78.233))) * 43758.5453);
}

vec3 lightSample(vec3 fN, vec3 fT, vec3 fB, vec3 fV, vec3 fH, vec3 diffuse, vec3 specular, vec2 shine) {
    vec3 fL = -fV + 2 * dot(fH, fV) * fH;

    float lightToNorm = max(0.0, dot(fN, fL));
    float viewToNorm = max(0.0, dot(fN, fV));
    vec3 spec = vec3(0.0);
    if (lightToNorm > 0.0) {// && viewToNorm > 0.0) {
        float lh = max(0.0, dot(fL, fH));
        float th = dot(fT, fH);
        float bh = dot(fB, fH);
        float nh = dot(fN, fH);

        if (specular.r > 0.0 || specular.g > 0.0 || specular.b > 0.0) {
            spec = specular + (vec3(1.0) - specular) * vec3(pow(1.0 - lh, 5)); // fresnel
            float exp = (shine.x * th * th + shine.y * bh * bh) / (1.0 - nh * nh);
            float denom = lh * max(lightToNorm, viewToNorm);
            float pS = sqrt((shine.x + 1.0) * (shine.y + 1.0)) / (8.0 * PI) * pow(nh, exp) / denom;

            spec = lightToNorm * pS * spec * texture(uSpecularRadiance, uInvView * fL).rgb;
        }
    }
    return spec;
}

float samplePhi(float u, vec2 shine) {
    return atan(sqrt((shine.x + 1) / (shine.y + 1)) * tan(PI * u / 2.0));
}

void main() {
    vec4 nt = texture(uNormalAndTangent, vTC);
    vec4 sz = texture(uShininessAndNTZ, vTC);
    float depth = texture(uDepth, vTC).r;

    // reconstruct packed vectors
    vec3 fN = normalize(vec3(nt.xy, sz.z));
    vec3 fT = normalize(vec3(nt.zw, sz.w));

    vec4 viewPos = uInvProj * (2.0 * vec4(vTC, depth, 1.0) - 1.0);
    viewPos = viewPos / viewPos.w;

    vec3 fV = -normalize(viewPos.xyz);
    vec3 fB = normalize(cross(fN, fT));

    vec3 spec = texture(uSpecularAlbedo, vTC).rgb;
    vec3 diff = texture(uDiffuseAlbedo, vTC).rgb;

    // diffuse term
    float viewTerm = (1.0 - pow(1.0 - max(0.0, dot(fN, fV)) / 2.0, 5.0));
    vec3 diffLight = viewTerm * diff * (1.0 - spec) * texture(uDiffuseIrradiance, uInvView * fN).rgb;

    // add in samples for specular using importance sampling
    vec2 shine = sz.xy;
    mat3 basis = mat3(fT, fB, fN);
    vec3 specLight = vec3(0.0);
    float pdfTerm = sqrt((shine.x + 1.0) * (shine.y + 1.0)) / (2.0 * PI);
    for (int i = 0; i < SAMPLES; i++) {
        float e1 = rand(fN.xy + vTC + 2 * i);
        float e2 = rand(fN.xy + vTC + 2 * i + 1);

        float phi;
        if (e1 < 0.25) {
            phi = samplePhi(4 * e1, shine);
        } else if (e1 < 0.5) {
            phi = PI - samplePhi(4 * (0.5 - e1), shine);
        } else if (e1 < 0.75) {
            phi = samplePhi(4 * (e1 - 0.5), shine) + PI;
        } else {
            phi = 2 * PI - samplePhi(4 * (1.0 - e1), shine);
        }

        float cosPhi = cos(phi);
        float sinPhi = sin(phi);

        float exp = (shine.x * cosPhi * cosPhi + shine.y * sinPhi * sinPhi);
        float cosTheta = pow((1 - e2), 1.0 / (exp + 1.0));
        float radius = sqrt(1.0 - cosTheta * cosTheta);
        vec3 fH = basis * normalize(vec3(cosPhi * radius, sinPhi * radius, cosTheta));
        float vh = dot(fV, fH);
        float pdf = pdfTerm * pow(max(0.0, dot(fN, fH)), exp) / (4.0 * vh);
        specLight = specLight + lightSample(fN, fT, fB, fV, fH, diff, spec, shine) / pdf;
    }
    specLight = specLight / SAMPLES;

    fColor = vec4(diffLight + specLight, 1.0);
}
