# Sentry
Citizens2 Town Sentry Character

This is my own re-working of the Sentry codebase; starting with tidying it up for readability, I have now updated the code to
remove any uses of deprecated methods from its dependencies, and have now moved on to re-organising the code to improve 
performance in critical sections, and also improve its flexibility (especially with regard to interfacing with other plugins).

TODO's
* finish interface for other plugins
* move all appropriate code into classes implementing this interface.
* rework the command handling to improve UX.
* add more features...