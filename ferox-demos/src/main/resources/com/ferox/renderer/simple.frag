#version 150

const mat3 RGB_TO_XYZ = mat3(0.4124, 0.2126, 0.0193, 0.3576, 0.7152, 0.1192, 0.1805, 0.0722, 0.9502);
const mat3 XYZ_TO_RGB = mat3(3.2406, -0.9689, 0.0557, -1.5372, 1.8758, -0.2040, -0.4986, 0.0415, 1.0570);

//uniform samplerCube uEnvMap;
uniform sampler2D uDualNeg;
uniform sampler2D uDualPos;

uniform mat3 uEnvTransform;
uniform bool uUseEnvMap;
uniform float uEnvLevel;

uniform float uPreScale;
uniform float uPostScale;
uniform float uBurn;
uniform float uAvgLuminance;

in vec3 vTCDir;
in vec2 vTC;
in vec4 vColor;

out vec4 fColor;

void main() {
    if (uUseEnvMap) {
        vec3 dir = uEnvTransform * normalize(vTCDir);
        vec3 color;
        if (dir.z >= 0) {
            float t = (1.0 - dir.z) / (dir.x * dir.x + dir.y * dir.y);
            vec2 st = 0.5 * vec2(t * dir.x, t * dir.y) + 0.5;
            color = textureLod(uDualPos, st, uEnvLevel).rgb;
        } else {
            float t = (1.0 + dir.z) / (dir.x * dir.x + dir.y * dir.y);
            vec2 st = 0.5 * vec2(t * dir.x, t * dir.y) + 0.5;
            color = textureLod(uDualNeg, st, uEnvLevel).rgb;
        }

        color = RGB_TO_XYZ * color;
//        vec3 color = RGB_TO_XYZ * textureLod(uEnvMap, uEnvTransform * vTC, uEnvLevel).rgb;

        float Ywa = uAvgLuminance;
        if (Ywa < 0.0)
            Ywa = 5.0;

        float alpha = 0.1;

        float Yw = uPreScale * alpha * uBurn;
        float invY2 = 1.0 / (Yw * Yw);
        float pScale = uPostScale * uPreScale * alpha / Ywa;

        color = color * (pScale * (1.0 + color.g * invY2) / (1.0 + color.g));
        color = XYZ_TO_RGB * color;

        fColor = vec4(color, 1.0);
    } else {
        fColor = vColor;
    }
}
