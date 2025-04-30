package mobi.sevenwinds.app.author

import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass

object AuthorTable : IntIdTable("author") {
    val name = varchar("name", 200)
    val creationDatetime = datetime("creationdatetime")
}

class AuthorEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AuthorEntity>(AuthorTable)

    var name by AuthorTable.name
    var creationDatetime by AuthorTable.creationDatetime

    fun toResponse(): AuthorRecord {
        return AuthorRecord(name, creationDatetime.toString())
    }
}