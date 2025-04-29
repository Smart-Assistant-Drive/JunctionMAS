/* Initial beliefs and rules */

/* Initial goals */

!start.

/* Plans */

+!start : true <-
    .wait(5000);
	.print("hello world").
