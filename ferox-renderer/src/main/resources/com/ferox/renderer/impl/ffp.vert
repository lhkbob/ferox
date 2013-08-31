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

uniform mat4 uTextureMatrix[4];
uniform mat4 uTexGenObjPlanes[4];
uniform mat4 uTexGenEyePlanes[4]; // assumed to be pre-multiplied by appropriate inverse modelview
uniform int uTexCoordSource[4]; // each component is a unit, ordinal value of TexCoordSource

in vec4 aVertex;
in vec3 aNormal;
in vec4 aDiffuse;

in vec4 aTexCoord[4];

out vec4 vPrimaryColor;
out vec4 vSecondaryColor;
out vec4 vTexCoord[4];
out vec4 vEyePos;

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

vec4 computeTextureCoord(const int tex, vec4 eyePos, vec3 eyeNorm) {
    vec4 tc;
    switch(uTexCoordSource[tex]) {
        case 0: // ATTRIBUTE
            tc = aTexCoord[tex];
            break;
        case 1: // EYE
            tc = uTexGenEyePlanes[tex] * eyePos;
            break;
        case 2: // OBJECT
            tc = uTexGenObjPlanes[tex] * aVertex;
            break;
        case 3: // SPHERE
            vec3 r = reflect(normalize(eyePos.xyz), eyeNorm);
            float m = 2.0 * sqrt(r.x * r.x + r.y * r.y + (r.z + 1.0) * (r.z + 1.0));
            tc = vec4(r.x / m + 0.5, r.y / m + 0.5, 0.0, 1.0);
            break;
        case 4: // NORMAL
            tc = vec4(eyeNorm, 1.0);
            break;
        case 5: // REFLECTION
            tc = vec4(reflect(normalize(eyePos.xyz), eyeNorm), 1.0);
            break;
    }
    return uTextureMatrix[tex] * tc;
}

void main() {
    vec4 eyePos = uModelview * aVertex;
    vec3 eyeNorm = normalize(uNormalMatrix * aNormal.xyz);

    vec4 primaryColor = aDiffuse;
    vec4 secondaryColor = vec4(0.0, 0.0, 0.0, 0.0);

    if (uEnableLighting) {
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

    vTexCoord[0] = computeTextureCoord(0, eyePos, eyeNorm);
    vTexCoord[1] = computeTextureCoord(1, eyePos, eyeNorm);
    vTexCoord[2] = computeTextureCoord(2, eyePos, eyeNorm);
    vTexCoord[3] = computeTextureCoord(3, eyePos, eyeNorm);

    vPrimaryColor = vec4(primaryColor.xyz, aDiffuse.w);
    vSecondaryColor = vec4(secondaryColor.xyz, 0.0);
    vEyePos = eyePos;
    gl_Position = uProjection * eyePos;
}
