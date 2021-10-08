sequenceDiagram

User->>+Gateway: GET /search?term=...&limit=10
alt Get first page
Gateway->>+SearchService: GET /search?term=...&limit=10
SearchService->>-Gateway: 200 OK
else Get next page
User->>+Gateway: GET /search?term=...&limit=10&cursor=...
Gateway->>+SearchService: GET /search?term...&limit=10&cursor=...
SearchService->>-Gateway: 200 OK
end
Gateway->>-User: 200 OK