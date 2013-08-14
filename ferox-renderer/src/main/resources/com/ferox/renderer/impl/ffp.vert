#version 150

uniform mat4 uModelview;
uniform mat3 uNormalMatrix; // compute on CPU to improve performance when vertex bound
uniform mat4 uProjection;

uniform bool uEnableLighting;
uniform vec4 uGlobalLight;

uniform bool uEnableLight[8];
uniform vec4 uLightPos[8];
uniform vec4 uLightDiffuse[8];
uniform vec4 uLightAmbient[8];
uniform vec4 uLightSpecular[8];
uniform vec3 uSpotlightDirection[8]; // assumed to be normalized
uniform float uSpotlightCutoff[8]; // cached to cos of actual angle, so 180 -> -1.0
uniform float uSpotlightExponent[8];
uniform vec3 uLightAttenuation[8];

uniform vec4 uMatAmbient;
uniform vec4 uMatSpecular;
uniform vec4 uMatEmissive;
uniform float uMatShininess;

in vec4 aVertex;
in vec3 aNormal;
in vec4 aDiffuse;

// FIXME add texture coordinates

out vec4 vPrimaryColor;
out vec4 vSecondaryColor;

void computeLighting(const int light, const vec4 eyePos, const vec3 eyeNorm,
                     out vec4 primaryColor, out vec4 secondaryColor) {
    vec3 vp;
    float attenuation = 1.0;
    float spot = 1.0;
    if (uLightPos[light].w == 0.0) {
        // DIRECTION LIGHT
        vp = normalize(uLightPos[light].xyz);
        // don't need to compute attenuation or spot
    } else {
        // POINT OR SPOT LIGHT
        vp = uLightPos[light].xyz - eyePos.xyz;
        float d = length(vp);
        vp = vp / d;

        attenuation = 1.0 / (uLightAttenuation[light].x + uLightAttenuation[light].y * d + uLightAttenuation[light].z * d * d);
        float sdi = max(dot(-vp, uSpotlightDirection[light]), 0.0);
        if (uSpotlightCutoff[light] != -1.0) {
            if (sdi >= uSpotlightCutoff[light]) {
                spot = pow(sdi, uSpotlightExponent[light]);
            } else {
                spot = 0.0;
            }
        }
    }

    float di = max(dot(eyeNorm, vp), 0.0);
    primaryColor = attenuation * spot * (uMatAmbient * uLightAmbient[light] + di * aDiffuse * uLightDiffuse[light]);

    if (di > 0) {
        vec3 h = normalize(vp - normalize(eyePos.xyz));
        float si = pow(max(dot(eyeNorm, h), 0.0), uMatShininess);
        secondaryColor = attenuation * spot * si * uMatSpecular * uLightSpecular[light];
    } else {
        secondaryColor = vec4(0.0, 0.0, 0.0, 0.0);
    }
}

void main() {
    vec4 eyePos = uModelview * aVertex;
    vec4 primaryColor = aDiffuse;
    vec4 secondaryColor = vec4(0.0, 0.0, 0.0, 0.0);

    if (uEnableLighting) {
        vec3 eyeNorm = normalize(uNormalMatrix * aNormal.xyz);
        vec3 backEyeNorm = -eyeNorm;

        primaryColor = uMatEmissive + uMatAmbient * uGlobalLight;

        for (int i = 0; i < 8; i++) {
            if (uEnableLight[i]) {
                vec4 fp, fs;
                computeLighting(i, eyePos, eyeNorm, fp, fs);

                primaryColor += fp;
                secondaryColor += fs;
            }
        }
    }

    vPrimaryColor = vec4(primaryColor.xyz, aDiffuse.w);
    vSecondaryColor = vec4(secondaryColor.xyz, 0.0);
    gl_Position = uProjection * eyePos;
}
