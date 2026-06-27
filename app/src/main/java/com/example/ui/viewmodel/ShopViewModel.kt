package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.*
import com.example.data.repository.ShopRepository
import com.example.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShopViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ShopRepository
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = ShopRepository(database)
    }

    // --- State Observables ---
    val products: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Cart Management (Kasir) ---
    private val _cart = MutableStateFlow<List<com.example.data.repository.CartItem>>(emptyList())
    val cart: StateFlow<List<com.example.data.repository.CartItem>> = _cart.asStateFlow()

    fun addToCart(product: Product, priceType: String, quantity: Int) {
        val list = _cart.value.toMutableList()
        val price = if (priceType == "retail") product.retailPrice else product.bundlePrice
        
        // Check if item exists in cart with same priceType
        val idx = list.indexOfFirst { it.product.id == product.id && it.priceType == priceType }
        if (idx >= 0) {
            val existing = list[idx]
            val newQty = existing.quantity + quantity
            list[idx] = existing.copy(
                quantity = newQty,
                subtotal = newQty * price
            )
        } else {
            list.add(
                com.example.data.repository.CartItem(
                    product = product,
                    priceType = priceType,
                    quantity = quantity,
                    unitPrice = price,
                    subtotal = quantity * price
                )
            )
        }
        _cart.value = list
    }

    fun updateCartQuantity(productId: Int, priceType: String, newQty: Int) {
        if (newQty <= 0) {
            removeFromCart(productId, priceType)
            return
        }
        val list = _cart.value.toMutableList()
        val idx = list.indexOfFirst { it.product.id == productId && it.priceType == priceType }
        if (idx >= 0) {
            val existing = list[idx]
            list[idx] = existing.copy(
                quantity = newQty,
                subtotal = newQty * existing.unitPrice
            )
            _cart.value = list
        }
    }

    fun removeFromCart(productId: Int, priceType: String) {
        val list = _cart.value.toMutableList()
        list.removeAll { it.product.id == productId && it.priceType == priceType }
        _cart.value = list
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    // --- Transaction Processing ---
    private val _transactionSuccess = MutableStateFlow<Boolean?>(null)
    val transactionSuccess = _transactionSuccess.asStateFlow()

    fun resetTransactionStatus() {
        _transactionSuccess.value = null
    }

    fun checkout(customerName: String?, isDebt: Boolean, note: String?) {
        viewModelScope.launch {
            if (_cart.value.isEmpty()) return@launch
            val success = repository.processSale(
                items = _cart.value,
                customerName = customerName,
                isDebt = isDebt,
                note = note
            )
            if (success) {
                clearCart()
                _transactionSuccess.value = true
            } else {
                _transactionSuccess.value = false
            }
        }
    }

    // --- Product Operations ---
    fun saveProduct(
        id: Int = 0,
        name: String,
        category: String,
        unit: String,
        bundleUnit: String,
        bundleQty: Int,
        costPrice: Double,
        retailPrice: Double,
        bundlePrice: Double,
        stock: Int,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (id == 0) {
                    val newProduct = Product(
                        name = name,
                        category = category,
                        unit = unit,
                        bundleUnit = bundleUnit,
                        bundleQty = bundleQty,
                        costPrice = costPrice,
                        retailPrice = retailPrice,
                        bundlePrice = bundlePrice,
                        stock = stock
                    )
                    repository.insertProduct(newProduct)
                } else {
                    val existing = repository.getProductById(id)
                    if (existing != null) {
                        val updatedProduct = existing.copy(
                            name = name,
                            category = category,
                            unit = unit,
                            bundleUnit = bundleUnit,
                            bundleQty = bundleQty,
                            costPrice = costPrice,
                            retailPrice = retailPrice,
                            bundlePrice = bundlePrice,
                            stock = stock
                        )
                        repository.updateProduct(updatedProduct)
                    }
                }
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun deleteProduct(id: Int, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteProductById(id)
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    // --- Customer Detail & Debt Ledger ---
    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer = _selectedCustomer.asStateFlow()

    private val _selectedCustomerLedger = MutableStateFlow<List<DebtWithBalance>>(emptyList())
    val selectedCustomerLedger = _selectedCustomerLedger.asStateFlow()

    fun selectCustomer(customerId: Int) {
        viewModelScope.launch {
            val customer = repository.getCustomerById(customerId)
            _selectedCustomer.value = customer
            if (customer != null) {
                // Collect debt ledger values safely
                repository.getCustomerDebtLedgerFlow(customerId).collect { list ->
                    _selectedCustomerLedger.value = list
                }
            }
        }
    }

    fun addManualDebt(customerId: Int, amount: Double, description: String?, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.recordManualDebt(customerId, amount, description)
            if (success) {
                // Refresh customer detail
                selectCustomer(customerId)
            }
            onComplete(success)
        }
    }

    fun payDebt(customerId: Int, amount: Double, description: String?, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.recordDebtPayment(customerId, amount, description)
            if (success) {
                // Refresh customer detail
                selectCustomer(customerId)
            }
            onComplete(success)
        }
    }

    fun addCustomer(name: String, phone: String?, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val customer = Customer(name = name, phone = phone)
                repository.insertCustomer(customer)
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    // --- Analytics & Reports ---
    private val _reportRange = MutableStateFlow("today")
    val reportRange = _reportRange.asStateFlow()

    private val _reportStartEpoch = MutableStateFlow(DateUtils.getStartOfDayEpoch(0))
    val reportStartEpoch = _reportStartEpoch.asStateFlow()

    private val _reportEndEpoch = MutableStateFlow(DateUtils.getEndOfDayEpoch())
    val reportEndEpoch = _reportEndEpoch.asStateFlow()

    private val _topProducts = MutableStateFlow<List<TopProduct>>(emptyList())
    val topProducts = _topProducts.asStateFlow()

    private val _categorySales = MutableStateFlow<List<CategorySales>>(emptyList())
    val categorySales = _categorySales.asStateFlow()

    private val _dailySalesTrends = MutableStateFlow<List<DailySales>>(emptyList())
    val dailySalesTrends = _dailySalesTrends.asStateFlow()

    fun setReportRange(range: String) {
        _reportRange.value = range
        val (start, end) = DateUtils.getDateRange(range)
        _reportStartEpoch.value = start
        _reportEndEpoch.value = end
        loadReports()
    }

    fun setCustomReportRange(start: Long, end: Long) {
        _reportRange.value = "custom"
        _reportStartEpoch.value = start
        _reportEndEpoch.value = end
        loadReports()
    }

    fun loadReports() {
        viewModelScope.launch {
            val start = _reportStartEpoch.value
            val end = _reportEndEpoch.value
            _topProducts.value = repository.getTopSellingProducts(start, end)
            _categorySales.value = repository.getSalesByCategory(start, end)
            _dailySalesTrends.value = repository.getDailySalesTrends(start, end)
        }
    }
}
