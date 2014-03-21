#version 150

const float PI = 3.1415927;

uniform sampler2D uNormalAndTangent;
uniform sampler2D uShininessAndNTZ;
uniform sampler2D uSpecularAlbedo;
uniform sampler2D uDepth;

uniform mat4 uInvProj;
uniform mat3 uInvView;

uniform samplerCube uSpecularRadiance;
uniform sampler2D uNoise;
uniform mat3 uEnvTransform;

const int SAMPLES = 32;
uniform vec3 uSample[SAMPLES];

in vec2 vTC;
out vec4 fColor;

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

    vec3 specular = texture(uSpecularAlbedo, vTC).rgb;

    vec2 shine = sz.xy;
    mat3 basis = mat3(fT, fB, fN);
    vec3 specLight = vec3(0.0);

    vec4 u1 = texture(uNoise, vTC);
    float viewToNorm = max(0.0, dot(fN, fV));

    // add in samples for specular using importance sampling
    if (specular.r > 0.0 || specular.g > 0.0 || specular.b > 0.0) {
        for (int i = 0; i < SAMPLES; i++) {
            vec3 u2 = uSample[i];
            // hack to get per-pixel random variables from the uniform samples that change
            float e1 = fract(377.23 * (u2.x * u1.x + u2.z * u1.y));
            float e2 = fract(541.34 * (u2.y * u1.z + u2.z * u1.w));

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
            vec3 fH = basis * vec3(cosPhi * radius, sinPhi * radius, cosTheta);

            vec3 fL = reflect(-fV, fH);

            float lightToNorm = max(0.0, dot(fN, fL));
            vec3 spec = vec3(0.0);
            if (lightToNorm > 0.0) {
                // THIS IS SIMPLIFIED BECAUSE DIVIDING BY THE PDF CANCELS OUT MANY TERMS
                // so I've optimized them away
                float lh = max(0.0, dot(fL, fH));

                spec = specular + (vec3(1.0) - specular) * vec3(pow(1.0 - lh, 5)); // fresnel
                float pS = 1.0 / max(lightToNorm, viewToNorm);
                spec = lightToNorm * pS * spec * texture(uSpecularRadiance, uEnvTransform * uInvView * fL).rgb;
            }

            specLight = specLight + spec;
        }
    }
    fColor = vec4(specLight, SAMPLES);
}
