sequenceDiagram

User->>+Gateway: POST /posts/
Gateway->>+PostService: POST /posts/
PostService->>-Gateway: 200 OK
Gateway->>-User: 200 OK
Note right of PostService: PostCreated[userId, content]
PostService->>NewsfeedService:
NewsfeedService->>+SubscriptionService: GET /subscriptions/to/:id/
SubscriptionService->>-NewsfeedService: 200 OK
NewsfeedService->>NewsfeedService: Update newsfeeds for followers

User->>+Gateway: GET /posts/me/
Gateway->>+NewsfeedService: GET /posts/me/
NewsfeedService->>-Gateway: 200 OK
Gateway->>-User: 200 OK