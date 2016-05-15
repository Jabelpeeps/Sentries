# Sentry
[![Build Status](http://jabelpeeps.org/jenkins/buildStatus/icon?job=21st_Sentry)](http://jabelpeeps.org/jenkins/job/21st_Sentry)

Citizens2 Town Sentry Character

This is my own updating & re-working of the Sentry codebase.

Originally this was with the previous maintainer's (mcmonkey4eva) blessing, although he has now abandoned it to start again from scratch, and is writing another plugin that does much the same stuff.  I guess that makes me the maintainer of Sentry now.

COMPLETED:-
* tidying codebase up for readability.
* updated the code to remove uses of deprecated methods from dependencies. (re-done for 1.9!)
* finish interface (actually an abstract class) for other plugins
* setup my own ci server for this fork.

ONGOING:-
* re-organising the code to improve performance in critical sections.
* move all appropriate code into classes implementing interface for other plugins.
* rework the command handling to improve UX.

TODO:-
* expand readme giving more details of features (old and new)
* remove use of NMS and classes from CraftBukkit as far as possible.
* add more features... 

(pls post any suggestions as issues and/or PR's)
