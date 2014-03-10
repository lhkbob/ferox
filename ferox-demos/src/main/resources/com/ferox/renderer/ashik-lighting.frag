#version 150

const float PI = 3.1415927;

uniform sampler2D uNormalAndTangent;
uniform sampler2D uShininessAndView;
uniform sampler2D uDiffuseAlbedo;
uniform sampler2D uSpecularAlbedo;

uniform bool uDiffuseMode;
//uniform mat3 uInvView;
uniform samplerCube uIrradianceMin;
uniform samplerCube uIrradianceMax;

uniform float uShininessMin;
uniform float uShininessMax;

in vec2 vTC;
out vec4 fColor;

vec3 decode(vec2 r) {
    vec4 nn = vec4(2 * r, 0, 0) + vec4(-1, -1, 1, -1);
    float l = dot(nn.xyz, -nn.xyw);
    nn.z = l;
    nn.xy *= sqrt(l);
    return nn.xyz * 2 + vec3(0, 0, -1);
}

vec3 lookupSpecular(vec3 r, float exp) {
    if (uShininessMax < 0.0) {
        return texture(uIrradianceMax, r).rgb;
    } else {
        float alpha = (exp - uShininessMin) / (uShininessMax - uShininessMin);
        return mix(texture(uIrradianceMin, r).rgb, texture(uIrradianceMax, r).rgb, alpha);
    }
}

void main() {
    vec4 nt = texture(uNormalAndTangent, vTC);
    vec4 sv = texture(uShininessAndView, vTC);

    // reconstruct packed vectors
    vec3 fN = decode(nt.xy);
    vec3 fT = decode(nt.zw);
    vec3 fV = decode(sv.zw);

    if (uDiffuseMode) {
        float viewTerm = (1.0 - pow(1.0 - max(0.0, dot(fN, fV)) / 2.0, 5.0));
        vec3 diff = texture(uDiffuseAlbedo, vTC).rgb;
        vec3 spec = texture(uSpecularAlbedo, vTC).rgb;
        diff = viewTerm * diff * (1.0 - spec) * texture(uIrradianceMin, fN).rgb;
        fColor = vec4(diff, 1.0);
    } else {
        vec3 fB = normalize(cross(fN, fT));

        vec3 spec = vec3(0.0);
        if (dot(fN, fV) > 0.0) {
            vec3 fR = reflect(-fV, fN);
            vec2 shine = sv.xy;

            float exp = max(shine.x, shine.y);
            if (exp >= uShininessMin && (exp < uShininessMax || uShininessMax < 0.0)) {
                vec3 light = lookupSpecular(fR, exp);

                float lh = dot(fR, fN); // R is approximately L, N is approximately H
                spec = texture(uSpecularAlbedo, vTC).rgb;
                if (spec.r > 0.0 && spec.g > 0.0 && spec.b > 0.0) {
                    spec = spec + (vec3(1.0) - spec) * vec3(pow(1.0 - lh, 5)); // Schlick's approximation

                    // NOTE: we don't divide by max(lightToNorm, viewToNorm) because we're also not multiplying
                    // this term with the lightToNorm during the convolution (and we approximate that the denominator
                    // and numerator are equal and would cancel)
                    spec = sqrt((shine.x + 1) * (shine.y + 1.0)) / (8.0 * PI) / max(0.3, lh) * spec * light;
                }
            }
        }

        fColor = vec4(spec, 1.0);
    }
}
