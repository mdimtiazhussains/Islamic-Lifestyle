package com.example.ui.components.money

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.InvestmentAsset
import com.example.data.model.InvestmentAssetCategory
import com.example.data.model.RentCollectionStatus
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddInvestmentAssetDialog(
    onDismiss: () -> Unit,
    onSave: (InvestmentAsset) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(InvestmentAssetCategory.STOCKS_FUNDS) }
    var currentValueText by remember { mutableStateOf("") }
    var investedAmountText by remember { mutableStateOf("") }
    var monthlyRentText by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("dialog_add_investment")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(selectedCategory.iconEmoji, fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Add Asset / Investment",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Asset Category Chips
                Text(
                    text = "Asset Class",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InvestmentAssetCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text("${cat.iconEmoji} ${cat.displayName}", fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Asset / Property Name") },
                    placeholder = { Text("Enter asset or property name") },
                    modifier = Modifier.fillMaxWidth().testTag("input_asset_name"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Current Value & Invested Principal
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentValueText,
                        onValueChange = { currentValueText = it },
                        label = { Text("Current Valuation (₹)") },
                        placeholder = { Text("0") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("input_asset_current_value"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = investedAmountText,
                        onValueChange = { investedAmountText = it },
                        label = { Text("Invested Principal (₹)") },
                        placeholder = { Text("0") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("input_asset_invested"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // If Real Estate / Rental: Monthly Rent Flow
                if (selectedCategory == InvestmentAssetCategory.REAL_ESTATE_RENTAL) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = monthlyRentText,
                        onValueChange = { monthlyRentText = it },
                        label = { Text("Monthly Rental Income (₹)") },
                        placeholder = { Text("e.g. 18000") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("Brokerage, folio number, registry details") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val currentVal = currentValueText.toDoubleOrNull()
                        if (currentVal == null || currentVal < 0) {
                            errorMsg = "Please enter a valid current valuation"
                            return@Button
                        }
                        if (name.isBlank()) {
                            errorMsg = "Please enter asset name"
                            return@Button
                        }
                        val investedVal = investedAmountText.toDoubleOrNull() ?: currentVal
                        val growthPct = if (investedVal > 0) ((currentVal - investedVal) / investedVal) * 100 else 0.0
                        val monthlyRent = monthlyRentText.toDoubleOrNull() ?: 0.0

                        val asset = InvestmentAsset(
                            id = UUID.randomUUID().toString(),
                            name = name.trim(),
                            category = selectedCategory,
                            currentValue = currentVal,
                            investedAmount = investedVal,
                            growthPercentage = growthPct,
                            monthlyRentalIncome = monthlyRent,
                            rentStatus = if (selectedCategory == InvestmentAssetCategory.REAL_ESTATE_RENTAL && monthlyRent > 0) RentCollectionStatus.COLLECTED else RentCollectionStatus.NONE,
                            notes = notes.trim()
                        )
                        onSave(asset)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_save_asset"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Investment Asset", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
