/* Initial beliefs and rules */
time(5000).

/* Initial goals */

!waitSem.

/* Plans */

+!start(X) : X > 0 <-
    .wait(1000);
	.print(X-1000);
	!start(X-1000).

+!waitSem <-
    while(time(X) & X > 0) {
        .print(X);
        .wait(1000);
        -+time(X - 1000)
    };
    -+time(5000);
    !waitSem.
