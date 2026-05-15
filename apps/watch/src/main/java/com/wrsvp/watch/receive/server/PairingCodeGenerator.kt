package com.wrsvp.watch.receive.server

import java.security.SecureRandom
import javax.inject.Inject

class PairingCodeGenerator @Inject constructor() {
    private val random = SecureRandom()

    fun generate(): String = (random.nextInt(900_000) + 100_000).toString()
}
