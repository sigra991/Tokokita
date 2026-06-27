package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ShopRepository(private val db: AppDatabase) {
    private val productDao = db.productDao()
    private val customerDao = db.customerDao()
    private val transactionDao = db.transactionDao()
    private val debtDao = db.debtDao()

    // --- Products ---
    val allProducts: Flow<List<Product>> = productDao.getAllProductsFlow()

    suspend fun getProductById(id: Int): Product? = withContext(Dispatchers.IO) {
        productDao.getProductById(id)
    }

    suspend fun insertProduct(product: Product): Long = withContext(Dispatchers.IO) {
        productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.updateProduct(product.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteProductById(id: Int) = withContext(Dispatchers.IO) {
        productDao.deleteProductById(id)
    }

    // --- Customers ---
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomersFlow()

    suspend fun getCustomerById(id: Int): Customer? = withContext(Dispatchers.IO) {
        customerDao.getCustomerById(id)
    }

    fun getCustomerByIdFlow(id: Int): Flow<Customer?> = customerDao.getCustomerByIdFlow(id)

    suspend fun insertCustomer(customer: Customer): Long = withContext(Dispatchers.IO) {
        customerDao.insertCustomer(customer)
    }

    suspend fun updateCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        customerDao.updateCustomer(customer)
    }

    suspend fun deleteCustomerById(id: Int) = withContext(Dispatchers.IO) {
        customerDao.deleteCustomerById(id)
    }

    // --- Transactions & Sales ---
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactionsFlow()

    fun getItemsForTransactionFlow(transactionId: Int): Flow<List<TransactionItem>> =
        transactionDao.getItemsForTransactionFlow(transactionId)

    /**
     * Processes a checkout from the Kasir screen.
     * Decrements product stock and records debt if customer chooses "Hutang".
     */
    suspend fun processSale(
        items: List<CartItem>,
        customerName: String?,
        isDebt: Boolean,
        note: String?
    ): Boolean = db.withTransaction {
        try {
            val totalAmount = items.sumOf { it.subtotal }
            
            // 1. Save Transaction Header
            val transactionHeader = Transaction(
                type = "sale",
                customerName = customerName?.takeIf { it.isNotBlank() },
                total = totalAmount,
                note = note?.takeIf { it.isNotBlank() }
            )
            
            val txnId = db.transactionDao().insertTransaction(transactionHeader).toInt()

            // 2. Save Transaction Items
            val dbItems = items.map { cartItem ->
                TransactionItem(
                    transactionId = txnId,
                    productId = cartItem.product.id,
                    productName = cartItem.product.name,
                    priceType = cartItem.priceType,
                    quantity = cartItem.quantity,
                    unitPrice = cartItem.unitPrice,
                    subtotal = cartItem.subtotal
                )
            }
            db.transactionDao().insertTransactionItems(dbItems)

            // 3. Update Product Stocks
            items.forEach { cartItem ->
                // Direct suspend calls are fully safe inside withTransaction block
                val product = db.productDao().getProductById(cartItem.product.id)
                if (product != null) {
                    val stockReduction = if (cartItem.priceType == "retail") {
                        cartItem.quantity
                    } else {
                        cartItem.quantity * product.bundleQty
                    }
                    val newStock = product.stock - stockReduction
                    db.productDao().updateStock(product.id, newStock)
                }
            }

            // 4. Handle Debt if requested
            if (isDebt && !customerName.isNullOrBlank()) {
                val nameTrimmed = customerName.trim()
                
                // Read customers directly
                val idCursor = db.query("SELECT * FROM customers WHERE UPPER(name) = UPPER(?)", arrayOf(nameTrimmed))
                var existingCustomer: Customer? = null
                if (idCursor.moveToFirst()) {
                    val id = idCursor.getInt(idCursor.getColumnIndexOrThrow("id"))
                    val name = idCursor.getString(idCursor.getColumnIndexOrThrow("name"))
                    val phone = idCursor.getString(idCursor.getColumnIndexOrThrow("phone"))
                    val totalDebt = idCursor.getDouble(idCursor.getColumnIndexOrThrow("totalDebt"))
                    existingCustomer = Customer(id, name, phone, totalDebt)
                }
                idCursor.close()

                val finalCustomerId = if (existingCustomer != null) {
                    val updatedCustomer = existingCustomer.copy(
                        totalDebt = existingCustomer.totalDebt + totalAmount
                    )
                    db.customerDao().updateCustomer(updatedCustomer)
                    existingCustomer.id
                } else {
                    val newCustomer = Customer(
                        name = nameTrimmed,
                        totalDebt = totalAmount
                    )
                    db.customerDao().insertCustomer(newCustomer).toInt()
                }

                // Insert into debts history
                val debtHistory = Debt(
                    customerId = finalCustomerId,
                    type = "hutang",
                    amount = totalAmount,
                    description = "Belanja (Nota #${txnId})",
                    transactionId = txnId
                )
                db.debtDao().insertDebt(debtHistory)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    // --- Debts & Customer Account History ---
    /**
     * Records a debt payment for a customer.
     */
    suspend fun recordDebtPayment(
        customerId: Int,
        amount: Double,
        description: String?
    ): Boolean = db.withTransaction {
        try {
            val customer = db.customerDao().getCustomerById(customerId) ?: return@withTransaction false
            
            // 1. Create financial transaction record
            val transactionHeader = Transaction(
                type = "debt_payment",
                customerName = customer.name,
                total = amount,
                note = "Bayar Hutang: ${description ?: "Tanpa catatan"}"
            )
            val txnId = db.transactionDao().insertTransaction(transactionHeader).toInt()

            // 2. Insert Debt item history as "bayar"
            val paymentDebt = Debt(
                customerId = customerId,
                type = "bayar",
                amount = amount,
                description = description ?: "Bayar hutang",
                transactionId = txnId
            )
            db.debtDao().insertDebt(paymentDebt)

            // 3. Update Customer's running total debt balance
            val newDebtBalance = (customer.totalDebt - amount).coerceAtLeast(0.0)
            db.customerDao().updateCustomerDebt(customerId, newDebtBalance)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Manually records an additional debt (e.g. adjust debt) without items.
     */
    suspend fun recordManualDebt(
        customerId: Int,
        amount: Double,
        description: String?
    ): Boolean = db.withTransaction {
        try {
            val customer = db.customerDao().getCustomerById(customerId) ?: return@withTransaction false
            
            val manualDebt = Debt(
                customerId = customerId,
                type = "hutang",
                amount = amount,
                description = description ?: "Penambahan manual"
            )
            db.debtDao().insertDebt(manualDebt)

            val newDebtBalance = customer.totalDebt + amount
            db.customerDao().updateCustomerDebt(customerId, newDebtBalance)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Retrieves debt ledger timeline with calculated running balances.
     */
    fun getCustomerDebtLedgerFlow(customerId: Int): Flow<List<DebtWithBalance>> {
        return debtDao.getDebtsForCustomerFlow(customerId).map { debts ->
            var runningBalance = 0.0
            debts.map { debt ->
                runningBalance = if (debt.type == "hutang") {
                    runningBalance + debt.amount
                } else {
                    runningBalance - debt.amount
                }
                DebtWithBalance(
                    id = debt.id,
                    customerId = debt.customerId,
                    type = debt.type,
                    amount = debt.amount,
                    description = debt.description,
                    transactionId = debt.transactionId,
                    createdAt = debt.createdAt,
                    runningBalance = runningBalance
                )
            }
        }
    }

    // --- Reports ---
    suspend fun getTopSellingProducts(startEpoch: Long, endEpoch: Long, limit: Int = 10): List<TopProduct> =
        withContext(Dispatchers.IO) {
            transactionDao.getTopProducts(startEpoch, endEpoch, limit)
        }

    suspend fun getSalesByCategory(startEpoch: Long, endEpoch: Long): List<CategorySales> =
        withContext(Dispatchers.IO) {
            transactionDao.getCategorySales(startEpoch, endEpoch)
        }

    suspend fun getDailySalesTrends(startEpoch: Long, endEpoch: Long): List<DailySales> =
        withContext(Dispatchers.IO) {
            transactionDao.getDailySales(startEpoch, endEpoch)
        }
}

// Representing cart item logic
data class CartItem(
    val product: Product,
    val priceType: String, // "retail" or "bundle"
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double
)
