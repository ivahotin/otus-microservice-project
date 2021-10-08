sequenceDiagram

alt Follow another user
User->>+Gateway: POST /subscriptions/:id/
Gateway->>+SubscriptionService: POST /subscriptions/:id/
SubscriptionService->>-Gateway: 201 Created
Gateway->>-User: 201 Created
Note right of SubscriptionService: Follow[fromUserId, toUserId]
SubscriptionService->>AnalyticalService:
else Unfollow another user
User->>+Gateway: DELETE /subscriptions/:id/
Gateway->>+SubscriptionService: DELETE /subscriptions/:id/
SubscriptionService->>-Gateway: 204 No content
Gateway->>-User: 204 No content
Note right of SubscriptionService: Unfollow[fromUserId, toUserId]
SubscriptionService->>AnalyticalService:
end