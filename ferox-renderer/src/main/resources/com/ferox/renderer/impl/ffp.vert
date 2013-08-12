#version 150

uniform mat4 uModelview;
uniform mat4 uProjection;

uniform bool uEnableLighting;
uniform vec4 uGlobalLight;

uniform bool uEnableLight[8];
uniform vec4 uLightPos[8];
uniform vec4 uLightDiffuse[8];
uniform vec4 uLightAmbient[8];
uniform vec4 uLightSpecular[8];
uniform vec3 uSpotlightDirection[8];
uniform float uSpotlightCutoff[8];
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

out vec4 vFrontPrimaryColor;
out vec4 vBackPrimaryColor;
out vec4 vFrontSecondaryColor;
out vec4 vBackSecondaryColor;

void computeLighting(const int light, const vec4 eyePos, const vec3 eyeNorm,
                     out vec4 primaryColor, out vec4 secondaryColor) {
    vec3 vp;
    float attenuation = 1.0;
    float spot = 0.0;
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
        if (uSpotlightCutoff[light] == 180.0) {
            spot = 1.0;
        } else if (sdi >= cos(radians(uSpotlightCutoff[light]))) {
            spot = pow(sdi, uSpotlightExponent[light]);
        }
    }

    float di = max(dot(eyeNorm, vp), 0.0);
    primaryColor = attenuation * spot * (uMatAmbient * uLightAmbient[light] + di * aDiffuse * uLightDiffuse[light]);
    if (di > 0) {
        vec3 h = normalize(vp - eyeNorm);
        float si = pow(max(dot(eyeNorm, h), 0.0), uMatShininess);
        secondaryColor = attenuation * spot * si * uMatSpecular * uLightSpecular[light];
    } else {
        secondaryColor = vec4(0.0, 0.0, 0.0, 0.0);
    }
}

void main() {
    vec4 eyePos = uModelview * aVertex;
    vec4 frontPrimaryColor = aDiffuse;
    vec4 frontSecondaryColor = vec4(0.0, 0.0, 0.0, 1.0);

    vec4 backPrimaryColor = frontPrimaryColor;
    vec4 backSecondaryColor = frontSecondaryColor;

    if (uEnableLighting) {
        vec3 eyeNorm = normalize((transpose(inverse(mat3(uModelview))) * aNormal.xyz));
        vec3 backEyeNorm = -eyeNorm;

        frontPrimaryColor = uMatEmissive + uMatAmbient * uGlobalLight;
        backPrimaryColor = frontPrimaryColor;

        for (int i = 0; i < 8; i++) {
            if (uEnableLight[i]) {
                vec4 fp, fs, bp, bs;
                computeLighting(i, eyePos, eyeNorm, fp, fs);
                computeLighting(i, eyePos, backEyeNorm, bp, bs);

                frontPrimaryColor += fp;
                frontSecondaryColor += fs;
                backPrimaryColor += bp;
                backSecondaryColor += bs;
            }
        }
    }

    vFrontPrimaryColor = vec4(frontPrimaryColor.xyz, aDiffuse.w);
    vFrontSecondaryColor = vec4(frontSecondaryColor.xyz, 0.0);
    vBackPrimaryColor = vec4(backPrimaryColor.xyz, aDiffuse.w);
    vBackSecondaryColor = vec4(backSecondaryColor.xyz, 0.0);
    gl_Position = uProjection * eyePos;
}
