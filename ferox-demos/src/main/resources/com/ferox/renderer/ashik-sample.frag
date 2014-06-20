#version 150

const float PI = 3.1415927;

uniform sampler2D uNormalAndTangent;
uniform sampler2D uShininessAndNTZ;
uniform sampler2D uSpecularAlbedo;
uniform sampler2D uDepth;

uniform mat4 uInvProj;
uniform mat3 uInvView;

//uniform samplerCube uSpecularRadiance;
uniform sampler2D uDualPos;
uniform sampler2D uDualNeg;
uniform sampler2D uNoise;
uniform mat3 uEnvTransform;
uniform int uTotalSamples;

const int SAMPLES = 32;
uniform vec4 uSample[SAMPLES];

uniform ivec2 uEnvSize;

in vec2 vTC;
out vec4 fColor;

float samplePhi(float u, vec2 shine) {
    if (u < 0.25) {
        return atan(sqrt((shine.x + 1) / (shine.y + 1)) * tan(2.0 * PI  * u));
    } else if (u < 0.5) {
        return PI - atan(sqrt((shine.x + 1) / (shine.y + 1)) * tan(2.0 * PI * (0.5 - u)));
    } else if (u < 0.75) {
        return PI + atan(sqrt((shine.x + 1) / (shine.y + 1)) * tan(2.0 * PI * (u - 0.5)));
    } else {
        return 2.0 * PI - atan(sqrt((shine.x + 1) / (shine.y + 1)) * tan(2.0 * PI * (1.0 - u)));
    }
}

void main() {
    vec4 nt = texture(uNormalAndTangent, vTC);
    vec4 sz = texture(uShininessAndNTZ, vTC);
    float depth = texture(uDepth, vTC).r;

    // reconstruct packed vectors
    vec3 fN = normalize(vec3(nt.xy, sz.z));
    vec3 fT = vec3(nt.zw, sz.w);
    vec3 fB = normalize(cross(fN, fT));
    fT = normalize(cross(fB, fN));

    vec4 viewPos = uInvProj * (2.0 * vec4(vTC, depth, 1.0) - 1.0);
    viewPos = viewPos / viewPos.w;

    vec3 fV = -normalize(viewPos.xyz);

    vec3 specular = texture(uSpecularAlbedo, vTC).rgb;

    vec2 shine = sz.xy;
    mat3 basis = mat3(fT, fB, fN);
    vec3 specLight = vec3(0.0);

    vec4 u1 = texture(uNoise, vTC);
    float viewToNorm = max(0.0, dot(fN, fV));

    mat3 envTrans = uEnvTransform * uInvView;
    float pdfFactor = sqrt((shine.x + 1.0) * (shine.y + 1.0)) / (2.0 * PI);
    // add in samples for specular using importance sampling
    if (specular.r > 0.0 || specular.g > 0.0 || specular.b > 0.0) {
        for (int i = 0; i < SAMPLES; i++) {
            vec4 u2 = uSample[i];
            // hack to get per-pixel random variables from the uniform samples that change
            float e1 = fract(u2.x + u1.x + u2.z * u1.z);
            float e2 = fract(u2.y + u1.y + u2.w * u1.w);
//            float e1 = fract(u1.x + u2.x);
//            float e2 = fract(u1.y + u2.y);
//            float e1 = u2.x;
//            float e2 = u2.y;

            float phi = samplePhi(e1, shine);

            float cosPhi = cos(phi);
            float sinPhi = sin(phi);

            float exp = (shine.x * cosPhi * cosPhi + shine.y * sinPhi * sinPhi);
            float cosTheta = pow((1 - e2), 1.0 / (exp + 1.0));
            float radius = sqrt(1.0 - cosTheta * cosTheta);
            vec3 fH = basis * vec3(cosPhi * radius, sinPhi * radius, cosTheta);
            vec3 fL = reflect(-fV, fH);

            float pdf = pdfFactor * pow(dot(fH, fN), exp);

            vec3 dir = envTrans * normalize(fL); // for sampling dual paraboloid
            float d = 4.0 * 1.2 * 1.2 * (abs(dir.z) + 1.0) * (abs(dir.z) + 1.0);

            float level = 0.0; //max(0.5 * log2(uEnvSize.x * uEnvSize.y / (uTotalSamples + SAMPLES)) - 0.5 * log2(pdf * d), 0.0);

            float lightToNorm = max(0.0, dot(fN, fL));
            vec3 spec = vec3(0.0);
            // THIS IS SIMPLIFIED BECAUSE DIVIDING BY THE PDF CANCELS OUT MANY TERMS
            // so I've optimized them away
            float lh = max(0.0, dot(fL, fH));

            spec = specular + (vec3(1.0) - specular) * vec3(pow(1.0 - lh, 5)); // fresnel
            float pS = 1.0 / max(lightToNorm, viewToNorm);


            vec3 light;
            if (dir.z >= 0) {
                float t = (1.0 - dir.z) / (dir.x * dir.x + dir.y * dir.y);
                vec2 st = 0.5 * vec2(t * dir.x, t * dir.y) + 0.5;
                light = textureLod(uDualPos, st, level).rgb;
            } else {
                float t = (1.0 + dir.z) / (dir.x * dir.x + dir.y * dir.y);
                vec2 st = 0.5 * vec2(t * dir.x, t * dir.y) + 0.5;
                light = textureLod(uDualNeg, st, level).rgb;
            }
            spec = lightToNorm * pS * spec * light;
            specLight = specLight + spec;
        }
    }
    fColor = vec4(specLight, SAMPLES);
}
