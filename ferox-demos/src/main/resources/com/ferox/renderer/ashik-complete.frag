#version 150

const float PI = 3.1415927;

uniform sampler2D uNormalAndTangent;
uniform sampler2D uShininessAndNTZ;
uniform sampler2D uDiffuseAlbedo;
uniform sampler2D uSpecularAlbedo;
uniform sampler2D uDepth;

uniform mat4 uInvProj;

uniform vec3 uLightDirection[40];
uniform vec3 uLightRadiance[40];

in vec2 vTC;
out vec4 fColor;

vec3 decode(vec2 r) {
    vec4 nn = vec4(2 * r, 0, 0) + vec4(-1, -1, 1, -1);
    float l = dot(nn.xyz, -nn.xyw);
    nn.z = l;
    nn.xy *= sqrt(l);
    return nn.xyz * 2 + vec3(0, 0, -1);
}

vec3 lightSample(vec3 fN, vec3 fT, vec3 fB, vec3 fV, vec3 fL, vec3 light, vec3 diffuse, vec3 specular, vec2 shine) {
    vec3 fH = normalize(fV + fL);

    float lightToNorm = max(0.0, dot(fN, fL));
    float viewToNorm = max(0.0, dot(fN, fV));
    vec3 spec = vec3(0.0);
    if (lightToNorm > 0.0) {// && viewToNorm > 0.0) {
        float lh = max(0.0, dot(fL, fH));
        float th = dot(fT, fH);
        float bh = dot(fB, fH);
        float nh = dot(fN, fH);

//        float pD = 28.0 / (23.0 * PI) * (1.0 - pow(1.0 - lightToNorm / 2.0, 5.0)) * (1.0 - pow(1.0 - viewToNorm / 2.0, 5.0));
//        vec3 diff = pD * (1.0 - specular) * diffuse;

        if (specular.r > 0.0 || specular.g > 0.0 || specular.b > 0.0) {
            spec = specular;
            spec = spec + (vec3(1.0) - spec) * vec3(pow(1.0 - lh, 5)); // fresnel
            float exp = (shine.x * th * th + shine.y * bh * bh) / (1.0 - nh * nh);
            float denom = lh * max(lightToNorm, viewToNorm);
            float pS = sqrt((shine.x + 1.0) * (shine.y + 1.0)) / (8.0 * PI) * pow(nh, exp) / denom;
            spec = pS * spec;
        }

        spec = lightToNorm * spec * light;
    }
    return spec;
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

    vec3 illum = vec3(0.0);
    for (int i = 0; i < 40; i++) {
        illum = illum + lightSample(fN, fT, fB, fV, uLightDirection[i], uLightRadiance[i], diff, spec, sz.xy);
    }

    fColor = vec4(illum, 0.0);
}
