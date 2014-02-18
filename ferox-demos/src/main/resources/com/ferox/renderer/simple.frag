#version 150

uniform vec4 uSolidColor;

uniform samplerCube uEnvMap;
uniform bool uUseEnvMap;

in vec3 vTC;

out vec4 fColor;

void main() {
    if (uUseEnvMap) {

        fColor = texture(uEnvMap, vTC);
    } else {
        fColor = uSolidColor;
    }
}
