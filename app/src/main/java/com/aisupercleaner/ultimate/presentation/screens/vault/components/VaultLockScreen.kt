package com.aisupercleaner.ultimate.presentation.screens.vault.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.aisupercleaner.ultimate.R
import com.aisupercleaner.ultimate.presentation.screens.vault.VaultUiState

@Composable
fun VaultLockScreen(
    state: VaultUiState,
    onPinChanged: (String) -> Unit,
    onSubmitPin: () -> Unit,
    onBiometric: (FragmentActivity) -> Unit,
    onSetupPin: () -> Unit,
) {
    val activity = androidx.compose.ui.platform.LocalContext.current as? FragmentActivity
    val shakeScale by animateFloatAsState(targetValue = if (state.pinError) 1.03f else 1f, label = "shake")

    LaunchedEffect(state.biometricAvailable, state.biometricEnabled, state.needsSetup) {
        if (!state.needsSetup && state.biometricAvailable && state.biometricEnabled && activity != null) onBiometric(activity)
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(120.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer).scale(shakeScale),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(60.dp))
        }
        Spacer(Modifier.height(32.dp))
        Text(stringResource(R.string.vault_locked_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            if (state.needsSetup) stringResource(R.string.vault_setup_hint) else stringResource(R.string.vault_unlock_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))

        if (!state.needsSetup) {
            OutlinedTextField(
                value = state.pinInput,
                onValueChange = onPinChanged,
                label = { Text(stringResource(R.string.vault_pin_label)) },
                singleLine = true,
                isError = state.pinError,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                supportingText = if (state.pinError) { { Text(stringResource(R.string.vault_pin_wrong), color = MaterialTheme.colorScheme.error) } } else null,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onSubmitPin, modifier = Modifier.fillMaxWidth().height(52.dp), shape = MaterialTheme.shapes.large) {
                Text(stringResource(R.string.vault_unlock), fontWeight = FontWeight.SemiBold)
            }
            if (state.biometricAvailable && state.biometricEnabled && activity != null) {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = { onBiometric(activity) }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = MaterialTheme.shapes.large) {
                    Icon(Icons.Rounded.Fingerprint, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.vault_use_biometric))
                }
            }
        } else {
            Button(onClick = onSetupPin, modifier = Modifier.fillMaxWidth().height(52.dp), shape = MaterialTheme.shapes.large) {
                Text(stringResource(R.string.vault_setup_pin), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
