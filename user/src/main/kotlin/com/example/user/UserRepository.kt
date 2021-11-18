package com.example.user

import java.util.UUID
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository

private const val createUserStmt = "insert into users (username, password) values (?, ?)"
private const val getUserByUsernameStmt = "select id, username, password from users where username = ?"

@Repository
class UserRepository(private val jdbcTemplate: JdbcTemplate) {

    fun create(user: User): UUID? {
        val keyHolder = GeneratedKeyHolder()
        try {
            jdbcTemplate.update(
                { conn ->
                    val ps = conn.prepareStatement(createUserStmt, arrayOf("id"))
                    ps.setString(1, user.username)
                    ps.setString(2, user.password)
                    ps
                },
                keyHolder
            )
        } catch (exc: DuplicateKeyException) {
            return null
        }

        return keyHolder.getKeyAs(UUID::class.java)
    }

    fun getUserByUsername(username: String): User? {
        return try {
            jdbcTemplate
                .queryForObject(
                    getUserByUsernameStmt,
                    { rs, _ ->
                        User(
                            id = UUID.fromString(rs.getString("id")),
                            username = rs.getString("username"),
                            password = rs.getString("password")
                        )
                    },
                    username
                )
        } catch (exc: EmptyResultDataAccessException) {
            null
        }
    }
}