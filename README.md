The PuckSwarm Simulation Package
---------------------------------
This package provides a simulator for a swarm of modified SRV-1 robots
operating on coloured pucks.  Through a separate package (SrvLink) it is
possible to use the same code to control the robots directly.  The best
description for the robots and some of the algorithms can be found in the
paper available below:

    http://www.cs.mun.ca/~av/supp/si12/

Running the Simulator
----------------------
Run the simulator in GUI mode (for debugging and visualization) by running
arena.RunTestbed.  Run without the GUI by executing arena.RunOffline. You
can use ant to execute these actions:

    ant RunTestbed
    ant RunOffline

Actually, just run the following for available options:

    ant

This requires both ant and ivy to be installed.

Controllers
------------

The controllers are located in the 'controllers' package.  The default
controller to run is set in controllers.ControllerUtils.  To modify this
change the following line

        String controllerType = ExperimentManager.getCurrent().getProperty(
                "controllerType", "CacheCons", null);

For example, to run BHD replace "CacheCons" with "BHD".

To modify various other parameters of the system (number of pucks, puck types,
robots, etc...) modify the appropriate lines in arena.Arena.  For example, to
increase the number of robots, go to the following line:

            int nRobots = ExperimentManager.getCurrent().getProperty(
                    "Arena.nRobots", 2, null);

and change 2 to whatever you wish (although you will get significant impact
on performance with a larger number of robots; mostly I used 4 robots but tested
up to 16).

Andrew Vardy
www.cs.mun.ca/~av
av@mun.ca
