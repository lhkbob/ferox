#version 150

const mat3 RGB_TO_XYZ = mat3(0.4124, 0.2126, 0.0193, 0.3576, 0.7152, 0.1192, 0.1805, 0.0722, 0.9502);
const mat3 XYZ_TO_RGB = mat3(3.2406, -0.9689, 0.0557, -1.5372, 1.8758, -0.2040, -0.4986, 0.0415, 1.0570);

uniform samplerCube uEnvMap;
uniform mat3 uEnvTransform;
uniform bool uUseEnvMap;

uniform float uExposure; // shutter
uniform float uSensitivity; // ISO
uniform float uFstop;
uniform float uGamma;

in vec3 vTC;
in vec4 vColor;

out vec4 fColor;

void main() {
    if (uUseEnvMap) {
        float factor = uExposure / (uFstop * uFstop) * uSensitivity * 0.65 / 10.0 * pow(118.0 / 255.0, uGamma);
        vec3 color = RGB_TO_XYZ * texture(uEnvMap, uEnvTransform * vTC).rgb;
        color = XYZ_TO_RGB * (factor * color);
        fColor = vec4(color, 1.0);
    } else {
        fColor = vColor;
    }
}
