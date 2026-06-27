package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction as RoomTransaction
import androidx.room.Update
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProductsFlow(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Int): Product?

    @Query("SELECT * FROM products WHERE id = :id")
    fun getProductByIdFlow(id: Int): Flow<Product?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: Int)

    @Query("UPDATE products SET stock = :newStock WHERE id = :id")
    suspend fun updateStock(id: Int, newStock: Int)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomersFlow(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Int): Customer?

    @Query("SELECT * FROM customers WHERE id = :id")
    fun getCustomerByIdFlow(id: Int): Flow<Customer?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("UPDATE customers SET totalDebt = :debt WHERE id = :id")
    suspend fun updateCustomerDebt(id: Int, debt: Double)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerById(id: Int)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun getAllTransactionsFlow(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Int): Transaction?

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun getItemsForTransaction(transactionId: Int): List<TransactionItem>

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    fun getItemsForTransactionFlow(transactionId: Int): Flow<List<TransactionItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionItems(items: List<TransactionItem>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Int)

    // Reporting and Analytis queries
    @Query("""
        SELECT ti.productId as productId, ti.productName as productName, SUM(ti.quantity) as totalQty, SUM(ti.subtotal) as totalSales
        FROM transaction_items ti
        INNER JOIN transactions t ON ti.transactionId = t.id
        WHERE t.type = 'sale' AND t.createdAt BETWEEN :startEpoch AND :endEpoch
        GROUP BY ti.productId, ti.productName
        ORDER BY totalSales DESC
        LIMIT :limit
    """)
    suspend fun getTopProducts(startEpoch: Long, endEpoch: Long, limit: Int = 10): List<TopProduct>

    @Query("""
        SELECT p.category as category, SUM(ti.subtotal) as totalSales
        FROM transaction_items ti
        INNER JOIN products p ON ti.productId = p.id
        INNER JOIN transactions t ON ti.transactionId = t.id
        WHERE t.type = 'sale' AND t.createdAt BETWEEN :startEpoch AND :endEpoch
        GROUP BY p.category
    """)
    suspend fun getCategorySales(startEpoch: Long, endEpoch: Long): List<CategorySales>

    @Query("""
        SELECT strftime('%Y-%m-%d', datetime(createdAt / 1000, 'unixepoch', 'localtime')) as dateStr, 
               SUM(total) as totalSales, 
               COUNT(id) as transactionCount
        FROM transactions
        WHERE type = 'sale' AND createdAt BETWEEN :startEpoch AND :endEpoch
        GROUP BY dateStr
        ORDER BY dateStr ASC
    """)
    suspend fun getDailySales(startEpoch: Long, endEpoch: Long): List<DailySales>
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts WHERE customerId = :customerId ORDER BY createdAt ASC")
    fun getDebtsForCustomerFlow(customerId: Int): Flow<List<Debt>>

    @Query("SELECT * FROM debts WHERE customerId = :customerId ORDER BY createdAt ASC")
    suspend fun getDebtsForCustomer(customerId: Int): List<Debt>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: Debt): Long

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteDebtById(id: Int)
}
