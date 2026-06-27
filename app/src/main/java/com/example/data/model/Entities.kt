package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String = "Umum",
    val unit: String = "pcs",
    val bundleUnit: String = "slop",
    val bundleQty: Int = 1,
    val costPrice: Double = 0.0,
    val retailPrice: Double = 0.0,
    val bundlePrice: Double = 0.0,
    val stock: Int = 0, // stock in retail units
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String? = null,
    val totalDebt: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "sale" or "debt_payment"
    val customerName: String? = null,
    val total: Double = 0.0,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transaction_items",
    foreignKeys = [
        ForeignKey(
            entity = Transaction::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("transactionId")]
)
data class TransactionItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val transactionId: Int,
    val productId: Int,
    val productName: String,
    val priceType: String, // "retail" or "bundle"
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double
)

@Entity(
    tableName = "debts",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("customerId")]
)
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val type: String, // "hutang" (increase debt) or "bayar" (decrease debt)
    val amount: Double,
    val description: String? = null,
    val transactionId: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)

// Helper structures for UI presentation
data class TransactionWithItems(
    val id: Int,
    val type: String,
    val customerName: String?,
    val total: Double,
    val note: String?,
    val createdAt: Long,
    val items: List<TransactionItem>
)

data class DebtWithBalance(
    val id: Int,
    val customerId: Int,
    val type: String,
    val amount: Double,
    val description: String?,
    val transactionId: Int?,
    val createdAt: Long,
    val runningBalance: Double
)

data class TopProduct(
    val productId: Int,
    val productName: String,
    val totalQty: Int,
    val totalSales: Double
)

data class CategorySales(
    val category: String,
    val totalSales: Double
)

data class DailySales(
    val dateStr: String, // formatted as "yyyy-MM-dd"
    val totalSales: Double,
    val transactionCount: Int
)
