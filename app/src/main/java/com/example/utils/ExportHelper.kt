package com.example.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.*
import java.io.File
import java.io.FileWriter

object ExportHelper {

    fun exportToCsv(
        context: Context,
        startDateStr: String,
        endDateStr: String,
        transactions: List<Transaction>,
        topProducts: List<TopProduct>,
        customers: List<Customer>
    ) {
        try {
            val cacheDir = context.cacheDir
            val csvFile = File(cacheDir, "Laporan_TokoKita_${startDateStr.replace("/", "-")}.csv")
            val writer = FileWriter(csvFile)

            // UTF-8 BOM so Excel opens with proper accents
            writer.write('\ufeff'.code)

            // 1. Header Ringkasan
            writer.write("LAPORAN TOKOKITA\n")
            writer.write("Periode;${startDateStr} s/d ${endDateStr}\n")
            writer.write("Tanggal Cetak;${DateUtils.formatDateTime(System.currentTimeMillis())}\n\n")

            val totalSales = transactions.filter { it.type == "sale" }.sumOf { it.total }
            val totalSalesCount = transactions.count { it.type == "sale" }
            val avgSales = if (totalSalesCount > 0) totalSales / totalSalesCount else 0.0
            val totalDebtSales = customers.sumOf { it.totalDebt }

            writer.write("RINGKASAN METRIK\n")
            writer.write("Total Penjualan;${CurrencyUtils.formatRupiah(totalSales)}\n")
            writer.write("Jumlah Transaksi;${totalSalesCount}\n")
            writer.write("Rata-rata Penjualan;${CurrencyUtils.formatRupiah(avgSales)}\n")
            writer.write("Total Piutang Berjalan;${CurrencyUtils.formatRupiah(totalDebtSales)}\n\n")

            // 2. Detail Transaksi
            writer.write("DATA TRANSAKSI\n")
            writer.write("No;Tanggal;Pelanggan;Total;Tipe;Keterangan\n")
            transactions.forEachIndexed { index, txn ->
                val date = DateUtils.formatDateTime(txn.createdAt)
                val customer = txn.customerName ?: "Tunai"
                val type = if (txn.type == "sale") "Penjualan" else "Bayar Hutang"
                val note = txn.note ?: "-"
                writer.write("${index + 1};$date;$customer;${txn.total};$type;$note\n")
            }
            writer.write("\n")

            // 3. Produk Terlaris
            writer.write("PRODUK TERLARIS (TOP 10)\n")
            writer.write("No;Nama Produk;Qty Terjual;Total Penjualan\n")
            topProducts.forEachIndexed { index, prod ->
                writer.write("${index + 1};${prod.productName};${prod.totalQty};${prod.totalSales}\n")
            }
            writer.write("\n")

            // 4. Rekap Hutang
            writer.write("REKAP PIUTANG PELANGGAN\n")
            writer.write("No;Nama Pelanggan;No. HP;Total Piutang;Status\n")
            customers.forEachIndexed { index, cust ->
                val status = if (cust.totalDebt > 0) "Belum Lunas" else "Lunas"
                writer.write("${index + 1};${cust.name};${cust.phone ?: "-"};${cust.totalDebt};$status\n")
            }

            writer.flush()
            writer.close()

            // Share file
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, csvFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Laporan TokoKita")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Bagikan Laporan Excel/CSV"))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportToPdf(
        context: Context,
        startDateStr: String,
        endDateStr: String,
        transactions: List<Transaction>,
        topProducts: List<TopProduct>,
        customers: List<Customer>
    ) {
        val totalSales = transactions.filter { it.type == "sale" }.sumOf { it.total }
        val totalSalesCount = transactions.count { it.type == "sale" }
        val avgSales = if (totalSalesCount > 0) totalSales / totalSalesCount else 0.0
        val totalDebtSales = customers.sumOf { it.totalDebt }

        // Compile HTML
        val htmlBuilder = StringBuilder()
        htmlBuilder.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Laporan TokoKita</title>
                <style>
                    body { font-family: sans-serif; color: #1f2937; margin: 20px; line-height: 1.4; }
                    .header { text-align: center; margin-bottom: 30px; border-bottom: 2px solid #16a34a; padding-bottom: 10px; }
                    .header h1 { color: #16a34a; margin: 0 0 5px 0; font-size: 24px; }
                    .header p { margin: 0; color: #6b7280; font-size: 14px; }
                    
                    .summary-grid { display: flex; justify-content: space-between; margin-bottom: 30px; gap: 10px; }
                    .summary-box { flex: 1; border: 1px solid #e5e7eb; padding: 15px; border-radius: 8px; background-color: #f9fafb; text-align: center; }
                    .summary-box .title { font-size: 11px; color: #6b7280; text-transform: uppercase; font-weight: bold; margin-bottom: 5px; }
                    .summary-box .value { font-size: 16px; color: #111827; font-weight: bold; }
                    
                    h2 { font-size: 16px; color: #15803d; border-bottom: 1px solid #e5e7eb; padding-bottom: 5px; margin-top: 25px; }
                    
                    table { width: 100%; border-collapse: collapse; margin-bottom: 20px; font-size: 12px; }
                    th, td { border: 1px solid #e5e7eb; padding: 8px; text-align: left; }
                    th { background-color: #f3f4f6; color: #374151; font-weight: bold; }
                    tr:nth-child(even) { background-color: #f9fafb; }
                    
                    .text-right { text-align: right; }
                    .badge { display: inline-block; padding: 3px 6px; border-radius: 4px; font-size: 10px; font-weight: bold; }
                    .badge-sale { background-color: #dcfce7; color: #16a34a; }
                    .badge-debt { background-color: #fee2e2; color: #dc2626; }
                    
                    .footer { text-align: center; font-size: 10px; color: #9ca3af; margin-top: 50px; border-top: 1px solid #e5e7eb; padding-top: 10px; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>LAPORAN KEUANGAN TOKOKITA</h1>
                    <p>Periode: $startDateStr s/d $endDateStr</p>
                    <p>Dicetak: ${DateUtils.formatDateTime(System.currentTimeMillis())}</p>
                </div>
                
                <div class="summary-grid">
                    <div class="summary-box">
                        <div class="title">Total Penjualan</div>
                        <div class="value">${CurrencyUtils.formatRupiah(totalSales)}</div>
                    </div>
                    <div class="summary-box">
                        <div class="title">Transaksi Penjualan</div>
                        <div class="value">$totalSalesCount Kali</div>
                    </div>
                    <div class="summary-box">
                        <div class="title">Rata-rata/Transaksi</div>
                        <div class="value">${CurrencyUtils.formatRupiah(avgSales)}</div>
                    </div>
                    <div class="summary-box">
                        <div class="title">Total Piutang Toko</div>
                        <div class="value">${CurrencyUtils.formatRupiah(totalDebtSales)}</div>
                    </div>
                </div>
                
                <h2>Ringkasan Transaksi Terbaru (Hingga 50)</h2>
                <table>
                    <thead>
                        <tr>
                            <th>No</th>
                            <th>Tanggal</th>
                            <th>Pelanggan</th>
                            <th>Tipe</th>
                            <th class="text-right">Total Transaksi</th>
                            <th>Catatan</th>
                        </tr>
                    </thead>
                    <tbody>
        """)

        transactions.take(50).forEachIndexed { index, txn ->
            val date = DateUtils.formatDateTime(txn.createdAt)
            val customer = txn.customerName ?: "Umum/Tunai"
            val typeBadge = if (txn.type == "sale") {
                "<span class=\"badge badge-sale\">Penjualan</span>"
            } else {
                "<span class=\"badge badge-debt\">Bayar Hutang</span>"
            }
            htmlBuilder.append("""
                <tr>
                    <td>${index + 1}</td>
                    <td>$date</td>
                    <td>$customer</td>
                    <td>$typeBadge</td>
                    <td class="text-right">${CurrencyUtils.formatRupiah(txn.total)}</td>
                    <td>${txn.note ?: "-"}</td>
                </tr>
            """)
        }

        htmlBuilder.append("""
                    </tbody>
                </table>
                
                <h2>Produk Terlaris (Top Selling)</h2>
                <table>
                    <thead>
                        <tr>
                            <th>No</th>
                            <th>Nama Produk</th>
                            <th class="text-right">Jumlah Terjual</th>
                            <th class="text-right">Total Penjualan</th>
                        </tr>
                    </thead>
                    <tbody>
        """)

        topProducts.forEachIndexed { index, prod ->
            htmlBuilder.append("""
                <tr>
                    <td>${index + 1}</td>
                    <td>${prod.productName}</td>
                    <td class="text-right">${prod.totalQty}</td>
                    <td class="text-right">${CurrencyUtils.formatRupiah(prod.totalSales)}</td>
                </tr>
            """)
        }

        htmlBuilder.append("""
                    </tbody>
                </table>
                
                <h2>Rekap Piutang Pelanggan</h2>
                <table>
                    <thead>
                        <tr>
                            <th>No</th>
                            <th>Nama Pelanggan</th>
                            <th>No. Telepon</th>
                            <th class="text-right">Total Piutang</th>
                            <th>Status</th>
                        </tr>
                    </thead>
                    <tbody>
        """)

        customers.forEachIndexed { index, cust ->
            val status = if (cust.totalDebt > 0) {
                "<span class=\"badge badge-debt\">Ada Hutang</span>"
            } else {
                "<span class=\"badge badge-sale\">Lunas</span>"
            }
            htmlBuilder.append("""
                <tr>
                    <td>${index + 1}</td>
                    <td>${cust.name}</td>
                    <td>${cust.phone ?: "-"}</td>
                    <td class="text-right">${CurrencyUtils.formatRupiah(cust.totalDebt)}</td>
                    <td>$status</td>
                </tr>
            """)
        }

        htmlBuilder.append("""
                    </tbody>
                </table>
                
                <div class="footer">
                    <p>Laporan ini dibuat otomatis oleh Aplikasi TokoKita - Manajemen Toko Kecil & Warung Anda.</p>
                </div>
            </body>
            </html>
        """)

        val htmlString = htmlBuilder.toString()
        val jobName = "Laporan_TokoKita_${startDateStr.replace("/", "_")}"

        // Load into Webview to print natively
        try {
            val mainLooper = android.os.Looper.getMainLooper()
            val handler = android.os.Handler(mainLooper)
            handler.post {
                val webView = android.webkit.WebView(context)
                webView.webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                        val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
                        val printAdapter = webView.createPrintDocumentAdapter(jobName)
                        printManager.print(jobName, printAdapter, android.print.PrintAttributes.Builder().build())
                    }
                }
                webView.loadDataWithBaseURL(null, htmlString, "text/html", "utf-8", null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
