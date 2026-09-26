package com.dollariniyavan.finance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { FinanceApp(FinanceDb(this)) } }
}

private fun money(value: Double) = NumberFormat.getCurrencyInstance(Locale.getDefault()).format(value)

@Composable fun FinanceApp(db: FinanceDb) {
    var tab by remember { mutableIntStateOf(0) }
    var refresh by remember { mutableIntStateOf(0) }
    val borrowers = remember(refresh) { db.borrowers() }
    val loans = remember(refresh) { db.loans() }
    Scaffold(topBar = { TopAppBar(title = { Text("Finance") }) }, bottomBar = {
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("Dashboard", "Borrowers", "Loans").forEachIndexed { i, label -> FilterChip(selected = tab == i, onClick = { tab = i }, label = { Text(label) }) }
        }
    }) { pad -> Column(Modifier.padding(pad).padding(16.dp).fillMaxSize()) {
        when (tab) { 0 -> Dashboard(borrowers, loans); 1 -> BorrowersScreen(borrowers) { name, phone, address -> db.addBorrower(name, phone, address); refresh++ }; else -> LoansScreen(borrowers, loans, { id, amount -> db.addPayment(id, amount); refresh++ }) { id, principal, rate, date -> db.addLoan(id, principal, rate, date); refresh++ } }
    } }
}

@Composable private fun Dashboard(borrowers: List<Borrower>, loans: List<Loan>) {
    val outstanding = loans.sumOf { (it.principal + it.principal * it.interestRate / 100) - it.paid }
    Text("Overview", style = MaterialTheme.typography.headlineMedium); Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Stat("Borrowers", borrowers.size.toString(), Modifier.weight(1f)); Stat("Active loans", loans.count { it.status == "ACTIVE" }.toString(), Modifier.weight(1f)) }
    Spacer(Modifier.height(8.dp)); Stat("Outstanding", money(outstanding), Modifier.fillMaxWidth()); Spacer(Modifier.height(24.dp)); Text("All data is stored locally on this device.", style = MaterialTheme.typography.bodyMedium)
}
@Composable private fun Stat(label: String, value: String, modifier: Modifier) { Card(modifier) { Column(Modifier.padding(16.dp)) { Text(label); Text(value, style = MaterialTheme.typography.titleLarge) } } }

@Composable private fun BorrowersScreen(data: List<Borrower>, add: (String, String, String) -> Unit) {
    var show by remember { mutableStateOf(false) }; Text("Borrowers", style = MaterialTheme.typography.headlineMedium); Button({ show = true }, Modifier.fillMaxWidth()) { Text("Add borrower") }; Spacer(Modifier.height(8.dp))
    LazyColumn { items(data) { b -> Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Column(Modifier.padding(12.dp)) { Text(b.name, style = MaterialTheme.typography.titleMedium); Text("${b.phone}  ${b.address}") } } } }
    if (show) BorrowerDialog({ show = false }, add)
}
@Composable private fun BorrowerDialog(close: () -> Unit, add: (String, String, String) -> Unit) { var name by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }; var address by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = close, title = { Text("New borrower") }, text = { Column { OutlinedTextField(name, { name = it }, label = { Text("Name") }); OutlinedTextField(phone, { phone = it }, label = { Text("Phone") }); OutlinedTextField(address, { address = it }, label = { Text("Address") }) } }, confirmButton = { Button({ if (name.isNotBlank()) { add(name, phone, address); close() } }) { Text("Save") } }, dismissButton = { TextButton(close) { Text("Cancel") } }) }

@Composable private fun LoansScreen(borrowers: List<Borrower>, loans: List<Loan>, pay: (Long, Double) -> Unit, add: (Long, Double, Double, String) -> Unit) {
    var show by remember { mutableStateOf(false) }; Text("Loans", style = MaterialTheme.typography.headlineMedium); Button({ if (borrowers.isNotEmpty()) show = true }, Modifier.fillMaxWidth()) { Text("Add loan") }; Spacer(Modifier.height(8.dp))
    LazyColumn { items(loans) { l -> Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Column(Modifier.padding(12.dp)) { Text(l.borrowerName, style = MaterialTheme.typography.titleMedium); Text("Principal ${money(l.principal)}  •  ${l.interestRate}% interest"); Text("Due ${money(l.principal + l.principal*l.interestRate/100 - l.paid)}  •  ${l.status}"); if (l.status == "ACTIVE") Button({ pay(l.id, l.principal + l.principal*l.interestRate/100 - l.paid) }) { Text("Mark fully paid") } } } } }
    if (show) LoanDialog(borrowers, { show = false }, add)
}
@Composable private fun LoanDialog(borrowers: List<Borrower>, close: () -> Unit, add: (Long, Double, Double, String) -> Unit) { var selected by remember { mutableStateOf(borrowers.first()) }; var principal by remember { mutableStateOf("") }; var rate by remember { mutableStateOf("0") }; AlertDialog(onDismissRequest = close, title = { Text("New loan") }, text = { Column { Text("Borrower: ${selected.name}"); borrowers.drop(1).forEach { TextButton({ selected = it }) { Text(it.name) } }; OutlinedTextField(principal, { principal = it }, label = { Text("Principal") }); OutlinedTextField(rate, { rate = it }, label = { Text("Interest %") }) } }, confirmButton = { Button({ principal.toDoubleOrNull()?.let { add(selected.id, it, rate.toDoubleOrNull() ?: 0.0, LocalDate.now().toString()); close() } }) { Text("Save") } }, dismissButton = { TextButton(close) { Text("Cancel") } }) }
