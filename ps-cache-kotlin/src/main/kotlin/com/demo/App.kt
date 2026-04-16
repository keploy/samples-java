package com.demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.http.ResponseEntity
import javax.sql.DataSource
import com.zaxxer.hikari.HikariDataSource

@SpringBootApplication
class App

fun main(args: Array<String>) {
    runApplication<App>(*args)
}

data class Account(
    val id: Int,
    val memberId: Int,
    val name: String,
    val balance: Int
)

@RestController
class AccountController(private val jdbc: JdbcTemplate, private val dataSource: DataSource) {

    @GetMapping("/health")
    fun health() = mapOf("status" to "ok")

    @GetMapping("/account")
    fun getAccount(@RequestParam("member") memberId: Int): ResponseEntity<Any> {
        val result = jdbc.execute(
            org.springframework.jdbc.core.ConnectionCallback<Account?> { conn ->
                conn.autoCommit = false
                try {
                    conn.prepareStatement(
                        """SELECT id, member_id, name, balance
                           FROM travelcard.travel_account
                           WHERE member_id = ?"""
                    ).use { ps ->
                        ps.setInt(1, memberId)
                        ps.executeQuery().use { rs ->
                            val account = if (rs.next()) {
                                Account(
                                    id = rs.getInt("id"),
                                    memberId = rs.getInt("member_id"),
                                    name = rs.getString("name"),
                                    balance = rs.getInt("balance")
                                )
                            } else null

                            conn.commit()
                            account
                        }
                    }
                } catch (e: Exception) {
                    conn.rollback()
                    throw e
                }
            })

        return if (result != null) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.status(404).body(mapOf("error" to "not found", "member_id" to memberId))
        }
    }

    @GetMapping("/evict")
    fun evict(): ResponseEntity<Map<String, Any>> {
        val hikari = dataSource as? HikariDataSource
            ?: return ResponseEntity.status(500).body(mapOf("error" to "not a HikariDataSource"))

        val mxBean = hikari.hikariPoolMXBean
            ?: return ResponseEntity.status(500).body(mapOf("error" to "pool MXBean not available"))

        mxBean.softEvictConnections()
        Thread.sleep(500)

        return ResponseEntity.ok(mapOf(
            "evicted" to true,
            "active" to mxBean.activeConnections,
            "idle" to mxBean.idleConnections
        ))
    }
}
