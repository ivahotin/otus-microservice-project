sequenceDiagram

alt Such email hasn't been used yet
User->>+ProfileService: POST /auth/registration/
ProfileService->>ProfileService: Save profile to profile db
Note right of ProfileService: ProfileCreated[email, encryptedPassword]
ProfileService-->>AuthService:
Note right of ProfileService: ProfileCreated[names, age, city, ...]
ProfileService-->>SearchService:
ProfileService->>-User: 201 Created
else Such email has already been used
User->>+ProfileService: POST/auth/registration/
ProfileService->>ProfileService: Try to save profile in DB but failed due to unique constraint violation
ProfileService->>-User: 409 Conflict
end
