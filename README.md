# GroupMessenger

Group messenger andriod application: Total and FIFO ordering (with failure handling)

Decentralized Ordering
The application implements ISIS algorithm to order messages and maintain consistency in the order of message delivery on all devices in the group. The algorithm is decentralized, that is there is no one 'master' device in the group that maintains the book-keeping and multicast the delivery order. Each device persist data in SQLite database.

Failiure Safety:
At the core of the algorithm, is the 3-way request-response mechanism. Thus, in case of faliure of one device, there is a need to handle the awaiting "response message" or awaiting "agreement message" from the failed device. This is done using timers stored in a HashMap and sending appropiate instructions to other devices at timeout events.


