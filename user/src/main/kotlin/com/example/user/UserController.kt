package com.example.user

import com.example.user.dto.RegisterDto
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(
    private val userRepository: UserRepository,
    private val sessionStorage: SessionStorage
) {

    private val bCryptPasswordEncoder = BCryptPasswordEncoder(10)

    @PostMapping("/auth/register")
    fun register(@RequestBody request: RegisterDto): ResponseEntity<*> {
        val encryptedPassword = bCryptPasswordEncoder.encode(request.password)
        val user = User(id = null, username = request.username, password = encryptedPassword)
        val userId = userRepository.create(user)
        return if (userId == null) {
            ResponseEntity
                .status(HttpStatus.CONFLICT)
                .build<Any>()
        } else {
            ResponseEntity
                .status(HttpStatus.CREATED)
                .build<Any>()
        }
    }

    @PostMapping("/auth/login")
    fun login(@RequestBody request: RegisterDto, response: HttpServletResponse): ResponseEntity<*> {
        val username = request.username
        val user = userRepository.getUserByUsername(username)
        if (user == null || !bCryptPasswordEncoder.matches(request.password, user.password)) {
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .build<Any>()
        }

        val sessionId = sessionStorage.createSession(user.id!!)
        val cookie = Cookie("session_id", sessionId)
        cookie.isHttpOnly = true
        cookie.secure = false
        response.addCookie(cookie)
        return ResponseEntity
            .status(HttpStatus.OK)
            .build<Any>()
    }

    @PostMapping("/auth/logout")
    fun logout(@CookieValue("session_id") sessionId: String): ResponseEntity<*> {
        sessionStorage.deleteSession(sessionId)
        return ResponseEntity.status(HttpStatus.OK).build<Any>()
    }

    @GetMapping("/auth")
    fun auth(@CookieValue("session_id") sessionId: String): ResponseEntity<*> {
        val userId = sessionStorage.getUserIdBySessionId(sessionId) ?:
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Any>()

        return ResponseEntity
            .status(HttpStatus.OK)
            .header("x-user-id", userId)
            .build<Any>()
    }
}