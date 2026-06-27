package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.data.repository.CartItem
import com.example.ui.components.EmptyState
import com.example.ui.components.LoadingOverlay
import com.example.ui.components.SummaryCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import com.example.utils.CurrencyUtils
import com.example.utils.DateUtils
import com.example.utils.ExportHelper
import java.util.*

// ==========================================
// SCREEN 1 - DASHBOARD SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ShopViewModel,
    onNavigateToTransactionDetail: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()

    // Calculate today's metrics
    val todayStart = DateUtils.getStartOfDayEpoch(0)
    val todayEnd = DateUtils.getEndOfDayEpoch()
    
    val todaySalesTxns = transactions.filter { it.type == "sale" && it.createdAt in todayStart..todayEnd }
    val todaySalesTotal = todaySalesTxns.sumOf { it.total }
    val todayTxnsCount = todaySalesTxns.size
    
    val totalDebtOutstanding = customers.sumOf { it.totalDebt }
    val totalProductCount = products.size

    // Calculate last 7 days of sales for chart
    val last7DaysSales = remember(transactions) {
        (0..6).reversed().map { dayIndex ->
            val start = DateUtils.getStartOfDayEpoch(dayIndex)
            val end = start + (24 * 60 * 60 * 1000) - 1
            val daySales = transactions.filter { it.type == "sale" && it.createdAt in start..end }.sumOf { it.total }
            
            // Get day abbreviation
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = start
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val dayStr = when (dayOfWeek) {
                Calendar.SUNDAY -> "Min"
                Calendar.MONDAY -> "Sen"
                Calendar.TUESDAY -> "Sel"
                Calendar.WEDNESDAY -> "Rab"
                Calendar.THURSDAY -> "Kam"
                Calendar.FRIDAY -> "Jum"
                Calendar.SATURDAY -> "Sab"
                else -> ""
            }
            Pair(dayStr, daySales)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("TokoKita", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Ringkasan Bisnis Warung", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
        ) {
            // Summary Metrics Grid (2x2 layout)
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    SummaryCard(
                        title = "Penjualan Hari Ini",
                        value = CurrencyUtils.formatRupiah(todaySalesTotal),
                        icon = Icons.Default.TrendingUp,
                        color = PrimaryGreen,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Transaksi Hari Ini",
                        value = "$todayTxnsCount Transaksi",
                        icon = Icons.Default.ReceiptLong,
                        color = ColorInfo,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    SummaryCard(
                        title = "Piutang Berjalan",
                        value = CurrencyUtils.formatRupiah(totalDebtOutstanding),
                        icon = Icons.Default.AccountBalanceWallet,
                        color = ColorDanger,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Total Produk",
                        value = "$totalProductCount Item",
                        icon = Icons.Default.Inventory,
                        color = ColorPurple,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Sales Chart Segment
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Penjualan 7 Hari Terakhir",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Grafik penjualan harian toko",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        CustomBarChart(
                            data = last7DaysSales,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Recent Transactions Segment Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transaksi Terbaru",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Recent Transactions List
            val recentTxns = transactions.take(5)
            if (recentTxns.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Belum ada transaksi hari ini", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else {
                items(recentTxns) { txn ->
                    TransactionItemRow(txn)
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun CustomBarChart(
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    val maxVal = remember(data) { data.maxOfOrNull { it.second }?.coerceAtLeast(10000.0) ?: 10000.0 }
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val paddingLeft = 10f
        val paddingRight = 10f
        val paddingTop = 20f
        val paddingBottom = 40f
        
        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom
        val columnWidth = chartWidth / data.size
        
        data.forEachIndexed { index, pair ->
            val label = pair.first
            val value = pair.second
            
            val barHeight = (value / maxVal) * chartHeight
            val left = paddingLeft + (index * columnWidth) + (columnWidth * 0.15f)
            val right = paddingLeft + ((index + 1) * columnWidth) - (columnWidth * 0.15f)
            val top = height - paddingBottom - barHeight.toFloat()
            val bottom = height - paddingBottom
            
            val isToday = index == data.size - 1
            
            // Draw bar with clean rounded-t corners matching High Density style
            drawRoundRect(
                color = if (isToday) PrimaryGreen else PrimaryLight,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
            
            // Draw Text labels under each bar using Android native canvas
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = if (isToday) android.graphics.Color.parseColor("#16A34A") else android.graphics.Color.parseColor("#9CA3AF")
                    textSize = 28f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    if (isToday) {
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    }
                }
                canvas.nativeCanvas.drawText(
                    label,
                    left + (right - left) / 2,
                    height - 10f,
                    paint
                )
                
                // If there's a positive value, draw a mini text above the bar
                if (value > 0) {
                    val valueText = if (value >= 1000000) {
                        String.format(Locale.US, "%.1fjt", value / 1000000.0)
                    } else if (value >= 1000) {
                        String.format(Locale.US, "%.0fk", value / 1000.0)
                    } else {
                        value.toInt().toString()
                    }
                    val valPaint = android.graphics.Paint().apply {
                        color = if (isToday) android.graphics.Color.parseColor("#16A34A") else android.graphics.Color.parseColor("#6B7280")
                        textSize = 24f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        if (isToday) {
                            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                        }
                    }
                    canvas.nativeCanvas.drawText(
                        valueText,
                        left + (right - left) / 2,
                        top - 10f,
                        valPaint
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItemRow(txn: Transaction) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (txn.type == "sale") PrimaryLight else ColorDanger.copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (txn.type == "sale") Icons.Default.AddShoppingCart else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (txn.type == "sale") PrimaryGreen else ColorDanger,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (txn.type == "sale") {
                            "Penjualan #${txn.id.toString().padStart(3, '0')}"
                        } else {
                            "Bayar Hutang #${txn.id.toString().padStart(3, '0')}"
                        },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = TextDark
                    )
                    Text(
                        text = "${DateUtils.formatDateTime(txn.createdAt)} • ${txn.customerName ?: "Umum/Tunai"}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = TextMuted
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyUtils.formatRupiah(txn.total),
                    fontWeight = FontWeight.Bold,
                    color = if (txn.type == "sale") PrimaryGreen else ColorDanger,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
                )
                if (!txn.note.isNullOrBlank()) {
                    Text(
                        text = txn.note,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ==========================================
// SCREEN 2 - PRODUCTS LIST SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: ShopViewModel,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToEditProduct: (Int) -> Unit
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsStateWithLifecycle()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    val categories = listOf("Semua", "Rokok", "Minuman", "Makanan", "Sembako", "Lainnya")

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { prod ->
            val matchQuery = prod.name.contains(searchQuery, ignoreCase = true) || 
                             prod.category.contains(searchQuery, ignoreCase = true)
            val matchCategory = selectedCategory == "Semua" || prod.category.equals(selectedCategory, ignoreCase = true)
            matchQuery && matchCategory
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Katalog Produk", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddProduct,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_product_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Produk")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Input Row
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari nama produk atau kategori...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("product_search_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = BorderGray
                ),
                singleLine = true
            )

            // Horizontal Category Selector Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Products list
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = "Produk Tidak Ditemukan",
                        description = "Belum ada produk yang sesuai dengan pencarian Anda. Ketuk FAB untuk menambah produk baru.",
                        icon = Icons.Outlined.Inventory,
                        actionButton = {
                            Button(onClick = onNavigateToAddProduct) {
                                Text("Tambah Produk Baru")
                            }
                        }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    items(filteredProducts) { product ->
                        ProductItemCard(
                            product = product,
                            onEdit = { onNavigateToEditProduct(product.id) },
                            onDelete = { productToDelete = product }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    productToDelete?.let { prod ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Hapus Produk") },
            text = { Text("Yakin ingin menghapus produk '${prod.name}' dari katalog? Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteProduct(prod.id) { success ->
                            if (success) {
                                Toast.makeText(context, "Produk berhasil dihapus", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Gagal menghapus produk", Toast.LENGTH_SHORT).show()
                            }
                            productToDelete = null
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ColorDanger)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun ProductItemCard(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isLowStock = product.stock < 5
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = product.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(PrimaryLight, shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                product.category,
                                style = MaterialTheme.typography.bodySmall,
                                color = PrimaryGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Eceran: ${CurrencyUtils.formatRupiah(product.retailPrice)} / ${product.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextDark
                    )
                    Text(
                        text = "Grosir: ${CurrencyUtils.formatRupiah(product.bundlePrice)} / ${product.bundleUnit} (isi ${product.bundleQty})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextDark
                    )
                }

                // Action buttons
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = ColorInfo)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = ColorDanger)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = BorderGray)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isLowStock) Icons.Default.Warning else Icons.Default.Inventory,
                        contentDescription = null,
                        tint = if (isLowStock) ColorWarning else PrimaryGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Stok: ${product.stock} ${product.unit}",
                        fontWeight = FontWeight.Bold,
                        color = if (isLowStock) ColorDanger else TextDark,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (isLowStock) {
                    Text(
                        text = "Stok Hampir Habis!",
                        color = ColorDanger,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================
// SCREEN 3 - ADD / EDIT PRODUCT SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    viewModel: ShopViewModel,
    productId: Int = 0,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsStateWithLifecycle()
    val product = remember(products, productId) { products.firstOrNull { it.id == productId } }

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Rokok") }
    var unit by remember { mutableStateOf("bungkus") }
    var bundleUnit by remember { mutableStateOf("slop") }
    var bundleQtyStr by remember { mutableStateOf("10") }
    var costPriceStr by remember { mutableStateOf("") }
    var retailPriceStr by remember { mutableStateOf("") }
    var bundlePriceStr by remember { mutableStateOf("") }
    var stockStr by remember { mutableStateOf("0") }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    val categories = listOf("Rokok", "Minuman", "Makanan", "Sembako", "Lainnya")

    // Populate data if editing
    LaunchedEffect(product) {
        product?.let {
            name = it.name
            category = it.category
            unit = it.unit
            bundleUnit = it.bundleUnit
            bundleQtyStr = it.bundleQty.toString()
            costPriceStr = it.costPrice.toInt().toString()
            retailPriceStr = it.retailPrice.toInt().toString()
            bundlePriceStr = it.bundlePrice.toInt().toString()
            stockStr = it.stock.toString()
        }
    }

    // Live margin calculations
    val bundleQty = bundleQtyStr.toIntOrNull() ?: 1
    val costPrice = costPriceStr.toDoubleOrNull() ?: 0.0
    val retailPrice = retailPriceStr.toDoubleOrNull() ?: 0.0
    val bundlePrice = bundlePriceStr.toDoubleOrNull() ?: 0.0

    val unitCostPrice = if (bundleQty > 0) costPrice / bundleQty else 0.0
    val eceranMarginNominal = retailPrice - unitCostPrice
    val eceranMarginPercentage = if (unitCostPrice > 0) (eceranMarginNominal / unitCostPrice) * 100 else 0.0

    val bundleMarginNominal = bundlePrice - costPrice
    val bundleMarginPercentage = if (costPrice > 0) (bundleMarginNominal / costPrice) * 100 else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (productId == 0) "Tambah Produk" else "Edit Produk", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Info Group
            item {
                Text("INFORMASI UMUM", style = MaterialTheme.typography.titleSmall, color = TextMuted, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Produk *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                // Dropdown Category Select
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        label = { Text("Kategori *") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(onClick = { categoryDropdownExpanded = !categoryDropdownExpanded }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Packagings & Satuan Group
            item {
                Divider(color = BorderGray, modifier = Modifier.padding(vertical = 8.dp))
                Text("SATUAN & BUNDLE", style = MaterialTheme.typography.titleSmall, color = TextMuted, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Satuan Eceran (pcs/bks)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = bundleUnit,
                        onValueChange = { bundleUnit = it },
                        label = { Text("Satuan Bundle (slop/dus)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = bundleQtyStr,
                    onValueChange = { bundleQtyStr = it },
                    label = { Text("Isi Eceran per Bundle (cth: 10) *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Pricing Group
            item {
                Divider(color = BorderGray, modifier = Modifier.padding(vertical = 8.dp))
                Text("HARGA JUAL & MODAL", style = MaterialTheme.typography.titleSmall, color = TextMuted, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = costPriceStr,
                    onValueChange = { costPriceStr = it },
                    label = { Text("Harga Modal (per Bundle)") },
                    prefix = { Text("Rp ") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = retailPriceStr,
                        onValueChange = { retailPriceStr = it },
                        label = { Text("Jual Eceran *") },
                        prefix = { Text("Rp ") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = bundlePriceStr,
                        onValueChange = { bundlePriceStr = it },
                        label = { Text("Jual Bundle *") },
                        prefix = { Text("Rp ") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Helper Margin Text Analysis
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryLight.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Analisis Margin Profit:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryGreen
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• Margin Eceran: ${CurrencyUtils.formatRupiah(eceranMarginNominal)} / $unit (${String.format(Locale.US, "%.1f", eceranMarginPercentage)}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDark
                        )
                        Text(
                            text = "• Margin Bundle: ${CurrencyUtils.formatRupiah(bundleMarginNominal)} / $bundleUnit (${String.format(Locale.US, "%.1f", bundleMarginPercentage)}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDark
                        )
                        
                        // Low margin warnings
                        if (eceranMarginPercentage < 5.0 && eceranMarginPercentage > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Warning: Margin eceran Anda sangat rendah (< 5%)!",
                                color = ColorWarning,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Stock Group
            item {
                Divider(color = BorderGray, modifier = Modifier.padding(vertical = 8.dp))
                Text("STOK GUDANG", style = MaterialTheme.typography.titleSmall, color = TextMuted, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = stockStr,
                    onValueChange = { stockStr = it },
                    label = { Text("Stok Sekarang (Satuan Eceran) *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Save Buttons
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        // Validate inputs
                        if (name.isBlank() || bundleQtyStr.isBlank() || retailPriceStr.isBlank() || bundlePriceStr.isBlank() || stockStr.isBlank()) {
                            Toast.makeText(context, "Semua kolom bertanda * wajib diisi!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val finalBundleQty = bundleQtyStr.toIntOrNull() ?: 1
                        val finalCostPrice = costPriceStr.toDoubleOrNull() ?: 0.0
                        val finalRetailPrice = retailPriceStr.toDoubleOrNull() ?: 0.0
                        val finalBundlePrice = bundlePriceStr.toDoubleOrNull() ?: 0.0
                        val finalStock = stockStr.toIntOrNull() ?: 0

                        val unitCost = finalCostPrice / finalBundleQty
                        if (finalRetailPrice < unitCost) {
                            Toast.makeText(context, "Harga eceran tidak boleh di bawah modal per unit eceran!", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        viewModel.saveProduct(
                            id = productId,
                            name = name,
                            category = category,
                            unit = unit,
                            bundleUnit = bundleUnit,
                            bundleQty = finalBundleQty,
                            costPrice = finalCostPrice,
                            retailPrice = finalRetailPrice,
                            bundlePrice = finalBundlePrice,
                            stock = finalStock
                        ) { success ->
                            if (success) {
                                Toast.makeText(context, "Produk berhasil disimpan", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            } else {
                                Toast.makeText(context, "Gagal menyimpan produk", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_product_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("SIMPAN PRODUK", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// ==========================================
// SCREEN 4 - KASIR SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KasirScreen(
    viewModel: ShopViewModel
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val txnSuccess by viewModel.transactionSuccess.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedProductForCart by remember { mutableStateOf<Product?>(null) }
    var cartQtySelection by remember { mutableStateOf(1) }
    var cartPriceTypeSelection by remember { mutableStateOf("retail") } // "retail" or "bundle"

    var customerName by remember { mutableStateOf("") }
    var isDebtTransaction by remember { mutableStateOf(false) }
    var noteInput by remember { mutableStateOf("") }

    val filteredProducts = remember(products, searchQuery) {
        products.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val totalCartPrice = remember(cart) { cart.sumOf { it.subtotal } }

    // Handle transaction outcome dialogs
    LaunchedEffect(txnSuccess) {
        txnSuccess?.let { success ->
            if (success) {
                Toast.makeText(context, "Transaksi Sukses Terbayarkan!", Toast.LENGTH_LONG).show()
                // Reset transaction values
                customerName = ""
                isDebtTransaction = false
                noteInput = ""
            } else {
                Toast.makeText(context, "Terjadi kesalahan dalam memproses transaksi.", Toast.LENGTH_LONG).show()
            }
            viewModel.resetTransactionStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kasir Toko Kita", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Product Catalog Search Section
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Pilih produk belanja...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Horizontal small product cards
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Produk kosong", color = TextMuted)
                }
            } else {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProducts) { product ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier
                                .width(150.dp)
                                .clickable {
                                    selectedProductForCart = product
                                    cartQtySelection = 1
                                    cartPriceTypeSelection = "retail"
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = product.name,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = CurrencyUtils.formatRupiah(product.retailPrice),
                                    color = PrimaryGreen,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Stok: ${product.stock}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (product.stock < 5) ColorDanger else TextMuted
                                )
                            }
                        }
                    }
                }
            }

            Divider(color = BorderGray, modifier = Modifier.padding(horizontal = 16.dp))

            // Cart Items Details View Segment
            Text(
                "KERANJANG BELANJA",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (cart.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = "Keranjang Belanja Kosong",
                        description = "Ketuk katalog produk di atas untuk memasukkan barang belanjaan pelanggan.",
                        icon = Icons.Outlined.ShoppingCart
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    items(cart) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, shape = RoundedCornerShape(12.dp))
                                .padding(12.dp)
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.product.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${if (item.priceType == "retail") "Eceran" else "Bundle"} @ ${CurrencyUtils.formatRupiah(item.unitPrice)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                                Text(
                                    text = "Subtotal: ${CurrencyUtils.formatRupiah(item.subtotal)}",
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryGreen,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            // Plus Minus Controls
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.updateCartQuantity(item.product.id, item.priceType, item.quantity - 1) }) {
                                    Icon(Icons.Default.Remove, contentDescription = null, tint = ColorDanger)
                                }
                                Text("${item.quantity}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                IconButton(onClick = { viewModel.updateCartQuantity(item.product.id, item.priceType, item.quantity + 1) }) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryGreen)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }

            // Checkout Panel Bottom Drawer View
            Card(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Overall Cart Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Pembayaran", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = CurrencyUtils.formatRupiah(totalCartPrice),
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Input Pelanggan Info
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        placeholder = { Text("Nama Pelanggan (Wajib jika Hutang)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Cash / Debt Toggles Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tipe Pembayaran", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = !isDebtTransaction,
                                onClick = { isDebtTransaction = false }
                            )
                            Text("Tunai", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.width(16.dp))
                            RadioButton(
                                selected = isDebtTransaction,
                                onClick = { isDebtTransaction = true }
                            )
                            Text("Hutang", style = MaterialTheme.typography.bodyMedium, color = ColorDanger, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        placeholder = { Text("Catatan transaksi (opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (cart.isEmpty()) return@Button
                            if (isDebtTransaction && customerName.isBlank()) {
                                Toast.makeText(context, "Nama pelanggan wajib diisi untuk transaksi HUTANG!", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            viewModel.checkout(
                                customerName = customerName,
                                isDebt = isDebtTransaction,
                                note = noteInput
                            )
                        },
                        enabled = cart.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("process_transaction_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("PROSES TRANSAKSI", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Modal to add Product to Cart
    selectedProductForCart?.let { product ->
        Dialog(onDismissRequest = { selectedProductForCart = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Choose Retail Price or Bundle Price Category
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (cartPriceTypeSelection == "retail") PrimaryLight else BorderGray.copy(alpha = 0.3f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (cartPriceTypeSelection == "retail") PrimaryGreen else Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { cartPriceTypeSelection = "retail" }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Eceran", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("${CurrencyUtils.formatRupiah(product.retailPrice)}/${product.unit}", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (cartPriceTypeSelection == "bundle") PrimaryLight else BorderGray.copy(alpha = 0.3f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (cartPriceTypeSelection == "bundle") PrimaryGreen else Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { cartPriceTypeSelection = "bundle" }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Bundle", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("${CurrencyUtils.formatRupiah(product.bundlePrice)}/${product.bundleUnit}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quantity Counter Widget
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { if (cartQtySelection > 1) cartQtySelection-- }) {
                            Icon(Icons.Default.Remove, contentDescription = null, tint = ColorDanger)
                        }
                        Text(
                            text = "$cartQtySelection",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        IconButton(onClick = { cartQtySelection++ }) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val computedSubtotal = if (cartPriceTypeSelection == "retail") {
                        product.retailPrice * cartQtySelection
                    } else {
                        product.bundlePrice * cartQtySelection
                    }

                    Text(
                        text = "Subtotal: ${CurrencyUtils.formatRupiah(computedSubtotal)}",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.addToCart(product, cartPriceTypeSelection, cartQtySelection)
                            selectedProductForCart = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Tambah ke Keranjang", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 5 - HUTANG SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HutangScreen(
    viewModel: ShopViewModel,
    onNavigateToCustomerDetail: (Int) -> Unit
) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showAddCustomerDialog by remember { mutableStateOf(false) }

    var newCustomerName by remember { mutableStateOf("") }
    var newCustomerPhone by remember { mutableStateOf("") }

    val filteredCustomers = remember(customers, searchQuery) {
        customers.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val totalShopPiutang = remember(customers) { customers.sumOf { it.totalDebt } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manajemen Piutang", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCustomerDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Tambah Pelanggan")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Summary Header Card Box
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ColorDanger.copy(alpha = 0.08f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Piutang Berjalan", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyUtils.formatRupiah(totalShopPiutang),
                            fontWeight = FontWeight.Bold,
                            color = ColorDanger,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(ColorDanger.copy(alpha = 0.15f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = ColorDanger)
                    }
                }
            }

            // Search input field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari nama pelanggan...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Customers dynamic FlatList list
            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = "Daftar Pelanggan Kosong",
                        description = "Belum ada daftar nama pelanggan piutang. Buat pelanggan baru untuk mencatat transaksi hutang.",
                        icon = Icons.Outlined.People,
                        actionButton = {
                            Button(onClick = { showAddCustomerDialog = true }) {
                                Text("Tambah Pelanggan Baru")
                            }
                        }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    items(filteredCustomers) { customer ->
                        CustomerDebtItemRow(customer, onClick = { onNavigateToCustomerDetail(customer.id) })
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Modal dialog to add client profile
    if (showAddCustomerDialog) {
        AlertDialog(
            onDismissRequest = { showAddCustomerDialog = false },
            title = { Text("Registrasi Pelanggan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newCustomerName,
                        onValueChange = { newCustomerName = it },
                        label = { Text("Nama Pelanggan *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = newCustomerPhone,
                        onValueChange = { newCustomerPhone = it },
                        label = { Text("Nomor Telepon/HP") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCustomerName.isBlank()) {
                            Toast.makeText(context, "Nama wajib diisi!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.addCustomer(newCustomerName, newCustomerPhone.takeIf { it.isNotBlank() }) { success ->
                            if (success) {
                                Toast.makeText(context, "Pelanggan berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                                newCustomerName = ""
                                newCustomerPhone = ""
                                showAddCustomerDialog = false
                            } else {
                                Toast.makeText(context, "Gagal menambahkan pelanggan", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Daftar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomerDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun CustomerDebtItemRow(customer: Customer, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Letter Avatar circle (compact)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(PrimaryLight, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = customer.name.firstOrNull()?.uppercase() ?: "P",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.sp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(customer.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = TextDark)
                    Text(customer.phone ?: "Tanpa No. HP", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = TextMuted)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (customer.totalDebt > 0) {
                    Text(
                        text = CurrencyUtils.formatRupiah(customer.totalDebt),
                        fontWeight = FontWeight.Bold,
                        color = ColorDanger,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
                    )
                    Text("Belum Lunas", color = ColorDanger, style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp), fontWeight = FontWeight.Bold)
                } else {
                    Box(
                        modifier = Modifier
                            .background(PrimaryLight, shape = RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Lunas", color = PrimaryGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp))
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 6 - CUSTOMER LEDGER DETAIL SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailPelangganScreen(
    viewModel: ShopViewModel,
    customerId: Int,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    
    // Select customer context in ViewModel
    LaunchedEffect(customerId) {
        viewModel.selectCustomer(customerId)
    }

    val customer by viewModel.selectedCustomer.collectAsStateWithLifecycle()
    val ledgerHistory by viewModel.selectedCustomerLedger.collectAsStateWithLifecycle()

    var showAddDebtDialog by remember { mutableStateOf(false) }
    var showPayDebtDialog by remember { mutableStateOf(false) }

    var inputAmountStr by remember { mutableStateOf("") }
    var inputDesc by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(customer?.name ?: "Detail Pelanggan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            customer?.let { cust ->
                // Information stats segment
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (cust.totalDebt > 0) ColorDanger.copy(alpha = 0.08f) else PrimaryLight.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Sisa Piutang Berjalan", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyUtils.formatRupiah(cust.totalDebt),
                            fontWeight = FontWeight.Bold,
                            color = if (cust.totalDebt > 0) ColorDanger else PrimaryGreen,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("No. HP: ${cust.phone ?: "-"}", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Quick buttons row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { 
                            showAddDebtDialog = true 
                            inputAmountStr = ""
                            inputDesc = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ColorDanger),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah Hutang", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { 
                            showPayDebtDialog = true 
                            inputAmountStr = ""
                            inputDesc = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bayar Hutang", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = BorderGray, modifier = Modifier.padding(horizontal = 16.dp))

            // Timeline header
            Text(
                "RIWAYAT HUTANG & PEMBAYARAN (TIMELINE)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // Timeline flat list
            if (ledgerHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada riwayat keuangan.", color = TextMuted)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    items(ledgerHistory.reversed()) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            // Timeline dot
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(
                                            if (entry.type == "hutang") ColorDanger else PrimaryGreen,
                                            shape = CircleShape
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(50.dp)
                                        .background(BorderGray)
                                )
                            }
                            
                            // Timeline details card
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            entry.description ?: if (entry.type == "hutang") "Tambah hutang" else "Bayar hutang",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(DateUtils.formatDateTime(entry.createdAt), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Sisa Saldo: ${CurrencyUtils.formatRupiah(entry.runningBalance)}",
                                            fontWeight = FontWeight.Medium,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMuted
                                        )
                                    }
                                    Text(
                                        text = (if (entry.type == "hutang") "+" else "-") + CurrencyUtils.formatRupiah(entry.amount),
                                        fontWeight = FontWeight.Bold,
                                        color = if (entry.type == "hutang") ColorDanger else PrimaryGreen,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Add Debt Dialog
    if (showAddDebtDialog) {
        AlertDialog(
            onDismissRequest = { showAddDebtDialog = false },
            title = { Text("Tambah Nominal Hutang") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = inputAmountStr,
                        onValueChange = { inputAmountStr = it },
                        label = { Text("Nominal Hutang (Rp) *") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = inputDesc,
                        onValueChange = { inputDesc = it },
                        label = { Text("Keterangan/Catatan") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = inputAmountStr.toDoubleOrNull() ?: 0.0
                        if (amt <= 0.0) {
                            Toast.makeText(context, "Nominal wajib diisi dengan benar!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.addManualDebt(customerId, amt, inputDesc.takeIf { it.isNotBlank() }) { success ->
                            if (success) {
                                Toast.makeText(context, "Hutang berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                                showAddDebtDialog = false
                            } else {
                                Toast.makeText(context, "Gagal memproses", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorDanger)
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDebtDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Modal Pay Debt Dialog
    if (showPayDebtDialog) {
        AlertDialog(
            onDismissRequest = { showPayDebtDialog = false },
            title = { Text("Bayar Hutang Pelanggan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = inputAmountStr,
                        onValueChange = { inputAmountStr = it },
                        label = { Text("Nominal Pembayaran (Rp) *") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = inputDesc,
                        onValueChange = { inputDesc = it },
                        label = { Text("Keterangan/Metode (cth: cash)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = inputAmountStr.toDoubleOrNull() ?: 0.0
                        if (amt <= 0.0) {
                            Toast.makeText(context, "Nominal wajib diisi dengan benar!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.payDebt(customerId, amt, inputDesc.takeIf { it.isNotBlank() }) { success ->
                            if (success) {
                                Toast.makeText(context, "Pembayaran berhasil dicatat", Toast.LENGTH_SHORT).show()
                                showPayDebtDialog = false
                            } else {
                                Toast.makeText(context, "Gagal memproses", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Bayar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPayDebtDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

// ==========================================
// SCREEN 7 - REPORTS & ANALYTICS SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporanScreen(
    viewModel: ShopViewModel
) {
    val context = LocalContext.current
    
    // Load reports initially when opening screen
    LaunchedEffect(Unit) {
        viewModel.loadReports()
    }

    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val topProducts by viewModel.topProducts.collectAsStateWithLifecycle()
    val categoriesSales by viewModel.categorySales.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()

    val rangeType by viewModel.reportRange.collectAsStateWithLifecycle()
    val startEpoch by viewModel.reportStartEpoch.collectAsStateWithLifecycle()
    val endEpoch by viewModel.reportEndEpoch.collectAsStateWithLifecycle()

    val startDateStr = DateUtils.formatDate(startEpoch)
    val endDateStr = DateUtils.formatDate(endEpoch)

    val reportRanges = listOf("today" to "Hari Ini", "week" to "Minggu Ini", "month" to "Bulan Ini", "year" to "Tahun Ini")

    // Compile active range totals
    val rangeTxns = transactions.filter { it.createdAt in startEpoch..endEpoch }
    val totalSales = rangeTxns.filter { it.type == "sale" }.sumOf { it.total }
    val transCount = rangeTxns.count { it.type == "sale" }
    val avgTransaction = if (transCount > 0) totalSales / transCount else 0.0
    val totalDebtAdded = rangeTxns.filter { it.type == "sale" && it.customerName != null }.sumOf { it.total }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan & Analitik", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Horizontal quick ranges row
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(reportRanges) { range ->
                        val isSelected = rangeType == range.first
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setReportRange(range.first) },
                            label = { Text(range.second) }
                        )
                    }
                }
            }

            // Custom Range Date pickers button
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Periode Aktif", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            Text("$startDateStr s/d $endDateStr", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }

                        Button(
                            onClick = {
                                val calendar = Calendar.getInstance()
                                val year = calendar.get(Calendar.YEAR)
                                val month = calendar.get(Calendar.MONTH)
                                val day = calendar.get(Calendar.DAY_OF_MONTH)

                                // Date range pickers triggering
                                DatePickerDialog(context, { _, yStart, mStart, dStart ->
                                    val startCal = Calendar.getInstance()
                                    startCal.set(yStart, mStart, dStart, 0, 0, 0)
                                    
                                    DatePickerDialog(context, { _, yEnd, mEnd, dEnd ->
                                        val endCal = Calendar.getInstance()
                                        endCal.set(yEnd, mEnd, dEnd, 23, 59, 59)
                                        viewModel.setCustomReportRange(startCal.timeInMillis, endCal.timeInMillis)
                                    }, year, month, day).show()

                                }, year, month, day).show()
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ubah")
                        }
                    }
                }
            }

            // Report KPI values
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    SummaryCard(
                        title = "Total Omset",
                        value = CurrencyUtils.formatRupiah(totalSales),
                        icon = Icons.Default.TrendingUp,
                        color = PrimaryGreen,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Total Transaksi",
                        value = "$transCount Transaksi",
                        icon = Icons.Default.ReceiptLong,
                        color = ColorInfo,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    SummaryCard(
                        title = "Rata-rata/Nota",
                        value = CurrencyUtils.formatRupiah(avgTransaction),
                        icon = Icons.Default.PriceCheck,
                        color = ColorPurple,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Hutang Baru Masuk",
                        value = CurrencyUtils.formatRupiah(totalDebtAdded),
                        icon = Icons.Default.AccountBalanceWallet,
                        color = ColorDanger,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Export panel
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Export & Cetak Laporan", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Bagikan data laporan warung Anda secara offline", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    ExportHelper.exportToCsv(context, startDateStr, endDateStr, rangeTxns, topProducts, customers)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.GridOn, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Excel/CSV", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    ExportHelper.exportToPdf(context, startDateStr, endDateStr, rangeTxns, topProducts, customers)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ColorInfo),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cetak PDF", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Top Products selling
            item {
                Text("PRODUK PALING LARIS", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                if (topProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Tidak ada data produk terjual.", color = TextMuted)
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Nama Produk", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodySmall)
                                Text("Terjual", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                                Text("Total", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
                            }
                            Divider(color = BorderGray, modifier = Modifier.padding(vertical = 4.dp))
                            
                            topProducts.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item.productName, modifier = Modifier.weight(1.5f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                                    Text("${item.totalQty}", modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(CurrencyUtils.formatRupiah(item.totalSales), modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Spacings
            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
