# Welcome to MiCA.

For more information, please see the MiCA wiki at https://github.com/mica-gossip/MiCA/wiki

# Quick start installation in IntelliJ

The recommended way of using MiCA for Prof. Birman's class is to create your own fork of the MiCA repository, and write your gossip protocols in there.
This quick start shows you how to get that running in IntelliJ.

1. From IntelliJ, choose New -> Project from Version Control (or "Get from Version Control")
2. Github URL: https://github.com/mica-gossip/MiCA.git
3. After IntelliJ has imported the repository, you should see the Project tab on the left. The next step is to mark source and test directories in IntelliJ.
4.   In the project tab, right-click the 'src' directory.  Choose 'Mark Directory as' -> 'Sources Root'
5.   Mark the 'test' directory as 'Test Sources Root'

# Tutorial for the impatient

There's a runnable main method that demonstrates running a gossip protocol simulation here: 
    [org.princehouse.mica.example.RunCompositeProtocol](https://github.com/mica-gossip/MiCA/blob/master/src/main/java/org/princehouse/mica/example/RunCompositeProtocol.java)

MiCA can run its protocols in a local simulation (using the TestHarness class), or it can run local nodes for a distributed system (see the Launcher class).
For development, it is recommended to run your system first in simulation mode without networking (the default TestHarness option), and then with local networking using the *-implementation simple* command line option.

# Support

lonnie.princehouse@gmail.com

# Code Style

The MiCA repository uses the [Java Google Style](https://github.com/google/styleguide/blob/gh-pages/intellij-java-google-style.xml). See 
[Configuring Google Style Guide for Java for IntelliJ](https://medium.com/swlh/configuring-google-style-guide-for-java-for-intellij-c727af4ef248).
