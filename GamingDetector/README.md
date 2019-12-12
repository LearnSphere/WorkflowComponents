Carnegie Mellon University, Massachusetts Institute of Technology, Stanford University, University of Memphis.
Copyright 2016. All Rights Reserved.

# LearnSphere and Tigris

[LearnSphere](LearnSphere.org) is co-developed by the [LearnLab](http://learnlab.org) – a flagship project of [Carnegie Mellon](http://cmu.edu)'s [Simon Initiative](https://www.cmu.edu/simon). It is community software infrastructure for sharing, analysis, and collaboration of/around educational data. LearnSphere integrates existing and new educational data infrastructures to offer a world class repository of education data. 

[Tigris](https://pslcdatashop.web.cmu.edu/LearnSphereLogin) is a workflow authoring tool which is part of the community software infrastructure being built for the LearnSphere project. The platform provides a way to create custom analyses and interact with new as well as existing data formats and repositories.

# Gaming Detector

This is a python implementation of a gaming the system detector on datashop format transaction data. The detector is a direct implementation taken from Paquette et al, 2014. The code implements a feature engineered version of gaming that was developed cognitive task analysis of experts who labeled  student behaviors on an Algebra 1 CogTutor.

## I. Dependencies
1. Python 3.6
2. python virtualenv

## Build instructions
Before executing the component, the local runtime environment must be configured. the install_component.sh script handles the local setup, assuming dependencies have already been installed. This script is automatically called when running 'ant dist' to build the component. It installs a python virtualenv within the componenet directory, install python dependencies specified within requirements.txt, and generates build.properties to point to the python binary within the newly created virtualenv.

To register teh component, the gen_add_component.sh script is provided for convenience to create a new sql entry for the component.

## Testing

You can test the component using 'ant runComponent'. The script, run_component.sh, is simply a wrapper around the options that ant runComponent would normally pass when calling the script. This file if for rapid testing and iteration of input paramters.

The component is configured to analyze a publicly available dataset from Datashop using default parameters.

## Files

program/main.py implments the bulk of the logic that loads the data, annotates it, and returns a label for each transaction for each of 14 patterns as well as the union of all patterns for a generic gaming label.
program/cmd_parser.py implmenters helpers to parse inputs when main.py is called as a script


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






