Carnegie Mellon University, Massachusetts Institute of Technology, Stanford University, University of Memphis.
Copyright 2016. All Rights Reserved.


# Tigris's Bayesian Knowledge Tracing (BKT) Workflow Component

Tigris is an open analytic method library and workflow-authoring environment for researchers to build models and run them across datasets. Bayesian Knowledge Tracing workflow component is one of the Tigris's student modeling components.

# Bayesian Knowledge Tracing Overview

Bayesian Knowledge Training (BKT) is a user modeling approach widely use in the area of Intelligent Tutoring Systems (ITS). In ITS, it is customary to tag problems and problem steps students are working on with knowledge quanta (also called skills). When a student is attempting to solve a problem or a problem step, they are said to _practice a skill (skills)_. A student model (BKT being one) performs deductions on whether the skill(s) is (are) mastered given a sequence of correct and incorrect attempts to solve problems (problem steps). BKT uses a formalism of a Hidden Markov Model to make the estimation of student's skill mastery. In BKT, there are two types of nodes: binary state nodes capture skill mastery (one per skill) that assume values of _mastered_ or _not mastered_ and binary observation nodes (one per skill) that assume values of _correct_ or _incorrect_. 

Each skill in standard BKT has the following parameters.

1. _p-init_ or p(Lo) - is a probability the skill was known a priori,
2. _p-learn_ or p(T) - is a probability the skill will transition into _mastered_ state after a practice attempt,
3. _p-forget_ or p(F) - is a probability that the skill will transition into _not mastered state_ after a practice attempt. Traditionally, _p-forget_ is set to zero and is not counted towards the total number of parameters.
4. _p-slip_ or p(S) - is a probability that a mastered skill is applied incorrectly, and
5. _p-guess_ or p(G) - is a probability that unmastered skill will be applied correctly.


For more details on BKT refer to the original paper on it [1]. For the documentation on the core utility that performs the fitting of the BKT model refer to [2]. For implementation details and theoretical issues of fitting hidden Markov models, refer to [3].

# References

1. Corbett, A. T., and Anderson, J. R.: Knowledge tracing: Modeling the acquisition of procedural knowledge. User Modeling and User-Adapted Interaction, 4(4), 253-278. (1995)

2. Yudelson, M. HMM-scalable – A Tool for fitting Hidden Markov Models models at scale. [http://yudelson.info/hmm-scalable](http://yudelson.info/hmm-scalable)

3. Levinson, S. E., Rabiner, L. R., and Sondhi, M. M.: An Introduction to the Application of the Theory of Probabilistic Functions of a Markov Process to Automatic Speech Recognition. Bell System Technical Journal, 62(4): 1035-1074. (1983)


# Appendix A. Technical Details 
## I. Dependencies

1. Ant 1.9 or greater
2. Java Enterprise Edition Software Development Kit (J2EE SDK)
2. Eclipse or Cygwin.
3. Clone the GitHub repository using `git clone https://github.com/PSLCDataShop/WorkflowComponents WorkflowComponents` command.
4. BKT contains executables which may need to be rebuilt for your system.
	- From the command-line in `WorkflowComponents/AnalysisBkt/program/standard-bkt-public-standard-bkt` folder issue the `make`command.
	- Then, copy the predicthmm.exe and `trainhmm.exe` to the `AnalysisBkt/program` directory.


## II. Documentation

See `WorkflowComponents/Workflow Components.docx` for detailed information on creating, modifying, or running components.

## III. Testing a workflow component in Eclipse, Cygwin, or Linux

### A. Eclipse
	
1. File -> Import -> General -> Existing Projects into Workspace.
2. Choose any component directory from your newly imported git clone, i.e. `WorkflowComponents/<AnyComponent>`.
3. Click 'Finish'.
4. In the Ant view (Windows -> Show View -> Ant), add the desired component's build.xml to your current buildfiles, e.g. `<AnyComponent>/build.xml`.
5. Double click the ant task `runToolTemplate`. The component should produce example XML output if it is setup correctly.

**NB**: For debugging, you may wish to add the jars in the directory `WorkflowComponents/CommonLibraries` to your build path.


### B. Cygwin or Linux

1. Change to your `WorkflowComponents` directory, e.g. `/cygdrive/c/your_workspace/<AnyComponent>/`
2. Issue the command `ant -p` to get a list of ant tasks
3. Issue `ant runComponent` to run the component with the included example data


## IV. Building components

### Build all components

Modify the `dir` variable in `WorkflowComponents/build.sh` to match your `WorkflowComponents` path, then run the script (requires bash)

### Building a single component

Issue the `ant dist` command.






