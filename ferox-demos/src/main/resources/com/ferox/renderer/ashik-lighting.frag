#version 150

const float PI = 3.1415927;

uniform sampler2D uNormalAndTangent;
uniform sampler2D uShininessAndNTZ;
uniform sampler2D uSpecularAlbedo;
uniform sampler2D uDepth;

uniform mat3 uInvView;
uniform mat4 uInvProj;
uniform samplerCube uIrradianceMin;
uniform samplerCube uIrradianceMax;
uniform mat3 uEnvTransform;

uniform float uShininessMin;
uniform float uShininessMax;

in vec2 vTC;
out vec4 fColor;

vec3 lookupSpecular(vec3 r, float exp) {
    if (uShininessMax < 0.0) {
        return texture(uIrradianceMax, uEnvTransform * uInvView * r).rgb;
    } else {
        float alpha = (exp - uShininessMin) / (uShininessMax - uShininessMin);
        return mix(texture(uIrradianceMin, uEnvTransform * uInvView * r).rgb, texture(uIrradianceMax, uEnvTransform * uInvView * r).rgb, alpha);
    }
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

    vec3 spec = vec3(0.0);
    if (dot(fN, fV) > 0.0) {
        vec3 fR = reflect(-fV, fN);
        vec2 shine = sz.xy;

        float exp = max(shine.x, shine.y);
        if (exp >= uShininessMin && (exp < uShininessMax || uShininessMax < 0.0)) {
            vec3 light = lookupSpecular(fR, exp);

            float lh = dot(fR, fN); // R is approximately L, N is approximately H
            spec = texture(uSpecularAlbedo, vTC).rgb;
            if (spec.r > 0.0 || spec.g > 0.0 || spec.b > 0.0) {
//                    spec = spec + (vec3(1.0) - spec) * vec3(pow(1.0 - lh, 5)); // Schlick's approximation

                // NOTE: we don't divide by max(lightToNorm, viewToNorm) because we're also not multiplying
                // this term with the lightToNorm during the convolution (and we approximate that the denominator
                // and numerator are equal and would cancel)
                spec = (exp + 1) / (8.0 * PI) / max(0.3, lh) * spec * light;
            }
        }
    }

    fColor = vec4(spec, 0.0);
}
