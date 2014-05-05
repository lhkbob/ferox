#version 150

const float PI = 3.1415927;

uniform sampler2D uDiffuse;
uniform sampler2D uSpecular;

in vec2 vTC;
out float fLuminance;

void main() {
    vec4 spec = texture(uSpecular, vTC);
    if (spec.a > 0.0) {
        spec = spec / spec.a;

    }
    vec3 high = texture(uDiffuse, vTC).rgb + spec.rgb;
    fLuminance = dot(vec3(0.2126, 0.7152, 0.0722), high);
}
