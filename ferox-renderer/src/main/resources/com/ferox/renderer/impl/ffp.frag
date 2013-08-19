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
uniform int uCombineFuncAlpha[4];
uniform int uCombineFuncRGB[4];
uniform vec4 uCombineColor[4];

in vec4 vPrimaryColor;
in vec4 vSecondaryColor;
in vec4 vTexCoord[4];

out vec4 fColor;

bool compare(const float testValue, const float refValue, const int test) {
    switch(test) {
        case 0: // EQUAL
            return testValue == refValue;
        case 1: // GREATER
            return testValue > refValue;
        case 2: // LESS
            return testValue < refValue;
        case 3: // GEQUAL
            return testValue >= refValue;
        case 4: // LEQUAL
            return testValue <= refValue;
        case 5: // NOT_EQUAL
            return testValue != refValue;
        case 6: // NEVER
            return false;
        default: // ALWAYS
            return true;
    }
}

vec4 sampleTexture(const int tex) {
    switch(uTexConfig[tex]) {
        case 0: // 1D
            float coord1 = vTexCoord[tex].s / vTexCoord[tex].q;
            return texture(uTex1D[tex], coord1);
            //return vec4(1.0, 0.0, 0.0, 1.0);
        case 1: // 2D
            if (uDepthComparison[tex] >= 0) {
                // depth comparison
                vec3 coord = vTexCoord[tex].stp / vTexCoord[tex].q;
                float d = texture(uTex2D[tex], coord.st).r;

                if (compare(d, coord.p, uDepthComparison[tex])) {
                    return vec4(1.0);
                } else {
                    return vec4(0.0);
                }
                //return vec4(0.0, 0.5, 0.5, 1.0);
            } else {
                // regular texture
                vec2 coord = vTexCoord[tex].st / vTexCoord[tex].q;
                return texture(uTex2D[tex], coord);
                //return vec4(0.0, 1.0, 0.0, 1.0);
            }
        case 2: // 3D
            vec3 coord3 = vTexCoord[tex].stp / vTexCoord[tex].q;
            return texture(uTex3D[tex], coord3);
            //return vec4(0.0, 0.0, 1.0, 1.0);
        case 3: // CUBE
            vec3 coord4 = vTexCoord[tex].stp; // no divide needed for cube maps
            return texture(uTexCube[tex], coord4);
            //return vec4(1.0, 1.0, 0.0, 1.0);
        default: // disabled
            return vec4(1.0, 0.0, 1.0, 1.0);
    }
}

vec4 select(const vec4 tex0, const vec4 tex1, const vec4 tex2, const vec4 tex3,
            const vec4 currTex, const vec4 prevTex, const vec4 primary, const vec4 constant,
            const int srcRGB, const int opRGB, const int srcAlpha, const int opAlpha) {
    vec4 selectRGB = vec4(0.0, 0.0, 0.0, 1.0);
    float selectAlpha = 0.0;
    vec3 postRGB = vec3(0.0, 0.0, 0.0);
    float postAlpha = 0.0;

    // compute postRGB = opRGB(srcRBG)
    switch(srcRGB) {
        case 0: // CURR_TEX
            selectRGB = currTex;
            break;
        case 1: // PREV_TEX
            selectRGB = prevTex;
            break;
        case 2: // CONSTANT
            selectRGB = constant;
            break;
        case 3: // PRIMARY
            selectRGB = primary;
            break;
        case 4: // TEX0
            selectRGB = tex0;
            break;
        case 5: // TEX1
            selectRGB = tex1;
            break;
        case 6: // TEX2
            selectRGB = tex2;
        case 7: // TEX3
            selectRGB = tex3;
    }
    switch(opRGB) {
        case 0: // COLOR
            postRGB = selectRGB.rgb;
            break;
        case 1: // ALPHA
            postRGB = vec3(selectRGB.a);
            break;
        case 2: // ONE_MINUS_COLOR
            postRGB = vec3(1.0) - selectRGB.rgb;
            break;
        case 3: // ONE_MINUS_ALPHA
            postRGB = vec3(1.0 - selectRGB.a);
            break;
    }

    // compute postAlpha = opAlpha(srcAlpha)
    switch(srcAlpha) {
        case 0: // CURR_TEX
            selectAlpha = currTex.a;
            break;
        case 1: // PREV_TEX
            selectAlpha = prevTex.a;
            break;
        case 2: // CONSTANT
            selectAlpha = constant.a;
            break;
        case 3: // PRIMARY
            selectAlpha = primary.a;
            break;
        case 4: // TEX0
            selectAlpha = tex0.a;
            break;
        case 5: // TEX1
            selectAlpha = tex1.a;
            break;
        case 6: // TEX2
            selectAlpha = tex2.a;
            break;
        case 7: // TEX3
            selectAlpha = tex3.a;
            break;
    }
    switch(opAlpha) {
        case 1: // ALPHA
            postAlpha = selectAlpha;
            break;
        case 3: // ONE_MINUS_ALPHA
            postAlpha = 1.0 - selectAlpha;
            break;
        // 0 and 2 (COLOR and ONE_MINUS_COLOR) are not valid for opAlpha
    }

    return vec4(postRGB, postAlpha);
}

