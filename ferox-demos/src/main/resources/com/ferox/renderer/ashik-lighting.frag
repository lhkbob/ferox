#version 150

const float PI = 3.1415927;

const int NUM_SAMPLES = 40;

uniform sampler2D uNormalAndTangent;
uniform sampler2D uTCAndView;
uniform vec2 uTCScale;

uniform sampler2D uAlbedoA;
uniform sampler2D uAlbedoB;
uniform float uAlbedoAlpha;

uniform sampler2D uShininessA;
uniform sampler2D uShininessB;
uniform float uShininessAlpha;
uniform vec2 uShininessScale;

uniform bool uDiffuseMode;
uniform mat3 uInvView;
uniform samplerCube uDiffuseIrradiance;

uniform vec3 uLightDirection[NUM_SAMPLES];
uniform vec3 uLightRadiance[NUM_SAMPLES];


in vec2 vTC;
out vec4 fColor;

vec3 albedo(vec2 tc) {
    vec3 c1 = texture(uAlbedoA, tc * uTCScale.x).rgb;
    vec3 c2 = texture(uAlbedoB, tc * uTCScale.y).rgb;
    return (1.0 - uAlbedoAlpha) * c1 + uAlbedoAlpha * c2;
}

vec2 shininess(vec2 tc) {
    vec2 s1 = texture(uShininessA, tc * uTCScale.x).rg;
    vec2 s2 = texture(uShininessB, tc * uTCScale.y).rg;
    return uShininessScale * ((1.0 - uShininessAlpha) * s1 + uShininessAlpha * s2);
}

vec3 decode(vec2 r) {
    vec4 nn = vec4(2 * r, 0, 0) + vec4(-1, -1, 1, -1);
    float l = dot(nn.xyz, -nn.xyw);
    nn.z = l;
    nn.xy *= sqrt(l);
    return nn.xyz * 2 + vec3(0, 0, -1);
}

vec3 lightSample(vec3 fN, vec3 fT, vec3 fB, vec3 fV, vec3 fL, vec3 light, vec3 albedo, vec2 shine) {
    vec3 fH = normalize(fV + fL);

    float lightToNorm = max(0.0, dot(fN, fL));
    float viewToNorm = max(0.0, dot(fN, fV));
    vec3 spec = vec3(0.0);
    if (lightToNorm > 0.0 && viewToNorm > 0.0) {
        float lh = dot(fL, fH);
        float th = dot(fT, fH);
        float bh = dot(fB, fH);
        float nh = dot(fN, fH);

        spec = albedo;
        if (spec.r > 0.0 || spec.g > 0.0 || spec.b > 0.0) {
            spec = spec + (vec3(1.0) - spec) * vec3(pow(1.0 - lh, 5)); // fresnel
            float exp = (shine.x * th * th + shine.y * bh * bh) / (1.0 - nh * nh);
            float denom = lh * max(lightToNorm, viewToNorm);
            float pS = sqrt((shine.x + 1.0) * (shine.y + 1.0)) / (8.0 * PI) * pow(nh, exp) / denom;
            spec = lightToNorm * pS * spec * light;
        }
    }
    return spec;
}

void main() {
    vec4 nt = texture(uNormalAndTangent, vTC);
    vec4 tc = texture(uTCAndView, vTC);

    // reconstruct packed vectors
    vec3 fN = decode(nt.xy);
    vec3 fT = decode(nt.zw);
    vec3 fV = decode(tc.zw);

    if (uDiffuseMode) {
        float viewTerm = (1.0 - pow(1.0 - max(0.0, dot(fN, fV)) / 2.0, 5.0));
        vec3 diff = albedo(tc.xy);
        // FIXME lookup specular somehow? or just switch to perfect lambertian
        diff = viewTerm * diff * texture(uDiffuseIrradiance, uInvView * fN).rgb;
        fColor = vec4(diff, 1.0);
    } else {
        vec3 fB = normalize(cross(fN, fT));
        vec3 alb = albedo(tc.xy);
        vec2 shine = shininess(tc.xy);

        vec3 spec = vec3(0.0);
        for (int i = 0; i < NUM_SAMPLES; i++) {
            spec += lightSample(fN, fT, fB, fV, uLightDirection[i], uLightRadiance[i], alb, shine);
        }

        fColor = vec4(spec, 1.0);
    }
}
