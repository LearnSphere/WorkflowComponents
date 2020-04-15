Carnegie Mellon University, Massachusetts Institute of Technology, Stanford University, University of Memphis.
Copyright 2016. All Rights Reserved.

# LearnSphere and Tigris

[LearnSphere](LearnSphere.org) is co-developed by the [LearnLab](http://learnlab.org) â€“ a flagship project of [Carnegie Mellon](http://cmu.edu)'s [Simon Initiative](https://www.cmu.edu/simon). It is community software infrastructure for sharing, analysis, and collaboration of/around educational data. LearnSphere integrates existing and new educational data infrastructures to offer a world class repository of education data.

[Tigris](https://pslcdatashop.web.cmu.edu/LearnSphereLogin) is a workflow authoring tool which is part of the community software infrastructure being built for the LearnSphere project. The platform provides a way to create custom analyses and interact with new as well as existing data formats and repositories.

# dAFM

This is a Tigris component created using an existing python implementation of the dAFM paper:
Original Sourcecode: https://github.com/CAHLR/dAFM
Derivative work: https://github.com/mkomisin/dAFM-component

Pardos, Z.A., Dadu, A. (2018) dAFM: Fusing Psychometric and Connectionist Modeling for Q-matrix Refinement. Journal of Educational Data Mining. Vol 10(2), 1-27.

This repository contains:
dAFM: dynamic or deep Additive Factors Model
Deep Knowledge Tracing
Additive Factors Model
Skill Model Generation using clustering on distributed representations

# Dependencies
bash
python3
numpy
scipy
scikit-learn
tabulate
requests
featuretools
csvkit
bokeh
pandas
theano
gensim
tensorflow
keras
matlab.engine (for clustering on matlab)

# Dataset:
Dataset should be tab-separated file. Training and test set will be generated on the basis of user-level split (except when using student information). It should have the following attributes:

user_id: unique id of student or user
problem_id: unique id of problem
skill_name: skill associated with the problem
correctness: first attempt correctness of a particular response
section: section information (set of problems belong to a particular section)
unit: contains set of sections

# Limitations
1. dAFM likely to be less accurate as a prediction model than models which take a student’s response history as input (BKT/PFA/DKT)
2. Q-matrix learning or refining is a task likely adjacent to causal modeling; therefore, findings of the model should be validated by other means before using them in production.
3. dAFM will not find new KCs, as currently designed

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






