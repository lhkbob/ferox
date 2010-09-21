package com.ferox.physics.dynamics;

public class ProjectedGaussSeidelSolver implements ConstraintSolver {
    // FIXME: convert contact pairs into solver constraints
    // then solve them
    //
    // this is done as follows in bullet:
    // 1. go through the manifolds -> solveGroupCacheFriendlySetup + convertContact
    // 2. solve constraints iteratively -> solveGroupCacheFriendlyIterations
    //   + solveSingleIteration + resolveSingleConstraintRowLowerLimit + resolveSingleConstraintRowGeneric
    // 3. add constraint velocities back to the bodies -> solveGroupCacheFriendlyFinish
}
