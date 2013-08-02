#version 150

uniform bool uEnableAlphaTest;
uniform uint uAlphaComparison; // ordinal of Comparison
uniform float uAlphaRefValue;

uniform vec3 uFogConfig; // 0 = start/density 1 = end 2 = signal (0 = linear, > = exp, < = exp squared)
uniform vec4 uFogColor;
uniform bool uEnableFog;

// FIXME add texture uniforms somehow

in vec4 vPrimaryColor;
in vec4 vSecondaryColor;

// FIXME add interpolated texture coordinates

out vec4 fColor;

void main() {
    // FIXME apply texturing to vPrimaryColor before color sum
    vec4 color = vPrimaryColor + vec4(vSecondaryColor.xyz, 0.0);

    // fog
    if (uEnableFog) {
        float factor = 0.0;
        if (uFogConfig.z > 0) {
            // EXP
            factor = exp(-uFogConfig.x * gl_FragDepth);
        } else if (uFogConfig.z < 0) {
            // EXP2
            factor = exp(-(uFogConfig.x * uFogConfig.x * gl_FragDepth * gl_FragDepth));
        } else {
            // LINEAR
            factor = (uFogConfig.y - gl_FragDepth) / (uFogConfig.y - uFogConfig.x);
        }

        // clamp to [0, 1]
        if (factor < 0.0) {
            factor = 0.0;
        } else if (factor > 1.0) {
            factor = 1.0;
        }

        // blend with fog color
        color = factor * color + (1.0 - factor) * uFogColor;
    }

    // alpha test
    if (uEnableAlphaTest) {
        if (uAlphaComparison == 0) {
            // EQUAL
            if (uAlphaRefValue != color.a) {
                discard;
            }
        } else if (uAlphaComparison == 1) {
            // GREATER
            if (uAlphaRefValue <= color.a) {
                discard;
            }
        } else if (uAlphaComparison == 2) {
            // LESS
            if (uAlphaRefValue >= color.a) {
                discard;
            }
        } else if (uAlphaComparison == 3) {
            // GEQUAL
            if (uAlphaRefValue < color.a) {
                discard;
            }
        } else if (uAlphaComparison == 4) {
            // LEQUAL
            if (uAlphaRefValue > color.a) {
                discard;
            }
        } else if (uAlphaComparison == 5) {
            // NOT_EQUAL
            if (uAlphaRefValue == color.a) {
                discard;
            }
        } else if (uAlphaComparison == 6) {
            // NEVER
            discard;
        }
        // else it's ALWAYS so no test is necessary
    }

    fColor = color;
}