float dot3(vec3 arg0, vec3 arg1) {
    return 4.0 * dot(arg0 - vec3(0.5), arg1 - vec3(0.5));
}

vec4 evaluateUnit(const int unit, const vec4 tex[4], const vec4 prevColor) {
    ivec3 srcRGB = uCombineSrcRGB[unit];
    ivec3 srcAlpha = uCombineSrcAlpha[unit];
    ivec3 opRGB = uCombineOpRGB[unit];
    ivec3 opAlpha = uCombineOpAlpha[unit];

    vec4 arg0 = select(tex[0], tex[1], tex[2], tex[3], tex[unit], prevColor, vPrimaryColor, uCombineColor[unit],
                       srcRGB.x, opRGB.x, srcAlpha.x, opAlpha.x);
    vec4 arg1 = select(tex[0], tex[1], tex[2], tex[3], tex[unit], prevColor, vPrimaryColor, uCombineColor[unit],
                       srcRGB.y, opRGB.y, srcAlpha.y, opAlpha.y);
    vec4 arg2 = select(tex[0], tex[1], tex[2], tex[3], tex[unit], prevColor, vPrimaryColor, uCombineColor[unit],
                       srcRGB.z, opRGB.z, srcAlpha.z, opAlpha.z);

    float alpha = 0.0;
    switch(uCombineFuncAlpha[unit]) {
        case 0: // REPLACE
            alpha = arg0.a;
            break;
        case 1: // MODULATE
            alpha = arg0.a * arg1.a;
            break;
        case 2: // ADD
            alpha = arg0.a + arg1.a;
            break;
        case 3: // ADD_SIGNED
            alpha = arg0.a + arg1.a - 0.5;
            break;
        case 4: // INTERPOLATE
            alpha = arg0.a * arg2.a + arg1.a * (1.0 - arg2.a);
            break;
        case 5: // SUBTRACT
            alpha = arg0.a - arg1.a;
            break;
        // ignore 6 and 7 (DOT3_RGB... since they ignore alpha)
    }

    vec3 rgb = vec3(0.0);
    switch(uCombineFuncRGB[unit]) {
        case 0: // REPLACE
            rgb = arg0.rgb;
            break;
        case 1: // MODULATE
            rgb = arg0.rgb * arg1.rgb;
            break;
        case 2: // ADD
            rgb = arg0.rgb + arg1.rgb;
            break;
        case 3: // ADD_SIGNED
            rgb = arg0.rgb + arg1.rgb - vec3(0.5);
            break;
        case 4: // INTERPOLATE
            rgb = arg0.rgb * arg2.rgb + arg1.rgb * (vec3(1.0) - arg2.rgb);
            break;
        case 5: // SUBTRACT
            rgb = arg0.rgb - arg1.rgb;
            break;
        case 6: // DOT3_RGB
            rgb = vec3(dot3(arg0.rgb, arg1.rgb));
            break;
        case 7: // DOT3_RGBA
            float d = dot3(arg0.rgb, arg1.rgb);
            rgb = vec3(d);
            alpha = d; // override the alpha
            break;
    }

    return vec4(rgb, alpha);
}

void main() {
    // texturing
    // FIXME hella slow
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

    // combine primary and secondary colors
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
    if (uAlphaComparison >= 0) {
        if (!compare(color.a, uAlphaRefValue, uAlphaComparison)) {
            discard;
        }
    }

    fColor = color;
}
