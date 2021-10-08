sequenceDiagram

alt Correct credentials
User->>+Gateway: POST /auth/
Gateway->>+AuthProxy: POST /auth/
AuthProxy->>AuthProxy: Check credentials
AuthProxy->>-Gateway: 200 Authorization: Bearer <TOKEN>
Gateway->>-User: 200 Authorization: Bearer <TOKEN>
else Invalid credentials
User->>+Gateway: POST /auth/
Gateway->>+AuthProxy: POST /auth/
AuthProxy->>AuthProxy: Check invalid credentials
AuthProxy->>-Gateway: 403 Forbidden
Gateway->>-User: 403 Forbidden
end