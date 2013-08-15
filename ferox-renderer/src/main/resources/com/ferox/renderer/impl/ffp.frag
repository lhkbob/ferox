#version 150

uniform int uAlphaComparison; // ordinal of Comparison, or -1 to disable
uniform float uAlphaRefValue;

uniform vec3 uFogConfig; // 0 = start/density 1 = end 2 = signal (0 = linear, > = exp, < = exp squared)
uniform vec4 uFogColor;
uniform bool uEnableFog;

// only one of the 4 sampler types will actually be in use, but we know 16 units are available because that
// is the minimum required for OpenGL 3
uniform sampler1D uTex1D[4];
uniform sampler2D uTex2D[4];
uniform sampler3D uTex3D[4];
uniform samplerCube uTexCube[4];
uniform int uTexConfig[4]; // neg. value is disabled, 0 = 1D, 1 = 2D, 2 = 3D, 3 = cube
uniform int uDepthComparison[4]; // -1 for disabled, otherwise ordinal of Comparison

uniform ivec3 uCombineSrcAlpha[4]; // xyz represent 0,1,2 arguments to the functions
uniform ivec3 uCombineSrcRGB[4]; // values are ordinal of CombineSource and CombineOperand
uniform ivec3 uCombineOpAlpha[4];
uniform ivec3 uCombineOpRGB[4];
uniform int uCombineFuncAlpha;
uniform int uCombineFuncRGB;
uniform vec4 uCombineColor[4];

in vec4 vPrimaryColor;
in vec4 vSecondaryColor;
in vec4 vTexCoord[4];

out vec4 fColor;

vec4 sampleTexture(const int tex) {
// FIXME perform depth comparison here
    vec4 s = vec4(0.0, 0.0, 0.0, 1.0); // DISABLED
    if (uTexConfig[tex] == 0) {
        // 1D
        float coord = vTexCoord[tex].s / vTexCoord[tex].q;
        s = texture(uTex1D[tex], coord);
    } else if (uTexConfig[tex] == 1) {
        // 2D
        vec2 coord = vTexCoord[tex].st / vTexCoord[tex].q;
        s = texture(uTex2D[tex], coord);
    } else if (uTexConfig[tex] == 2) {
        // 3D
        vec3 coord = vTexCoord[tex].str / vTexCoord[tex].q;
        s = texture(uTex3D[tex], coord);
    } else if (uTexConfig[tex] == 3) {
        // cube
        vec3 coord = vTexCoord[tex].str; // no divide needed for cube maps
        s = texture(uTexCube[tex]);
    }
    return s;
}

vec4 select(const vec4 tex0, const vec4 tex1, const vec4 tex2, const vec4 tex3,
            const vec4 currTex, const vec4 prevTex, const vec4 primary, const vec4 constant,
            const int srcRGB, const int opRGB, const int srcAlpha, const int opAlpha) {
    vec4 selectRGB = vec4(0.0, 0.0, 0.0, 1.0);
    float selectAlpha = 0.0;
    vec3 postRGB = vec3(0.0, 0.0, 0.0);
    float postAlpha = 0.0;

    // compute postRGB = opRGB(srcRBG)
    if (srcRGB == 0) {
        // CURR_TEX
        selectRGB = currTex;
    } else if (srcRGB == 1) {
        // PREV_TEX
        selectRGB = prevTex;
    } else if (srcRGB == 2) {
        // CONSTANT
        selectRGB = constant;
    } else if (srcRGB == 3) {
        // PRIMARY
        selectRGB = primary;
    } else if (srcRGB == 4) {
        // TEX0
        selectRGB = tex0;
    } else if (srcRGB == 5) {
        // TEX1
        selectRGB = tex1;
    } else if (srcRGB == 6) {
        // TEX2
        selectRGB = tex2;
    } else if (srcRGB == 7) {
        // TEX3
        selectRGB = tex3;
    }
    if (opRGB == 0) {
        // COLOR
        postRGB = selectRGB.rgb;
    } else if (opRGB == 1) {
        // ALPHA
        postRGB = vec3(selectRGB.a);
    } else if (opRGB == 2) {
        // ONE_MINUS_COLOR
        postRGB = vec3(1.0) - selectRGB.rgb;
    } else if (opRGB == 3) {
        // ONE_MINUS_ALPHA
        postRGB = vec3(1.0 - selectRGB.a);
    }

    // compute postAlpha = opAlpha(srcAlpha)
    if (srcAlpha == 0) {
        // CURR_TEX
        selectAlpha = currTex.a;
    } else if (srcAlpha == 1) {
        // PREV_TEX
        selectAlpha = prevTex.a;
    } else if (srcAlpha == 2) {
        // CONSTANT
        selectAlpha = constant.a;
    } else if (srcAlpha == 3) {
        // PRIMARY
        selectAlpha = primary.a;
    } else if (srcAlpha == 4) {
        // TEX0
        selectAlpha = tex0.a;
    } else if (srcAlpha == 5) {
        // TEX1
        selectAlpha = tex1.a;
    } else if (srcAlpha == 6) {
        // TEX2
        selectAlpha = tex2.a;
    } else if (srcAlpha == 7) {
        // TEX3
        selectAlpha = tex3.a;
    }
    if (opAlpha == 1) {
        // ALPHA
        postAlpha = selectAlpha;
    } else if (opAlpha == 3) {
        // ONE_MINUS_ALPHA
        postAlpha = 1.0 - selectAlpha;
    } // 0 and 2 (COLOR and ONE_MINUS_COLOR) are not valid for opAlpha

    return vec4(postRGB, postAlpha);
}

