package com.ferox.shader;

/**
 * <p>
 * Comparison represents the various logical comparisons possible, generally with
 * pixel data, such as depth or alpha values. Each enum value corresponds to one
 * of the mathematical operations: ==, >, <, >=, <=, !=; or in the case of NEVER
 * and ALWAYS, constant boolean values: false, true. Often the comparison is set
 * up as:
 * <ol>
 * <li>Pv = A value from the current pixel or fragment being tested, e.g. that
 * pixel's eye depth</li>
 * <li>Cv = A comparison value, this might be a constant (such as with alpha
 * tests), or it could be based on the current value in the pixel (e.g. the
 * previous depth value)</li>
 * <li>OP = The Comparison configured for the test being conducted</li>
 * </ol>
 * The test will pass if (Pv OP Cv) is true. If it's true, the pixel will
 * continue to be processed; otherwise it will be rejected and will not be
 * rendered.
 * </p>
 * <p>
 * The above is the common usage of the Comparison. The enum values are generic
 * enough that they can be used in any situation where these math operations
 * must be described.
 * </p>
 * 
 * @author Michael Ludwig
 */
public enum Comparison {
	EQUAL, GREATER, LESS, GEQUAL, LEQUAL, NOT_EQUAL, NEVER, ALWAYS
}