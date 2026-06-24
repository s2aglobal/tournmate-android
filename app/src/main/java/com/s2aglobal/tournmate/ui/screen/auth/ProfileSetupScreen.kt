package com.s2aglobal.tournmate.ui.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.s2aglobal.tournmate.R
import com.s2aglobal.tournmate.domain.model.Gender
import com.s2aglobal.tournmate.domain.model.HomeRegionCountry
import com.s2aglobal.tournmate.domain.model.PlayerAvatar
import com.s2aglobal.tournmate.ui.theme.BrandPurple

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileSetupScreen(
    viewModel: ProfileSetupViewModel = hiltViewModel(),
    onComplete: () -> Unit,
) {
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf(Gender.PREFER_NOT_TO_SAY) }
    var selectedAvatar by remember { mutableStateOf(PlayerAvatar.DEFAULT) }
    var selectedCountry by remember { mutableStateOf(HomeRegionCountry.fallback) }
    var postalCode by remember { mutableStateOf("") }
    var countryMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.profile_setup_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = "Step ${step + 1} of 3",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(32.dp))

        when (step) {
            // Step 1: Name + Gender
            0 -> {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.enter_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.select_gender),
                    style = MaterialTheme.typography.titleMedium,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Gender.entries.forEach { gender ->
                    OutlinedButton(
                        onClick = { selectedGender = gender },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = if (selectedGender == gender) {
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = BrandPurple.copy(alpha = 0.1f),
                                contentColor = BrandPurple,
                            )
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        },
                    ) {
                        Text(gender.displayName)
                    }
                }
            }

            // Step 2: Avatar
            1 -> {
                Text(
                    text = stringResource(R.string.choose_avatar),
                    style = MaterialTheme.typography.titleMedium,
                )

                Spacer(modifier = Modifier.height(16.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    PlayerAvatar.selectable.forEach { avatar ->
                        val isSelected = avatar == selectedAvatar
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(avatar.avatarUrl(64))
                                .crossfade(true)
                                .build(),
                            contentDescription = avatar.displayName,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .then(
                                    if (isSelected) Modifier.border(3.dp, BrandPurple, CircleShape)
                                    else Modifier.border(1.dp, Color.LightGray, CircleShape)
                                )
                                .clickable { selectedAvatar = avatar },
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }

            // Step 3: Home region
            2 -> {
                Text(
                    text = stringResource(R.string.home_region),
                    style = MaterialTheme.typography.titleMedium,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box {
                    OutlinedButton(
                        onClick = { countryMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("${selectedCountry.name} (${selectedCountry.code})")
                    }

                    DropdownMenu(
                        expanded = countryMenuExpanded,
                        onDismissRequest = { countryMenuExpanded = false },
                    ) {
                        HomeRegionCountry.pickerOptions.forEach { country ->
                            DropdownMenuItem(
                                text = { Text("${country.name} (${country.code})") },
                                onClick = {
                                    selectedCountry = country
                                    countryMenuExpanded = false
                                },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = postalCode,
                    onValueChange = { postalCode = it },
                    label = { Text(stringResource(R.string.postal_code)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Navigation buttons
        if (step > 0) {
            OutlinedButton(
                onClick = { step-- },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.back))
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                if (step < 2) {
                    step++
                } else {
                    viewModel.saveProfile(
                        name = name,
                        gender = selectedGender,
                        avatarId = selectedAvatar.id,
                        homeCountryCode = selectedCountry.code,
                        homePostalCode = postalCode,
                        onComplete = onComplete,
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
            enabled = when (step) {
                0 -> name.length >= 2
                else -> true
            },
        ) {
            Text(
                if (step < 2) stringResource(R.string.next) else stringResource(R.string.done),
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
