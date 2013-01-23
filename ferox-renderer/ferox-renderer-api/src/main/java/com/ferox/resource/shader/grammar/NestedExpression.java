package com.ferox.resource.shader.grammar;

// FIXME if we can remove this expression type, we can really consolidate the
// expression hierarchy, since the nesting effectively allows any expression
// to be used anywhere. The only down side is that the produced syntax requires
// the parentheses to be in the proper places, and I could err on the side of
// putting parentheses around everything but that gets really gross.
//
// If I don't just always do parens, then parentheses will need to be computed
// to preserve the proper parsing given the precedence of the operators and
// expression types.
//
// Perhaps (I need to be more awake to know for sure) a solution is:
//   make expressions comparable to each other
//   each expression also has a left or right associativity
//   then depending on its associativity and how it compares to any 
//    child/referenced expressions it wraps the expression in parens
//    (effectively downgrading the ordering of the child to nested expression)
//  I think this works pretty well for left-associative, but I don't know 
//   for right associative ones (like assignment and conditional expression)

// So for left-associativity and precedence, we have the following example:
// x + y * z, which creates the tree: 
//   +
// x   *
//    y z
//
// but (x + y) * z is:
//     *
//   +   z
//  x y
//
// so in the first case, the addition expression does not need to emit parens around
//  the right expression because multiplication is higher precedence than addition
// in the first case, the multiply expression does need to emit parens around the
//  left expression because addition has lower precedence than multiplication
//
// if precedence(left) >= precedence(op) then no parens on left
// if precedence(op) > precedence(right) then no parens on right
//
// for right associative, we reverse the comparisons
//
//
// Now what do we do about the problems where some places expect a 'consant-epxression',
// which has a higher precedence than the assignment expression. That means that
// any assignment expression must be wrapped in parentheses, but we'd have to bake
// this logic into the types that require constant expressions instead of 
// full expressions
public class NestedExpression implements PrimaryExpression {
    private final Expression expression;

    public NestedExpression(Expression expression) {
        this.expression = expression;
    }
}