float dot3(vec3 arg0, vec3 arg1) {
    float f = 4.0 * dot(arg0 - vec3(0.5), arg1 - vec3(0.5));
    return f;
}

vec4 evaluateUnit(const int unit, const vec4 tex[4], const vec4 prevColor) {
    ivec3 srcRGB = uCombineSrcRGB[unit];
    ivec3 srcAlpha = uCombineSrcAlpha[unit];
    ivec3 opRGB = uCombineOpRGB[unit];
    ivec3 opAlpha = uCombineOpAlpha[unit];

    arg0 = select(tex[0], tex[1], tex[2], tex[3], tex[unit], prevColor, vPrimaryColor, uCombineColor[unit],
                  srcRGB.x, opRGB.x, srcAlpha.x, opAlpha.x);
    arg1 = select(tex[0], tex[1], tex[2], tex[3], tex[unit], prevColor, vPrimaryColor, uCombineColor[unit],
                  srcRGB.y, opRGB.y, srcAlpha.y, opAlpha.y);
    arg2 = select(tex[0], tex[1], tex[2], tex[3], tex[unit], prevColor, vPrimaryColor, uCombineColor[unit],
                  srcRGB.z, opRGB.z, srcAlpha.z, opAlpha.z);

    float alpha = 0.0;
    if (uCombineFuncAlpha[unit] == 0) {
        // REPLACE
        alpha = arg0.a;
    } else if (uCombineFuncAlpha[unit] == 1) {
        // MODULATE
        alpha = arg0.a * arg1.a;
    } else if (uCombineFuncAlpha[unit] == 2) {
        // ADD
        alpha = arg0.a + arg1.a;
    } else if (uCombineFuncAlpha[unit] == 3) {
        // ADD_SIGNED
        alpha = arg0.a + arg1.a - 0.5;
    } else if (uCombineFuncAlpha[unit] == 4) {
        // INTERPOLATE
        alpha = arg0.a * arg2.a + arg1.a * (1.0 - arg2.a);
    } else if (uCombineFuncAlpha[unit] == 5) {
        // SUBTRACT
        alpha = arg0.a - arg1.a;
    } // ignore 6 and 7 (DOT3_RGB... since they ignore alpha)

    vec3 rgb = vec3(0.0);
    if (uCombineFuncRGB[0] == 0) {
        // REPLACE
        rgb = arg0.rgb;
    } else if (uCombineFuncRGB[0] == 1) {
        // MODULATE
        rgb = arg0.rgb * arg1.rgb;
    } else if (uCombineFuncRGB[0] == 2) {
        // ADD
        rgb = arg0.rgb + arg1.rgb;
    } else if (uCombineFuncRGB[0] == 3) {
        // ADD_SIGNED
        rgb = arg0.rgb + arg1.rgb - vec3(0.5);
    } else if (uCombineFuncRGB[0] == 4) {
        // INTERPOLATE
        rgb = arg0.rgb * arg2.rgb + arg1.rgb * (vec3(1.0) - arg2.rgb);
    } else if (uCombineFuncRGB[0] == 5) {
        // SUBTRACT
        rgb = arg0.rgb - arg1.rgb;
    } else if (uCombineFuncRGB[0] == 6) {
        // DOT3_RGB
        rgb = vec3(dot3(arg0.rgb, arg1.rgb));
    } else if (uCombineFuncRGB[0] == 7) {
        // DOT3_RGBA
        float d = dot3(arg0.rgb, arg1.rgb);
        rgb = vec3(d);
        alpha = d; // override the alpha
    }

    return vec4(rgb, alpha);
}

void main() {
    vec4 tex[4];
    tex[0] = sampleTexture(0);
    tex[1] = sampleTexture(1);
    tex[2] = sampleTexture(2);
    tex[3] = sampleTexture(3);

    vec4 prev = vPrimaryColor;

    if (uTexConfig[0] >= 0) {
        prev = evaluateUnit(0, tex, prev);
    }
    if (uTexConfig[1] >= 0) {
        prev = evaluateUnit(1, tex, prev);
    }
    if (uTexConfig[2] >= 0) {
        prev = evaluateUnit(2, tex, prev);
    }
    if (uTexConfig[3] >= 0) {
        prev = evaluateUnit(3, tex, prev);
    }

    vec4 color = prev + vSecondaryColor;

    // fog
    if (uEnableFog) {
    // FIXME I think this part is broken
        float factor = 0.0;
        if (uFogConfig.z > 0) {
            // EXP
            factor = exp(-uFogConfig.x * gl_FragCoord.z);
        } else if (uFogConfig.z < 0) {
            // EXP2
            factor = exp(-(uFogConfig.x * uFogConfig.x * gl_FragCoord.z * gl_FragCoord.z));
        } else {
            // LINEAR
            factor = (uFogConfig.y - gl_FragCoord.z) / (uFogConfig.y - uFogConfig.x);
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
    // FIXME move this into a compare() function that takes the value, ref value, and test and reports true or false
    if (uAlphaComparison >= 0) {
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
