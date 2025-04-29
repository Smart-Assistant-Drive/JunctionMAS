/* Initial beliefs and rules */

/* Initial goals */
!start.

/* Plans */

+!start : true <-
    initSemaphores;
    !temporalBehavior;
    .print("agent ready").

+semaphore(X, C) <-
    .print("Aggiornato semaforo ", X, " con colore ", C).

+!temporalBehavior : semaphore(1, green) &  semaphore(2, red) & semaphore(3, green) & semaphore(4, red) <-
    .wait(5000);
    !yellowBehavior.

+!temporalBehavior : semaphore(1, red) &  semaphore(2, green) & semaphore(3, red) & semaphore(4, green) <-
    .wait(5000);
    !yellowBehavior.

+!yellowBehavior : true <-
    setYellow;
    .wait(5000);
    changeColor;
    !temporalBehavior.

+!changeColor(C): true <-
    .print("changing color in... ", C);
    change(C);
    .print("change color in ", C).
