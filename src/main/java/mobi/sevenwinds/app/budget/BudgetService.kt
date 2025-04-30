package mobi.sevenwinds.app.budget

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mobi.sevenwinds.app.author.AuthorTable
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.transaction

object BudgetService {
    suspend fun addRecord(body: BudgetRecord): BudgetRecord = withContext(Dispatchers.IO) {
        transaction {
            val entity = BudgetEntity.new {
                this.year = body.year
                this.month = body.month
                this.amount = body.amount
                this.type = body.type
                this.author = body.author?.let { EntityID(it, AuthorTable) }
            }

            return@transaction entity.toResponse()
        }
    }

    suspend fun getYearStats(param: BudgetYearParam): BudgetYearStatsResponse = withContext(Dispatchers.IO) {
        transaction {
            val query = (BudgetTable leftJoin AuthorTable)
                .slice(BudgetTable.columns + AuthorTable.name + AuthorTable.creationDatetime)
                .select { BudgetTable.year eq param.year }
                .orderBy(BudgetTable.month to SortOrder.ASC)
                .orderBy(BudgetTable.amount to SortOrder.DESC)
                .limit(param.limit, param.offset)

            if (param.authorName != null) {
                query.andWhere { AuthorTable.name eq param.authorName }
            }

            val total = BudgetTable.select { BudgetTable.year eq param.year }.count()

            val data = query.map { row ->
                val year = row[BudgetTable.year]
                val month = row[BudgetTable.month]
                val amount = row[BudgetTable.amount]
                val type = row[BudgetTable.type]
                val author = row[BudgetTable.author]
                val authorName = row[AuthorTable.name]
                val authorCreationDatetime = row[AuthorTable.creationDatetime]

                BudgetRecord(
                    year = year,
                    month = month,
                    amount = amount,
                    type = type,
                    author = author?.value,
                    authorName = authorName,
                    authorCreationDatetime = authorCreationDatetime?.toString()
                )
            }

            val sumByTypeQuery = BudgetTable
                .slice(BudgetTable.type, BudgetTable.amount.sum())
                .select { BudgetTable.year eq param.year }
                .groupBy(BudgetTable.type)

            val sumByType = sumByTypeQuery.associate {
                it[BudgetTable.type].name to (it[BudgetTable.amount.sum()] ?: 0)
            }

            return@transaction BudgetYearStatsResponse(
                total = total,
                totalByType = sumByType,
                items = data
            )
        }
    }
}