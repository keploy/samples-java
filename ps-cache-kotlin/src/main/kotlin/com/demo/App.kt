package com.demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
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

    /**
     * /account?member=N queries the travel_account table.
     *
     * JDBC PS caching (prepareThreshold=1):
     * - 1st call on a fresh connection: Parse(query="SELECT ...") + Bind + Describe + Execute
     * - 2nd+ calls on same connection: Bind(ps="S_1") + Execute only (cached PS)
     *
     * The /evict endpoint forces HikariCP to evict all connections, so the
     * NEXT /account call gets a fresh connection with cold PS cache.
     */
    @GetMapping("/account")
    fun getAccount(@RequestParam("member") memberId: Int): Any {
        return jdbc.execute { conn: java.sql.Connection ->
            conn.autoCommit = false
            try {
                val ps = conn.prepareStatement(
                    """SELECT id, member_id, name, balance
                       FROM travelcard.travel_account
                       WHERE member_id = ?"""
                )
                ps.setInt(1, memberId)
                val rs = ps.executeQuery()

                val result = if (rs.next()) {
                    Account(
                        id = rs.getInt("id"),
                        memberId = rs.getInt("member_id"),
                        name = rs.getString("name"),
                        balance = rs.getInt("balance")
                    )
                } else {
                    mapOf("error" to "not found", "member_id" to memberId)
                }

                rs.close()
                ps.close()
                conn.commit()
                result
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }!!
    }

    /**
     * /evict forces HikariCP to evict all idle connections.
     * Next request gets a FRESH PG connection → cold PS cache.
     * This simulates what happens in production when connections cycle.
     */
    @GetMapping("/evict")
    fun evict(): Map<String, Any> {
        val hikari = dataSource as HikariDataSource
        hikari.hikariPoolMXBean?.softEvictConnections()
        // Also wait briefly for eviction
        Thread.sleep(200)
        return mapOf("evicted" to true, "active" to (hikari.hikariPoolMXBean?.activeConnections ?: 0),
                      "idle" to (hikari.hikariPoolMXBean?.idleConnections ?: 0))
    }
}
