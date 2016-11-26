# Sentries
[![Build Status](http://jabelpeeps.org/jenkins/buildStatus/icon?job=Sentries)](http://jabelpeeps.org/jenkins/job/Sentries)

Citizens2 Sentry, Bodyguard, and Fighting Trait.

This is my own near-total re-write of the Sentry codebase, with a focus on updating & re-working it to improve performance, while maintaining it's current features.

This re-write would not have been possible without the original codebase for me to start from, and so credit and thanks are due to aufdemrand, jrbudda and mcmonkey4eva the previous maintainers of this project.

You will need to be running your server on Java 8 to run this version. 

COMPLETED:-
* tidying codebase up for readability.
* updated the code to remove uses of deprecated methods from dependencies. (re-done for 1.9 & 1.10!)
* define and implement interface for integration with other plugins
* move all appropriate code into classes implementing interface for other plugins.
* setup my own [ci server](https://jabelpeeps.org/jenkins/job/Sentries/) for this fork. 
* removing use of NMS and direct import of classes from CraftBukkit.
* reworking the command handling to improve UX.
* implement new target handling/selection system.

ONGOING:-
* re-organising the code to improve performance in critical sections.
* fixing bugs

TODO:-
* support spigot 1.11
* expand readme giving more details of features (old and new)
* make better use of the second (off)hand.
* (maybe) have sentries work together better (in teams)
* improve functioning of mounted sentries (i.e. better integration between the two npc's)
* add integration with a wider range of plugins 
* add more features... 

(pls post any suggestions as issues and/or PR's)
