Carnegie Mellon University, Massachusetts Institute of Technology, Stanford University, University of Memphis.
Copyright 2016. All Rights Reserved.

# LearnSphere and Tigris

[LearnSphere](LearnSphere.org) is co-developed by the [LearnLab](http://learnlab.org) – a flagship project of [Carnegie Mellon](http://cmu.edu)'s [Simon Initiative](https://www.cmu.edu/simon). It is community software infrastructure for sharing, analysis, and collaboration of/around educational data. LearnSphere integrates existing and new educational data infrastructures to offer a world class repository of education data. 

[Tigris](https://pslcdatashop.web.cmu.edu/LearnSphereLogin) is a workflow authoring tool which is part of the community software infrastructure being built for the LearnSphere project. The platform provides a way to create custom analyses and interact with new as well as existing data formats and repositories.

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

## V. Instructions for setting up component Featuretools-MOOCdb:

### Python version needed to run this component:

2.7

### Python packages needed to run this component:

FeatureTools.  Refer to install_dependencies.py for installation of python packages

### Other changes needed to run this component:

In the top directory of this component, make file build.properties based on your machine's configuration. Use build.properties.sample as example.

Place your .tsv files in the program/input directory of this component.