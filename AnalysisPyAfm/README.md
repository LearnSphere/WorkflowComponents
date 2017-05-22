Carnegie Mellon University, Massachusetts Institute of Technology, Stanford University, University of Memphis.
Copyright 2016. All Rights Reserved.

# LearnSphere and Tigris

[LearnSphere](LearnSphere.org) is co-developed by the [LearnLab](http://learnlab.org) – a flagship project of [Carnegie Mellon](http://cmu.edu)'s [Simon Initiative](https://www.cmu.edu/simon). It is community software infrastructure for sharing, analysis, and collaboration of/around educational data. LearnSphere integrates existing and new educational data infrastructures to offer a world class repository of education data. 

[Tigris](https://pslcdatashop.web.cmu.edu/LearnSphereLogin) is a workflow authoring tool which is part of the community software infrastructure being built for the LearnSphere project. The platform provides a way to create custom analyses and interact with new as well as existing data formats and repositories.

# AFM and AFM+S in Python

This is a python implementation of the Additive Factors Model (Cenn, 2009) and
The Additive Factors Model with Slipping Paramters (MacLellan et al., 2015).
Wherever possible I tried to maintain scikit-learn convention, so that the code
would be compatible with their helper functions (e.g., for cross validation).

custom\_logistic.py is an estimator that can be used to implement AFM.
bounded\_logistic.py is an estimator that can be used to implement AFM+S.

process\_datashop.py can be used to read a student step or transaction export
from [DataShop](https://pslcdatashop.web.cmu.edu/) and to run AFM and AFM+S on
it. Alternatively, the data can be passed to the scripts as long as it adheres
to the [tab-delimited DataShop
format](https://pslcdatashop.web.cmu.edu/help?page=importFormatTd).This script
can be called with the -h argument to get details on supported arguments. By
default the script runs AFM+S when passed a student step export from Datashop;
e.g., `$ python process_datashop.py student_step_datashop_export.txt`

plot\_datashop.py can be used to plot student learning curves with the learning
curves predicted by AFM and AFM+S. Similar to process\_datashop.py, it accepts
student step files in datashop format; e.g.,
`$ python plot_datashop.py student_step_datashop_export.txt`

For a slightly more detailed example of how to use these scripts see my
[blog post](http://christopia.net/blog/modeling-student-learning-in-python).

# Citing this Software

If you use this software in a scientific publiction, then we would appreciate
citation of the following paper:

MacLellan, C.J., Liu, R., Koedinger, K.R. (2015) Accounting for Slipping and
Other False Negatives in Logistic Models of Student Learning. In O.C. Santos et
al. (Eds.), Proceedings of the 8th International Conference on Educational Data
Mining. Madrid, Spain: International Educational Data Mining Society [(pdf)](http://christopia.net/media/publications/maclellan2-2015.pdf).

Bibtex entry:

    @inproceedings{afmslip:2015,
    author={MacLellan, C.J. and Liu, R. and Koedinger, K.R.},
    title={Accounting for Slipping and Other False Negatives in Logistic Models
    of Student Learning.},
    booktitle={Proceedings of the 8th International Conference on Educational
    Data Mining},
    editor={Santos, O.C. and Boticario, J.G. and Romero, C. and Pechenizkiy, M.
    and Merceron, A. and Mitros, P. and Luna, J.M. and Mihaescu, C. and Moreno,
    P. and Hershkovitz, A. and Ventura S. and Desmarais, M.},
    year={2015},
    publisher={Interational Educational Data Mining Society},
    address={Madrid, Spain}
    }

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






