sequenceDiagram

User->>+Gateway: POST /messages/:userId/
Gateway->>+MessageService: POST /messages/:userId/
MessageService->>-Gateway: 201 OK
Gateway->>-User: 201 OK

User->>+Gateway: GET /messages/:userId/
Gateway->>+MessageService: GET /messages/:userId/
MessageService->>-Gateway: 200 OK
Gateway->>-User: 200 OK