Carnegie Mellon University, Massachusetts Institute of Technology, Stanford University, University of Memphis.
Copyright 2016. All Rights Reserved.


I. Dependencies

1. Ant 1.9 or greater
2. Java Enterprise Edition Software Development Kit (J2EE SDK)
2. Eclipse or Cygwin
3. Git

    git clone https://github.com/PSLCDataShop/WorkflowComponents WorkflowComponents


II. Documentation

  See WorkflowComponents/Workflow Components.docx for detailed information on creating, modifying, or running components.



III. Testing a workflow component in Eclipse, Cygwin, or Linux

A. Eclipse

  1. File -> Import -> General -> Existing Projects into Workspace
  2. Choose any component directory from your newly imported git clone, i.e. WorkflowComponents/<AnyComponent>
  3. Click 'Finish'
  4. In the Ant view (Windows -> Show View -> Ant), add the desired component's build.xml to your current buildfiles, e.g. <AnyComponent>/build.xml
  5. Double click the ant task "runToolTemplate". The component should produce example XML output if it is setup correctly.

* For debugging, you may wish to add the jars in the directory WorkflowComponents/CommonLibraries to your build path.


B. Cygwin or Linux

  1. Change to your WorkflowComponents directory, e.g. /cygdrive/c/your_workspace/<AnyComponent>/
  2. Type the command "ant -p" to get a list of ant tasks
  3. Type "ant runComponent" to run the component with the included example data



IV. Building components

Build all components

  Modify the "dir" variable in WorkflowComponents/build.sh to match your WorkflowComponents path, then run the script (requires bash)

Building a single component

  ant dist






