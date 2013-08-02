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

out vec4 vPrimaryColor;
out vec4 vSecondaryColor;

void main() {
    vec4 eyePos = uModelview * aVertex;
    vec4 primaryColor = aDiffuse;
    vec4 secondaryColor = vec4(0.0, 0.0, 0.0, 1.0);

    if (uEnableLighting) {
        vec3 eyeNorm = normalize((transpose(inverse(mat3(uModelview))) * aNormal.xyz));

        primaryColor = uMatEmissive + uMatAmbient * uGlobalLight;

        for (int i = 0; i < 8; i++) {
            if (uEnableLight[i]) {
                vec3 lightContrib = uMatAmbient * uLightAmbient[i];

                vec3 vp;
                float attenuation = 1.0;
                float spot = 0.0;
                if (uLightPos[i].w == 0.0) {
                    // DIRECTION LIGHT
                    vp = normalize(uLightPos[i].xyz);
                    // don't need to compute attenuation or spot
                } else {
                    // POINT OR SPOT LIGHT
                    vp = uLightPos[i].xyz - eyePos;
                    float d = length(vp);
                    vp = vp / d;

                    attenuation = 1.0 / (uLightAttenuation[i].x + uLightAttenuation[i].y * d + uLightAttenuation[i].z * d * d);
                    float sdi = max(dot(-vp, uSpotlightDirection[i]), 0.0);
                    if (uSpotlightDirection[i].w == 180.0) {
                        spot = 1.0;
                    } else if (sdi >= cos(radians(uSpotlightCutoff[i]))) {
                        spot = exp(sdi, uSpotlightExponent[i]);
                    }
                }

                float di = max(dot(eyeNorm, vp), 0.0);
                primaryColor = primaryColor + attenuation * spot * (uMatAmbient * uLightAmbient[i] + di * aDiffuse * uLightDiffuse[i]);
                if (di > 0) {
                    vec3 h = normalize(vp - eyeNorm);
                    float si = exp(max(dot(eyeNorm, h), 0.0), uMatShininess);
                    secondaryColor = secondaryColor + attenuation * spot * si * uMatSpecular * uLightSpecular[i];
                }
            }
        }
    }

    primaryColor.w = 1.0;
    secondaryColor.w = 1.0;
    vPrimaryColor = primaryColor;
    vSecondaryColor = secondaryColor;
    gl_Position = uProjection * eyePos;
}

