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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinanceTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    FinanceApp(FinanceDb(this))
                }
            }
        }
    }
}

@Composable
private fun FinanceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}

private fun money(value: Double): String = NumberFormat.getCurrencyInstance(Locale.getDefault()).format(value)

@Composable
fun FinanceApp(db: FinanceDb) {
    var tab by remember { mutableIntStateOf(0) }
    var refresh by remember { mutableIntStateOf(0) }

    val borrowers = remember(refresh) { db.borrowers() }
    val loans = remember(refresh) { db.loans() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Finance") }) },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Dashboard", "Borrowers", "Loans").forEachIndexed { index, label ->
                    FilterChip(
                        selected = tab == index,
                        onClick = { tab = index },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            when (tab) {
                0 -> Dashboard(borrowers, loans)
                1 -> BorrowersScreen(borrowers) { name, phone, address ->
                    db.addBorrower(name, phone, address)
                    refresh += 1
                }
                else -> LoansScreen(borrowers, loans, { loanId, amount ->
                    db.addPayment(loanId, amount)
                    refresh += 1
                }) { borrowerId, principal, rate, date ->
                    db.addLoan(borrowerId, principal, rate, date)
                    refresh += 1
                }
            }
        }
    }
}

@Composable
private fun Dashboard(borrowers: List<Borrower>, loans: List<Loan>) {
    val outstanding = loans.sumOf { loan ->
        (loan.principal + (loan.principal * loan.interestRate / 100.0)) - loan.paid
    }

    Text("Overview", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard("Borrowers", borrowers.size.toString(), Modifier.weight(1f))
        StatCard("Active loans", loans.count { it.status == "ACTIVE" }.toString(), Modifier.weight(1f))
    }

    Spacer(Modifier.height(12.dp))
    StatCard("Outstanding", money(outstanding), Modifier.fillMaxWidth())

    Spacer(Modifier.height(20.dp))
    Text(
        text = "All data is stored locally on this device.",
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Gray
    )
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun BorrowersScreen(
    data: List<Borrower>,
    add: (String, String, String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Text("Borrowers", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(8.dp))
    Button(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) {
        Text("Add borrower")
    }

    Spacer(Modifier.height(8.dp))

    LazyColumn {
        items(data) { borrower ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(borrower.name, style = MaterialTheme.typography.titleMedium)
                    Text("${borrower.phone} • ${borrower.address}")
                }
            }
        }
    }

    if (showDialog) {
        BorrowerDialog(
            close = { showDialog = false },
            add = add
        )
    }
}

@Composable
private fun BorrowerDialog(
    close: () -> Unit,
    add: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = close,
        title = { Text("New borrower") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        add(name.trim(), phone.trim(), address.trim())
                        close()
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = close) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun LoansScreen(
    borrowers: List<Borrower>,
    loans: List<Loan>,
    pay: (Long, Double) -> Unit,
    add: (Long, Double, Double, String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Text("Loans", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = { if (borrowers.isNotEmpty()) showDialog = true },
        modifier = Modifier.fillMaxWidth(),
        enabled = borrowers.isNotEmpty()
    ) {
        Text("Add loan")
    }

    if (borrowers.isEmpty()) {
        Spacer(Modifier.height(18.dp))
        Text("Add a borrower first before creating a loan.")
    }

    Spacer(Modifier.height(8.dp))

    LazyColumn {
        items(loans) { loan ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(loan.borrowerName, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Principal: ${money(loan.principal)}")
                    Text("Rate: ${loan.interestRate}%")
                    Text("Outstanding: ${money((loan.principal + (loan.principal * loan.interestRate / 100.0)) - loan.paid)}")
                    Text("Status: ${loan.status}")

                    if (loan.status == "ACTIVE") {
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val remaining = (loan.principal + (loan.principal * loan.interestRate / 100.0)) - loan.paid
                                pay(loan.id, remaining)
                            }
                        ) {
                            Text("Mark fully paid")
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        LoanDialog(
            borrowers = borrowers,
            close = { showDialog = false },
            add = add
        )
    }
}

@Composable
private fun LoanDialog(
    borrowers: List<Borrower>,
    close: () -> Unit,
    add: (Long, Double, Double, String) -> Unit
) {
    val firstBorrower = borrowers.firstOrNull() ?: return
    var selected by remember { mutableStateOf(firstBorrower) }
    var principal by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = close,
        title = { Text("New loan") },
        text = {
            Column {
                Text("Selected borrower: ${selected.name}")
                Spacer(Modifier.height(8.dp))
                borrowers.forEach { borrower ->
                    TextButton(onClick = { selected = borrower }) {
                        Text(borrower.name)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = principal,
                    onValueChange = { principal = it },
                    label = { Text("Principal amount") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = rate,
                    onValueChange = { rate = it },
                    label = { Text("Interest %") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = principal.toDoubleOrNull() ?: return@Button
                    val r = rate.toDoubleOrNull() ?: 0.0
                    add(selected.id, p, r, LocalDate.now().toString())
                    close()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = close) {
                Text("Cancel")
            }
        }
    )
}
