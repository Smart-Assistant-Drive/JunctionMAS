/* Initial beliefs and rules */

/* Initial goals */
!start.

/* Plans */

+!start : true <-
    initSemaphores;
    !temporalBehavior.

+semaphore(X, C) <-
    .print("Aggiornato semaforo ", X, " con colore ", C).

+!temporalBehavior : semaphore(1, green) &  semaphore(2, red) & semaphore(3, green) & semaphore(4, red) <-
    !send_request;
    !wait_response;
    !yellowBehavior.

+!temporalBehavior : semaphore(1, red) &  semaphore(2, green) & semaphore(3, red) & semaphore(4, green) <-
    !send_request;
    !wait_response;
    !yellowBehavior.

+!temporalBehavior : semaphore(1, red) &  semaphore(2, yellow) & semaphore(3, red) & semaphore(4, yellow) <-
    !send_request;
    !wait_response;
    changeOneSemaphore(red, 2, 4);
    .wait(1000);
    changeOneSemaphore(green, 1, 3);
    !temporalBehavior.

+!temporalBehavior : semaphore(1, yellow) &  semaphore(2, red) & semaphore(3, yellow) & semaphore(4, red) <-
   !send_request;
   !wait_response;
   changeOneSemaphore(red, 1, 3);
   .wait(1000);
   changeOneSemaphore(green, 2, 4);
   !temporalBehavior.

+!yellowBehavior : true <-
    setYellow;
    !temporalBehavior.

+!changeColor(C): true <-
    .print("changing color in... ", C);
    change(C);
    .print("change color in ", C).

+!send_request: other(Receiver) <-
    !sendMessageTo(start, Receiver).

+!wait_response: other(Sender) <-
    .wait({ +restart[source(Sender)] });
    -restart[source(Sender)].

 +!sendMessageTo(Message, Receiver) <-
   .print("Sending ", Message, " to ", Receiver);
   .send(Receiver, tell, Message).