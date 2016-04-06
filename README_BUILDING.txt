The whole project was initially in eclipse (2012-).  Then I exported the ant build file from eclipse and used Apache Ivy to pull down the dependencies in lib/default (3 April 2016).  These come from the maven2 repository which had the right version of everything except for GRAL which needed to be 0.8 (or at least that was what was working).  So everything except for the two GRAL jars in lib can be generated with the following:

    ant resolve

The GRAL jars are in non-ivy-lib.  Meanwhile, just run the following for usage info.

    ant

Next steps:
- Get working with current GRAL (or properly pull down the older 0.8 version).

Andrew Vardy
4 April 2016
