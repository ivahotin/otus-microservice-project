sequenceDiagram

alt Get my page
User->>+Gateway: GET /me/
par Fetch profile
Gateway->>+ProfileService: GET /profiles/me/
ProfileService->>-Gateway: 200 OK
and Fetch subscriptions
Gateway->>+SubscriptionService: GET /subscriptions/me/
SubscriptionService->>-Gateway: 200 OK
end
else Get another user's page
Gateway->>+ProfileService: GET /profiles/:id/
ProfileService->>-Gateway: 200 OK
else Edit my page
User->>+Gateway: PUT /me/
Gateway->>+ProfileService: GET /profiles/me/
ProfileService->>-Gateway: 200 OK
Note right of ProfileService: ProfileUpdated
ProfileService-->>SearchService:
end
Gateway->>-User: 200 OK
