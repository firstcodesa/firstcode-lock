package sa.firstcode.lock

/** Reject callbacks from a cancelled request, an older screen, or a consumed success. */
class AuthRequests {
    private var next = 0L
    private var active: Long? = null
    private var screen = -1
    fun start(screenVersion: Int): Long {
        screen = screenVersion
        return (++next).also { active = it }
    }
    fun current(ticket: Long, screenVersion: Int) = active == ticket && screen == screenVersion
    fun consume(ticket: Long, screenVersion: Int): Boolean {
        if (!current(ticket, screenVersion)) return false
        active = null
        return true
    }
    fun cancel() { active = null }
}
