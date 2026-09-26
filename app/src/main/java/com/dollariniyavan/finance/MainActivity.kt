package com.dollariniyavan.finance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = FinanceDb(this)
        db.seedDemoDataIfEmpty()

        setContent {
            FinanceApp(db)
        }
    }
}

private fun formatMoney(value: Double): String = NumberFormat.getCurrencyInstance(Locale.getDefault()).format(value)

@Composable
fun FinanceApp(db: FinanceDb) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var refresh by remember { mutableIntStateOf(0) }

    val borrowers = remember(refresh) { db.borrowers() }
    val loans = remember(refresh) { db.loans() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Loan Manager") }) },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(onClick = { refresh += 1 }) {
                    Icon(Icons.Default.Add, contentDescription = "Add borrower")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF5F7FB))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Overview", "Borrowers", "Loans").forEachIndexed { index, label ->
                    FilterChip(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(label) }
                    )
                }
            }

            when (selectedTab) {
                0 -> OverviewScreen(borrowers, loans)
                1 -> BorrowersScreen(
                    borrowers = borrowers,
                    onAdd = { name, phone, address ->
                        db.addBorrower(name, phone, address)
                        refresh += 1
                    },
                    onDelete = { borrowerId ->
                        db.deleteBorrower(borrowerId)
                        refresh += 1
                    }
                )
                2 -> LoansScreen(
                    borrowers = borrowers,
                    loans = loans,
                    onAddLoan = { borrowerId, principal, rate, date ->
                        db.addLoan(borrowerId, principal, rate, date)
                        refresh += 1
                    },
                    onAddPayment = { loanId, amount ->
                        db.addPayment(loanId, amount)
                        refresh += 1
                    },
                    onDeleteLoan = { loanId ->
                        db.deleteLoan(loanId)
                        refresh += 1
                    }
                )
            }
        }
    }
}

@Composable
private fun OverviewScreen(borrowers: List<Borrower>, loans: List<Loan>) {
    val totalOutstanding = loans.sumOf { loan ->
        (loan.principal + (loan.principal * loan.interestRate / 100.0)) - loan.paid
    }
    val totalPrincipal = loans.sumOf { it.principal }
    val paid = loans.sumOf { it.paid }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text("Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(title = "Borrowers", value = borrowers.size.toString(), modifier = Modifier.weight(1f))
            StatCard(title = "Active", value = loans.count { it.status == "ACTIVE" }.toString(), modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))
        StatCard(title = "Outstanding", value = formatMoney(totalOutstanding), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        StatCard(title = "Principal", value = formatMoney(totalPrincipal), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        StatCard(title = "Collected", value = formatMoney(paid), modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BorrowersScreen(
    borrowers: List<Borrower>,
    onAdd: (String, String, String) -> Unit,
    onDelete: (Long) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Borrowers", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Button(onClick = { showDialog = true }) { Text("Add") }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn {
            items(borrowers) { borrower ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(borrower.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${borrower.phone}")
                            Text("${borrower.address}")
                        }

                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete borrower",
                            tint = Color.Red,
                            modifier = Modifier
                                .size(30.dp)
                                .clickable { onDelete(borrower.id) }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        BorrowerDialog(
            onDismiss = { showDialog = false },
            onSave = { name, phone, address ->
                onAdd(name, phone, address)
                showDialog = false
            }
        )
    }
}

@Composable
private fun BorrowerDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add borrower") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") })
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = { onSave(name.trim(), phone.trim(), address.trim()) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun LoansScreen(
    borrowers: List<Borrower>,
    loans: List<Loan>,
    onAddLoan: (Long, Double, Double, String) -> Unit,
    onAddPayment: (Long, Double) -> Unit,
    onDeleteLoan: (Long) -> Unit
) {
    var showLoanDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Loans", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Button(onClick = { if (borrowers.isNotEmpty()) showLoanDialog = true }, enabled = borrowers.isNotEmpty()) {
                Text("Add")
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn {
            items(loans) { loan ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(loan.borrowerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete loan",
                                tint = Color.Red,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clickable { onDeleteLoan(loan.id) }
                            )
                        }

                        Spacer(Modifier.height(6.dp))
                        Text("Principal: ${formatMoney(loan.principal)}")
                        Text("Interest: ${loan.interestRate}%")
                        Text("Outstanding: ${formatMoney((loan.principal + (loan.principal * loan.interestRate / 100.0)) - loan.paid)}")
                        Text("Status: ${loan.status}")

                        if (loan.status == "ACTIVE") {
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onAddPayment(loan.id, (loan.principal + (loan.principal * loan.interestRate / 100.0)) - loan.paid) }) {
                                    Text("Pay full")
                                }
                                Button(onClick = { onAddPayment(loan.id, loan.principal * 0.1) }) {
                                    Text("Pay 10%")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLoanDialog) {
        LoanDialog(
            borrowers = borrowers,
            onDismiss = { showLoanDialog = false },
            onSave = { borrowerId, principal, rate, date ->
                onAddLoan(borrowerId, principal, rate, date)
                showLoanDialog = false
            }
        )
    }
}

@Composable
private fun LoanDialog(
    borrowers: List<Borrower>,
    onDismiss: () -> Unit,
    onSave: (Long, Double, Double, String) -> Unit
) {
    val firstBorrower = borrowers.firstOrNull() ?: return
    var selectedBorrower by remember { mutableStateOf(firstBorrower) }
    var principal by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("10") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add loan") },
        text = {
            Column {
                Text("Borrower: ${selectedBorrower.name}")
                Spacer(Modifier.height(8.dp))
                borrowers.forEach { borrower ->
                    TextButton(onClick = { selectedBorrower = borrower }) {
                        Text(borrower.name)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = principal, onValueChange = { principal = it }, label = { Text("Principal") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = rate, onValueChange = { rate = it }, label = { Text("Interest %") })
            }
        },
        confirmButton = {
            Button(
                enabled = principal.toDoubleOrNull() != null,
                onClick = {
                    val p = principal.toDoubleOrNull() ?: return@Button
                    val r = rate.toDoubleOrNull() ?: 0.0
                    onSave(selectedBorrower.id, p, r, LocalDate.now().toString())
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
