package com.dsh.harness.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** 独立路由：编辑提供方（占位，主编辑流程通过 ProviderDialog 完成）。 */
@Composable
fun ProviderEditScreen(providerId: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Provider: $providerId")
    }
}
